package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PaperAccountEntity
import com.example.data.local.entity.PaperOrderEntity
import com.example.data.local.entity.PaperPositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    // Account
    @Query("SELECT * FROM paper_account WHERE id = 1")
    fun getAccount(): Flow<PaperAccountEntity?>

    @Query("SELECT * FROM paper_account WHERE id = 1")
    suspend fun getAccountOnce(): PaperAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAccount(account: PaperAccountEntity)

    // Positions
    @Query("SELECT * FROM paper_positions ORDER BY symbol ASC")
    fun getAllPositions(): Flow<List<PaperPositionEntity>>

    @Query("SELECT * FROM paper_positions WHERE symbol = :symbol")
    suspend fun getPositionBySymbol(symbol: String): PaperPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePosition(position: PaperPositionEntity)

    @Delete
    suspend fun deletePosition(position: PaperPositionEntity)

    @Query("DELETE FROM paper_positions WHERE symbol = :symbol")
    suspend fun deletePositionBySymbol(symbol: String)

    // Orders
    @Query("SELECT * FROM paper_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<PaperOrderEntity>>

    @Insert
    suspend fun insertOrder(order: PaperOrderEntity): Long

    // Reset paper account
    @Query("DELETE FROM paper_positions")
    suspend fun clearAllPositions()

    @Query("DELETE FROM paper_orders")
    suspend fun clearAllOrders()
}
