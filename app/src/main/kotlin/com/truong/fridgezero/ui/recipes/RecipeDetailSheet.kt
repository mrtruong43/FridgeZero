package com.truong.fridgezero.ui.recipes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.truong.fridgezero.R
import com.truong.fridgezero.domain.CulinaryGuideEngine
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.MatchTier
import com.truong.fridgezero.domain.MatchedIngredient
import com.truong.fridgezero.domain.RecipeDifficulty
import com.truong.fridgezero.domain.RecipeIngredient
import com.truong.fridgezero.domain.RecipeRecommendation
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterSoft
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.PaperCream
import com.truong.fridgezero.ui.theme.SkyPale
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import com.truong.fridgezero.ui.theme.dotGridBackground

/**
 * Full-screen Neo-Brutalist recipe detail view separating in-fridge items, missing items,
 * measured seasonings, cooking steps, original culinary citation link, and post-cook confirmation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailSheet(
  recommendation: RecipeRecommendation,
  onCompleteCook: (usedUp: List<MatchedIngredient>, leftovers: List<MatchedIngredient>) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val recipe = recommendation.recipe
  val uriHandler = LocalUriHandler.current
  val context = LocalContext.current
  val openErrorText = stringResource(R.string.recipe_open_source_error)
  val backCd = stringResource(R.string.recipe_detail_back_cd)
  val cookedEmptyToastText = stringResource(R.string.recipe_cooked_toast_empty)
  val scrollState = rememberScrollState()
  LaunchedEffect(recipe.id) { scrollState.scrollTo(0) }
  var cookedTriggered by remember(recipe.id) { mutableStateOf(false) }
  var showCookConfirmDialog by remember(recipe.id) { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
      .semantics(mergeDescendants = false) { isTraversalGroup = true }
      .pointerInput(Unit) {}
  ) {
    // Top Bar: square ← button | recipe title | source badge
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      NeoBrutalistButton(
        onClick = onDismiss,
        modifier = Modifier
          .size(44.dp)
          .semantics { contentDescription = backCd },
        containerColor = SurfaceWhite,
        cornerRadius = 12.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(0.dp)
      ) {
        Text(
          text = stringResource(R.string.bento_back_symbol),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 20.sp,
          color = InkBlack
        )
      }

      Text(
        text = recipe.title,
        modifier = Modifier.weight(1f),
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        color = InkBlack,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      StickerBadge(
        text = recipe.sourceName,
        containerColor = SkyPale,
        tiltDegrees = 0f
      )
    }

    // Scrollable body
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header: Emoji + Title + Summary + Meta badges
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceWhite)
            .border(2.5.dp, InkBlack, RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = recipe.emoji, fontSize = 30.sp)
        }
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = recipe.title,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp,
            lineHeight = 23.sp,
            color = InkBlack
          )
          Text(
            text = recipe.summary,
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = InkBlack.copy(alpha = 0.78f)
          )
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        StickerBadge(
          text = stringResource(
            R.string.recipe_prep_cook_time_fmt,
            recipe.prepMinutes,
            recipe.cookMinutes
          ),
          containerColor = SurfaceWhite,
          tiltDegrees = 0f,
          mono = true
        )
        StickerBadge(
          text = stringResource(R.string.recipe_servings_fmt, recipe.servingsLabel),
          containerColor = ButterSoft,
          tiltDegrees = 0f
        )
        StickerBadge(
          text = stringResource(
            if (recipe.difficulty == RecipeDifficulty.EASY) {
              R.string.recipe_difficulty_easy
            } else {
              R.string.recipe_difficulty_medium
            }
          ),
          containerColor = if (recipe.difficulty == RecipeDifficulty.EASY) LimeFresh else LemonWarn,
          tiltDegrees = 0f
        )
        StickerBadge(
          text = stringResource(
            R.string.recipe_match_ratio_fmt,
            recommendation.matchedCount,
            recommendation.totalFridgeIngredients
          ),
          containerColor = if (recommendation.isReadyToCook) LimeFresh else LemonWarn,
          tiltDegrees = 0f,
          mono = true
        )
      }

      // Section 1: Reference matches / similar items in fridge
      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        RecipeSectionHeader(stringResource(R.string.recipe_section_in_fridge))
        Text(
          text = stringResource(R.string.recipe_section_in_fridge_hint),
          fontFamily = MonoFont,
          fontWeight = FontWeight.Medium,
          fontSize = 10.sp,
          color = InkBlack.copy(alpha = 0.65f)
        )
      }
      if (recommendation.matchedIngredients.isEmpty()) {
        NeoBrutalistCard(
          modifier = Modifier.fillMaxWidth(),
          containerColor = SurfaceWhite,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp
        ) {
          Text(
            text = stringResource(R.string.recipe_section_in_fridge_empty),
            modifier = Modifier.padding(12.dp),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = InkBlack.copy(alpha = 0.72f)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          recommendation.matchedIngredients.forEach { matched ->
            MatchedIngredientRow(matched = matched)
          }
        }
      }

      // Section 2: Missing Fridge Ingredients
      RecipeSectionHeader(stringResource(R.string.recipe_section_missing))
      if (recommendation.missingIngredients.isEmpty()) {
        NeoBrutalistCard(
          modifier = Modifier.fillMaxWidth(),
          containerColor = LimeFresh,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp
        ) {
          Text(
            text = stringResource(R.string.recipe_section_missing_none),
            modifier = Modifier.padding(12.dp),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = InkBlack
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          recommendation.missingIngredients.forEach { missing ->
            MissingIngredientRow(ingredient = missing)
          }
        }
      }

      // Section 3: Measured Seasonings Table
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RecipeSectionHeader(stringResource(R.string.recipe_section_pantry))
        Text(
          text = stringResource(R.string.recipe_section_pantry_hint),
          fontFamily = MonoFont,
          fontWeight = FontWeight.Medium,
          fontSize = 10.sp,
          color = InkBlack.copy(alpha = 0.65f)
        )
        NeoBrutalistCard(
          modifier = Modifier.fillMaxWidth(),
          containerColor = SurfaceWhite,
          cornerRadius = 14.dp,
          borderWidth = 2.dp,
          shadowOffset = 3.dp
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            recipe.seasonings.forEachIndexed { index, spec ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Text(
                  text = spec.name,
                  modifier = Modifier.weight(1f),
                  fontFamily = BodyFont,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = InkBlack
                )
                Text(
                  text = spec.amount,
                  fontFamily = MonoFont,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = InkBlack.copy(alpha = 0.85f)
                )
              }
              if (index < recipe.seasonings.lastIndex) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(InkBlack.copy(alpha = 0.14f))
                )
              }
            }
          }
        }
      }

      // Section 4: Cooking Steps inside NeoBrutalistCard
      RecipeSectionHeader(stringResource(R.string.recipe_section_steps))
      NeoBrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = SurfaceWhite,
        cornerRadius = 16.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 3.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          recipe.steps.forEachIndexed { index, stepText ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(RoundedCornerShape(7.dp))
                  .background(InkBlack),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${index + 1}",
                  fontFamily = MonoFont,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 12.sp,
                  color = LimeFresh
                )
              }
              Text(
                text = stepText,
                modifier = Modifier.weight(1f),
                fontFamily = BodyFont,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = InkBlack
              )
            }
          }
        }
      }

      Spacer(Modifier.height(8.dp))
    }

    // Pinned Bottom Action Bar: Source Link | 1-Tap Cook Deduction
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(SurfaceWhite)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.5.dp)
          .background(InkBlack)
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        NeoBrutalistButton(
          onClick = {
            runCatching { uriHandler.openUri(recipe.sourceUrl) }
              .onFailure {
                Toast.makeText(context, openErrorText, Toast.LENGTH_SHORT).show()
              }
          },
          modifier = Modifier.weight(1f),
          containerColor = SurfaceWhite,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
        ) {
          Text(
            text = stringResource(R.string.recipe_action_source),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = InkBlack,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        NeoBrutalistButton(
          onClick = {
            if (recommendation.matchedIngredients.isEmpty()) {
              if (!cookedTriggered) {
                cookedTriggered = true
                Toast.makeText(context, cookedEmptyToastText, Toast.LENGTH_SHORT).show()
                onDismiss()
              }
            } else {
              showCookConfirmDialog = true
            }
          },
          modifier = Modifier.weight(1.15f),
          containerColor = LimeFresh,
          cornerRadius = 12.dp,
          borderWidth = 2.5.dp,
          shadowOffset = 4.dp,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
        ) {
          Text(
            text = stringResource(R.string.recipe_action_cooked),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = InkBlack,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }

  if (showCookConfirmDialog && recommendation.matchedIngredients.isNotEmpty()) {
    CookCompletionConfirmDialog(
      recipeId = recipe.id,
      matchedIngredients = recommendation.matchedIngredients,
      onConfirm = { usedUp, leftovers ->
        if (!cookedTriggered) {
          cookedTriggered = true
          onCompleteCook(usedUp, leftovers)
          val toastMsg = when {
            usedUp.isNotEmpty() && leftovers.isNotEmpty() ->
              context.getString(R.string.cook_toast_mixed_fmt, usedUp.size, leftovers.size)
            leftovers.isNotEmpty() ->
              context.getString(R.string.cook_toast_leftover_only_fmt, leftovers.size)
            else ->
              context.getString(R.string.cook_toast_used_only_fmt, usedUp.size)
          }
          Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
          showCookConfirmDialog = false
          onDismiss()
        }
      },
      onCancel = { showCookConfirmDialog = false }
    )
  }
}

@Composable
private fun CookCompletionConfirmDialog(
  recipeId: String,
  matchedIngredients: List<MatchedIngredient>,
  onConfirm: (usedUp: List<MatchedIngredient>, leftovers: List<MatchedIngredient>) -> Unit,
  onCancel: () -> Unit
) {
  val leftoverByIndex = remember(recipeId, matchedIngredients) {
    mutableStateMapOf<Int, Boolean>()
  }
  val hasAnyLeftover = matchedIngredients.indices.any { leftoverByIndex[it] == true }

  Dialog(onDismissRequest = onCancel) {
    NeoBrutalistCard(
      modifier = Modifier.fillMaxWidth(),
      containerColor = PaperCream,
      cornerRadius = 16.dp,
      borderWidth = 2.5.dp,
      shadowOffset = 4.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = stringResource(R.string.cook_confirm_title),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = InkBlack
          )
          Text(
            text = stringResource(R.string.cook_confirm_subtitle),
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = InkBlack.copy(alpha = 0.78f)
          )
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          matchedIngredients.forEachIndexed { index, matched ->
            val isLeftover = leftoverByIndex[index] == true
            val isWeightLeftover =
              matched.item.isOpened && CulinaryGuideEngine.isWeightBasedUnit(matched.item.unit)
            val qtyLabel = when {
              isWeightLeftover -> stringResource(R.string.bento_leftover_remaining)
              matched.item.unit.isNullOrBlank() ->
                stringResource(R.string.cook_confirm_qty_only_fmt, matched.item.quantity)
              else ->
                stringResource(R.string.bento_qty_unit_fmt, matched.item.quantity, matched.item.unit)
            }

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .border(2.dp, InkBlack, RoundedCornerShape(12.dp))
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PaperCream)
                    .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(text = ingredientGlyph(matched.ingredient), fontSize = 17.sp)
                }
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                  Text(
                    text = matched.item.name,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = InkBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = qtyLabel,
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = InkBlack.copy(alpha = 0.72f)
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                NeoBrutalistButton(
                  onClick = { leftoverByIndex[index] = false },
                  modifier = Modifier.weight(1f),
                  containerColor = if (!isLeftover) LimeFresh else SurfaceWhite,
                  cornerRadius = 9.dp,
                  borderWidth = 2.dp,
                  shadowOffset = if (!isLeftover) 3.dp else 1.dp,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
                ) {
                  Text(
                    text = stringResource(R.string.cook_confirm_used_up),
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = InkBlack,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                NeoBrutalistButton(
                  onClick = { leftoverByIndex[index] = true },
                  modifier = Modifier.weight(1f),
                  containerColor = if (isLeftover) ButterSoft else SurfaceWhite,
                  cornerRadius = 9.dp,
                  borderWidth = 2.dp,
                  shadowOffset = if (isLeftover) 3.dp else 1.dp,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
                ) {
                  Text(
                    text = stringResource(R.string.cook_confirm_leftover),
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = InkBlack,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }

        if (hasAnyLeftover) {
          Text(
            text = stringResource(R.string.cook_confirm_leftover_hint),
            fontFamily = MonoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = InkBlack.copy(alpha = 0.75f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          NeoBrutalistButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            containerColor = SurfaceWhite,
            cornerRadius = 12.dp,
            borderWidth = 2.dp,
            shadowOffset = 3.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
          ) {
            Text(
              text = stringResource(R.string.cook_confirm_cancel),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.sp,
              color = InkBlack,
              softWrap = false,
              maxLines = 1
            )
          }

          NeoBrutalistButton(
            onClick = {
              val usedUp = mutableListOf<MatchedIngredient>()
              val leftovers = mutableListOf<MatchedIngredient>()
              matchedIngredients.forEachIndexed { idx, entry ->
                if (leftoverByIndex[idx] == true) leftovers.add(entry) else usedUp.add(entry)
              }
              onConfirm(usedUp, leftovers)
            },
            modifier = Modifier.weight(1.1f),
            containerColor = LimeFresh,
            cornerRadius = 12.dp,
            borderWidth = 2.5.dp,
            shadowOffset = 4.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
          ) {
            Text(
              text = stringResource(R.string.cook_confirm_apply),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.sp,
              color = InkBlack,
              softWrap = false,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RecipeSectionHeader(text: String) {
  Text(
    text = text,
    fontFamily = DisplayFont,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 14.sp,
    color = InkBlack
  )
}

@Composable
private fun MatchedIngredientRow(matched: MatchedIngredient) {
  val freshness = matched.item.freshness
  val rowColor: Color = when (freshness) {
    Freshness.BAD -> CoralUrgent
    Freshness.SOON -> LemonWarn
    Freshness.OK -> SurfaceWhite
  }
  val expiryBadgeColor = freshness.color()
  val expiryLabel = when {
    matched.item.minDaysLeft < 0 ->
      stringResource(R.string.urgent_badge_overdue_fmt, -matched.item.minDaysLeft)
    matched.item.minDaysLeft == 0 -> stringResource(R.string.urgent_badge_today)
    else -> stringResource(R.string.urgent_badge_days_fmt, matched.item.minDaysLeft)
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(rowColor)
      .border(2.dp, InkBlack, RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(PaperCream)
        .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center
    ) {
      Text(text = ingredientGlyph(matched.ingredient), fontSize = 18.sp)
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = "${matched.ingredient.name} · ${matched.ingredient.amountText}",
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        color = InkBlack,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      val subtitle = if (matched.tier == MatchTier.GROUP_FALLBACK) {
        stringResource(R.string.recipe_substitute_fmt, matched.item.name)
      } else {
        stringResource(R.string.recipe_exact_match_fmt, matched.item.name, matched.item.quantity)
      }
      Text(
        text = subtitle,
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        color = InkBlack.copy(alpha = 0.78f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    StickerBadge(
      text = expiryLabel,
      containerColor = expiryBadgeColor,
      tiltDegrees = 0f,
      mono = true
    )
  }
}

@Composable
private fun MissingIngredientRow(ingredient: RecipeIngredient) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(SurfaceWhite)
      .border(2.dp, InkBlack, RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(PaperCream)
        .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center
    ) {
      Text(text = ingredientGlyph(ingredient), fontSize = 18.sp)
    }
    Text(
      text = ingredient.name,
      modifier = Modifier.weight(1f),
      fontFamily = BodyFont,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      color = InkBlack,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = ingredient.amountText,
      fontFamily = MonoFont,
      fontWeight = FontWeight.Bold,
      fontSize = 12.sp,
      color = InkBlack.copy(alpha = 0.75f),
      softWrap = false,
      maxLines = 1
    )
  }
}
