package com.truong.fridgezero.ui.bento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.DisplayItem
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.StorageZone
import com.truong.fridgezero.domain.zoneForCompartment
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodGroup
import com.truong.fridgezero.ui.icons.FoodIcon
import com.truong.fridgezero.ui.theme.BodyFont
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
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import com.truong.fridgezero.ui.theme.dotGridBackground

enum class BentoFilter(val labelRes: Int) {
  ALL(R.string.filter_all),
  URGENT(R.string.filter_urgent),
  VEGGIES(R.string.filter_veggies),
  MEAT_FISH(R.string.filter_meat_fish),
  DAIRY_EGG(R.string.filter_dairy_egg),
  DRINKS(R.string.filter_drinks),
  FROZEN(R.string.filter_frozen)
}

/**
 * Screen 3 — Flat 2-Column Neo-Brutalist Bento Grid for managing food inside a compartment.
 */
@Composable
fun CompartmentBentoScreen(
  compartment: CompartmentUi,
  allCompartments: List<CompartmentUi>,
  recentKeys: List<String>,
  initialFilter: BentoFilter = BentoFilter.ALL,
  onSelectCompartment: (String) -> Unit,
  onBack: () -> Unit,
  onAddItem: (compartmentId: String, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onUpdateItem: (id: Long, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onAdjustQuantity: (id: Long, delta: Int) -> Unit,
  onToggleOpened: (id: Long) -> Unit,
  onMoveCompartment: (ids: List<Long>, targetCompartmentId: String, newDaysLeft: Int?) -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember(compartment.id, initialFilter) { mutableStateOf(initialFilter) }
  var showAddSheet by remember { mutableStateOf(false) }
  var detailTarget by remember { mutableStateOf<Pair<DisplayItem, Boolean>?>(null) }

  val isFreezerComp = zoneForCompartment(compartment) == StorageZone.FREEZER
  val filteredItems = remember(compartment.items, selectedFilter, isFreezerComp) {
    val list = when (selectedFilter) {
      BentoFilter.ALL -> compartment.items
      BentoFilter.URGENT -> compartment.items.filter { it.freshness != Freshness.OK }
      BentoFilter.VEGGIES -> compartment.items.filter {
        it.group == FoodGroup.RAU || it.group == FoodGroup.TRAI_CAY
      }
      BentoFilter.MEAT_FISH -> compartment.items.filter {
        it.group == FoodGroup.THIT || it.group == FoodGroup.CA_HAI_SAN
      }
      BentoFilter.DAIRY_EGG -> compartment.items.filter { it.group == FoodGroup.SUA_TRUNG }
      BentoFilter.DRINKS -> compartment.items.filter { it.group == FoodGroup.DO_UONG }
      BentoFilter.FROZEN -> compartment.items.filter {
        isFreezerComp || it.group == FoodGroup.AN_SAN || it.iconKey in setOf("ice_cream", "dumpling", "shrimp", "salmon")
      }
    }
    list.sortedWith(compareBy({ it.freshness.ordinal * -1 }, { it.minDaysLeft }))
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    // Top Bar: square ← button | "NGĂN MÁT · N MÓN" | + Thêm button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      val backCd = stringResource(R.string.bento_back_cd)
      NeoBrutalistButton(
        onClick = onBack,
        modifier = Modifier.size(44.dp).semantics { contentDescription = backCd },
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
        text = stringResource(R.string.bento_title_fmt, compartment.name.uppercase(), compartment.items.size),
        modifier = Modifier.weight(1f),
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        color = InkBlack,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      NeoBrutalistButton(
        onClick = { showAddSheet = true },
        containerColor = LimeFresh,
        cornerRadius = 12.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Text(
          text = stringResource(R.string.bento_add_button),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }
    }

    // Quick compartment switcher chips
    if (allCompartments.size > 1) {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
      ) {
        items(allCompartments, key = { it.id }) { comp ->
          val active = comp.id == compartment.id
          NeoBrutalistButton(
            onClick = { onSelectCompartment(comp.id) },
            containerColor = if (active) SkyInfo else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              text = "${comp.name} (${comp.items.size})",
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp,
              color = InkBlack
            )
          }
        }
      }
    }

    // Category filter chips
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      items(BentoFilter.entries) { filter ->
        val selected = selectedFilter == filter
        val chipBg = when {
          selected && filter == BentoFilter.URGENT -> CoralUrgent
          selected && filter == BentoFilter.FROZEN -> LilacAccent
          selected -> LemonWarn
          else -> SurfaceWhite
        }
        NeoBrutalistButton(
          onClick = { selectedFilter = filter },
          containerColor = chipBg,
          cornerRadius = 10.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp)
        ) {
          Text(
            text = stringResource(filter.labelRes),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = InkBlack
          )
        }
      }
    }

    // 2-Column Flat Bento Grid (zero text truncation)
    if (filteredItems.isEmpty()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        NeoBrutalistCard(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = stringResource(
              if (compartment.items.isEmpty()) R.string.bento_empty_compartment
              else R.string.bento_empty_filter
            ),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = InkBlack,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(20.dp)
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredItems, key = { it.primaryId }) { item ->
          FoodBentoCard(
            item = item,
            onCardClick = { detailTarget = item to false },
            onDecrement = {
              if (item.quantity <= 1) {
                detailTarget = item to true
              } else {
                onAdjustQuantity(item.primaryId, -1)
              }
            },
            onIncrement = { onAdjustQuantity(item.primaryId, +1) }
          )
        }
      }
    }
  }

  if (showAddSheet) {
    QuickAddItemSheet(
      compartments = allCompartments,
      initialCompartmentId = compartment.id,
      recentKeys = recentKeys,
      onSaveItem = onAddItem,
      onDismiss = { showAddSheet = false }
    )
  }

  val activeDetail = detailTarget
  if (activeDetail != null) {
    val latestItem = compartment.items.firstOrNull { it.primaryId == activeDetail.first.primaryId }
      ?: activeDetail.first
    ItemDetailSheet(
      item = latestItem,
      compartments = allCompartments,
      startWithDeleteReason = activeDetail.second,
      onUpdateItem = onUpdateItem,
      onToggleOpened = onToggleOpened,
      onMoveCompartment = onMoveCompartment,
      onMarkStatus = onMarkStatus,
      onDismiss = { detailTarget = null }
    )
  }
}

@Composable
private fun FoodBentoCard(
  item: DisplayItem,
  onCardClick: () -> Unit,
  onDecrement: () -> Unit,
  onIncrement: () -> Unit
) {
  val emoji = (EmojiIconRepository.iconFor(item.iconKey) as? FoodIcon.Emoji)?.glyph ?: "🍽️"
  val freshnessBg = when (item.freshness) {
    Freshness.BAD -> CoralUrgent
    Freshness.WARN -> LemonWarn
    Freshness.OK -> LimeFresh
  }
  val expiryText = when {
    item.minDaysLeft < 0 -> stringResource(R.string.urgent_badge_overdue_fmt, -item.minDaysLeft)
    item.minDaysLeft == 0 -> stringResource(R.string.urgent_badge_today)
    else -> stringResource(R.string.urgent_badge_days_fmt, item.minDaysLeft)
  }
  val minusCd = stringResource(R.string.bento_qty_minus_cd)
  val plusCd = stringResource(R.string.bento_qty_plus_cd)

  NeoBrutalistCard(
    modifier = Modifier.fillMaxWidth(),
    containerColor = SurfaceWhite,
    cornerRadius = 16.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 166.dp)
        .clickable(role = Role.Button, onClick = onCardClick)
        .padding(11.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top row: 48.dp emoji icon box + tilted freshness status pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PaperCream)
            .border(2.dp, InkBlack, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = emoji, fontSize = 28.sp)
        }
        StickerBadge(
          text = expiryText,
          containerColor = freshnessBg,
          tiltDegrees = -2f,
          mono = true
        )
      }

      Spacer(Modifier.height(8.dp))

      // Middle: full 2-line Vietnamese item name + group / opened tags
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
          text = item.name,
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          lineHeight = 18.sp,
          color = InkBlack,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(PaperCream)
              .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp))
              .padding(horizontal = 5.dp, vertical = 2.dp)
          ) {
            Text(
              text = item.group.labelVi,
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              color = InkBlack,
              maxLines = 1
            )
          }
          if (item.isOpened) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(ButterSoft)
                .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
              Text(
                text = stringResource(R.string.opened_badge),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                color = InkBlack,
                maxLines = 1
              )
            }
          }
        }
      }

      Spacer(Modifier.height(8.dp))

      // Bottom row: crisp 30.dp x 30.dp − [qty] + stepper buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        NeoBrutalistButton(
          onClick = onDecrement,
          modifier = Modifier.size(30.dp).semantics { contentDescription = minusCd },
          containerColor = if (item.quantity <= 1) Color(0xFFFFD6DF) else SurfaceWhite,
          cornerRadius = 8.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(
            text = "−",
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            color = InkBlack
          )
        }

        Text(
          text = if (item.unit.isNullOrBlank()) "${item.quantity}"
          else stringResource(R.string.bento_qty_unit_fmt, item.quantity, item.unit),
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        NeoBrutalistButton(
          onClick = onIncrement,
          modifier = Modifier.size(30.dp).semantics { contentDescription = plusCd },
          containerColor = LimeFresh,
          cornerRadius = 8.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(
            text = "+",
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            color = InkBlack
          )
        }
      }
    }
  }
}
