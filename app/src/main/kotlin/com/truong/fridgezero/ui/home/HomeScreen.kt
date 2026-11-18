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
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.FridgeUiState
import com.truong.fridgezero.ui.bento.QuickAddItemSheet
import com.truong.fridgezero.ui.fridge3d.DoorProjection
import com.truong.fridgezero.ui.fridge3d.Fridge3DStageSlot
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterPale
import com.truong.fridgezero.ui.theme.CoralUrgent
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
  onOpenAiScan: () -> Unit,
  onOpenDesigner: () -> Unit,
  modifier: Modifier = Modifier
) {
  val fridge = uiState.activeFridge
  val compartments = uiState.compartments
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

  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current

  fun triggerDoorOpen(compartmentId: String) {
    if (isNavigatingOut) return
    isNavigatingOut = true
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
      remindersEnabled = remindersEnabled,
      onToggleReminders = onToggleReminders,
      onOpenCompartment = onOpenCompartment
    )

    // Center 3D Hero Stage + Floating "+ Thêm nhanh" button
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 4.dp)
      ) {
        StickerBadge(
          text = stringResource(R.string.home_stage_hint),
          containerColor = SurfaceWhite,
          tiltDegrees = 0f
        )
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 24.dp, bottom = 12.dp)
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
          surfaceBg = "#FAF8F5",
          onDoorClick = { compId -> triggerDoorOpen(compId) },
          onProjectionsUpdated = { projections = it },
          modifier = Modifier.fillMaxSize()
        )
        DoorStickersOverlay(
          compartments = compartments,
          projections = projections,
          onStickerClick = { compId -> triggerDoorOpen(compId) }
        )
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

    // Fixed 72.dp Neo-Brutalist Bottom Task Bar
    HomeTaskBar(
      onResetFridgeView = { resetOrbitKey++ },
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
    Freshness.WARN -> LemonWarn
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
        .padding(horizontal = 9.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Box(
        modifier = Modifier
          .size(9.dp)
          .clip(CircleShape)
          .background(dotColor)
          .border(1.5.dp, InkBlack, CircleShape)
      )
      Text(
        text = compartment.name,
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        color = InkBlack,
        softWrap = false,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(InkBlack)
          .padding(horizontal = 5.dp, vertical = 1.dp)
      ) {
        Text(
          text = "${compartment.items.size}",
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
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
  onResetFridgeView: () -> Unit,
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
        .height(72.dp)
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      NeoBrutalistButton(
        onClick = onResetFridgeView,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        containerColor = ButterPale,
        cornerRadius = 12.dp,
        borderWidth = 2.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
      ) {
        Text(
          text = stringResource(R.string.home_tab_fridge),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Box(modifier = Modifier.weight(1.05f).fillMaxHeight()) {
        NeoBrutalistButton(
          onClick = onOpenAiScan,
          modifier = Modifier.fillMaxSize(),
          containerColor = LilacAccent,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
          Text(
            text = stringResource(R.string.home_tab_scan_ai),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = InkBlack,
            softWrap = false,
            maxLines = 1
          )
        }
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 4.dp, y = (-6).dp)
        ) {
          StickerBadge(
            text = stringResource(R.string.home_tab_scan_badge),
            containerColor = LemonWarn,
            tiltDegrees = 3f,
            mono = true
          )
        }
      }

      NeoBrutalistButton(
        onClick = onOpenDesigner,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        containerColor = SurfaceWhite,
        cornerRadius = 12.dp,
        borderWidth = 2.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
      ) {
        Text(
          text = stringResource(R.string.home_tab_redesign),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    Spacer(Modifier.navigationBarsPadding())
  }
}
