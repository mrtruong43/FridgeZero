package com.truong.fridgezero.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.CulinaryFilter
import com.truong.fridgezero.domain.CulinaryGuideEngine
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.FridgeUiState
import com.truong.fridgezero.ui.bento.QuickAddItemSheet
import com.truong.fridgezero.ui.fridge3d.DoorProjection
import com.truong.fridgezero.ui.fridge3d.FallbackCompartmentStat
import com.truong.fridgezero.ui.fridge3d.Fridge3DStageSlot
import com.truong.fridgezero.ui.fridge3d.LocalFridge3DStageController
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterPale
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DotGridSpec
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val DOOR_OPEN_NAV_DELAY_MS = 380L

/**
 * Screen 2 — Home Dashboard + Interactive 3D Hero Stage + Door Stickers + FAB + Fixed Task Bar.
 */
@Composable
fun HomeScreen(
  uiState: FridgeUiState,
  remindersEnabled: Boolean,
  justClosedCompartmentId: String? = null,
  onJustClosedConsumed: () -> Unit = {},
  onToggleReminders: () -> Unit,
  onWelcomeSpinConsumed: () -> Unit,
  onOpenCompartment: (compartmentId: String, filterUrgent: Boolean) -> Unit,
  onAddItem: (compartmentId: String, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  onOpenRecipes: (recipeId: String?) -> Unit,
  onOpenAiScan: () -> Unit,
  onOpenDesigner: () -> Unit,
  modifier: Modifier = Modifier
) {
  val fridge = uiState.activeFridge
  val compartments = uiState.compartments
  val allItems = remember(compartments) { compartments.flatMap { it.items } }
  val recommendations = remember(allItems) {
    CulinaryGuideEngine.recommend(allItems, CulinaryFilter.ALL)
  }
  val readyRecipeCount = remember(recommendations) {
    recommendations.count { it.isReadyToCook }
  }
  val topRecommendation = remember(recommendations) {
    recommendations.firstOrNull { it.hasUrgentMatch }
      ?: recommendations.firstOrNull { it.matchedCount > 0 }
      ?: recommendations.firstOrNull()
  }
  val entities = remember(fridge?.id, compartments) {
    val fridgeId = fridge?.id ?: "default_fridge"
    compartments.mapIndexed { idx, c ->
      CompartmentEntity(
        id = c.id,
        fridgeId = fridgeId,
        name = c.name,
        x = c.rect.x,
        y = c.rect.y,
        w = c.rect.w,
        h = c.rect.h,
        kind = c.kind,
        shelves = c.shelves,
        sort = idx
      )
    }
  }

  var resetOrbitKey by remember { mutableIntStateOf(0) }
  var itemAddedPulseKey by remember { mutableIntStateOf(0) }
  var openingId by remember { mutableStateOf<String?>(justClosedCompartmentId) }
  var isNavigatingOut by remember { mutableStateOf(false) }
  var showQuickAddSheet by remember { mutableStateOf(false) }
  var projections by remember { mutableStateOf<Map<String, DoorProjection>>(emptyMap()) }

  // When returning from CompartmentBentoScreen, start with the door open for one frame and close smoothly.
  LaunchedEffect(justClosedCompartmentId) {
    if (justClosedCompartmentId != null && !isNavigatingOut) {
      openingId = justClosedCompartmentId
      delay(40L)
      openingId = null
      onJustClosedConsumed()
    }
  }

  val stageController = LocalFridge3DStageController.current
  val fallbackStats = remember(compartments) {
    compartments.associate { comp ->
      comp.id to FallbackCompartmentStat(
        itemCount = comp.items.size,
        worstFreshness = comp.worstFreshness
      )
    }
  }
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current

  fun triggerDoorOpen(compartmentId: String) {
    if (isNavigatingOut) return
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    if (stageController.use2DFallback) {
      onOpenCompartment(compartmentId, false)
      return
    }
    isNavigatingOut = true
    openingId = compartmentId
    scope.launch {
      delay(DOOR_OPEN_NAV_DELAY_MS)
      onOpenCompartment(compartmentId, false)
      openingId = null
      isNavigatingOut = false
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
  ) {
    HomeDashboardHeader(
      compartments = compartments,
      topRecommendation = topRecommendation,
      remindersEnabled = remindersEnabled,
      onToggleReminders = onToggleReminders,
      onOpenCompartment = onOpenCompartment,
      onOpenRecipes = onOpenRecipes,
      onMarkStatus = onMarkStatus
    )

    // Center 3D Hero Stage + Floating "+ Thêm nhanh" button
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 6.dp, bottom = 8.dp)
      ) {
        Fridge3DStageSlot(
          colorKey = fridge?.colorKey ?: "inox",
          compartments = entities,
          template = fridge?.template ?: "top_freezer",
          openCompartmentId = openingId,
          resetOrbitKey = resetOrbitKey,
          itemAddedPulseKey = itemAddedPulseKey,
          playWelcomeSpin = uiState.playWelcomeSpin,
          onWelcomeSpinConsumed = onWelcomeSpinConsumed,
          showStickers = true,
          compartmentStats = fallbackStats,
          surfaceBg = DotGridSpec.PAPER_HEX,
          onDoorClick = { compId -> triggerDoorOpen(compId) },
          onProjectionsUpdated = { projections = it },
          modifier = Modifier.fillMaxSize()
        )
        if (!stageController.use2DFallback) {
          DoorStickersOverlay(
            compartments = compartments,
            projections = projections,
            onStickerClick = { compId -> triggerDoorOpen(compId) }
          )
        }
      }

      // Floating "+ Thêm nhanh" CTA anchored above bottom-right of Task Bar
      NeoBrutalistButton(
        onClick = { showQuickAddSheet = true },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 16.dp, bottom = 14.dp),
        containerColor = LimeFresh,
        cornerRadius = 14.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 4.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(InkBlack),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "+",
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = LimeFresh
          )
        }
        Text(
          text = stringResource(R.string.home_fab_quick_add).removePrefix("+ "),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }
    }

    // Fixed Neo-Brutalist 4-Button Bottom Task Bar calibrated for 360dp+ viewports
    HomeTaskBar(
      readyRecipeCount = readyRecipeCount,
      onResetFridgeView = { resetOrbitKey++ },
      onOpenRecipes = { onOpenRecipes(null) },
      onOpenAiScan = onOpenAiScan,
      onOpenDesigner = onOpenDesigner
    )
  }

  if (showQuickAddSheet && compartments.isNotEmpty()) {
    QuickAddItemSheet(
      compartments = compartments,
      initialCompartmentId = null,
      recentKeys = uiState.recentItemKeys,
      onSaveItem = { compId, name, iconKey, qty, daysLeft, unit ->
        onAddItem(compId, name, iconKey, qty, daysLeft, unit)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
          delay(140L)
          itemAddedPulseKey++
        }
      },
      onDismiss = { showQuickAddSheet = false }
    )
  }
}

@Composable
private fun DoorStickersOverlay(
  compartments: List<CompartmentUi>,
  projections: Map<String, DoorProjection>,
  onStickerClick: (String) -> Unit
) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val density = LocalDensity.current
    val maxWPx = with(density) { maxWidth.toPx() }
    val maxHPx = with(density) { maxHeight.toPx() }
    val padPx = with(density) { 8.dp.toPx() }

    compartments.forEachIndexed { index, comp ->
      key(comp.id) {
        val proj = projections[comp.id]
        val isVisible = proj?.visible == true
        val stickerAlpha by animateFloatAsState(
          targetValue = if (isVisible) 1f else 0f,
          animationSpec = tween(durationMillis = 140),
          label = "stickerAlpha_${comp.id}"
        )
        var stickerSize by remember { mutableStateOf(IntSize.Zero) }
        if (proj != null && stickerAlpha > 0.02f) {
          val tilt = if (index % 2 == 0) -1.5f else 1.5f
          val rawX = proj.xFrac * maxWPx - stickerSize.width / 2f
          val rawY = proj.yFrac * maxHPx - stickerSize.height / 2f
          val clampedX = rawX.coerceIn(padPx, (maxWPx - stickerSize.width - padPx).coerceAtLeast(padPx))
          val clampedY = rawY.coerceIn(padPx, (maxHPx - stickerSize.height - padPx).coerceAtLeast(padPx))

          DoorStickerBadge(
            compartment = comp,
            tiltDegrees = tilt,
            enabled = isVisible,
            onClick = { onStickerClick(comp.id) },
            modifier = Modifier
              .onSizeChanged { stickerSize = it }
              .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
              .alpha(stickerAlpha)
          )
        }
      }
    }
  }
}

@Composable
private fun DoorStickerBadge(
  compartment: CompartmentUi,
  tiltDegrees: Float,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val dotColor = when (compartment.worstFreshness) {
    Freshness.BAD -> CoralUrgent
    Freshness.SOON -> LemonWarn
    Freshness.OK -> LimeFresh
  }
  val cd = stringResource(
    R.string.sticker_urgent_cd,
    compartment.name,
    compartment.items.size,
    compartment.urgentCount
  )
  val corner = 10.dp
  val shape = RoundedCornerShape(corner)

  Box(
    modifier = modifier
      .rotate(tiltDegrees)
      .padding(end = 2.dp, bottom = 2.dp)
      .semantics { contentDescription = cd }
  ) {
    Row(
      modifier = Modifier
        .drawBehind {
          val d = 2.dp.toPx()
          val r = corner.toPx()
          drawRoundRect(
            color = InkBlack,
            topLeft = Offset(d, d),
            size = size,
            cornerRadius = CornerRadius(r, r)
          )
        }
        .background(SurfaceWhite, shape)
        .border(2.dp, InkBlack, shape)
        .clip(shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(dotColor)
          .border(1.5.dp, InkBlack, CircleShape)
      )
      Text(
        text = compartment.name,
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = InkBlack,
        softWrap = false,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Box(
        modifier = Modifier
          .defaultMinSize(minWidth = 22.dp)
          .clip(RoundedCornerShape(7.dp))
          .background(InkBlack)
          .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "${compartment.items.size}",
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          lineHeight = 14.sp,
          color = LimeFresh,
          softWrap = false,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun HomeTaskBar(
  readyRecipeCount: Int,
  onResetFridgeView: () -> Unit,
  onOpenRecipes: () -> Unit,
  onOpenAiScan: () -> Unit,
  onOpenDesigner: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(SurfaceWhite)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.5.dp)
        .background(InkBlack)
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(74.dp)
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      CompactTaskTabButton(
        emoji = "🧊",
        label = stringResource(R.string.home_tab_fridge_short),
        containerColor = ButterPale,
        onClick = onResetFridgeView,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
      CompactTaskTabButton(
        emoji = "🍳",
        label = stringResource(R.string.home_tab_recipes_short),
        containerColor = SurfaceWhite,
        badgeText = if (readyRecipeCount > 0) "$readyRecipeCount" else null,
        badgeColor = LimeFresh,
        contentDescriptionText = stringResource(R.string.home_tab_recipes_cd),
        onClick = onOpenRecipes,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
      CompactTaskTabButton(
        emoji = "📷",
        label = stringResource(R.string.home_tab_scan_ai_short),
        containerColor = LilacAccent,
        badgeText = stringResource(R.string.home_tab_scan_badge),
        badgeColor = LemonWarn,
        onClick = onOpenAiScan,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
      CompactTaskTabButton(
        emoji = "⚙️",
        label = stringResource(R.string.home_tab_redesign_short),
        containerColor = SurfaceWhite,
        onClick = onOpenDesigner,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
    }
    Spacer(Modifier.navigationBarsPadding())
  }
}

@Composable
private fun CompactTaskTabButton(
  emoji: String,
  label: String,
  containerColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  badgeText: String? = null,
  badgeColor: Color = LimeFresh,
  contentDescriptionText: String? = null
) {
  val buttonModifier = if (contentDescriptionText != null) {
    Modifier.fillMaxSize().semantics { contentDescription = contentDescriptionText }
  } else {
    Modifier.fillMaxSize()
  }
  Box(modifier = modifier) {
    NeoBrutalistButton(
      onClick = onClick,
      modifier = buttonModifier,
      containerColor = containerColor,
      cornerRadius = 11.dp,
      borderWidth = 2.dp,
      shadowOffset = 2.5.dp,
      contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
      ) {
        Text(text = emoji, fontSize = 15.sp, lineHeight = 16.sp)
        Text(
          text = label,
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.5.sp,
          lineHeight = 12.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (badgeText != null) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(x = (-1).dp, y = (-5).dp)
          .rotate(3f)
          .clip(RoundedCornerShape(6.dp))
          .background(badgeColor)
          .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp))
          .padding(horizontal = 5.dp, vertical = 1.dp)
      ) {
        Text(
          text = badgeText,
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 9.sp,
          lineHeight = 10.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1
        )
      }
    }
  }
}
