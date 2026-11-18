package com.truong.fridgezero.ui.icons

import androidx.annotation.DrawableRes

sealed interface FoodIcon {
  data class Emoji(val glyph: String) : FoodIcon
  data class Vector(@param:DrawableRes val res: Int) : FoodIcon
}

enum class FoodGroup(val labelVi: String, val fallbackEmoji: String) {
  RAU("Rau", "🥬"),
  THIT("Thịt", "🥩"),
  CA_HAI_SAN("Cá & Hải sản", "🐟"),
  SUA_TRUNG("Sữa & Trứng", "🥛"),
  TRAI_CAY("Trái cây", "🍎"),
  DO_KHO("Đồ khô", "🌾"),
  DO_UONG("Đồ uống", "🧃"),
  AN_SAN("Ăn sẵn", "🥟"),
  KHAC("Khác", "🍽️")
}

data class IconEntry(
  val key: String,
  val label: String,
  val group: FoodGroup,
  val glyph: String
)

interface IconRepository {
  fun iconFor(key: String): FoodIcon
  fun groupFor(key: String): FoodGroup
  fun search(query: String): List<IconEntry>
}

object EmojiIconRepository : IconRepository {

  val entries: List<IconEntry> = listOf(
    IconEntry("shrimp", "Tôm", FoodGroup.CA_HAI_SAN, "🦐"),
    IconEntry("salmon", "Cá hồi", FoodGroup.CA_HAI_SAN, "🐟"),
    IconEntry("crab", "Cua", FoodGroup.CA_HAI_SAN, "🦀"),
    IconEntry("squid", "Mực", FoodGroup.CA_HAI_SAN, "🦑"),
    IconEntry("chicken", "Thịt gà", FoodGroup.THIT, "🍗"),
    IconEntry("meat", "Thịt heo / bò", FoodGroup.THIT, "🥩"),
    IconEntry("bacon", "Thịt ba chỉ", FoodGroup.THIT, "🥓"),
    IconEntry("milk", "Sữa", FoodGroup.SUA_TRUNG, "🥛"),
    IconEntry("egg", "Trứng", FoodGroup.SUA_TRUNG, "🥚"),
    IconEntry("cheese", "Phô mai", FoodGroup.SUA_TRUNG, "🧀"),
    IconEntry("yogurt", "Sữa chua", FoodGroup.SUA_TRUNG, "🥣"),
    IconEntry("butter", "Bơ", FoodGroup.SUA_TRUNG, "🧈"),
    IconEntry("orange_juice", "Nước cam", FoodGroup.DO_UONG, "🧃"),
    IconEntry("water", "Nước suối", FoodGroup.DO_UONG, "🥤"),
    IconEntry("ice_cream", "Kem", FoodGroup.AN_SAN, "🍦"),
    IconEntry("dumpling", "Bánh bao", FoodGroup.AN_SAN, "🥟"),
    IconEntry("tomato_sauce", "Sốt cà", FoodGroup.DO_KHO, "🍅"),
    IconEntry("pickle", "Dưa muối", FoodGroup.RAU, "🥒"),
    IconEntry("carrot", "Cà rốt", FoodGroup.RAU, "🥕"),
    IconEntry("lettuce", "Xà lách", FoodGroup.RAU, "🥬"),
    IconEntry("bell_pepper", "Ớt chuông", FoodGroup.RAU, "🫑"),
    IconEntry("broccoli", "Bông cải", FoodGroup.RAU, "🥦"),
    IconEntry("corn", "Bắp ngọt", FoodGroup.RAU, "🌽"),
    IconEntry("mushroom", "Nấm", FoodGroup.RAU, "🍄"),
    IconEntry("apple", "Táo", FoodGroup.TRAI_CAY, "🍎"),
    IconEntry("banana", "Chuối", FoodGroup.TRAI_CAY, "🍌"),
    IconEntry("grape", "Nho", FoodGroup.TRAI_CAY, "🍇"),
    IconEntry("strawberry", "Dâu tây", FoodGroup.TRAI_CAY, "🍓"),
    IconEntry("watermelon", "Dưa hấu", FoodGroup.TRAI_CAY, "🍉"),
    IconEntry("mango", "Xoài", FoodGroup.TRAI_CAY, "🥭"),
    IconEntry("bread", "Bánh mì", FoodGroup.DO_KHO, "🍞"),
    IconEntry("rice", "Cơm / Gạo", FoodGroup.DO_KHO, "🍚")
  )

  private val byKey = entries.associateBy { it.key }

  override fun iconFor(key: String): FoodIcon {
    val entry = byKey[key]
    return FoodIcon.Emoji(entry?.glyph ?: groupFor(key).fallbackEmoji)
  }

  override fun groupFor(key: String): FoodGroup =
    byKey[key]?.group ?: FoodGroup.KHAC

  override fun search(query: String): List<IconEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return entries
    return entries.filter {
      it.label.lowercase().contains(q) ||
        it.key.lowercase().contains(q) ||
        it.group.labelVi.lowercase().contains(q)
    }
  }
}
