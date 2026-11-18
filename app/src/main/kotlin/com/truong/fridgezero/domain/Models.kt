package com.truong.fridgezero.domain

import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.FridgeEntity
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodGroup

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
  val isOpened: Boolean = false
) {
  val freshness: Freshness get() = freshnessOf(minDaysLeft)
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
        isOpened = xs.any { it.isOpened }
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
