package com.truong.fridgezero.ui.recipes

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.truong.fridgezero.R
import com.truong.fridgezero.domain.AnalyzedCookingStep
import com.truong.fridgezero.domain.CookingAction
import com.truong.fridgezero.domain.CookingStepAnalyzer
import com.truong.fridgezero.domain.CulinaryGuideEngine
import com.truong.fridgezero.domain.MatchedIngredient
import com.truong.fridgezero.domain.RecipeRecommendation
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterPale
import com.truong.fridgezero.ui.theme.ButterSoft
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.PaperCream
import com.truong.fridgezero.ui.theme.SkyInfo
import com.truong.fridgezero.ui.theme.SkyPale
import com.truong.fridgezero.ui.theme.SurfaceWhite
import com.truong.fridgezero.ui.theme.dotGridBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class StepTimerState(
  val durationSec: Int = 0,
  val remainingSec: Int = 0,
  val isRunning: Boolean = false,
  val isFinished: Boolean = false,
  val showPresets: Boolean = false
)

private val QUICK_TIMER_PRESET_MINUTES = listOf(1, 3, 5, 10)

/**
 * Full-screen Neo-Brutalist Flashcard walkthrough presenting one large illustrated cooking step
 * per page, with a convenient inline countdown timer, quick ingredients peek, and final-step
 * fridge inventory deduction confirmation.
 */
@Composable
fun RecipeCookingScreen(
  recommendation: RecipeRecommendation,
  initialStepIndex: Int = 0,
  onStepChanged: (Int) -> Unit = {},
  onCompleteCook: (usedUp: List<MatchedIngredient>, leftovers: List<MatchedIngredient>) -> Unit,
  onBackToDetail: () -> Unit,
  onFinishAndExit: () -> Unit,
  modifier: Modifier = Modifier
) {
  val recipe = recommendation.recipe
  val analyzedSteps = remember(recipe.id) { CookingStepAnalyzer.analyzeRecipe(recipe) }
  val totalSteps = analyzedSteps.size
  val initialPage = initialStepIndex.coerceIn(0, maxOf(0, totalSteps - 1))
  val pagerState = rememberPagerState(initialPage = initialPage) { totalSteps }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val view = LocalView.current

  val backCd = stringResource(R.string.cooking_back_cd)
  val cookedEmptyToastText = stringResource(R.string.recipe_cooked_toast_empty)

  var showIngredientsOverlay by remember(recipe.id) { mutableStateOf(false) }
  var showCookConfirmDialog by remember(recipe.id) { mutableStateOf(false) }
  var cookedTriggered by remember(recipe.id) { mutableStateOf(false) }
  val timerStates = remember(recipe.id) { mutableStateMapOf<Int, StepTimerState>() }

  LaunchedEffect(pagerState.currentPage) {
    onStepChanged(pagerState.currentPage)
  }

  // Keep kitchen screen awake only while active in guided cooking mode.
  DisposableEffect(view) {
    val prevKeepScreenOn = view.keepScreenOn
    view.keepScreenOn = true
    onDispose {
      view.keepScreenOn = prevKeepScreenOn
    }
  }

  val hasRunningTimer = timerStates.values.any { it.isRunning }
  LaunchedEffect(recipe.id, hasRunningTimer) {
    if (!hasRunningTimer) return@LaunchedEffect
    while (isActive) {
      delay(1000L)
      var finishedAny = false
      for (stepIdx in timerStates.keys.toList()) {
        val current = timerStates[stepIdx] ?: continue
        if (!current.isRunning) continue
        val next = (current.remainingSec - 1).coerceAtLeast(0)
        if (next == 0) {
          timerStates[stepIdx] = current.copy(
            remainingSec = 0,
            isRunning = false,
            isFinished = true
          )
          finishedAny = true
        } else {
          timerStates[stepIdx] = current.copy(remainingSec = next)
        }
      }
      if (finishedAny) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      }
    }
  }

  BackHandler(enabled = showIngredientsOverlay) {
    showIngredientsOverlay = false
  }

  val isLastStep = totalSteps > 0 && pagerState.currentPage >= totalSteps - 1

  Box(
    modifier = modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
      .semantics(mergeDescendants = false) { isTraversalGroup = true }
      .pointerInput(Unit) {}
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Bar: Back | Dish + Step Counter + Segmented Progress | Quick Ingredients Peek
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(SurfaceWhite)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            NeoBrutalistButton(
              onClick = onBackToDetail,
              modifier = Modifier
                .size(42.dp)
                .semantics { contentDescription = backCd },
              containerColor = PaperCream,
              cornerRadius = 11.dp,
              borderWidth = 2.dp,
              shadowOffset = 3.dp,
              contentPadding = PaddingValues(0.dp)
            ) {
              Text(
                text = stringResource(R.string.bento_back_symbol),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = InkBlack
              )
            }

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                text = "${recipe.emoji} ${recipe.title}",
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (totalSteps > 0) {
                Text(
                  text = stringResource(
                    R.string.cooking_step_counter_fmt,
                    pagerState.currentPage + 1,
                    totalSteps
                  ),
                  fontFamily = MonoFont,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 11.sp,
                  color = InkBlack.copy(alpha = 0.72f),
                  maxLines = 1
                )
              }
            }

            NeoBrutalistButton(
              onClick = { showIngredientsOverlay = true },
              modifier = Modifier.height(42.dp),
              containerColor = ButterSoft,
              cornerRadius = 11.dp,
              borderWidth = 2.dp,
              shadowOffset = 3.dp,
              contentPadding = PaddingValues(horizontal = 11.dp, vertical = 0.dp)
            ) {
              Text(
                text = stringResource(R.string.cooking_ingredients_btn),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.5.sp,
                color = InkBlack,
                softWrap = false,
                maxLines = 1
              )
            }
          }

          if (totalSteps > 1) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              repeat(totalSteps) { idx ->
                val segmentColor = when {
                  idx == pagerState.currentPage -> LemonWarn
                  idx < pagerState.currentPage -> LimeFresh
                  else -> PaperCream
                }
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(segmentColor)
                    .border(1.5.dp, InkBlack, RoundedCornerShape(4.dp))
                    .clickable(role = Role.Tab) {
                      scope.launch { pagerState.animateScrollToPage(idx) }
                    }
                )
              }
            }
          }
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .background(InkBlack)
        )
      }

      // Center Flashcard Stage: 1 Unified Card per Step inside HorizontalPager
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) { pageIndex ->
        val step = analyzedSteps[pageIndex]
        val timerState = timerStates[step.index] ?: StepTimerState()

        CookingStepFlashcard(
          step = step,
          timerState = timerState,
          showSwipeHint = pageIndex == 0 && totalSteps > 1,
          onStartDetectedTimer = { seconds ->
            timerStates[step.index] = StepTimerState(
              durationSec = seconds,
              remainingSec = seconds,
              isRunning = true,
              isFinished = false,
              showPresets = false
            )
          },
          onTogglePresets = {
            timerStates[step.index] = timerState.copy(showPresets = !timerState.showPresets)
          },
          onSelectPresetMinutes = { minutes ->
            val seconds = minutes * 60
            timerStates[step.index] = StepTimerState(
              durationSec = seconds,
              remainingSec = seconds,
              isRunning = true,
              isFinished = false,
              showPresets = false
            )
          },
          onTogglePauseResume = {
            if (timerState.durationSec > 0 && !timerState.isFinished) {
              timerStates[step.index] = timerState.copy(isRunning = !timerState.isRunning)
            }
          },
          onAddOneMinute = {
            val baseRemaining = if (timerState.isFinished) 0 else timerState.remainingSec
            timerStates[step.index] = timerState.copy(
              durationSec = maxOf(timerState.durationSec + 60, baseRemaining + 60),
              remainingSec = baseRemaining + 60,
              isRunning = true,
              isFinished = false,
              showPresets = false
            )
          },
          onResetTimer = {
            timerStates.remove(step.index)
          },
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
        )
      }

      // Pinned Bottom Navigation Bar
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
              if (pagerState.currentPage > 0) {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
              }
            },
            enabled = pagerState.currentPage > 0,
            modifier = Modifier.weight(0.85f),
            containerColor = SurfaceWhite,
            cornerRadius = 12.dp,
            borderWidth = 2.dp,
            shadowOffset = 3.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 13.dp)
          ) {
            Text(
              text = stringResource(R.string.cooking_prev_step),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp,
              color = if (pagerState.currentPage > 0) InkBlack else InkBlack.copy(alpha = 0.42f),
              softWrap = false,
              maxLines = 1
            )
          }

          NeoBrutalistButton(
            onClick = {
              if (!isLastStep) {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
              } else {
                if (recommendation.matchedIngredients.isEmpty()) {
                  if (!cookedTriggered) {
                    cookedTriggered = true
                    Toast.makeText(context, cookedEmptyToastText, Toast.LENGTH_SHORT).show()
                    onFinishAndExit()
                  }
                } else {
                  showCookConfirmDialog = true
                }
              }
            },
            modifier = Modifier.weight(1.45f),
            containerColor = LimeFresh,
            cornerRadius = 12.dp,
            borderWidth = 2.5.dp,
            shadowOffset = 4.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 13.dp)
          ) {
            Text(
              text = stringResource(
                if (isLastStep) R.string.cooking_finish_cook else R.string.cooking_next_step
              ),
              fontFamily = DisplayFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.sp,
              color = InkBlack,
              softWrap = false,
              maxLines = 1
            )
          }
        }
      }
    }

    // Quick-Peek Ingredients & Seasonings Overlay
    AnimatedVisibility(
      visible = showIngredientsOverlay,
      modifier = Modifier.fillMaxSize(),
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(InkBlack.copy(alpha = 0.48f))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { showIngredientsOverlay = false }
          )
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        NeoBrutalistCard(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = {}
            ),
          containerColor = PaperCream,
          cornerRadius = 18.dp,
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
              Text(
                text = stringResource(R.string.cooking_ingredients_sheet_title),
                modifier = Modifier.weight(1f),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = InkBlack
              )
              NeoBrutalistButton(
                onClick = { showIngredientsOverlay = false },
                containerColor = SurfaceWhite,
                cornerRadius = 9.dp,
                borderWidth = 2.dp,
                shadowOffset = 2.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = stringResource(R.string.cooking_ingredients_sheet_close),
                  fontFamily = BodyFont,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 12.sp,
                  color = InkBlack
                )
              }
            }

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
            ) {
              UnifiedPrepTableCard(recommendation = recommendation)
            }
          }
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
          onFinishAndExit()
        }
      },
      onCancel = { showCookConfirmDialog = false }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CookingStepFlashcard(
  step: AnalyzedCookingStep,
  timerState: StepTimerState,
  showSwipeHint: Boolean,
  onStartDetectedTimer: (Int) -> Unit,
  onTogglePresets: () -> Unit,
  onSelectPresetMinutes: (Int) -> Unit,
  onTogglePauseResume: () -> Unit,
  onAddOneMinute: () -> Unit,
  onResetTimer: () -> Unit,
  modifier: Modifier = Modifier
) {
  val actionTitle = cookingActionTitle(step.action)
  val actionAccent = cookingActionColor(step.action)
  val stepLabel = stringResource(R.string.recipe_step_label_fmt, step.index + 1)

  NeoBrutalistCard(
    modifier = modifier,
    containerColor = SurfaceWhite,
    cornerRadius = 18.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 4.dp
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Artwork Zone (~45% height): Full-bleed Neo-Brutalist illustration + Step badge + Ingredient tags
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(0.45f)
          .background(PaperCream)
      ) {
        Image(
          painter = painterResource(actionDrawableRes(step.action)),
          contentDescription = actionTitle,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(InkBlack)
            .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = stepLabel,
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.5.sp,
            color = LimeFresh
          )
        }

        if (step.mentionedIngredients.isNotEmpty()) {
          FlowRow(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            step.mentionedIngredients.forEach { ingredient ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(SurfaceWhite.copy(alpha = 0.94f))
                  .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
                  .padding(horizontal = 7.dp, vertical = 3.dp)
              ) {
                Text(
                  text = "${ingredientGlyph(ingredient)} ${ingredient.name}",
                  fontFamily = BodyFont,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 11.sp,
                  color = InkBlack,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      // Middle Action + Convenient Compact Timer Strip
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.5.dp)
          .background(InkBlack)
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(actionAccent)
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = actionTitle,
          modifier = Modifier.weight(1f),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.5.sp,
          color = InkBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        StepTimerControls(
          step = step,
          timerState = timerState,
          onStartDetectedTimer = onStartDetectedTimer,
          onTogglePresets = onTogglePresets,
          onTogglePauseResume = onTogglePauseResume,
          onAddOneMinute = onAddOneMinute,
          onResetTimer = onResetTimer
        )
      }

      if (timerState.showPresets && timerState.durationSec == 0) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(InkBlack.copy(alpha = 0.25f))
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(ButterPale)
            .padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          QUICK_TIMER_PRESET_MINUTES.forEach { mins ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(SurfaceWhite)
                .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp))
                .clickable(role = Role.Button) { onSelectPresetMinutes(mins) }
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = stringResource(R.string.cooking_timer_preset_fmt, mins),
                fontFamily = MonoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp,
                color = InkBlack
              )
            }
          }
          Spacer(Modifier.weight(1f))
          Box(
            modifier = Modifier
              .size(26.dp)
              .clip(RoundedCornerShape(7.dp))
              .background(SurfaceWhite)
              .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp))
              .clickable(role = Role.Button, onClick = onTogglePresets),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "×",
              fontFamily = DisplayFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp,
              color = InkBlack
            )
          }
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.5.dp)
          .background(InkBlack)
      )

      // Bottom Instruction Zone (~55% height): Large kitchen-readable copy
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(0.55f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = remember(step.text) { highlightStepMeasurements(step.text) },
          fontFamily = BodyFont,
          fontWeight = FontWeight.SemiBold,
          fontSize = 18.sp,
          lineHeight = 28.sp,
          color = InkBlack
        )

        if (showSwipeHint) {
          Text(
            text = stringResource(R.string.cooking_swipe_hint),
            modifier = Modifier.padding(top = 14.dp),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            color = InkBlack.copy(alpha = 0.52f)
          )
        }
      }
    }
  }
}

@Composable
private fun StepTimerControls(
  step: AnalyzedCookingStep,
  timerState: StepTimerState,
  onStartDetectedTimer: (Int) -> Unit,
  onTogglePresets: () -> Unit,
  onTogglePauseResume: () -> Unit,
  onAddOneMinute: () -> Unit,
  onResetTimer: () -> Unit
) {
  val resetCd = stringResource(R.string.cooking_timer_reset_cd)
  val pauseCd = stringResource(R.string.cooking_timer_pause_cd)

  if (timerState.durationSec == 0) {
    val detected = step.timer
    val pillBg = if (detected != null) LemonWarn else SurfaceWhite
    val pillText = if (detected != null) {
      "⏱ ${detected.displayLabel}"
    } else {
      "⏱ ${stringResource(R.string.cooking_timer_idle_default)}"
    }
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(pillBg)
        .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
        .clickable(role = Role.Button) {
          if (detected != null) onStartDetectedTimer(detected.totalSeconds) else onTogglePresets()
        }
        .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
      Text(
        text = pillText,
        fontFamily = MonoFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.5.sp,
        color = InkBlack,
        softWrap = false,
        maxLines = 1
      )
    }
  } else {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      val mainBg = when {
        timerState.isFinished -> CoralUrgent
        timerState.isRunning -> LimeFresh
        else -> ButterSoft
      }
      val mainText = if (timerState.isFinished) {
        "🔔 ${stringResource(R.string.cooking_timer_done)}"
      } else {
        val icon = if (timerState.isRunning) "⏸" else "▶"
        "$icon ${CookingStepAnalyzer.formatCountdown(timerState.remainingSec)}"
      }
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(mainBg)
          .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
          .semantics { contentDescription = pauseCd }
          .clickable(role = Role.Button) {
            if (timerState.isFinished) onResetTimer() else onTogglePauseResume()
          }
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = mainText,
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 11.5.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(SurfaceWhite)
          .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
          .clickable(role = Role.Button, onClick = onAddOneMinute)
          .padding(horizontal = 6.dp, vertical = 4.dp)
      ) {
        Text(
          text = stringResource(R.string.cooking_timer_add_min),
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 11.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(SurfaceWhite)
          .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
          .semantics { contentDescription = resetCd }
          .clickable(role = Role.Button, onClick = onResetTimer)
          .padding(horizontal = 6.dp, vertical = 4.dp)
      ) {
        Text(
          text = "↺",
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 11.5.sp,
          color = InkBlack
        )
      }
    }
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

@DrawableRes
private fun actionDrawableRes(action: CookingAction): Int = when (action) {
  CookingAction.PREP_CUT -> R.drawable.ill_step_prep_cut
  CookingAction.MARINATE_MIX -> R.drawable.ill_step_marinate_mix
  CookingAction.PAN_SEAR_STIR -> R.drawable.ill_step_pan_sear_stir
  CookingAction.BOIL_SIMMER -> R.drawable.ill_step_boil_simmer
  CookingAction.STEAM -> R.drawable.ill_step_steam
  CookingAction.BAKE_AIRFRY -> R.drawable.ill_step_bake_airfry
  CookingAction.BLEND_CHILL -> R.drawable.ill_step_blend_chill
  CookingAction.PLATE_SERVE -> R.drawable.ill_step_plate_serve
}

@Composable
private fun cookingActionTitle(action: CookingAction): String = when (action) {
  CookingAction.PREP_CUT -> stringResource(R.string.cooking_action_prep_cut)
  CookingAction.MARINATE_MIX -> stringResource(R.string.cooking_action_marinate_mix)
  CookingAction.PAN_SEAR_STIR -> stringResource(R.string.cooking_action_pan_sear_stir)
  CookingAction.BOIL_SIMMER -> stringResource(R.string.cooking_action_boil_simmer)
  CookingAction.STEAM -> stringResource(R.string.cooking_action_steam)
  CookingAction.BAKE_AIRFRY -> stringResource(R.string.cooking_action_bake_airfry)
  CookingAction.BLEND_CHILL -> stringResource(R.string.cooking_action_blend_chill)
  CookingAction.PLATE_SERVE -> stringResource(R.string.cooking_action_plate_serve)
}

@Composable
internal fun cookingActionShortLabel(action: CookingAction): String = when (action) {
  CookingAction.PREP_CUT -> stringResource(R.string.cooking_action_prep_cut_short)
  CookingAction.MARINATE_MIX -> stringResource(R.string.cooking_action_marinate_mix_short)
  CookingAction.PAN_SEAR_STIR -> stringResource(R.string.cooking_action_pan_sear_stir_short)
  CookingAction.BOIL_SIMMER -> stringResource(R.string.cooking_action_boil_simmer_short)
  CookingAction.STEAM -> stringResource(R.string.cooking_action_steam_short)
  CookingAction.BAKE_AIRFRY -> stringResource(R.string.cooking_action_bake_airfry_short)
  CookingAction.BLEND_CHILL -> stringResource(R.string.cooking_action_blend_chill_short)
  CookingAction.PLATE_SERVE -> stringResource(R.string.cooking_action_plate_serve_short)
}

private fun cookingActionColor(action: CookingAction): Color = when (action) {
  CookingAction.PREP_CUT -> LimeFresh.copy(alpha = 0.42f)
  CookingAction.MARINATE_MIX -> ButterSoft
  CookingAction.PAN_SEAR_STIR -> CoralUrgent.copy(alpha = 0.34f)
  CookingAction.BOIL_SIMMER -> SkyPale
  CookingAction.STEAM -> SkyInfo.copy(alpha = 0.32f)
  CookingAction.BAKE_AIRFRY -> LemonWarn.copy(alpha = 0.55f)
  CookingAction.BLEND_CHILL -> LilacAccent.copy(alpha = 0.32f)
  CookingAction.PLATE_SERVE -> LimeFresh.copy(alpha = 0.52f)
}
