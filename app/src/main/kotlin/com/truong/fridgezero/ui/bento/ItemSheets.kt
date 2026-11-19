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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.ConfigurableShelfLifeRepository
import com.truong.fridgezero.domain.DisplayItem
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.StorageZone
import com.truong.fridgezero.domain.zoneForCompartment
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodGroup
import com.truong.fridgezero.ui.icons.FoodIcon
import com.truong.fridgezero.ui.icons.IconEntry
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val UNITS = listOf("cái", "gói", "hộp", "kg", "chai", "quả")
private val QUICK_EXPIRY_CHIPS = listOf(
  3 to R.string.quick_add_3d,
  7 to R.string.quick_add_7d,
  14 to R.string.quick_add_14d,
  30 to R.string.quick_add_30d
)
private val DEFAULT_FAVORITE_KEYS = listOf("milk", "egg", "chicken", "carrot", "lettuce", "salmon", "orange_juice", "apple")

private fun suggestCompartmentId(
  group: FoodGroup,
  iconKey: String,
  compartments: List<CompartmentUi>
): String? {
  if (compartments.isEmpty()) return null
  val preferredZone = when {
    iconKey in setOf("ice_cream", "dumpling", "shrimp") -> StorageZone.FREEZER
    group == FoodGroup.RAU -> StorageZone.DRAWER
    else -> StorageZone.CHILLED
  }
  return compartments.firstOrNull { zoneForCompartment(it) == preferredZone }?.id
    ?: compartments.firstOrNull { zoneForCompartment(it) == StorageZone.CHILLED }?.id
    ?: compartments.first().id
}

/**
 * Fast Add Item Sheet (<= 3 taps) used from both CompartmentBentoScreen (+ Thêm)
 * and HomeScreen's floating "+ Thêm nhanh" CTA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddItemSheet(
  compartments: List<CompartmentUi>,
  initialCompartmentId: String?,
  recentKeys: List<String>,
  onSaveItem: (compartmentId: String, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedCompartmentId by remember(initialCompartmentId, compartments) {
    mutableStateOf(
      initialCompartmentId
        ?: suggestCompartmentId(FoodGroup.KHAC, "milk", compartments)
        ?: compartments.firstOrNull()?.id.orEmpty()
    )
  }
  var compartmentPinnedManually by remember(initialCompartmentId) {
    mutableStateOf(initialCompartmentId != null)
  }
  var selectedIconKey by remember { mutableStateOf("milk") }
  var itemName by remember { mutableStateOf("") }
  var iconQuery by remember { mutableStateOf("") }
  var quantity by remember { mutableIntStateOf(1) }
  var selectedUnit by remember { mutableStateOf<String?>("hộp") }
  var daysLeftText by remember { mutableStateOf("") }
  var expiryPinnedManually by remember { mutableStateOf(false) }
  var autoSuggestedZoneLabel by remember { mutableStateOf<String?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }

  val zoneChilledLabel = stringResource(R.string.zone_label_chilled)
  val zoneDrawerLabel = stringResource(R.string.zone_label_drawer)
  val zoneFreezerLabel = stringResource(R.string.zone_label_freezer)

  val quickKeys = remember(recentKeys) {
    (recentKeys + DEFAULT_FAVORITE_KEYS).distinct().take(8)
  }
  val quickEntries = remember(quickKeys) {
    quickKeys.mapNotNull { key -> EmojiIconRepository.entries.firstOrNull { it.key == key } }
  }
  val filteredIcons = remember(iconQuery) { EmojiIconRepository.search(iconQuery) }

  fun applySuggestedExpiry(iconKey: String, compId: String) {
    val selectedComp = compartments.firstOrNull { it.id == compId }
    val zone = selectedComp?.let(::zoneForCompartment) ?: zoneForCompartment(compId)
    val suggested = ConfigurableShelfLifeRepository.suggestDays(
      iconKey = iconKey,
      group = EmojiIconRepository.groupFor(iconKey),
      zone = zone
    )
    if (suggested != null) {
      daysLeftText = suggested.toString()
      autoSuggestedZoneLabel = when (zone) {
        StorageZone.CHILLED -> zoneChilledLabel
        StorageZone.DRAWER -> zoneDrawerLabel
        StorageZone.FREEZER -> zoneFreezerLabel
      }
    } else {
      autoSuggestedZoneLabel = null
    }
  }

  fun pickIcon(entry: IconEntry, fillName: Boolean) {
    selectedIconKey = entry.key
    if (fillName || itemName.isBlank()) {
      itemName = entry.label
    }
    if (!compartmentPinnedManually) {
      suggestCompartmentId(entry.group, entry.key, compartments)?.let {
        selectedCompartmentId = it
      }
    }
    expiryPinnedManually = false
    applySuggestedExpiry(entry.key, selectedCompartmentId)
  }

  fun resetForNextEntry() {
    itemName = ""
    iconQuery = ""
    quantity = 1
    daysLeftText = ""
    expiryPinnedManually = false
    autoSuggestedZoneLabel = null
  }

  val parsedDays = daysLeftText.toIntOrNull()
  val canSave = itemName.isNotBlank() && parsedDays != null && selectedCompartmentId.isNotEmpty()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = PaperCream,
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(R.string.add_item_title).uppercase(),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 20.sp,
          color = InkBlack
        )
        val previewEmoji = (EmojiIconRepository.iconFor(selectedIconKey) as? FoodIcon.Emoji)?.glyph ?: "🍽️"
        StickerBadge(text = previewEmoji, containerColor = LimeFresh, tiltDegrees = 2f)
      }

      // Recent / quick-pick chips (1-tap fills icon, name & smart compartment)
      Text(
        text = stringResource(R.string.recent_items_label),
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = InkBlack
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(quickEntries, key = { it.key }) { entry ->
          val active = selectedIconKey == entry.key && itemName == entry.label
          NeoBrutalistButton(
            onClick = { pickIcon(entry, fillName = true) },
            containerColor = if (active) LimeFresh else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(text = entry.glyph, fontSize = 15.sp)
            Text(
              text = entry.label,
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
      }

      // Item name + icon search row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        BrutalInputBox(
          value = itemName,
          onValueChange = { itemName = it },
          placeholder = stringResource(R.string.quick_add_name_placeholder),
          modifier = Modifier.weight(1.3f)
        )
        BrutalInputBox(
          value = iconQuery,
          onValueChange = { iconQuery = it },
          placeholder = stringResource(R.string.search_icon_hint),
          modifier = Modifier.weight(1f)
        )
      }

      // Searchable emoji icon row
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filteredIcons, key = { it.key }) { entry ->
          val selected = selectedIconKey == entry.key
          NeoBrutalistButton(
            onClick = { pickIcon(entry, fillName = false) },
            containerColor = if (selected) ButterSoft else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(text = entry.glyph, fontSize = 18.sp)
          }
        }
      }

      // Compartment chips
      Text(
        text = stringResource(R.string.target_compartment_label),
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = InkBlack
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(compartments, key = { it.id }) { comp ->
          val selected = comp.id == selectedCompartmentId
          NeoBrutalistButton(
            onClick = {
              selectedCompartmentId = comp.id
              compartmentPinnedManually = true
              if (!expiryPinnedManually) {
                applySuggestedExpiry(selectedIconKey, comp.id)
              }
            },
            containerColor = if (selected) SkyInfo else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = comp.name,
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
      }

      // Quantity stepper + unit chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = stringResource(R.string.field_quantity),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack
        )
        NeoBrutalistButton(
          onClick = { if (quantity > 1) quantity-- },
          cornerRadius = 8.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp)
        ) { Text("−", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = InkBlack) }
        Text(
          text = "$quantity",
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 16.sp,
          color = InkBlack
        )
        NeoBrutalistButton(
          onClick = { quantity++ },
          cornerRadius = 8.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp)
        ) { Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = InkBlack) }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(UNITS) { unit ->
          val active = selectedUnit == unit
          NeoBrutalistButton(
            onClick = { selectedUnit = if (active) null else unit },
            containerColor = if (active) LemonWarn else SurfaceWhite,
            cornerRadius = 8.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              text = unit,
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
      }

      // Expiry quick chips + manual input + DatePicker + packaging hint
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(R.string.quick_add_expiry_label),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack
        )
        Text(
          text = stringResource(R.string.expiry_packaging_hint),
          fontFamily = BodyFont,
          fontStyle = FontStyle.Italic,
          fontSize = 11.sp,
          color = InkBlack.copy(alpha = 0.65f)
        )
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(QUICK_EXPIRY_CHIPS) { (days, labelRes) ->
          val active = parsedDays == days
          NeoBrutalistButton(
            onClick = {
              daysLeftText = days.toString()
              expiryPinnedManually = true
              autoSuggestedZoneLabel = null
            },
            containerColor = if (active) LimeFresh else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = stringResource(labelRes),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
        item {
          NeoBrutalistButton(
            onClick = { showDatePicker = true },
            containerColor = ButterSoft,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "📅 ${stringResource(R.string.pick_date_label)}",
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
      }

      BrutalInputBox(
        value = daysLeftText,
        onValueChange = { raw ->
          daysLeftText = raw.filter { it.isDigit() || it == '-' }.take(4)
          expiryPinnedManually = true
          autoSuggestedZoneLabel = null
        },
        placeholder = stringResource(R.string.quick_add_expiry_required_hint),
        keyboardType = KeyboardType.Number,
        modifier = Modifier.fillMaxWidth()
      )
      autoSuggestedZoneLabel?.let { zoneLabel ->
        Text(
          text = stringResource(R.string.quick_add_usda_auto_hint, zoneLabel),
          fontFamily = MonoFont,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          color = InkBlack.copy(alpha = 0.72f)
        )
      }

      // Dual Save / Save & Continue buttons
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        NeoBrutalistButton(
          onClick = {
            val d = parsedDays ?: return@NeoBrutalistButton
            onSaveItem(selectedCompartmentId, itemName, selectedIconKey, quantity, d, selectedUnit)
            resetForNextEntry()
          },
          enabled = canSave,
          modifier = Modifier.weight(1f),
          containerColor = LemonWarn
        ) {
          Text(
            text = stringResource(R.string.action_save_and_next),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = InkBlack
          )
        }
        NeoBrutalistButton(
          onClick = {
            val d = parsedDays ?: return@NeoBrutalistButton
            onSaveItem(selectedCompartmentId, itemName, selectedIconKey, quantity, d, selectedUnit)
            onDismiss()
          },
          enabled = canSave,
          modifier = Modifier.weight(1f),
          containerColor = LimeFresh
        ) {
          Text(
            text = stringResource(R.string.action_save),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = InkBlack
          )
        }
      }
    }
  }

  if (showDatePicker) {
    val pickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { millis ->
            val selectedDay = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
            val diff = (selectedDay - LocalDate.now().toEpochDay()).toInt()
            daysLeftText = diff.toString()
            expiryPinnedManually = true
            autoSuggestedZoneLabel = null
          }
          showDatePicker = false
        }) {
          Text(stringResource(R.string.action_save), fontWeight = FontWeight.ExtraBold, color = InkBlack)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(stringResource(R.string.reminder_prompt_later), color = InkBlack)
        }
      }
    ) {
      DatePicker(state = pickerState)
    }
  }
}

/**
 * Item Detail, Edit, Compartment Move & 1-Tap Direct Delete Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
  item: DisplayItem,
  compartments: List<CompartmentUi>,
  onUpdateItem: (id: Long, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onMoveCompartment: (ids: List<Long>, targetCompartmentId: String, newDaysLeft: Int?) -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember(item.primaryId) { mutableStateOf(item.name) }
  var quantity by remember(item.primaryId, item.quantity) { mutableIntStateOf(item.quantity) }
  var unit by remember(item.primaryId, item.unit) { mutableStateOf(item.unit) }
  var daysText by remember(item.primaryId, item.minDaysLeft) { mutableStateOf(item.minDaysLeft.toString()) }
  var pendingMoveTargetId by remember { mutableStateOf<String?>(null) }

  val emoji = (EmojiIconRepository.iconFor(item.iconKey) as? FoodIcon.Emoji)?.glyph ?: "🍽️"
  val badgeColor = when (item.freshness) {
    Freshness.BAD -> CoralUrgent
    Freshness.SOON -> LemonWarn
    Freshness.OK -> LimeFresh
  }
  val expiryBadgeText = when {
    item.minDaysLeft < 0 -> stringResource(R.string.days_overdue_fmt, -item.minDaysLeft)
    item.minDaysLeft == 0 -> stringResource(R.string.days_today)
    else -> stringResource(R.string.days_left_fmt, item.minDaysLeft)
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
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header
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
          Text(text = emoji, fontSize = 32.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = item.name,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp,
            color = InkBlack,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StickerBadge(text = item.group.labelVi, containerColor = SurfaceWhite, tiltDegrees = 0f)
            StickerBadge(text = expiryBadgeText, containerColor = badgeColor, tiltDegrees = -1.5f, mono = true)
          }
        }
      }

      // Edit name + days + quantity
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        BrutalInputBox(
          value = name,
          onValueChange = { name = it },
          placeholder = stringResource(R.string.field_name),
          modifier = Modifier.weight(1.6f)
        )
        BrutalInputBox(
          value = daysText,
          onValueChange = { raw -> daysText = raw.filter { it.isDigit() || it == '-' }.take(4) },
          placeholder = stringResource(R.string.field_days_left),
          keyboardType = KeyboardType.Number,
          modifier = Modifier.weight(0.8f)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        NeoBrutalistButton(
          onClick = { if (quantity > 1) quantity-- },
          cornerRadius = 8.dp, borderWidth = 2.dp, shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp)
        ) { Text("−", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = InkBlack) }
        Text(
          text = if (unit.isNullOrBlank()) "$quantity" else "$quantity $unit",
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp,
          color = InkBlack
        )
        NeoBrutalistButton(
          onClick = { quantity++ },
          cornerRadius = 8.dp, borderWidth = 2.dp, shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp)
        ) { Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = InkBlack) }
        Spacer(Modifier.weight(1f))
        NeoBrutalistButton(
          onClick = {
            val d = daysText.toIntOrNull() ?: item.minDaysLeft
            onUpdateItem(item.primaryId, name, item.iconKey, quantity, d, unit)
            onDismiss()
          },
          enabled = name.isNotBlank() && daysText.toIntOrNull() != null,
          containerColor = LimeFresh,
          cornerRadius = 10.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
        ) {
          Text(
            text = stringResource(R.string.action_save),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = InkBlack
          )
        }
      }

      // Move compartment
      val otherCompartments = compartments.filter { it.id != item.compartmentId }
      val fromComp = compartments.firstOrNull { it.id == item.compartmentId }
      val fromZone = fromComp?.let(::zoneForCompartment) ?: zoneForCompartment(item.compartmentId)
      if (otherCompartments.isNotEmpty()) {
        Text(
          text = stringResource(R.string.action_move_compartment),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(otherCompartments, key = { it.id }) { target ->
            NeoBrutalistButton(
              onClick = {
                val fromFreezer = fromZone == StorageZone.FREEZER
                val toFreezer = zoneForCompartment(target) == StorageZone.FREEZER
                if (fromFreezer != toFreezer) {
                  pendingMoveTargetId = target.id
                } else {
                  onMoveCompartment(item.itemIds, target.id, null)
                  onDismiss()
                }
              },
              containerColor = Color(0xFFE0F2FE),
              cornerRadius = 10.dp,
              borderWidth = 2.dp,
              shadowOffset = 2.dp,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = "→ ${target.name}",
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = InkBlack
              )
            }
          }
        }
      }

      // Chilled <-> Freezer expiry recalculation prompt
      val moveTarget = pendingMoveTargetId
      if (moveTarget != null) {
        val targetComp = compartments.firstOrNull { it.id == moveTarget }
        val movingToFreezer = (targetComp?.let(::zoneForCompartment) ?: zoneForCompartment(moveTarget)) == StorageZone.FREEZER
        NeoBrutalistCard(modifier = Modifier.fillMaxWidth(), containerColor = ButterSoft, cornerRadius = 12.dp, shadowOffset = 3.dp) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = stringResource(R.string.move_recalc_prompt),
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = InkBlack
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              NeoBrutalistButton(
                onClick = {
                  onMoveCompartment(item.itemIds, moveTarget, null)
                  onDismiss()
                },
                modifier = Modifier.weight(1f),
                containerColor = SurfaceWhite,
                cornerRadius = 8.dp, borderWidth = 2.dp, shadowOffset = 2.dp
              ) {
                Text(stringResource(R.string.move_keep_expiry), fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = InkBlack)
              }
              NeoBrutalistButton(
                onClick = {
                  val recalculated = if (movingToFreezer) (item.minDaysLeft.coerceAtLeast(0) + 30) else 3
                  onMoveCompartment(item.itemIds, moveTarget, recalculated)
                  onDismiss()
                },
                modifier = Modifier.weight(1f),
                containerColor = if (movingToFreezer) LilacAccent else LimeFresh,
                cornerRadius = 8.dp, borderWidth = 2.dp, shadowOffset = 2.dp
              ) {
                Text(
                  text = stringResource(if (movingToFreezer) R.string.move_add_freezer_days else R.string.move_set_chilled_days),
                  fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = InkBlack
                )
              }
            }
          }
        }
      }

      // 1-Tap Direct Deletion
      NeoBrutalistButton(
        onClick = {
          onMarkStatus(item.itemIds, ItemStatus.USED_UP)
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth(),
        containerColor = CoralUrgent
      ) {
        Text(
          text = "🗑️ ${stringResource(R.string.action_delete)}",
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          color = InkBlack
        )
      }
    }
  }
}

@Composable
private fun BrutalInputBox(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  keyboardType: KeyboardType = KeyboardType.Text
) {
  val shape = RoundedCornerShape(10.dp)
  Box(
    modifier = modifier
      .clip(shape)
      .background(SurfaceWhite)
      .border(2.dp, InkBlack, shape)
      .padding(horizontal = 10.dp, vertical = 9.dp)
  ) {
    if (value.isEmpty()) {
      Text(
        text = placeholder,
        fontFamily = BodyFont,
        fontSize = 12.sp,
        color = InkBlack.copy(alpha = 0.45f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
      textStyle = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = InkBlack
      ),
      cursorBrush = SolidColor(InkBlack),
      modifier = Modifier.fillMaxWidth()
    )
  }
}
