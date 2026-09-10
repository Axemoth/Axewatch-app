package com.example.data.model

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
    val dayHigh: Double = lastPrice * 1.015,
    val dayLow: Double = lastPrice * 0.985,
    val volume: String = "5.0M",
    val sector: String = "General",
    val week52High: Double = lastPrice * 1.25,
    val week52Low: Double = lastPrice * 0.75,
    val peRatio: Double = 24.5,
    val marketCapCr: Double = 50000.0,
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
    val score: Int, // -100 to +100
    val signal: String, // "STRONG BUY", "BUY", "NEUTRAL", "SELL"
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val estimatedDays: Int,
    val walkForwardAccuracy: Double, // e.g. 58.4%
    val newsHeadline: String = "Trading in active market range with institutional interest.",
    val newsSentiment: String = "Positive", // "Positive", "Neutral", "Negative"
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
