package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pan_vault")
data class PanVaultEntity(
    @PrimaryKey
    val panNumber: String,
    val holderName: String,
    val relation: String = "Self",
    val addedAt: Long = System.currentTimeMillis()
) {
    val maskedPan: String
        get() {
            val upper = panNumber.trim().uppercase()
            return if (upper.length >= 10) {
                "${upper.take(5)}****${upper.takeLast(1)}"
            } else {
                "*****"
            }
        }
}

@Entity(tableName = "allotment_records")
data class AllotmentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val maskedPan: String,
    val ipoSymbol: String,
    val ipoName: String,
    val sharesApplied: Int,
    val sharesAllotted: Int,
    val status: String, // "ALLOTTED", "NOT_ALLOTTED", "NOT_APPLIED", "RESULTS_NOT_OUT"
    val registrar: String,
    val checkedAt: Long = System.currentTimeMillis(),
    val applicationNo: String = ""
) {
    val isAllotted: Boolean get() = status == "ALLOTTED"
    val isNotAllotted: Boolean get() = status == "NOT_ALLOTTED"
    val isNotApplied: Boolean get() = status == "NOT_APPLIED"
    val isResultsNotOut: Boolean get() = status == "RESULTS_NOT_OUT" || status == "AWAITING"

    val statusLabel: String
        get() = when (status) {
            "ALLOTTED" -> "Allotted"
            "NOT_ALLOTTED" -> "Not Allotted"
            "NOT_APPLIED" -> "Not Applied"
            "RESULTS_NOT_OUT", "AWAITING" -> "Results Not Out Yet"
            else -> status
        }
}

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey
    val symbol: String,
    val name: String,
    val assetType: String = "EQUITY", // EQUITY, INDEX, IPO
    val addedPrice: Double,
    val targetAlert: Double? = null,
    val addedAt: Long = System.currentTimeMillis()
)
