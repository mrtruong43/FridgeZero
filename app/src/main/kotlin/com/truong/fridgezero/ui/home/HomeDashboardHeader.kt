package com.truong.fridgezero.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.DisplayItem
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodIcon
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
 * Top Home Dashboard (spec §3.2):
 * Row 1: FRIDGEZERO + dynamic greeting | live HH:mm:ss + day/date pill
 * Row 2: Weather badge (☀️ 28°C · Đà Nẵng) | Interactive expiring-items alert badge
 */
@Composable
fun HomeDashboardHeader(
  compartments: List<CompartmentUi>,
  remindersEnabled: Boolean,
  onToggleReminders: () -> Unit,
  onOpenCompartment: (compartmentId: String, filterUrgent: Boolean) -> Unit,
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
        .filter { it.freshness == Freshness.WARN || it.freshness == Freshness.BAD }
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
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Row 1: Brand + Greeting | Live digital clock
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = stringResource(R.string.home_brand),
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
          letterSpacing = 1.4.sp,
          color = InkBlack.copy(alpha = 0.65f)
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

    // Row 2: Weather badge | Urgent expiring alert badge
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      WeatherBadge()

      val alertBg = when {
        hasBad -> CoralUrgent
        urgentEntries.isNotEmpty() -> LemonWarn
        else -> LimeFresh
      }
      val alertLabel = if (urgentEntries.isEmpty()) {
        stringResource(R.string.home_all_fresh_badge)
      } else {
        stringResource(R.string.home_urgent_badge_fmt, urgentEntries.size)
      }

      NeoBrutalistButton(
        onClick = { showUrgentSheet = true },
        containerColor = alertBg,
        cornerRadius = 10.dp,
        borderWidth = 2.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = alertLabel,
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack,
          maxLines = 1
        )
      }
    }
  }

  if (showUrgentSheet) {
    UrgentItemsBottomSheet(
      entries = urgentEntries,
      remindersEnabled = remindersEnabled,
      onToggleReminders = onToggleReminders,
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

@Composable
private fun WeatherBadge() {
  val shape = RoundedCornerShape(10.dp)
  Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .offset(2.dp, 2.dp)
        .background(InkBlack, shape)
    )
    Box(
      modifier = Modifier
        .background(SkyPale, shape)
        .border(2.dp, InkBlack, shape)
        .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      Text(
        text = stringResource(R.string.home_weather_badge),
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = InkBlack,
        maxLines = 1
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrgentItemsBottomSheet(
  entries: List<UrgentEntry>,
  remindersEnabled: Boolean,
  onToggleReminders: () -> Unit,
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
        LazyColumn(
          modifier = Modifier.fillMaxWidth().height(310.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(entries, key = { "${it.compartmentId}_${it.item.primaryId}" }) { entry ->
            UrgentItemRow(
              entry = entry,
              onClick = { onOpenCompartment(entry.compartmentId) }
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
  onClick: () -> Unit
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
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
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
      NeoBrutalistButton(
        onClick = onClick,
        containerColor = LimeFresh,
        cornerRadius = 10.dp,
        borderWidth = 2.dp,
        shadowOffset = 2.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
      ) {
        Text(
          text = stringResource(R.string.urgent_sheet_open_compartment),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack
        )
      }
    }
  }
}
