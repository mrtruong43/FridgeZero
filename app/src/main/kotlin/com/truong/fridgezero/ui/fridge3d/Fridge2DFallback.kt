package com.truong.fridgezero.ui.fridge3d

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterPale
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.SkyInfo
import com.truong.fridgezero.ui.theme.SurfaceWhite

data class FallbackCompartmentStat(
  val itemCount: Int,
  val worstFreshness: Freshness
)

/**
 * Pure Compose Neo-Brutalist 2D refrigerator fallback used when WebGL exceeds the bounded
 * crash-recovery budget (`> 2` failures within 60s). Keeps all Designer & Home flows interactive.
 */
@Composable
fun Fridge2DFallback(
  colorKey: String,
  compartments: List<CompartmentEntity>,
  modifier: Modifier = Modifier,
  openCompartmentId: String? = null,
  compartmentStats: Map<String, FallbackCompartmentStat> = emptyMap(),
  showItemCounts: Boolean = false,
  surfaceBg: Color? = null,
  onDoorClick: ((compartmentId: String) -> Unit)? = null
) {
  val normalizedKey = ShellColors.normalizeKey(colorKey)
  val shellColor = ShellColors.forKey(normalizedKey).color
  val handleColor = if (normalizedKey == ShellColors.Matte.key) {
    Color(0xFF94A3B8)
  } else {
    InkBlack
  }

  val containerModifier = if (surfaceBg != null) {
    modifier.background(surfaceBg)
  } else {
    modifier
  }

  BoxWithConstraints(
    modifier = containerModifier
      .fillMaxSize()
      .padding(horizontal = 24.dp, vertical = 14.dp),
    contentAlignment = Alignment.Center
  ) {
    val feetHeight = 8.dp
    val availH = (maxHeight - feetHeight).coerceAtLeast(120.dp)
    val availW = maxWidth.coerceAtLeast(80.dp)
    val aspect = 0.54f
    val cabH = minOf(availH, availW / aspect)
    val cabW = cabH * aspect

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      NeoBrutalistCard(
        modifier = Modifier.size(width = cabW, height = cabH),
        containerColor = shellColor,
        cornerRadius = 16.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 4.dp
      ) {
        BoxWithConstraints(
          modifier = Modifier
            .fillMaxSize()
            .padding(7.dp)
        ) {
          val innerW = maxWidth
          val innerH = maxHeight
          val inset = 3.dp

          compartments.forEachIndexed { index, comp ->
            val xOff = innerW * comp.x + inset
            val yOff = innerH * comp.y + inset
            val doorW = (innerW * comp.w - inset * 2).coerceAtLeast(36.dp)
            val doorH = (innerH * comp.h - inset * 2).coerceAtLeast(36.dp)
            val isOpen = comp.id == openCompartmentId
            val isRightHinge = (comp.kind == CompartmentKind.DOOR) && (comp.w < 0.6f && comp.x > 0.4f)
            val doorShape = RoundedCornerShape(10.dp)

            Box(
              modifier = Modifier
                .offset(x = xOff, y = yOff)
                .size(width = doorW, height = doorH)
                .clip(doorShape)
                .background(
                  color = if (isOpen) ButterPale.copy(alpha = 0.45f) else Color.Transparent,
                  shape = doorShape
                )
                .border(2.dp, InkBlack, doorShape)
                .clickable(
                  enabled = onDoorClick != null,
                  role = Role.Button,
                  onClick = { onDoorClick?.invoke(comp.id) }
                )
                .padding(6.dp)
            ) {
              // Handle indicator
              if (comp.kind == CompartmentKind.DOOR) {
                Box(
                  modifier = Modifier
                    .align(if (isRightHinge) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 2.dp)
                    .width(4.dp)
                    .height((doorH * 0.28f).coerceIn(14.dp, 40.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(handleColor)
                )
              } else {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .width((doorW * 0.34f).coerceIn(24.dp, 64.dp))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(handleColor)
                )
              }

              // Signature mini magnets on top compartment
              if (index == 0) {
                Row(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 8.dp),
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(7.dp)
                      .clip(CircleShape)
                      .background(LemonWarn)
                      .border(1.dp, InkBlack, CircleShape)
                  )
                  Box(
                    modifier = Modifier
                      .size(width = 10.dp, height = 6.dp)
                      .clip(RoundedCornerShape(2.dp))
                      .background(CoralUrgent)
                      .border(1.dp, InkBlack, RoundedCornerShape(2.dp))
                  )
                }
              }

              // Compartment name + optional count badge
              val stat = compartmentStats[comp.id]
              val badgeShape = RoundedCornerShape(8.dp)
              Row(
                modifier = Modifier
                  .align(Alignment.Center)
                  .background(SurfaceWhite, badgeShape)
                  .border(2.dp, InkBlack, badgeShape)
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                if (showItemCounts && stat != null) {
                  val dotColor = when (stat.worstFreshness) {
                    Freshness.BAD -> CoralUrgent
                    Freshness.SOON -> LemonWarn
                    Freshness.OK -> LimeFresh
                  }
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(dotColor)
                      .border(1.dp, InkBlack, CircleShape)
                  )
                  Text(
                    text = comp.name,
                    modifier = Modifier.weight(1f, fill = false),
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
                      .defaultMinSize(minWidth = 22.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(InkBlack)
                      .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "${stat.itemCount}",
                      fontFamily = MonoFont,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 13.sp,
                      lineHeight = 14.sp,
                      color = LimeFresh,
                      softWrap = false,
                      maxLines = 1
                    )
                  }
                } else {
                  val kindDot = if (comp.kind == CompartmentKind.DOOR) LilacAccent else SkyInfo
                  Box(
                    modifier = Modifier
                      .size(7.dp)
                      .clip(CircleShape)
                      .background(kindDot)
                      .border(1.dp, InkBlack, CircleShape)
                  )
                  Text(
                    text = comp.name,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = InkBlack,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }

      // Grounded cabinet feet
      Row(
        modifier = Modifier
          .width(cabW - 28.dp)
          .padding(top = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Spacer(
          modifier = Modifier
            .size(width = 16.dp, height = feetHeight)
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(InkBlack)
        )
        Spacer(
          modifier = Modifier
            .size(width = 16.dp, height = feetHeight)
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(InkBlack)
        )
      }
    }
  }
}
