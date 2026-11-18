package com.truong.fridgezero

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truong.fridgezero.data.AppConfig
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.Seed
import com.truong.fridgezero.notifications.rememberReminderPromptController
import com.truong.fridgezero.ui.aiscan.AiScanPlaceholderScreen
import com.truong.fridgezero.ui.bento.BentoFilter
import com.truong.fridgezero.ui.bento.CompartmentBentoScreen
import com.truong.fridgezero.ui.designer.FridgeDesignerScreen
import com.truong.fridgezero.ui.fridge3d.Fridge3DStageBackdrop
import com.truong.fridgezero.ui.fridge3d.LocalFridge3DStageController
import com.truong.fridgezero.ui.fridge3d.SharedFridge3DWebView
import com.truong.fridgezero.ui.fridge3d.rememberFridge3DStageController
import com.truong.fridgezero.ui.home.HomeScreen
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.FridgeZeroTheme
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.dotGridBackground
import com.truong.fridgezero.vm.FridgeViewModel
import kotlinx.coroutines.launch

private const val CRASH_TRACE_FILE = "crash_trace.txt"

private class FridgeCrashHandler(
  private val crashFile: File,
  private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
  override fun uncaughtException(thread: Thread, throwable: Throwable) {
    runCatching {
      crashFile.writeText(Log.getStackTraceString(throwable))
    }
    previous?.uncaughtException(thread, throwable)
  }
}

private sealed interface RootRoute {
  data object Home : RootRoute
  data object Designer : RootRoute
  data object AiScan : RootRoute
  data class Bento(val compartmentId: String, val filterUrgent: Boolean = false) : RootRoute
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val crashFile = File(applicationContext.filesDir, CRASH_TRACE_FILE)
    if (Thread.getDefaultUncaughtExceptionHandler() !is FridgeCrashHandler) {
      val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
      Thread.setDefaultUncaughtExceptionHandler(FridgeCrashHandler(crashFile, previousHandler))
    }

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
    )
    setContent {
      FridgeZeroTheme {
        var savedCrashTrace by remember {
          mutableStateOf(
            runCatching {
              if (crashFile.exists()) crashFile.readText().takeIf { it.isNotBlank() } else null
            }.getOrNull()
          )
        }

        if (savedCrashTrace != null) {
          CrashReportScreen(
            trace = savedCrashTrace.orEmpty(),
            onClearAndContinue = {
              runCatching { if (crashFile.exists()) crashFile.delete() }
              savedCrashTrace = null
            }
          )
        } else {
          val stageController = rememberFridge3DStageController()
          val appConfig = remember { AppConfig(applicationContext) }
          val scope = rememberCoroutineScope()
          val onboardingDone by appConfig.hasCompletedOnboarding
            .collectAsStateWithLifecycle(initialValue = null)
          val vm: FridgeViewModel = viewModel(factory = FridgeViewModel.factory(applicationContext))
          val uiState by vm.uiState.collectAsStateWithLifecycle()
          val reminderController = rememberReminderPromptController(appConfig)
          var route by remember { mutableStateOf<RootRoute>(RootRoute.Home) }
          var justClosedCompartmentId by remember { mutableStateOf<String?>(null) }

          CompositionLocalProvider(LocalFridge3DStageController provides stageController) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .dotGridBackground()
                .onGloballyPositioned { stageController.rootCoordinates = it }
            ) {
              // Optional framed card backdrop behind WebView (used by FridgeDesignerScreen)
              Fridge3DStageBackdrop(
                controller = stageController,
                modifier = Modifier.zIndex(0.5f)
              )

              // Single shared WebView hosted at root above dotGridBackground and below foreground UI
              SharedFridge3DWebView(
                controller = stageController,
                modifier = Modifier.zIndex(1f)
              )

              // Foreground screen UI, DoorStickersOverlay, HomeDashboardHeader, HomeTaskBar, and FAB
              Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                when {
                  onboardingDone == null -> Unit
                  onboardingDone == false || route is RootRoute.Designer -> {
                    val isEditingExisting = onboardingDone == true
                    val fridge = uiState.activeFridge ?: Seed.defaultFridge
                    val current = if (isEditingExisting) {
                      uiState.compartments.mapIndexed { idx, c ->
                        CompartmentEntity(
                          id = c.id, fridgeId = fridge.id, name = c.name,
                          x = c.rect.x, y = c.rect.y, w = c.rect.w, h = c.rect.h,
                          kind = c.kind, shelves = c.shelves, sort = idx
                        )
                      }
                    } else {
                      Seed.compartmentsForTemplate(Seed.defaultFridge.template, Seed.defaultFridge.id)
                    }
                    if (isEditingExisting) {
                      BackHandler { route = RootRoute.Home }
                    }
                    FridgeDesignerScreen(
                      initialFridge = if (isEditingExisting) fridge else Seed.defaultFridge,
                      initialCompartments = current,
                      onConfirm = { f, comps ->
                        vm.saveCustomFridge(
                          fridge = f,
                          compartments = comps,
                          includeSampleItems = true,
                          triggerWelcomeSpin = true
                        )
                        scope.launch { appConfig.setOnboardingCompleted(true) }
                        route = RootRoute.Home
                      }
                    )
                  }
                  route is RootRoute.AiScan -> {
                    BackHandler { route = RootRoute.Home }
                    AiScanPlaceholderScreen(onBack = { route = RootRoute.Home })
                  }
                  route is RootRoute.Bento -> {
                    val bentoRoute = route as RootRoute.Bento
                    val comp = uiState.compartments.firstOrNull { it.id == bentoRoute.compartmentId }
                      ?: uiState.compartments.firstOrNull()
                    if (comp == null) {
                      route = RootRoute.Home
                    } else {
                      val exitToHome = {
                        justClosedCompartmentId = comp.id
                        route = RootRoute.Home
                      }
                      BackHandler(onBack = exitToHome)
                      CompartmentBentoScreen(
                        compartment = comp,
                        allCompartments = uiState.compartments,
                        recentKeys = uiState.recentItemKeys,
                        initialFilter = if (bentoRoute.filterUrgent) BentoFilter.URGENT else BentoFilter.ALL,
                        onSelectCompartment = { nextId ->
                          route = RootRoute.Bento(nextId, filterUrgent = false)
                        },
                        onBack = exitToHome,
                        onAddItem = { compId, name, iconKey, qty, daysLeft, unit ->
                          vm.addItem(compId, name, iconKey, qty, daysLeft, unit)
                          reminderController.onItemSavedWithExpiry(daysLeft)
                        },
                        onUpdateItem = { id, name, iconKey, qty, daysLeft, unit ->
                          vm.updateItem(id, name, iconKey, qty, daysLeft, unit)
                          reminderController.onItemSavedWithExpiry(daysLeft)
                        },
                        onAdjustQuantity = vm::adjustQuantity,
                        onToggleOpened = vm::toggleItemOpened,
                        onMoveCompartment = { ids, targetId, newDays ->
                          vm.moveItemToCompartment(ids, targetId, newDays)
                        },
                        onMarkStatus = { ids, status ->
                          vm.markItemStatus(ids, status)
                        }
                      )
                    }
                  }
                  else -> {
                    HomeScreen(
                      uiState = uiState,
                      remindersEnabled = reminderController.remindersEnabled,
                      justClosedCompartmentId = justClosedCompartmentId,
                      onJustClosedConsumed = { justClosedCompartmentId = null },
                      onToggleReminders = reminderController.onToggleReminders,
                      onWelcomeSpinConsumed = vm::consumeWelcomeSpin,
                      onOpenCompartment = { compId, filterUrgent ->
                        route = RootRoute.Bento(compId, filterUrgent)
                      },
                      onAddItem = { compId, name, iconKey, qty, daysLeft, unit ->
                        vm.addItem(compId, name, iconKey, qty, daysLeft, unit)
                        reminderController.onItemSavedWithExpiry(daysLeft)
                      },
                      onOpenAiScan = { route = RootRoute.AiScan },
                      onOpenDesigner = { route = RootRoute.Designer }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CrashReportScreen(
  trace: String,
  onClearAndContinue: () -> Unit
) {
  val context = LocalContext.current
  val summary = remember(trace) {
    trace.lineSequence().firstOrNull { it.isNotBlank() } ?: "UncaughtException"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "NHẬT KÝ LỖI THIẾT BỊ",
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 20.sp,
          color = InkBlack
        )
        Text(
          text = summary,
          fontFamily = BodyFont,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = InkBlack.copy(alpha = 0.8f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      StickerBadge(
        text = "CRASH LOG",
        containerColor = CoralUrgent,
        mono = true
      )
    }

    NeoBrutalistCard(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(12.dp)
      ) {
        Text(
          text = trace,
          fontFamily = MonoFont,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          lineHeight = 16.sp,
          color = InkBlack
        )
      }
    }

    Spacer(Modifier.height(2.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      NeoBrutalistButton(
        onClick = {
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
          cm?.setPrimaryClip(ClipData.newPlainText("FridgeZero Crash", trace))
          Toast.makeText(context, "Đã sao chép lỗi", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.weight(1f),
        containerColor = LemonWarn,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
      ) {
        Text(
          text = "Sao chép lỗi",
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          color = InkBlack,
          maxLines = 1
        )
      }

      NeoBrutalistButton(
        onClick = onClearAndContinue,
        modifier = Modifier.weight(1f),
        containerColor = LimeFresh,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
      ) {
        Text(
          text = "Xóa lỗi & Vào app",
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          color = InkBlack,
          maxLines = 1
        )
      }
    }
  }
}
