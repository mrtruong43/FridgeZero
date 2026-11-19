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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.MatchTier
import com.truong.fridgezero.domain.MatchedIngredient
import com.truong.fridgezero.domain.RecipeDifficulty
import com.truong.fridgezero.domain.RecipeIngredient
import com.truong.fridgezero.domain.RecipeRecommendation
import com.truong.fridgezero.ui.theme.BodyFont
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

/**
 * Detailed recipe sheet separating in-fridge items, missing items, pantry staples,
 * cooking steps, original culinary citation link, and 1-tap inventory deduction.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailSheet(
  recommendation: RecipeRecommendation,
  onCooked: () -> Unit,
  onDismiss: () -> Unit
) {
  val recipe = recommendation.recipe
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val uriHandler = LocalUriHandler.current
  val context = LocalContext.current
  val openErrorText = stringResource(R.string.recipe_open_source_error)
  val cookedToastText = if (recommendation.matchedCount > 0) {
    stringResource(R.string.recipe_cooked_toast_fmt, recommendation.matchedCount)
  } else {
    stringResource(R.string.recipe_cooked_toast_empty)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = PaperCream,
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Scrollable body
      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
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
            text = stringResource(R.string.recipe_prep_time_long_fmt, recipe.cookMinutes),
            containerColor = SurfaceWhite,
            tiltDegrees = 0f,
            mono = true
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
            text = recipe.sourceName,
            containerColor = SkyPale,
            tiltDegrees = 0f
          )
        }

        // Section 1: In Fridge
        RecipeSectionHeader(stringResource(R.string.recipe_section_in_fridge))
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

        // Section 3: Pantry Staples
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          RecipeSectionHeader(stringResource(R.string.recipe_section_pantry))
          Text(
            text = stringResource(R.string.recipe_section_pantry_hint),
            fontFamily = MonoFont,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = InkBlack.copy(alpha = 0.65f)
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            recipe.pantryStaples.forEach { staple ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(SurfaceWhite)
                  .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
                  .padding(horizontal = 9.dp, vertical = 4.dp)
              ) {
                Text(
                  text = staple,
                  fontFamily = BodyFont,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = InkBlack
                )
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
      }

      // Bottom Action Row: Source Link | 1-Tap Cook Deduction
      Row(
        modifier = Modifier.fillMaxWidth(),
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
            onCooked()
            Toast.makeText(context, cookedToastText, Toast.LENGTH_SHORT).show()
            onDismiss()
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
        "${matched.item.name} (${matched.item.quantity})"
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
