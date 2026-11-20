package com.truong.fridgezero.domain

import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.data.Seed
import com.truong.fridgezero.ui.fridge3d.BoundedWebGlCrashTracker
import com.truong.fridgezero.ui.fridge3d.LOW_MEM_THRESHOLD_BYTES
import com.truong.fridgezero.ui.fridge3d.WebGlRecoveryAction
import com.truong.fridgezero.ui.fridge3d.isAllowedFridge3dUrl
import com.truong.fridgezero.ui.fridge3d.resolveWebGlQuality
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodGroup
import com.truong.fridgezero.ui.theme.DotGridInk
import com.truong.fridgezero.ui.theme.DotGridSpec
import com.truong.fridgezero.ui.theme.PaperCream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LodAndFreshnessTest {

  @Test
  fun freshnessThresholdsMatchSpec() {
    assertEquals(Freshness.BAD, freshnessOf(-1))
    assertEquals(Freshness.BAD, freshnessOf(0))
    assertEquals(Freshness.SOON, freshnessOf(1))
    assertEquals(Freshness.OK, freshnessOf(2))
    assertEquals(Freshness.OK, freshnessOf(3))
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
    val today = java.time.LocalDate.of(2026, 11, 20).toEpochDay()
    val items = listOf(
      FoodItemEntity(
        id = 1L,
        compartmentId = "fresh",
        name = "Sữa tươi",
        iconKey = "milk",
        group = "SUA_TRUNG",
        quantity = 1,
        addedAtEpochDay = today - 3,
        expiresAtEpochDay = today + 5
      ),
      FoodItemEntity(
        id = 2L,
        compartmentId = "fresh",
        name = "sữa tươi",
        iconKey = "milk",
        group = "SUA_TRUNG",
        quantity = 2,
        addedAtEpochDay = today - 1,
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
    assertEquals(today + 1, display[0].earliestExpiryEpochDay)
    assertEquals(today, display[0].earliestAddedEpochDay)
    assertEquals("21/11", display[0].formattedExpiryDate(today))
    assertEquals(0, display[0].daysSinceAdded(today))

    assertEquals(2L, display[1].primaryId)
    assertEquals(listOf(1L, 2L), display[1].itemIds)
    assertEquals(3, display[1].quantity)
    assertEquals(2, display[1].minDaysLeft)
    assertEquals(today + 2, display[1].earliestExpiryEpochDay)
    assertEquals(today - 3, display[1].earliestAddedEpochDay)
    assertEquals("22/11", display[1].formattedExpiryDate(today))
    assertEquals(3, display[1].daysSinceAdded(today))
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

  @Test
  fun shelfLifeItemRuleOverridesGroupFallback() {
    assertEquals(7, ConfigurableShelfLifeRepository.suggestDays("milk", FoodGroup.SUA_TRUNG, StorageZone.CHILLED))
    assertEquals(21, ConfigurableShelfLifeRepository.suggestDays("egg", FoodGroup.SUA_TRUNG, StorageZone.CHILLED))
    EmojiIconRepository.entries.forEach { entry ->
      assertNotNull(
        "Missing chilled shelf-life rule for ${entry.key}",
        ConfigurableShelfLifeRepository.suggestDays(entry.key, entry.group, StorageZone.CHILLED)
      )
    }
  }

  @Test
  fun shelfLifeZoneSwitchingUsesChilledFreezerAndDrawerRules() {
    assertEquals(2, ConfigurableShelfLifeRepository.suggestDays("chicken", FoodGroup.THIT, StorageZone.CHILLED))
    assertEquals(270, ConfigurableShelfLifeRepository.suggestDays("chicken", FoodGroup.THIT, StorageZone.FREEZER))
    assertEquals(14, ConfigurableShelfLifeRepository.suggestDays("carrot", FoodGroup.RAU, StorageZone.CHILLED))
    assertEquals(21, ConfigurableShelfLifeRepository.suggestDays("carrot", FoodGroup.RAU, StorageZone.DRAWER))
  }

  @Test
  fun shelfLifeFallsBackToGroupForUnknownItemKeys() {
    assertEquals(3, ConfigurableShelfLifeRepository.suggestDays("custom_item", FoodGroup.THIT, StorageZone.CHILLED))
    assertEquals(3, ConfigurableShelfLifeRepository.suggestDays("custom_item", FoodGroup.AN_SAN, StorageZone.CHILLED))
  }

  @Test
  fun culinaryCatalogCoversAll32IconKeysAndMeetsRecipeMetadataSpec() {
    val recipes = CulinaryGuideRepository.recipes
    assertEquals(48, recipes.size)
    assertEquals(48, recipes.map { it.id }.toSet().size)

    val bannedArtificialIds = setOf(
      "salad_ca_hoi_dua_muoi",
      "banh_bao_chien_sot_ca_dua_muoi",
      "kem_sua_chua_cam_tuoi"
    )
    assertTrue(recipes.none { it.id in bannedArtificialIds })

    val coveredKeys = recipes
      .flatMap { it.fridgeIngredients }
      .flatMap { it.iconKeys }
      .toSet()
    val allIconKeys = EmojiIconRepository.entries.map { it.key }.toSet()
    assertTrue("Missing icon keys: ${allIconKeys - coveredKeys}", coveredKeys.containsAll(allIconKeys))

    recipes.forEach { recipe ->
      assertTrue("${recipe.id} must have prepMinutes > 0", recipe.prepMinutes > 0)
      assertTrue("${recipe.id} must have cookMinutes > 0", recipe.cookMinutes > 0)
      assertTrue("${recipe.id} must declare servingsLabel", recipe.servingsLabel.isNotBlank())
      assertTrue("${recipe.id} must have 1..4 fridge ingredients", recipe.fridgeIngredients.size in 1..4)
      assertTrue("${recipe.id} must declare measured seasonings", recipe.seasonings.isNotEmpty())
      recipe.seasonings.forEach { spec ->
        assertTrue("${recipe.id} seasoning name blank", spec.name.isNotBlank())
        assertTrue("${recipe.id} seasoning amount blank", spec.amount.isNotBlank())
      }
      assertTrue("${recipe.id} must have 5..6 steps", recipe.steps.size in 5..6)
      assertTrue("${recipe.id} must cite sourceName", recipe.sourceName.isNotBlank())
      assertTrue("${recipe.id} must cite https sourceUrl", recipe.sourceUrl.startsWith("https://"))
      val hostAndPath = recipe.sourceUrl.removePrefix("https://")
      assertTrue(
        "${recipe.id} sourceUrl must target a dish-specific path or query",
        hostAndPath.contains('?') || hostAndPath.trimEnd('/').contains('/')
      )
      assertTrue(
        "${recipe.id} must belong to at least one dish category",
        recipe.categories.any { it != CulinaryFilter.ALL }
      )
      assertEquals(
        "${recipe.id} difficulty should default from cookMinutes",
        if (recipe.cookMinutes <= 15) RecipeDifficulty.EASY else RecipeDifficulty.MEDIUM,
        recipe.difficulty
      )
      recipe.fridgeIngredients.forEach { ing ->
        assertTrue("${recipe.id}/${ing.name} amountText blank", ing.amountText.isNotBlank())
        assertTrue("${recipe.id}/${ing.name} unitsToConsume < 1", ing.unitsToConsume >= 1)
      }
    }

    val seedDisplay = Seed.foodItems(20_000L).toDisplay(20_000L)
    val allFeed = CulinaryGuideEngine.recommend(seedDisplay, CulinaryFilter.ALL)
    assertTrue("Expected at least 8 seed-ready recipes", allFeed.count { it.isReadyToCook } >= 8)
    assertTrue(allFeed.any { it.hasUrgentMatch })
    val topOverall = allFeed.first()
    assertTrue(topOverall.isReadyToCook)
    assertTrue(topOverall.hasUrgentMatch)
    assertTrue(topOverall.matchedIngredients.all { it.tier == MatchTier.EXACT_ICON })

    val topSixCounts = allFeed
      .take(CulinaryGuideEngine.DEFAULT_FEED_DIVERSITY_WINDOW)
      .mapNotNull { it.recipe.fridgeIngredients.firstOrNull()?.iconKeys?.firstOrNull() }
      .groupingBy { it }
      .eachCount()
    assertTrue(
      "Top 6 ALL feed exceeded diversity cap: $topSixCounts",
      topSixCounts.values.all { it <= CulinaryGuideEngine.DEFAULT_FEED_MAX_PER_PRIMARY_KEY }
    )
  }

  @Test
  fun culinaryEngineThreeLayerMatchingPicksEarliestExpiryAndAvoidsDoubleAssignment() {
    val recipe = CulinaryRecipe(
      id = "test_3layer",
      title = "Test 3-layer recipe",
      emoji = "🍳",
      summary = "Test",
      prepMinutes = 8,
      cookMinutes = 15,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(
        RecipeIngredient("Thịt gà", listOf("chicken"), listOf("thịt gà", "gà"), FoodGroup.THIT, "200g"),
        RecipeIngredient("Ớt chuông", listOf("bell_pepper"), listOf("ớt chuông"), FoodGroup.RAU, "1 quả"),
        RecipeIngredient("Cà rốt", listOf("carrot"), listOf("cà rốt"), FoodGroup.RAU, "1 củ"),
        RecipeIngredient("Bơ", listOf("butter"), listOf("bơ"), FoodGroup.SUA_TRUNG, "10g")
      ),
      seasonings = listOf(SeasoningSpec("Muối", "1/2 muỗng cà phê")),
      steps = listOf("B1", "B2", "B3", "B4", "B5"),
      sourceName = "VnExpress Ẩm Thực",
      sourceUrl = "https://timkiem.vnexpress.net/?q=ga+xao"
    )
    val fridge = listOf(
      DisplayItem(10L, listOf(10L), "fresh", "Ức gà tươi", "chicken", FoodGroup.THIT, 1, minDaysLeft = 5),
      DisplayItem(11L, listOf(11L), "fresh", "Đùi gà", "chicken", FoodGroup.THIT, 1, minDaysLeft = 1),
      DisplayItem(12L, listOf(12L), "fresh", "Thịt bò", "meat", FoodGroup.THIT, 1, minDaysLeft = 0),
      DisplayItem(13L, listOf(13L), "crisper", "Ot chuong do", "custom_veg", FoodGroup.RAU, 1, minDaysLeft = 2),
      DisplayItem(14L, listOf(14L), "crisper", "Nấm đùi gà", "mushroom", FoodGroup.RAU, 1, minDaysLeft = 3)
    )

    val eval = CulinaryGuideEngine.evaluate(recipe, fridge)
    assertEquals(3, eval.matchedCount)
    assertEquals(1, eval.missingIngredients.size)
    assertEquals("Bơ", eval.missingIngredients.single().name)

    val chickenMatch = eval.matchedIngredients.first { it.ingredient.name == "Thịt gà" }
    assertEquals(MatchTier.EXACT_ICON, chickenMatch.tier)
    assertEquals(11L, chickenMatch.item.primaryId)

    val pepperMatch = eval.matchedIngredients.first { it.ingredient.name == "Ớt chuông" }
    assertEquals(MatchTier.KEYWORD, pepperMatch.tier)
    assertEquals(13L, pepperMatch.item.primaryId)

    val carrotMatch = eval.matchedIngredients.first { it.ingredient.name == "Cà rốt" }
    assertEquals(MatchTier.GROUP_FALLBACK, carrotMatch.tier)
    assertEquals(14L, carrotMatch.item.primaryId)

    // Layer 3 must not steal an exact Layer 1 item belonging to another ingredient in the same recipe.
    val antiStealRecipe = CulinaryRecipe(
      id = "anti_steal",
      title = "Anti steal",
      emoji = "🥗",
      summary = "Test",
      prepMinutes = 5,
      cookMinutes = 10,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(
        RecipeIngredient("Cà rốt", listOf("carrot"), listOf("cà rốt"), FoodGroup.RAU, "1 củ"),
        RecipeIngredient("Xà lách", listOf("lettuce"), listOf("xà lách"), FoodGroup.RAU, "1 bó")
      ),
      seasonings = listOf(SeasoningSpec("Muối", "1/2 muỗng cà phê")),
      steps = listOf("B1", "B2", "B3", "B4", "B5"),
      sourceName = "Cooky",
      sourceUrl = "https://www.cooky.vn/tim-kiem?q=salad"
    )
    val antiStealEval = CulinaryGuideEngine.evaluate(
      antiStealRecipe,
      listOf(
        DisplayItem(20L, listOf(20L), "crisper", "Xà lách", "lettuce", FoodGroup.RAU, 1, minDaysLeft = 2),
        DisplayItem(21L, listOf(21L), "crisper", "Nấm rơm", "mushroom", FoodGroup.RAU, 1, minDaysLeft = 4)
      )
    )
    assertTrue(antiStealEval.isReadyToCook)
    val lettuceRes = antiStealEval.matchedIngredients.first { it.ingredient.name == "Xà lách" }
    assertEquals(MatchTier.EXACT_ICON, lettuceRes.tier)
    assertEquals(20L, lettuceRes.item.primaryId)
    val carrotRes = antiStealEval.matchedIngredients.first { it.ingredient.name == "Cà rốt" }
    assertEquals(MatchTier.GROUP_FALLBACK, carrotRes.tier)
    assertEquals(21L, carrotRes.item.primaryId)

    // Single RAU item must never be assigned to two ingredients in the same recipe.
    val singleRauEval = CulinaryGuideEngine.evaluate(
      antiStealRecipe,
      listOf(DisplayItem(21L, listOf(21L), "crisper", "Nấm rơm", "mushroom", FoodGroup.RAU, 1, minDaysLeft = 4))
    )
    assertEquals(1, singleRauEval.matchedCount)
    assertEquals(1, singleRauEval.missingIngredients.size)
    assertFalse(singleRauEval.isReadyToCook)
  }

  @Test
  fun culinaryEngineExpiryRescueScoringAndFilterRanking() {
    fun makeRecipe(
      id: String,
      cookMinutes: Int,
      categories: Set<CulinaryFilter>,
      vararg keys: Pair<String, FoodGroup>
    ) = CulinaryRecipe(
      id = id,
      title = id,
      emoji = "🍽️",
      summary = id,
      prepMinutes = 8,
      cookMinutes = cookMinutes,
      servingsLabel = "2 người ăn",
      categories = categories,
      fridgeIngredients = keys.map { (k, g) ->
        RecipeIngredient(k, listOf(k), listOf(k), g, "1 phần")
      },
      seasonings = listOf(SeasoningSpec("Muối", "1/2 muỗng cà phê")),
      steps = listOf("B1", "B2", "B3", "B4", "B5"),
      sourceName = "Savoury Days",
      sourceUrl = "https://www.savourydays.com/?s=$id"
    )

    val twoUrgentReady = makeRecipe(
      "two_urgent_ready", 12, setOf(CulinaryFilter.VIET_HOME),
      "salmon" to FoodGroup.CA_HAI_SAN, "lettuce" to FoodGroup.RAU
    ) // 15 + 15 + 8 = 38
    val twoFreshReady = makeRecipe(
      "two_fresh_ready", 25, setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      "milk" to FoodGroup.SUA_TRUNG, "carrot" to FoodGroup.RAU
    ) // 6 + 6 + 8 = 20
    val oneUrgentOneMissing = makeRecipe(
      "one_urgent_missing", 15, setOf(CulinaryFilter.EAT_CLEAN),
      "chicken" to FoodGroup.THIT, "dumpling" to FoodGroup.AN_SAN
    ) // 15 - 3 = 12
    val twoMissing = makeRecipe(
      "two_missing", 20, setOf(CulinaryFilter.VIET_HOME),
      "mango" to FoodGroup.TRAI_CAY, "apple" to FoodGroup.TRAI_CAY
    ) // -3 - 3 = -6

    val fridge = listOf(
      DisplayItem(1L, listOf(1L), "freezer", "Cá hồi", "salmon", FoodGroup.CA_HAI_SAN, 1, minDaysLeft = -1),
      DisplayItem(2L, listOf(2L), "crisper", "Xà lách", "lettuce", FoodGroup.RAU, 1, minDaysLeft = 1),
      DisplayItem(3L, listOf(3L), "fresh", "Thịt gà", "chicken", FoodGroup.THIT, 1, minDaysLeft = 1),
      DisplayItem(4L, listOf(4L), "fresh", "Sữa", "milk", FoodGroup.SUA_TRUNG, 1, minDaysLeft = 2),
      DisplayItem(5L, listOf(5L), "crisper", "Cà rốt", "carrot", FoodGroup.RAU, 1, minDaysLeft = 8)
    )
    val subset = listOf(twoMissing, twoFreshReady, oneUrgentOneMissing, twoUrgentReady)

    val all = CulinaryGuideEngine.recommend(fridge, CulinaryFilter.ALL, subset)
    assertEquals(listOf("two_urgent_ready", "two_fresh_ready", "one_urgent_missing", "two_missing"), all.map { it.recipe.id })
    assertEquals(listOf(38, 20, 12, -6), all.map { it.score })

    assertEquals(listOf("two_urgent_ready", "two_fresh_ready", "two_missing"), CulinaryGuideEngine.recommend(fridge, CulinaryFilter.VIET_HOME, subset).map { it.recipe.id })
    assertEquals(listOf("two_fresh_ready", "one_urgent_missing"), CulinaryGuideEngine.recommend(fridge, CulinaryFilter.EAT_CLEAN, subset).map { it.recipe.id })
  }

  @Test
  fun culinaryEngineDiversityCapLimitsPrimaryIngredientInTopSix() {
    fun makeDish(id: String, primaryKey: String, primaryGroup: FoodGroup, secondaryKey: String? = null) =
      CulinaryRecipe(
        id = id,
        title = id,
        emoji = "🍽️",
        summary = id,
        prepMinutes = 5,
        cookMinutes = 10,
        servingsLabel = "2 người ăn",
        categories = setOf(CulinaryFilter.EAT_CLEAN),
        fridgeIngredients = buildList {
          add(RecipeIngredient(primaryKey, listOf(primaryKey), listOf(primaryKey), primaryGroup, "200g"))
          if (secondaryKey != null) {
            add(RecipeIngredient(secondaryKey, listOf(secondaryKey), listOf(secondaryKey), FoodGroup.RAU, "100g"))
          }
        },
        seasonings = listOf(SeasoningSpec("Muối", "1/2 muỗng cà phê")),
        steps = listOf("B1", "B2", "B3", "B4", "B5"),
        sourceName = "Cooky",
        sourceUrl = "https://www.cooky.vn/tim-kiem?q=$id"
      )

    val subset = listOf(
      makeDish("salmon_1", "salmon", FoodGroup.CA_HAI_SAN, "lettuce"), // 15+6+8=29
      makeDish("salmon_2", "salmon", FoodGroup.CA_HAI_SAN, "lettuce"), // 29
      makeDish("salmon_3", "salmon", FoodGroup.CA_HAI_SAN, "lettuce"), // 29 (deferred past top 6)
      makeDish("salmon_4", "salmon", FoodGroup.CA_HAI_SAN, "lettuce"), // 29 (deferred past top 6)
      makeDish("chicken_1", "chicken", FoodGroup.THIT),                 // 6+8=14
      makeDish("shrimp_1", "shrimp", FoodGroup.CA_HAI_SAN),            // 14
      makeDish("egg_1", "egg", FoodGroup.SUA_TRUNG),                   // 14
      makeDish("meat_1", "meat", FoodGroup.THIT)                       // 14
    )
    val fridge = listOf(
      DisplayItem(1L, listOf(1L), "freezer", "Cá hồi", "salmon", FoodGroup.CA_HAI_SAN, 1, minDaysLeft = 0),
      DisplayItem(2L, listOf(2L), "crisper", "Xà lách", "lettuce", FoodGroup.RAU, 1, minDaysLeft = 3),
      DisplayItem(3L, listOf(3L), "fresh", "Thịt gà", "chicken", FoodGroup.THIT, 1, minDaysLeft = 4),
      DisplayItem(4L, listOf(4L), "freezer", "Tôm", "shrimp", FoodGroup.CA_HAI_SAN, 1, minDaysLeft = 10),
      DisplayItem(5L, listOf(5L), "fresh", "Trứng", "egg", FoodGroup.SUA_TRUNG, 1, minDaysLeft = 12),
      DisplayItem(6L, listOf(6L), "fresh", "Thịt bò", "meat", FoodGroup.THIT, 1, minDaysLeft = 5)
    )

    val allFeed = CulinaryGuideEngine.recommend(fridge, CulinaryFilter.ALL, subset)
    assertEquals(subset.size, allFeed.size)
    val top6Ids = allFeed.take(6).map { it.recipe.id }
    val salmonInTop6 = allFeed.take(6).count { it.recipe.fridgeIngredients.first().iconKeys.first() == "salmon" }
    assertEquals(2, salmonInTop6)
    assertEquals(listOf("salmon_1", "salmon_2", "chicken_1", "egg_1", "meat_1", "shrimp_1"), top6Ids)
    assertEquals(listOf("salmon_3", "salmon_4"), allFeed.drop(6).map { it.recipe.id })

    // Dish category filters keep pure score ranking without cross-feed diversity deferrals.
    val eatCleanOnly = CulinaryGuideEngine.recommend(fridge, CulinaryFilter.EAT_CLEAN, subset)
    assertEquals(
      listOf("salmon_1", "salmon_2", "salmon_3", "salmon_4", "chicken_1", "egg_1", "meat_1", "shrimp_1"),
      eatCleanOnly.map { it.recipe.id }
    )
  }

  @Test
  fun culinaryEnginePantryStaplesNeverBlockReadiness() {
    val recipe = CulinaryRecipe(
      id = "pantry_only_test",
      title = "Trứng chiên nước mắm tỏi",
      emoji = "🥚",
      summary = "Gia vị đo lường sẵn trong bếp không bao giờ chặn trạng thái đủ đồ.",
      prepMinutes = 5,
      cookMinutes = 10,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(
        RecipeIngredient("Trứng gà", listOf("egg"), listOf("trứng"), FoodGroup.SUA_TRUNG, "3 quả")
      ),
      seasonings = listOf(
        SeasoningSpec("Nước mắm ngon", "1,5 muỗng canh"),
        SeasoningSpec("Tỏi băm", "2 tép (1 muỗng cà phê)"),
        SeasoningSpec("Tiêu đen xay", "1/2 muỗng cà phê"),
        SeasoningSpec("Đường", "1 muỗng cà phê"),
        SeasoningSpec("Dầu ăn", "1,5 muỗng canh")
      ),
      steps = listOf("Đánh trứng.", "Phi thơm tỏi.", "Chiên trứng.", "Pha mắm đường.", "Chan sốt và rắc tiêu."),
      sourceName = "VnExpress Ẩm Thực",
      sourceUrl = "https://timkiem.vnexpress.net/?q=trung+chien+nuoc+mam"
    )
    val fridgeWithOnlyEgg = listOf(
      DisplayItem(1L, listOf(1L), "fresh", "Trứng", "egg", FoodGroup.SUA_TRUNG, 2, minDaysLeft = 5)
    )

    val result = CulinaryGuideEngine.evaluate(recipe, fridgeWithOnlyEgg)
    assertTrue(result.isReadyToCook)
    assertTrue(result.missingIngredients.isEmpty())
    assertEquals(1, result.matchedCount)
    assertEquals(14, result.score)
  }

  @Test
  fun culinaryEnginePlanDeductionsDecrementsAndMarksUsedUpEarliestFirst() {
    val today = 20_000L
    val milkEarly = FoodItemEntity(
      id = 101L,
      compartmentId = "fresh",
      name = "Sữa tươi",
      iconKey = "milk",
      group = FoodGroup.SUA_TRUNG.name,
      quantity = 1,
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 1,
      status = ItemStatus.ACTIVE
    )
    val milkLater = FoodItemEntity(
      id = 102L,
      compartmentId = "fresh",
      name = "Sữa tươi",
      iconKey = "milk",
      group = FoodGroup.SUA_TRUNG.name,
      quantity = 3,
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 4,
      status = ItemStatus.ACTIVE
    )
    val chickenSingle = FoodItemEntity(
      id = 103L,
      compartmentId = "fresh",
      name = "Thịt gà",
      iconKey = "chicken",
      group = FoodGroup.THIT.name,
      quantity = 1,
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 1,
      status = ItemStatus.ACTIVE
    )

    val milkDisplay = listOf(milkLater, milkEarly).toDisplay(today).single()
    val chickenDisplay = listOf(chickenSingle).toDisplay(today).single()

    val matched = listOf(
      MatchedIngredient(
        ingredient = RecipeIngredient("Sữa tươi", listOf("milk"), listOf("sữa"), FoodGroup.SUA_TRUNG, "2 hộp", unitsToConsume = 2),
        item = milkDisplay,
        tier = MatchTier.EXACT_ICON
      ),
      MatchedIngredient(
        ingredient = RecipeIngredient("Thịt gà", listOf("chicken"), listOf("gà"), FoodGroup.THIT, "300g", unitsToConsume = 1),
        item = chickenDisplay,
        tier = MatchTier.EXACT_ICON
      )
    )
    val entitiesById = mapOf(
      101L to milkEarly,
      102L to milkLater,
      103L to chickenSingle
    )

    val deductions = CulinaryGuideEngine.planDeductions(matched, entitiesById).associateBy { it.itemId }
    assertEquals(3, deductions.size)
    assertEquals(0, deductions[101L]?.remainingQuantity)
    assertEquals(2, deductions[102L]?.remainingQuantity)
    assertEquals(0, deductions[103L]?.remainingQuantity)
  }

  @Test
  fun fridge3dUrlAllowlistAllowsOnlyLocalAssetsAndAboutBlank() {
    assertTrue(isAllowedFridge3dUrl("file:///android_asset/fridge3d/index.html"))
    assertTrue(isAllowedFridge3dUrl("file:///android_asset/fridge3d/index.html?q=lite"))
    assertTrue(isAllowedFridge3dUrl("file:///android_asset/fridge3d/index.html?q=high"))
    assertTrue(isAllowedFridge3dUrl("file:///android_asset/fridge3d/three.min.js"))
    assertTrue(isAllowedFridge3dUrl("about:blank"))

    assertFalse(isAllowedFridge3dUrl("https://example.com"))
    assertFalse(isAllowedFridge3dUrl("http://localhost:8080/"))
    assertFalse(isAllowedFridge3dUrl("file:///data/local/tmp/exploit.html"))
    assertFalse(isAllowedFridge3dUrl("file:///android_asset/fridge3d/../secret.html"))
    assertFalse(isAllowedFridge3dUrl(""))
    assertFalse(isAllowedFridge3dUrl(null))
  }

  @Test
  fun webGlQualityResolvesLiteForLowRamLowMemOrForcedFlag() {
    assertEquals("high", resolveWebGlQuality(isLowRamDevice = false, totalMemBytes = 8L * 1024 * 1024 * 1024, forceLiteMode = false))
    assertEquals("lite", resolveWebGlQuality(isLowRamDevice = true, totalMemBytes = 8L * 1024 * 1024 * 1024, forceLiteMode = false))
    assertEquals("lite", resolveWebGlQuality(isLowRamDevice = false, totalMemBytes = LOW_MEM_THRESHOLD_BYTES, forceLiteMode = false))
    assertEquals("lite", resolveWebGlQuality(isLowRamDevice = false, totalMemBytes = 8L * 1024 * 1024 * 1024, forceLiteMode = true))
  }

  @Test
  fun boundedWebGlCrashTrackerEnforcesTwoRecoveriesIn60SecondsThenFallback() {
    val tracker = BoundedWebGlCrashTracker(windowMs = 60_000L, maxRecoveries = 2)

    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE_AND_PERSIST, tracker.recordCrash(1_000L))
    assertEquals(1, tracker.recentCrashCount(1_000L))
    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE, tracker.recordCrash(10_000L))
    assertEquals(2, tracker.recentCrashCount(10_000L))
    assertEquals(WebGlRecoveryAction.FALLBACK_TO_2D, tracker.recordCrash(59_000L))

    // Leaving and re-entering screen resets recovery budget.
    tracker.reset()
    assertEquals(0, tracker.recentCrashCount(60_000L))
    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE_AND_PERSIST, tracker.recordCrash(100_000L))
    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE, tracker.recordCrash(120_000L))

    // Old entries (> 60s earlier) expire from sliding window.
    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE, tracker.recordCrash(165_000L))
    assertEquals(2, tracker.recentCrashCount(165_000L))
  }

  @Test
  fun dotGridSpecMatchesDesignTokens() {
    assertEquals("#EAE2D3", DotGridSpec.PAPER_HEX)
    assertEquals("#C4B8A3", DotGridSpec.DOT_HEX)
    assertEquals(18f, DotGridSpec.STEP_DP, 0.0001f)
    assertEquals(1.5f, DotGridSpec.DOT_RADIUS_DP, 0.0001f)
    assertEquals(PaperCream, DotGridSpec.paperColor)
    assertEquals(DotGridInk, DotGridSpec.dotColor)
  }

  @Test
  fun culinaryEngineLeftoverUpdateKeepsActiveMarksOpenedAndClampsExpiryToTwoDays() {
    val today = 20_000L
    val freshMeat = FoodItemEntity(
      id = 201L,
      compartmentId = "fresh",
      name = "Thịt thăn bò",
      iconKey = "meat",
      group = FoodGroup.THIT.name,
      quantity = 300,
      unit = "g",
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 6,
      isOpened = false,
      status = ItemStatus.ACTIVE
    )
    val urgentSalmon = FoodItemEntity(
      id = 202L,
      compartmentId = "fresh",
      name = "Cá hồi",
      iconKey = "salmon",
      group = FoodGroup.CA_HAI_SAN.name,
      quantity = 1,
      unit = "gói",
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 1,
      isOpened = false,
      status = ItemStatus.ACTIVE
    )

    val leftovers = listOf(
      MatchedIngredient(
        ingredient = RecipeIngredient("Thịt bò", listOf("meat"), listOf("thịt bò"), FoodGroup.THIT, "200g"),
        item = listOf(freshMeat).toDisplay(today).single(),
        tier = MatchTier.EXACT_ICON
      ),
      MatchedIngredient(
        ingredient = RecipeIngredient("Cá hồi", listOf("salmon"), listOf("cá hồi"), FoodGroup.CA_HAI_SAN, "250g"),
        item = listOf(urgentSalmon).toDisplay(today).single(),
        tier = MatchTier.EXACT_ICON
      )
    )
    val entitiesById = mapOf(201L to freshMeat, 202L to urgentSalmon)
    val updated = CulinaryGuideEngine.planLeftoverUpdates(leftovers, entitiesById, today).associateBy { it.id }

    assertEquals(2, updated.size)
    val updatedMeat = updated.getValue(201L)
    assertEquals(ItemStatus.ACTIVE, updatedMeat.status)
    assertTrue(updatedMeat.isOpened)
    assertEquals(today + CulinaryGuideEngine.LEFTOVER_MAX_DAYS_LEFT, updatedMeat.expiresAtEpochDay)

    val updatedSalmon = updated.getValue(202L)
    assertEquals(ItemStatus.ACTIVE, updatedSalmon.status)
    assertTrue(updatedSalmon.isOpened)
    // Already expiring sooner than 2 days -> keep tighter existing expiry.
    assertEquals(today + 1, updatedSalmon.expiresAtEpochDay)
  }

  @Test
  fun culinaryEnginePlanDeductionsMarksWeightBasedItemsUsedUpInOneCook() {
    val today = 20_000L
    val beef500g = FoodItemEntity(
      id = 301L,
      compartmentId = "fresh",
      name = "Thịt bò",
      iconKey = "meat",
      group = FoodGroup.THIT.name,
      quantity = 500,
      unit = "g",
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 3,
      status = ItemStatus.ACTIVE
    )
    val eggs = FoodItemEntity(
      id = 302L,
      compartmentId = "fresh",
      name = "Trứng gà",
      iconKey = "egg",
      group = FoodGroup.SUA_TRUNG.name,
      quantity = 4,
      unit = "quả",
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 10,
      status = ItemStatus.ACTIVE
    )

    val matched = listOf(
      MatchedIngredient(
        ingredient = RecipeIngredient("Thịt bò", listOf("meat"), listOf("bò"), FoodGroup.THIT, "250g", unitsToConsume = 1),
        item = listOf(beef500g).toDisplay(today).single(),
        tier = MatchTier.EXACT_ICON
      ),
      MatchedIngredient(
        ingredient = RecipeIngredient("Trứng gà", listOf("egg"), listOf("trứng"), FoodGroup.SUA_TRUNG, "2 quả", unitsToConsume = 1),
        item = listOf(eggs).toDisplay(today).single(),
        tier = MatchTier.EXACT_ICON
      )
    )
    val entitiesById = mapOf(301L to beef500g, 302L to eggs)
    val deductions = CulinaryGuideEngine.planDeductions(matched, entitiesById).associateBy { it.itemId }

    assertEquals(0, deductions[301L]?.remainingQuantity)
    assertEquals(3, deductions[302L]?.remainingQuantity)
  }

  @Test
  fun culinaryCatalogIncludesExpandedStylesAndSupportsKeywordSearch() {
    val seedDisplay = Seed.foodItems(20_000L).toDisplay(20_000L)

    assertTrue(CulinaryGuideEngine.recommend(seedDisplay, CulinaryFilter.ASIAN_FUSION).size >= 6)
    assertTrue(CulinaryGuideEngine.recommend(seedDisplay, CulinaryFilter.WESTERN).size >= 8)
    assertTrue(CulinaryGuideEngine.recommend(seedDisplay, CulinaryFilter.VEGETARIAN).size >= 8)
    assertTrue(CulinaryGuideEngine.recommend(seedDisplay, CulinaryFilter.BREAKFAST_DRINK).size >= 8)

    // Accent-insensitive keyword search across dish title and ingredients
    val salmonResults = CulinaryGuideEngine.recommend(seedDisplay, CulinaryFilter.ALL, query = "ca hoi")
    assertTrue(salmonResults.isNotEmpty())
    assertTrue(salmonResults.all {
      it.recipe.title.contains("cá hồi", ignoreCase = true) ||
        it.recipe.summary.contains("cá hồi", ignoreCase = true)
    })

    // Combined category + keyword search
    val japaneseEgg = CulinaryGuideEngine.recommend(
      fridgeItems = seedDisplay,
      filter = CulinaryFilter.ASIAN_FUSION,
      query = "tamagoyaki"
    )
    assertEquals(listOf("trung_cuon_nhat_tamagoyaki"), japaneseEgg.map { it.recipe.id })
  }

  @Test
  fun cookingStepAnalyzerCoversAllRecipesTimersAndMentionedIngredients() {
    val allRecipes = CulinaryGuideRepository.recipes
    assertEquals(48, allRecipes.size)

    val analyzedAll = allRecipes.flatMap { CookingStepAnalyzer.analyzeRecipe(it) }
    assertEquals(240, analyzedAll.size)

    val coveredActions = analyzedAll.map { it.action }.toSet()
    assertEquals(CookingAction.entries.toSet(), coveredActions)

    val firstTimer = CookingStepAnalyzer.extractTimer("Áp chảo 4 phút cho vàng giòn.")
    assertEquals(240, firstTimer?.totalSeconds)
    assertEquals("4 phút", firstTimer?.displayLabel)

    val rangeTimer = CookingStepAnalyzer.extractTimer("Đảo đều khoảng 3–4 phút trên lửa vừa.")
    assertEquals(240, rangeTimer?.totalSeconds)
    assertEquals("4 phút", rangeTimer?.displayLabel)

    val decimalTimer = CookingStepAnalyzer.extractTimer("Hấp cách thủy 2,5 phút nữa.")
    assertEquals(150, decimalTimer?.totalSeconds)
    assertEquals("2,5 phút", decimalTimer?.displayLabel)

    val secondTimer = CookingStepAnalyzer.extractTimer("Chần nhanh 30 giây rồi vớt ra.")
    assertEquals(30, secondTimer?.totalSeconds)
    assertEquals("30 giây", secondTimer?.displayLabel)

    val hourTimer = CookingStepAnalyzer.extractTimer("Ủ lạnh ngăn mát 1 giờ trước khi dùng.")
    assertEquals(3600, hourTimer?.totalSeconds)
    assertEquals("1 giờ", hourTimer?.displayLabel)

    assertNull(CookingStepAnalyzer.extractTimer("Rắc tiêu xay và thưởng thức ngay."))

    assertEquals("00:00", CookingStepAnalyzer.formatCountdown(0))
    assertEquals("00:00", CookingStepAnalyzer.formatCountdown(-5))
    assertEquals("02:30", CookingStepAnalyzer.formatCountdown(150))
    assertEquals("1:01:05", CookingStepAnalyzer.formatCountdown(3665))

    val sampleRecipe = CulinaryRecipe(
      id = "step_mention_test",
      title = "Bò áp chảo bơ tỏi",
      emoji = "🥩",
      summary = "Test",
      prepMinutes = 5,
      cookMinutes = 10,
      categories = setOf(CulinaryFilter.WESTERN),
      fridgeIngredients = listOf(
        RecipeIngredient("Thịt thăn bò", listOf("meat"), listOf("thịt bò", "bò"), FoodGroup.THIT, "200g"),
        RecipeIngredient("Bơ lạt", listOf("butter"), listOf("bơ lạt", "bơ"), FoodGroup.SUA_TRUNG, "15g"),
        RecipeIngredient("Cà rốt", listOf("carrot"), listOf("cà rốt"), FoodGroup.RAU, "50g")
      ),
      seasonings = listOf(SeasoningSpec("Muối", "1/2 muỗng cà phê")),
      steps = listOf(
        "Thấm khô thịt bò và cắt cà rốt lát mỏng.",
        "Đun chảy bo lat trên chảo nóng rồi áp chảo thit bo."
      ),
      sourceName = "Savoury Days",
      sourceUrl = "https://www.savourydays.com/"
    )
    val mentionedStep1 = CookingStepAnalyzer.extractMentionedIngredients(sampleRecipe.steps[0], sampleRecipe)
    assertEquals(listOf("Thịt thăn bò", "Cà rốt"), mentionedStep1.map { it.name })

    val mentionedStep2 = CookingStepAnalyzer.extractMentionedIngredients(sampleRecipe.steps[1], sampleRecipe)
    assertEquals(listOf("Bơ lạt", "Thịt thăn bò"), mentionedStep2.map { it.name })
  }
}
