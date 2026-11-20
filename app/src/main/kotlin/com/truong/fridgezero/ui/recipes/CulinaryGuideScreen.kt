package com.truong.fridgezero.ui.recipes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.truong.fridgezero.ui.theme.ButterPale
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
 * Screen 4 — Neo-Brutalist Culinary Guide with keyword search bar, dish-style category chips,
 * and single-column horizontal recipe cards matching the compartment food list.
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

  var selectedFilter by remember { mutableStateOf(RecipeCategory.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedRecipeId by remember(initialRecipeId) { mutableStateOf(initialRecipeId) }
  var isCookingMode by remember(selectedRecipeId) { mutableStateOf(false) }
  var savedCookingStep by remember(selectedRecipeId) { mutableIntStateOf(0) }
  val focusManager = LocalFocusManager.current

  val filteredRecommendations = remember(allItems, selectedFilter, searchQuery) {
    CulinaryGuideEngine.recommend(
      fridgeItems = allItems,
      filter = selectedFilter,
      query = searchQuery
    )
  }
  val categoryCounts = remember(allItems, searchQuery) {
    RecipeCategory.entries.associateWith { cat ->
      CulinaryGuideEngine.recommend(
        fridgeItems = allItems,
        filter = cat,
        query = searchQuery
      ).size
    }
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
    if (isCookingMode) {
      isCookingMode = false
    } else {
      selectedRecipeId = null
    }
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
        text = stringResource(R.string.recipe_total_badge_fmt, allRecommendations.size),
        containerColor = ButterSoft,
        mono = true
      )
    }

    // Pinned Keyword Search Bar + Category Filter Strip
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      RecipeSearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        onSearchDone = { focusManager.clearFocus() }
      )

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
      ) {
        items(RecipeCategory.entries, key = { it.name }) { category ->
          val selected = selectedFilter == category
          val count = categoryCounts[category] ?: 0
          val chipColor = if (selected) LimeFresh else SurfaceWhite
          NeoBrutalistButton(
            onClick = { selectedFilter = category },
            containerColor = chipColor,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = if (selected) 3.dp else 2.dp,
            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp)
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
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(if (selected) InkBlack else ButterPale)
                .border(1.dp, InkBlack, RoundedCornerShape(5.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
              Text(
                text = "$count",
                fontFamily = MonoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                color = if (selected) SurfaceWhite else InkBlack,
                softWrap = false,
                maxLines = 1
              )
            }
          }
        }
      }

      if (searchQuery.isNotBlank() || selectedFilter != RecipeCategory.ALL) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(7.dp))
              .background(ButterPale)
              .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = stringResource(R.string.recipe_result_count_fmt, filteredRecommendations.size),
              fontFamily = MonoFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp,
              color = InkBlack
            )
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(7.dp))
              .background(SurfaceWhite)
              .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp))
              .clickable(role = Role.Button) {
                searchQuery = ""
                selectedFilter = RecipeCategory.ALL
                focusManager.clearFocus()
              }
              .padding(horizontal = 9.dp, vertical = 3.dp)
          ) {
            Text(
              text = stringResource(R.string.recipe_filter_reset),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp,
              color = InkBlack
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (filteredRecommendations.isEmpty()) {
        item(key = "empty_filter") {
          NeoBrutalistCard(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = if (searchQuery.isNotBlank()) {
                stringResource(R.string.recipe_empty_search_fmt, searchQuery.trim())
              } else {
                stringResource(R.string.recipe_empty_filter)
              },
              modifier = Modifier.padding(20.dp),
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = InkBlack
            )
          }
        }
      } else {
        items(filteredRecommendations, key = { it.recipe.id }) { rec ->
          RecipeHorizontalCard(
            recommendation = rec,
            onClick = {
              focusManager.clearFocus()
              selectedRecipeId = rec.recipe.id
            }
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
          resumeStepIndex = savedCookingStep.takeIf { it > 0 },
          onStartCooking = { isCookingMode = true },
          onDismiss = {
            isCookingMode = false
            selectedRecipeId = null
          }
        )
      }
    }

    AnimatedVisibility(
      visible = activeDetail != null && isCookingMode,
      modifier = Modifier.fillMaxSize(),
      enter = slideInHorizontally(
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        initialOffsetX = { it / 6 }
      ) + fadeIn(animationSpec = tween(durationMillis = 180)),
      exit = slideOutHorizontally(
        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing),
        targetOffsetX = { it / 6 }
      ) + fadeOut(animationSpec = tween(durationMillis = 180))
    ) {
      displayedDetail?.let { detail ->
        RecipeCookingScreen(
          recommendation = detail,
          initialStepIndex = savedCookingStep,
          onStepChanged = { stepIdx -> savedCookingStep = stepIdx },
          onCompleteCook = { usedUp, leftovers ->
            if (selectedRecipeId == detail.recipe.id) {
              onCompleteCook(usedUp, leftovers)
            }
          },
          onBackToDetail = { isCookingMode = false },
          onFinishAndExit = {
            isCookingMode = false
            selectedRecipeId = null
          }
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeHorizontalCard(
  recommendation: RecipeRecommendation,
  onClick: () -> Unit
) {
  val recipe = recommendation.recipe
  val matchedByIng = remember(recommendation) {
    recommendation.matchedIngredients.associateBy { it.ingredient.name }
  }
  val ratioColor = if (recommendation.isReadyToCook) LimeFresh else LemonWarn
  val accentStripColor = when {
    recommendation.hasUrgentMatch -> CoralUrgent
    recommendation.isReadyToCook -> LimeFresh
    else -> LemonWarn
  }

  NeoBrutalistCard(
    modifier = Modifier.fillMaxWidth(),
    containerColor = SurfaceWhite,
    cornerRadius = 14.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        // 7.5dp accounts for the 2.5dp InkBlack border so a 5dp status strip stays flush on the left edge.
        .drawBehind {
          drawRect(
            color = accentStripColor,
            topLeft = Offset.Zero,
            size = Size(7.5.dp.toPx(), size.height)
          )
        }
        .clickable(role = Role.Button, onClick = onClick)
        .padding(start = 16.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Left Zone: fixed 50.dp icon box matching FoodHorizontalCard
      Box(
        modifier = Modifier
          .size(50.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(PaperCream)
          .border(2.dp, InkBlack, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(text = recipe.emoji, fontSize = 28.sp)
      }

      // Center & Right Zones
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(7.dp)
      ) {
        // Top row: recipe title (left) + match ratio pill (right)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = recipe.title,
            modifier = Modifier.weight(1f),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = InkBlack,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          StickerBadge(
            text = stringResource(
              R.string.recipe_match_ratio_fmt,
              recommendation.matchedCount,
              recommendation.totalFridgeIngredients
            ),
            containerColor = ratioColor,
            tiltDegrees = 0f,
            mono = true
          )
        }

        // Concise summary on high-contrast SurfaceWhite card
        Text(
          text = recipe.summary,
          fontFamily = BodyFont,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          lineHeight = 16.sp,
          color = InkBlack.copy(alpha = 0.84f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        // Bottom row: ingredient icons + metadata chips (left) & action badge (right)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                  .size(24.dp)
                  .alpha(if (match != null) 1f else 0.45f)
                  .clip(RoundedCornerShape(6.dp))
                  .background(bg)
                  .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
              ) {
                Text(text = ingredientGlyph(ingredient), fontSize = 13.sp)
              }
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SkyPale)
                .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
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
            }
            Text(
              text = recipe.sourceName,
              modifier = Modifier.padding(vertical = 3.dp),
              fontFamily = BodyFont,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.5.sp,
              color = InkBlack.copy(alpha = 0.78f),
              softWrap = false,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(7.dp))
              .background(ButterSoft)
              .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = stringResource(R.string.recipe_hero_cta),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 10.5.sp,
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
private fun RecipeSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  onSearchDone: () -> Unit
) {
  val clearCd = stringResource(R.string.recipe_search_clear_cd)
  NeoBrutalistCard(
    modifier = Modifier.fillMaxWidth(),
    containerColor = SurfaceWhite,
    cornerRadius = 12.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 3.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(RoundedCornerShape(7.dp))
          .background(ButterSoft)
          .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(text = "🔍", fontSize = 14.sp)
      }

      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart
      ) {
        if (query.isEmpty()) {
          Text(
            text = stringResource(R.string.recipe_search_placeholder),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = InkBlack.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = TextStyle(
            fontFamily = BodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = InkBlack
          ),
          cursorBrush = SolidColor(InkBlack),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onSearchDone() }),
          modifier = Modifier.fillMaxWidth()
        )
      }

      if (query.isNotEmpty()) {
        Box(
          modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(CoralUrgent)
            .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp))
            .semantics { contentDescription = clearCd }
            .clickable(role = Role.Button) { onQueryChange("") },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "×",
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = InkBlack
          )
        }
      }
    }
  }
}

@Composable
private fun categoryLabel(category: RecipeCategory): String = when (category) {
  CulinaryFilter.ALL -> stringResource(R.string.recipe_filter_all)
  CulinaryFilter.VIET_HOME -> stringResource(R.string.recipe_filter_viet)
  CulinaryFilter.ASIAN_FUSION -> stringResource(R.string.recipe_filter_asian)
  CulinaryFilter.WESTERN -> stringResource(R.string.recipe_filter_western)
  CulinaryFilter.EAT_CLEAN -> stringResource(R.string.recipe_filter_eat_clean)
  CulinaryFilter.VEGETARIAN -> stringResource(R.string.recipe_filter_vegetarian)
  CulinaryFilter.BREAKFAST_DRINK -> stringResource(R.string.recipe_filter_breakfast_drink)
}

internal fun ingredientGlyph(ingredient: RecipeIngredient): String {
  val key = ingredient.iconKeys.firstOrNull()
  val icon = key?.let { EmojiIconRepository.iconFor(it) }
  return (icon as? FoodIcon.Emoji)?.glyph ?: ingredient.fallbackGroup.fallbackEmoji
}
