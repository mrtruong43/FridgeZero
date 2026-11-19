package com.truong.fridgezero.domain

import androidx.compose.ui.graphics.Color

enum class Freshness {
  OK,
  SOON,
  BAD;

  fun color(): Color = when (this) {
    OK -> Color(0xFFA3E635)
    SOON -> Color(0xFFFACC15)
    BAD -> Color(0xFFFB7185)
  }
}

fun freshnessOf(daysLeft: Int): Freshness = when {
  daysLeft <= 0 -> Freshness.BAD
  daysLeft == 1 -> Freshness.SOON
  else -> Freshness.OK
}
