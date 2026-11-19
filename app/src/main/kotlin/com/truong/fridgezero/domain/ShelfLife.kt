package com.truong.fridgezero.domain

import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.ui.icons.FoodGroup

enum class StorageZone {
  CHILLED,
  FREEZER,
  DRAWER
}

data class ShelfLifeRule(
  val chilledDays: Int? = null,
  val freezerDays: Int? = null,
  val drawerDays: Int? = null
)

interface ShelfLifeRepository {
  fun suggestDays(
    iconKey: String,
    group: FoodGroup,
    zone: StorageZone,
    isOpened: Boolean = false
  ): Int?
}

/**
 * Standardized shelf-life lookup table based on conservative USDA FoodKeeper / FDA lower-bound
 * cold-storage guidance across chilled (`~2–4°C`), freezer (`≤ -18°C`), and crisper drawer zones.
 */
object ConfigurableShelfLifeRepository : ShelfLifeRepository {

  private const val OPENED_FACTOR = 0.5f

  private val byKey: Map<String, ShelfLifeRule> = mapOf(
    "chicken" to ShelfLifeRule(chilledDays = 2, freezerDays = 270, drawerDays = 2),
    "meat" to ShelfLifeRule(chilledDays = 3, freezerDays = 180, drawerDays = 3),
    "bacon" to ShelfLifeRule(chilledDays = 7, freezerDays = 30, drawerDays = 7),
    "shrimp" to ShelfLifeRule(chilledDays = 2, freezerDays = 120, drawerDays = 2),
    "salmon" to ShelfLifeRule(chilledDays = 2, freezerDays = 60, drawerDays = 2),
    "crab" to ShelfLifeRule(chilledDays = 2, freezerDays = 90, drawerDays = 2),
    "squid" to ShelfLifeRule(chilledDays = 2, freezerDays = 90, drawerDays = 2),
    "lettuce" to ShelfLifeRule(chilledDays = 4, freezerDays = null, drawerDays = 5),
    "broccoli" to ShelfLifeRule(chilledDays = 4, freezerDays = 240, drawerDays = 5),
    "mushroom" to ShelfLifeRule(chilledDays = 4, freezerDays = 180, drawerDays = 5),
    "corn" to ShelfLifeRule(chilledDays = 3, freezerDays = 240, drawerDays = 4),
    "bell_pepper" to ShelfLifeRule(chilledDays = 7, freezerDays = 180, drawerDays = 10),
    "carrot" to ShelfLifeRule(chilledDays = 14, freezerDays = 240, drawerDays = 21),
    "pickle" to ShelfLifeRule(chilledDays = 30, freezerDays = null, drawerDays = 30),
    "strawberry" to ShelfLifeRule(chilledDays = 3, freezerDays = 180, drawerDays = 4),
    "banana" to ShelfLifeRule(chilledDays = 4, freezerDays = 60, drawerDays = 5),
    "mango" to ShelfLifeRule(chilledDays = 5, freezerDays = 180, drawerDays = 6),
    "grape" to ShelfLifeRule(chilledDays = 7, freezerDays = 180, drawerDays = 10),
    "watermelon" to ShelfLifeRule(chilledDays = 7, freezerDays = null, drawerDays = 10),
    "apple" to ShelfLifeRule(chilledDays = 21, freezerDays = 180, drawerDays = 28),
    "milk" to ShelfLifeRule(chilledDays = 7, freezerDays = 90, drawerDays = 7),
    "yogurt" to ShelfLifeRule(chilledDays = 14, freezerDays = 60, drawerDays = 14),
    "egg" to ShelfLifeRule(chilledDays = 21, freezerDays = null, drawerDays = 21),
    "cheese" to ShelfLifeRule(chilledDays = 21, freezerDays = 180, drawerDays = 21),
    "butter" to ShelfLifeRule(chilledDays = 30, freezerDays = 180, drawerDays = 30),
    "rice" to ShelfLifeRule(chilledDays = 3, freezerDays = 30, drawerDays = 3),
    "bread" to ShelfLifeRule(chilledDays = 5, freezerDays = 90, drawerDays = 5),
    "tomato_sauce" to ShelfLifeRule(chilledDays = 30, freezerDays = 90, drawerDays = 30),
    "orange_juice" to ShelfLifeRule(chilledDays = 7, freezerDays = 180, drawerDays = 7),
    "water" to ShelfLifeRule(chilledDays = 30, freezerDays = null, drawerDays = 30),
    "dumpling" to ShelfLifeRule(chilledDays = 3, freezerDays = 60, drawerDays = 3),
    "ice_cream" to ShelfLifeRule(chilledDays = 1, freezerDays = 60, drawerDays = 1)
  )

  private val byGroup: Map<FoodGroup, ShelfLifeRule> = mapOf(
    FoodGroup.THIT to ShelfLifeRule(chilledDays = 3, freezerDays = 180, drawerDays = 3),
    FoodGroup.CA_HAI_SAN to ShelfLifeRule(chilledDays = 2, freezerDays = 90, drawerDays = 2),
    FoodGroup.RAU to ShelfLifeRule(chilledDays = 4, freezerDays = 180, drawerDays = 5),
    FoodGroup.TRAI_CAY to ShelfLifeRule(chilledDays = 5, freezerDays = 180, drawerDays = 7),
    FoodGroup.SUA_TRUNG to ShelfLifeRule(chilledDays = 7, freezerDays = 60, drawerDays = 7),
    FoodGroup.DO_KHO to ShelfLifeRule(chilledDays = 7, freezerDays = 90, drawerDays = 7),
    FoodGroup.DO_UONG to ShelfLifeRule(chilledDays = 7, freezerDays = 90, drawerDays = 7),
    FoodGroup.AN_SAN to ShelfLifeRule(chilledDays = 3, freezerDays = 60, drawerDays = 3),
    FoodGroup.KHAC to ShelfLifeRule(chilledDays = 4, freezerDays = 60, drawerDays = 4)
  )

  override fun suggestDays(
    iconKey: String,
    group: FoodGroup,
    zone: StorageZone,
    isOpened: Boolean
  ): Int? {
    val rule = byKey[iconKey] ?: byGroup[group] ?: return null
    val baseDays = when (zone) {
      StorageZone.CHILLED -> rule.chilledDays
      StorageZone.FREEZER -> rule.freezerDays
      StorageZone.DRAWER -> rule.drawerDays ?: rule.chilledDays
    } ?: return null

    return if (isOpened) {
      (baseDays * OPENED_FACTOR).toInt().coerceAtLeast(1)
    } else {
      baseDays
    }
  }
}

fun zoneForCompartment(
  compartmentId: String,
  compartmentName: String = "",
  kind: CompartmentKind = CompartmentKind.DOOR
): StorageZone {
  val text = "$compartmentId $compartmentName".lowercase()
  return when {
    text.contains("freezer") || text.contains("đông") || text.contains("đá") -> StorageZone.FREEZER
    kind == CompartmentKind.DRAWER || text.contains("crisper") || text.contains("rau") -> StorageZone.DRAWER
    else -> StorageZone.CHILLED
  }
}

fun zoneForCompartment(compartment: CompartmentEntity): StorageZone =
  zoneForCompartment(compartment.id, compartment.name, compartment.kind)

fun zoneForCompartment(compartment: CompartmentUi): StorageZone =
  zoneForCompartment(compartment.id, compartment.name, compartment.kind)
