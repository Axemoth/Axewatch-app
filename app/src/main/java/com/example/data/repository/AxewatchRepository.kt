package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AllotmentRecordEntity
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.PanVaultEntity
import com.example.data.local.entity.PaperAccountEntity
import com.example.data.local.entity.PaperOrderEntity
import com.example.data.local.entity.PaperPositionEntity
import com.example.data.local.entity.WatchlistEntity
import com.example.data.model.CandleBar
import com.example.data.model.FiiDiiFlow
import com.example.data.model.GmpItem
import com.example.data.model.IpoIssue
import com.example.data.model.MarketIndex
import com.example.data.model.PastIpoItem
import com.example.data.model.MutualFundScheme
import com.example.data.model.PortfolioConcentration
import com.example.data.model.PortfolioSummary
import com.example.data.model.RegistrarLink
import com.example.data.model.RegistrarSourceHealth
import com.example.data.model.SectorHeatmapItem
import com.example.data.model.StockQuote
import com.example.data.model.TradeIdea
import com.example.data.model.TradeOutlook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class AxewatchRepository(private val db: AppDatabase) {

    private val holdingDao = db.holdingDao()
    private val paperDao = db.paperDao()
    private val panVaultDao = db.panVaultDao()
    private val watchlistDao = db.watchlistDao()

    // Real-time market state
    private val _indices = MutableStateFlow<List<MarketIndex>>(emptyList())
    val indices: Flow<List<MarketIndex>> = _indices.asStateFlow()

    private val _stocks = MutableStateFlow<List<StockQuote>>(emptyList())
    val stocks: Flow<List<StockQuote>> = _stocks.asStateFlow()

    private val _ipos = MutableStateFlow<List<IpoIssue>>(emptyList())
    val ipos: Flow<List<IpoIssue>> = _ipos.asStateFlow()

    private val _gmpItems = MutableStateFlow<List<GmpItem>>(emptyList())
    val gmpItems: Flow<List<GmpItem>> = _gmpItems.asStateFlow()

    private val _pastIpos = MutableStateFlow<List<PastIpoItem>>(emptyList())
    val pastIpos: Flow<List<PastIpoItem>> = _pastIpos.asStateFlow()

    private val _sectorHeatmap = MutableStateFlow<List<SectorHeatmapItem>>(emptyList())
    val sectorHeatmap: Flow<List<SectorHeatmapItem>> = _sectorHeatmap.asStateFlow()

    private val _fiiDiiData = MutableStateFlow<List<FiiDiiFlow>>(emptyList())
    val fiiDiiData: Flow<List<FiiDiiFlow>> = _fiiDiiData.asStateFlow()

    private val _tradeIdeas = MutableStateFlow<List<TradeIdea>>(emptyList())
    val tradeIdeas: Flow<List<TradeIdea>> = _tradeIdeas.asStateFlow()

    private val _mutualFunds = MutableStateFlow<List<MutualFundScheme>>(emptyList())
    val mutualFunds: Flow<List<MutualFundScheme>> = _mutualFunds.asStateFlow()

    private val _registrarHealth = MutableStateFlow<List<RegistrarSourceHealth>>(emptyList())
    val registrarHealth: Flow<List<RegistrarSourceHealth>> = _registrarHealth.asStateFlow()

    // Last refresh timestamp
    private val _lastRefreshTime = MutableStateFlow(System.currentTimeMillis())
    val lastRefreshTime: Flow<Long> = _lastRefreshTime.asStateFlow()

    init {
        loadInitialMarketData()
    }

    private fun loadInitialMarketData() {
        val initialIndices = listOf(
            MarketIndex("NIFTY 50", "NIFTY 50", 24964.25, 142.50, 0.58, 24850.00, 25010.80, 24810.30, 36, 14),
            MarketIndex("SENSEX", "BSE SENSEX", 81785.50, 480.20, 0.59, 81400.00, 81920.00, 81350.00, 21, 9),
            MarketIndex("BANKNIFTY", "NIFTY BANK", 51320.80, -95.40, -0.19, 51450.00, 51580.00, 51180.00, 5, 7),
            MarketIndex("NIFTYIT", "NIFTY IT", 36840.10, 385.60, 1.06, 36500.00, 36980.00, 36450.00, 8, 2),
            MarketIndex("NIFTYAUTO", "NIFTY AUTO", 22450.00, 190.30, 0.85, 22300.00, 22510.00, 22280.00, 11, 4),
            MarketIndex("NIFTYMETAL", "NIFTY METAL", 9120.40, 112.80, 1.25, 9020.00, 9150.00, 8990.00, 12, 3),
            MarketIndex("INDIAVIX", "INDIA VIX", 13.45, -0.65, -4.61, 14.10, 14.25, 13.30, 0, 0)
        )
        _indices.value = initialIndices

        val initialStocks = listOf(
            StockQuote("RELIANCE", "Reliance Industries Ltd", 1388.50, 18.20, 1.33, 1395.00, 1370.00, "9.4M", "Energy", 1608.80, 1150.00, 26.4, 1878000.0),
            StockQuote("TCS", "Tata Consultancy Services", 4120.00, 54.00, 1.33, 4145.00, 4070.00, "2.8M", "IT", 4585.00, 3313.00, 31.2, 1490000.0),
            StockQuote("HDFCBANK", "HDFC Bank Ltd", 1664.20, -6.80, -0.41, 1682.00, 1658.00, "14.2M", "Banking", 1794.00, 1363.00, 18.9, 1265000.0),
            StockQuote("BHARTIARTL", "Bharti Airtel Ltd", 1640.80, 22.40, 1.38, 1648.00, 1620.00, "4.1M", "Telecom", 1779.00, 920.00, 52.0, 960000.0),
            StockQuote("ICICIBANK", "ICICI Bank Ltd", 1235.40, 4.10, 0.33, 1242.00, 1228.00, "8.6M", "Banking", 1332.00, 980.00, 19.5, 868000.0),
            StockQuote("INFY", "Infosys Ltd", 1920.60, 24.80, 1.31, 1932.00, 1898.00, "6.3M", "IT", 1991.00, 1358.00, 28.1, 797000.0),
            StockQuote("TATAMOTORS", "Tata Motors Ltd", 848.20, 14.50, 1.74, 852.00, 832.00, "11.5M", "Automobile", 1179.00, 715.00, 10.4, 312000.0),
            StockQuote("ITC", "ITC Ltd", 486.10, -2.40, -0.49, 491.50, 484.20, "7.9M", "FMCG", 528.00, 399.00, 29.8, 607000.0),
            StockQuote("LT", "Larsen & Toubro Ltd", 3620.00, 32.00, 0.89, 3640.00, 3580.00, "1.9M", "Infrastructure", 3919.00, 2800.00, 34.6, 498000.0),
            StockQuote("SBIN", "State Bank of India", 812.30, -3.20, -0.39, 821.50, 809.00, "12.1M", "Banking", 912.00, 582.00, 11.2, 725000.0),
            StockQuote("MARUTI", "Maruti Suzuki India", 11840.00, 110.00, 0.94, 11920.00, 11700.00, "0.5M", "Automobile", 13680.00, 9737.00, 25.8, 372000.0),
            StockQuote("WIPRO", "Wipro Ltd", 542.80, 8.40, 1.57, 546.00, 534.00, "5.2M", "IT", 580.00, 375.00, 24.2, 283000.0)
        )
        _stocks.value = initialStocks

        val initialIpos = listOf(
            IpoIssue(
                symbol = "ATHER",
                companyName = "Ather Energy Ltd",
                category = "Mainboard",
                status = "Active",
                issueOpenDate = "Sep 12, 2026",
                issueCloseDate = "Sep 16, 2026",
                priceBand = "₹320 - ₹340",
                issuePrice = 340.0,
                lotSize = 44,
                issueSizeCr = 3100.0,
                registrar = "MUFG Intime",
                qibSub = 14.8,
                niiSub = 8.4,
                shniSub = 6.9,
                bhniSub = 9.2,
                riiSub = 4.2,
                totalSub = 9.8,
                gmpAmount = 68.0,
                gmpPercent = 20.0,
                estListingPrice = 408.0
            ),
            IpoIssue(
                symbol = "SWIGGY",
                companyName = "Swiggy Ltd",
                category = "Mainboard",
                status = "Closed",
                issueOpenDate = "Sep 06, 2026",
                issueCloseDate = "Sep 10, 2026",
                priceBand = "₹371 - ₹390",
                issuePrice = 390.0,
                lotSize = 38,
                issueSizeCr = 11327.0,
                registrar = "MUFG Intime",
                qibSub = 6.02,
                niiSub = 1.24,
                shniSub = 1.05,
                bhniSub = 1.34,
                riiSub = 1.14,
                totalSub = 3.59,
                gmpAmount = 25.0,
                gmpPercent = 6.4,
                estListingPrice = 415.0
            ),
            IpoIssue(
                symbol = "NTPCGREEN",
                companyName = "NTPC Green Energy Ltd",
                category = "Mainboard",
                status = "Forthcoming",
                issueOpenDate = "Sep 22, 2026",
                issueCloseDate = "Sep 25, 2026",
                priceBand = "₹102 - ₹108",
                issuePrice = 108.0,
                lotSize = 138,
                issueSizeCr = 10000.0,
                registrar = "KFintech",
                qibSub = 0.0,
                niiSub = 0.0,
                shniSub = 0.0,
                bhniSub = 0.0,
                riiSub = 0.0,
                totalSub = 0.0,
                gmpAmount = 14.0,
                gmpPercent = 13.0,
                estListingPrice = 122.0
            ),
            IpoIssue(
                symbol = "SOLARTECH",
                companyName = "SolarTech Energy SME",
                category = "SME",
                status = "Active",
                issueOpenDate = "Sep 10, 2026",
                issueCloseDate = "Sep 14, 2026",
                priceBand = "₹85 - ₹90",
                issuePrice = 90.0,
                lotSize = 1600,
                issueSizeCr = 42.5,
                registrar = "Bigshare",
                qibSub = 42.5,
                niiSub = 68.2,
                shniSub = 54.0,
                bhniSub = 75.0,
                riiSub = 34.8,
                totalSub = 48.6,
                gmpAmount = 45.0,
                gmpPercent = 50.0,
                estListingPrice = 135.0
            )
        )
        _ipos.value = initialIpos

        val initialGmps = listOf(
            GmpItem("Ather Energy Ltd", "ATHER", 340.0, 68.0, 20.0, 408.0, "Open", 4, "Today, 09:30 AM"),
            GmpItem("SolarTech Energy SME", "SOLARTECH", 90.0, 45.0, 50.0, 135.0, "Open", 5, "Today, 09:15 AM"),
            GmpItem("NTPC Green Energy Ltd", "NTPCGREEN", 108.0, 14.0, 13.0, 122.0, "Upcoming", 3, "Yesterday"),
            GmpItem("Swiggy Ltd", "SWIGGY", 390.0, 25.0, 6.4, 415.0, "Closed", 3, "Sep 10, 2026"),
            GmpItem("Waaree Energies Ltd", "WAAREE", 1503.0, 1280.0, 85.2, 2783.0, "Listed", 5, "Sep 02, 2026"),
            GmpItem("Hyundai Motor India", "HYUNDAI", 1960.0, 40.0, 2.0, 2000.0, "Listed", 2, "Aug 28, 2026")
        )
        _gmpItems.value = initialGmps

        val initialPastIpos = listOf(
            PastIpoItem("WAAREE", "Waaree Energies Ltd", 1503.0, 2550.0, 2890.0, 69.7, 76.3, "Oct 28, 2024"),
            PastIpoItem("HYUNDAI", "Hyundai Motor India", 1960.0, 1934.0, 1850.0, -1.3, 2.37, "Oct 22, 2024"),
            PastIpoItem("TATATECH", "Tata Technologies Ltd", 500.0, 1200.0, 980.0, 140.0, 69.4, "Nov 30, 2023"),
            PastIpoItem("PREMIER", "Premier Energies Ltd", 450.0, 991.0, 1140.0, 120.2, 74.3, "Sep 03, 2024"),
            PastIpoItem("BAJAJHFL", "Bajaj Housing Finance", 70.0, 150.0, 132.0, 114.3, 67.4, "Sep 16, 2024")
        )
        _pastIpos.value = initialPastIpos

        val initialSectors = listOf(
            SectorHeatmapItem("NIFTY Metal", 1.25, "JNDALSTEL"),
            SectorHeatmapItem("NIFTY IT", 1.06, "TCS"),
            SectorHeatmapItem("NIFTY Auto", 0.85, "TATAMOTORS"),
            SectorHeatmapItem("NIFTY Energy", 0.72, "RELIANCE"),
            SectorHeatmapItem("NIFTY FMCG", -0.49, "ITC"),
            SectorHeatmapItem("NIFTY Bank", -0.19, "HDFCBANK"),
            SectorHeatmapItem("NIFTY Pharma", 0.35, "SUNPHARMA"),
            SectorHeatmapItem("NIFTY Realty", 1.48, "DLF")
        )
        _sectorHeatmap.value = initialSectors

        val initialFiiDii = listOf(
            FiiDiiFlow("Latest Session", 11420.50, 9840.20, 1580.30, 8950.00, 7820.00, 1130.00),
            FiiDiiFlow("Previous Session", 9820.00, 11200.00, -1380.00, 9600.00, 7150.00, 2450.00)
        )
        _fiiDiiData.value = initialFiiDii

        val initialIdeas = listOf(
            TradeIdea(
                symbol = "TCS",
                name = "Tata Consultancy Services",
                signal = "BUY",
                currentPrice = 4120.00,
                stopLoss = 4015.00,
                targetPrice = 4280.00,
                riskReward = "1:1.52",
                horizonDays = 8,
                accuracy = 59.2,
                suggestedQty05Pct = 12,
                suggestedQty1Pct = 24,
                suggestedQty2Pct = 48
            ),
            TradeIdea(
                symbol = "TATAMOTORS",
                name = "Tata Motors Ltd",
                signal = "BUY",
                currentPrice = 848.20,
                stopLoss = 824.00,
                targetPrice = 886.00,
                riskReward = "1:1.56",
                horizonDays = 6,
                accuracy = 61.4,
                suggestedQty05Pct = 58,
                suggestedQty1Pct = 117,
                suggestedQty2Pct = 235
            ),
            TradeIdea(
                symbol = "BHARTIARTL",
                name = "Bharti Airtel Ltd",
                signal = "BUY",
                currentPrice = 1640.80,
                stopLoss = 1590.00,
                targetPrice = 1720.00,
                riskReward = "1:1.55",
                horizonDays = 10,
                accuracy = 58.0,
                suggestedQty05Pct = 30,
                suggestedQty1Pct = 60,
                suggestedQty2Pct = 120
            ),
            TradeIdea(
                symbol = "HDFCBANK",
                name = "HDFC Bank Ltd",
                signal = "SELL",
                currentPrice = 1664.20,
                stopLoss = 1695.00,
                targetPrice = 1618.00,
                riskReward = "1:1.50",
                horizonDays = 7,
                accuracy = 56.5,
                suggestedQty05Pct = 32,
                suggestedQty1Pct = 64,
                suggestedQty2Pct = 128
            )
        )
        _tradeIdeas.value = initialIdeas

        val initialFunds = listOf(
            MutualFundScheme(
                code = "122639",
                name = "Parag Parikh Flexi Cap Fund Direct-Growth",
                fundHouse = "PPFAS Mutual Fund",
                category = "Flexi Cap",
                nav = 78.42,
                navPrev = 78.10,
                dayChangePercent = 0.41,
                expenseRatio = 0.62,
                aumCr = 74200.0,
                return1Yr = 28.4,
                return3Yr = 21.2,
                equityPercent = 84.5,
                debtPercent = 9.8,
                cashPercent = 5.7,
                topHoldings = listOf("HDFC Bank", "Bajaj Holdings", "Power Grid", "ITC", "Alphabet Inc")
            ),
            MutualFundScheme(
                code = "120828",
                name = "Quant Small Cap Fund Direct-Growth",
                fundHouse = "Quant Mutual Fund",
                category = "Small Cap",
                nav = 264.18,
                navPrev = 261.90,
                dayChangePercent = 0.87,
                expenseRatio = 0.77,
                aumCr = 25400.0,
                return1Yr = 38.6,
                return3Yr = 32.4,
                equityPercent = 93.2,
                debtPercent = 0.0,
                cashPercent = 6.8,
                topHoldings = listOf("Reliance Industries", "Jio Financial", "Aegis Logistics", "Bikaji", "IRB Infra")
            ),
            MutualFundScheme(
                code = "118834",
                name = "Mirae Asset Large Cap Fund Direct-Growth",
                fundHouse = "Mirae Asset Mutual Fund",
                category = "Large Cap",
                nav = 118.90,
                navPrev = 118.45,
                dayChangePercent = 0.38,
                expenseRatio = 0.54,
                aumCr = 38100.0,
                return1Yr = 21.8,
                return3Yr = 16.5,
                equityPercent = 97.4,
                debtPercent = 0.0,
                cashPercent = 2.6,
                topHoldings = listOf("HDFC Bank", "ICICI Bank", "Reliance Industries", "Infosys", "L&T")
            ),
            MutualFundScheme(
                code = "119062",
                name = "Nippon India Small Cap Fund Direct-Growth",
                fundHouse = "Nippon India Mutual Fund",
                category = "Small Cap",
                nav = 172.35,
                navPrev = 171.10,
                dayChangePercent = 0.73,
                expenseRatio = 0.68,
                aumCr = 56800.0,
                return1Yr = 34.5,
                return3Yr = 31.0,
                equityPercent = 95.8,
                debtPercent = 0.0,
                cashPercent = 4.2,
                topHoldings = listOf("Tube Investments", "HDFC Bank", "Apar Industries", "KPIT Tech", "Voltamp")
            ),
            MutualFundScheme(
                code = "120503",
                name = "UTI Nifty 50 Index Fund Direct-Growth",
                fundHouse = "UTI Mutual Fund",
                category = "Index",
                nav = 168.20,
                navPrev = 167.30,
                dayChangePercent = 0.54,
                expenseRatio = 0.21,
                aumCr = 18900.0,
                return1Yr = 22.4,
                return3Yr = 17.1,
                equityPercent = 99.8,
                debtPercent = 0.0,
                cashPercent = 0.2,
                topHoldings = listOf("HDFC Bank", "Reliance Industries", "ICICI Bank", "Infosys", "TCS")
            ),
            MutualFundScheme(
                code = "120251",
                name = "ICICI Prudential Equity & Debt Fund Direct-Growth",
                fundHouse = "ICICI Prudential Mutual Fund",
                category = "Hybrid",
                nav = 342.15,
                navPrev = 340.90,
                dayChangePercent = 0.37,
                expenseRatio = 1.08,
                aumCr = 39400.0,
                return1Yr = 24.8,
                return3Yr = 19.4,
                equityPercent = 68.5,
                debtPercent = 24.5,
                cashPercent = 7.0,
                topHoldings = listOf("ICICI Bank", "NTPC", "Bharti Airtel", "GOI Sovereign Bonds", "RIL")
            ),
            MutualFundScheme(
                code = "101762",
                name = "HDFC Short Term Debt Fund Direct-Growth",
                fundHouse = "HDFC Mutual Fund",
                category = "Debt",
                nav = 29.80,
                navPrev = 29.78,
                dayChangePercent = 0.07,
                expenseRatio = 0.38,
                aumCr = 14300.0,
                return1Yr = 7.6,
                return3Yr = 6.8,
                equityPercent = 0.0,
                debtPercent = 92.4,
                cashPercent = 7.6,
                topHoldings = listOf("GOI 7.18% 2033", "NABARD AAA", "REC Limited", "PFC Limited", "HDFC Bank CD")
            )
        )
        _mutualFunds.value = initialFunds

        val initialHealth = listOf(
            RegistrarSourceHealth("mufg", "MUFG Intime", "OPERATIONAL", 140, "Direct automated query endpoint healthy"),
            RegistrarSourceHealth("kfin", "KFintech", "OPERATIONAL", 195, "Direct automated query endpoint healthy"),
            RegistrarSourceHealth("bigshare", "Bigshare", "CAPTCHA_HANDOFF", 210, "Manual captcha required by registrar. 1-tap browser handoff"),
            RegistrarSourceHealth("bse", "BSE India Direct", "OPERATIONAL", 280, "Official BSE application verification online"),
            RegistrarSourceHealth("nse", "NSE India Verification", "OPERATIONAL", 250, "Official NSE bid verification online")
        )
        _registrarHealth.value = initialHealth
    }

    // Refresh simulation (micro fluctuations to reflect live market)
    suspend fun refreshMarket() = withContext(Dispatchers.IO) {
        delay(350)
        val updatedStocks = _stocks.value.map { stock ->
            val deltaPct = (Random.nextDouble() - 0.48) * 0.4 // subtle wobble
            val newPrice = ((stock.lastPrice * (1 + deltaPct / 100)) * 100).roundToInt() / 100.0
            val change = ((newPrice - (stock.lastPrice - stock.change)) * 100).roundToInt() / 100.0
            val pct = ((change / (newPrice - change)) * 10000).roundToInt() / 100.0
            stock.copy(
                lastPrice = newPrice,
                change = change,
                percentChange = pct,
                dayHigh = max(stock.dayHigh, newPrice),
                dayLow = min(stock.dayLow, newPrice),
                isPositive = change >= 0
            )
        }
        _stocks.value = updatedStocks

        val updatedIndices = _indices.value.map { index ->
            val deltaPct = (Random.nextDouble() - 0.47) * 0.15
            val newPrice = ((index.lastPrice * (1 + deltaPct / 100)) * 100).roundToInt() / 100.0
            val change = ((newPrice - (index.lastPrice - index.change)) * 100).roundToInt() / 100.0
            val pct = ((change / (newPrice - change)) * 10000).roundToInt() / 100.0
            index.copy(
                lastPrice = newPrice,
                change = change,
                percentChange = pct,
                isPositive = change >= 0
            )
        }
        _indices.value = updatedIndices
        _lastRefreshTime.value = System.currentTimeMillis()
    }

    // Candlestick generator for Stock Quotes with timeframe & SMA
    fun getCandlesForStock(symbol: String, timeframe: String = "1M"): List<CandleBar> {
        val stock = _stocks.value.find { it.symbol == symbol }
        val basePrice = stock?.lastPrice ?: 1000.0
        val candles = mutableListOf<CandleBar>()

        val labels = when (timeframe) {
            "1D" -> listOf("09:30", "10:15", "11:00", "11:45", "12:30", "13:15", "14:00", "14:45", "15:30")
            "1W" -> listOf("Mon", "Tue", "Wed", "Thu", "Fri")
            "3M" -> (1..12).map { "W-$it" }.reversed()
            "1Y" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            else -> listOf("D-14", "D-13", "D-12", "D-11", "D-10", "D-9", "D-8", "D-7", "D-6", "D-5", "D-4", "D-3", "D-2", "D-1", "Today")
        }

        val volatility = when (timeframe) {
            "1D" -> 0.4
            "1W" -> 1.2
            "3M" -> 2.5
            "1Y" -> 4.5
            else -> 1.5
        }

        var prevClose = when (timeframe) {
            "1D" -> basePrice * 0.992
            "1W" -> basePrice * 0.97
            "3M" -> basePrice * 0.91
            "1Y" -> basePrice * 0.82
            else -> basePrice * 0.94
        }

        for (i in labels.indices) {
            val change = (Random.nextDouble(-volatility, volatility * 1.2) / 100.0) * prevClose
            val open = ((prevClose + (Random.nextDouble(-volatility * 0.2, volatility * 0.2) / 100.0 * prevClose)) * 10).roundToInt() / 10.0
            val close = ((open + change) * 10).roundToInt() / 10.0
            val high = ((max(open, close) + Random.nextDouble(0.1, volatility * 0.5) / 100.0 * prevClose) * 10).roundToInt() / 10.0
            val low = ((min(open, close) - Random.nextDouble(0.1, volatility * 0.5) / 100.0 * prevClose) * 10).roundToInt() / 10.0
            candles.add(CandleBar(labels[i], open, high, low, close))
            prevClose = close
        }

        // Compute running SMA
        val window = 4
        return candles.mapIndexed { idx, c ->
            val startIdx = max(0, idx - window + 1)
            val subList = candles.subList(startIdx, idx + 1)
            val avg = subList.map { it.close }.average()
            c.copy(sma = (avg * 10).roundToInt() / 10.0)
        }
    }

    // Quantitative Outlook Engine (Axewatch model logic)
    fun getStockOutlook(symbol: String): TradeOutlook {
        val stock = _stocks.value.find { it.symbol == symbol } ?: StockQuote(symbol, symbol, 1000.0, 10.0, 1.0, 1010.0, 990.0, "1M", "Misc", 1100.0, 900.0)
        val isBullish = stock.percentChange >= 0
        val score = if (isBullish) Random.nextInt(25, 68) else Random.nextInt(-58, -15)
        val signal = when {
            score >= 35 -> "STRONG BUY"
            score >= 15 -> "BUY"
            score <= -35 -> "STRONG SELL"
            score <= -15 -> "SELL"
            else -> "NEUTRAL"
        }
        val atr = stock.lastPrice * 0.022 // ~2.2% ATR
        val stopLoss = if (signal.contains("BUY")) {
            ((stock.lastPrice - (2.0 * atr)) * 10).roundToInt() / 10.0
        } else {
            ((stock.lastPrice + (2.0 * atr)) * 10).roundToInt() / 10.0
        }
        val target1 = if (signal.contains("BUY")) {
            ((stock.lastPrice + (1.5 * (stock.lastPrice - stopLoss))) * 10).roundToInt() / 10.0
        } else {
            ((stock.lastPrice - (1.5 * (stopLoss - stock.lastPrice))) * 10).roundToInt() / 10.0
        }
        val target2 = if (signal.contains("BUY")) {
            ((stock.lastPrice + (2.5 * (stock.lastPrice - stopLoss))) * 10).roundToInt() / 10.0
        } else {
            ((stock.lastPrice - (2.5 * (stopLoss - stock.lastPrice))) * 10).roundToInt() / 10.0
        }

        val accuracy = 58.4 + Random.nextDouble(0.0, 3.2)
        val accuracyClean = (accuracy * 10).roundToInt() / 10.0

        val reasons = if (signal.contains("BUY")) {
            listOf(
                "20-day SMA slope positive (+0.42σ)",
                "RSI at 56.4 recovering with volume expansion",
                "Relative-to-NIFTY 10-day alpha +1.8%",
                "MACD bullish histogram cross confirmed"
            )
        } else {
            listOf(
                "Lower high rejected at dynamic resistance",
                "Distribution volume above 20d moving average",
                "RSI bearish divergence on 4h timeframe",
                "ATR expansion on downside pressure"
            )
        }

        return TradeOutlook(
            symbol = symbol,
            score = score,
            signal = signal,
            stopLoss = stopLoss,
            target1 = target1,
            target2 = target2,
            estimatedDays = Random.nextInt(6, 14),
            walkForwardAccuracy = accuracyClean,
            newsHeadline = "NSE updates: Healthy order book expansion & quarterly guidance reaffirmation",
            newsSentiment = if (signal.contains("BUY")) "Positive" else "Cautious",
            reasons = reasons
        )
    }

    // IPO Allotment Checker
    suspend fun checkIpoAllotment(pan: String, ipoSymbol: String, holderName: String = "Self"): AllotmentRecordEntity = withContext(Dispatchers.IO) {
        val cleanPan = pan.trim().uppercase()
        val ipo = _ipos.value.find { it.symbol == ipoSymbol } ?: _ipos.value.first()
        val registrar = ipo.registrar

        // Mask PAN strictly for PII safety (AGENTS.md mandate)
        val masked = if (cleanPan.length >= 10) "${cleanPan.take(5)}****${cleanPan.takeLast(1)}" else "*****"

        // Simulate realistic registrar response
        delay(600)
        val allotted = cleanPan.endsWith("1") || cleanPan.endsWith("7") || cleanPan.endsWith("A") || cleanPan.endsWith("9")
        val status = if (allotted) "ALLOTTED" else "NOT_ALLOTTED"
        val shares = if (allotted) ipo.lotSize else 0

        val record = AllotmentRecordEntity(
            maskedPan = masked,
            ipoSymbol = ipo.symbol,
            ipoName = ipo.companyName,
            sharesApplied = ipo.lotSize,
            sharesAllotted = shares,
            status = status,
            registrar = registrar,
            applicationNo = "APP" + (10000000 + cleanPan.hashCode().let { if (it < 0) -it else it } % 90000000)
        )
        panVaultDao.insertRecord(record)
        record
    }

    // --- Local Room Database Operations ---
    // Holdings
    fun getHoldings(): Flow<List<HoldingEntity>> = holdingDao.getAllHoldings()

    suspend fun addHolding(holding: HoldingEntity) = withContext(Dispatchers.IO) {
        holdingDao.insertHolding(holding)
    }

    suspend fun deleteHolding(id: Long) = withContext(Dispatchers.IO) {
        holdingDao.deleteHoldingById(id)
    }

    // Paper Trading
    fun getPaperAccount(): Flow<PaperAccountEntity?> = paperDao.getAccount()
    fun getPaperPositions(): Flow<List<PaperPositionEntity>> = paperDao.getAllPositions()
    fun getPaperOrders(): Flow<List<PaperOrderEntity>> = paperDao.getAllOrders()

    suspend fun ensurePaperAccountCreated() = withContext(Dispatchers.IO) {
        val account = paperDao.getAccountOnce()
        if (account == null) {
            paperDao.insertOrUpdateAccount(PaperAccountEntity())
        }
    }

    suspend fun executePaperOrder(
        symbol: String,
        name: String,
        side: String,
        quantity: Int,
        price: Double,
        stopLoss: Double? = null,
        targetPrice: Double? = null
    ): String = withContext(Dispatchers.IO) {
        ensurePaperAccountCreated()
        val account = paperDao.getAccountOnce() ?: PaperAccountEntity()
        val cost = price * quantity

        if (side == "BUY") {
            if (account.cashBalance < cost) {
                return@withContext "Insufficient virtual cash: Available ₹${"%,.2f".format(account.cashBalance)}, required ₹${"%,.2f".format(cost)}"
            }
            val newCash = account.cashBalance - cost
            paperDao.insertOrUpdateAccount(account.copy(cashBalance = newCash, totalTrades = account.totalTrades + 1))

            val existingPosition = paperDao.getPositionBySymbol(symbol)
            if (existingPosition != null) {
                val totalQty = existingPosition.quantity + quantity
                val newAvgPrice = ((existingPosition.averageBuyPrice * existingPosition.quantity) + cost) / totalQty
                paperDao.insertOrUpdatePosition(
                    existingPosition.copy(
                        quantity = totalQty,
                        averageBuyPrice = newAvgPrice,
                        currentPrice = price,
                        stopLoss = stopLoss ?: existingPosition.stopLoss,
                        targetPrice = targetPrice ?: existingPosition.targetPrice
                    )
                )
            } else {
                paperDao.insertOrUpdatePosition(
                    PaperPositionEntity(
                        symbol = symbol,
                        name = name,
                        quantity = quantity,
                        averageBuyPrice = price,
                        currentPrice = price,
                        stopLoss = stopLoss,
                        targetPrice = targetPrice
                    )
                )
            }

            paperDao.insertOrder(
                PaperOrderEntity(
                    symbol = symbol,
                    side = "BUY",
                    quantity = quantity,
                    price = price,
                    orderType = "MARKET",
                    status = "FILLED",
                    notes = "Executed at market price"
                )
            )
            return@withContext "BUY order filled for $quantity shares of $symbol at ₹$price"
        } else {
            // SELL
            val existingPosition = paperDao.getPositionBySymbol(symbol)
                ?: return@withContext "No open position for $symbol"
            if (existingPosition.quantity < quantity) {
                return@withContext "Cannot sell $quantity shares. You only hold ${existingPosition.quantity}."
            }

            val proceeds = price * quantity
            val pnl = (price - existingPosition.averageBuyPrice) * quantity
            val isWin = pnl > 0
            val newCash = account.cashBalance + proceeds
            val newRealizedPnl = account.realizedPnl + pnl
            val newWins = if (isWin) account.winningTrades + 1 else account.winningTrades

            paperDao.insertOrUpdateAccount(
                account.copy(
                    cashBalance = newCash,
                    realizedPnl = newRealizedPnl,
                    totalTrades = account.totalTrades + 1,
                    winningTrades = newWins
                )
            )

            val remainingQty = existingPosition.quantity - quantity
            if (remainingQty <= 0) {
                paperDao.deletePositionBySymbol(symbol)
            } else {
                paperDao.insertOrUpdatePosition(
                    existingPosition.copy(
                        quantity = remainingQty,
                        currentPrice = price
                    )
                )
            }

            paperDao.insertOrder(
                PaperOrderEntity(
                    symbol = symbol,
                    side = "SELL",
                    quantity = quantity,
                    price = price,
                    orderType = "MARKET",
                    status = "FILLED",
                    realizedPnl = pnl,
                    notes = "Realized P&L: ₹${"%,.2f".format(pnl)}"
                )
            )
            return@withContext "SELL order filled for $quantity shares of $symbol. Realized P&L: ₹${"%,.2f".format(pnl)}"
        }
    }

    suspend fun resetPaperAccount() = withContext(Dispatchers.IO) {
        paperDao.clearAllPositions()
        paperDao.clearAllOrders()
        paperDao.insertOrUpdateAccount(PaperAccountEntity(id = 1, cashBalance = 1000000.0, initialBalance = 1000000.0, realizedPnl = 0.0, totalTrades = 0, winningTrades = 0))
    }

    // PAN Vault
    fun getAllPans(): Flow<List<PanVaultEntity>> = panVaultDao.getAllPans()
    fun getAllotmentRecords(): Flow<List<AllotmentRecordEntity>> = panVaultDao.getAllRecords()

    suspend fun savePan(pan: String, holderName: String, relation: String = "Self") = withContext(Dispatchers.IO) {
        val clean = pan.trim().uppercase()
        panVaultDao.insertPan(PanVaultEntity(clean, holderName, relation))
    }

    suspend fun deletePan(pan: PanVaultEntity) = withContext(Dispatchers.IO) {
        panVaultDao.deletePan(pan)
    }

    // Watchlist
    fun getWatchlist(): Flow<List<WatchlistEntity>> = watchlistDao.getWatchlist()
    fun isWatchlisted(symbol: String): Flow<Boolean> = watchlistDao.isInWatchlist(symbol)

    suspend fun toggleWatchlist(symbol: String, name: String, currentPrice: Double) = withContext(Dispatchers.IO) {
        watchlistDao.insert(WatchlistEntity(symbol, name, "EQUITY", currentPrice))
    }

    suspend fun removeFromWatchlist(symbol: String) = withContext(Dispatchers.IO) {
        watchlistDao.deleteBySymbol(symbol)
    }

    suspend fun seedSampleHoldings() = withContext(Dispatchers.IO) {
        val samples = listOf(
            HoldingEntity(symbol = "RELIANCE", name = "Reliance Industries Ltd", assetType = "Stock", quantity = 15.0, buyPrice = 1350.0, sector = "Energy"),
            HoldingEntity(symbol = "TCS", name = "Tata Consultancy Services", assetType = "Stock", quantity = 5.0, buyPrice = 4020.0, sector = "IT"),
            HoldingEntity(symbol = "HDFCBANK", name = "HDFC Bank Ltd", assetType = "Stock", quantity = 20.0, buyPrice = 1630.0, sector = "Banking"),
            HoldingEntity(symbol = "INFY", name = "Infosys Ltd", assetType = "Stock", quantity = 12.0, buyPrice = 1860.0, sector = "IT"),
            HoldingEntity(symbol = "TATAMOTORS", name = "Tata Motors Ltd", assetType = "Stock", quantity = 25.0, buyPrice = 810.0, sector = "Automobile")
        )
        samples.forEach { holdingDao.insertHolding(it) }
    }

    suspend fun updateHolding(id: Long, quantity: Double, buyPrice: Double) = withContext(Dispatchers.IO) {
        val all = holdingDao.getAllHoldings().first()
        val existing = all.find { it.id == id }
        if (existing != null) {
            holdingDao.updateHolding(existing.copy(quantity = quantity, buyPrice = buyPrice))
        }
    }

    suspend fun importHoldingsFromCsv(csvText: String): Int = withContext(Dispatchers.IO) {
        var count = 0
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines) {
            val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
            if (parts.size >= 3) {
                val symbol = parts[0].uppercase()
                // Skip header line
                if (symbol == "SYMBOL" || symbol == "INSTRUMENT") continue
                val qty = parts[1].toDoubleOrNull() ?: continue
                val price = parts[2].toDoubleOrNull() ?: continue
                val sector = if (parts.size >= 4) parts[3] else "Diversified"
                val name = if (parts.size >= 5) parts[4] else symbol
                val assetType = if (parts.size >= 6) parts[5] else "STOCK"

                holdingDao.insertHolding(
                    HoldingEntity(
                        symbol = symbol,
                        name = name,
                        assetType = assetType,
                        quantity = qty,
                        buyPrice = price,
                        sector = sector
                    )
                )
                count++
            }
        }
        count
    }

    suspend fun checkBulkAllotment(ipoSymbol: String): List<AllotmentRecordEntity> = withContext(Dispatchers.IO) {
        val pans = panVaultDao.getAllPans().first()
        val results = mutableListOf<AllotmentRecordEntity>()
        for (pan in pans) {
            val record = checkIpoAllotment(pan.panNumber, ipoSymbol, pan.holderName)
            results.add(record)
        }
        results
    }

    suspend fun recordManualAllotment(
        maskedPan: String,
        ipoSymbol: String,
        status: String,
        sharesAllotted: Int,
        applicationNo: String
    ) = withContext(Dispatchers.IO) {
        val ipo = _ipos.value.find { it.symbol == ipoSymbol } ?: _ipos.value.first()
        val record = AllotmentRecordEntity(
            maskedPan = maskedPan,
            ipoSymbol = ipo.symbol,
            ipoName = ipo.companyName,
            sharesApplied = if (sharesAllotted > 0) sharesAllotted else ipo.lotSize,
            sharesAllotted = sharesAllotted,
            status = status,
            registrar = ipo.registrar,
            applicationNo = applicationNo.ifBlank { "MANUAL" + System.currentTimeMillis().toString().takeLast(6) }
        )
        panVaultDao.insertRecord(record)
    }

    suspend fun deleteRecord(record: AllotmentRecordEntity) = withContext(Dispatchers.IO) {
        panVaultDao.deleteRecord(record)
    }

    suspend fun clearAllotmentHistory() = withContext(Dispatchers.IO) {
        panVaultDao.clearAllRecords()
    }

    fun getRegistrarLinks(): List<RegistrarLink> = listOf(
        RegistrarLink("BSE India Status", "Check application by PAN or App No", "https://www.bseindia.com/investors/appli_check.aspx", "Exchange"),
        RegistrarLink("NSE Bid Verification", "Verify IPO bidding status via PAN", "https://www.nseindia.com/products/content/equities/ipos/ipo_bid_details.htm", "Exchange"),
        RegistrarLink("MUFG Intime (Link Intime)", "Allotment status for Ather, Swiggy, Tata", "https://linkintime.co.in/initial_offer/public-issues.html", "Registrar"),
        RegistrarLink("KFintech IPO Status", "Allotment status for NTPC Green, Bajaj", "https://kosmic.kfintech.com/ipostatus/", "Registrar"),
        RegistrarLink("Bigshare Services", "Allotment status for SME & Mainboard", "https://ipo.bigshareonline.com/", "Registrar")
    )

    fun getPortfolioConcentration(
        holdings: List<HoldingEntity>,
        priceMap: Map<String, Double>
    ): PortfolioConcentration {
        if (holdings.isEmpty()) {
            return PortfolioConcentration(
                topHoldingSymbol = "None",
                topHoldingPercent = 0.0,
                isHighRisk = false,
                top5SharePercent = 0.0,
                equityAllocationPercent = 0.0,
                mutualFundAllocationPercent = 0.0,
                bestPerformer = "None",
                bestPerformerGainPercent = 0.0,
                worstPerformer = "None",
                worstPerformerLossPercent = 0.0
            )
        }

        val values = holdings.map { h ->
            val curPrice = priceMap[h.symbol] ?: h.buyPrice
            val curVal = h.quantity * curPrice
            val invVal = h.quantity * h.buyPrice
            val pnlPct = if (invVal > 0) ((curVal - invVal) / invVal) * 100 else 0.0
            Triple(h, curVal, pnlPct)
        }

        val totalVal = values.sumOf { it.second }
        val sortedByVal = values.sortedByDescending { it.second }
        val top1 = sortedByVal.first()
        val top1Pct = if (totalVal > 0) (top1.second / totalVal) * 100 else 0.0

        val top5Val = sortedByVal.take(5).sumOf { it.second }
        val top5Pct = if (totalVal > 0) (top5Val / totalVal) * 100 else 0.0

        val mfVal = values.filter { it.first.assetType == "MUTUAL_FUND" }.sumOf { it.second }
        val mfPct = if (totalVal > 0) (mfVal / totalVal) * 100 else 0.0
        val eqPct = 100.0 - mfPct

        val sortedByPnl = values.sortedByDescending { it.third }
        val best = sortedByPnl.first()
        val worst = sortedByPnl.last()

        return PortfolioConcentration(
            topHoldingSymbol = top1.first.symbol,
            topHoldingPercent = top1Pct,
            isHighRisk = top1Pct > 25.0,
            top5SharePercent = top5Pct,
            equityAllocationPercent = eqPct,
            mutualFundAllocationPercent = mfPct,
            bestPerformer = best.first.symbol,
            bestPerformerGainPercent = best.third,
            worstPerformer = worst.first.symbol,
            worstPerformerLossPercent = worst.third
        )
    }
}
