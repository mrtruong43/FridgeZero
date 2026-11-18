package com.truong.fridgezero.data

import com.truong.fridgezero.domain.StorageZone
import com.truong.fridgezero.domain.zoneForCompartment
import kotlinx.coroutines.flow.Flow

class FridgeRepository(
  private val fridgeDao: FridgeDao,
  private val compartmentDao: CompartmentDao,
  private val foodItemDao: FoodItemDao
) {
  fun observeActiveFridge(): Flow<FridgeEntity?> = fridgeDao.observeActiveFridge()

  fun observeCompartments(fridgeId: String = Seed.DEFAULT_FRIDGE_ID): Flow<List<CompartmentEntity>> =
    compartmentDao.observeByFridge(fridgeId)

  fun observeFoodItems(): Flow<List<FoodItemEntity>> =
    foodItemDao.observeByStatus(ItemStatus.ACTIVE)

  suspend fun getCompartment(id: String): CompartmentEntity? = compartmentDao.getById(id)

  suspend fun getFoodItem(id: Long): FoodItemEntity? = foodItemDao.getById(id)

  suspend fun upsertFoodItem(item: FoodItemEntity): Long = foodItemDao.upsert(item)

  suspend fun updateItemStatus(id: Long, status: ItemStatus) =
    foodItemDao.updateStatus(id, status)

  suspend fun updateItemsStatus(ids: List<Long>, status: ItemStatus) {
    if (ids.isNotEmpty()) {
      foodItemDao.updateStatusForIds(ids, status)
    }
  }

  suspend fun moveItemToCompartment(id: Long, compartmentId: String) =
    foodItemDao.moveToCompartment(id, compartmentId)

  suspend fun moveItemsToCompartment(ids: List<Long>, compartmentId: String) {
    if (ids.isNotEmpty()) {
      foodItemDao.moveToCompartmentForIds(ids, compartmentId)
    }
  }

  suspend fun deleteFoodItem(id: Long) = foodItemDao.deleteById(id)

  suspend fun applyTemplate(
    template: String,
    colorKey: String,
    handleStyle: String,
    fridgeId: String = Seed.DEFAULT_FRIDGE_ID
  ) {
    fridgeDao.upsert(
      Seed.defaultFridge.copy(
        id = fridgeId,
        template = template,
        colorKey = colorKey,
        handleStyle = handleStyle
      )
    )
    val nextCompartments = Seed.compartmentsForTemplate(template, fridgeId)
    compartmentDao.replaceForFridge(fridgeId, nextCompartments)
    reassignOrphanedActiveItems(nextCompartments)
  }

  suspend fun saveCustomFridge(
    fridge: FridgeEntity,
    compartments: List<CompartmentEntity>,
    includeSampleItems: Boolean = false
  ) {
    fridgeDao.upsert(fridge)
    compartmentDao.replaceForFridge(fridge.id, compartments)
    reassignOrphanedActiveItems(compartments)

    if (includeSampleItems && compartments.isNotEmpty() &&
      foodItemDao.getByStatusSnapshot(ItemStatus.ACTIVE).isEmpty()
    ) {
      val validIds = compartments.map { it.id }.toSet()
      val chilledId = preferredChilledCompartment(compartments).id
      val drawerId = compartments.firstOrNull { zoneForCompartment(it) == StorageZone.DRAWER }?.id ?: chilledId
      val freezerId = compartments.firstOrNull { zoneForCompartment(it) == StorageZone.FREEZER }?.id ?: chilledId
      val mappedSeed = Seed.foodItems().map { item ->
        if (item.compartmentId in validIds) {
          item
        } else {
          val mappedCompId = when (item.compartmentId) {
            "freezer" -> freezerId
            "crisper" -> drawerId
            else -> chilledId
          }
          item.copy(compartmentId = mappedCompId)
        }
      }
      foodItemDao.insertAll(mappedSeed)
    }
  }

  private suspend fun reassignOrphanedActiveItems(compartments: List<CompartmentEntity>) {
    if (compartments.isEmpty()) return
    val validIds = compartments.map { it.id }
    val fallback = preferredChilledCompartment(compartments)
    foodItemDao.reassignOrphanedItems(validIds = validIds, fallbackId = fallback.id)
  }

  private fun preferredChilledCompartment(compartments: List<CompartmentEntity>): CompartmentEntity =
    compartments.firstOrNull { zoneForCompartment(it) == StorageZone.CHILLED }
      ?: compartments.first()

  suspend fun populateSampleData() {
    fridgeDao.upsert(Seed.defaultFridge)
    compartmentDao.insertAll(Seed.compartments)
    foodItemDao.insertAll(Seed.foodItems())
  }
}
