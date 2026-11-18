package com.truong.fridgezero.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NeoBrutalistColors = lightColorScheme(
  primary = LimeFresh,
  onPrimary = InkBlack,
  secondary = LemonWarn,
  onSecondary = InkBlack,
  tertiary = LilacAccent,
  error = CoralUrgent,
  onError = InkBlack,
  background = PaperCream,
  onBackground = InkBlack,
  surface = SurfaceWhite,
  onSurface = InkBlack,
  surfaceVariant = PaperCream,
  onSurfaceVariant = InkBlack,
  outline = InkBlack
)

@Composable
fun FridgeZeroTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = NeoBrutalistColors,
    typography = NeoBrutalistTypography,
    content = content
  )
}
