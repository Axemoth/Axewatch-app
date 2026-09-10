package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.HoldingDao
import com.example.data.local.dao.PanVaultDao
import com.example.data.local.dao.PaperDao
import com.example.data.local.dao.WatchlistDao
import com.example.data.local.entity.AllotmentRecordEntity
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.PanVaultEntity
import com.example.data.local.entity.PaperAccountEntity
import com.example.data.local.entity.PaperOrderEntity
import com.example.data.local.entity.PaperPositionEntity
import com.example.data.local.entity.WatchlistEntity

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
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun holdingDao(): HoldingDao
    abstract fun paperDao(): PaperDao
    abstract fun panVaultDao(): PanVaultDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "axewatch.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
