package com.aistudio.axewatch.trader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aistudio.axewatch.trader.data.remote.IpoAllotmentService

@Entity(tableName = "pan_vault")
data class PanVaultEntity(
    @PrimaryKey
    val panNumber: String,
    val holderName: String,
    val relation: String = "Self",
    val addedAt: Long = System.currentTimeMillis()
) {
    /** Single source of truth is IpoAllotmentService.maskPan ("AB*****F").
     *  The previous local implementation ("ABCDE****F") leaked 5 of 10 PAN
     *  characters and could never match registrar records. */
    val maskedPan: String
        get() = IpoAllotmentService.maskPan(panNumber)
}

/**
 * Legacy-compatible masked-PAN matching. Records stored before the mask was
 * unified carry the old over-revealing "ABCDE****F" format (5 visible chars +
 * 4 stars + last), while the vault now produces the compliant "AB*****F"
 * (2 + 5 stars + 1). Both formats keep the leading characters and the final
 * character, so the shorter visible prefix must be a prefix of the longer one
 * and both must end with the same character. The full PAN is never involved —
 * this operates on masked strings only, so a record can never re-identify it.
 */
fun maskMatches(vaultMasked: String, recordMasked: String): Boolean {
    if (vaultMasked == recordMasked) return true
    val v = vaultMasked.replace("*", "")
    val r = recordMasked.replace("*", "")
    if (v.isEmpty() || r.isEmpty()) return false
    val (short, long) = if (v.length <= r.length) v to r else r to v
    return long.startsWith(short.dropLast(1)) && v.last() == r.last()
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
    val status: String, // "ALLOTTED", "NOT_ALLOTTED", "NOT_APPLIED", "RESULTS_NOT_OUT", "LOOKUP_FAILED"
    val registrar: String,
    val checkedAt: Long = System.currentTimeMillis(),
    val applicationNo: String = ""
) {
    val isAllotted: Boolean get() = status == "ALLOTTED"
    val isNotAllotted: Boolean get() = status == "NOT_ALLOTTED"
    val isNotApplied: Boolean get() = status == "NOT_APPLIED"
    val isResultsNotOut: Boolean get() = status == "RESULTS_NOT_OUT" || status == "AWAITING"
    // Transport failure is its own state: the registrar could not be
    // reached, which must never render as "Not Allotted".
    val isLookupFailed: Boolean get() = status == "LOOKUP_FAILED"

    val statusLabel: String
        get() = when (status) {
            "ALLOTTED" -> "Allotted"
            "NOT_ALLOTTED" -> "Not Allotted"
            "NOT_APPLIED" -> "Not Applied"
            "RESULTS_NOT_OUT", "AWAITING" -> "Results Not Out Yet"
            "LOOKUP_FAILED" -> "Lookup Failed — Retry"
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
