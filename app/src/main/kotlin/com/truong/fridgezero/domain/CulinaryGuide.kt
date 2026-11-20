package com.truong.fridgezero.domain

import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.ui.icons.FoodGroup
import java.text.Normalizer
import java.util.Locale

enum class CulinaryFilter(val labelVi: String) {
  ALL("Tất cả"),
  VIET_HOME("Cơm nhà Việt"),
  ASIAN_FUSION("Món Á"),
  WESTERN("Món Âu"),
  EAT_CLEAN("Eat-Clean"),
  VEGETARIAN("Món Chay"),
  BREAKFAST_DRINK("Sáng & Đồ uống")
}

typealias RecipeCategory = CulinaryFilter

enum class RecipeDifficulty {
  EASY,
  MEDIUM
}

enum class MatchTier {
  EXACT_ICON,
  KEYWORD,
  GROUP_FALLBACK
}

data class InventoryDeduction(
  val itemId: Long,
  val remainingQuantity: Int
)

data class SeasoningSpec(
  val name: String,
  val amount: String
)

data class RecipeIngredient(
  val name: String,
  val iconKeys: List<String>,
  val keywords: List<String>,
  val fallbackGroup: FoodGroup,
  val amountText: String,
  val unitsToConsume: Int = 1
)

data class CulinaryRecipe(
  val id: String,
  val title: String,
  val emoji: String,
  val summary: String,
  val prepMinutes: Int,
  val cookMinutes: Int,
  val servingsLabel: String = "2–3 người ăn",
  val categories: Set<CulinaryFilter>,
  val fridgeIngredients: List<RecipeIngredient>,
  val seasonings: List<SeasoningSpec>,
  val steps: List<String>,
  val sourceName: String,
  val sourceUrl: String,
  val difficulty: RecipeDifficulty = if (cookMinutes <= 15) RecipeDifficulty.EASY else RecipeDifficulty.MEDIUM
) {
  val totalMinutes: Int get() = prepMinutes + cookMinutes
}

data class MatchedIngredient(
  val ingredient: RecipeIngredient,
  val item: DisplayItem,
  val tier: MatchTier
) {
  val isUrgent: Boolean get() = item.minDaysLeft <= CulinaryGuideEngine.URGENT_DAYS_THRESHOLD
}

data class RecipeRecommendation(
  val recipe: CulinaryRecipe,
  val matchedIngredients: List<MatchedIngredient>,
  val missingIngredients: List<RecipeIngredient>,
  val score: Int
) {
  val matchedCount: Int get() = matchedIngredients.size
  val totalFridgeIngredients: Int get() = recipe.fridgeIngredients.size
  val isReadyToCook: Boolean get() = recipe.fridgeIngredients.isNotEmpty() && missingIngredients.isEmpty()
  val urgentMatchedCount: Int get() = matchedIngredients.count { it.isUrgent }
  val freshMatchedCount: Int get() = matchedIngredients.count { !it.isUrgent }
  val hasUrgentMatch: Boolean get() = urgentMatchedCount > 0
  val minUrgentDaysLeft: Int? get() = matchedIngredients.minOfOrNull { it.item.minDaysLeft }
}

/**
 * 3-layer ingredient similarity matcher and expiry-rescue ranker:
 * Layer 1: Exact `iconKey` -> Layer 2: Vietnamese name keyword -> Layer 3: `FoodGroup` fallback.
 * On the default `ALL` feed, limits any single primary ingredient to at most 2 cards in the top 6.
 */
object CulinaryGuideEngine {

  const val URGENT_DAYS_THRESHOLD = 1
  const val SCORE_URGENT_MATCH = 15
  const val SCORE_FRESH_MATCH = 6
  const val SCORE_MISSING_PENALTY = -3
  const val SCORE_READY_BONUS = 8
  const val DEFAULT_FEED_DIVERSITY_WINDOW = 6
  const val DEFAULT_FEED_MAX_PER_PRIMARY_KEY = 2
  const val LEFTOVER_MAX_DAYS_LEFT = 2

  private val WEIGHT_UNITS = setOf("g", "kg")
  private val VI_LOCALE = Locale.forLanguageTag("vi-VN")
  private val NON_WORD_REGEX = Regex("[^\\p{L}\\p{Nd}]+")
  private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

  fun recommend(
    fridgeItems: List<DisplayItem>,
    filter: CulinaryFilter = CulinaryFilter.ALL,
    recipes: List<CulinaryRecipe> = CulinaryGuideRepository.recipes,
    query: String = ""
  ): List<RecipeRecommendation> {
    val sorted = recipes
      .map { evaluate(it, fridgeItems) }
      .filter { rec ->
        when (filter) {
          CulinaryFilter.ALL -> true
          CulinaryFilter.VIET_HOME -> CulinaryFilter.VIET_HOME in rec.recipe.categories
          CulinaryFilter.ASIAN_FUSION -> CulinaryFilter.ASIAN_FUSION in rec.recipe.categories
          CulinaryFilter.WESTERN -> CulinaryFilter.WESTERN in rec.recipe.categories
          CulinaryFilter.EAT_CLEAN -> CulinaryFilter.EAT_CLEAN in rec.recipe.categories
          CulinaryFilter.VEGETARIAN -> CulinaryFilter.VEGETARIAN in rec.recipe.categories
          CulinaryFilter.BREAKFAST_DRINK -> CulinaryFilter.BREAKFAST_DRINK in rec.recipe.categories
        }
      }
      .filter { rec -> matchesSearchQuery(rec, query) }
      .sortedWith(
        compareByDescending<RecipeRecommendation> { it.score }
          .thenByDescending { it.isReadyToCook }
          .thenByDescending { it.urgentMatchedCount }
          .thenByDescending { rec -> rec.matchedIngredients.count { it.tier == MatchTier.EXACT_ICON } }
          .thenByDescending { rec ->
            rec.matchedIngredients.count {
              it.tier == MatchTier.EXACT_ICON || it.tier == MatchTier.KEYWORD
            }
          }
          .thenBy { it.recipe.cookMinutes }
          .thenBy { it.recipe.id }
      )
    return if (filter == CulinaryFilter.ALL && query.isBlank()) {
      applyTopWindowDiversityCap(sorted)
    } else {
      sorted
    }
  }

  private fun matchesSearchQuery(rec: RecipeRecommendation, rawQuery: String): Boolean {
    val trimmed = rawQuery.trim()
    if (trimmed.isEmpty()) return true
    val normalizedPhrase = stripVietnameseAccents(trimmed).trim()
    if (normalizedPhrase.isEmpty()) return true
    val tokens = normalizedPhrase.split(NON_WORD_REGEX).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return true

    val recipe = rec.recipe
    val structuredText = buildString {
      append(recipe.title).append(' ')
      recipe.categories.forEach { append(it.labelVi).append(' ') }
      recipe.fridgeIngredients.forEach { ing ->
        append(ing.name).append(' ')
        ing.keywords.forEach { append(it).append(' ') }
      }
      recipe.seasonings.forEach { append(it.name).append(' ') }
    }
    val fullText = buildString {
      append(structuredText).append(' ')
      append(recipe.summary).append(' ')
      append(recipe.sourceName)
    }

    val fullStripped = stripVietnameseAccents(fullText)
    if (fullStripped.contains(" $normalizedPhrase")) return true

    val structuredStripped = stripVietnameseAccents(structuredText)
    return tokens.all { token ->
      if (token.length <= 3) {
        structuredStripped.contains(" $token ")
      } else {
        structuredStripped.contains(" $token")
      }
    }
  }

  internal fun applyTopWindowDiversityCap(
    sorted: List<RecipeRecommendation>,
    windowSize: Int = DEFAULT_FEED_DIVERSITY_WINDOW,
    maxPerPrimaryKey: Int = DEFAULT_FEED_MAX_PER_PRIMARY_KEY
  ): List<RecipeRecommendation> {
    if (sorted.size <= 1 || windowSize <= 0 || maxPerPrimaryKey <= 0) return sorted

    val topSelected = ArrayList<RecipeRecommendation>(minOf(windowSize, sorted.size))
    val deferred = ArrayList<RecipeRecommendation>()
    val countsInWindow = HashMap<String, Int>()
    var nextIndex = 0

    while (nextIndex < sorted.size && topSelected.size < windowSize) {
      val rec = sorted[nextIndex++]
      val primaryKey = rec.recipe.fridgeIngredients
        .firstOrNull()
        ?.iconKeys
        ?.firstOrNull()
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf { it.isNotEmpty() }

      val count = if (primaryKey != null) countsInWindow[primaryKey] ?: 0 else 0
      if (primaryKey == null || count < maxPerPrimaryKey) {
        topSelected.add(rec)
        if (primaryKey != null) countsInWindow[primaryKey] = count + 1
      } else {
        deferred.add(rec)
      }
    }

    while (topSelected.size < windowSize && deferred.isNotEmpty()) {
      topSelected.add(deferred.removeAt(0))
    }

    val result = ArrayList<RecipeRecommendation>(sorted.size)
    result.addAll(topSelected)
    result.addAll(deferred)
    while (nextIndex < sorted.size) {
      result.add(sorted[nextIndex++])
    }
    return result
  }

  fun evaluate(
    recipe: CulinaryRecipe,
    fridgeItems: List<DisplayItem>
  ): RecipeRecommendation {
    // Track consumed candidates by index rather than primaryId so in-memory items with id=0 stay distinct.
    val candidates = fridgeItems
      .filter { it.quantity > 0 }
      .withIndex()
      .sortedWith(compareBy<IndexedValue<DisplayItem>> { it.value.minDaysLeft }.thenBy { it.index })

    val usedIndices = mutableSetOf<Int>()
    val matchedByIngredient = arrayOfNulls<MatchedIngredient>(recipe.fridgeIngredients.size)

    assignTier(
      recipe = recipe,
      candidates = candidates,
      usedIndices = usedIndices,
      matchedByIngredient = matchedByIngredient,
      tier = MatchTier.EXACT_ICON
    ) { ingredient, item ->
      val itemKey = item.iconKey.trim().lowercase(Locale.ROOT)
      ingredient.iconKeys.any { it.trim().lowercase(Locale.ROOT) == itemKey }
    }

    assignTier(
      recipe = recipe,
      candidates = candidates,
      usedIndices = usedIndices,
      matchedByIngredient = matchedByIngredient,
      tier = MatchTier.KEYWORD
    ) { ingredient, item ->
      matchesKeyword(item.name, ingredient.keywords)
    }

    assignTier(
      recipe = recipe,
      candidates = candidates,
      usedIndices = usedIndices,
      matchedByIngredient = matchedByIngredient,
      tier = MatchTier.GROUP_FALLBACK
    ) { ingredient, item ->
      item.group == ingredient.fallbackGroup
    }

    val matchedList = mutableListOf<MatchedIngredient>()
    val missingList = mutableListOf<RecipeIngredient>()
    recipe.fridgeIngredients.forEachIndexed { idx, ingredient ->
      val matched = matchedByIngredient[idx]
      if (matched != null) matchedList.add(matched) else missingList.add(ingredient)
    }

    val urgentCount = matchedList.count { it.isUrgent }
    val freshCount = matchedList.size - urgentCount
    val missingCount = missingList.size
    val isReady = recipe.fridgeIngredients.isNotEmpty() && missingCount == 0
    val score = (urgentCount * SCORE_URGENT_MATCH) +
      (freshCount * SCORE_FRESH_MATCH) +
      (missingCount * SCORE_MISSING_PENALTY) +
      if (isReady) SCORE_READY_BONUS else 0

    return RecipeRecommendation(
      recipe = recipe,
      matchedIngredients = matchedList,
      missingIngredients = missingList,
      score = score
    )
  }

  fun isWeightBasedUnit(unit: String?): Boolean =
    unit?.trim()?.lowercase(Locale.ROOT) in WEIGHT_UNITS

  /**
   * Computes earliest-expiry quantity decrements across active Room rows backing matched items.
   * Returned entries with `remainingQuantity <= 0` should be transitioned to [ItemStatus.USED_UP].
   */
  fun planDeductions(
    matched: List<MatchedIngredient>,
    entitiesById: Map<Long, FoodItemEntity>
  ): List<InventoryDeduction> {
    if (matched.isEmpty() || entitiesById.isEmpty()) return emptyList()
    val originalQty = entitiesById
      .filterValues { it.status == ItemStatus.ACTIVE && it.quantity > 0 }
      .mapValues { it.value.quantity }
    val remainingQty = originalQty.toMutableMap()

    matched.forEach { entry ->
      var need = entry.ingredient.unitsToConsume.coerceAtLeast(1)
      val orderedIds = entry.item.itemIds
        .distinct()
        .filter { (remainingQty[it] ?: 0) > 0 }
        .sortedWith(compareBy<Long> { entitiesById[it]?.expiresAtEpochDay ?: Long.MAX_VALUE }.thenBy { it })

      for (id in orderedIds) {
        if (need <= 0) break
        val available = remainingQty[id] ?: 0
        if (available <= 0) continue
        val effectiveUnit = entitiesById[id]?.unit ?: entry.item.unit
        if (isWeightBasedUnit(effectiveUnit)) {
          remainingQty[id] = 0
          need -= 1
        } else {
          val consumed = minOf(available, need)
          remainingQty[id] = available - consumed
          need -= consumed
        }
      }
    }

    return remainingQty.entries.mapNotNull { (id, remaining) ->
      val initial = originalQty[id] ?: return@mapNotNull null
      if (remaining < initial) InventoryDeduction(itemId = id, remainingQuantity = remaining) else null
    }
  }

  /** Marks an active entity as opened/leftover and clamps expiry to at most 2 days left. */
  fun applyLeftoverUpdate(
    entity: FoodItemEntity,
    todayEpochDay: Long
  ): FoodItemEntity {
    val maxExpiry = todayEpochDay + LEFTOVER_MAX_DAYS_LEFT.toLong()
    val clampedExpiry = if (entity.expiresAtEpochDay > maxExpiry) maxExpiry else entity.expiresAtEpochDay
    return entity.copy(
      isOpened = true,
      expiresAtEpochDay = clampedExpiry,
      status = ItemStatus.ACTIVE
    )
  }

  fun planLeftoverUpdates(
    leftovers: List<MatchedIngredient>,
    entitiesById: Map<Long, FoodItemEntity>,
    todayEpochDay: Long
  ): List<FoodItemEntity> =
    leftovers
      .flatMap { it.item.itemIds }
      .distinct()
      .mapNotNull { id ->
        val entity = entitiesById[id] ?: return@mapNotNull null
        if (entity.status == ItemStatus.ACTIVE) applyLeftoverUpdate(entity, todayEpochDay) else null
      }

  private inline fun assignTier(
    recipe: CulinaryRecipe,
    candidates: List<IndexedValue<DisplayItem>>,
    usedIndices: MutableSet<Int>,
    matchedByIngredient: Array<MatchedIngredient?>,
    tier: MatchTier,
    predicate: (RecipeIngredient, DisplayItem) -> Boolean
  ) {
    recipe.fridgeIngredients.forEachIndexed { idx, ingredient ->
      if (matchedByIngredient[idx] != null) return@forEachIndexed
      val match = candidates.firstOrNull { candidate ->
        candidate.index !in usedIndices && predicate(ingredient, candidate.value)
      } ?: return@forEachIndexed
      usedIndices.add(match.index)
      matchedByIngredient[idx] = MatchedIngredient(ingredient, match.value, tier)
    }
  }

  private fun matchesKeyword(itemName: String, keywords: List<String>): Boolean {
    val normNameAccented = normalizeAccented(itemName)
    if (normNameAccented.isEmpty()) return false
    val normNameStripped = stripVietnameseAccents(itemName)

    return keywords.any { rawKw ->
      val normKwAccented = normalizeAccented(rawKw)
      if (normKwAccented.isEmpty()) return@any false
      if (normNameAccented.contains(normKwAccented)) return@any true

      // Only fall back to unaccented matching for multi-word or >=4-char keywords
      // so short Vietnamese pairs like "bơ" (butter) vs "bò" (beef) never collide.
      val trimmedKw = normKwAccented.trim()
      if (' ' in trimmedKw || trimmedKw.length >= 4) {
        val normKwStripped = stripVietnameseAccents(rawKw)
        normKwStripped.isNotEmpty() && normNameStripped.contains(normKwStripped)
      } else {
        false
      }
    }
  }

  internal fun normalizeAccented(text: String): String {
    val cleaned = text.lowercase(VI_LOCALE).replace(NON_WORD_REGEX, " ").trim()
    return if (cleaned.isEmpty()) "" else " $cleaned "
  }

  internal fun stripVietnameseAccents(text: String): String {
    val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
    val withoutMarks = DIACRITICS_REGEX.replace(nfd, "")
      .replace('đ', 'd')
      .replace('Đ', 'D')
    return normalizeAccented(withoutMarks)
  }
}

/**
 * Curated reference cookbook of 32 authentic Vietnamese home-cooking & Eat-Clean recipes
 * covering all 32 `EmojiIconRepository` icon keys with measured seasonings and dish-specific citations.
 */
object CulinaryGuideRepository {

  private const val SRC_SAVOURY_DAYS = "Savoury Days"
  private const val SRC_COOKY = "Cooky"
  private const val SRC_VNEXPRESS = "VnExpress Ẩm Thực"
  private const val SRC_DMX = "Điện máy XANH Vào Bếp"
  private const val SRC_SERIOUS_EATS = "Serious Eats"

  private fun sp(name: String, amount: String) = SeasoningSpec(name = name, amount = amount)

  private fun ingChicken(amount: String = "300g", name: String = "Thịt gà / Ức gà") = RecipeIngredient(
    name = name,
    iconKeys = listOf("chicken"),
    keywords = listOf("thịt gà", "ức gà", "đùi gà", "cánh gà", "gà"),
    fallbackGroup = FoodGroup.THIT,
    amountText = amount
  )

  private fun ingMeat(amount: String = "250g", name: String = "Thịt thăn bò") = RecipeIngredient(
    name = name,
    iconKeys = listOf("meat"),
    keywords = listOf("thịt bò", "thịt heo", "thịt lợn", "nạc vai", "thăn bò", "thịt bằm", "bò", "heo"),
    fallbackGroup = FoodGroup.THIT,
    amountText = amount
  )

  private fun ingBacon(amount: String = "300g") = RecipeIngredient(
    name = "Thịt ba chỉ",
    iconKeys = listOf("bacon"),
    keywords = listOf("ba chỉ", "ba rọi", "thịt ba chỉ", "bacon"),
    fallbackGroup = FoodGroup.THIT,
    amountText = amount
  )

  private fun ingSalmon(amount: String = "300g") = RecipeIngredient(
    name = "Phi lê cá hồi",
    iconKeys = listOf("salmon"),
    keywords = listOf("cá hồi", "phi lê cá", "salmon"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingShrimp(amount: String = "250g") = RecipeIngredient(
    name = "Tôm tươi",
    iconKeys = listOf("shrimp"),
    keywords = listOf("tôm", "tôm sú", "tôm thẻ", "tôm tươi"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingCrab(amount: String = "200g") = RecipeIngredient(
    name = "Thịt cua tươi",
    iconKeys = listOf("crab"),
    keywords = listOf("thịt cua", "cua", "cua biển", "ghẹ"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingSquid(amount: String = "300g") = RecipeIngredient(
    name = "Mực ống tươi",
    iconKeys = listOf("squid"),
    keywords = listOf("mực", "mực ống", "mực lá", "mực tươi"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingLettuce(amount: String = "150g") = RecipeIngredient(
    name = "Rau xà lách",
    iconKeys = listOf("lettuce"),
    keywords = listOf("xà lách", "rau xà lách", "rau cải", "rau xanh"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingCarrot(amount: String = "1/2 củ (60g)") = RecipeIngredient(
    name = "Cà rốt",
    iconKeys = listOf("carrot"),
    keywords = listOf("cà rốt", "carrot"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingBellPepper(amount: String = "1 quả (150g)") = RecipeIngredient(
    name = "Ớt chuông",
    iconKeys = listOf("bell_pepper"),
    keywords = listOf("ớt chuông", "ớt ngọt", "ớt chuông đỏ"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingBroccoli(amount: String = "1 bông (250g)") = RecipeIngredient(
    name = "Bông cải xanh",
    iconKeys = listOf("broccoli"),
    keywords = listOf("bông cải", "súp lơ", "bông cải xanh"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingCorn(amount: String = "1 trái (150g hạt)") = RecipeIngredient(
    name = "Bắp ngọt",
    iconKeys = listOf("corn"),
    keywords = listOf("bắp ngọt", "ngô ngọt", "bắp mỹ", "ngô", "bắp"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingMushroom(amount: String = "150g") = RecipeIngredient(
    name = "Nấm tươi",
    iconKeys = listOf("mushroom"),
    keywords = listOf("nấm", "nấm kim châm", "nấm đùi gà", "nấm hương", "nấm rơm"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingPickle(amount: String = "150g") = RecipeIngredient(
    name = "Dưa cải muối chua",
    iconKeys = listOf("pickle"),
    keywords = listOf("dưa muối", "dưa cải", "dưa chua", "dưa leo", "dưa chuột"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingEgg(amount: String = "2 quả") = RecipeIngredient(
    name = "Trứng gà",
    iconKeys = listOf("egg"),
    keywords = listOf("trứng", "trứng gà", "trứng vịt"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingMilk(amount: String = "100ml") = RecipeIngredient(
    name = "Sữa tươi không đường",
    iconKeys = listOf("milk"),
    keywords = listOf("sữa tươi", "sữa không đường", "sữa"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingCheese(amount: String = "30g") = RecipeIngredient(
    name = "Phô mai",
    iconKeys = listOf("cheese"),
    keywords = listOf("phô mai", "cheddar", "mozzarella", "cheese"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingYogurt(amount: String = "1 hũ (100g)") = RecipeIngredient(
    name = "Sữa chua không đường",
    iconKeys = listOf("yogurt"),
    keywords = listOf("sữa chua", "sữa chua không đường", "yogurt"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingButter(amount: String = "15g") = RecipeIngredient(
    name = "Bơ lạt",
    iconKeys = listOf("butter"),
    keywords = listOf("bơ lạt", "bơ mặn", "bơ thực vật", "bơ"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingOrangeJuice(amount: String = "120ml") = RecipeIngredient(
    name = "Nước cam tươi",
    iconKeys = listOf("orange_juice"),
    keywords = listOf("nước cam", "cam ép", "nước ép cam"),
    fallbackGroup = FoodGroup.DO_UONG,
    amountText = amount
  )

  private fun ingWater(amount: String = "500ml") = RecipeIngredient(
    name = "Nước suối / Nước lọc",
    iconKeys = listOf("water"),
    keywords = listOf("nước suối", "nước lọc", "nước tinh khiết"),
    fallbackGroup = FoodGroup.DO_UONG,
    amountText = amount
  )

  private fun ingTomatoSauce(amount: String = "60g (4 muỗng canh)") = RecipeIngredient(
    name = "Sốt cà chua",
    iconKeys = listOf("tomato_sauce"),
    keywords = listOf("sốt cà", "sốt cà chua", "tương cà", "cà chua cô đặc"),
    fallbackGroup = FoodGroup.DO_KHO,
    amountText = amount
  )

  private fun ingBread(amount: String = "2 ổ (160g)") = RecipeIngredient(
    name = "Bánh mì",
    iconKeys = listOf("bread"),
    keywords = listOf("bánh mì", "bánh mì gối", "sandwich", "baguette"),
    fallbackGroup = FoodGroup.DO_KHO,
    amountText = amount
  )

  private fun ingRice(amount: String = "350g (2 chén cơm nguội)") = RecipeIngredient(
    name = "Cơm nguội hạt dẻo",
    iconKeys = listOf("rice"),
    keywords = listOf("cơm", "cơm nguội", "gạo", "gạo tẻ"),
    fallbackGroup = FoodGroup.DO_KHO,
    amountText = amount
  )

  private fun ingDumpling(amount: String = "3 cái (270g)") = RecipeIngredient(
    name = "Bánh bao",
    iconKeys = listOf("dumpling"),
    keywords = listOf("bánh bao", "há cảo", "sủi cảo"),
    fallbackGroup = FoodGroup.AN_SAN,
    amountText = amount
  )

  private fun ingIceCream(amount: String = "2 viên (120g)") = RecipeIngredient(
    name = "Kem vani",
    iconKeys = listOf("ice_cream"),
    keywords = listOf("kem", "kem vani", "kem tươi"),
    fallbackGroup = FoodGroup.AN_SAN,
    amountText = amount
  )

  private fun ingApple(amount: String = "1 quả (150g)") = RecipeIngredient(
    name = "Táo tươi",
    iconKeys = listOf("apple"),
    keywords = listOf("táo", "táo đỏ", "táo xanh"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingBanana(amount: String = "1 quả (120g)") = RecipeIngredient(
    name = "Chuối chín",
    iconKeys = listOf("banana"),
    keywords = listOf("chuối", "chuối chín", "chuối cau"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingGrape(amount: String = "100g") = RecipeIngredient(
    name = "Nho tươi không hạt",
    iconKeys = listOf("grape"),
    keywords = listOf("nho", "nho xanh", "nho đỏ", "nho đen"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingStrawberry(amount: String = "100g") = RecipeIngredient(
    name = "Dâu tây tươi",
    iconKeys = listOf("strawberry"),
    keywords = listOf("dâu tây", "dâu", "strawberry"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingWatermelon(amount: String = "350g ruột") = RecipeIngredient(
    name = "Dưa hấu đỏ",
    iconKeys = listOf("watermelon"),
    keywords = listOf("dưa hấu", "ruột dưa hấu"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingMango(amount: String = "1 quả (250g)", name: String = "Xoài") = RecipeIngredient(
    name = name,
    iconKeys = listOf("mango"),
    keywords = listOf("xoài", "xoài xanh", "xoài chín"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  val recipes: List<CulinaryRecipe> = listOf(
    // 1. Cá hồi áp chảo sốt bơ chanh tỏi
    CulinaryRecipe(
      id = "ca_hoi_ap_chao_bo_chanh_toi",
      title = "Cá hồi áp chảo sốt bơ chanh tỏi",
      emoji = "🐟",
      summary = "Phi lê cá hồi da giòn quyện sốt bơ chanh tỏi thơm béo chuẩn nhà hàng, ăn kèm xà lách giòn mát.",
      prepMinutes = 10,
      cookMinutes = 12,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSalmon("300g"), ingButter("20g"), ingLettuce("100g")),
      seasonings = listOf(
        sp("Tỏi đập dập / băm", "3 tép (1 muỗng canh)"),
        sp("Nước cốt chanh vàng", "1,5 muỗng canh"),
        sp("Dầu ô liu", "1 muỗng canh"),
        sp("Muối biển", "1/2 muỗng cà phê"),
        sp("Tiêu đen xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Rửa nhanh cá hồi với nước muối loãng rồi dùng khăn giấy thấm thật khô hai mặt để khi áp chảo không bắn dầu và da giòn đều.",
        "Ướp đều hai mặt cá hồi với 1/2 muỗng cà phê muối biển và 1/3 muỗng cà phê tiêu đen xay, để nghỉ 10 phút cho thấm vị.",
        "Làm nóng chảo chống dính với 1 muỗng canh dầu ô liu trên lửa vừa lớn, đặt mặt da cá úp xuống áp 4 phút rồi nhẹ nhàng lật áp thêm 2 phút.",
        "Hạ lửa nhỏ, cho 20g bơ lạt và 3 tép tỏi vào góc chảo cho thơm rồi nghiêng chảo múc bơ tỏi nóng rưới liên tục lên thân cá khoảng 1 phút.",
        "Gắp cá đặt lên đĩa xà lách đã rửa ráo; tắt bếp, vắt 1,5 muỗng canh nước cốt chanh vào phần bơ tỏi còn lại khuấy hòa quyện rồi chan lên cá."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=ca+hoi+ap+chao+sot+bo+chanh+toi"
    ),

    // 2. Cá hồi áp chảo sốt cam mật ong
    CulinaryRecipe(
      id = "ca_hoi_sot_cam_mat_ong",
      title = "Cá hồi áp chảo sốt cam mật ong",
      emoji = "🍊",
      summary = "Cá hồi chín mềm mọng kết hợp nước sốt cam mật ong bơ lạt chua ngọt thanh tao, cực kỳ bắt vị.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSalmon("300g"), ingOrangeJuice("120ml"), ingButter("15g")),
      seasonings = listOf(
        sp("Mật ong nguyên chất", "1 muỗng canh"),
        sp("Tỏi băm", "2 tép (1 muỗng cà phê)"),
        sp("Dầu ô liu", "1 muỗng canh"),
        sp("Bột bắp pha loãng", "1 muỗng cà phê bột + 1 muỗng nước"),
        sp("Muối", "1/2 muỗng cà phê"),
        sp("Tiêu đen xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Thấm ráo hoàn toàn hai miếng phi lê cá hồi (mỗi miếng khoảng 150g), xoa đều 1/2 muỗng cà phê muối và 1/3 muỗng cà phê tiêu.",
        "Khuấy sẵn hỗn hợp sốt trong chén gồm 120ml nước cam tươi và 1 muỗng canh mật ong cho tan đều.",
        "Đun nóng 1 muỗng canh dầu ô liu trên lửa vừa, áp chảo mặt da cá trước khoảng 3–4 phút tới khi vàng giòn rồi trở mặt áp thêm 2 phút.",
        "Gắp cá hồi ra đĩa ấm; hạ lửa nhỏ, cho 15g bơ lạt và 2 tép tỏi băm vào chảo phi thơm khoảng 30 giây.",
        "Đổ chén nước cam mật ong vào đun sôi lăn tăn, rót từ từ nước bột bắp khuấy đều 1–2 phút tới khi sốt sánh bóng rồi rưới lên cá hồi."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = "https://www.savourydays.com/?s=ca+hoi+sot+cam"
    ),

    // 3. Salad cá hồi áp chảo xà lách & cà rốt
    CulinaryRecipe(
      id = "salad_ca_hoi_ap_chao_rau_cu",
      title = "Salad cá hồi áp chảo xà lách & cà rốt",
      emoji = "🥗",
      summary = "Bữa tối Eat-Clean đủ đạm và chất xơ với cá hồi áp chảo, xà lách giòn và xốt dầu giấm chanh mật ong.",
      prepMinutes = 10,
      cookMinutes = 12,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSalmon("250g"), ingLettuce("150g"), ingCarrot("80g (1/2 củ)")),
      seasonings = listOf(
        sp("Dầu ô liu", "2 muỗng canh"),
        sp("Nước cốt chanh tươi", "1,5 muỗng canh"),
        sp("Mật ong", "1 muỗng cà phê"),
        sp("Tỏi băm nhuyễn", "1 tép"),
        sp("Muối", "1/2 muỗng cà phê"),
        sp("Tiêu đen xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Rửa sạch 150g xà lách ngâm nước muối loãng rồi quay ráo và cắt khúc vừa ăn; gọt vỏ cà rốt bào sợi mỏng.",
        "Thấm khô 250g cá hồi, ướp nhẹ với 1/3 muỗng cà phê muối và 1/3 muỗng cà phê tiêu đen xay trong 5 phút.",
        "Áp chảo cá hồi với 1/2 muỗng canh dầu ô liu ở lửa vừa mỗi mặt 3 phút đến khi cá chín xém vàng, để nghỉ 2 phút rồi tách miếng vừa ăn.",
        "Pha xốt trộn salad trong hũ nhỏ gồm 1,5 muỗng canh dầu ô liu còn lại, 1,5 muỗng canh nước cốt chanh, 1 muỗng cà phê mật ong, tỏi băm và nhúm muối.",
        "Trộn nhẹ xà lách và cà rốt với một nửa lượng xốt ra đĩa sâu lòng, xếp cá hồi lên trên và rưới nốt phần xốt còn lại."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = "https://www.seriouseats.com/search?q=pan+seared+salmon+salad"
    ),

    // 4. Gà kho gừng sả ớt đậm đà
    CulinaryRecipe(
      id = "ga_kho_gung_sa_ot",
      title = "Gà kho gừng sả ớt đậm đà",
      emoji = "🍗",
      summary = "Món mặn đưa cơm kinh điển với thịt gà săn chắc thấm vị nước mắm ngon, sả thơm nồng và gừng ấm.",
      prepMinutes = 15,
      cookMinutes = 25,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingChicken("500g", "Thịt gà chặt miếng")),
      seasonings = listOf(
        sp("Gừng tươi thái sợi", "30g (1 củ nhỏ)"),
        sp("Sả băm & đập dập", "2 cây"),
        sp("Hành tím băm", "2 củ"),
        sp("Tỏi băm", "3 tép"),
        sp("Nước mắm ngon", "2,5 muỗng canh"),
        sp("Đường vàng", "1,5 muỗng canh"),
        sp("Tiêu xay & ớt tươi", "1/2 muỗng cà phê tiêu, 1 trái ớt"),
        sp("Dầu ăn", "1,5 muỗng canh")
      ),
      steps = listOf(
        "Xát thịt gà với chút muối và gừng giã rồi rửa sạch, chặt miếng vừa ăn và để thật ráo nước.",
        "Ướp gà 15 phút với 2,5 muỗng canh nước mắm, 1/2 muỗng canh đường, 1/2 lượng hành tỏi băm, sả băm và 1/2 muỗng cà phê tiêu.",
        "Bắc nồi lên bếp với 1,5 muỗng canh dầu ăn và 1 muỗng canh đường còn lại, thắng lửa nhỏ đến khi đường chuyển màu cánh gián nhạt.",
        "Trút hành tỏi phi thơm rồi cho gà đã ướp vào đảo săn trên lửa lớn 4–5 phút cho da gà săn bóng và thấm màu.",
        "Rắc 30g gừng thái sợi cùng ớt cắt lát, thêm 60ml nước sôi đậy nắp kho lửa nhỏ 15 phút cho nước sốt sánh sệt bám quanh miếng gà."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=ga+kho+gung+sa+ot"
    ),

    // 5. Ức gà xào ớt chuông cà rốt
    CulinaryRecipe(
      id = "uc_ga_xao_ot_chuong_ca_rot",
      title = "Ức gà xào ớt chuông hành tây & cà rốt",
      emoji = "🫑",
      summary = "Ức gà mềm ngọt không bị khô bã, xào nhanh trên lửa lớn cùng ớt chuông và cà rốt giòn tươi.",
      prepMinutes = 10,
      cookMinutes = 12,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken("300g", "Ức gà phi lê"), ingBellPepper("1 quả (150g)"), ingCarrot("1/2 củ (60g)")),
      seasonings = listOf(
        sp("Dầu hào", "1,5 muỗng canh"),
        sp("Nước tương (xì dầu)", "1 muỗng canh"),
        sp("Tỏi băm", "3 tép (1 muỗng canh)"),
        sp("Bột bắp", "1 muỗng cà phê"),
        sp("Dầu ăn", "1,5 muỗng canh"),
        sp("Tiêu đen xay", "1/2 muỗng cà phê")
      ),
      steps = listOf(
        "Ức gà thái lát mỏng vừa ăn rồi ướp 10 phút với 1 muỗng canh nước tương, 1/2 lượng tỏi băm, 1 muỗng cà phê bột bắp và 1/3 muỗng cà phê tiêu để giữ ẩm.",
        "Ớt chuông bỏ ruột thái miếng vuông cạnh 3cm; cà rốt gọt vỏ thái lát xéo mỏng khoảng 3mm để chín đồng đều.",
        "Làm nóng 1 muỗng canh dầu ăn trên lửa lớn, trút gà vào đảo nhanh 3 phút đến khi săn chín khoảng 85% thì múc riêng ra đĩa.",
        "Dùng 1/2 muỗng canh dầu còn lại phi thơm phần tỏi băm, cho cà rốt vào xào trước 2 phút với 1 muỗng canh nước lọc rồi thêm ớt chuông đảo tiếp 1,5 phút.",
        "Đổ đĩa gà trở lại chảo, nêm 1,5 muỗng canh dầu hào đảo đều trên lửa lớn 1 phút rồi rắc nốt tiêu xay và dọn ra đĩa."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=uc+ga+xao+ot+chuong"
    ),

    // 6. Salad ức gà áp chảo sốt sữa chua Hy Lạp
    CulinaryRecipe(
      id = "salad_uc_ga_sot_sua_chua",
      title = "Salad ức gà áp chảo sốt sữa chua",
      emoji = "🥗",
      summary = "Xốt sữa chua chanh mật ong thanh nhẹ thay mayonnaise, hợp hoàn hảo với ức gà mọng nước và xà lách.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken("250g", "Ức gà"), ingLettuce("150g"), ingYogurt("1 hũ (100g)"), ingCarrot("1/2 củ (50g)")),
      seasonings = listOf(
        sp("Nước cốt chanh tươi", "1 muỗng canh"),
        sp("Mật ong", "1 muỗng canh"),
        sp("Dầu ô liu", "1 muỗng canh"),
        sp("Tỏi băm", "1 tép"),
        sp("Muối", "1/2 muỗng cà phê"),
        sp("Tiêu đen xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Khứa nhẹ mặt ức gà rồi ướp với 1/3 muỗng cà phê muối, 1/3 muỗng cà phê tiêu và 1/2 muỗng canh dầu ô liu trong 10 phút.",
        "Rửa sạch 150g xà lách ngâm nước muối loãng rồi quay thật ráo; cà rốt bào sợi nhỏ.",
        "Làm nóng chảo chống dính trên lửa vừa, áp chảo ức gà mỗi mặt 4–5 phút tới khi chín vàng đều, tắt bếp ủ thêm 3 phút rồi thái lát xéo.",
        "Đánh quyện trong chén gồm 100g sữa chua không đường, 1 muỗng canh mật ong, 1 muỗng canh nước cốt chanh, 1/2 muỗng canh dầu ô liu, tỏi băm và nhúm muối.",
        "Xếp xà lách và cà rốt ra đĩa rộng, đặt từng lát ức gà ấm lên trên rồi rưới đều nước xốt sữa chua trước khi ăn."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = "https://www.savourydays.com/?s=salad+uc+ga+sot+sua+chua"
    ),

    // 7. Gà hầm nấm hương cà rốt & bắp ngọt
    CulinaryRecipe(
      id = "ga_ham_nam_bap_ngot",
      title = "Gà hầm nấm cà rốt & bắp ngọt",
      emoji = "🍲",
      summary = "Nước dùng trong ngọt tự nhiên từ xương gà, bắp Mỹ và nấm tươi, giàu dinh dưỡng cho bữa cơm gia đình.",
      prepMinutes = 15,
      cookMinutes = 30,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingChicken("500g", "Thịt gà chặt khúc"), ingMushroom("150g"), ingCarrot("1 củ (100g)"), ingCorn("1 trái (180g)")),
      seasonings = listOf(
        sp("Hành tím đập dập", "2 củ"),
        sp("Gừng tươi thái lát", "15g"),
        sp("Nước mắm ngon", "1,5 muỗng canh"),
        sp("Muối hạt", "1 muỗng cà phê"),
        sp("Hạt nêm", "1 muỗng cà phê"),
        sp("Hành lá & ngò rí", "2 nhánh"),
        sp("Tiêu xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Rửa sạch 500g gà chặt khúc, ướp với 1 muỗng canh nước mắm, 1/2 muỗng cà phê muối và 1 củ hành tím băm trong 10 phút.",
        "Cắt bắp ngọt thành 4–5 khoanh dày; cà rốt tỉa khoanh 1,5cm; nấm rửa sạch cắt bỏ gốc.",
        "Phi thơm phần hành tím và gừng với 1 muỗng nhỏ dầu ăn, cho gà vào đảo săn 3 phút rồi chế khoảng 1,1 lít nước lọc vào nấu sôi.",
        "Hớt sạch bọt để nước canh trong, thả bắp ngọt và cà rốt vào hầm lửa vừa nhỏ 15 phút.",
        "Cho nấm tươi vào nấu thêm 5 phút, nêm 1/2 muỗng canh nước mắm còn lại, hạt nêm cho vừa khẩu vị rồi rắc hành ngò và tiêu xay."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=ga+ham+nam+ca+rot+bap"
    ),

    // 8. Thịt ba chỉ kho trứng nước dừa
    CulinaryRecipe(
      id = "ba_chi_kho_trung_nuoc_dua",
      title = "Thịt ba chỉ kho trứng nước dừa",
      emoji = "🥘",
      summary = "Món thịt kho tàu chuẩn vị Việt với ba chỉ mềm tan, trứng thấm màu cánh gián óng ả nước dừa.",
      prepMinutes = 15,
      cookMinutes = 45,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingBacon("500g"), ingEgg("4 quả")),
      seasonings = listOf(
        sp("Nước dừa tươi", "350ml"),
        sp("Nước mắm ngon", "3,5 muỗng canh"),
        sp("Đường phèn hoặc đường vàng", "2 muỗng canh"),
        sp("Hành tím băm", "3 củ"),
        sp("Tỏi băm", "3 tép"),
        sp("Tiêu xay", "1/2 muỗng cà phê")
      ),
      steps = listOf(
        "Cắt thịt ba chỉ thành miếng vuông cạnh 3,5cm, chần sơ nước sôi 2 phút rồi xả nước nguội cho sạch bọt; luộc chín 4 quả trứng và bóc vỏ.",
        "Ướp thịt 20 phút với 3 muỗng canh nước mắm, 1 muỗng canh đường, hành tím băm, tỏi băm và 1/2 muỗng cà phê tiêu xay.",
        "Cho 1 muỗng canh đường còn lại vào nồi cùng 1 muỗng canh nước lọc, thắng lửa nhỏ tới khi chuyển màu cánh gián đẹp thì tắt bếp 5 giây rồi cho thịt vào đảo săn.",
        "Chế 350ml nước dừa tươi ngập mặt thịt, đun sôi hớt bọt rồi hạ lửa liu riu kho mở vung khoảng 20 phút.",
        "Thả trứng đã bóc vỏ vào kho thêm 20 phút tới khi thịt mềm rục và nước kho rút sánh lại còn 1/3, nêm lại 1/2 muỗng canh nước mắm cho vừa miệng."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=thit+ba+chi+kho+trung+nuoc+dua"
    ),

    // 9. Thịt ba chỉ rang cháy cạnh kèm dưa cải
    CulinaryRecipe(
      id = "ba_chi_rang_chay_canh_dua_cai",
      title = "Thịt ba chỉ rang cháy cạnh ăn kèm dưa cải",
      emoji = "🥓",
      summary = "Ba chỉ áp chảo cháy cạnh giòn thơm quyện mắm hành, dọn kèm dưa cải muối chua giảm ngấy hoàn hảo.",
      prepMinutes = 10,
      cookMinutes = 20,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingBacon("400g"), ingPickle("150g")),
      seasonings = listOf(
        sp("Nước mắm ngon", "2 muỗng canh"),
        sp("Đường", "1 muỗng cà phê"),
        sp("Hành tím băm", "2 củ"),
        sp("Tỏi băm", "2 tép"),
        sp("Hành lá thái khúc", "3 nhánh"),
        sp("Tiêu đen xay", "1/2 muỗng cà phê")
      ),
      steps = listOf(
        "Rửa sạch 400g ba chỉ, thái lát mỏng khoảng 4–5mm; vắt nhẹ 150g dưa cải muối cho bớt chua mặn rồi bày ra đĩa riêng.",
        "Đặt thịt ba chỉ vào chảo khô trên lửa vừa (không cần thêm dầu), rang 8–10 phút đến khi thịt tiết bớt mỡ và hai mặt xém vàng giòn.",
        "Chắt bớt khoảng 2/3 lượng mỡ thừa trong chảo ra bát để món ăn không bị ngấy.",
        "Đẩy thịt sang một bên chảo, phi thơm hành tím và tỏi băm trong phần mỡ còn lại khoảng 30 giây.",
        "Rưới hỗn hợp 2 muỗng canh nước mắm và 1 muỗng cà phê đường vào đảo đều 2 phút cho thịt thấm óng, rắc hành lá và tiêu rồi ăn nóng cùng dưa cải."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=ba+chi+rang+chay+canh"
    ),

    // 10. Thịt bò xào bông cải xanh cà rốt
    CulinaryRecipe(
      id = "bo_xao_bong_cai_xanh",
      title = "Thịt bò xào bông cải xanh cà rốt",
      emoji = "🥦",
      summary = "Thịt thăn bò mềm mọng nước xào nhanh lửa lớn cùng bông cải xanh và cà rốt giữ trọn độ giòn ngọt.",
      prepMinutes = 12,
      cookMinutes = 10,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMeat("250g", "Thịt thăn bò"), ingBroccoli("1 bông (250g)"), ingCarrot("1/2 củ (60g)")),
      seasonings = listOf(
        sp("Tỏi băm", "4 tép (1 muỗng canh)"),
        sp("Dầu hào", "1,5 muỗng canh"),
        sp("Nước tương (xì dầu)", "1 muỗng canh"),
        sp("Bột bắp", "1 muỗng cà phê"),
        sp("Dầu ăn", "2 muỗng canh"),
        sp("Tiêu đen xay", "1/2 muỗng cà phê")
      ),
      steps = listOf(
        "Thịt thăn bò thái lát mỏng ngang thớ, ướp 10 phút với 1 muỗng canh nước tương, 1 muỗng cà phê bột bắp, 1/2 lượng tỏi băm và 1/2 muỗng canh dầu ăn.",
        "Tách 250g bông cải xanh thành từng nhánh vừa ăn; cà rốt thái lát mỏng 3mm.",
        "Chần bông cải và cà rốt trong nồi nước sôi pha chút muối đúng 45 giây rồi vớt ngay vào tô nước lạnh giữ màu xanh mướt và để ráo.",
        "Đun nóng già 1,5 muỗng canh dầu ăn trên lửa lớn, trút thịt bò vào đảo thật nhanh 60–90 giây cho vừa chín tái rồi múc ra đĩa.",
        "Phi thơm nốt phần tỏi băm, xào bông cải và cà rốt với 1,5 muỗng canh dầu hào 1,5 phút rồi đổ thịt bò vào đảo chung 30 giây, rắc tiêu và tắt bếp."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=thit+bo+xao+bong+cai+xanh"
    ),

    // 11. Thịt bò xào dưa cải chua
    CulinaryRecipe(
      id = "bo_xao_dua_cai_chua",
      title = "Thịt bò xào dưa cải chua",
      emoji = "🥩",
      summary = "Món xào cơm nhà đậm đà với thịt bò mềm thơm tỏi hòa quyện vị chua giòn kích thích vị giác của dưa cải.",
      prepMinutes = 10,
      cookMinutes = 12,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingMeat("250g", "Thịt bò thăn"), ingPickle("200g")),
      seasonings = listOf(
        sp("Tỏi băm", "3 tép (1 muỗng canh)"),
        sp("Gừng băm", "1 muỗng cà phê"),
        sp("Dầu hào", "1 muỗng canh"),
        sp("Đường", "1/2 muỗng canh"),
        sp("Nước tương (xì dầu)", "1 muỗng canh"),
        sp("Dầu ăn", "1,5 muỗng canh"),
        sp("Tiêu xay", "1/2 muỗng cà phê")
      ),
      steps = listOf(
        "Rửa sơ 200g dưa cải muối nếu quá chua mặn rồi vắt ráo nước, cắt khúc khoảng 3cm.",
        "Thịt bò thái mỏng ngang thớ, ướp 10 phút với 1 muỗng canh nước tương, gừng băm, 1/2 lượng tỏi băm và 1/2 muỗng canh dầu ăn.",
        "Làm nóng 1/2 muỗng canh dầu ăn, phi thơm tỏi rồi cho dưa cải vào xào 3 phút cùng 1/2 muỗng canh đường để dưa dịu vị và giòn thơm, trút ra đĩa.",
        "Bật lửa lớn nhất, thêm 1/2 muỗng canh dầu còn lại và xào thịt bò thật nhanh trong 1 phút cho thịt vừa tái mềm.",
        "Đổ đĩa dưa cải trở lại chảo cùng 1 muỗng canh dầu hào, đảo chung trên lửa lớn 45 giây rồi rắc tiêu xay và dọn ăn nóng."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=thit+bo+xao+dua+cai+chua"
    ),

    // 12. Canh nấm thịt bằm cà rốt & bông cải
    CulinaryRecipe(
      id = "canh_nam_thit_bam_ca_rot",
      title = "Canh nấm thịt bằm cà rốt & bông cải",
      emoji = "🍄",
      summary = "Nước canh thanh ngọt tự nhiên không cần ninh xương nhờ thịt heo bằm xào thơm cùng nấm và rau củ.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMushroom("150g"), ingMeat("150g", "Thịt heo bằm"), ingCarrot("1/2 củ (60g)"), ingBroccoli("150g")),
      seasonings = listOf(
        sp("Hành tím băm", "2 củ"),
        sp("Nước mắm ngon", "1,5 muỗng canh"),
        sp("Muối", "1/2 muỗng cà phê"),
        sp("Hành lá & ngò rí", "2 nhánh"),
        sp("Dầu ăn", "1 muỗng cà phê"),
        sp("Tiêu đen xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Ướp 150g thịt heo bằm với 1/2 lượng hành tím băm, 1/2 muỗng canh nước mắm và 1/3 muỗng cà phê tiêu trong 5 phút.",
        "Nấm cắt gốc rửa sạch; cà rốt gọt vỏ tỉa lát mỏng; bông cải xanh cắt miếng nhỏ vừa thìa.",
        "Làm nóng nồi với 1 muỗng cà phê dầu, phi thơm hành tím còn lại rồi cho thịt bằm vào đảo tơi 2 phút cho săn dậy mùi.",
        "Chế 900ml nước vào nồi đun sôi, hớt bọt rồi thả cà rốt vào nấu trước 3 phút trên lửa vừa.",
        "Cho bông cải và nấm vào nấu thêm 3 phút cho rau chín vừa tới, nêm 1 muỗng canh nước mắm còn lại và 1/2 muỗng cà phê muối, rắc hành ngò rồi tắt bếp."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = "https://www.cooky.vn/tim-kiem?q=canh+nam+thit+bam+ca+rot"
    ),

    // 13. Tôm sú rim mặn ngọt tiêu xanh
    CulinaryRecipe(
      id = "tom_su_rim_man_ngot",
      title = "Tôm sú rim mặn ngọt tiêu xanh",
      emoji = "🦐",
      summary = "Từng con tôm sú đỏ au săn chắc áo lớp nước sốt mắm đường cánh gián sánh bóng, thơm cay dịu tiêu.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingShrimp("350g (tôm sú tươi)")),
      seasonings = listOf(
        sp("Nước mắm ngon", "2 muỗng canh"),
        sp("Đường vàng", "1,5 muỗng canh"),
        sp("Tỏi băm", "3 tép"),
        sp("Hành tím băm", "2 củ"),
        sp("Hành lá & tiêu xay", "2 nhánh hành, 1/2 muỗng cà phê tiêu"),
        sp("Dầu ăn", "1,5 muỗng canh")
      ),
      steps = listOf(
        "Tôm sú cắt bỏ râu, phần đầu nhọn và rút sạch chỉ đen dọc sống lưng, rửa nước muối loãng rồi để thật ráo nước.",
        "Ướp tôm 10 phút với 1 muỗng canh nước mắm, 1/2 muỗng canh đường, hành tím băm và 1/3 muỗng cà phê tiêu.",
        "Đun 1,5 muỗng canh dầu ăn và 1 muỗng canh đường còn lại trên lửa vừa tới khi đường tan và ngả màu vàng cánh gián thì cho tỏi băm vào phi thơm.",
        "Đổ tôm đã ướp vào đảo nhanh trên lửa lớn 3 phút cho vỏ tôm chuyển đỏ au và săn cong lại.",
        "Hạ lửa vừa rim tiếp 5–6 phút cùng 1 muỗng canh nước mắm còn lại tới khi sốt kẹo sánh bám bóng quanh vỏ tôm, rắc hành lá và tiêu xay."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=tom+rim+man+ngot"
    ),

    // 14. Tôm tươi xào bông cải xanh & ớt chuông
    CulinaryRecipe(
      id = "tom_xao_bong_cai_toi",
      title = "Tôm tươi xào bông cải xanh & ớt chuông",
      emoji = "🥦",
      summary = "Đĩa xào thanh đạm giàu đạm ít béo chỉ 12 phút nấu với tôm sú giòn ngọt và rau củ rực rỡ sắc màu.",
      prepMinutes = 10,
      cookMinutes = 12,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingShrimp("250g"), ingBroccoli("200g"), ingBellPepper("1/2 quả (80g)")),
      seasonings = listOf(
        sp("Tỏi băm", "3 tép (1 muỗng canh)"),
        sp("Dầu hào", "1 muỗng canh"),
        sp("Nước mắm ngon", "1/2 muỗng canh"),
        sp("Muối", "1/3 muỗng cà phê"),
        sp("Dầu ăn", "1,5 muỗng canh"),
        sp("Tiêu xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Bóc vỏ tôm chừa phần đuôi, rút chỉ lưng và ướp nhẹ với nhúm muối cùng 1/3 muỗng cà phê tiêu xay trong 5 phút.",
        "Cắt bông cải xanh thành từng bông nhỏ đều nhau và chần nhanh trong nước sôi 45 giây rồi vớt ra ngâm nước nguội, để ráo; ớt chuông thái miếng vừa ăn.",
        "Đun nóng 1 muỗng canh dầu ăn, phi thơm 1/2 lượng tỏi băm rồi cho tôm vào xào lửa lớn 2 phút tới khi chín đỏ săn lại thì múc ra đĩa.",
        "Cho phần dầu và tỏi còn lại vào chảo, trút bông cải và ớt chuông vào xào nhanh tay trong 2 phút trên lửa lớn.",
        "Đổ tôm trở lại chảo, nêm 1 muỗng canh dầu hào và 1/2 muỗng canh nước mắm đảo đều 45 giây cho quyện vị rồi dọn ra đĩa."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = "https://www.cooky.vn/tim-kiem?q=tom+xao+bong+cai+xanh"
    ),

    // 15. Mực xào cần tỏi ớt chuông giòn ngọt
    CulinaryRecipe(
      id = "muc_xao_can_toi_ot_chuong",
      title = "Mực xào cần tỏi ớt chuông giòn ngọt",
      emoji = "🦑",
      summary = "Mực ống khứa vảy rồng chần gừng giữ độ giòn sần sật, xào lửa lớn với ớt chuông và cà rốt không hề ra nước.",
      prepMinutes = 12,
      cookMinutes = 12,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSquid("300g"), ingBellPepper("1 quả (140g)"), ingCarrot("1/2 củ (60g)")),
      seasonings = listOf(
        sp("Gừng tươi đập dập", "15g"),
        sp("Tỏi băm", "3 tép (1 muỗng canh)"),
        sp("Dầu hào", "1 muỗng canh"),
        sp("Nước mắm ngon", "1 muỗng cà phê"),
        sp("Hành lá & cần tây (nếu có)", "2 nhánh"),
        sp("Dầu ăn", "1,5 muỗng canh"),
        sp("Tiêu đen xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Làm sạch ruột, túi mực và lớp da ngoài của 300g mực ống, khứa nhẹ hình caro bên trong thân rồi cắt miếng chữ nhật 3x4cm.",
        "Chần mực nhanh 30 giây trong nồi nước sôi có 15g gừng đập dập rồi vớt ngay ra tô đá lạnh 1 phút để mực giòn và hết hẳn nước khi xào.",
        "Thái lát mỏng cà rốt và cắt miếng vuông vừa đũa phần ớt chuông; hành lá thái khúc dài 4cm.",
        "Phi thơm 1/2 lượng tỏi với 1 muỗng canh dầu trên lửa lớn, xào mực nhanh tay 1 phút cùng 1/2 muỗng canh dầu hào rồi múc ra.",
        "Xào cà rốt và ớt chuông 2 phút cho chín tới, trút mực vào đảo đều 45 giây với phần dầu hào còn lại, 1 muỗng cà phê nước mắm và hành lá."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=muc+xao+ot+chuong"
    ),

    // 16. Mực hấp gừng sả cuốn xà lách mắm gừng
    CulinaryRecipe(
      id = "muc_hap_gung_sa_cuon_rau",
      title = "Mực hấp gừng sả cuốn xà lách mắm gừng",
      emoji = "🦑",
      summary = "Mực tươi hấp sả gừng giữ nguyên vị ngọt giòn tự nhiên của hải sản, cuốn lá xà lách chấm mắm gừng chua cay.",
      prepMinutes = 10,
      cookMinutes = 10,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSquid("350g"), ingLettuce("150g")),
      seasonings = listOf(
        sp("Gừng tươi (sợi hấp + giã làm mắm)", "35g"),
        sp("Sả cây đập dập", "3 cây"),
        sp("Nước mắm ngon", "2 muỗng canh"),
        sp("Đường", "1,5 muỗng canh"),
        sp("Nước cốt chanh", "1,5 muỗng canh"),
        sp("Tỏi & ớt giã nhuyễn", "2 tép tỏi, 1 trái ớt")
      ),
      steps = listOf(
        "Làm sạch mực ống, bóp nhẹ với chút rượu trắng hoặc gừng đập rồi xả sạch, để nguyên ống cho ngọt nước hoặc khứa khoanh.",
        "Xếp 3 cây sả đập dập và một nửa số gừng thái sợi xuống đáy đĩa sâu lòng, đặt mực lên trên.",
        "Đun sôi nước trong xửng rồi mới đặt đĩa mực vào hấp trên lửa lớn đúng 6–7 phút (không hấp quá lâu để mực không bị dai).",
        "Trong lúc chờ mực chín, rửa sạch 150g xà lách để thật ráo; giã nhuyễn phần gừng còn lại cùng tỏi ớt rồi pha với 2 muỗng canh nước mắm, 1,5 muỗng canh đường và 1,5 muỗng canh nước cốt chanh.",
        "Vớt mực ra cắt khoanh dày 1,5cm nếu để nguyên con, bày kèm lá xà lách tươi và chén nước mắm gừng thơm nồng."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=muc+hap+gung+sa"
    ),

    // 17. Súp cua bắp ngọt trứng gà sánh mịn
    CulinaryRecipe(
      id = "sup_cua_bap_ngot_trung_ga",
      title = "Súp cua bắp ngọt trứng gà sánh mịn",
      emoji = "🦀",
      summary = "Chén súp nóng hổi sánh nhẹ chuẩn vị quán với thịt cua ngọt thanh, hạt bắp vàng giòn và vân trứng mây tơ mịn.",
      prepMinutes = 15,
      cookMinutes = 20,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingCrab("200g"), ingCorn("1 trái (150g hạt)"), ingEgg("2 quả"), ingCarrot("1/2 củ (50g)")),
      seasonings = listOf(
        sp("Bột năng hòa tan", "3 muỗng canh bột + 80ml nước lạnh"),
        sp("Hành tím băm", "1 củ"),
        sp("Hạt nêm", "1,5 muỗng cà phê"),
        sp("Muối", "1/2 muỗng cà phê"),
        sp("Dầu mè", "1 muỗng cà phê"),
        sp("Ngò rí & tiêu xay", "2 nhánh ngò, 1/2 muỗng cà phê tiêu")
      ),
      steps = listOf(
        "Tách hạt 1 trái bắp ngọt; cà rốt thái hạt lựu thật nhỏ; dùng tay gỡ nhẹ phần thịt cua kiểm tra kỹ để không sót vỏ cứng.",
        "Phi thơm 1 củ hành tím băm với 1 muỗng cà phê dầu ăn, cho thịt cua vào đảo nhanh 1 phút cho dậy thơm rồi múc ra bát.",
        "Đun sôi 900ml nước dùng (hoặc nước lọc) cùng bắp ngọt và cà rốt trong 10 phút cho mềm ngọt rồi nêm 1,5 muỗng cà phê hạt nêm và 1/2 muỗng cà phê muối.",
        "Hạ lửa vừa, vừa khuấy đều tay vừa rót từ từ nước bột năng vào đến khi nồi súp đạt độ sánh mượt vừa ý, sau đó thả thịt cua vào.",
        "Đánh tan 2 quả trứng gà rồi rót qua rây thành dòng thật mảnh vào nồi súp đang sôi nhẹ đồng thời khuấy một chiều tạo vân mây; tắt bếp thêm 1 muỗng cà phê dầu mè, ngò rí và tiêu xay."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = "https://www.savourydays.com/?s=sup+cua+bap+ngot"
    ),

    // 18. Trứng chiên thịt bằm sốt cà chua
    CulinaryRecipe(
      id = "trung_chien_thit_bam_sot_ca",
      title = "Trứng chiên thịt bằm sốt cà chua",
      emoji = "🍳",
      summary = "Món cơm nhà cực kỳ bắt cơm với trứng chiên nhân thịt bằm đậm vị rưới sốt cà chua chua ngọt óng ả.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingEgg("3 quả"), ingMeat("100g", "Thịt heo bằm"), ingTomatoSauce("60g (4 muỗng canh)")),
      seasonings = listOf(
        sp("Hành tím băm", "2 củ"),
        sp("Nước mắm ngon", "1,5 muỗng canh"),
        sp("Đường", "1 muỗng cà phê"),
        sp("Hành lá thái nhỏ", "2 nhánh"),
        sp("Dầu ăn", "1,5 muỗng canh"),
        sp("Tiêu đen xay", "1/2 muỗng cà phê")
      ),
      steps = listOf(
        "Ướp 100g thịt heo bằm với 1/2 lượng hành tím băm, 1/2 muỗng canh nước mắm và 1/3 muỗng cà phê tiêu; đánh tan 3 quả trứng với hành lá và nhúm tiêu.",
        "Xào chín săn phần thịt bằm trong chảo với 1/2 muỗng canh dầu ăn trong 2 phút rồi trút vào tô trứng khuấy đều.",
        "Làm nóng 1 muỗng canh dầu còn lại trên chảo chống dính, đổ hỗn hợp trứng thịt bằm vào chiên lửa vừa mỗi mặt 2–3 phút tới khi vàng đều rồi gắp ra đĩa.",
        "Trên cùng chảo đó phi thơm phần hành tím còn lại, đổ 4 muỗng canh sốt cà chua cùng 40ml nước lọc, 1 muỗng canh nước mắm và 1 muỗng cà phê đường vào đảo.",
        "Đun sốt cà chua khoảng 2–3 phút cho sánh nhẹ rồi chan lên đĩa trứng thịt bằm, rắc nốt tiêu xay và dùng nóng với cơm."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = "https://www.cooky.vn/tim-kiem?q=trung+chien+thit+bam+sot+ca"
    ),

    // 19. Trứng bác sữa tươi bơ phô mai kiểu Pháp
    CulinaryRecipe(
      id = "trung_bac_sua_tuoi_pho_mai",
      title = "Trứng bác sữa tươi bơ phô mai kiểu Pháp",
      emoji = "🥚",
      summary = "Bữa sáng mềm ẩm, tan mịn như kem nhờ đánh trứng với sữa tươi và nấu lửa thật nhỏ cùng bơ lạt và phô mai.",
      prepMinutes = 5,
      cookMinutes = 8,
      servingsLabel = "1–2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingEgg("3 quả"), ingMilk("45ml"), ingButter("15g"), ingCheese("30g")),
      seasonings = listOf(
        sp("Muối mịn", "1/3 muỗng cà phê"),
        sp("Tiêu đen xay", "1/4 muỗng cà phê"),
        sp("Hành lá hoặc ngò rí thái nhuyễn", "1 muỗng canh")
      ),
      steps = listOf(
        "Đập 3 quả trứng gà vào tô, thêm 45ml sữa tươi không đường và 1/3 muỗng cà phê muối rồi đánh nhẹ đến khi lòng trắng và lòng đỏ hòa tan hoàn toàn.",
        "Cắt nhỏ hoặc xé sợi 30g phô mai; đặt chảo chống dính lên bếp ở lửa thật nhỏ và cho 15g bơ lạt vào tan chảy từ từ (tránh làm cháy bơ).",
        "Đổ hỗn hợp trứng sữa vào chảo, chờ khoảng 15 giây cho phần đáy se nhẹ rồi dùng phới silicon đẩy nhẹ từng lớp trứng từ mép vào tâm.",
        "Lặp lại thao tác khuấy chậm 3–4 phút cho đến khi hình thành các mảng trứng mềm mịn như kem và vẫn còn hơi bóng ẩm.",
        "Tắt bếp ngay khi trứng chưa khô kiệt (sức nóng dư sẽ làm trứng chín tiếp), thả phô mai vào đảo tan đều, rắc tiêu đen và hành lá lên mặt."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = "https://www.seriouseats.com/search?q=soft+scrambled+eggs+butter+cheese"
    ),

    // 20. Canh trứng mây cà chua & bắp ngọt
    CulinaryRecipe(
      id = "canh_trung_may_sot_ca_bap_ngot",
      title = "Canh trứng mây cà chua & bắp ngọt",
      emoji = "🍅",
      summary = "Bát canh quốc dân thanh nhẹ nấu chỉ 12 phút, nổi bật sắc đỏ cà chua, hạt bắp ngọt và vân trứng mịn mượt.",
      prepMinutes = 5,
      cookMinutes = 12,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN, CulinaryFilter.VEGETARIAN),
      fridgeIngredients = listOf(ingTomatoSauce("45g (3 muỗng canh)"), ingEgg("2 quả"), ingCorn("1/2 trái (80g hạt)"), ingWater("750ml")),
      seasonings = listOf(
        sp("Hành tím băm", "1 củ"),
        sp("Nước mắm ngon", "1 muỗng canh"),
        sp("Muối", "1/2 muỗng cà phê"),
        sp("Dầu ăn", "1 muỗng cà phê"),
        sp("Hành lá & ngò rí", "2 nhánh"),
        sp("Tiêu xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Đánh tan 2 quả trứng gà với nhúm muối nhỏ cho đều tay; tách hạt 1/2 trái bắp ngọt và thái nhỏ hành lá, ngò rí.",
        "Làm nóng 1 muỗng cà phê dầu ăn trong nồi, phi thơm 1 củ hành tím băm rồi cho 3 muỗng canh sốt cà chua vào đảo 1 phút cho dậy màu đỏ đẹp.",
        "Chế 750ml nước suối vào nồi, thả hạt bắp ngọt vào nấu sôi khoảng 5 phút để hạt bắp chín mềm và ra vị ngọt tự nhiên.",
        "Nêm 1 muỗng canh nước mắm và 1/2 muỗng cà phê muối vừa khẩu vị, chỉnh bếp sôi lăn tăn nhẹ.",
        "Rót trứng từ từ theo vòng tròn quanh nồi rồi khuấy nhẹ theo một chiều tạo vân mây mịn mượt, tắt bếp thêm hành ngò và tiêu xay."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=canh+trung+may+ca+chua"
    ),

    // 21. Cơm chiên Dương Châu tôm trứng cà rốt
    CulinaryRecipe(
      id = "com_chien_duong_chau",
      title = "Cơm chiên Dương Châu tôm trứng cà rốt",
      emoji = "🍚",
      summary = "Hạt cơm tơi săn vàng óng áo trứng gà, đầy đủ sắc vị từ tôm sú giòn, cà rốt hạt lựu và hương dầu mè thơm lừng.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.ASIAN_FUSION),
      fridgeIngredients = listOf(ingRice("350g (2 chén cơm nguội)"), ingShrimp("150g"), ingEgg("2 quả"), ingCarrot("1/2 củ (50g)")),
      seasonings = listOf(
        sp("Tỏi băm", "2 tép"),
        sp("Nước tương (xì dầu)", "1,5 muỗng canh"),
        sp("Dầu hào", "1/2 muỗng canh"),
        sp("Dầu ăn", "2 muỗng canh"),
        sp("Hành lá thái nhỏ", "2 nhánh"),
        sp("Tiêu đen xay", "1/2 muỗng cà phê")
      ),
      steps = listOf(
        "Tôm bóc vỏ rút chỉ lưng rồi cắt hạt lựu, xóc với chút tiêu; cà rốt thái hạt lựu nhỏ rồi chần nước sôi 1,5 phút cho mềm và để ráo.",
        "Tán tơi 350g cơm nguội với 1 lòng đỏ trứng gà và 1/2 muỗng canh dầu ăn để hạt cơm săn rời và có màu vàng đều đẹp mắt.",
        "Làm nóng 1 muỗng canh dầu ăn trên chảo rộng, cho tỏi băm vào phi thơm rồi xào tôm và cà rốt 2 phút cho vừa chín săn thì múc ra đĩa.",
        "Thêm 1/2 muỗng canh dầu, đổ phần trứng còn lại vào đảo tơi rồi trút tô cơm vào rang trên lửa vừa 4–5 phút đến khi hạt cơm săn nhảy nhẹ.",
        "Cho tôm và cà rốt vào đảo chung, nêm 1,5 muỗng canh nước tương và 1/2 muỗng canh dầu hào xào lửa lớn thêm 1,5 phút rồi rắc hành lá và tiêu."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=com+chien+duong+chau"
    ),

    // 22. Cơm chiên dưa bò chuẩn vị quán
    CulinaryRecipe(
      id = "com_chien_dua_bo",
      title = "Cơm chiên dưa bò chuẩn vị quán",
      emoji = "🍚",
      summary = "Đĩa cơm chiên nóng giòn trứ danh với thịt bò mềm, dưa cải muối chua xào thấm vị và hạt cơm rang trứng tơi xốp.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingRice("350g (2 chén cơm nguội)"), ingMeat("150g", "Thịt bò thăn"), ingPickle("120g"), ingEgg("2 quả")),
      seasonings = listOf(
        sp("Tỏi băm", "3 tép"),
        sp("Nước tương (xì dầu)", "1,5 muỗng canh"),
        sp("Dầu hào", "1 muỗng canh"),
        sp("Đường", "1/2 muỗng cà phê"),
        sp("Dầu ăn", "2 muỗng canh"),
        sp("Hành lá & tiêu xay", "2 nhánh hành, 1/2 muỗng cà phê tiêu")
      ),
      steps = listOf(
        "Thịt bò thái lát thật mỏng rồi băm nhẹ hoặc cắt miếng nhỏ, ướp 5 phút với 1/2 muỗng canh nước tương, 1/2 lượng tỏi băm và 1/2 muỗng canh dầu ăn.",
        "Vắt ráo 120g dưa cải muối chua và thái nhỏ vừa miếng ăn; tán tơi cơm nguội với 1 quả trứng để cơm không bị vón cục.",
        "Xào dưa cải với 1/2 muỗng canh dầu và 1/2 muỗng cà phê đường trong 2 phút cho săn thơm rồi đẩy sang một bên; bật lửa lớn xào thịt bò tái 1 phút và trút chung ra đĩa.",
        "Làm nóng 1 muỗng canh dầu còn lại, chiên tơi quả trứng thứ 2 rồi đổ cơm nguội vào rang trên lửa vừa 4–5 phút tới khi cơm khô tơi.",
        "Trút đĩa dưa bò vào rang chung trên lửa lớn, nêm 1 muỗng canh nước tương còn lại và 1 muỗng canh dầu hào đảo đều 1,5 phút, rắc hành lá và tiêu."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=com+chien+dua+bo"
    ),

    // 23. Bánh mì chảo trứng ốp la sốt cà bơ thơm
    CulinaryRecipe(
      id = "banh_mi_chao_trung_sot_ca",
      title = "Bánh mì chảo trứng ốp la sốt cà bơ thơm",
      emoji = "🥖",
      summary = "Bữa sáng kiểu tiệm nóng hổi ngay tại nhà với chảo sốt cà bơ tỏi béo thơm, trứng ốp la lòng đào và bánh mì giòn rụm.",
      prepMinutes = 5,
      cookMinutes = 12,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingBread("2 ổ (160g)"), ingEgg("2 quả"), ingTomatoSauce("60g (4 muỗng canh)"), ingButter("15g")),
      seasonings = listOf(
        sp("Tỏi băm", "2 tép"),
        sp("Đường", "1 muỗng cà phê"),
        sp("Nước tương (xì dầu)", "1 muỗng cà phê"),
        sp("Ngò rí & hành lá", "2 nhánh"),
        sp("Tiêu đen xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Xịt nhẹ nước hoặc áp nóng 2 ổ bánh mì trên chảo khô hoặc lò nướng ở 180°C khoảng 3 phút cho lớp vỏ giòn tan.",
        "Đun chảy 15g bơ lạt trên chảo gang hoặc chảo chống dính nhỏ ở lửa vừa nhỏ rồi phi thơm 2 tép tỏi băm trong 20 giây.",
        "Cho 4 muỗng canh sốt cà chua, 30ml nước lọc và 1 muỗng cà phê đường vào đun sôi lăn tăn khoảng 2 phút cho sốt sánh mượt.",
        "Gạt nhẹ tạo hai khoảng trống trên mặt sốt rồi đập 2 quả trứng gà vào ốp la, đậy nắp nấu trên lửa nhỏ khoảng 2,5 phút để lòng trắng chín mượt còn lòng đỏ vẫn dẻo thơm.",
        "Tắt bếp, rưới 1 muỗng cà phê nước tương lên trứng, rắc ngò rí và tiêu đen rồi mang cả chảo nóng ra chấm cùng bánh mì giòn."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = "https://www.savourydays.com/?s=banh+mi+chao"
    ),

    // 24. Bánh mì nướng bơ tỏi nấm phô mai
    CulinaryRecipe(
      id = "banh_mi_bo_toi_nam_pho_mai",
      title = "Bánh mì nướng bơ tỏi nấm phô mai",
      emoji = "🧀",
      summary = "Vỏ bánh mì giòn rụm thấm bơ tỏi thơm lừng kết hợp nấm xào đậm đà và lớp phô mai nướng tan kéo sợi.",
      prepMinutes = 10,
      cookMinutes = 12,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingBread("2 ổ nhỏ (150g)"), ingButter("25g"), ingMushroom("120g"), ingCheese("50g")),
      seasonings = listOf(
        sp("Tỏi băm thật nhuyễn", "4 tép (1 muỗng canh)"),
        sp("Muối mịn", "1/4 muỗng cà phê"),
        sp("Tiêu đen xay", "1/4 muỗng cà phê"),
        sp("Ngò rí hoặc lá oregano băm", "1 muỗng canh")
      ),
      steps = listOf(
        "Để 25g bơ lạt mềm ở nhiệt độ phòng rồi trộn thật nhuyễn với 4 tép tỏi băm, 1/4 muỗng cà phê muối, 1/4 muỗng cà phê tiêu và ngò rí băm.",
        "Thái lát mỏng 120g nấm tươi, cho lên chảo khô đảo lửa lớn 3 phút với nhúm muối nhỏ cho nấm chín và ra hết hơi nước để không làm ướt bánh.",
        "Xẻ đôi 2 ổ bánh mì theo chiều dọc, phết đều hỗn hợp bơ tỏi kín hai mặt cắt.",
        "Trải đều phần nấm đã xào khô lên bề mặt bánh rồi phủ 50g phô mai thái sợi lên trên.",
        "Nướng bằng nồi chiên không dầu hoặc lò nướng ở 180°C trong 6–7 phút (hoặc áp chảo đậy vung 5 phút) tới khi phô mai tan chảy vàng xém cạnh."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = "https://www.savourydays.com/?s=banh+mi+nuong+bo+toi+pho+mai"
    ),

    // 25. Bánh mì nướng kiểu Pháp chuối mật ong
    CulinaryRecipe(
      id = "banh_mi_nuong_phap_chuoi",
      title = "Bánh mì nướng kiểu Pháp chuối mật ong",
      emoji = "🍌",
      summary = "Bánh mì nhúng trứng sữa áp chảo mềm thơm bên trong, xém vàng bên ngoài ăn kèm chuối chín cắt lát.",
      prepMinutes = 8,
      cookMinutes = 12,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.EAT_CLEAN, CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingBread("4 lát sandwich (120g)"), ingBanana("1 quả (100g)"), ingMilk("80ml"), ingEgg("2 quả")),
      seasonings = listOf(
        sp("Mật ong", "1,5 muỗng canh"),
        sp("Bột quế (tùy chọn)", "1/4 muỗng cà phê"),
        sp("Muối tinh", "1 nhúm nhỏ"),
        sp("Dầu dừa hoặc dầu ăn", "1 muỗng canh")
      ),
      steps = listOf(
        "Đập 2 quả trứng gà vào tô rộng, thêm 80ml sữa tươi không đường, 1 nhúm muối tinh và 1/4 muỗng cà phê bột quế rồi đánh hòa quyện.",
        "Bóc vỏ 1 quả chuối chín, cắt khoanh chéo dày khoảng 5mm để bày kèm.",
        "Nhúng từng lát bánh mì sandwich vào hỗn hợp trứng sữa khoảng 15 giây mỗi mặt sao cho bánh thấm ướt nhưng không bị nhũn rách.",
        "Làm nóng chảo chống dính với 1 muỗng canh dầu dừa trên lửa vừa nhỏ, áp chảo bánh mỗi mặt 2,5–3 phút đến khi vàng ruộm thơm mùi trứng sữa.",
        "Xếp bánh chồng lên đĩa, phủ các lát chuối chín lên mặt và rưới đều 1,5 muỗng canh mật ong trước khi dùng ấm."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = "https://www.seriouseats.com/search?q=french+toast+banana"
    ),

    // 26. Bánh bao hấp nóng ăn sáng kèm sữa tươi
    CulinaryRecipe(
      id = "banh_bao_hap_sua_tuoi_an_sang",
      title = "Bánh bao hấp nóng ăn sáng kèm sữa tươi",
      emoji = "🥟",
      summary = "Bữa sáng nhanh gọn chỉ 12 phút với bánh bao hấp giữ vỏ xốp ẩm cùng ly sữa tươi thanh mát đủ năng lượng.",
      prepMinutes = 3,
      cookMinutes = 12,
      servingsLabel = "1–2 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingDumpling("3 cái (270g)"), ingMilk("200ml")),
      seasonings = listOf(
        sp("Giấm trắng (nhỏ vào nước hấp)", "1 muỗng cà phê"),
        sp("Nước tương & tương ớt ăn kèm", "1 muỗng canh (tùy thích)")
      ),
      steps = listOf(
        "Chuẩn bị nồi hấp với lượng nước vừa đủ, nhỏ thêm 1 muỗng cà phê giấm trắng giúp vỏ bánh sau khi hấp trắng xốp tự nhiên.",
        "Đợi nước trong xửng sôi mạnh, xếp 3 chiếc bánh bao lên đĩa hoặc giấy nến cách nhau 2cm để hơi nước lưu thông đều và không dính đáy.",
        "Bọc một chiếc khăn sạch dưới nắp nồi hấp để hơi nước ngưng tụ không nhỏ giọt làm nhão mặt vỏ bánh.",
        "Hấp lửa vừa trong 10–12 phút nếu bánh để ngăn mát (hoặc 15 phút nếu lấy thẳng từ ngăn đông) đến khi vỏ bánh phồng mềm.",
        "Tắt bếp chờ 1 phút rồi mở nắp từ từ để bánh không bị xẹp vỏ; rót 200ml sữa tươi ra ly dùng nóng cùng bánh bao và tương ớt nếu thích."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=cach+hap+banh+bao+mem+xop"
    ),

    // 27. Gỏi xoài xanh tôm thịt ba chỉ
    CulinaryRecipe(
      id = "goi_xoai_xanh_tom_thit",
      title = "Gỏi xoài xanh tôm thịt ba chỉ",
      emoji = "🥭",
      summary = "Đĩa gỏi chua cay mặn ngọt đúng điệu Việt Nam với xoài keo xanh bào sợi giòn sần sật, tôm luộc và ba chỉ thái mỏng.",
      prepMinutes = 15,
      cookMinutes = 15,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMango("1 quả xoài xanh (300g)", "Xoài keo xanh"), ingShrimp("150g"), ingBacon("120g"), ingCarrot("1/2 củ (50g)")),
      seasonings = listOf(
        sp("Nước mắm ngon", "2,5 muỗng canh"),
        sp("Đường", "2,5 muỗng canh"),
        sp("Nước cốt chanh tươi", "2 muỗng canh"),
        sp("Tỏi & ớt băm nhuyễn", "3 tép tỏi, 1 trái ớt"),
        sp("Đậu phộng rang giã dập", "2 muỗng canh"),
        sp("Rau răm hoặc húng lủi", "1 nắm nhỏ")
      ),
      steps = listOf(
        "Luộc chín 120g thịt ba chỉ với nhúm muối khoảng 12 phút rồi vớt ngâm nước lạnh và thái lát mỏng; tôm luộc 3 phút tới khi chín đỏ, bóc vỏ chẻ đôi lưng.",
        "Gọt vỏ xoài xanh và cà rốt, bào sợi dài đều nhau rồi ngâm tô nước đá khoảng 5 phút và vớt thật ráo để giữ độ giòn.",
        "Pha nước trộn gỏi theo tỷ lệ chuẩn 2,5 muỗng canh nước mắm : 2,5 muỗng canh đường : 2 muỗng canh nước cốt chanh, khuấy tan rồi cho tỏi ớt băm vào.",
        "Cho xoài và cà rốt vào âu lớn, rưới một nửa chén nước gỏi vào đảo nhẹ tay 2 phút cho thấm rồi chắt bỏ bớt nước tiết ra.",
        "Thêm tôm, ba chỉ thái mỏng, rau răm thái rối và phần nước trộn còn lại đảo đều; bày ra đĩa rắc 2 muỗng canh đậu phộng rang giã dập lên mặt."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=goi+xoai+tom+thit"
    ),

    // 28. Salad ức gà táo xanh nho tươi dầu giấm
    CulinaryRecipe(
      id = "salad_uc_ga_tao_nho",
      title = "Salad ức gà táo nho dầu giấm",
      emoji = "🍏",
      summary = "Món salad tươi giòn cân bằng vị ngọt của táo, nho mọng nước cùng ức gà áp chảo và xốt dầu giấm mật ong.",
      prepMinutes = 10,
      cookMinutes = 12,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingApple("1 quả (150g)"), ingGrape("100g"), ingChicken("200g", "Ức gà"), ingLettuce("120g")),
      seasonings = listOf(
        sp("Dầu ô liu", "2 muỗng canh"),
        sp("Nước cốt chanh hoặc giấm táo", "1,5 muỗng canh"),
        sp("Mật ong", "1 muỗng canh"),
        sp("Muối", "1/2 muỗng cà phê"),
        sp("Tiêu đen xay", "1/3 muỗng cà phê")
      ),
      steps = listOf(
        "Ướp 200g ức gà với 1/3 muỗng cà phê muối, tiêu đen và 1/2 muỗng canh dầu ô liu trong 5 phút rồi áp chảo mỗi mặt 4 phút cho chín vàng, thái lát vừa ăn.",
        "Rửa sạch 120g xà lách để thật ráo; 100g nho cắt đôi theo chiều dọc.",
        "Táo tươi rửa sạch giữ vỏ, cắt lát mỏng vừa ăn và ngâm nhanh 2 phút trong tô nước pha chút nước cốt chanh loãng để không bị thâm, sau đó để ráo.",
        "Khuấy đều xốt dầu giấm gồm 1,5 muỗng canh dầu ô liu, 1,5 muỗng canh nước cốt chanh (hoặc giấm táo), 1 muỗng canh mật ong và nhúm muối.",
        "Trộn nhẹ xà lách, táo, nho trong âu với 2/3 phần xốt rồi trút ra đĩa, xếp ức gà ấm lên trên và rưới nốt phần xốt còn lại."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = "https://www.seriouseats.com/search?q=apple+grape+chicken+salad"
    ),

    // 29. Sinh tố chuối dâu tây sữa chua ít đường
    CulinaryRecipe(
      id = "sinh_to_chuoi_dau_sua_chua",
      title = "Sinh tố chuối dâu tây sữa chua ít đường",
      emoji = "🍓",
      summary = "Thức uống Eat-Clean sánh mịn tự nhiên không cần thêm đường tinh luyện nhờ độ ngọt từ chuối chín và dâu tây tươi.",
      prepMinutes = 5,
      cookMinutes = 3,
      servingsLabel = "1–2 người ăn",
      categories = setOf(CulinaryFilter.EAT_CLEAN, CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingBanana("1 quả (120g)"), ingStrawberry("100g"), ingYogurt("1 hũ (100g)"), ingMilk("100ml")),
      seasonings = listOf(
        sp("Mật ong (tùy khẩu vị)", "1 muỗng cà phê"),
        sp("Đá viên nhỏ", "60g (khoảng 4–5 viên)"),
        sp("Hạt chia (tùy chọn)", "1 muỗng cà phê")
      ),
      steps = listOf(
        "Ngâm nhẹ 100g dâu tây với nước muối loãng 2 phút rồi rửa sạch, cắt bỏ đài cuống và bổ đôi quả.",
        "Bóc vỏ 1 quả chuối chín, cắt khoanh dày khoảng 2cm (có thể dùng chuối đã cấp đông để ly sinh tố sánh lạnh hơn).",
        "Cho chuối, dâu tây, 100g sữa chua không đường và 100ml sữa tươi vào cối xay sinh tố.",
        "Thêm đá viên và 1 muỗng cà phê mật ong nếu muốn ngọt hơn, bật máy xay ở tốc độ cao khoảng 30–40 giây tới khi hỗn hợp mịn mượt hoàn toàn.",
        "Rót sinh tố ra ly cao, rắc 1 muỗng cà phê hạt chia lên mặt và thưởng thức ngay để giữ trọn vitamin C."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=sinh+to+chuoi+dau+tay+sua+chua"
    ),

    // 30. Sữa chua tô trái cây táo nho dâu tây
    CulinaryRecipe(
      id = "sua_chua_to_trai_cay_tuoi",
      title = "Sữa chua tô trái cây táo nho dâu tây",
      emoji = "🍓",
      summary = "Bữa sáng thanh nhẹ không cần bật bếp, kết hợp 2 hũ sữa chua mát lạnh cùng táo giòn, nho ngọt và dâu tây tươi.",
      prepMinutes = 8,
      cookMinutes = 2,
      servingsLabel = "1–2 người ăn",
      categories = setOf(CulinaryFilter.EAT_CLEAN, CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingYogurt("2 hũ (200g)"), ingApple("1/2 quả (80g)"), ingGrape("80g"), ingStrawberry("80g")),
      seasonings = listOf(
        sp("Mật ong nguyên chất", "1 muỗng canh"),
        sp("Nước cốt chanh (ngâm táo)", "1/2 muỗng cà phê"),
        sp("Granola / yến mạch rang (tùy chọn)", "2 muỗng canh")
      ),
      steps = listOf(
        "Rửa sạch táo, nho và dâu tây qua nước muối loãng rồi thấm ráo hoàn toàn để không làm loãng sữa chua.",
        "Thái hạt lựu hoặc lát mỏng 80g táo và xóc nhanh với vài giọt nước cốt chanh giữ màu sáng giòn.",
        "Cắt đôi 80g nho bỏ hạt (nếu có) và bổ đôi hoặc lát mỏng 80g dâu tây.",
        "Múc 200g sữa chua lạnh ra tô rộng, dùng muỗng dàn đều thành bề mặt phẳng mịn.",
        "Xếp xen kẽ táo, nho và dâu tây lên trên, rắc thêm 2 muỗng canh granola nếu có rồi rưới 1 muỗng canh mật ong."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = "https://www.cooky.vn/tim-kiem?q=sua+chua+trai+cay+healthy"
    ),

    // 31. Xoài dầm sữa tươi kem vani mát lạnh
    CulinaryRecipe(
      id = "kem_xoai_sua_tuoi_tran_chau",
      title = "Xoài dầm sữa tươi kem vani mát lạnh",
      emoji = "🍨",
      summary = "Món tráng miệng chiều hè chuẩn quán chè với thịt xoài chín thơm lừng, sữa tươi béo nhẹ và kem vani mát lạnh.",
      prepMinutes = 7,
      cookMinutes = 3,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingIceCream("2 viên (120g)"), ingMango("1 quả xoài chín (250g)", "Xoài cát chín"), ingMilk("100ml")),
      seasonings = listOf(
        sp("Sữa đặc có đường (hoặc mật ong)", "1,5 muỗng canh"),
        sp("Đá bào hoặc đá viên nhỏ", "80g"),
        sp("Dừa sấy khô / hạnh nhân lát (tùy chọn)", "1 muỗng canh")
      ),
      steps = listOf(
        "Gọt vỏ 1 quả xoài chín ngọt, cắt phần má xoài thành các khối vuông cạnh 1,5cm; giữ lại vài miếng đầu hạt để xay sệt.",
        "Xay nhuyễn phần thịt xoài vụn với 40ml sữa tươi và 1 muỗng canh sữa đặc tạo hỗn hợp sốt xoài vàng sánh.",
        "Chuẩn bị 2 chén hoặc ly thủy tinh lạnh, rải đá bào xuống đáy và chan phần sốt xoài sánh vào.",
        "Đổ nốt 60ml sữa tươi còn lại cùng 1/2 muỗng canh sữa đặc vào ly rồi xếp đều xoài cắt khối vuông lên trên.",
        "Múc 2 viên kem vani đặt lên chính giữa mặt ly, rắc thêm chút dừa khô nếu thích và thưởng thức ngay bằng muỗng."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=xoai+dam+sua+tuoi+kem"
    ),

    // 32. Nước ép dưa hấu cam tươi bạc hà giải nhiệt
    CulinaryRecipe(
      id = "nuoc_ep_dua_hau_cam_tuoi",
      title = "Nước ép dưa hấu cam tươi bạc hà giải nhiệt",
      emoji = "🍉",
      summary = "Thức uống giải nhiệt giàu vitamin C kết hợp ruột dưa hấu ngọt mát, nước cam ép và hương bạc hà sảng khoái.",
      prepMinutes = 7,
      cookMinutes = 3,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.EAT_CLEAN, CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingWatermelon("350g ruột"), ingOrangeJuice("120ml"), ingWater("100ml")),
      seasonings = listOf(
        sp("Lá bạc hà tươi", "6–8 lá"),
        sp("Nước cốt chanh", "1/2 muỗng canh"),
        sp("Đá viên", "80g"),
        sp("Muối tinh", "1 nhúm cực nhỏ")
      ),
      steps = listOf(
        "Gọt bỏ phần vỏ trắng và nhặt sạch hạt từ 350g ruột dưa hấu đỏ, cắt thành khối nhỏ cho dễ xay.",
        "Cho dưa hấu cùng 100ml nước suối mát và 1 nhúm muối cực nhỏ (giúp vị dưa ngọt đậm hơn) vào máy xay sinh tố xay 20 giây.",
        "Đặt rây lọc qua ca lớn để loại bỏ phần bã xơ giúp nước ép thanh trong và dễ uống.",
        "Vò nhẹ 6 lá bạc hà cho tinh dầu tiết ra dưới đáy 2 ly cao rồi thêm đá viên đến nửa ly.",
        "Rót nước ép dưa hấu, 120ml nước cam tươi và 1/2 muỗng canh nước cốt chanh vào khuấy đều, gắn thêm nhánh bạc hà trên miệng ly."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = "https://www.cooky.vn/tim-kiem?q=nuoc+ep+dua+hau+cam+bac+ha"
    ),

    // 33. Trứng cuộn Nhật Bản (Tamagoyaki) — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "trung_cuon_nhat_tamagoyaki",
      title = "Trứng cuộn Nhật Bản (Tamagoyaki)",
      emoji = "🥚",
      summary = "Món trứng cuộn nhiều lớp mềm mịn chuẩn vị Nhật từ trứng gà, nước dùng thanh nhẹ, đường và nước tương.",
      prepMinutes = 5,
      cookMinutes = 10,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.ASIAN_FUSION, CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingEgg("4 quả"), ingWater("30ml (thay nước Dashi)")),
      seasonings = listOf(
        sp("Đường", "1 muỗng canh"),
        sp("Nước tương (xì dầu)", "1 muỗng cà phê"),
        sp("Muối", "1 nhúm nhỏ"),
        sp("Dầu ăn (thoa chảo)", "1 muỗng canh")
      ),
      steps = listOf(
        "Đập 4 quả trứng gà vào tô, cho 1 muỗng canh nước lọc (hoặc nước dùng Dashi), 1 muỗng canh đường, 1 muỗng cà phê nước tương và chút muối vào đánh kiểu cắt lòng trắng cho hòa quyện.",
        "Lọc hỗn hợp trứng qua rây để loại bỏ phần dây trắng giúp mặt trứng chín mịn màng hoàn toàn.",
        "Đặt chảo chống dính lên bếp lửa nhỏ, dùng khăn giấy thấm dầu thoa một lớp thật mỏng khắp lòng và thành chảo.",
        "Đổ khoảng 1/4 lượng trứng tráng lớp mỏng; khi mặt trứng se 70–80% thì nhẹ nhàng cuộn tròn từ xa về phía mình rồi đẩy về góc chảo.",
        "Thoa dầu mỏng lại mặt chảo trống, đổ lớp trứng mới (nhấc nhẹ cuộn cũ cho trứng chảy xuống dưới kết dính) và cuộn lặp lại đến hết, ép nhẹ thành khối rồi cắt khoanh 2–3cm."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=trung+cuon+nhat+ban+tamagoyaki"
    ),

    // 34. Ức gà nướng sốt Teriyaki — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "uc_ga_sot_teriyaki",
      title = "Ức gà nướng sốt Teriyaki",
      emoji = "🍗",
      summary = "Ức gà ướp nước ép cam, dầu hào, nước tương, đường nâu và dầu mè nướng thơm đậm vị Nhật Bản.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.ASIAN_FUSION, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken("400g", "Ức gà"), ingOrangeJuice("60ml")),
      seasonings = listOf(
        sp("Nước tương (xì dầu)", "2 muỗng canh"),
        sp("Dầu hào", "1 muỗng canh"),
        sp("Đường nâu", "1,5 muỗng canh"),
        sp("Dầu mè & mè rang", "1 muỗng cà phê dầu mè, 1 muỗng mè rang"),
        sp("Tỏi băm & gừng băm", "1 muỗng cà phê mỗi loại")
      ),
      steps = listOf(
        "Rửa sạch 400g ức gà với muối và gừng để khử mùi tanh, thấm ráo rồi cắt lát dày khoảng 1,5–2cm.",
        "Trộn đều tỏi băm, gừng băm, 60ml nước ép cam, 2 muỗng canh nước tương, 1 muỗng canh dầu hào, 1,5 muỗng canh đường nâu, dầu mè và mè rang thành sốt ướp.",
        "Ướp ức gà trong hỗn hợp sốt ít nhất 15–20 phút cho thịt ngấm đậm gia vị.",
        "Làm nóng bếp nướng hoặc chảo chống dính lửa vừa, đặt ức gà lên nướng/áp mỗi mặt 5–6 phút (thỉnh thoảng phết thêm nước sốt ướp).",
        "Khi thịt gà chín vàng óng và nước sốt sánh thơm thì gắp ra thớt thái miếng vừa ăn, rắc thêm mè rang lên trên."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=uc+ga+nuong+sot+teriyaki"
    ),

    // 35. Thịt ba chỉ xào kim chi — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "ba_chi_xao_kim_chi_han_quoc",
      title = "Thịt ba chỉ xào kim chi",
      emoji = "🥓",
      summary = "Thịt ba chỉ đảo cháy cạnh béo thơm xào lửa lớn cùng kim chi / dưa cải chua cay đậm vị chuẩn Hàn Quốc.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.ASIAN_FUSION),
      fridgeIngredients = listOf(ingBacon("400g"), ingPickle("150g")),
      seasonings = listOf(
        sp("Ớt bột", "1 muỗng cà phê"),
        sp("Đường", "1 muỗng cà phê"),
        sp("Hành tím & tỏi băm", "1 củ hành tím, 1 tép tỏi"),
        sp("Dầu ăn", "2 muỗng canh"),
        sp("Đầu hành lá & ngò rí", "5 đầu hành, 1 nhánh ngò")
      ),
      steps = listOf(
        "Rửa sạch 400g thịt ba chỉ với nước muối loãng rồi cắt lát vừa ăn; cắt khúc 150g kim chi hoặc dưa cải chua.",
        "Bắc chảo với 2 muỗng canh dầu ăn phi thơm hành tím và tỏi băm.",
        "Cho thịt ba chỉ vào đảo đều ở lửa vừa khoảng 8–10 phút đến khi thịt săn lại, ra bớt mỡ thừa và xém vàng cạnh.",
        "Cho kim chi (kèm chút nước kim chi), 1 muỗng cà phê ớt bột và 1 muỗng cà phê đường vào đảo lửa lớn 3–5 phút cho thấm gia vị.",
        "Múc thịt xào ra đĩa, rắc đầu hành lá và ngò rí lên trên, dùng nóng với cơm trắng."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=thit+ba+chi+xao+kim+chi"
    ),

    // 36. Canh Tom Yum hải sản / tôm — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "tom_nau_canh_chua_cay_tom_yum",
      title = "Canh tôm chua cay Tom Yum kiểu Thái",
      emoji = "🦐",
      summary = "Nước canh chua cay mặn ngọt cân bằng hương sả, riềng, lá chanh đặc trưng Thái Lan cùng tôm tươi và nấm.",
      prepMinutes = 10,
      cookMinutes = 15,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.ASIAN_FUSION),
      fridgeIngredients = listOf(ingShrimp("250g"), ingMushroom("150g"), ingTomatoSauce("45g (3 muỗng canh)"), ingWater("800ml")),
      seasonings = listOf(
        sp("Sả đập dập & củ riềng thái lát", "2 cây sả, 4 lát riềng"),
        sp("Lá chanh vò sơ", "4 lá"),
        sp("Nước cốt chanh tươi", "2 muỗng canh"),
        sp("Nước mắm & đường", "2 muỗng canh nước mắm, 1 muỗng cà phê đường"),
        sp("Hành tím, tỏi & ớt hiểm", "1 củ hành, 2 tép tỏi, 1 trái ớt")
      ),
      steps = listOf(
        "Rửa sạch 250g tôm, bóc vỏ rút chỉ lưng (giữ lại đầu/vỏ tôm để xào lấy vị ngọt); nấm cắt gốc ngâm nước muối loãng rồi cắt đôi.",
        "Phi thơm hành tím, tỏi băm cùng 3 muỗng canh sốt cà chua, xào săn phần đầu/vỏ tôm rồi chế 800ml nước vào đun sôi.",
        "Cho sả cây đập dập, riềng thái lát và lá chanh vào nấu khoảng 10 phút cho nước dùng dậy hương thảo mộc rồi vớt bỏ bã vỏ tôm.",
        "Thả nấm và tôm tươi vào nồi nước dùng đang sôi, nêm 2 muỗng canh nước mắm và 1 muỗng cà phê đường nấu 2–3 phút tới khi tôm vừa chín đỏ.",
        "Tắt bếp rồi thêm 2 muỗng canh nước cốt chanh tươi, ngò rí và ớt cắt lát để cân bằng vị chua cay mặn ngọt."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=canh+tom+yum+thai+lan"
    ),

    // 37. Thịt bò xào sốt Bulgogi — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "bo_xao_bulgogi_nam_ca_rot",
      title = "Thịt bò xào sốt Bulgogi nấm",
      emoji = "🥩",
      summary = "Thịt thăn bò thái mỏng ướp xì dầu Hàn Quốc, đường nâu, dầu mè xào lửa lớn mềm ngọt đậm đà.",
      prepMinutes = 15,
      cookMinutes = 12,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.ASIAN_FUSION, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMeat("400g", "Thịt thăn bò"), ingMushroom("120g"), ingEgg("1 quả")),
      seasonings = listOf(
        sp("Xì dầu / nước tương Hàn Quốc", "4 muỗng canh"),
        sp("Đường nâu", "1,5 muỗng canh"),
        sp("Dầu mè", "1,5 muỗng canh"),
        sp("Tỏi băm", "3 tép"),
        sp("Mè rang", "1 muỗng cà phê")
      ),
      steps = listOf(
        "Thịt thăn bò thấm khô, thái lát mỏng vừa ăn; nấm rửa sạch xé nhỏ.",
        "Ướp thịt bò với 4 muỗng canh xì dầu, 1,5 muỗng canh đường nâu, 1,5 muỗng canh dầu mè và tỏi băm trong 20–30 phút cho ngấm vị.",
        "Làm nóng chảo với chút dầu ăn, cho thịt bò đã ướp vào xào nhanh trên lửa lớn để thịt săn mềm giữ được nước ngọt tự nhiên.",
        "Cho tiếp nấm tươi vào đảo đều cùng thịt bò khoảng 2–3 phút cho nấm chín thấm phần sốt Bulgogi.",
        "Trút bò xào ra đĩa, rắc mè rang lên trên và có thể kèm 1 quả trứng ốp la béo ngậy."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=thit+bo+xao+bulgogi"
    ),

    // 38. Cơm chiên kim chi phô mai — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "com_chien_trung_pho_mai_han_quoc",
      title = "Cơm chiên kim chi phô mai",
      emoji = "🍚",
      summary = "Cơm nguội rang tơi cùng kim chi cay dịu và dầu mè, phủ trứng ốp la lòng đào và phô mai Mozzarella kéo sợi.",
      prepMinutes = 8,
      cookMinutes = 12,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.ASIAN_FUSION, CulinaryFilter.VEGETARIAN),
      fridgeIngredients = listOf(ingRice("300g cơm nguội"), ingPickle("100g"), ingCheese("150g"), ingEgg("1 quả")),
      seasonings = listOf(
        sp("Tương ớt Hàn Quốc", "10g (2 muỗng cà phê)"),
        sp("Bột ớt & đường", "1 muỗng cà phê bột ớt, 10g đường"),
        sp("Dầu hào & dầu mè", "10ml mỗi loại"),
        sp("Hành hoa băm nhỏ", "20g")
      ),
      steps = listOf(
        "Cắt nhỏ 100g kim chi (hoặc dưa cải chua) trộn với 10g đường và chút bột ớt; chuẩn bị sẵn 150g phô mai Mozzarella.",
        "Làm nóng chảo với 10ml dầu ăn phi thơm hành hoa, cho phần kim chi đã trộn vào đảo đều cho chín dậy mùi.",
        "Trút 300g cơm nguội cùng 10ml dầu hào, 10g tương ớt Hàn Quốc và 10ml dầu mè vào rang lửa vừa tới khi hạt cơm săn tơi.",
        "Đun chảy nhẹ phô mai ở lửa nhỏ trên chảo chống dính; dùng chảo khác chiên 1 quả trứng ốp la giữ nguyên lòng đỏ.",
        "Múc cơm chiên ra đĩa, đặt trứng ốp la lên trên và rưới phô mai Mozzarella tan chảy xung quanh."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=com+chien+kim+chi+pho+mai"
    ),

    // 39. Gà sốt kem nấm kiểu Âu — Savoury Days
    CulinaryRecipe(
      id = "uc_ga_sot_kem_nam_kieu_au",
      title = "Gà sốt kem nấm bơ sữa kiểu Âu",
      emoji = "🍗",
      summary = "Thịt gà áp chảo nấu sốt kem nấm bơ lạt, sữa tươi và phô mai sánh ngậy theo công thức của Savoury Days.",
      prepMinutes = 10,
      cookMinutes = 18,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN),
      fridgeIngredients = listOf(ingChicken("250g", "Thịt đùi / lườn gà"), ingMushroom("120g"), ingMilk("80ml"), ingCheese("15g")),
      seasonings = listOf(
        sp("Bơ nhạt", "2 thìa canh"),
        sp("Dầu ô liu", "1 thìa canh"),
        sp("Tỏi băm & hành tây băm", "2 tép tỏi, 1/4 củ hành tây"),
        sp("Lá thơm Basil khô (tùy chọn)", "3g"),
        sp("Muối & tiêu xay", "1/2 muỗng cà phê muối, 1/3 muỗng tiêu")
      ),
      steps = listOf(
        "Lọc bỏ xương da 250g thịt gà rồi thái miếng vừa ăn; nấm rửa sạch thái lát dọc dày 3–5mm; băm nhỏ hành tây và tỏi.",
        "Đun chảy bơ nhạt (hoặc dầu ô liu) trong chảo, cho tỏi và hành tây vào phi thơm.",
        "Cho thịt gà vào xào đến khi thịt săn và chuyển màu trắng, tiếp tục cho nấm tươi vào đảo đều cùng chút muối tiêu.",
        "Khi nấm và gà chín tới, hạ nhỏ lửa đổ 80ml sữa tươi vào đảo đều cho hỗn hợp sôi nhẹ.",
        "Thêm 15g phô mai và lá Basil khô khuấy tan tạo độ sánh ngậy, nêm lại muối tiêu vừa ăn rồi dùng nóng với cơm hoặc bánh mì."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = "https://www.savourydays.com/?s=my+y+sot+kem+ga+nam"
    ),

    // 40. Thịt bò áp chảo bơ tỏi — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "bo_ap_chao_bo_toi_tieu_den",
      title = "Thịt bò áp chảo bơ tỏi",
      emoji = "🥩",
      summary = "Thăn bò áp chảo lửa lớn giữ nước ngọt bên trong, rưới bơ lạt tỏi thơm lừng chuẩn vị Âu.",
      prepMinutes = 15,
      cookMinutes = 10,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMeat("350g", "Thịt thăn nội / thăn lưng bò"), ingButter("30g")),
      seasonings = listOf(
        sp("Dầu ô liu", "1–2 muỗng canh"),
        sp("Tỏi đập dập", "2–3 tép"),
        sp("Lá hương thảo Rosemary (tùy chọn)", "2 nhánh"),
        sp("Muối & tiêu xay", "1/2 muỗng cà phê mỗi loại")
      ),
      steps = listOf(
        "Thấm thật khô hai mặt miếng thăn bò dày 2–3cm; ướp đều muối, tiêu xay và chút dầu ô liu rồi để thịt nghỉ ở nhiệt độ phòng 15 phút.",
        "Đặt chảo gang hoặc chảo chống dính lên bếp lửa lớn với 1 muỗng canh dầu ô liu cho đến khi chảo thật nóng.",
        "Cho miếng thịt bò vào áp chảo mỗi mặt 2–3 phút (tùy mức độ chín mong muốn) để tạo lớp vỏ vàng nâu giữ độ mọng bên trong.",
        "Ở phút cuối, hạ nhỏ lửa thả 30g bơ lạt, tỏi đập dập và lá hương thảo vào; nghiêng chảo múc bơ nóng rưới liên tục lên mặt thịt rồi tắt bếp.",
        "Lấy thịt bò ra đĩa để nghỉ 5 phút trước khi thái lát vừa ăn, rưới phần sốt bơ tỏi trong chảo lên mặt."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=thit+bo+ap+chao+bo+toi"
    ),

    // 41. Salad tôm tươi & rau củ giòn mát — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "salad_tom_pho_mai_ot_chuong",
      title = "Salad tôm tươi & rau củ giòn mát",
      emoji = "🥗",
      summary = "Salad thanh mát từ tôm luộc ngọt thịt trộn cùng xà lách, cà rốt, bắp Mỹ giòn ngọt và sốt chanh ô liu.",
      prepMinutes = 10,
      cookMinutes = 10,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingShrimp("200g"), ingLettuce("150g"), ingCarrot("1/2 củ (50g)"), ingCorn("80g hạt bắp")),
      seasonings = listOf(
        sp("Dầu ô liu (hoặc mayonnaise)", "2 muỗng canh"),
        sp("Nước cốt chanh tươi", "1,5 muỗng canh"),
        sp("Mật ong", "1 muỗng cà phê"),
        sp("Muối & tiêu đen", "1/2 muỗng cà phê muối, 1/3 muỗng tiêu")
      ),
      steps = listOf(
        "Rửa sạch tôm tươi, luộc chín khoảng 3 phút tới khi tôm chuyển đỏ săn rồi bóc vỏ rút chỉ lưng để nguội.",
        "Rửa sạch 150g xà lách ngâm nước muối loãng rồi quay ráo, cắt khúc vừa ăn.",
        "Gọt vỏ 50g cà rốt thái sợi; luộc chín 80g hạt bắp ngọt rồi vớt ráo nước.",
        "Khuấy đều 2 muỗng canh dầu ô liu, 1,5 muỗng canh nước cốt chanh, 1 muỗng cà phê mật ong và nhúm muối tiêu thành nước trộn salad.",
        "Cho xà lách, cà rốt, bắp ngọt và tôm vào âu lớn, rưới sốt trộn nhẹ tay rồi bày ra đĩa."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=salad+tom"
    ),

    // 42. Sandwich trứng phô mai & thịt xông khói — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "sandwich_ba_chi_trung_pho_mai",
      title = "Sandwich áp chảo ba chỉ trứng phô mai",
      emoji = "🥪",
      summary = "Bữa sáng sandwich áp chảo nhanh gọn với vỏ bánh vàng giòn, thịt xông khói/ba chỉ giòn thơm, trứng và phô mai tan chảy.",
      prepMinutes = 5,
      cookMinutes = 10,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.WESTERN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingBread("4 lát sandwich"), ingBacon("100g"), ingEgg("2 quả"), ingCheese("40g")),
      seasonings = listOf(
        sp("Tương cà hoặc tương ớt", "1 muỗng canh"),
        sp("Muối & tiêu xay", "1 nhúm nhỏ")
      ),
      steps = listOf(
        "Áp chảo 100g thịt ba chỉ thái mỏng / thịt xông khói trên lửa vừa 3–4 phút cho vàng giòn hai mặt rồi gắp ra.",
        "Đánh tan 2 quả trứng gà với nhúm muối tiêu, đổ vào chảo chống dính lửa nhỏ tạo lớp trứng mỏng.",
        "Khi trứng còn hơi ướt bề mặt, đặt ngay 2 lát bánh mì sandwich lên trên rồi lật nhẹ toàn bộ mặt trứng cùng bánh mì.",
        "Xếp phô mai và thịt ba chỉ giòn lên giữa lát bánh, rưới tương cà rồi gấp mép trứng và úp lát bánh mì vào nhau.",
        "Áp thêm 1 phút mỗi mặt cho phô mai tan chảy và vỏ bánh giòn rụm, cắt đôi dùng nóng."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=sandwich+trung+pho+mai+bacon"
    ),

    // 43. Bông cải xanh xào nấm chay — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "bong_cai_xao_nam_dau_hao_chay",
      title = "Bông cải xanh xào nấm chay",
      emoji = "🥦",
      summary = "Món xào chay thanh đạm từ bông cải xanh chần giòn xào lửa lớn cùng nấm tươi, nước tương và dầu hào chay.",
      prepMinutes = 8,
      cookMinutes = 10,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN, CulinaryFilter.VEGETARIAN),
      fridgeIngredients = listOf(ingBroccoli("250g"), ingMushroom("150g")),
      seasonings = listOf(
        sp("Hạt nêm chay", "1,5 muỗng cà phê"),
        sp("Nước tương & dầu hào chay", "1 muỗng canh mỗi loại"),
        sp("Hành boa-rô / tỏi băm", "2 tép"),
        sp("Dầu ăn & tiêu xay", "1,5 muỗng canh dầu, 1/3 muỗng tiêu")
      ),
      steps = listOf(
        "Cắt bông cải xanh thành miếng vừa ăn, ngâm nước muối loãng rửa sạch rồi chần sơ nước sôi 1,5 phút cho giữ màu xanh giòn.",
        "Nấm cắt gốc, ngâm rửa sạch để ráo rồi xé nhỏ hoặc thái lát vừa ăn.",
        "Phi thơm hành boa-rô (hoặc tỏi băm) với 1,5 muỗng canh dầu ăn rồi cho nấm vào xào săn khoảng 2 phút cùng chút hạt nêm chay.",
        "Trút bông cải xanh vào đảo nhanh tay trên lửa lớn để rau chín giòn không bị nhũn.",
        "Nêm 1 muỗng canh nước tương và 1 muỗng canh dầu hào chay cho vừa miệng, đảo thêm 2 phút rồi tắt bếp rắc tiêu xay."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=bong+cai+xao+nam+chay"
    ),

    // 44. Trứng hấp nấm mềm mịn — VnExpress & Điện máy XANH
    CulinaryRecipe(
      id = "trung_hap_nam_ca_rot_thanh_dam",
      title = "Trứng hấp nấm mềm mịn",
      emoji = "🥚",
      summary = "Trứng gà đánh lọc qua rây hấp cách thủy lửa nhỏ cùng nấm băm tạo kết cấu mịn mướt không rỗ, thơm dầu mè.",
      prepMinutes = 8,
      cookMinutes = 14,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN, CulinaryFilter.VEGETARIAN),
      fridgeIngredients = listOf(ingEgg("3 quả"), ingMushroom("80g"), ingWater("180ml")),
      seasonings = listOf(
        sp("Nước tương (xì dầu)", "1 muỗng canh"),
        sp("Dầu mè", "1/2 muỗng canh"),
        sp("Muối / hạt nêm & tiêu", "1/3 muỗng cà phê muối, chút tiêu"),
        sp("Hành lá cắt nhỏ", "1 nhánh")
      ),
      steps = listOf(
        "Nấm ngâm nước muối loãng rửa sạch rồi băm nhỏ hoặc thái hạt lựu; hành lá cắt nhỏ.",
        "Đập 3 quả trứng gà vào tô, thêm 180ml nước ấm và 1/3 muỗng cà phê muối đánh đều nhẹ tay rồi lọc qua rây cho mịn không bọt.",
        "Cho nấm băm vào tô trứng khuấy đều nhẹ nhàng rồi đậy kín mặt tô.",
        "Đặt vào xửng hấp cách thủy ở lửa nhỏ trong 10–14 phút; dùng tăm xăm thử giữa khối trứng không dính nước là chín.",
        "Lấy tô trứng hấp ra, rưới dầu mè và nước tương lên trên cùng hành lá và chút tiêu xay."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=trung+hap+nam"
    ),

    // 45. Canh rau củ thập cẩm chay — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "canh_rau_cu_nam_ngot_thanh",
      title = "Canh rau củ thập cẩm nấm chay",
      emoji = "🥕",
      summary = "Canh chay ngọt nước tự nhiên từ bắp Mỹ, cà rốt, bông cải xanh và nấm tươi ít dầu mỡ.",
      prepMinutes = 10,
      cookMinutes = 18,
      servingsLabel = "3–4 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN, CulinaryFilter.VEGETARIAN),
      fridgeIngredients = listOf(ingCorn("1 trái (150g)"), ingCarrot("1 củ (100g)"), ingBroccoli("150g"), ingMushroom("120g")),
      seasonings = listOf(
        sp("Nước lọc", "900ml"),
        sp("Hạt nêm chay", "1,5 muỗng cà phê"),
        sp("Muối", "1 muỗng cà phê"),
        sp("Hành boa-rô & ngò rí", "2 nhánh")
      ),
      steps = listOf(
        "Cà rốt gọt vỏ tỉa khoanh vừa ăn; bắp Mỹ cắt khúc; bông cải xanh tách nhánh rửa sạch; nấm ngâm nước muối loãng để ráo.",
        "Đun sôi 900ml nước, cho bắp Mỹ và cà rốt vào nấu trước khoảng 10 phút để lấy nước dùng ngọt thanh tự nhiên.",
        "Nêm 1 muỗng cà phê muối và 1,5 muỗng cà phê hạt nêm chay cho vừa khẩu vị.",
        "Cho tiếp nấm và bông cải xanh vào nấu thêm 4–5 phút tới khi rau củ chín tới vẫn giữ được độ giòn ngọt.",
        "Tắt bếp, thêm hành boa-rô phi thơm hoặc ngò rí thái khúc rồi múc ra tô dùng nóng."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=canh+rau+cu+thap+cam+chay"
    ),

    // 46. Salad trái cây sữa chua thanh mát — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "salad_trai_cay_ngu_sac_sua_chua",
      title = "Salad trái cây sữa chua thanh mát",
      emoji = "🍎",
      summary = "Salad táo, dâu tây và xà lách giòn mát trộn sốt sữa chua, nước cốt chanh và mật ong thanh nhẹ.",
      prepMinutes = 10,
      cookMinutes = 5,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.EAT_CLEAN, CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingApple("1 quả (150g)"), ingStrawberry("100g"), ingLettuce("100g"), ingYogurt("1 hũ (100g)")),
      seasonings = listOf(
        sp("Mật ong (hoặc sữa đặc)", "1,5 muỗng canh"),
        sp("Nước cốt chanh tươi", "1 muỗng cà phê")
      ),
      steps = listOf(
        "Rửa sạch táo, dâu tây và xà lách với nước muối loãng rồi để thật ráo nước.",
        "Thái hạt lựu táo, cắt đôi dâu tây và cắt khúc vừa ăn phần xà lách.",
        "Pha phần sốt: trộn đều 1 hũ sữa chua không đường cùng 1,5 muỗng canh mật ong (hoặc sữa đặc) và vài giọt nước cốt chanh.",
        "Cho toàn bộ trái cây và rau vào tô lớn, rưới sốt sữa chua lên trên rồi xóc trộn nhẹ nhàng để không bị nát.",
        "Có thể ướp trong ngăn mát tủ lạnh khoảng 20–30 phút trước khi thưởng thức để món salad mát lạnh hơn."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=salad+trai+cay+sua+chua"
    ),

    // 47. Canh chua tôm cà chua thanh mát — VnExpress & Điện máy XANH
    CulinaryRecipe(
      id = "canh_chua_tom_ca_chua_bap",
      title = "Canh chua tôm cà chua & bắp ngọt",
      emoji = "🍲",
      summary = "Canh chua thanh mát hài hòa vị chua ngọt từ tôm tươi, sốt cà chua và bắp Mỹ giải nhiệt cho bữa cơm Việt.",
      prepMinutes = 8,
      cookMinutes = 15,
      servingsLabel = "2–3 người ăn",
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingShrimp("200g"), ingTomatoSauce("60g (4 muỗng canh)"), ingCorn("1 trái (150g)"), ingWater("800ml")),
      seasonings = listOf(
        sp("Hành tím băm", "1 củ"),
        sp("Nước cốt chanh hoặc me", "1,5 muỗng canh"),
        sp("Nước mắm & đường", "1,5 muỗng canh nước mắm, 1,5 muỗng cà phê đường"),
        sp("Ngò gai & hành lá", "3 nhánh")
      ),
      steps = listOf(
        "Tôm tươi bóc vỏ rút chỉ lưng rửa sạch; bắp ngọt cắt khúc vừa ăn; hành lá và ngò gai thái nhỏ.",
        "Phi thơm hành tím băm với chút dầu ăn, cho 4 muỗng canh sốt cà chua vào xào 1 phút cho lên màu đẹp.",
        "Đổ 800ml nước vào nồi đun sôi rồi cho bắp ngọt vào nấu 7 phút cho ngọt nước.",
        "Nêm nước mắm, đường và chút muối vừa ăn, thả tôm tươi vào nấu sôi 2–3 phút cho tôm chín đỏ.",
        "Tắt bếp rồi thêm nước cốt chanh cân bằng vị chua thanh, rắc ngò gai và hành lá lên mặt."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = "https://timkiem.vnexpress.net/?q=canh+chua+tom"
    ),

    // 48. Bánh mì nướng bơ sữa — Điện máy XANH Vào Bếp
    CulinaryRecipe(
      id = "banh_mi_nuong_bo_sua_gion",
      title = "Bánh mì nướng bơ sữa",
      emoji = "🥖",
      summary = "Bánh mì ép dẹt phết bơ lạt và sữa tươi nướng giòn tan thơm béo bằng lò nướng hoặc nồi chiên không dầu.",
      prepMinutes = 5,
      cookMinutes = 8,
      servingsLabel = "2 người ăn",
      categories = setOf(CulinaryFilter.VEGETARIAN, CulinaryFilter.BREAKFAST_DRINK),
      fridgeIngredients = listOf(ingBread("2 ổ (160g)"), ingButter("25g"), ingMilk("40ml")),
      seasonings = listOf(
        sp("Sữa đặc hoặc đường", "1,5 muỗng canh"),
        sp("Mè rang (tùy chọn)", "1 muỗng cà phê")
      ),
      steps = listOf(
        "Xẻ dọc 2 ổ bánh mì và dùng chày hoặc muỗng lớn ép nhẹ cho bánh hơi dẹt để dễ phết bơ thấm đều.",
        "Làm tan chảy 25g bơ lạt rồi khuấy quyện cùng 40ml sữa tươi và 1,5 muỗng canh sữa đặc (hoặc đường).",
        "Dùng cọ phết đều hỗn hợp bơ sữa lên cả hai mặt bánh mì.",
        "Nướng bánh trong nồi chiên không dầu hoặc lò nướng ở 175°C khoảng 6–8 phút (lật mặt giữa giờ) đến khi vàng giòn.",
        "Lấy bánh ra rắc thêm mè rang nếu thích và thưởng thức ngay khi còn giòn rụm."
      ),
      sourceName = SRC_DMX,
      sourceUrl = "https://www.dienmayxanh.com/kinh-nghiem-hay/tim-kiem?q=banh+mi+nuong+bo+sua"
    )
  )
}
