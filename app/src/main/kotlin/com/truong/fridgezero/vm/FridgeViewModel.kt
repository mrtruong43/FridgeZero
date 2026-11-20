package com.truong.fridgezero.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.FridgeDb
import com.truong.fridgezero.data.FridgeEntity
import com.truong.fridgezero.data.FridgeRepository
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.ConfigurableShelfLifeRepository
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.CulinaryGuideEngine
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.FridgeUiState
import com.truong.fridgezero.domain.MatchedIngredient
import com.truong.fridgezero.domain.NRect
import com.truong.fridgezero.domain.toDisplay
import com.truong.fridgezero.domain.zoneForCompartment
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class FridgeViewModel(
  private val repository: FridgeRepository
) : ViewModel() {

  private val pendingWelcomeSpin = MutableStateFlow(false)

  val uiState: StateFlow<FridgeUiState> = combine(
    repository.observeActiveFridge(),
    repository.observeCompartments(),
    repository.observeFoodItems(),
    pendingWelcomeSpin
  ) { activeFridge, compartments, foodItems, welcomeSpin ->
    val today = LocalDate.now().toEpochDay()
    val itemsByComp = foodItems.groupBy { it.compartmentId }

    val uiCompartments = compartments.map { entity ->
      val rawItems = itemsByComp[entity.id].orEmpty()
      val displayItems = rawItems.toDisplay(today)
      val worst = displayItems.minByOrNull { it.minDaysLeft }?.freshness ?: Freshness.OK
      val urgentCount = displayItems.count { it.freshness != Freshness.OK }

      CompartmentUi(
        id = entity.id,
        name = entity.name,
        rect = NRect(entity.x, entity.y, entity.w, entity.h),
        kind = entity.kind,
        shelves = entity.shelves,
        items = displayItems,
        worstFreshness = worst,
        urgentCount = urgentCount
      )
    }

    val recentKeys = foodItems
      .sortedByDescending { it.addedAtEpochDay }
      .map { it.iconKey }
      .distinct()
      .take(8)

    FridgeUiState(
      activeFridge = activeFridge,
      compartments = uiCompartments,
      recentItemKeys = recentKeys,
      playWelcomeSpin = welcomeSpin
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = FridgeUiState()
  )

  fun consumeWelcomeSpin() {
    pendingWelcomeSpin.value = false
  }

  fun addItem(
    compartmentId: String,
    name: String,
    iconKey: String,
    quantity: Int,
    daysLeft: Int,
    unit: String? = null,
    isOpened: Boolean = false
  ) {
    val cleanName = name.trim()
    if (cleanName.isEmpty()) return
    val today = LocalDate.now().toEpochDay()
    viewModelScope.launch {
      repository.upsertFoodItem(
        FoodItemEntity(
          compartmentId = compartmentId,
          name = cleanName,
          iconKey = iconKey,
          group = EmojiIconRepository.groupFor(iconKey).name,
          quantity = quantity.coerceAtLeast(1),
          unit = unit?.takeIf { it.isNotBlank() },
          addedAtEpochDay = today,
          expiresAtEpochDay = today + daysLeft.toLong(),
          isOpened = isOpened,
          status = ItemStatus.ACTIVE,
          source = "manual"
        )
      )
    }
  }

  fun updateItem(
    id: Long,
    name: String,
    iconKey: String,
    quantity: Int,
    daysLeft: Int,
    unit: String? = null
  ) {
    val cleanName = name.trim()
    if (cleanName.isEmpty()) return
    val today = LocalDate.now().toEpochDay()
    viewModelScope.launch {
      val existing = repository.getFoodItem(id) ?: return@launch
      repository.upsertFoodItem(
        existing.copy(
          name = cleanName,
          iconKey = iconKey,
          group = EmojiIconRepository.groupFor(iconKey).name,
          quantity = quantity.coerceAtLeast(1),
          unit = unit ?: existing.unit,
          expiresAtEpochDay = today + daysLeft.toLong()
        )
      )
    }
  }

  fun adjustQuantity(id: Long, delta: Int) {
    viewModelScope.launch {
      val existing = repository.getFoodItem(id) ?: return@launch
      val next = existing.quantity + delta
      if (next <= 0) {
        repository.updateItemStatus(id, ItemStatus.USED_UP)
      } else {
        repository.upsertFoodItem(existing.copy(quantity = next))
      }
    }
  }

  fun toggleItemOpened(id: Long) {
    val today = LocalDate.now().toEpochDay()
    viewModelScope.launch {
      val existing = repository.getFoodItem(id) ?: return@launch
      val nextOpened = !existing.isOpened
      var nextExpiry = existing.expiresAtEpochDay
      if (nextOpened) {
        val currentRemaining = (existing.expiresAtEpochDay - today).toInt()
        val group = EmojiIconRepository.groupFor(existing.iconKey)
        val comp = repository.getCompartment(existing.compartmentId)
        val zone = comp?.let(::zoneForCompartment) ?: zoneForCompartment(existing.compartmentId)
        val suggestedOpened = ConfigurableShelfLifeRepository.suggestDays(
          iconKey = existing.iconKey,
          group = group,
          zone = zone,
          isOpened = true
        )
        if (suggestedOpened != null && currentRemaining > suggestedOpened) {
          nextExpiry = today + suggestedOpened.toLong()
        } else if (currentRemaining > 1) {
          val halved = (currentRemaining * 0.5f).toInt().coerceAtLeast(1)
          nextExpiry = today + halved.toLong()
        }
      }
      repository.upsertFoodItem(
        existing.copy(
          isOpened = nextOpened,
          expiresAtEpochDay = nextExpiry
        )
      )
    }
  }

  fun markItemStatus(ids: List<Long>, status: ItemStatus) {
    if (ids.isEmpty()) return
    viewModelScope.launch {
      repository.updateItemsStatus(ids, status)
    }
  }

  fun markItemStatus(id: Long, status: ItemStatus) = markItemStatus(listOf(id), status)

  fun consumeRecipeIngredients(matched: List<MatchedIngredient>) =
    completeRecipeCook(usedUp = matched, leftovers = emptyList())

  fun completeRecipeCook(
    usedUp: List<MatchedIngredient>,
    leftovers: List<MatchedIngredient>
  ) {
    if (usedUp.isEmpty() && leftovers.isEmpty()) return
    val today = LocalDate.now().toEpochDay()
    val maxLeftoverExpiry = today + CulinaryGuideEngine.LEFTOVER_MAX_DAYS_LEFT.toLong()
    viewModelScope.launch {
      val usedUpIds = if (usedUp.isNotEmpty()) {
        val uniqueIds = usedUp.flatMap { it.item.itemIds }.distinct()
        val entitiesById = uniqueIds.mapNotNull { id ->
          repository.getFoodItem(id)?.let { id to it }
        }.toMap()
        val deductions = CulinaryGuideEngine.planDeductions(usedUp, entitiesById)
        deductions.forEach { deduction ->
          val entity = entitiesById[deduction.itemId] ?: return@forEach
          if (deduction.remainingQuantity <= 0) {
            repository.updateItemStatus(deduction.itemId, ItemStatus.USED_UP)
          } else {
            repository.upsertFoodItem(entity.copy(quantity = deduction.remainingQuantity))
          }
        }
        uniqueIds.toSet()
      } else {
        emptySet()
      }

      val leftoverIds = leftovers
        .flatMap { it.item.itemIds }
        .distinct()
        .filter { it !in usedUpIds }
      if (leftoverIds.isNotEmpty()) {
        repository.markItemsLeftover(leftoverIds, maxLeftoverExpiry)
      }
    }
  }

  fun moveItemToCompartment(
    ids: List<Long>,
    compartmentId: String,
    newDaysLeft: Int? = null
  ) {
    if (ids.isEmpty()) return
    val today = LocalDate.now().toEpochDay()
    viewModelScope.launch {
      if (newDaysLeft != null) {
        ids.forEach { itemId ->
          val existing = repository.getFoodItem(itemId) ?: return@forEach
          repository.upsertFoodItem(
            existing.copy(
              compartmentId = compartmentId,
              expiresAtEpochDay = today + newDaysLeft.toLong()
            )
          )
        }
      } else {
        repository.moveItemsToCompartment(ids, compartmentId)
      }
    }
  }

  fun moveItemToCompartment(
    id: Long,
    compartmentId: String,
    newDaysLeft: Int? = null
  ) = moveItemToCompartment(listOf(id), compartmentId, newDaysLeft)

  fun deleteItem(id: Long) {
    viewModelScope.launch {
      repository.updateItemStatus(id, ItemStatus.DISCARDED)
    }
  }

  fun applyFridgeTemplate(template: String, colorKey: String, handleStyle: String) {
    viewModelScope.launch {
      repository.applyTemplate(template, colorKey, handleStyle)
    }
  }

  fun saveCustomFridge(
    fridge: FridgeEntity,
    compartments: List<CompartmentEntity>,
    includeSampleItems: Boolean = false,
    triggerWelcomeSpin: Boolean = false
  ) {
    if (triggerWelcomeSpin) {
      pendingWelcomeSpin.value = true
    }
    viewModelScope.launch {
      repository.saveCustomFridge(fridge, compartments, includeSampleItems)
    }
  }

  fun loadSampleData() {
    viewModelScope.launch {
      repository.populateSampleData()
    }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory {
      val appContext = context.applicationContext
      return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          val db = FridgeDb.get(appContext)
          val repo = FridgeRepository(db.fridgeDao(), db.compartmentDao(), db.foodItemDao())
          return FridgeViewModel(repo) as T
        }
      }
    }
  }
}
