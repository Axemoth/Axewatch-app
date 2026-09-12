package com.example.data.model

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
    val estListingPrice: Double = 0.0
)

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
    val currentPrice: Double,
    val listingGainPercent: Double,
    val totalSub: Double,
    val listingDate: String
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
    val accuracy: Double,
    val score: Int = if (signal == "BUY") 72 else -58,
    val driftPercent: Double = 0.4,
    val ageDays: Double = 0.5,
    val suggestedQty05Pct: Int,
    val suggestedQty1Pct: Int,
    val suggestedQty2Pct: Int,
    val reason: String = "Multi-timeframe momentum breakout above 20 EMA with volume expansion."
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
    val modelName: String = "NIFTY-50 Multi-Factor Alpha Engine",
    val modelVersion: String = "2.4.1",
    val embargoStatus: String = "Active (10D Embargo)",
    val status: String = "Ready",
    val walkForwardAccuracy: Double = 58.4,
    val accuracyOutSample: Double = walkForwardAccuracy,
    val walkForwardAuc: Double = 0.63,
    val rocAuc: Double = walkForwardAuc,
    val strongBuyPrecision: Double = 64.0,
    val precisionStrongBuy: Double = strongBuyPrecision,
    val pickSpreadBps: Double = 340.0,
    val stocksCovered: Int = 50,
    val samplesCount: Int = 15200,
    val sampleCount: Int = samplesCount,
    val featuresCount: Int = 49,
    val featureVersion: Int = 2,
    val baseRate: Double = 50.2,
    val baseRateAccuracy: Double = baseRate,
    val hasEdge: Boolean = true,
    val horizonDays: Int = 10,
    val forecastHorizonDays: Int = horizonDays,
    val trainedDate: String = "Today",
    val calibrationBuckets: List<ModelCalibrationBucket> = listOf(
        ModelCalibrationBucket("40–48%", 0.40, 0.48, 3040, 43.0, "40–48%"),
        ModelCalibrationBucket("48–52%", 0.48, 0.52, 4120, 51.0, "48–52%"),
        ModelCalibrationBucket("52–58%", 0.52, 0.58, 3860, 57.0, "52–58%"),
        ModelCalibrationBucket("58–65%", 0.58, 0.65, 2480, 63.0, "58–65%"),
        ModelCalibrationBucket("65–78%", 0.65, 0.78, 1700, 69.0, "65–78%")
    )
)

data class PortfolioSummary(
    val totalInvested: Double,
    val currentValue: Double,
    val totalProfitLoss: Double,
    val profitLossPercent: Double,
    val dayReturn: Double,
    val dayReturnPercent: Double,
    val xirrPercent: Double = 14.8,
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
    val return5Yr: Double = return3Yr * 0.94,
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
