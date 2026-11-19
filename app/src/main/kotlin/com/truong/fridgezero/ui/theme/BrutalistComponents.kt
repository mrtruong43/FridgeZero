package com.truong.fridgezero.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BrutalBorder = 2.5.dp
val BrutalShadow = 4.dp
val PressShift = 2.dp

/** Warm paper with a cached tiled dot grid (spec §1.1 & DotGridSpec). */
fun Modifier.dotGridBackground(
  paper: Color = DotGridSpec.paperColor,
  dot: Color = DotGridSpec.dotColor,
  step: Dp = DotGridSpec.step,
  dotRadius: Dp = DotGridSpec.dotRadius
): Modifier = this.drawWithCache {
  if (size.width <= 0f || size.height <= 0f) {
    return@drawWithCache onDrawBehind {}
  }
  val tilePx = step.toPx().toInt().coerceIn(2, 256)
  if (tilePx <= 0) {
    return@drawWithCache onDrawBehind {}
  }
  val tileBitmap = ImageBitmap(tilePx, tilePx)
  val tileCanvas = Canvas(tileBitmap)
  val bgPaint = Paint().apply { color = paper }
  tileCanvas.drawRect(0f, 0f, tilePx.toFloat(), tilePx.toFloat(), bgPaint)
  val dotPaint = Paint().apply {
    color = dot
    isAntiAlias = true
  }
  val center = tilePx / 2f
  val radiusPx = dotRadius.toPx().coerceAtLeast(0.5f)
  tileCanvas.drawCircle(Offset(center, center), radiusPx, dotPaint)
  val tiledBrush = ShaderBrush(ImageShader(tileBitmap, TileMode.Repeated, TileMode.Repeated))
  onDrawBehind {
    drawRect(tiledBrush)
  }
}

private fun Modifier.hardOffsetShadow(
  color: Color,
  cornerRadius: Dp,
  offset: Dp
): Modifier = this.drawBehind {
  val d = offset.toPx()
  val r = cornerRadius.toPx()
  drawRoundRect(
    color = color,
    topLeft = Offset(d, d),
    size = size,
    cornerRadius = CornerRadius(r, r)
  )
}

/** White card, 2.5.dp ink border, hard (4.dp, 4.dp) zero-blur shadow drawn strictly behind the face. */
@Composable
fun NeoBrutalistCard(
  modifier: Modifier = Modifier,
  containerColor: Color = SurfaceWhite,
  cornerRadius: Dp = 16.dp,
  borderWidth: Dp = BrutalBorder,
  shadowOffset: Dp = BrutalShadow,
  transparentInterior: Boolean = false,
  content: @Composable () -> Unit
) {
  val shape = RoundedCornerShape(cornerRadius)
  Box(
    modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset),
    propagateMinConstraints = true
  ) {
    val faceModifier = if (transparentInterior) {
      Modifier
        .drawWithCache {
          if (size.width <= 0f || size.height <= 0f) {
            return@drawWithCache onDrawBehind {}
          }
          val d = shadowOffset.toPx()
          val r = cornerRadius.toPx()
          val cr = CornerRadius(r, r)
          val inner = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, cr))
          }
          val shadow = Path().apply {
            addRoundRect(RoundRect(d, d, size.width + d, size.height + d, cr))
          }
          val outerShadow = Path.combine(PathOperation.Difference, shadow, inner)
          onDrawBehind {
            drawPath(outerShadow, color = InkBlack)
          }
        }
        .border(borderWidth, InkBlack, shape)
        .clip(shape)
    } else {
      Modifier
        .hardOffsetShadow(InkBlack, cornerRadius, shadowOffset)
        .background(containerColor, shape)
        .border(borderWidth, InkBlack, shape)
        .clip(shape)
    }
    Box(modifier = faceModifier) {
      content()
    }
  }
}

/** Tactile button: hard shadow drawn strictly behind face; on press face shifts (+2.dp, +2.dp). */
@Composable
fun NeoBrutalistButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = SurfaceWhite,
  enabled: Boolean = true,
  cornerRadius: Dp = 12.dp,
  borderWidth: Dp = BrutalBorder,
  shadowOffset: Dp = BrutalShadow,
  contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
  content: @Composable RowScope.() -> Unit
) {
  val shape = RoundedCornerShape(cornerRadius)
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val haptic = LocalHapticFeedback.current
  LaunchedEffect(pressed) {
    if (pressed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
  }
  val shown = if (pressed) minOf(PressShift, shadowOffset) else shadowOffset
  val shift = shadowOffset - shown
  val faceColor = if (enabled) containerColor else PaperCream
  val shadowColor = if (enabled) InkBlack else DotGridInk

  Box(
    modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset),
    propagateMinConstraints = true
  ) {
    Box(
      modifier = Modifier
        .offset(shift, shift)
        .hardOffsetShadow(shadowColor, cornerRadius, shown)
        .background(faceColor, shape)
        .border(BorderStroke(borderWidth, InkBlack), shape)
        .clip(shape)
        .clickable(
          interactionSource = interaction,
          indication = null,
          enabled = enabled,
          role = Role.Button,
          onClick = onClick
        )
        .padding(contentPadding),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
      )
    }
  }
}

/** Tilted sticker label: -2°..+2°, 2.dp ink border, hard (2.dp, 2.dp) shadow behind the face. */
@Composable
fun StickerBadge(
  text: String,
  modifier: Modifier = Modifier,
  containerColor: Color = LemonWarn,
  contentColor: Color = InkBlack,
  tiltDegrees: Float = -2f,
  mono: Boolean = false
) {
  val corner = 8.dp
  val shape = RoundedCornerShape(corner)
  Box(
    modifier = modifier
      .rotate(tiltDegrees)
      .padding(end = 2.dp, bottom = 2.dp)
  ) {
    Box(
      modifier = Modifier
        .hardOffsetShadow(InkBlack, corner, 2.dp)
        .background(containerColor, shape)
        .border(2.dp, InkBlack, shape)
        .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
      Text(
        text = text,
        color = contentColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = if (mono) MonoFont else BodyFont,
        softWrap = false,
        maxLines = 1
      )
    }
  }
}
