package com.truong.fridgezero.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DbConverters {
  @TypeConverter
  fun fromKind(kind: CompartmentKind): String = kind.name

  @TypeConverter
  fun toKind(value: String): CompartmentKind =
    runCatching { CompartmentKind.valueOf(value) }.getOrDefault(CompartmentKind.DOOR)

  @TypeConverter
  fun fromStatus(status: ItemStatus): String = status.name

  @TypeConverter
  fun toStatus(value: String): ItemStatus =
    runCatching { ItemStatus.valueOf(value) }.getOrDefault(ItemStatus.ACTIVE)
}

@Database(
  entities = [FridgeEntity::class, CompartmentEntity::class, FoodItemEntity::class],
  version = 3,
  exportSchema = false
)
@TypeConverters(DbConverters::class)
abstract class FridgeDb : RoomDatabase() {

  abstract fun fridgeDao(): FridgeDao
  abstract fun compartmentDao(): CompartmentDao
  abstract fun foodItemDao(): FoodItemDao

  companion object {
    private const val DB_NAME = "fridgezero_mvp.db"

    @Volatile
    private var INSTANCE: FridgeDb? = null

    fun get(context: Context): FridgeDb {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
      }
    }

    private fun build(appContext: Context): FridgeDb {
      return Room.databaseBuilder(appContext, FridgeDb::class.java, DB_NAME)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .addCallback(SeedCallback(appContext))
        .build()
    }
  }

  private class SeedCallback(private val appContext: Context) : Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
      super.onCreate(db)
      CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        val instance = get(appContext)
        instance.fridgeDao().upsert(Seed.defaultFridge)
        instance.compartmentDao().insertAll(Seed.compartments)
      }
    }
  }
}
