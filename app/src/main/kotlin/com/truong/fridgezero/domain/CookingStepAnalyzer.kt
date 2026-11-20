package com.truong.fridgezero.domain

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Eight standardized culinary action phases covering all steps in [CulinaryGuideRepository].
 */
enum class CookingAction {
  PREP_CUT,
  MARINATE_MIX,
  PAN_SEAR_STIR,
  BOIL_SIMMER,
  STEAM,
  BAKE_AIRFRY,
  BLEND_CHILL,
  PLATE_SERVE
}

/**
 * Parsed countdown specification extracted from a recipe step instruction.
 */
data class StepTimerSpec(
  val totalSeconds: Int,
  val displayLabel: String
)

/**
 * Structured representation of a single recipe cooking step for Flashcard walkthrough UI.
 */
data class AnalyzedCookingStep(
  val index: Int,
  val text: String,
  val action: CookingAction,
  val timer: StepTimerSpec?,
  val mentionedIngredients: List<RecipeIngredient>
)

/**
 * Classifies cooking steps into visual action categories, parses countdown timers,
 * and identifies recipe ingredients mentioned in each step.
 */
object CookingStepAnalyzer {

  private val VI_LOCALE = Locale.forLanguageTag("vi-VN")

  private val FINAL_SERVING_REGEX = Regex(
    """thưởng thức|tắt bếp|tắt lửa|múc ra|dọn ra|trình bày|rắc .* lên|rưới .* lên|ăn nóng|ăn kèm|cho ra đĩa|cho ra tô|ra ly|ra đĩa|ra tô|cắt miếng vừa ăn|bày ra"""
  )
  private val EXCLUDE_FINAL_COOK_REGEX = Regex("""nướng|hấp|hầm \d|kho \d""")

  private val ACTION_RULES: List<Pair<CookingAction, Regex>> = listOf(
    CookingAction.BAKE_AIRFRY to Regex("""nồi chiên|lò nướng|đút lò|nướng|160°c|170°c|180°c|190°c|200°c"""),
    CookingAction.STEAM to Regex("""hấp|xửng"""),
    CookingAction.BLEND_CHILL to Regex("""máy xay|xay\b|ướp lạnh|ngăn mát|ủ lạnh|ngâm đá|bỏ tủ lạnh|đá viên|để lạnh|ngăn đông|tủ lạnh"""),
    CookingAction.BOIL_SIMMER to Regex("""đun sôi|luộc|hầm|kho\b|om\b|nấu canh|ninh|trần|chần|nấu chín|nấu nhỏ lửa|đun liu riu|nấu lửa nhỏ|đun lửa nhỏ|đun sủi|sôi lăn tăn|thắng lửa nhỏ|bắc nồi|nước dùng|nấu sôi"""),
    CookingAction.PAN_SEAR_STIR to Regex("""áp chảo|xào|chiên|rán|phi thơm|đảo đều|đảo nhanh|đảo lửa|cháy cạnh|rim\b|đun sệt|rang\b|bắc chảo|làm nóng chảo|đun chảy bơ|đổ hỗn hợp|đổ trứng|đổ bột|ốp la|chảo|sốt cà|lật nhẹ"""),
    CookingAction.MARINATE_MIX to Regex("""ướp|trộn|đánh tan|đánh nhẹ|đánh hòa|khuấy|pha sốt|pha nước|nhào|lăn bột|nhồi|phết|quết|đánh đều|xoa đều|nhúng từng|tán tơi|đập \d+ quả trứng|nêm """),
    CookingAction.PREP_CUT to Regex("""rửa|cắt|thái|băm|gọt|nhặt|lột|bóc|ngâm|đập dập|chặt|rã đông|lọc|tách|bào|cạo|khía|sơ chế|thấm khô|thấm ráo|để ráo|xé sợi|xé nhỏ|chắt bớt"""),
    CookingAction.PLATE_SERVE to Regex("""dọn|trình bày|múc |xếp ra|rắc|rưới|chan|thưởng thức|tắt bếp|tắt lửa|cho ra đĩa|cho ra tô|ra đĩa|ra tô|ăn kèm|ăn nóng|bày ra|xếp lên|kẹp bánh|cuốn|dàn đều""")
  )

  private val TIMER_REGEX = Regex(
    """(\d+(?:[.,]\d+)?)\s*(?:[–-]\s*(\d+(?:[.,]\d+)?)\s*)?(phút|giây|giờ|tiếng)""",
    RegexOption.IGNORE_CASE
  )

  private val TERM_SPLIT_REGEX = Regex("""[/()]""")

  fun analyzeRecipe(recipe: CulinaryRecipe): List<AnalyzedCookingStep> {
    val totalSteps = recipe.steps.size
    return recipe.steps.mapIndexed { index, text ->
      AnalyzedCookingStep(
        index = index,
        text = text,
        action = classifyAction(stepText = text, stepIndex = index, totalSteps = totalSteps),
        timer = extractTimer(text),
        mentionedIngredients = extractMentionedIngredients(text, recipe)
      )
    }
  }

  fun classifyAction(stepText: String, stepIndex: Int, totalSteps: Int): CookingAction {
    val low = stepText.lowercase(VI_LOCALE)
    if (
      totalSteps > 0 &&
      stepIndex == totalSteps - 1 &&
      FINAL_SERVING_REGEX.containsMatchIn(low) &&
      !EXCLUDE_FINAL_COOK_REGEX.containsMatchIn(low)
    ) {
      return CookingAction.PLATE_SERVE
    }
    for ((action, regex) in ACTION_RULES) {
      if (regex.containsMatchIn(low)) return action
    }
    return when {
      stepIndex == 0 -> CookingAction.PREP_CUT
      totalSteps > 0 && stepIndex == totalSteps - 1 -> CookingAction.PLATE_SERVE
      else -> CookingAction.PAN_SEAR_STIR
    }
  }

  fun extractTimer(stepText: String): StepTimerSpec? {
    val match = TIMER_REGEX.find(stepText) ?: return null
    val rawFirst = match.groupValues[1]
    val rawSecond = match.groupValues[2]
    val rawUnit = match.groupValues[3]
    val selectedRaw = rawSecond.ifBlank { rawFirst }
    val numericValue = selectedRaw.replace(',', '.').toDoubleOrNull() ?: return null
    if (numericValue <= 0.0) return null

    val unitLower = rawUnit.lowercase(VI_LOCALE)
    val multiplier = when (unitLower) {
      "giây" -> 1
      "phút" -> 60
      "giờ", "tiếng" -> 3600
      else -> return null
    }
    val totalSeconds = (numericValue * multiplier).roundToInt().coerceAtLeast(5)
    val label = "$selectedRaw $unitLower"
    return StepTimerSpec(totalSeconds = totalSeconds, displayLabel = label)
  }

  fun formatCountdown(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val hours = clamped / 3600
    val minutes = (clamped % 3600) / 60
    val seconds = clamped % 60
    return if (hours > 0) {
      String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  fun extractMentionedIngredients(
    stepText: String,
    recipe: CulinaryRecipe,
    limit: Int = 3
  ): List<RecipeIngredient> {
    val normAccentedStep = CulinaryGuideEngine.normalizeAccented(stepText)
    if (normAccentedStep.isEmpty()) return emptyList()
    val normStrippedStep = CulinaryGuideEngine.stripVietnameseAccents(stepText)

    val matchedWithOrder = mutableListOf<Pair<Int, RecipeIngredient>>()
    recipe.fridgeIngredients.forEachIndexed { originalIdx, ingredient ->
      val terms = buildList {
        ingredient.name.split(TERM_SPLIT_REGEX).forEach { part ->
          val trimmed = part.trim()
          if (trimmed.isNotEmpty()) add(trimmed)
        }
        addAll(ingredient.keywords)
      }.distinct()

      var earliestPos = Int.MAX_VALUE
      for (rawTerm in terms) {
        val accTerm = CulinaryGuideEngine.normalizeAccented(rawTerm)
        val cleanAcc = accTerm.trim()
        if (cleanAcc.length < 2) continue

        val accPos = normAccentedStep.indexOf(accTerm)
        if (accPos >= 0) {
          if (accPos < earliestPos) earliestPos = accPos
          continue
        }

        // Unaccented fallback only for multi-word or >= 4-char phrases to avoid Vietnamese tonal collisions.
        if (' ' in cleanAcc || cleanAcc.length >= 4) {
          val strippedTerm = CulinaryGuideEngine.stripVietnameseAccents(rawTerm)
          if (strippedTerm.isNotEmpty()) {
            val strPos = normStrippedStep.indexOf(strippedTerm)
            if (strPos >= 0 && strPos < earliestPos) {
              earliestPos = strPos
            }
          }
        }
      }

      if (earliestPos < Int.MAX_VALUE) {
        matchedWithOrder.add(earliestPos * 100 + originalIdx to ingredient)
      }
    }

    return matchedWithOrder
      .sortedBy { it.first }
      .map { it.second }
      .take(limit)
  }
}
