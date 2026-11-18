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
  val drawerDays: Int? = null,
  val openedFactor: Float = 0.5f
)

interface ShelfLifeRepository {
  fun suggestDays(
    iconKey: String,
    group: FoodGroup,
    zone: StorageZone,
    isOpened: Boolean
  ): Int?
}

/**
 * Configurable shelf-life lookup table.
 * Intentionally empty by default per spec.md §0 & §6: do NOT invent unverified
 * storage day numbers. Add verified Vietnamese food storage rules here when approved;
 * missing entries return null so the user enters or picks the expiry date directly.
 */
object ConfigurableShelfLifeRepository : ShelfLifeRepository {

  private val byKey: Map<String, ShelfLifeRule> = emptyMap()
  private val byGroup: Map<FoodGroup, ShelfLifeRule> = emptyMap()

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
      (baseDays * rule.openedFactor).toInt().coerceAtLeast(1)
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
