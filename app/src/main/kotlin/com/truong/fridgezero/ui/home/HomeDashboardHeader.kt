package com.truong.fridgezero.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.DisplayItem
import com.truong.fridgezero.domain.Freshness
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
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class UrgentEntry(
  val compartmentId: String,
  val compartmentName: String,
  val item: DisplayItem
)

private val CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM", Locale.US)

private fun DayOfWeek.toViShort(): String = when (this) {
  DayOfWeek.MONDAY -> "T2"
  DayOfWeek.TUESDAY -> "T3"
  DayOfWeek.WEDNESDAY -> "T4"
  DayOfWeek.THURSDAY -> "T5"
  DayOfWeek.FRIDAY -> "T6"
  DayOfWeek.SATURDAY -> "T7"
  DayOfWeek.SUNDAY -> "CN"
}

/**
 * Top Home Dashboard:
 * Row 1: Compact brand+weather caption + dynamic greeting | live HH:mm:ss + day/date pill
 * Row 2: Prominent centered expiring-items assistant banner directly above the 3D fridge
 */
@Composable
fun HomeDashboardHeader(
  compartments: List<CompartmentUi>,
  topRecommendation: RecipeRecommendation?,
  remindersEnabled: Boolean,
  onToggleReminders: () -> Unit,
  onOpenCompartment: (compartmentId: String, filterUrgent: Boolean) -> Unit,
  onOpenRecipes: (recipeId: String?) -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  modifier: Modifier = Modifier
) {
  var now by remember { mutableStateOf(LocalDateTime.now()) }
  LaunchedEffect(Unit) {
    while (true) {
      now = LocalDateTime.now()
      delay(1000L)
    }
  }

  val urgentEntries = remember(compartments) {
    compartments.flatMap { comp ->
      comp.items
        .filter { it.freshness == Freshness.SOON || it.freshness == Freshness.BAD }
        .map { UrgentEntry(comp.id, comp.name, it) }
    }.sortedBy { it.item.minDaysLeft }
  }
  val hasBad = urgentEntries.any { it.item.freshness == Freshness.BAD }
  var showUrgentSheet by remember { mutableStateOf(false) }

  val greetingRes = when (now.hour) {
    in 5..10 -> R.string.home_greeting_morning
    in 11..13 -> R.string.home_greeting_noon
    in 14..17 -> R.string.home_greeting_afternoon
    else -> R.string.home_greeting_evening
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Row 1: Merged brand + weather caption above Greeting | Live digital clock
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = stringResource(R.string.home_brand_weather),
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
          letterSpacing = 1.0.sp,
          color = InkBlack.copy(alpha = 0.65f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = stringResource(greetingRes),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 17.sp,
          color = InkBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      DigitalClockPill(now = now)
    }

    // Row 2: Alert banner + live recipe recommendation strip
    if (urgentEntries.isNotEmpty()) {
      NeoBrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (hasBad) CoralUrgent else LemonWarn,
        cornerRadius = 14.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 4.dp
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(role = Role.Button) { showUrgentSheet = true }
              .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = stringResource(R.string.home_urgent_banner_title_fmt, urgentEntries.size),
              modifier = Modifier.weight(1f),
              fontFamily = DisplayFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp,
              color = InkBlack,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceWhite)
                .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = stringResource(R.string.home_urgent_view_list),
                fontFamily = MonoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                color = InkBlack,
                softWrap = false,
                maxLines = 1
              )
            }
          }

          if (topRecommendation != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(InkBlack)
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(ButterSoft)
                .clickable(role = Role.Button) { onOpenRecipes(topRecommendation.recipe.id) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = stringResource(
                  R.string.home_banner_recipe_fmt,
                  topRecommendation.recipe.title,
                  topRecommendation.recipe.cookMinutes
                ),
                modifier = Modifier.weight(1f),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (topRecommendation.isReadyToCook) LimeFresh else LemonWarn)
                  .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = stringResource(
                    R.string.recipe_match_ratio_fmt,
                    topRecommendation.matchedCount,
                    topRecommendation.totalFridgeIngredients
                  ),
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
      }
    } else {
      val remindersCd = stringResource(R.string.home_reminders_cd)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        NeoBrutalistButton(
          onClick = { onOpenRecipes(topRecommendation?.recipe?.id) },
          modifier = Modifier.weight(1f),
          containerColor = LimeFresh,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = stringResource(R.string.home_all_fresh_title),
              fontFamily = MonoFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 10.sp,
              color = InkBlack.copy(alpha = 0.85f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (topRecommendation != null) {
              Text(
                text = stringResource(
                  R.string.home_banner_recipe_fmt,
                  topRecommendation.recipe.title,
                  topRecommendation.recipe.cookMinutes
                ),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        NeoBrutalistButton(
          onClick = { showUrgentSheet = true },
          modifier = Modifier
            .size(46.dp)
            .semantics { contentDescription = remindersCd },
          containerColor = if (remindersEnabled) ButterPale else SurfaceWhite,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(text = "🔔", fontSize = 16.sp)
        }
      }
    }
  }

  if (showUrgentSheet) {
    UrgentItemsBottomSheet(
      entries = urgentEntries,
      topRecommendation = topRecommendation,
      remindersEnabled = remindersEnabled,
      onToggleReminders = onToggleReminders,
      onMarkStatus = onMarkStatus,
      onOpenRecipe = { recipeId ->
        showUrgentSheet = false
        onOpenRecipes(recipeId)
      },
      onDismiss = { showUrgentSheet = false },
      onOpenCompartment = { compId ->
        showUrgentSheet = false
        onOpenCompartment(compId, true)
      }
    )
  }
}

@Composable
private fun DigitalClockPill(now: LocalDateTime) {
  val shape = RoundedCornerShape(10.dp)
  val timeStr = remember(now.second, now.minute, now.hour) { now.format(CLOCK_FMT) }
  val dateStr = remember(now.dayOfYear, now.year) {
    "${now.dayOfWeek.toViShort()}, ${now.format(DATE_FMT)}"
  }
  Box(modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .offset(3.dp, 3.dp)
        .background(LimeFresh, shape)
        .border(1.5.dp, InkBlack, shape)
    )
    Column(
      modifier = Modifier
        .background(InkBlack, shape)
        .border(2.dp, InkBlack, shape)
        .padding(horizontal = 10.dp, vertical = 5.dp),
      horizontalAlignment = Alignment.End
    ) {
      Text(
        text = timeStr,
        fontFamily = MonoFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        color = Color.White
      )
      Text(
        text = dateStr,
        fontFamily = MonoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        color = LimeFresh
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrgentItemsBottomSheet(
  entries: List<UrgentEntry>,
  topRecommendation: RecipeRecommendation?,
  remindersEnabled: Boolean,
  onToggleReminders: () -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  onOpenRecipe: (String) -> Unit,
  onDismiss: () -> Unit,
  onOpenCompartment: (String) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
        .padding(horizontal = 16.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.urgent_sheet_title),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = InkBlack
          )
          Text(
            text = stringResource(R.string.urgent_sheet_subtitle),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = InkBlack.copy(alpha = 0.7f)
          )
        }
        if (entries.isNotEmpty()) {
          StickerBadge(
            text = "${entries.size}",
            containerColor = CoralUrgent,
            mono = true
          )
        }
      }

      if (entries.isEmpty()) {
        NeoBrutalistCard(modifier = Modifier.fillMaxWidth(), containerColor = LimeFresh) {
          Text(
            text = stringResource(R.string.urgent_sheet_empty),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = InkBlack,
            modifier = Modifier.padding(16.dp)
          )
        }
      } else {
        // Highlighted clickable recipe suggestion card linked directly to CulinaryGuideScreen
        if (topRecommendation != null) {
          val recipe = topRecommendation.recipe
          NeoBrutalistCard(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(role = Role.Button) { onOpenRecipe(recipe.id) },
            containerColor = ButterSoft,
            cornerRadius = 14.dp,
            borderWidth = 2.5.dp,
            shadowOffset = 3.dp
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = stringResource(R.string.urgent_meal_title),
                  fontFamily = DisplayFont,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 13.sp,
                  color = InkBlack
                )
                StickerBadge(
                  text = stringResource(
                    R.string.recipe_match_ratio_fmt,
                    topRecommendation.matchedCount,
                    topRecommendation.totalFridgeIngredients
                  ),
                  containerColor = if (topRecommendation.isReadyToCook) LimeFresh else LemonWarn,
                  tiltDegrees = 0f,
                  mono = true
                )
              }
              Text(
                text = stringResource(
                  R.string.urgent_meal_recipe_fmt,
                  recipe.emoji,
                  recipe.title,
                  recipe.cookMinutes
                ),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = InkBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = stringResource(R.string.urgent_meal_cta),
                fontFamily = MonoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = InkBlack
              )
            }
          }
        }

        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(entries, key = { "${it.compartmentId}_${it.item.primaryId}" }) { entry ->
            UrgentItemRow(
              entry = entry,
              onMarkUsedUp = { onMarkStatus(entry.item.itemIds, ItemStatus.USED_UP) },
              onOpenCompartment = { onOpenCompartment(entry.compartmentId) }
            )
          }
        }
      }

      NeoBrutalistButton(
        onClick = onToggleReminders,
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (remindersEnabled) LimeFresh else SurfaceWhite,
        cornerRadius = 12.dp,
        shadowOffset = 3.dp
      ) {
        Text(
          text = "${stringResource(R.string.reminder_toggle_label)} · ${
            stringResource(if (remindersEnabled) R.string.reminder_toggle_on else R.string.reminder_toggle_off)
          }",
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }
    }
  }
}

@Composable
private fun UrgentItemRow(
  entry: UrgentEntry,
  onMarkUsedUp: () -> Unit,
  onOpenCompartment: () -> Unit
) {
  val item = entry.item
  val icon = EmojiIconRepository.iconFor(item.iconKey)
  val emoji = (icon as? FoodIcon.Emoji)?.glyph ?: "🍽️"
  val daysText = when {
    item.minDaysLeft < 0 -> stringResource(R.string.urgent_badge_overdue_fmt, -item.minDaysLeft)
    item.minDaysLeft == 0 -> stringResource(R.string.urgent_badge_today)
    else -> stringResource(R.string.urgent_badge_days_fmt, item.minDaysLeft)
  }
  val badgeColor = if (item.freshness == Freshness.BAD) CoralUrgent else LemonWarn

  NeoBrutalistCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp, shadowOffset = 3.dp) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PaperCream)
            .border(2.dp, InkBlack, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = emoji, fontSize = 24.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = item.name,
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = InkBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            StickerBadge(
              text = entry.compartmentName,
              containerColor = SurfaceWhite,
              tiltDegrees = 0f
            )
            StickerBadge(
              text = daysText,
              containerColor = badgeColor,
              tiltDegrees = -1.5f,
              mono = true
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        NeoBrutalistButton(
          onClick = onMarkUsedUp,
          modifier = Modifier.weight(1f),
          containerColor = LimeFresh,
          cornerRadius = 10.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(
            text = stringResource(R.string.urgent_sheet_mark_used_up),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = InkBlack,
            maxLines = 1
          )
        }
        NeoBrutalistButton(
          onClick = onOpenCompartment,
          modifier = Modifier.weight(1f),
          containerColor = SurfaceWhite,
          cornerRadius = 10.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(
            text = stringResource(R.string.urgent_sheet_open_compartment),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = InkBlack,
            maxLines = 1
          )
        }
      }
    }
  }
}
