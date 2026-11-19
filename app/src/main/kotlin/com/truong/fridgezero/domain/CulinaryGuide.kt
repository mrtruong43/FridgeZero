package com.truong.fridgezero.domain

import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.ui.icons.FoodGroup
import java.text.Normalizer
import java.util.Locale

enum class CulinaryFilter(val labelVi: String) {
  ALL("Tất cả"),
  READY_NOW("Đủ đồ nấu ngay"),
  RESCUE_URGENT("Cứu đồ sắp hết hạn"),
  QUICK_15("Nhanh ≤ 15p"),
  VIET_HOME("Cơm nhà Việt"),
  EAT_CLEAN("Eat-Clean")
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
  val cookMinutes: Int,
  val servings: Int = 2,
  val categories: Set<CulinaryFilter>,
  val fridgeIngredients: List<RecipeIngredient>,
  val pantryStaples: List<String>,
  val steps: List<String>,
  val sourceName: String,
  val sourceUrl: String,
  val difficulty: RecipeDifficulty = if (cookMinutes <= 15) RecipeDifficulty.EASY else RecipeDifficulty.MEDIUM
)

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
 * 3-layer ingredient matcher and expiry-rescue ranker:
 * Layer 1: Exact `iconKey` -> Layer 2: Vietnamese name keyword -> Layer 3: `FoodGroup` fallback.
 */
object CulinaryGuideEngine {

  const val URGENT_DAYS_THRESHOLD = 1
  const val SCORE_URGENT_MATCH = 15
  const val SCORE_FRESH_MATCH = 6
  const val SCORE_MISSING_PENALTY = -3
  const val SCORE_READY_BONUS = 8

  private val VI_LOCALE = Locale.forLanguageTag("vi-VN")
  private val NON_WORD_REGEX = Regex("[^\\p{L}\\p{Nd}]+")
  private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

  fun recommend(
    fridgeItems: List<DisplayItem>,
    filter: CulinaryFilter = CulinaryFilter.ALL,
    recipes: List<CulinaryRecipe> = CulinaryGuideRepository.recipes
  ): List<RecipeRecommendation> {
    return recipes
      .map { evaluate(it, fridgeItems) }
      .filter { rec ->
        when (filter) {
          CulinaryFilter.ALL -> true
          CulinaryFilter.READY_NOW -> rec.isReadyToCook
          CulinaryFilter.RESCUE_URGENT -> rec.hasUrgentMatch
          CulinaryFilter.QUICK_15 ->
            rec.recipe.cookMinutes <= 15 || CulinaryFilter.QUICK_15 in rec.recipe.categories
          CulinaryFilter.VIET_HOME -> CulinaryFilter.VIET_HOME in rec.recipe.categories
          CulinaryFilter.EAT_CLEAN -> CulinaryFilter.EAT_CLEAN in rec.recipe.categories
        }
      }
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
        val consumed = minOf(available, need)
        remainingQty[id] = available - consumed
        need -= consumed
      }
    }

    return remainingQty.entries.mapNotNull { (id, remaining) ->
      val initial = originalQty[id] ?: return@mapNotNull null
      if (remaining < initial) InventoryDeduction(itemId = id, remainingQuantity = remaining) else null
    }
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

  private fun normalizeAccented(text: String): String {
    val cleaned = text.lowercase(VI_LOCALE).replace(NON_WORD_REGEX, " ").trim()
    return if (cleaned.isEmpty()) "" else " $cleaned "
  }

  private fun stripVietnameseAccents(text: String): String {
    val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
    val withoutMarks = DIACRITICS_REGEX.replace(nfd, "")
      .replace('đ', 'd')
      .replace('Đ', 'D')
    return normalizeAccented(withoutMarks)
  }
}

/**
 * Curated catalog of 36 practical Vietnamese home-cooking & Eat-Clean recipes
 * covering all 32 `EmojiIconRepository` icon keys.
 */
object CulinaryGuideRepository {

  private const val SRC_SAVOURY_DAYS = "Savoury Days"
  private const val URL_SAVOURY_DAYS = "https://www.savourydays.com/"
  private const val SRC_COOKY = "Cooky"
  private const val URL_COOKY = "https://www.cooky.vn/cach-lam"
  private const val SRC_VNEXPRESS = "VnExpress Ẩm Thực"
  private const val URL_VNEXPRESS = "https://vnexpress.net/doi-song/cooking"
  private const val SRC_DMX = "Điện máy XANH Vào Bếp"
  private const val URL_DMX = "https://www.dienmayxanh.com/vao-bep"
  private const val SRC_SERIOUS_EATS = "Serious Eats"
  private const val URL_SERIOUS_EATS = "https://www.seriouseats.com/"

  private fun ingChicken(amount: String = "300g") = RecipeIngredient(
    name = "Thịt gà / Ức gà",
    iconKeys = listOf("chicken"),
    keywords = listOf("thịt gà", "ức gà", "đùi gà", "cánh gà", "gà"),
    fallbackGroup = FoodGroup.THIT,
    amountText = amount
  )

  private fun ingMeat(amount: String = "250g", name: String = "Thịt bò / Thịt nạc") = RecipeIngredient(
    name = name,
    iconKeys = listOf("meat"),
    keywords = listOf("thịt bò", "thịt heo", "thịt lợn", "nạc vai", "thăn bò", "thịt bằm", "bò", "heo"),
    fallbackGroup = FoodGroup.THIT,
    amountText = amount
  )

  private fun ingBacon(amount: String = "200g") = RecipeIngredient(
    name = "Thịt ba chỉ",
    iconKeys = listOf("bacon"),
    keywords = listOf("ba chỉ", "ba rọi", "thịt ba chỉ", "bacon"),
    fallbackGroup = FoodGroup.THIT,
    amountText = amount
  )

  private fun ingSalmon(amount: String = "250g") = RecipeIngredient(
    name = "Cá hồi",
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
    name = "Cua / Thịt cua",
    iconKeys = listOf("crab"),
    keywords = listOf("thịt cua", "cua", "cua biển", "ghẹ"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingSquid(amount: String = "250g") = RecipeIngredient(
    name = "Mực tươi",
    iconKeys = listOf("squid"),
    keywords = listOf("mực", "mực ống", "mực lá", "mực tươi"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingLettuce(amount: String = "1 bó") = RecipeIngredient(
    name = "Rau xà lách",
    iconKeys = listOf("lettuce"),
    keywords = listOf("xà lách", "rau xà lách", "rau cải", "rau xanh"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingCarrot(amount: String = "1 củ") = RecipeIngredient(
    name = "Cà rốt",
    iconKeys = listOf("carrot"),
    keywords = listOf("cà rốt", "carrot"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingBellPepper(amount: String = "1 quả") = RecipeIngredient(
    name = "Ớt chuông",
    iconKeys = listOf("bell_pepper"),
    keywords = listOf("ớt chuông", "ớt ngọt", "ớt chuông đỏ"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingBroccoli(amount: String = "1 bông") = RecipeIngredient(
    name = "Bông cải xanh",
    iconKeys = listOf("broccoli"),
    keywords = listOf("bông cải", "súp lơ", "bông cải xanh"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingCorn(amount: String = "1 trái") = RecipeIngredient(
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

  private fun ingPickle(amount: String = "100g") = RecipeIngredient(
    name = "Dưa muối / Dưa chuột",
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

  private fun ingMilk(amount: String = "150ml") = RecipeIngredient(
    name = "Sữa tươi",
    iconKeys = listOf("milk"),
    keywords = listOf("sữa tươi", "sữa không đường", "sữa"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingCheese(amount: String = "2 lát") = RecipeIngredient(
    name = "Phô mai",
    iconKeys = listOf("cheese"),
    keywords = listOf("phô mai", "cheddar", "mozzarella", "cheese"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingYogurt(amount: String = "1 hũ") = RecipeIngredient(
    name = "Sữa chua",
    iconKeys = listOf("yogurt"),
    keywords = listOf("sữa chua", "sữa chua không đường", "yogurt"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingButter(amount: String = "15g") = RecipeIngredient(
    name = "Bơ",
    iconKeys = listOf("butter"),
    keywords = listOf("bơ lạt", "bơ mặn", "bơ thực vật", "bơ"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingOrangeJuice(amount: String = "120ml") = RecipeIngredient(
    name = "Nước cam",
    iconKeys = listOf("orange_juice"),
    keywords = listOf("nước cam", "cam ép", "nước ép cam"),
    fallbackGroup = FoodGroup.DO_UONG,
    amountText = amount
  )

  private fun ingWater(amount: String = "300ml") = RecipeIngredient(
    name = "Nước suối",
    iconKeys = listOf("water"),
    keywords = listOf("nước suối", "nước lọc", "nước tinh khiết"),
    fallbackGroup = FoodGroup.DO_UONG,
    amountText = amount
  )

  private fun ingTomatoSauce(amount: String = "3 muỗng") = RecipeIngredient(
    name = "Sốt cà",
    iconKeys = listOf("tomato_sauce"),
    keywords = listOf("sốt cà", "sốt cà chua", "tương cà", "cà chua cô đặc"),
    fallbackGroup = FoodGroup.DO_KHO,
    amountText = amount
  )

  private fun ingBread(amount: String = "2 ổ / lát") = RecipeIngredient(
    name = "Bánh mì",
    iconKeys = listOf("bread"),
    keywords = listOf("bánh mì", "bánh mì gối", "sandwich", "baguette"),
    fallbackGroup = FoodGroup.DO_KHO,
    amountText = amount
  )

  private fun ingRice(amount: String = "2 chén") = RecipeIngredient(
    name = "Cơm nguội / Gạo",
    iconKeys = listOf("rice"),
    keywords = listOf("cơm", "cơm nguội", "gạo", "gạo tẻ"),
    fallbackGroup = FoodGroup.DO_KHO,
    amountText = amount
  )

  private fun ingDumpling(amount: String = "3 cái") = RecipeIngredient(
    name = "Bánh bao",
    iconKeys = listOf("dumpling"),
    keywords = listOf("bánh bao", "há cảo", "sủi cảo"),
    fallbackGroup = FoodGroup.AN_SAN,
    amountText = amount
  )

  private fun ingIceCream(amount: String = "2 viên") = RecipeIngredient(
    name = "Kem lạnh",
    iconKeys = listOf("ice_cream"),
    keywords = listOf("kem", "kem vani", "kem tươi"),
    fallbackGroup = FoodGroup.AN_SAN,
    amountText = amount
  )

  private fun ingApple(amount: String = "1 quả") = RecipeIngredient(
    name = "Táo",
    iconKeys = listOf("apple"),
    keywords = listOf("táo", "táo đỏ", "táo xanh"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingBanana(amount: String = "1 quả") = RecipeIngredient(
    name = "Chuối",
    iconKeys = listOf("banana"),
    keywords = listOf("chuối", "chuối chín", "chuối cau"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingGrape(amount: String = "100g") = RecipeIngredient(
    name = "Nho tươi",
    iconKeys = listOf("grape"),
    keywords = listOf("nho", "nho xanh", "nho đỏ", "nho đen"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingStrawberry(amount: String = "100g") = RecipeIngredient(
    name = "Dâu tây",
    iconKeys = listOf("strawberry"),
    keywords = listOf("dâu tây", "dâu", "strawberry"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingWatermelon(amount: String = "300g") = RecipeIngredient(
    name = "Dưa hấu",
    iconKeys = listOf("watermelon"),
    keywords = listOf("dưa hấu", "ruột dưa hấu"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingMango(amount: String = "1 quả") = RecipeIngredient(
    name = "Xoài",
    iconKeys = listOf("mango"),
    keywords = listOf("xoài", "xoài xanh", "xoài chín"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  val recipes: List<CulinaryRecipe> = listOf(
    // 1–12: Ready out-of-the-box with Seed.kt and prioritize rescuing salmon (1d), chicken (1d), orange_juice (2d), lettuce (2d), ice_cream (3d).
    CulinaryRecipe(
      id = "ga_ap_chao_sot_cam_xa_lach",
      title = "Gà áp chảo sốt cam mật ong & xà lách",
      emoji = "🍗",
      summary = "Ức gà áp chảo vàng cạnh quyện sốt cam chua ngọt dịu nhẹ, ăn cùng xà lách giòn mát.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken(), ingOrangeJuice(), ingLettuce(), ingButter("10g")),
      pantryStaples = listOf("Tỏi băm", "Mật ong", "Muối", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thấm khô thịt gà, ướp với chút muối, tiêu và tỏi băm trong 5 phút.",
        "Làm nóng chảo với dầu ăn, áp chảo gà mỗi mặt 4 phút cho chín vàng đều rồi gắp ra.",
        "Hạ lửa nhỏ, cho bơ, nước cam và 1 muỗng mật ong vào chảo khuấy tới khi sốt hơi sánh.",
        "Xếp xà lách rửa sạch ra đĩa, thái lát gà đặt lên trên và rưới sốt cam ấm nóng."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "ca_hoi_sot_cam_bo_toi",
      title = "Cá hồi áp chảo sốt cam bơ tỏi & xà lách",
      emoji = "🐟",
      summary = "Cá hồi da giòn thấm bơ tỏi kết hợp sốt cam thơm thanh, chuẩn bữa Eat-Clean nhanh gọn.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSalmon(), ingOrangeJuice("100ml"), ingButter("15g"), ingLettuce()),
      pantryStaples = listOf("Tỏi đập dập", "Muối", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thấm thật ráo miếng cá hồi rồi rắc đều một lớp muối mỏng và tiêu lên hai mặt.",
        "Áp chảo mặt da trước trên lửa vừa khoảng 3 phút cho giòn, lật mặt áp tiếp 2 phút.",
        "Cho bơ và tỏi đập dập vào chảo rưới đều lên thân cá, sau đó gắp cá đặt lên đĩa xà lách.",
        "Đổ nước cam vào phần bơ tỏi còn lại, đun sôi nhẹ 1 phút cho sánh rồi chan lên cá."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = URL_SERIOUS_EATS
    ),
    CulinaryRecipe(
      id = "ca_hoi_ap_chao_bo_toi_salad",
      title = "Cá hồi áp chảo bơ tỏi kèm salad cà rốt",
      emoji = "🥗",
      summary = "Phi lê cá hồi béo ngậy bơ tỏi ăn kèm xà lách và cà rốt bào sợi cân vị.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSalmon(), ingButter("15g"), ingLettuce(), ingCarrot()),
      pantryStaples = listOf("Tỏi băm", "Nước cốt chanh", "Muối", "Tiêu xay", "Dầu ô liu"),
      steps = listOf(
        "Ướp cá hồi với muối tiêu trong 5 phút; xà lách cắt vừa ăn, cà rốt bào sợi mảnh.",
        "Áp chảo cá hồi với ít dầu cho vàng hai mặt rồi thả bơ và tỏi băm vào dậy mùi thơm.",
        "Trộn xà lách và cà rốt với dầu ô liu, nước cốt chanh và chút muối tiêu làm salad nền.",
        "Đặt cá hồi lên trên đĩa salad, rưới thêm bơ tỏi nóng và thưởng thức ngay."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "salad_ga_xe_sua_chua",
      title = "Salad ức gà xé xà lách sốt sữa chua",
      emoji = "🥗",
      summary = "Xốt sữa chua chua dịu thay mayonnaise giúp đĩa salad gà thanh đạm mà vẫn đậm vị.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken("250g"), ingLettuce(), ingYogurt(), ingCarrot()),
      pantryStaples = listOf("Nước cốt chanh", "Mật ong", "Muối", "Tiêu xay", "Tỏi băm"),
      steps = listOf(
        "Luộc hoặc áp chảo chín ức gà với nhúm muối, để nguội bớt rồi xé thành sợi vừa ăn.",
        "Rửa sạch xà lách để ráo, cà rốt thái sợi mỏng hoặc hạt lựu nhỏ.",
        "Khuấy đều sữa chua với 1 muỗng mật ong, nước cốt chanh, muối và tiêu làm xốt trộn.",
        "Cho gà xé, xà lách, cà rốt vào tô lớn, rưới xốt sữa chua và đảo nhẹ trước khi ăn."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "ga_xao_ot_chuong_ca_rot",
      title = "Ức gà xào ớt chuông cà rốt kiểu cơm nhà",
      emoji = "🫑",
      summary = "Món xào nhiều màu sắc giàu đạm, thịt gà mềm ngọt hòa cùng ớt chuông giòn tươi.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken(), ingBellPepper(), ingCarrot()),
      pantryStaples = listOf("Hành tỏi băm", "Dầu hào", "Nước mắm", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thái gà miếng vừa ăn, ướp nhanh với tỏi băm, 1 muỗng dầu hào và nửa muỗng tiêu.",
        "Thái vuông ớt chuông và tỉa lát mỏng cà rốt để khi xào chín đồng đều.",
        "Xào nhanh thịt gà trên lửa lớn cho săn chín khoảng 80% rồi trút ra đĩa.",
        "Phi thơm hành tỏi, xào cà rốt và ớt chuông 2 phút rồi trút gà vào đảo chung, nêm nước mắm vừa ăn."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "salad_ca_hoi_dua_muoi",
      title = "Salad cá hồi áp chảo dưa muối thanh vị",
      emoji = "🥬",
      summary = "Chút chua giòn của dưa muối kết hợp cá hồi béo thơm giúp chống ngấy cực hiệu quả.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSalmon(), ingLettuce(), ingPickle("80g"), ingCarrot()),
      pantryStaples = listOf("Dầu ô liu", "Đường", "Muối", "Tiêu xay", "Hành tím"),
      steps = listOf(
        "Vắt nhẹ dưa muối cho ráo nước và thái nhỏ vừa ăn; cà rốt bào sợi, xà lách cắt khúc.",
        "Áp chảo cá hồi với muối tiêu đến khi chín vàng hai mặt, tách miếng vừa gắp.",
        "Pha nhanh dầu ô liu, chút đường và tiêu thành nước trộn salad cân vị.",
        "Xếp xà lách, cà rốt, dưa muối ra tô, đặt cá hồi lên trên và rưới sốt trộn đều."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "kem_sua_chua_cam_tuoi",
      title = "Kem sữa chua sốt cam tươi mát lạnh",
      emoji = "🍨",
      summary = "Tráng miệng 5 phút giải cứu ngay hộp kem lạnh, nước cam và sữa chua trong tủ.",
      cookMinutes = 5,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingIceCream(), ingOrangeJuice("80ml"), ingYogurt()),
      pantryStaples = listOf("Mật ong", "Lá bạc hà hoặc vỏ cam bào"),
      steps = listOf(
        "Đun ấm nước cam với nửa muỗng mật ong trong 2 phút cho thơm nhẹ rồi để nguội.",
        "Múc sữa chua ra ly hoặc chén lạnh làm lớp nền mịn màng.",
        "Thêm 2 viên kem lạnh lên trên phần sữa chua.",
        "Rưới nước sốt cam nguội lên mặt kem, trang trí thêm vỏ cam bào và thưởng thức ngay."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "trung_chien_pho_mai_sot_ca",
      title = "Trứng chiên phô mai sốt cà chua đậm đà",
      emoji = "🍳",
      summary = "Trứng xốp thơm mùi bơ kẹp nhân phô mai tan chảy và sốt cà chua bắt cơm.",
      cookMinutes = 12,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingEgg("3 quả"), ingCheese(), ingTomatoSauce(), ingButter("10g")),
      pantryStaples = listOf("Hành lá", "Nước mắm", "Đường", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Đánh tan trứng với hành lá cắt nhỏ, nửa muỗng nước mắm và chút tiêu xay.",
        "Đun chảy bơ trên chảo chống dính lửa nhỏ, đổ trứng vào tráng mỏng đều mặt chảo.",
        "Đặt phô mai lên một nửa mặt trứng, rưới sốt cà pha chút đường rồi gập đôi miếng trứng.",
        "Chiên thêm 1 phút cho phô mai chảy đều, rưới phần sốt cà còn lại lên mặt và rắc tiêu."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "tom_rim_bo_toi_sot_ca",
      title = "Tôm rim bơ tỏi sốt cà ăn kèm dưa muối",
      emoji = "🦐",
      summary = "Tôm rim săn chắc áo lớp sốt cà bơ tỏi bóng đẹp, ăn cùng cơm trắng và dưa muối cực đưa cơm.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingShrimp(), ingButter("15g"), ingTomatoSauce(), ingPickle()),
      pantryStaples = listOf("Tỏi băm", "Nước mắm", "Đường", "Tiêu xay", "Hành lá"),
      steps = listOf(
        "Cắt bỏ râu tôm, rút chỉ lưng, rửa sạch để ráo và xóc nhẹ với chút tiêu.",
        "Đun chảy bơ cùng tỏi băm cho dậy thơm rồi cho tôm vào đảo nhanh trên lửa lớn.",
        "Khi tôm chuyển đỏ săn lại, thêm sốt cà, 1 muỗng nước mắm và 1 muỗng nhỏ đường vào rim.",
        "Rim 3 phút đến khi sốt sệt bám đều vỏ tôm, rắc hành lá và dọn kèm đĩa dưa muối."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "trung_bac_sua_tuoi_pho_mai",
      title = "Trứng bác sữa tươi phô mai béo mịn",
      emoji = "🥚",
      summary = "Bữa sáng mềm mịn kiểu Tây nhờ đánh trứng với sữa tươi và nấu chậm cùng bơ phô mai.",
      cookMinutes = 10,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingEgg("3 quả"), ingMilk("60ml"), ingCheese(), ingButter("15g")),
      pantryStaples = listOf("Muối", "Tiêu đen", "Hành lá hoặc ngò rí"),
      steps = listOf(
        "Đập trứng vào tô, thêm sữa tươi và nhúm muối rồi đánh đều cho tan quyện.",
        "Làm tan chảy bơ ở lửa thật nhỏ trên chảo chống dính.",
        "Đổ trứng vào, dùng phới đảo nhẹ liên tục từ ngoài vào trong để tạo các lớp trứng mềm mịn.",
        "Khi trứng còn hơi ướt thì tắt bếp, thả phô mai xé nhỏ vào đảo tan và rắc tiêu đen."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = URL_SERIOUS_EATS
    ),
    CulinaryRecipe(
      id = "banh_bao_chien_sot_ca_dua_muoi",
      title = "Bánh bao chiên giòn chấm sốt cà & dưa muối",
      emoji = "🥟",
      summary = "Bánh bao vỏ giòn rụm nóng hổi chấm sốt cà pha đậm vị và kèm dưa muối chống ngấy.",
      cookMinutes = 12,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingDumpling("4 cái"), ingTomatoSauce(), ingPickle("80g")),
      pantryStaples = listOf("Dầu ăn", "Tỏi băm", "Đường", "Xì dầu", "Tương ớt"),
      steps = listOf(
        "Cho bánh bao vào chảo dầu nóng vừa (hoặc nồi chiên không dầu), chiên vàng giòn các mặt.",
        "Trong lúc đó phi thơm tỏi băm, cho sốt cà, chút xì dầu và nửa muỗng đường vào đảo sánh.",
        "Vớt bánh bao ra giấy thấm dầu và cắt đôi cho dễ thưởng thức.",
        "Dọn bánh bao nóng kèm chén sốt cà ấm và dưa muối thái vừa ăn."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "tom_xao_ot_chuong_ca_rot",
      title = "Tôm tươi xào ớt chuông cà rốt giòn ngọt",
      emoji = "🦐",
      summary = "Món xào cơm nhà ít dầu mỡ, giữ nguyên vị ngọt giòn tự nhiên của tôm và rau củ.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingShrimp(), ingBellPepper(), ingCarrot()),
      pantryStaples = listOf("Tỏi băm", "Dầu hào", "Hành lá", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Tôm bóc vỏ chừa đuôi, rút chỉ lưng, ướp nhanh với chút tỏi băm và tiêu.",
        "Cà rốt tỉa lát mỏng, ớt chuông bỏ hạt thái miếng vuông vừa đũa.",
        "Xào tôm trên lửa lớn 2 phút đến khi vừa săn cong lại thì trút ra bát.",
        "Xào cà rốt và ớt chuông 2 phút, trút tôm vào đảo cùng 1 muỗng dầu hào và rắc hành lá."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),

    // 13–36: Practical Vietnamese home-cooking & Eat-Clean coverage for all remaining iconKeys.
    CulinaryRecipe(
      id = "ba_chi_rang_chay_canh_dua_muoi",
      title = "Thịt ba chỉ rang cháy cạnh kèm dưa muối & cơm",
      emoji = "🥓",
      summary = "Ba chỉ xém vàng thơm lừng mắm hành, ăn cùng dưa cải muối chua và cơm nóng chuẩn vị.",
      cookMinutes = 20,
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingBacon("300g"), ingPickle("150g"), ingRice()),
      pantryStaples = listOf("Hành tím băm", "Nước mắm", "Đường", "Hành lá", "Tiêu xay"),
      steps = listOf(
        "Thịt ba chỉ thái miếng mỏng vừa ăn, chần sơ nước sôi 1 phút rồi để ráo.",
        "Đặt chảo nóng, đảo thịt trên lửa vừa đến khi ra mỡ và các cạnh cháy xém vàng.",
        "Chắt bớt mỡ thừa, phi thơm hành tím rồi nêm nước mắm, chút đường đảo thấm đều.",
        "Rắc hành lá, tiêu và dọn ăn nóng cùng dưa muối và cơm trắng."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "bo_xao_bong_cai_ca_rot",
      title = "Thịt bò xào bông cải xanh cà rốt",
      emoji = "🥦",
      summary = "Thịt bò mềm mọng xào lửa lớn với bông cải xanh và cà rốt giòn giữ trọn vitamin.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMeat("250g", "Thịt bò"), ingBroccoli(), ingCarrot()),
      pantryStaples = listOf("Tỏi băm", "Dầu hào", "Xì dầu", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thái mỏng thịt bò ngang thớ, ướp 1 muỗng dầu hào, chút xì dầu, tiêu và nửa phần tỏi băm.",
        "Cắt nhỏ bông cải xanh và thái lát cà rốt, chần qua nước sôi 45 giây rồi vớt ngâm nước mát.",
        "Làm nóng chảo thật già dầu, xào thịt bò thật nhanh tay trên lửa lớn 1 phút rồi trút ra.",
        "Xào thơm phần tỏi còn lại với bông cải, cà rốt rồi đổ thịt bò vào đảo chung 30 giây."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "bo_xao_ot_chuong_nam",
      title = "Thịt bò xào ớt chuông nấm hương",
      emoji = "🥩",
      summary = "Nấm ngọt nước quyện vị thịt bò mềm và ớt chuông thơm lừng cho bữa tối gọn lẹ.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingMeat("250g", "Thịt bò"), ingBellPepper(), ingMushroom()),
      pantryStaples = listOf("Tỏi băm", "Dầu hào", "Xì dầu", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thịt bò thái lát mỏng ướp nhanh với xì dầu, tỏi băm và tiêu trong 5 phút.",
        "Nấm làm sạch cắt đôi; ớt chuông bỏ hạt thái miếng vuông vừa ăn.",
        "Xào thịt bò trên lửa lớn cho chín tái rồi vớt ra đĩa riêng.",
        "Xào nấm và ớt chuông với chút dầu hào 2 phút rồi trút bò vào đảo đều, rắc tiêu và tắt bếp."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "muc_xao_ot_chuong_nam",
      title = "Mực xào ớt chuông nấm & cà rốt giòn ngọt",
      emoji = "🦑",
      summary = "Mực khứa vảy rồng giòn sần sật xào nhanh cùng ớt chuông, nấm và cà rốt không ra nước.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSquid(), ingBellPepper(), ingMushroom("100g"), ingCarrot()),
      pantryStaples = listOf("Gừng thái lát", "Tỏi băm", "Dầu hào", "Nước mắm", "Hành lá"),
      steps = listOf(
        "Làm sạch mực, khứa caro nhẹ và cắt miếng vừa, chần nhanh qua nước sôi có vài lát gừng.",
        "Cắt miếng vừa ăn ớt chuông, cà rốt và nấm tươi.",
        "Phi thơm tỏi, xào mực trên lửa lớn 1 phút với nửa muỗng dầu hào rồi vớt ra.",
        "Xào rau củ 2 phút cho chín tới, trút mực vào đảo lại cùng hành lá và chút nước mắm."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "muc_hap_gung_cuon_xa_lach",
      title = "Mực hấp gừng sả cuốn xà lách",
      emoji = "🦑",
      summary = "Mực hấp giữ nguyên độ giòn ngọt tự nhiên, cuốn xà lách cà rốt nhẹ bụng ít calo.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSquid("300g"), ingLettuce(), ingCarrot()),
      pantryStaples = listOf("Gừng thái sợi", "Sả đập dập", "Nước mắm", "Chanh", "Tỏi ớt"),
      steps = listOf(
        "Rửa sạch mực với gừng giã để khử tanh, để nguyên con hoặc cắt khoanh dày.",
        "Lót sả đập dập và gừng sợi dưới đĩa, xếp mực lên hấp cách thủy khoảng 6–7 phút.",
        "Rửa sạch xà lách và thái cà rốt thành thanh dài để chuẩn bị đồ cuốn.",
        "Pha nước mắm gừng chanh tỏi ớt; dọn mực hấp nóng ăn cuốn kèm xà lách và cà rốt."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "sup_cua_bap_ngot_trung",
      title = "Súp cua bắp ngọt trứng gà sánh mịn",
      emoji = "🦀",
      summary = "Chén súp nóng hổi sánh nhẹ với thịt cua ngọt, hạt bắp vàng giòn và vân trứng mây.",
      cookMinutes = 25,
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingCrab(), ingCorn(), ingEgg(), ingCarrot("1/2 củ")),
      pantryStaples = listOf("Bột năng hoặc bột bắp", "Hành tím băm", "Dầu mè", "Tiêu xay", "Ngò rí"),
      steps = listOf(
        "Tách hạt bắp ngọt, thái hạt lựu nhỏ cà rốt; xào thơm nhẹ thịt cua với hành tím băm.",
        "Đun sôi 800ml nước với bắp và cà rốt trong 8 phút cho ra nước ngọt.",
        "Cho thịt cua vào, nêm muối và hạt nêm rồi khuấy từ từ nước bột năng cho súp sánh nhẹ.",
        "Đánh tan trứng rót thành dòng mảnh vào nồi khuấy theo một chiều, thêm dầu mè, tiêu và ngò."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "com_chien_cua_bap_ngot",
      title = "Cơm chiên thịt cua bắp ngọt trứng vàng",
      emoji = "🦀",
      summary = "Hạt cơm tơi săn áo trứng vàng ươm hòa với thịt cua và bắp ngọt thơm nức mũi.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingCrab("150g"), ingRice(), ingCorn("1/2 trái"), ingEgg()),
      pantryStaples = listOf("Hành tím băm", "Nước mắm", "Hành lá", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Đánh 1 lòng đỏ trứng trộn sẵn vào cơm nguội để hạt cơm tơi và lên màu vàng đẹp.",
        "Phi thơm hành tím, xào nhanh thịt cua và hạt bắp ngọt với chút tiêu rồi múc ra.",
        "Đổ phần trứng còn lại vào chảo đảo tơi, trút cơm vào rang săn trên lửa vừa.",
        "Cho thịt cua và bắp vào đảo chung 2 phút, nêm chút nước mắm và rắc hành lá trước khi tắt bếp."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "com_chien_duong_chau_tom",
      title = "Cơm chiên Dương Châu tôm trứng bắp ngọt",
      emoji = "🍚",
      summary = "Đĩa cơm chiên đủ sắc màu từ tôm, trứng và hạt bắp ngọt tận dụng hoàn hảo cơm nguội.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingRice(), ingShrimp("150g"), ingEgg(), ingCorn("1/2 trái")),
      pantryStaples = listOf("Tỏi băm", "Xì dầu", "Dầu ăn", "Hành lá", "Tiêu xay"),
      steps = listOf(
        "Tôm bóc vỏ cắt hạt lựu; luộc sơ hạt bắp ngọt khoảng 2 phút cho chín mềm.",
        "Làm nóng dầu, chiên tơi trứng gà rồi trút cơm nguội vào đảo đều cho hạt cơm ráo tơi.",
        "Đẩy cơm sang một bên chảo, xào chín tôm và bắp với chút tỏi băm rồi trộn chung.",
        "Nêm xì dầu vừa miệng, đảo thêm 1 phút trên lửa lớn và rắc hành lá, tiêu xay."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "com_chien_ba_chi_dua_muoi",
      title = "Cơm chiên thịt ba chỉ dưa cải muối",
      emoji = "🍚",
      summary = "Vị chua mặn của dưa cải muối giúp đĩa cơm rang thịt ba chỉ trứng gà tròn vị không ngán.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingRice(), ingBacon("150g"), ingPickle("100g"), ingEgg()),
      pantryStaples = listOf("Tỏi băm", "Nước mắm", "Hành lá", "Tiêu xay", "Đường"),
      steps = listOf(
        "Thịt ba chỉ thái hạt lựu nhỏ, dưa muối vắt ráo thái vừa; đảo ba chỉ xém cạnh để lấy mỡ thơm.",
        "Cho tỏi băm và dưa muối vào xào săn cùng chút đường rồi múc bớt ra.",
        "Đập trứng vào chảo đảo nhanh tay cùng cơm nguội cho hạt cơm khô tơi.",
        "Trút dưa muối và ba chỉ vào đảo chung 2 phút, nêm chút nước mắm, rắc hành lá và tiêu."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "canh_thit_bam_nam_bong_cai",
      title = "Canh thịt bằm nấm bông cải cà rốt thanh ngọt",
      emoji = "🍲",
      summary = "Bát canh rau củ thanh mát, ngọt tự nhiên từ nấm, bông cải, cà rốt và thịt nạc bằm.",
      cookMinutes = 20,
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMeat("150g", "Thịt heo bằm"), ingMushroom(), ingBroccoli("1/2 bông"), ingCarrot()),
      pantryStaples = listOf("Hành tím băm", "Nước mắm", "Muối", "Hành lá", "Tiêu xay"),
      steps = listOf(
        "Ướp thịt bằm với hành tím, chút nước mắm và tiêu; cắt vừa ăn nấm, bông cải và cà rốt.",
        "Xào săn thịt bằm với nhúm hành tím trong nồi rồi chế khoảng 800ml nước đun sôi.",
        "Hớt bọt, thả cà rốt vào nấu 3 phút rồi cho tiếp bông cải và nấm vào.",
        "Nấu thêm 2–3 phút đến khi rau chín tới, nêm muối vừa ăn, rắc hành lá và tiêu."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "ba_chi_cuon_nam_ap_chao",
      title = "Thịt ba chỉ cuộn nấm áp chảo cuốn xà lách",
      emoji = "🍄",
      summary = "Nấm ngọt mọng gói gọn trong lát ba chỉ áp chảo cháy cạnh, ăn cuốn xà lách cực cuốn.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingBacon(), ingMushroom("200g"), ingLettuce()),
      pantryStaples = listOf("Xì dầu", "Tỏi băm", "Mật ong", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Nấm cắt gốc rửa sạch để ráo; trải lát ba chỉ mỏng cuộn chặt quanh từng bó nấm nhỏ.",
        "Pha xốt quét gồm 2 muỗng xì dầu, 1 muỗng mật ong, tỏi băm và ít tiêu.",
        "Áp chảo các cuộn thịt nấm ở lửa vừa cho xém vàng đều các mặt không cần nhiều dầu.",
        "Phết đều xốt lên cuộn thịt trong 1 phút cuối cho bóng đẹp, dọn cuốn với xà lách tươi."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "ga_ham_nam_bap_ngot",
      title = "Gà hầm nấm cà rốt bắp ngọt bổ dưỡng",
      emoji = "🍄",
      summary = "Nồi gà hầm thanh ngọt đậm vị nấm, cà rốt và bắp thích hợp cho bữa cơm gia đình ấm áp.",
      cookMinutes = 30,
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingChicken("400g"), ingMushroom(), ingCarrot(), ingCorn()),
      pantryStaples = listOf("Hành tím", "Gừng đập dập", "Nước mắm", "Muối", "Hành ngò"),
      steps = listOf(
        "Chặt gà miếng vừa ăn, ướp với chút muối, nước mắm và hành tím trong 10 phút.",
        "Cắt khúc bắp ngọt, thái khoanh cà rốt và làm sạch nấm tươi.",
        "Xào săn thịt gà với hành tím và gừng, đổ nước ngập mặt rồi hầm lửa vừa 12 phút.",
        "Cho cà rốt, bắp ngọt và nấm vào hầm thêm 10 phút đến khi mềm ngọt, nêm vừa ăn và rắc hành ngò."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "tom_xao_bong_cai_toi",
      title = "Tôm xào bông cải xanh tỏi thơm Eat-Clean",
      emoji = "🥦",
      summary = "Bữa tối ít béo chỉ 12 phút với tôm săn giòn cùng bông cải xanh và cà rốt.",
      cookMinutes = 12,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingShrimp(), ingBroccoli(), ingCarrot("1/2 củ")),
      pantryStaples = listOf("Tỏi băm", "Dầu hào", "Muối", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Bóc vỏ tôm chừa đuôi, xóc với chút muối tiêu; tách bông cải miếng nhỏ và thái lát cà rốt.",
        "Chần nhanh bông cải và cà rốt trong nước sôi 45 giây rồi vớt ra giữ màu xanh.",
        "Phi thơm tỏi băm với ít dầu, xào tôm trên lửa lớn 2 phút đến khi vừa chuyển đỏ.",
        "Trút bông cải và cà rốt vào đảo đều 1 phút với nửa muỗng dầu hào và rắc tiêu."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "banh_mi_chao_trung_sot_ca",
      title = "Bánh mì chảo trứng ốp la sốt cà bơ thơm",
      emoji = "🥖",
      summary = "Bữa sáng kiểu tiệm nóng hổi ngay tại nhà với bánh mì giòn chấm sốt cà chua bơ béo.",
      cookMinutes = 12,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingBread("1 ổ"), ingEgg("2 quả"), ingTomatoSauce(), ingButter("15g")),
      pantryStaples = listOf("Tỏi băm", "Đường", "Xì dầu", "Tiêu xay", "Ngò rí"),
      steps = listOf(
        "Đun chảy bơ với chút tỏi băm trên chảo nhỏ, cho sốt cà và nửa muỗng đường vào đun sánh.",
        "Tạo hai khoảng trống trên chảo sốt rồi đập 2 quả trứng gà vào ốp la nhẹ tay.",
        "Đậy nắp khoảng 2 phút trên lửa nhỏ để lòng trắng chín mượt còn lòng đỏ vẫn dẻo thơm.",
        "Rắc tiêu, ngò rí lên mặt chảo, rưới chút xì dầu và chấm nóng với bánh mì nướng giòn."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "banh_mi_kep_ba_chi_pho_mai",
      title = "Bánh mì kẹp thịt ba chỉ phô mai xà lách",
      emoji = "🥪",
      summary = "Sandwich kẹp nhanh 10 phút với ba chỉ áp chảo giòn, phô mai tan nhẹ và xà lách tươi.",
      cookMinutes = 10,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingBread("2 lát"), ingBacon("120g"), ingCheese(), ingLettuce("3 lá")),
      pantryStaples = listOf("Tiêu đen", "Tương cà hoặc tương ớt", "Dầu ăn"),
      steps = listOf(
        "Áp chảo thịt ba chỉ trên lửa vừa cho xém giòn hai mặt rồi thấm bớt dầu.",
        "Áp nóng nhanh hai lát bánh mì trên chính chảo vừa chiên để bánh giòn thơm.",
        "Xếp xà lách khô ráo lên lát bánh dưới, đặt ba chỉ nóng và lát phô mai lên trên cho chảy nhẹ.",
        "Phết thêm chút tương cà hoặc tương ớt, rắc tiêu và kẹp lát bánh còn lại."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = URL_SERIOUS_EATS
    ),
    CulinaryRecipe(
      id = "banh_mi_bo_toi_nam_pho_mai",
      title = "Bánh mì nướng bơ tỏi nấm phô mai kéo sợi",
      emoji = "🧀",
      summary = "Lớp bơ tỏi thơm lừng phủ nấm xào đậm vị và phô mai nướng chảy vàng rụm.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingBread("2 ổ nhỏ"), ingButter("25g"), ingMushroom("120g"), ingCheese("50g")),
      pantryStaples = listOf("Tỏi băm", "Muối", "Tiêu xay", "Ngò rí hoặc lá oregano"),
      steps = listOf(
        "Trộn bơ mềm với tỏi băm, nhúm muối và ngò rí băm nhuyễn; xào nhanh nấm thái lát cho ráo nước.",
        "Xẻ dọc bánh mì, phết đều hỗn hợp bơ tỏi lên mặt cắt.",
        "Xếp phần nấm đã xào lên bánh rồi phủ phô mai bào sợi kín mặt.",
        "Nướng ở 180°C trong 6–7 phút (hoặc áp chảo đậy nắp) đến khi phô mai tan vàng rụm."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "banh_mi_nuong_phap_chuoi",
      title = "Bánh mì nướng kiểu Pháp chuối sữa tươi",
      emoji = "🍌",
      summary = "Bánh mì nhúng hỗn hợp trứng sữa áp chảo thơm lừng kết hợp chuối chín ngọt tự nhiên.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingBread("3 lát"), ingBanana("2 quả"), ingMilk("100ml"), ingEgg()),
      pantryStaples = listOf("Mật ong", "Bột quế (tùy chọn)", "Nhúm muối", "Dầu dừa hoặc dầu ăn"),
      steps = listOf(
        "Đánh đều trứng gà, sữa tươi và nhúm muối nhỏ trong tô rộng; cắt lát chuối chín.",
        "Nhúng từng lát bánh mì vào hỗn hợp trứng sữa khoảng 15 giây cho thấm đều hai mặt.",
        "Áp chảo bánh trên lửa vừa nhỏ mỗi mặt 2–3 phút đến khi vàng thơm.",
        "Xếp bánh ra đĩa kèm chuối thái lát và rưới thêm chút mật ong trước khi thưởng thức."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "sinh_to_chuoi_dau_sua_chua",
      title = "Sinh tố chuối dâu tây sữa chua ít đường",
      emoji = "🍓",
      summary = "Ly sinh tố sánh mịn giàu chất xơ, ngọt dịu từ chuối chín và dâu tây tươi mát.",
      cookMinutes = 5,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingBanana(), ingStrawberry(), ingYogurt(), ingMilk("100ml")),
      pantryStaples = listOf("Đá viên", "Hạt chia hoặc mật ong (tùy chọn)"),
      steps = listOf(
        "Rửa sạch dâu tây, bỏ cuống và cắt đôi; bóc vỏ chuối cắt khoanh vừa xay.",
        "Cho chuối, dâu tây, sữa chua và sữa tươi vào máy xay sinh tố.",
        "Thêm vài viên đá nhỏ nếu thích uống lạnh và xay nhuyễn khoảng 30 giây.",
        "Rót ra ly, rắc thêm chút hạt chia nếu có và dùng ngay để giữ vitamin."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "sua_chua_to_tao_nho_dau",
      title = "Sữa chua tô trái cây táo nho dâu tây",
      emoji = "🍎",
      summary = "Smoothie bowl mát giòn đầy màu sắc từ táo, nho, dâu tây và sữa chua không làm nóng bếp.",
      cookMinutes = 5,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingYogurt("2 hũ"), ingApple(), ingGrape(), ingStrawberry("80g")),
      pantryStaples = listOf("Mật ong", "Yến mạch hoặc hạt điều rang (tùy chọn)"),
      steps = listOf(
        "Rửa sạch táo, nho và dâu tây với nước muối loãng rồi để ráo hoàn toàn.",
        "Thái táo miếng nhỏ vừa ăn, cắt đôi quả nho và lát mỏng dâu tây.",
        "Đổ sữa chua lạnh ra tô rộng làm phần nền sánh mịn.",
        "Xếp gọn táo, nho và dâu tây lên mặt tô, rưới thêm 1 muỗng nhỏ mật ong nếu thích ngọt."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "salad_ga_tao_nho_xa_lach",
      title = "Salad ức gà táo nho xà lách giòn mát",
      emoji = "🍏",
      summary = "Vị ngọt giòn của táo và nho giúp món salad ức gà Eat-Clean tươi mới và dễ ăn hơn hẳn.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken("250g"), ingApple(), ingGrape("100g"), ingLettuce()),
      pantryStaples = listOf("Dầu ô liu", "Nước cốt chanh", "Mật ong", "Muối", "Tiêu xay"),
      steps = listOf(
        "Áp chảo hoặc luộc chín ức gà với nhúm muối, để nguội bớt rồi thái lát vừa ăn.",
        "Cắt xà lách khúc ngắn; táo thái lát mỏng ngâm nhanh nước chanh loãng chống thâm; nho cắt đôi.",
        "Khuấy đều 2 muỗng dầu ô liu, 1 muỗng nước cốt chanh, nửa muỗng mật ong, muối và tiêu.",
        "Cho toàn bộ nguyên liệu vào tô, rưới sốt trộn nhẹ tay và thưởng thức."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = URL_SERIOUS_EATS
    ),
    CulinaryRecipe(
      id = "goi_xoai_tom_ca_rot",
      title = "Gỏi xoài tôm tươi cà rốt cuốn xà lách",
      emoji = "🥭",
      summary = "Đĩa gỏi xoài chua ngọt giòn sần sật cùng tôm luộc ngọt thịt và nước mắm chua cay.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMango(), ingShrimp("200g"), ingCarrot("1/2 củ"), ingLettuce()),
      pantryStaples = listOf("Nước mắm", "Đường", "Nước cốt chanh", "Tỏi ớt băm", "Đậu phộng rang"),
      steps = listOf(
        "Luộc chín tôm với nhúm muối, bóc vỏ và chẻ đôi theo chiều dọc; xoài và cà rốt bào sợi.",
        "Pha nước trộn gỏi theo tỷ lệ 2 muỗng nước mắm, 2 muỗng đường, 2 muỗng nước chanh và tỏi ớt.",
        "Trộn xoài bào và cà rốt với một nửa nước trộn cho thấm dịu rồi gạn bớt nước.",
        "Cho tôm và phần nước gỏi còn lại vào đảo đều, bày trên lá xà lách và rắc đậu phộng rang."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "sinh_to_xoai_sua_tuoi_kem",
      title = "Sinh tố xoài sữa tươi topping kem lạnh",
      emoji = "🥭",
      summary = "Món tráng miệng xoài chín thơm lừng, sánh mịn quyện sữa tươi và viên kem mát lạnh.",
      cookMinutes = 8,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingMango(), ingMilk("150ml"), ingIceCream("1 viên")),
      pantryStaples = listOf("Đá viên", "Đường hoặc mật ong (tùy chọn)"),
      steps = listOf(
        "Gọt vỏ xoài chín, lấy phần thịt cắt miếng vuông (giữ lại vài miếng nhỏ để trang trí).",
        "Cho thịt xoài, sữa tươi và ít đá viên vào máy xay mịn trong 30 giây.",
        "Rót sinh tố xoài ra ly cao, nhẹ nhàng đặt 1 viên kem lạnh lên trên mặt.",
        "Trang trí thêm vài miếng xoài cắt hạt lựu rồi dùng ngay bằng muỗng và ống hút."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "detox_dua_hau_nuoc_cam",
      title = "Nước ép detox dưa hấu nước cam giải nhiệt",
      emoji = "🍉",
      summary = "Thức uống giải nhiệt giàu vitamin C kết hợp dưa hấu thanh mát, nước cam và nước suối lạnh.",
      cookMinutes = 5,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingWatermelon(), ingOrangeJuice(), ingWater("150ml")),
      pantryStaples = listOf("Lá bạc hà", "Đá viên", "Nước cốt chanh (tùy chọn)"),
      steps = listOf(
        "Lọc bỏ hạt dưa hấu, cắt phần ruột đỏ thành khối nhỏ cho vào cối xay.",
        "Thêm nước suối mát vào xay nhanh 20 giây rồi lọc qua rây nếu thích nước trong mượt.",
        "Rót hỗn hợp dưa hấu ra ly lớn cùng nước cam tươi và khuấy nhẹ cho hòa quyện.",
        "Thêm đá viên và vài lá bạc hà vò nhẹ để tăng hương thơm sảng khoái."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "canh_trung_ca_chua_bap_ngot",
      title = "Canh trứng mây sốt cà bắp ngọt thanh mát",
      emoji = "🍅",
      summary = "Bát canh quốc dân nấu chỉ 12 phút, vân trứng tơ mềm cùng vị cà chua và bắp ngọt tự nhiên.",
      cookMinutes = 12,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingEgg(), ingTomatoSauce(), ingCorn("1/2 trái"), ingWater("600ml")),
      pantryStaples = listOf("Hành tím băm", "Nước mắm", "Hành lá", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Phi thơm hành tím với chút dầu, đảo nhanh sốt cà chua trong 1 phút cho lên màu đỏ đẹp.",
        "Chế nước suối và hạt bắp ngọt vào nồi, đun sôi khoảng 5 phút cho hạt bắp chín mềm.",
        "Nêm nước mắm và nhúm muối vừa khẩu vị, đánh tan trứng rồi rót thành dòng mỏng vào nồi đang sôi nhẹ.",
        "Khuấy vòng tròn tạo vân trứng mây mượt, rắc hành lá cùng tiêu xay rồi tắt bếp."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    )
  )
}
