package com.truong.fridgezero.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R

// spec.md §1.2 — Space Grotesk / Plus Jakarta Sans / JetBrains Mono with full Vietnamese support.
val SpaceGroteskFont: FontFamily = FontFamily(
  Font(R.font.space_grotesk_bold, FontWeight.SemiBold),
  Font(R.font.space_grotesk_bold, FontWeight.Bold),
  Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

val PlusJakartaSansFont: FontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans_medium, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
  Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

val JetBrainsMonoFont: FontFamily = FontFamily(
  Font(R.font.jetbrains_mono_medium, FontWeight.Normal),
  Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
  Font(R.font.jetbrains_mono_medium, FontWeight.SemiBold),
  Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
  Font(R.font.jetbrains_mono_bold, FontWeight.ExtraBold)
)

val DisplayFont: FontFamily = SpaceGroteskFont
val BodyFont: FontFamily = PlusJakartaSansFont
val MonoFont: FontFamily = JetBrainsMonoFont

val NeoBrutalistTypography = Typography(
  headlineMedium = TextStyle(
    fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold,
    fontSize = 26.sp, letterSpacing = (-0.5).sp, color = InkBlack
  ),
  titleLarge = TextStyle(
    fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
    fontSize = 20.sp, color = InkBlack
  ),
  titleMedium = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold,
    fontSize = 15.sp, color = InkBlack
  ),
  bodyLarge = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp, color = InkBlack
  ),
  bodyMedium = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.Medium,
    fontSize = 13.sp, color = InkBlack
  ),
  labelLarge = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold,
    fontSize = 13.sp, color = InkBlack
  ),
  labelMedium = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.Bold,
    fontSize = 12.sp, color = InkBlack
  ),
  labelSmall = TextStyle(
    fontFamily = MonoFont, fontWeight = FontWeight.Bold,
    fontSize = 11.sp, color = InkBlack
  )
)

val FridgeTypography: Typography = NeoBrutalistTypography
