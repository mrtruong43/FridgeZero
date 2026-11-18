package com.truong.fridgezero.domain

import androidx.compose.ui.graphics.Color

enum class Freshness {
  OK,
  WARN,
  BAD;

  fun color(): Color = when (this) {
    OK -> Color(0xFFA3E635)
    WARN -> Color(0xFFFACC15)
    BAD -> Color(0xFFFB7185)
  }
}

fun freshnessOf(daysLeft: Int): Freshness = when {
  daysLeft <= 1 -> Freshness.BAD
  daysLeft <= 3 -> Freshness.WARN
  else -> Freshness.OK
}
