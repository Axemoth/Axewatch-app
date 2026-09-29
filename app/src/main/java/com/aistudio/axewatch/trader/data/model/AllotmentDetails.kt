package com.aistudio.axewatch.trader.data.model

import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import com.aistudio.axewatch.trader.data.local.entity.PanVaultEntity
import com.aistudio.axewatch.trader.data.local.entity.maskMatches

fun uniqueSavedPan(record: AllotmentRecordEntity, vault: List<PanVaultEntity>): PanVaultEntity? =
    vault.filter { maskMatches(it.maskedPan, record.maskedPan) }.singleOrNull()

fun formatShareQuantity(value: Int): String {
    val digits = value.toString()
    if (digits.length <= 3) return digits
    return digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed() + "," + digits.takeLast(3)
}

val AllotmentRecordEntity.appliedSharesLabel: String
    get() = when {
        appliedSharesSource in setOf("REGISTRAR", "USER") -> formatShareQuantity(sharesApplied)
        appliedSharesSource == "LEGACY" && sharesApplied > 0 -> formatShareQuantity(sharesApplied)
        else -> "Not reported"
    }

val AllotmentRecordEntity.appliedSourceLabel: String
    get() = when (appliedSharesSource) {
        "REGISTRAR" -> "Registrar reported"
        "USER" -> "User entered"
        "LEGACY" -> "Saved count unverified"
        else -> "Quantity unavailable"
    }

/** Refreshing details must never erase a saved verdict or replace it with an outage. */
fun mergeAllotmentDetails(old: AllotmentRecordEntity, fresh: AllotmentRecordEntity): AllotmentRecordEntity {
    if (old.ipoSymbol != fresh.ipoSymbol || !maskMatches(old.maskedPan, fresh.maskedPan)) return old
    if (fresh.status !in setOf("ALLOTTED", "NOT_ALLOTTED", "NOT_APPLIED")) return old
    if (old.status in setOf("ALLOTTED", "NOT_ALLOTTED", "NOT_APPLIED") &&
        (old.status != fresh.status || old.sharesAllotted != fresh.sharesAllotted)) return old
    return fresh.copy(
        id = old.id,
        sharesApplied = if (fresh.appliedSharesSource == "REGISTRAR") fresh.sharesApplied else old.sharesApplied,
        appliedSharesSource = if (fresh.appliedSharesSource == "REGISTRAR") "REGISTRAR" else old.appliedSharesSource,
        applicationNo = fresh.applicationNo.ifBlank { old.applicationNo }
    )
}

fun validManualQuantities(status: String, allotted: Int, applied: Int?): Boolean =
    allotted >= 0 && (status != "ALLOTTED" || allotted > 0) &&
        (status == "ALLOTTED" || allotted == 0) &&
        (applied == null || (applied >= allotted && (status != "NOT_APPLIED" || applied == 0)))
