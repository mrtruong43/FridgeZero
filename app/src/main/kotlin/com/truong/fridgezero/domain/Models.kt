package com.truong.fridgezero.domain

import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.FridgeEntity
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DD_MM_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

data class NRect(val x: Float, val y: Float, val w: Float, val h: Float) {
  val cx: Float get() = x + w / 2f
  val cy: Float get() = y + h / 2f
}

data class DisplayItem(
  val primaryId: Long,
  val itemIds: List<Long> = listOf(primaryId),
  val compartmentId: String = "",
  val name: String,
  val iconKey: String,
  val group: FoodGroup = EmojiIconRepository.groupFor(iconKey),
  val quantity: Int,
  val unit: String? = null,
  val minDaysLeft: Int,
  val isOpened: Boolean = false,
  val earliestExpiryEpochDay: Long = 0L,
  val earliestAddedEpochDay: Long = 0L
) {
  val freshness: Freshness get() = freshnessOf(minDaysLeft)
}

fun DisplayItem.formattedExpiryDate(todayEpochDay: Long = LocalDate.now().toEpochDay()): String {
  val epochDay = if (earliestExpiryEpochDay > 0L) earliestExpiryEpochDay else todayEpochDay + minDaysLeft.toLong()
  return LocalDate.ofEpochDay(epochDay).format(DD_MM_FORMATTER)
}

fun DisplayItem.daysSinceAdded(todayEpochDay: Long = LocalDate.now().toEpochDay()): Int {
  if (earliestAddedEpochDay <= 0L) return 0
  return (todayEpochDay - earliestAddedEpochDay).toInt().coerceAtLeast(0)
}

fun List<FoodItemEntity>.toDisplay(todayEpochDay: Long): List<DisplayItem> =
  groupBy { it.name.trim().lowercase() }
    .values
    .map { xs ->
      val earliest = xs.minBy { it.expiresAtEpochDay }
      DisplayItem(
        primaryId = earliest.id,
        itemIds = xs.map { it.id },
        compartmentId = earliest.compartmentId,
        name = earliest.name,
        iconKey = earliest.iconKey,
        group = EmojiIconRepository.groupFor(earliest.iconKey),
        quantity = xs.sumOf { it.quantity },
        unit = earliest.unit,
        minDaysLeft = xs.minOf { (it.expiresAtEpochDay - todayEpochDay).toInt() },
        isOpened = xs.any { it.isOpened },
        earliestExpiryEpochDay = earliest.expiresAtEpochDay,
        earliestAddedEpochDay = xs.minOf { it.addedAtEpochDay }
      )
    }
    .sortedWith(compareBy({ it.freshness.ordinal * -1 }, { it.minDaysLeft }))

data class CompartmentUi(
  val id: String,
  val name: String,
  val rect: NRect,
  val kind: CompartmentKind,
  val shelves: Int = 2,
  val items: List<DisplayItem>,
  val worstFreshness: Freshness,
  val urgentCount: Int
)

data class FridgeUiState(
  val activeFridge: FridgeEntity? = null,
  val compartments: List<CompartmentUi> = emptyList(),
  val recentItemKeys: List<String> = emptyList(),
  val playWelcomeSpin: Boolean = false
)
