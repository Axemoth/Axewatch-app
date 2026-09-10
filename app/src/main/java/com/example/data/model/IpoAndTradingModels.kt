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
    val stopLoss: Double,
    val targetPrice: Double,
    val riskReward: String,
    val horizonDays: Int,
    val accuracy: Double,
    val suggestedQty05Pct: Int,
    val suggestedQty1Pct: Int,
    val suggestedQty2Pct: Int
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
    val equityPercent: Double,
    val debtPercent: Double,
    val cashPercent: Double,
    val topHoldings: List<String> = emptyList()
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
