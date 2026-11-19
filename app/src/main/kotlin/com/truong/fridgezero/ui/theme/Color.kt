package com.truong.fridgezero.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// spec.md §1.1 — Neo-Brutalist palette
val PaperCream = Color(0xFFEAE2D3)
val DotGridInk = Color(0xFFC4B8A3)
val SurfaceWhite = Color(0xFFFFFFFF)
val InkBlack = Color(0xFF111111)
val LimeFresh = Color(0xFFA3E635)
val LemonWarn = Color(0xFFFACC15)
val CoralUrgent = Color(0xFFFB7185)
val LilacAccent = Color(0xFFC084FC)
val SkyInfo = Color(0xFF38BDF8)
val CavitySlate = Color(0xFF1E293B)

val ButterSoft = Color(0xFFFEF08A)
val ButterPale = Color(0xFFFEF9C3)
val SkyPale = Color(0xFFE0F2FE)

/** Single source of truth for the Neo-Brutalist dot-grid background shared by Compose and Three.js. */
object DotGridSpec {
  const val PAPER_HEX: String = "#EAE2D3"
  const val DOT_HEX: String = "#C4B8A3"
  const val STEP_DP: Float = 18f
  const val DOT_RADIUS_DP: Float = 1.5f

  val paperColor: Color = PaperCream
  val dotColor: Color = DotGridInk
  val step: Dp = STEP_DP.dp
  val dotRadius: Dp = DOT_RADIUS_DP.dp
}
