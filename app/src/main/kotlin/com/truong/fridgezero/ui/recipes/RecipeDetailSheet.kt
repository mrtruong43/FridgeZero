package com.truong.fridgezero.ui.recipes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.domain.CookingStepAnalyzer
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.MatchTier
import com.truong.fridgezero.domain.MatchedIngredient
import com.truong.fridgezero.domain.RecipeDifficulty
import com.truong.fridgezero.domain.RecipeIngredient
import com.truong.fridgezero.domain.RecipeRecommendation
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterPale
import com.truong.fridgezero.ui.theme.ButterSoft
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.DotGridInk
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
 * Full-screen Neo-Brutalist recipe preparation overview combining essential metadata,
 * a unified Ingredients & Seasonings prep board, a compact step roadmap preview,
 * and the primary "Bắt đầu nấu" CTA leading to [RecipeCookingScreen].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailSheet(
  recommendation: RecipeRecommendation,
  resumeStepIndex: Int? = null,
  onStartCooking: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val recipe = recommendation.recipe
  val uriHandler = LocalUriHandler.current
  val context = LocalContext.current
  val openErrorText = stringResource(R.string.recipe_open_source_error)
  val backCd = stringResource(R.string.recipe_detail_back_cd)
  val scrollState = rememberScrollState()
  val analyzedSteps = remember(recipe.id) { CookingStepAnalyzer.analyzeRecipe(recipe) }

  LaunchedEffect(recipe.id) { scrollState.scrollTo(0) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
      .semantics(mergeDescendants = false) { isTraversalGroup = true }
      .pointerInput(Unit) {}
  ) {
    // Top Bar on solid SurfaceWhite strip with crisp ink rule
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(SurfaceWhite)
    ) {
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
          containerColor = PaperCream,
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
          text = stringResource(R.string.recipe_detail_topbar_title),
          modifier = Modifier.weight(1f),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp,
          letterSpacing = 0.4.sp,
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
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.5.dp)
          .background(InkBlack)
      )
    }

    // Decluttered scrollable body: Hero Overview + 1 Unified Prep Table + Compact Roadmap
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Block 1: Hero Overview (single flat card, unified 4-cell metadata bar, no nested boxes)
      NeoBrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = SurfaceWhite,
        cornerRadius = 16.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 4.dp
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(ButterSoft)
                  .border(2.dp, InkBlack, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
              ) {
                Text(text = recipe.emoji, fontSize = 30.sp)
              }
              Text(
                text = recipe.title,
                modifier = Modifier.weight(1f),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                color = InkBlack
              )
            }

            Text(
              text = recipe.summary,
              fontFamily = BodyFont,
              fontWeight = FontWeight.Medium,
              fontSize = 13.5.sp,
              lineHeight = 19.5.sp,
              color = InkBlack.copy(alpha = 0.86f)
            )
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(2.dp)
              .background(InkBlack)
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min)
          ) {
            MetadataCell(
              value = stringResource(R.string.recipe_meta_total_time_fmt, recipe.totalMinutes),
              subLabel = stringResource(R.string.recipe_meta_prep_sub_fmt, recipe.prepMinutes),
              modifier = Modifier.weight(1f),
              containerColor = ButterPale.copy(alpha = 0.55f),
              monoValue = true
            )
            MetadataDivider()
            MetadataCell(
              value = recipe.servingsLabel,
              subLabel = stringResource(R.string.recipe_meta_servings_sub),
              modifier = Modifier.weight(1.1f),
              containerColor = SurfaceWhite,
              monoValue = false
            )
            MetadataDivider()
            MetadataCell(
              value = stringResource(
                if (recipe.difficulty == RecipeDifficulty.EASY) {
                  R.string.recipe_difficulty_easy_short
                } else {
                  R.string.recipe_difficulty_medium_short
                }
              ),
              subLabel = stringResource(R.string.recipe_meta_difficulty_sub),
              modifier = Modifier.weight(0.85f),
              containerColor = SurfaceWhite,
              monoValue = false
            )
            MetadataDivider()
            MetadataCell(
              value = stringResource(
                R.string.recipe_match_ratio_fmt,
                recommendation.matchedCount,
                recommendation.totalFridgeIngredients
              ),
              subLabel = stringResource(R.string.recipe_meta_ready_sub),
              modifier = Modifier.weight(0.85f),
              containerColor = if (recommendation.isReadyToCook) LimeFresh else LemonWarn,
              monoValue = true
            )
          }
        }
      }

      // Block 2: Unified Prep Board combining In-Fridge, Missing & Seasonings cleanly
      UnifiedPrepTableCard(recommendation = recommendation)

      // Block 3: Compact Cooking Roadmap Preview (tappable to launch guided mode)
      NeoBrutalistCard(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(role = Role.Button, onClick = onStartCooking),
        containerColor = SurfaceWhite,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = stringResource(R.string.recipe_roadmap_title),
              modifier = Modifier.weight(1f),
              fontFamily = DisplayFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.5.sp,
              color = InkBlack
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(ButterSoft)
                .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = stringResource(
                  R.string.recipe_roadmap_badge_fmt,
                  recipe.steps.size,
                  recipe.cookMinutes
                ),
                fontFamily = MonoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = InkBlack,
                softWrap = false,
                maxLines = 1
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            analyzedSteps.forEach { step ->
              val shortAction = cookingActionShortLabel(step.action)
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(PaperCream.copy(alpha = 0.7f))
                  .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = stringResource(
                    R.string.recipe_roadmap_step_fmt,
                    step.index + 1,
                    shortAction
                  ),
                  fontFamily = BodyFont,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.5.sp,
                  color = InkBlack,
                  softWrap = false,
                  maxLines = 1
                )
              }
            }
          }

          Text(
            text = stringResource(R.string.recipe_roadmap_hint),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            color = InkBlack.copy(alpha = 0.70f)
          )
        }
      }

      Spacer(Modifier.height(4.dp))
    }

    // Pinned Bottom Action Bar: Source Link | Start Guided Cooking Mode
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
          modifier = Modifier.weight(0.85f),
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

        val startLabel = if (resumeStepIndex != null && resumeStepIndex > 0 && recipe.steps.isNotEmpty()) {
          stringResource(
            R.string.recipe_action_resume_cooking_fmt,
            (resumeStepIndex + 1).coerceAtMost(recipe.steps.size),
            recipe.steps.size
          )
        } else {
          stringResource(R.string.recipe_action_start_cooking)
        }

        NeoBrutalistButton(
          onClick = onStartCooking,
          modifier = Modifier.weight(1.45f),
          containerColor = LimeFresh,
          cornerRadius = 12.dp,
          borderWidth = 2.5.dp,
          shadowOffset = 4.dp,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        ) {
          Text(
            text = startLabel,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
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
private fun MetadataCell(
  value: String,
  subLabel: String,
  modifier: Modifier = Modifier,
  containerColor: Color = SurfaceWhite,
  monoValue: Boolean = false
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(containerColor)
      .padding(horizontal = 6.dp, vertical = 9.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = value,
      fontFamily = if (monoValue) MonoFont else BodyFont,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 12.5.sp,
      color = InkBlack,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = subLabel,
      fontFamily = BodyFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = 10.sp,
      color = InkBlack.copy(alpha = 0.72f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun MetadataDivider() {
  Box(
    modifier = Modifier
      .fillMaxHeight()
      .width(1.dp)
      .background(InkBlack.copy(alpha = 0.22f))
  )
}

/**
 * Unified Neo-Brutalist table combining Main Ingredients (in-fridge + missing) and Measured Seasonings.
 * Also reused inside [RecipeCookingScreen]'s quick-peek ingredients sheet.
 */
@Composable
internal fun UnifiedPrepTableCard(
  recommendation: RecipeRecommendation,
  modifier: Modifier = Modifier
) {
  val recipe = recommendation.recipe
  val sortedMatched = remember(recommendation) {
    recommendation.matchedIngredients.sortedBy { it.item.minDaysLeft }
  }
  val headerColor = if (recommendation.isReadyToCook) LimeFresh else ButterSoft

  NeoBrutalistCard(
    modifier = modifier.fillMaxWidth(),
    containerColor = SurfaceWhite,
    cornerRadius = 16.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 4.dp
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Main Header Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(headerColor)
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = stringResource(R.string.recipe_prep_board_title),
          modifier = Modifier.weight(1f),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.5.sp,
          color = InkBlack
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(InkBlack)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = stringResource(
              R.string.recipe_prep_board_badge_fmt,
              recommendation.matchedCount,
              recommendation.totalFridgeIngredients
            ),
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            color = SurfaceWhite,
            softWrap = false,
            maxLines = 1
          )
        }
      }

      TableInkDivider(thickness = 2.dp, alpha = 1f)

      // Sub-header 1: Main Fridge Ingredients
      PrepSubHeaderRow(
        leftTitle = stringResource(R.string.recipe_prep_main_header),
        rightTitle = stringResource(R.string.recipe_prep_amount_header),
        backgroundColor = PaperCream.copy(alpha = 0.72f)
      )
      TableInkDivider(thickness = 1.dp, alpha = 0.16f)

      val totalMainRows = sortedMatched.size + recommendation.missingIngredients.size
      var rowCursor = 0

      sortedMatched.forEach { matched ->
        UnifiedMatchedRow(matched = matched)
        rowCursor++
        if (rowCursor < totalMainRows) {
          TableInkDivider(thickness = 1.dp, alpha = 0.14f)
        }
      }

      recommendation.missingIngredients.forEach { missing ->
        UnifiedMissingRow(ingredient = missing)
        rowCursor++
        if (rowCursor < totalMainRows) {
          TableInkDivider(thickness = 1.dp, alpha = 0.14f)
        }
      }

      if (recipe.seasonings.isNotEmpty()) {
        TableInkDivider(thickness = 1.5.dp, alpha = 1f)

        // Sub-header 2: Seasonings
        PrepSubHeaderRow(
          leftTitle = stringResource(R.string.recipe_prep_seasonings_header),
          rightTitle = stringResource(R.string.recipe_prep_amount_header),
          backgroundColor = SkyPale.copy(alpha = 0.75f)
        )
        TableInkDivider(thickness = 1.dp, alpha = 0.16f)

        recipe.seasonings.forEachIndexed { index, spec ->
          val rowBg = if (index % 2 == 0) SurfaceWhite else ButterPale.copy(alpha = 0.42f)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(rowBg)
              .padding(horizontal = 14.dp, vertical = 9.dp),
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
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp,
              color = InkBlack,
              textAlign = TextAlign.End
            )
          }
          if (index < recipe.seasonings.lastIndex) {
            TableInkDivider(thickness = 1.dp, alpha = 0.14f)
          }
        }
      }
    }
  }
}

@Composable
private fun PrepSubHeaderRow(
  leftTitle: String,
  rightTitle: String,
  backgroundColor: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(backgroundColor)
      .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = leftTitle,
      fontFamily = MonoFont,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 10.5.sp,
      letterSpacing = 0.3.sp,
      color = InkBlack.copy(alpha = 0.78f)
    )
    Text(
      text = rightTitle,
      fontFamily = MonoFont,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 10.5.sp,
      letterSpacing = 0.3.sp,
      color = InkBlack.copy(alpha = 0.78f)
    )
  }
}

@Composable
private fun UnifiedMatchedRow(matched: MatchedIngredient) {
  val freshness = matched.item.freshness
  val stripColor = freshness.color()
  val isUrgent = matched.item.minDaysLeft <= 1
  val urgentBg = when (freshness) {
    Freshness.BAD -> CoralUrgent.copy(alpha = 0.14f)
    Freshness.SOON -> LemonWarn.copy(alpha = 0.14f)
    Freshness.OK -> SurfaceWhite
  }
  val expiryLabel = when {
    matched.item.minDaysLeft < 0 ->
      stringResource(R.string.urgent_badge_overdue_fmt, -matched.item.minDaysLeft)
    matched.item.minDaysLeft == 0 -> stringResource(R.string.urgent_badge_today)
    else -> stringResource(R.string.urgent_badge_days_fmt, matched.item.minDaysLeft)
  }
  val matchNote = if (matched.tier == MatchTier.GROUP_FALLBACK) {
    stringResource(R.string.recipe_substitute_fmt, matched.item.name)
  } else {
    stringResource(R.string.recipe_exact_match_fmt, matched.item.name, matched.item.quantity)
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(urgentBg)
      .drawBehind {
        drawRect(
          color = stripColor,
          topLeft = Offset.Zero,
          size = Size(4.5.dp.toPx(), size.height)
        )
      }
      .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(text = ingredientGlyph(matched.ingredient), fontSize = 20.sp)

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = matched.ingredient.name,
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.5.sp,
        color = InkBlack,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = if (isUrgent) matchNote else "$matchNote · $expiryLabel",
          modifier = Modifier.weight(1f, fill = false),
          fontFamily = BodyFont,
          fontWeight = FontWeight.Medium,
          fontSize = 11.5.sp,
          color = InkBlack.copy(alpha = 0.72f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (isUrgent) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(5.dp))
              .background(stripColor)
              .border(1.dp, InkBlack, RoundedCornerShape(5.dp))
              .padding(horizontal = 5.dp, vertical = 1.dp)
          ) {
            Text(
              text = expiryLabel,
              fontFamily = MonoFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 10.sp,
              color = InkBlack,
              softWrap = false,
              maxLines = 1
            )
          }
        }
      }
    }

    Text(
      text = matched.ingredient.amountText,
      fontFamily = MonoFont,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 12.5.sp,
      color = InkBlack,
      textAlign = TextAlign.End
    )
  }
}

@Composable
private fun UnifiedMissingRow(ingredient: RecipeIngredient) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(SurfaceWhite)
      .drawBehind {
        drawRect(
          color = DotGridInk,
          topLeft = Offset.Zero,
          size = Size(4.5.dp.toPx(), size.height)
        )
      }
      .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(text = ingredientGlyph(ingredient), fontSize = 20.sp)

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = ingredient.name,
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        color = InkBlack,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = stringResource(R.string.recipe_prep_missing_status),
          fontFamily = BodyFont,
          fontWeight = FontWeight.Medium,
          fontSize = 11.5.sp,
          color = InkBlack.copy(alpha = 0.65f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(LemonWarn.copy(alpha = 0.72f))
            .border(1.dp, InkBlack, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
          Text(
            text = stringResource(R.string.recipe_prep_missing_tag),
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            color = InkBlack,
            softWrap = false,
            maxLines = 1
          )
        }
      }
    }

    Text(
      text = ingredient.amountText,
      fontFamily = MonoFont,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 12.5.sp,
      color = InkBlack,
      textAlign = TextAlign.End
    )
  }
}

@Composable
private fun TableInkDivider(
  thickness: androidx.compose.ui.unit.Dp,
  alpha: Float
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(thickness)
      .background(InkBlack.copy(alpha = alpha))
  )
}

private val STEP_HIGHLIGHT_REGEX = Regex(
  """\d+(?:[.,–-]\d+)?\s*(?:phút|giây|tiếng|giờ|°C|kg|g|ml|l|muỗng\s+(?:canh|cà\s+phê)|tép|quả|trái|củ|miếng)\b""",
  RegexOption.IGNORE_CASE
)

internal fun highlightStepMeasurements(text: String): AnnotatedString = buildAnnotatedString {
  append(text)
  STEP_HIGHLIGHT_REGEX.findAll(text).forEach { match ->
    addStyle(
      style = SpanStyle(
        fontWeight = FontWeight.ExtraBold,
        background = ButterSoft
      ),
      start = match.range.first,
      end = match.range.last + 1
    )
  }
}
