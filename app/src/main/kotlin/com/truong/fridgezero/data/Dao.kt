package com.truong.fridgezero.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FridgeDao {
  @Query("SELECT * FROM fridges LIMIT 1")
  fun observeActiveFridge(): Flow<FridgeEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(fridge: FridgeEntity)
}

@Dao
interface CompartmentDao {
  @Query("SELECT * FROM compartments ORDER BY sort ASC")
  fun observeAll(): Flow<List<CompartmentEntity>>

  @Query("SELECT * FROM compartments WHERE fridgeId = :fridgeId ORDER BY sort ASC")
  fun observeByFridge(fridgeId: String): Flow<List<CompartmentEntity>>

  @Query("SELECT * FROM compartments WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): CompartmentEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<CompartmentEntity>)

  @Query("DELETE FROM compartments WHERE fridgeId = :fridgeId")
  suspend fun deleteByFridge(fridgeId: String)

  @Transaction
  suspend fun replaceForFridge(fridgeId: String, items: List<CompartmentEntity>) {
    deleteByFridge(fridgeId)
    insertAll(items)
  }
}

@Dao
interface FoodItemDao {
  @Query("SELECT * FROM food_items WHERE status = :status ORDER BY expiresAtEpochDay ASC")
  fun observeByStatus(status: ItemStatus = ItemStatus.ACTIVE): Flow<List<FoodItemEntity>>

  @Query("SELECT * FROM food_items WHERE status = :status")
  suspend fun getByStatusSnapshot(status: ItemStatus = ItemStatus.ACTIVE): List<FoodItemEntity>

  @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): FoodItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<FoodItemEntity>)

  @Upsert
  suspend fun upsert(item: FoodItemEntity): Long

  @Query("UPDATE food_items SET status = :status WHERE id = :id")
  suspend fun updateStatus(id: Long, status: ItemStatus)

  @Query("UPDATE food_items SET status = :status WHERE id IN (:ids)")
  suspend fun updateStatusForIds(ids: List<Long>, status: ItemStatus)

  @Query(
    "UPDATE food_items SET isOpened = 1, " +
      "expiresAtEpochDay = CASE WHEN expiresAtEpochDay > :maxExpiryEpochDay THEN :maxExpiryEpochDay ELSE expiresAtEpochDay END " +
      "WHERE id IN (:ids)"
  )
  suspend fun markLeftoverForIds(ids: List<Long>, maxExpiryEpochDay: Long)

  @Query("UPDATE food_items SET compartmentId = :compartmentId WHERE id = :id")
  suspend fun moveToCompartment(id: Long, compartmentId: String)

  @Query("UPDATE food_items SET compartmentId = :compartmentId WHERE id IN (:ids)")
  suspend fun moveToCompartmentForIds(ids: List<Long>, compartmentId: String)

  @Query("UPDATE food_items SET compartmentId = :fallbackId WHERE status = :status AND compartmentId NOT IN (:validIds)")
  suspend fun reassignOrphanedItems(
    validIds: List<String>,
    fallbackId: String,
    status: ItemStatus = ItemStatus.ACTIVE
  )

  @Query("DELETE FROM food_items WHERE id = :id")
  suspend fun deleteById(id: Long)
}
