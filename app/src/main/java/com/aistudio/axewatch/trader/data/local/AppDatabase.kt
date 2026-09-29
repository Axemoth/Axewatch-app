package com.aistudio.axewatch.trader.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aistudio.axewatch.trader.data.local.dao.HoldingDao
import com.aistudio.axewatch.trader.data.local.dao.PanVaultDao
import com.aistudio.axewatch.trader.data.local.dao.PaperDao
import com.aistudio.axewatch.trader.data.local.dao.WatchlistDao
import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import com.aistudio.axewatch.trader.data.local.entity.HoldingEntity
import com.aistudio.axewatch.trader.data.local.entity.PanVaultEntity
import com.aistudio.axewatch.trader.data.local.entity.PaperAccountEntity
import com.aistudio.axewatch.trader.data.local.entity.PaperOrderEntity
import com.aistudio.axewatch.trader.data.local.entity.PaperPositionEntity
import com.aistudio.axewatch.trader.data.local.entity.WatchlistEntity

@Database(
    entities = [
        HoldingEntity::class,
        PaperAccountEntity::class,
        PaperPositionEntity::class,
        PaperOrderEntity::class,
        PanVaultEntity::class,
        AllotmentRecordEntity::class,
        WatchlistEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun holdingDao(): HoldingDao
    abstract fun paperDao(): PaperDao
    abstract fun panVaultDao(): PanVaultDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE allotment_records ADD COLUMN appliedSharesSource TEXT NOT NULL DEFAULT 'LEGACY'")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "axewatch.db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
