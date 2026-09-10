package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val name: String,
    val assetType: String = "STOCK", // STOCK or MUTUAL_FUND
    val quantity: Double,
    val buyPrice: Double,
    val buyDate: Long = System.currentTimeMillis(),
    val sector: String = "Diversified",
    val notes: String = ""
)
