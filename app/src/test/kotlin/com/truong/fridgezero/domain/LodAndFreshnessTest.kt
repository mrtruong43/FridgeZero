package com.truong.fridgezero.domain

import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.Seed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LodAndFreshnessTest {

  @Test
  fun freshnessThresholdsMatchSpec() {
    assertEquals(Freshness.BAD, freshnessOf(0))
    assertEquals(Freshness.BAD, freshnessOf(1))
    assertEquals(Freshness.WARN, freshnessOf(2))
    assertEquals(Freshness.WARN, freshnessOf(3))
    assertEquals(Freshness.OK, freshnessOf(4))
  }

  @Test
  fun seedItemsMatchExpectedCounts() {
    val today = 20_000L
    val items = Seed.foodItems(today)
    assertEquals(4, items.count { it.compartmentId == "freezer" })
    assertEquals(9, items.count { it.compartmentId == "fresh" })
    assertEquals(3, items.count { it.compartmentId == "crisper" })
  }

  @Test
  fun toDisplayAggregatesDuplicatesTracksAllIdsAndSortsByExpiry() {
    val today = 20_000L
    val items = listOf(
      FoodItemEntity(
        id = 1L,
        compartmentId = "fresh",
        name = "Sữa tươi",
        iconKey = "milk",
        group = "SUA_TRUNG",
        quantity = 1,
        addedAtEpochDay = today,
        expiresAtEpochDay = today + 5
      ),
      FoodItemEntity(
        id = 2L,
        compartmentId = "fresh",
        name = "sữa tươi",
        iconKey = "milk",
        group = "SUA_TRUNG",
        quantity = 2,
        addedAtEpochDay = today,
        expiresAtEpochDay = today + 2
      ),
      FoodItemEntity(
        id = 3L,
        compartmentId = "fresh",
        name = "Thịt gà",
        iconKey = "chicken",
        group = "THIT",
        quantity = 1,
        addedAtEpochDay = today,
        expiresAtEpochDay = today + 1
      )
    )
    val display = items.toDisplay(today)
    assertEquals(2, display.size)
    assertEquals("Thịt gà", display[0].name)
    assertEquals(1, display[0].minDaysLeft)
    assertEquals(listOf(3L), display[0].itemIds)

    assertEquals(2L, display[1].primaryId)
    assertEquals(listOf(1L, 2L), display[1].itemIds)
    assertEquals(3, display[1].quantity)
    assertEquals(2, display[1].minDaysLeft)
  }

  @Test
  fun zoneDetectionSupportsCustomVietnameseCompartmentNamesAndKinds() {
    val customFreezer = CompartmentEntity("c_abc", "f1", "Ngăn đông mềm", 0f, 0f, 1f, 0.3f, CompartmentKind.DOOR, 1, 0)
    val customIce = CompartmentEntity("c_xyz", "f1", "Ngăn đá nhỏ", 0f, 0f, 1f, 0.2f, CompartmentKind.DOOR, 1, 1)
    val customDrawer = CompartmentEntity("c_123", "f1", "Ngăn kéo phụ", 0f, 0f, 1f, 0.2f, CompartmentKind.DRAWER, 1, 2)
    val customChilled = CompartmentEntity("c_999", "f1", "Cánh phụ", 0f, 0f, 1f, 0.5f, CompartmentKind.DOOR, 2, 3)

    assertEquals(StorageZone.FREEZER, zoneForCompartment(customFreezer))
    assertEquals(StorageZone.FREEZER, zoneForCompartment(customIce))
    assertEquals(StorageZone.DRAWER, zoneForCompartment(customDrawer))
    assertEquals(StorageZone.CHILLED, zoneForCompartment(customChilled))
  }

  @Test
  fun frenchDoorTemplateHasTwoTopDoorsPlusTwoDrawers() {
    val comps = Seed.compartmentsForTemplate("french_door")
    assertEquals(4, comps.size)
    assertEquals(CompartmentKind.DOOR, comps[0].kind)
    assertEquals(CompartmentKind.DOOR, comps[1].kind)
    assertEquals(0.04f, comps[0].x, 0.001f)
    assertEquals(0.453f, comps[0].w, 0.001f)
    assertEquals(0.507f, comps[1].x, 0.001f)
    assertEquals(0.453f, comps[1].w, 0.001f)
    assertTrue(comps[0].y == 0.02f && comps[1].y == 0.02f)
    assertEquals(CompartmentKind.DRAWER, comps[2].kind)
    assertEquals(CompartmentKind.DRAWER, comps[3].kind)
  }
}
