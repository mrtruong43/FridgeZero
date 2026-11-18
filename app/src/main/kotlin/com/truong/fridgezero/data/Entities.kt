package com.truong.fridgezero.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CompartmentKind {
  DOOR,
  DRAWER
}

enum class ItemStatus {
  ACTIVE,
  USED_UP,
  DISCARDED
}

@Entity(tableName = "fridges")
data class FridgeEntity(
  @PrimaryKey val id: String,
  val name: String,
  val template: String,
  val colorKey: String,
  val handleStyle: String
)

@Entity(tableName = "compartments")
data class CompartmentEntity(
  @PrimaryKey val id: String,
  val fridgeId: String,
  val name: String,
  val x: Float,
  val y: Float,
  val w: Float,
  val h: Float,
  val kind: CompartmentKind,
  val shelves: Int,
  val sort: Int
)

@Entity(tableName = "food_items")
data class FoodItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val compartmentId: String,
  val name: String,
  val iconKey: String,
  val group: String,
  val quantity: Int = 1,
  val unit: String? = null,
  val addedAtEpochDay: Long,
  val expiresAtEpochDay: Long,
  val note: String? = null,
  val price: Long? = null,
  val isOpened: Boolean = false,
  val status: ItemStatus = ItemStatus.ACTIVE,
  val source: String = "manual"
)
