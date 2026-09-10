package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AllotmentRecordEntity
import com.example.data.local.entity.PanVaultEntity
import com.example.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PanVaultDao {
    @Query("SELECT * FROM pan_vault ORDER BY addedAt DESC")
    fun getAllPans(): Flow<List<PanVaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPan(pan: PanVaultEntity)

    @Delete
    suspend fun deletePan(pan: PanVaultEntity)

    @Query("SELECT * FROM allotment_records ORDER BY checkedAt DESC")
    fun getAllRecords(): Flow<List<AllotmentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AllotmentRecordEntity)

    @Delete
    suspend fun deleteRecord(record: AllotmentRecordEntity)

    @Query("DELETE FROM allotment_records")
    suspend fun clearAllRecords()
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY symbol ASC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Delete
    suspend fun delete(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    fun isInWatchlist(symbol: String): Flow<Boolean>
}
