package com.truong.fridgezero.data

import com.truong.fridgezero.ui.icons.EmojiIconRepository
import java.time.LocalDate

object Seed {

  const val DEFAULT_FRIDGE_ID = "default_fridge"

  val defaultFridge = FridgeEntity(
    id = DEFAULT_FRIDGE_ID,
    name = "Tủ lạnh chính",
    template = "top_freezer",
    colorKey = "inox",
    handleStyle = "recessed"
  )

  fun compartmentsForTemplate(
    template: String,
    fridgeId: String = DEFAULT_FRIDGE_ID
  ): List<CompartmentEntity> = when (template) {
    "single" -> listOf(
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.04f, 0.03f, 0.92f, 0.72f, CompartmentKind.DOOR, 4, 0),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.04f, 0.765f, 0.92f, 0.21f, CompartmentKind.DRAWER, 1, 1)
    )
    "bottom_freezer" -> listOf(
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.04f, 0.02f, 0.92f, 0.48f, CompartmentKind.DOOR, 3, 0),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.04f, 0.513f, 0.92f, 0.20f, CompartmentKind.DRAWER, 1, 1),
      CompartmentEntity("freezer", fridgeId, "Ngăn đông", 0.04f, 0.726f, 0.92f, 0.254f, CompartmentKind.DRAWER, 2, 2)
    )
    "side_by_side" -> listOf(
      CompartmentEntity("freezer", fridgeId, "Ngăn đông", 0.04f, 0.02f, 0.41f, 0.96f, CompartmentKind.DOOR, 4, 0),
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.465f, 0.02f, 0.495f, 0.73f, CompartmentKind.DOOR, 3, 1),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.465f, 0.765f, 0.495f, 0.215f, CompartmentKind.DRAWER, 1, 2)
    )
    "french_door" -> listOf(
      CompartmentEntity("fresh_left", fridgeId, "Ngăn mát trái", 0.04f, 0.02f, 0.453f, 0.50f, CompartmentKind.DOOR, 3, 0),
      CompartmentEntity("fresh_right", fridgeId, "Ngăn mát phải", 0.507f, 0.02f, 0.453f, 0.50f, CompartmentKind.DOOR, 3, 1),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.04f, 0.533f, 0.92f, 0.19f, CompartmentKind.DRAWER, 1, 2),
      CompartmentEntity("freezer", fridgeId, "Ngăn đông", 0.04f, 0.736f, 0.92f, 0.244f, CompartmentKind.DRAWER, 2, 3)
    )
    "mini" -> listOf(
      CompartmentEntity("freezer", fridgeId, "Ngăn đá", 0.05f, 0.04f, 0.90f, 0.26f, CompartmentKind.DOOR, 1, 0),
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.05f, 0.32f, 0.90f, 0.64f, CompartmentKind.DOOR, 2, 1)
    )
    else -> listOf( // top_freezer
      CompartmentEntity("freezer", fridgeId, "Ngăn đông", 0.04f, 0.02f, 0.92f, 0.25f, CompartmentKind.DOOR, 2, 0),
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.04f, 0.28f, 0.92f, 0.48f, CompartmentKind.DOOR, 3, 1),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.04f, 0.773f, 0.92f, 0.207f, CompartmentKind.DRAWER, 1, 2)
    )
  }

  val compartments: List<CompartmentEntity>
    get() = compartmentsForTemplate("top_freezer")

  fun foodItems(todayEpochDay: Long = LocalDate.now().toEpochDay()): List<FoodItemEntity> {
    fun item(compartmentId: String, name: String, iconKey: String, daysLeft: Long) =
      FoodItemEntity(
        compartmentId = compartmentId,
        name = name,
        iconKey = iconKey,
        group = EmojiIconRepository.groupFor(iconKey).name,
        quantity = 1,
        addedAtEpochDay = todayEpochDay,
        expiresAtEpochDay = todayEpochDay + daysLeft,
        status = ItemStatus.ACTIVE,
        source = "seed"
      )

    return listOf(
      item("freezer", "Tôm", "shrimp", 40),
      item("freezer", "Kem", "ice_cream", 3),
      item("freezer", "Bánh bao", "dumpling", 25),
      item("freezer", "Cá hồi", "salmon", 1),
      item("fresh", "Sữa", "milk", 4),
      item("fresh", "Trứng", "egg", 12),
      item("fresh", "Phô mai", "cheese", 9),
      item("fresh", "Nước cam", "orange_juice", 2),
      item("fresh", "Thịt gà", "chicken", 1),
      item("fresh", "Sữa chua", "yogurt", 6),
      item("fresh", "Bơ", "butter", 30),
      item("fresh", "Sốt cà", "tomato_sauce", 20),
      item("fresh", "Dưa muối", "pickle", 15),
      item("crisper", "Cà rốt", "carrot", 8),
      item("crisper", "Xà lách", "lettuce", 2),
      item("crisper", "Ớt chuông", "bell_pepper", 4)
    )
  }
}
