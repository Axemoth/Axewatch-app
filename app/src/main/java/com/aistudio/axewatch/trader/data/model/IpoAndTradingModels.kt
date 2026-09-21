package com.aistudio.axewatch.trader.data.model

data class IpoIssue(
    val symbol: String,
    val companyName: String,
    val category: String, // "Mainboard" or "SME"
    val status: String, // "Active", "Forthcoming", "Closed", "Listed"
    val issueOpenDate: String,
    val issueCloseDate: String,
    val priceBand: String,
    val issuePrice: Double,
    val lotSize: Int,
    val issueSizeCr: Double,
    val registrar: String,
    val qibSub: Double = 0.0,
    val niiSub: Double = 0.0,
    val shniSub: Double = 0.0, // Small HNI (₹2L - ₹10L)
    val bhniSub: Double = 0.0, // Big HNI (> ₹10L)
    val riiSub: Double = 0.0,  // Retail
    val totalSub: Double = 0.0,
    val gmpAmount: Double = 0.0,
    val gmpPercent: Double = 0.0,
    val estListingPrice: Double = 0.0,
    // Basis-of-allotment declaration date from the registrar directory
    // (ipomarket/IPOWatch tables), e.g. "18 Sep 2026". Blank = unknown.
    // Drives the "Results declared" section and recent-first ordering.
    val allotmentDate: String = ""
)

/** Section rank for the allotment picker: declared results first. */
fun ipoSection(issue: IpoIssue): Int {
    if (issue.allotmentDate.isNotBlank()) return 0
    return when (issue.status.lowercase()) {
        "closed", "listed", "allotted" -> 0
        "active", "open" -> 1
        "forthcoming", "upcoming" -> 2
        else -> 3
    }
}

private val LOOSE_MONTHS = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
    "jul" to 7, "aug" to 8, "sep" to 9, "sept" to 9, "oct" to 10, "nov" to 11, "dec" to 12
)

/**
 * Best-effort date key (yyyyMMdd) from loose tracker strings:
 * "18 Sep 2026", "18 Sept", "2026-09-18", "18-09-2026". Missing year =
 * current year. Unparseable = 0 (sorts last). Pure function, unit-tested.
 */
fun parseLooseDate(text: String, nowYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)): Long {
    val t = text.trim()
    if (t.isEmpty() || t == "—" || t.equals("-", ignoreCase = true)) return 0
    var m = Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(t)
    if (m != null) {
        val (y, mo, d) = m.destructured
        return y.toLong() * 10000 + mo.toLong() * 100 + d.toLong()
    }
    m = Regex("(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2,4})").find(t)
    if (m != null) {
        var (d, mo, y) = m.destructured
        val year = if (y.length == 2) 2000 + y.toLong() else y.toLong()
        return year * 10000 + mo.toLong() * 100 + d.toLong()
    }
    val monthHit = LOOSE_MONTHS.entries.firstOrNull { (k, _) -> k in t.lowercase() }
    // First plausible day-of-month (a bare year like "Sep 2026" has none).
    val day = Regex("\\d{1,4}").findAll(t)
        .mapNotNull { it.value.toLongOrNull() }
        .firstOrNull { it in 1..31 }
    if (monthHit != null && day != null) {
        val year = Regex("(19|20)\\d{2}").find(t)?.value?.toLongOrNull() ?: nowYear.toLong()
        return year * 10000 + monthHit.value * 100 + day
    }
    return 0
}

/** Recency key for recent-first ordering: allotment date, else close, else open. */
fun ipoRecencyKey(issue: IpoIssue): Long {
    return parseLooseDate(issue.allotmentDate)
        .takeIf { it > 0 }
        ?: parseLooseDate(issue.issueCloseDate)
            .takeIf { it > 0 }
        ?: parseLooseDate(issue.issueOpenDate)
}

/**
 * Returns true if the IPO issue is in pre-apply stage and bidding has not opened yet.
 * IPOs in this stage will naturally have no subscription figures on stock exchanges.
 */
fun IpoIssue.isBiddingNotStarted(): Boolean {
    val openKey = parseLooseDate(issueOpenDate)
    val cal = java.util.Calendar.getInstance()
    val todayKey = cal.get(java.util.Calendar.YEAR) * 10000L +
                   (cal.get(java.util.Calendar.MONTH) + 1) * 100L +
                   cal.get(java.util.Calendar.DAY_OF_MONTH)
    if (openKey > 0 && openKey > todayKey) return true
    val s = status.lowercase()
    return s == "forthcoming" || s == "upcoming" || s.contains("pre")
}

/**
 * Finds the matching registrar direct portal link for an IPO issue.
 */
fun findMatchingRegistrarLink(registrarName: String, links: List<RegistrarLink>): RegistrarLink? {
    if (registrarName.isBlank() || registrarName.equals("Unknown", ignoreCase = true)) return null
    val regLower = registrarName.lowercase()
    return links.find { link ->
        val titleLower = link.title.lowercase()
        val badgeLower = link.badge.lowercase()
        titleLower.contains(regLower) ||
        regLower.contains(badgeLower) ||
        ((regLower.contains("mufg") || regLower.contains("intime")) && (titleLower.contains("mufg") || titleLower.contains("intime"))) ||
        (regLower.contains("kfin") && titleLower.contains("kfin")) ||
        (regLower.contains("bigshare") && titleLower.contains("bigshare")) ||
        (regLower.contains("skyline") && titleLower.contains("skyline")) ||
        (regLower.contains("cameo") && titleLower.contains("cameo")) ||
        (regLower.contains("maashitla") && titleLower.contains("maashitla")) ||
        (regLower.contains("purva") && titleLower.contains("purva")) ||
        (regLower.contains("beetal") && titleLower.contains("beetal"))
    }
}

data class GmpItem(
    val companyName: String,
    val symbol: String,
    val issuePrice: Double,
    val gmpAmount: Double,
    val gmpPercent: Double,
    val estListingPrice: Double,
    val status: String, // "Open", "Upcoming", "Closed", "Listed"
    val fireRating: Int, // 1 to 5
    val lastUpdated: String
)

data class PastIpoItem(
    val symbol: String,
    val companyName: String,
    val issuePrice: Double,
    val listingPrice: Double,
    // Unknown unless the tracker published them: 0.0 renders as "—", and
    // listingDate "" renders as "—". Never derive these from other columns.
    val currentPrice: Double = 0.0,
    val listingGainPercent: Double,
    val currentGainPercent: Double = 0.0,
    val totalSub: Double = 0.0,
    val listingDate: String = ""
)

data class TradeIdea(
    val symbol: String,
    val name: String,
    val signal: String, // "BUY" or "SELL"
    val currentPrice: Double,
    val entryPrice: Double = currentPrice,
    val stopLoss: Double,
    val targetPrice: Double,
    val targetPrice2: Double = targetPrice * 1.025,
    val riskReward: String,
    val horizonDays: Int,
    // 0.0 = unvalidated on-device. Displayed accuracy must come from a real
    // measurement, never from a placeholder constant.
    val accuracy: Double = 0.0,
    val score: Int = 0,
    val driftPercent: Double = 0.0,
    val ageDays: Double = 0.0,
    val suggestedQty05Pct: Int,
    val suggestedQty1Pct: Int,
    val suggestedQty2Pct: Int,
    val reason: String = ""
)

data class ModelCalibrationBucket(
    val rangeLabel: String,
    val lowProb: Double = 0.4,
    val highProb: Double = 0.5,
    val sampleCount: Int,
    val actualWinRate: Double, // in percent, e.g. 64.0
    val confidenceRange: String = rangeLabel
)

data class QuantModelReportCard(
    // No on-device training exists: every metric defaults to "no measurement"
    // and the UI must gate the whole card on `validated`. Do NOT fill these
    // with plausible-looking constants — that shipped once and misled users.
    val modelName: String = "NIFTY-50 Multi-Factor Alpha Engine",
    val modelVersion: String = "2.4.1",
    val embargoStatus: String = "Not validated on-device",
    val status: String = "Unvalidated",
    val validated: Boolean = false,
    val walkForwardAccuracy: Double = 0.0,
    val accuracyOutSample: Double = walkForwardAccuracy,
    val walkForwardAuc: Double = 0.0,
    val rocAuc: Double = walkForwardAuc,
    val strongBuyPrecision: Double = 0.0,
    val precisionStrongBuy: Double = strongBuyPrecision,
    val pickSpreadBps: Double = 0.0,
    val stocksCovered: Int = 0,
    val samplesCount: Int = 0,
    val sampleCount: Int = samplesCount,
    val featuresCount: Int = 49,
    val featureVersion: Int = 2,
    val baseRate: Double = 0.0,
    val baseRateAccuracy: Double = baseRate,
    val hasEdge: Boolean = false,
    val horizonDays: Int = 10,
    val forecastHorizonDays: Int = horizonDays,
    val trainedDate: String = "—",
    val calibrationBuckets: List<ModelCalibrationBucket> = emptyList()
)

data class PortfolioSummary(
    val totalInvested: Double,
    val currentValue: Double,
    val totalProfitLoss: Double,
    val profitLossPercent: Double,
    // Null until measurable: day return needs per-holding prev-close data
    // (not yet collected); XIRR needs dated cashflows. Render as "—".
    val dayReturn: Double? = null,
    val dayReturnPercent: Double? = null,
    // Null until computed from dated cashflows (holdings carry no purchase
    // dates yet) — renders as "—", never as a plausible constant.
    val xirrPercent: Double? = null,
    val holdingsCount: Int
)

data class MutualFundScheme(
    val code: String,
    val name: String,
    val fundHouse: String,
    val category: String, // "Flexi Cap", "Large Cap", "Small Cap", "Mid Cap", "Index", "Hybrid", "Debt"
    val nav: Double,
    val navPrev: Double,
    val dayChangePercent: Double,
    val expenseRatio: Double,
    val aumCr: Double,
    val return1Yr: Double,
    val return3Yr: Double,
    // Null until derived from real NAV history — renders as "—".
    val return5Yr: Double? = null,
    val equityPercent: Double,
    val debtPercent: Double,
    val cashPercent: Double,
    val otherPercent: Double = 0.0,
    val topHoldings: List<String> = emptyList(),
    val benchmark: String = "NIFTY 500 TRI",
    val riskLevel: String = "Very High"
)

data class RegistrarSourceHealth(
    val id: String,
    val name: String,
    val status: String, // "OPERATIONAL", "CAPTCHA_HANDOFF", "OFFLINE"
    val latencyMs: Int,
    val description: String
)

data class RegistrarLink(
    val title: String,
    val subtitle: String,
    val url: String,
    val badge: String = "Official"
)

data class PortfolioConcentration(
    val topHoldingSymbol: String,
    val topHoldingPercent: Double,
    val isHighRisk: Boolean, // > 25% single stock
    val top5SharePercent: Double,
    val equityAllocationPercent: Double,
    val mutualFundAllocationPercent: Double,
    val bestPerformer: String,
    val bestPerformerGainPercent: Double,
    val worstPerformer: String,
    val worstPerformerLossPercent: Double
)
