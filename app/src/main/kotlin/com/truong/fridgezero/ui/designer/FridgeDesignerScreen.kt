package com.truong.fridgezero.ui.designer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.data.FridgeEntity
import com.truong.fridgezero.data.Seed
import com.truong.fridgezero.ui.fridge3d.Fridge3DStageSlot
import com.truong.fridgezero.ui.fridge3d.LocalFridge3DStageController
import com.truong.fridgezero.ui.fridge3d.ShellColors
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.SkyInfo
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite

private val TEMPLATES = listOf(
  "single" to R.string.template_single,
  "top_freezer" to R.string.template_top_freezer,
  "bottom_freezer" to R.string.template_bottom_freezer,
  "side_by_side" to R.string.template_side_by_side,
  "french_door" to R.string.template_french_door,
  "mini" to R.string.template_mini
)

private fun colorLabel(key: String): Int = when (key) {
  "butter" -> R.string.color_butter
  "mint" -> R.string.color_mint
  "matte" -> R.string.color_matte
  else -> R.string.color_inox
}

/** Screen 1 — live 3D preview (top ~43%) + Neo-Brutalist controls + "CHỐT TỦ NÀY →". */
@Composable
fun FridgeDesignerScreen(
  initialFridge: FridgeEntity,
  initialCompartments: List<CompartmentEntity>,
  onConfirm: (FridgeEntity, List<CompartmentEntity>) -> Unit,
  modifier: Modifier = Modifier
) {
  var template by rememberSaveable { mutableStateOf(initialFridge.template) }
  var colorKey by rememberSaveable { mutableStateOf(ShellColors.normalizeKey(initialFridge.colorKey)) }
  var compartments by remember {
    mutableStateOf(initialCompartments.sortedBy { it.sort }.ifEmpty {
      Seed.compartmentsForTemplate(initialFridge.template, initialFridge.id)
    })
  }
  var openId by remember { mutableStateOf<String?>(null) }

  val fridgeId = initialFridge.id
  val doorFmt = stringResource(R.string.designer_new_door_fmt, compartments.size + 1)
  val drawerFmt = stringResource(R.string.designer_new_drawer_fmt, compartments.size + 1)

  fun applyTemplate(key: String) {
    template = key
    compartments = Seed.compartmentsForTemplate(key, fridgeId)
    openId = null
  }

  fun addCompartment(kind: CompartmentKind) {
    if (compartments.size >= MAX_COMPARTMENTS) return
    val c = CompartmentEntity(
      id = "c_" + System.currentTimeMillis().toString(36),
      fridgeId = fridgeId,
      name = if (kind == CompartmentKind.DOOR) doorFmt else drawerFmt,
      x = 0f, y = 0f, w = 1f, h = 1f,
      kind = kind,
      shelves = if (kind == CompartmentKind.DOOR) 2 else 1,
      sort = compartments.size
    )
    compartments = normalizeCompartmentLayout(compartments + c, template)
    openId = c.id
  }

  fun update(id: String, change: (CompartmentEntity) -> CompartmentEntity, relayout: Boolean = false) {
    val next = compartments.map { if (it.id == id) change(it) else it }
    compartments = if (relayout) normalizeCompartmentLayout(next, template) else next
  }

  fun remove(id: String) {
    if (compartments.size <= 1) return
    compartments = normalizeCompartmentLayout(compartments.filterNot { it.id == id }, template)
    if (openId == id) openId = null
  }

  val screenH = LocalConfiguration.current.screenHeightDp.dp
  val templateLabel = TEMPLATES.firstOrNull { it.first == template }?.second ?: R.string.template_single
  val stageController = LocalFridge3DStageController.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    // Top bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          stringResource(R.string.designer_title),
          fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold,
          fontSize = 22.sp, color = InkBlack
        )
        Text(
          stringResource(R.string.designer_subtitle),
          fontFamily = BodyFont, fontWeight = FontWeight.Medium,
          fontSize = 12.sp, color = InkBlack.copy(alpha = 0.7f)
        )
      }
      StickerBadge(stringResource(R.string.designer_step_badge), containerColor = LemonWarn, mono = true)
    }

    // Live 3D preview (~43% of screen height)
    NeoBrutalistCard(
      transparentInterior = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(screenH * 0.43f)
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Fridge3DStageSlot(
          colorKey = colorKey,
          compartments = compartments,
          template = template,
          openCompartmentId = openId,
          surfaceBg = "#FFFFFF",
          cornerRadius = 16.dp,
          backdropColor = SurfaceWhite,
          onDoorClick = { id -> openId = if (openId == id) null else id },
          showStickers = false,
          modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
          StickerBadge(
            stringResource(
              if (stageController.use2DFallback) R.string.fallback_2d_badge else R.string.designer_swipe_hint
            ),
            containerColor = if (stageController.use2DFallback) LemonWarn else SurfaceWhite,
            tiltDegrees = 0f
          )
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
          StickerBadge(stringResource(templateLabel), containerColor = LimeFresh, tiltDegrees = 1f)
        }
      }
    }

    // Scrollable controls
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(top = 14.dp, bottom = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      SectionTitle(stringResource(R.string.designer_section_template))
      LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(TEMPLATES) { (key, label) ->
          NeoBrutalistButton(
            onClick = { applyTemplate(key) },
            containerColor = if (template == key) LimeFresh else SurfaceWhite,
            shadowOffset = 3.dp,
            borderWidth = 2.dp
          ) {
            Text(stringResource(label), fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = InkBlack)
          }
        }
      }

      SectionTitle(stringResource(R.string.designer_section_color))
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ShellColors.all.forEach { spec ->
          val selected = colorKey == spec.key
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            NeoBrutalistButton(
              onClick = { colorKey = spec.key },
              modifier = Modifier.fillMaxWidth(),
              containerColor = if (selected) LimeFresh else SurfaceWhite,
              shadowOffset = 3.dp,
              borderWidth = 2.dp,
              contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp, horizontal = 4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(26.dp)
                  .clip(CircleShape)
                  .background(spec.color)
                  .border(2.dp, InkBlack, CircleShape)
              )
            }
            Text(
              stringResource(colorLabel(spec.key)),
              fontFamily = BodyFont, fontWeight = FontWeight.Bold,
              fontSize = 11.sp, color = InkBlack,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }

      SectionTitle(stringResource(R.string.designer_section_compartments))
      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        compartments.forEachIndexed { index, c ->
          CompartmentRow(
            index = index,
            compartment = c,
            isSelected = c.id == openId,
            canRemove = compartments.size > 1,
            onFocus = { openId = if (openId == c.id) null else c.id },
            onName = { name -> update(c.id, { it.copy(name = name) }) },
            onToggleKind = {
              update(
                c.id,
                {
                  val next = if (it.kind == CompartmentKind.DOOR) CompartmentKind.DRAWER else CompartmentKind.DOOR
                  it.copy(kind = next)
                },
                relayout = true
              )
            },
            onShelves = { delta -> update(c.id, { it.copy(shelves = (it.shelves + delta).coerceIn(1, 5)) }) },
            onRemove = { remove(c.id) }
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          NeoBrutalistButton(
            onClick = { addCompartment(CompartmentKind.DOOR) },
            modifier = Modifier.weight(1f),
            enabled = compartments.size < MAX_COMPARTMENTS,
            containerColor = LilacAccent,
            shadowOffset = 3.dp
          ) {
            Text(stringResource(R.string.designer_add_door), fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = InkBlack)
          }
          NeoBrutalistButton(
            onClick = { addCompartment(CompartmentKind.DRAWER) },
            modifier = Modifier.weight(1f),
            enabled = compartments.size < MAX_COMPARTMENTS,
            containerColor = SkyInfo,
            shadowOffset = 3.dp
          ) {
            Text(stringResource(R.string.designer_add_drawer), fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = InkBlack)
          }
        }
        if (compartments.size >= MAX_COMPARTMENTS) {
          Text(stringResource(R.string.designer_max_reached), fontFamily = MonoFont, fontSize = 11.sp, color = InkBlack.copy(alpha = 0.6f))
        }
      }
      Spacer(Modifier.height(4.dp))
    }

    // Bottom CTA
    NeoBrutalistButton(
      onClick = {
        val fridge = initialFridge.copy(template = template, colorKey = colorKey)
        onConfirm(fridge, normalizeSort(compartments))
      },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      containerColor = LimeFresh,
      contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp, horizontal = 16.dp)
    ) {
      Text(
        stringResource(R.string.designer_confirm),
        fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp, color = InkBlack
      )
    }
  }
}

private fun normalizeSort(list: List<CompartmentEntity>): List<CompartmentEntity> =
  list.mapIndexed { i, c -> c.copy(sort = i) }

@Composable
private fun SectionTitle(text: String) {
  Text(
    text,
    modifier = Modifier.padding(horizontal = 16.dp),
    fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold,
    fontSize = 15.sp, color = InkBlack
  )
}

@Composable
private fun CompartmentRow(
  index: Int,
  compartment: CompartmentEntity,
  isSelected: Boolean,
  canRemove: Boolean,
  onFocus: () -> Unit,
  onName: (String) -> Unit,
  onToggleKind: () -> Unit,
  onShelves: (Int) -> Unit,
  onRemove: () -> Unit
) {
  val removeCd = stringResource(R.string.designer_remove_cd)
  val minusCd = stringResource(R.string.designer_shelves_minus_cd)
  val plusCd = stringResource(R.string.designer_shelves_plus_cd)
  NeoBrutalistCard(
    modifier = Modifier.fillMaxWidth(),
    containerColor = if (isSelected) Color(0xFFFEF9C3) else SurfaceWhite,
    cornerRadius = 14.dp,
    shadowOffset = 3.dp,
    borderWidth = if (isSelected) 2.5.dp else 2.dp
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (isSelected) LimeFresh else LemonWarn)
            .border(2.dp, InkBlack, CircleShape)
            .clickable(onClick = onFocus),
          contentAlignment = Alignment.Center
        ) {
          Text("${index + 1}", fontFamily = MonoFont, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = InkBlack)
        }
        BasicTextField(
          value = compartment.name,
          onValueChange = onName,
          singleLine = true,
          textStyle = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkBlack),
          cursorBrush = SolidColor(InkBlack),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, InkBlack, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
        )
        NeoBrutalistButton(
          onClick = onRemove,
          enabled = canRemove,
          containerColor = CoralUrgent,
          shadowOffset = 2.dp,
          borderWidth = 2.dp,
          cornerRadius = 8.dp,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.semantics { contentDescription = removeCd }
        ) {
          Text(stringResource(R.string.designer_remove), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = InkBlack)
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val isDoor = compartment.kind == CompartmentKind.DOOR
        NeoBrutalistButton(
          onClick = onToggleKind,
          containerColor = if (isDoor) LilacAccent else SkyInfo,
          shadowOffset = 2.dp,
          borderWidth = 2.dp,
          cornerRadius = 8.dp,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Text(
            stringResource(if (isDoor) R.string.kind_door else R.string.kind_drawer),
            fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = InkBlack
          )
        }
        Spacer(Modifier.weight(1f))
        NeoBrutalistButton(
          onClick = { onShelves(-1) },
          shadowOffset = 2.dp, borderWidth = 2.dp, cornerRadius = 8.dp,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 3.dp),
          modifier = Modifier.semantics { contentDescription = minusCd }
        ) { Text("−", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = InkBlack) }
        Text(
          stringResource(R.string.designer_shelves_fmt, compartment.shelves),
          fontFamily = MonoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = InkBlack
        )
        NeoBrutalistButton(
          onClick = { onShelves(1) },
          shadowOffset = 2.dp, borderWidth = 2.dp, cornerRadius = 8.dp,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 3.dp),
          modifier = Modifier.semantics { contentDescription = plusCd }
        ) { Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = InkBlack) }
      }
    }
  }
}
