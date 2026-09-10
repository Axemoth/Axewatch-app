package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paper_account")
data class PaperAccountEntity(
    @PrimaryKey
    val id: Int = 1,
    val cashBalance: Double = 1000000.0, // ₹10,00,000 starting cash
    val initialBalance: Double = 1000000.0,
    val realizedPnl: Double = 0.0,
    val totalTrades: Int = 0,
    val winningTrades: Int = 0
)

@Entity(tableName = "paper_positions")
data class PaperPositionEntity(
    @PrimaryKey
    val symbol: String,
    val name: String,
    val quantity: Int,
    val averageBuyPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double? = null,
    val targetPrice: Double? = null,
    val openedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "paper_orders")
data class PaperOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val side: String, // "BUY" or "SELL"
    val quantity: Int,
    val price: Double,
    val orderType: String = "MARKET", // "MARKET" or "LIMIT"
    val status: String = "FILLED",
    val realizedPnl: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
