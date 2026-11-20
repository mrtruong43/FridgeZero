package com.truong.fridgezero.ui.recipes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.CulinaryFilter
import com.truong.fridgezero.domain.CulinaryGuideEngine
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.MatchedIngredient
import com.truong.fridgezero.domain.RecipeCategory
import com.truong.fridgezero.domain.RecipeIngredient
import com.truong.fridgezero.domain.RecipeRecommendation
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodIcon
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
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import com.truong.fridgezero.ui.theme.dotGridBackground

/**
 * Screen 4 — Neo-Brutalist Culinary Guide with top rescue hero card, category chips,
 * and 2-column Bento recipe grid.
 */
@Composable
fun CulinaryGuideScreen(
  compartments: List<CompartmentUi>,
  initialRecipeId: String? = null,
  onBack: () -> Unit,
  onCompleteCook: (usedUp: List<MatchedIngredient>, leftovers: List<MatchedIngredient>) -> Unit,
  modifier: Modifier = Modifier
) {
  val allItems = remember(compartments) { compartments.flatMap { it.items } }
  val allRecommendations = remember(allItems) {
    CulinaryGuideEngine.recommend(allItems, CulinaryFilter.ALL)
  }
  val readyCount = remember(allRecommendations) {
    allRecommendations.count { it.isReadyToCook }
  }
  val heroRecommendation = remember(allRecommendations) {
    allRecommendations.firstOrNull { it.hasUrgentMatch }
      ?: allRecommendations.firstOrNull { it.matchedCount > 0 }
      ?: allRecommendations.firstOrNull()
  }

  var selectedFilter by remember { mutableStateOf(RecipeCategory.ALL) }
  var selectedRecipeId by remember(initialRecipeId) { mutableStateOf(initialRecipeId) }

  val filteredRecommendations = remember(allItems, selectedFilter) {
    CulinaryGuideEngine.recommend(allItems, selectedFilter)
  }
  val activeDetail = remember(allRecommendations, selectedRecipeId) {
    selectedRecipeId?.let { id -> allRecommendations.firstOrNull { it.recipe.id == id } }
  }
  val lastDetailRef = remember { object { var value: RecipeRecommendation? = activeDetail } }
  if (activeDetail != null) {
    lastDetailRef.value = activeDetail
  }
  val displayedDetail = activeDetail ?: lastDetailRef.value

  BackHandler(enabled = activeDetail != null) {
    selectedRecipeId = null
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .dotGridBackground()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
    // Top Bar: square ← button | "CẨM NANG ẨM THỰC" | Ready count badge
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      val backCd = stringResource(R.string.recipe_back_cd)
      NeoBrutalistButton(
        onClick = onBack,
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
        text = stringResource(R.string.recipe_screen_title),
        modifier = Modifier.weight(1f),
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        color = InkBlack,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      StickerBadge(
        text = stringResource(R.string.recipe_ready_badge_fmt, readyCount),
        containerColor = if (readyCount > 0) LimeFresh else LemonWarn,
        mono = true
      )
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (heroRecommendation != null) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "hero_rescue_card") {
          HeroRescueRecipeCard(
            recommendation = heroRecommendation,
            onClick = { selectedRecipeId = heroRecommendation.recipe.id }
          )
        }
      }

      item(span = { GridItemSpan(maxLineSpan) }, key = "category_chips") {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 2.dp)
        ) {
          items(RecipeCategory.entries, key = { it.name }) { category ->
            val selected = selectedFilter == category
            NeoBrutalistButton(
              onClick = { selectedFilter = category },
              containerColor = if (selected) LimeFresh else SurfaceWhite,
              cornerRadius = 10.dp,
              borderWidth = 2.dp,
              shadowOffset = if (selected) 3.dp else 2.dp,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
              Text(
                text = categoryLabel(category),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = InkBlack,
                softWrap = false,
                maxLines = 1
              )
            }
          }
        }
      }

      if (filteredRecommendations.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "empty_filter") {
          NeoBrutalistCard(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = stringResource(R.string.recipe_empty_filter),
              modifier = Modifier.padding(20.dp),
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.78f)
            )
          }
        }
      } else {
        items(filteredRecommendations, key = { it.recipe.id }) { rec ->
          RecipeBentoCard(
            recommendation = rec,
            onClick = { selectedRecipeId = rec.recipe.id }
          )
        }
      }
    }
    }

    AnimatedVisibility(
      visible = activeDetail != null,
      modifier = Modifier.fillMaxSize(),
      enter = slideInVertically(
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        initialOffsetY = { it / 12 }
      ) + fadeIn(animationSpec = tween(durationMillis = 180)),
      exit = slideOutVertically(
        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing),
        targetOffsetY = { it / 12 }
      ) + fadeOut(animationSpec = tween(durationMillis = 180))
    ) {
      displayedDetail?.let { detail ->
        RecipeDetailSheet(
          recommendation = detail,
          onCompleteCook = { usedUp, leftovers ->
            if (selectedRecipeId == detail.recipe.id) {
              onCompleteCook(usedUp, leftovers)
            }
          },
          onDismiss = { selectedRecipeId = null }
        )
      }
    }
  }
}

@Composable
private fun HeroRescueRecipeCard(
  recommendation: RecipeRecommendation,
  onClick: () -> Unit
) {
  val recipe = recommendation.recipe
  val hasUrgent = recommendation.hasUrgentMatch
  val cardColor = if (hasUrgent) CoralUrgent else ButterSoft
  val ratioColor = if (recommendation.isReadyToCook) LimeFresh else LemonWarn

  NeoBrutalistCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClick = onClick),
    containerColor = cardColor,
    cornerRadius = 16.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        StickerBadge(
          text = stringResource(
            if (hasUrgent) R.string.recipe_hero_badge else R.string.recipe_hero_all_fresh_badge
          ),
          containerColor = SurfaceWhite,
          tiltDegrees = -1.2f
        )
        StickerBadge(
          text = stringResource(
            R.string.recipe_match_ratio_fmt,
            recommendation.matchedCount,
            recommendation.totalFridgeIngredients
          ),
          containerColor = ratioColor,
          tiltDegrees = 1.5f,
          mono = true
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PaperCream)
            .border(2.dp, InkBlack, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = recipe.emoji, fontSize = 28.sp)
        }
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Text(
            text = recipe.title,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = InkBlack,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = recipe.summary,
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = InkBlack.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (hasUrgent) {
          StickerBadge(
            text = stringResource(R.string.recipe_hero_rescue_fmt, recommendation.urgentMatchedCount),
            containerColor = LemonWarn,
            tiltDegrees = 0f
          )
        }
        Text(
          text = "${stringResource(R.string.recipe_prep_time_short_fmt, recipe.cookMinutes)} · ${recipe.sourceName}",
          modifier = Modifier.weight(1f),
          fontFamily = MonoFont,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          color = InkBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(InkBlack)
            .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
          Text(
            text = stringResource(R.string.recipe_hero_cta),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            color = LimeFresh,
            softWrap = false,
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
private fun RecipeBentoCard(
  recommendation: RecipeRecommendation,
  onClick: () -> Unit
) {
  val recipe = recommendation.recipe
  val matchedByIng = remember(recommendation) {
    recommendation.matchedIngredients.associateBy { it.ingredient.name }
  }
  val ratioColor = if (recommendation.isReadyToCook) LimeFresh else LemonWarn

  NeoBrutalistCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClick = onClick),
    containerColor = SurfaceWhite,
    cornerRadius = 16.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PaperCream)
            .border(2.dp, InkBlack, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = recipe.emoji, fontSize = 22.sp)
        }
        StickerBadge(
          text = stringResource(
            R.string.recipe_match_ratio_fmt,
            recommendation.matchedCount,
            recommendation.totalFridgeIngredients
          ),
          containerColor = ratioColor,
          tiltDegrees = -2f,
          mono = true
        )
      }

      Text(
        text = recipe.title,
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = InkBlack,
        minLines = 2,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        recipe.fridgeIngredients.take(4).forEach { ingredient ->
          val match = matchedByIng[ingredient.name]
          val bg = when {
            match == null -> PaperCream
            match.item.freshness == Freshness.BAD -> CoralUrgent
            match.item.freshness == Freshness.SOON -> LemonWarn
            else -> LimeFresh
          }
          Box(
            modifier = Modifier
              .size(26.dp)
              .alpha(if (match != null) 1f else 0.45f)
              .clip(RoundedCornerShape(7.dp))
              .background(bg)
              .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(text = ingredientGlyph(ingredient), fontSize = 14.sp)
          }
        }
      }

      Spacer(Modifier.height(1.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = stringResource(R.string.recipe_prep_time_short_fmt, recipe.cookMinutes),
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1
        )
        Text(
          text = "· ${recipe.sourceName}",
          modifier = Modifier.weight(1f),
          fontFamily = BodyFont,
          fontWeight = FontWeight.SemiBold,
          fontSize = 10.sp,
          color = InkBlack.copy(alpha = 0.68f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun categoryLabel(category: RecipeCategory): String = when (category) {
  CulinaryFilter.ALL -> stringResource(R.string.recipe_filter_all)
  CulinaryFilter.READY_NOW -> stringResource(R.string.recipe_filter_ready)
  CulinaryFilter.RESCUE_URGENT -> stringResource(R.string.recipe_filter_rescue)
  CulinaryFilter.QUICK_15 -> stringResource(R.string.recipe_filter_quick)
  CulinaryFilter.VIET_HOME -> stringResource(R.string.recipe_filter_viet)
  CulinaryFilter.EAT_CLEAN -> stringResource(R.string.recipe_filter_eat_clean)
}

internal fun ingredientGlyph(ingredient: RecipeIngredient): String {
  val key = ingredient.iconKeys.firstOrNull()
  val icon = key?.let { EmojiIconRepository.iconFor(it) }
  return (icon as? FoodIcon.Emoji)?.glyph ?: ingredient.fallbackGroup.fallbackEmoji
}
