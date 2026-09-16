package com.aistudio.axewatch.trader.data.model

data class MarketIndex(
    val symbol: String,
    val name: String,
    val lastPrice: Double,
    val change: Double,
    val percentChange: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val advances: Int = 0,
    val declines: Int = 0,
    val isPositive: Boolean = percentChange >= 0
)

data class StockQuote(
    val symbol: String,
    val name: String,
    val lastPrice: Double,
    val change: Double,
    val percentChange: Double,
    val dayHigh: Double = lastPrice,
    val dayLow: Double = lastPrice,
    val volume: String = "—",
    val sector: String = "General",
    // Fundamentals below are unknown unless a quote provider supplies them.
    // Null renders as "—" — never invent P/E or market-cap figures.
    val week52High: Double? = null,
    val week52Low: Double? = null,
    val peRatio: Double? = null,
    val marketCapCr: Double? = null,
    val isPositive: Boolean = percentChange >= 0
)

data class CandleBar(
    val dateLabel: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val sma: Double? = null,
    val isBullish: Boolean = close >= open
)

data class TradeOutlook(
    val symbol: String,
    val score: Int, // -100 to +100, transparent on-device rule score
    val signal: String, // "STRONG BUY", "BUY", "NEUTRAL", "SELL", "STRONG SELL", "INSUFFICIENT DATA"
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val estimatedDays: Int,
    // Null = not measured on-device. Any displayed accuracy must come from a
    // real validation run, never from a formula of the current score.
    val walkForwardAccuracy: Double? = null,
    val newsHeadline: String = "",
    val newsSentiment: String = "Neutral", // "Positive", "Neutral", "Negative"
    val reasons: List<String>
)

data class SectorHeatmapItem(
    val sector: String,
    val percentChange: Double,
    val topStock: String
)

data class FiiDiiFlow(
    val date: String,
    val fiiGrossBuy: Double,
    val fiiGrossSell: Double,
    val fiiNet: Double,
    val diiGrossBuy: Double,
    val diiGrossSell: Double,
    val diiNet: Double
)

data class IndexConstituent(
    val symbol: String,
    val name: String,
    val sector: String,
    val weightPercent: Double,
    val lastPrice: Double,
    val change: Double,
    val percentChange: Double,
    val dayHigh: Double = lastPrice,
    val dayLow: Double = lastPrice,
    val volume: String = "—",
    val isPositive: Boolean = percentChange >= 0
) {
    fun toStockQuote(): StockQuote = StockQuote(
        symbol = symbol,
        name = name,
        lastPrice = lastPrice,
        change = change,
        percentChange = percentChange,
        dayHigh = dayHigh,
        dayLow = dayLow,
        volume = volume,
        sector = sector,
        week52High = null,
        week52Low = null,
        peRatio = null,
        marketCapCr = null,
        isPositive = isPositive
    )
}
