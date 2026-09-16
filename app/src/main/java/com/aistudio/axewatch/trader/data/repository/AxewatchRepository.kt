package com.aistudio.axewatch.trader.data.repository

import android.content.SharedPreferences
import com.aistudio.axewatch.trader.data.analysis.TechnicalAnalysisEngine
import com.aistudio.axewatch.trader.data.local.AppDatabase
import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import com.aistudio.axewatch.trader.data.local.entity.HoldingEntity
import com.aistudio.axewatch.trader.data.local.entity.PanVaultEntity
import com.aistudio.axewatch.trader.data.local.entity.PaperAccountEntity
import com.aistudio.axewatch.trader.data.local.entity.PaperOrderEntity
import com.aistudio.axewatch.trader.data.local.entity.PaperPositionEntity
import com.aistudio.axewatch.trader.data.local.entity.WatchlistEntity
import com.aistudio.axewatch.trader.data.model.CandleBar
import com.aistudio.axewatch.trader.data.model.FiiDiiFlow
import com.aistudio.axewatch.trader.data.model.GmpItem
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.data.model.MarketIndex
import com.aistudio.axewatch.trader.data.model.MutualFundScheme
import com.aistudio.axewatch.trader.data.model.PastIpoItem
import com.aistudio.axewatch.trader.data.model.PortfolioConcentration
import com.aistudio.axewatch.trader.data.model.PortfolioSummary
import com.aistudio.axewatch.trader.data.model.QuantModelReportCard
import com.aistudio.axewatch.trader.data.model.RegistrarLink
import com.aistudio.axewatch.trader.data.model.RegistrarSourceHealth
import com.aistudio.axewatch.trader.data.model.SectorHeatmapItem
import com.aistudio.axewatch.trader.data.model.StockQuote
import com.aistudio.axewatch.trader.data.model.TradeIdea
import com.aistudio.axewatch.trader.data.model.TradeOutlook
import com.aistudio.axewatch.trader.data.provider.IndexConstituentsProvider
import com.aistudio.axewatch.trader.data.remote.IpoAllotmentService
import com.aistudio.axewatch.trader.data.remote.FiiDiiService
import com.aistudio.axewatch.trader.data.remote.IpoGmpService
import com.aistudio.axewatch.trader.data.remote.MutualFundService
import com.aistudio.axewatch.trader.data.remote.NewsSentimentService
import com.aistudio.axewatch.trader.data.remote.YahooFinanceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class AxewatchRepository(
    private val db: AppDatabase,
    private val prefs: SharedPreferences? = null
) {

    private val holdingDao = db.holdingDao()
    private val paperDao = db.paperDao()
    private val panVaultDao = db.panVaultDao()
    private val watchlistDao = db.watchlistDao()

    // Real Remote API Services
    val yahooService = YahooFinanceService()
    val allotmentService = IpoAllotmentService()
    val gmpService = IpoGmpService()
    val mfService = MutualFundService()
    val newsService = NewsSentimentService()

    init {
        // Per-attempt allotment health: the service callback fires on every
        // MUFG/KFin attempt (definitive answers AND transport trouble), so
        // the strip is measured, never hardcoded.
        allotmentService.onSourceResult = { id, ok, ms -> recordAllotSource(id, ok, ms) }
    }

    private val candleCache = mutableMapOf<String, List<CandleBar>>()
    private val outlookCache = mutableMapOf<String, TradeOutlook>()

    // ---- Registrar ID memory + attribution (mirrors the web backend) ----
    // MUFG rotates its dropdown to recent issues, but SearchOnPan keeps
    // answering old company IDs. Every list we see merges into append-only
    // SharedPreferences memory so past IPOs stay checkable.
    data class IdEntry(val id: String, val name: String, val lastSeen: Long)

    private fun readIdMemory(key: String): MutableMap<String, IdEntry> {
        val out = mutableMapOf<String, IdEntry>()
        try {
            val raw = prefs?.getString(key, null) ?: return out
            val obj = org.json.JSONObject(raw)
            val now = System.currentTimeMillis()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val e = obj.optJSONObject(k) ?: continue
                if (now - e.optLong("lastSeen", 0) < 180L * 86400_000L) {
                    out[k] = IdEntry(e.optString("id"), e.optString("name"), e.optLong("lastSeen", 0))
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun rememberIds(key: String, companies: List<Pair<String, String>>) {
        val prefs = prefs ?: return
        try {
            val mem = readIdMemory(key)
            val now = System.currentTimeMillis()
            for ((id, name) in companies) {
                val canon = IpoAllotmentService.canonIpoName(name)
                if (canon.isNotEmpty() && id.isNotBlank()) {
                    mem[canon] = IdEntry(id, name, now)
                }
            }
            val obj = org.json.JSONObject()
            for ((k, v) in mem) {
                obj.put(k, org.json.JSONObject().put("id", v.id).put("name", v.name).put("lastSeen", v.lastSeen))
            }
            prefs.edit().putString(key, obj.toString()).apply()
        } catch (_: Exception) {
        }
    }

    data class RegistrarAttribution(
        val registrar: String, // "MUFG Intime" | "Bigshare" | "KFintech?" | "Unknown"
        val dropdownName: String?,
        val automated: Boolean
    )

    /** Bigshare's captcha guards only SEARCH — company dropdowns are public. */
    private var bigshareDirCache: Pair<Long, List<Pair<String, String>>> = 0L to emptyList()

    private suspend fun bigshareDirectory(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val (ts, cached) = bigshareDirCache
        if (cached.isNotEmpty() && System.currentTimeMillis() - ts < 24 * 3600_000L) return@withContext cached
        val out = mutableListOf<Pair<String, String>>()
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        for (base in listOf("https://ipo.bigshareonline.com", "https://ipo1.bigshareonline.com", "https://ipo2.bigshareonline.com")) {
            try {
                kotlinx.coroutines.delay(1000)
                val req = okhttp3.Request.Builder().url("$base/ipo_status.html")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)").get().build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) continue
                val html = resp.body?.string() ?: continue
                val sel = Regex("<select[^>]*id=\"ddlCompany\"[^>]*>(.*?)</select>",
                    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                    .find(html)?.groupValues?.getOrNull(1) ?: html
                val opts = Regex("<option[^>]*value=\"([^\"]*)\"[^>]*>([^<]{2,100})</option>").findAll(sel)
                for (m in opts) {
                    val v = m.groupValues[1].trim()
                    val n = m.groupValues[2].trim()
                    if (v.isNotEmpty() && v != "0" && n.isNotEmpty() && !n.contains("select", true)) {
                        out.add(v to n)
                    }
                }
            } catch (_: Exception) {
            }
        }
        val dedup = out.distinctBy { it.second }
        bigshareDirCache = System.currentTimeMillis() to dedup
        rememberIds("bigshare_ids", dedup)
        dedup
    }

    suspend fun attributeRegistrar(issueName: String, symbol: String): RegistrarAttribution =
        withContext(Dispatchers.IO) {
            // 1/2. Live MUFG list, then remembered IDs.
            try {
                val live = allotmentService.fetchMufgCompanies()
                rememberIds("mufg_ids", live.map { it.id to it.name })
                val want = IpoAllotmentService.canonIpoName(issueName.ifBlank { symbol })
                val hit = live.firstOrNull {
                    IpoAllotmentService.canonIpoName(it.name) == want
                } ?: live.firstOrNull {
                    val c = IpoAllotmentService.canonIpoName(it.name)
                    c.length >= 10 && (c.contains(want) || (want.length >= 10 && want.contains(c)))
                }
                if (hit != null) return@withContext RegistrarAttribution("MUFG Intime", hit.name, true)
            } catch (_: Exception) {
            }
            val wantMem = IpoAllotmentService.canonIpoName(issueName.ifBlank { symbol })
            readIdMemory("mufg_ids")[wantMem]?.let {
                return@withContext RegistrarAttribution("MUFG Intime", it.name, true)
            }
            // 3/4. Bigshare live directory, then remembered IDs.
            try {
                for ((_, name) in bigshareDirectory()) {
                    if (IpoAllotmentService.ipoNamesMatch(name, issueName.ifBlank { symbol })) {
                        return@withContext RegistrarAttribution("Bigshare", name, false)
                    }
                }
            } catch (_: Exception) {
            }
            val wantBs = IpoAllotmentService.canonIpoName(issueName.ifBlank { symbol })
            readIdMemory("bigshare_ids")[wantBs]?.let {
                return@withContext RegistrarAttribution("Bigshare", it.name, false)
            }
            RegistrarAttribution("Unknown", null, true)
        }

    // ---- Measured source health (never hardcoded OPERATIONAL) ----
    private data class SourceStat(var ok: Int = 0, var fail: Int = 0, var lastMs: Int = 0)

    private val allotSourceStats = mutableMapOf(
        "allot_mufg" to SourceStat(),
        "allot_kfin" to SourceStat()
    )

    fun recordAllotSource(id: String, ok: Boolean, ms: Int) {
        val s = allotSourceStats.getOrPut(id) { SourceStat() }
        if (ok) s.ok++ else s.fail++
        s.lastMs = ms
        rebuildAllotHealth()
    }

    private fun rebuildAllotHealth(extra: List<RegistrarSourceHealth> = emptyList()) {
        fun entry(id: String, name: String): RegistrarSourceHealth {
            val s = allotSourceStats[id] ?: SourceStat()
            val total = s.ok + s.fail
            return when {
                total == 0 -> RegistrarSourceHealth(id, name, "IDLE", 0, "No checks run yet this session")
                s.fail > 0 && s.ok == 0 -> RegistrarSourceHealth(id, name, "DEGRADED", s.lastMs, "Recent lookups failing — backing off")
                else -> RegistrarSourceHealth(id, name, "OPERATIONAL", s.lastMs, "Last lookup succeeded")
            }
        }
        _registrarHealth.value = listOf(
            entry("allot_mufg", "MUFG Intime"),
            entry("allot_kfin", "KFintech"),
            RegistrarSourceHealth("bigshare", "Bigshare", "CAPTCHA_HANDOFF", 0, "Manual captcha required. 1-tap browser handoff"),
            RegistrarSourceHealth("bse", "BSE India", "CAPTCHA_HANDOFF", 0, "Manual captcha + bot wall. Use the manual link"),
            RegistrarSourceHealth("nse", "NSE India", "CAPTCHA_HANDOFF", 0, "Manual verification + bot wall. Use the manual link")
        ) + extra
    }

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

    private val _quantModelReportCard = MutableStateFlow(QuantModelReportCard())
    val quantModelReportCard: Flow<QuantModelReportCard> = _quantModelReportCard.asStateFlow()

    private val _mutualFunds = MutableStateFlow<List<MutualFundScheme>>(emptyList())
    val mutualFunds: Flow<List<MutualFundScheme>> = _mutualFunds.asStateFlow()

    private val _registrarHealth = MutableStateFlow<List<RegistrarSourceHealth>>(emptyList())
    val registrarHealth: Flow<List<RegistrarSourceHealth>> = _registrarHealth.asStateFlow()

    // ---- Live trade-idea scan (on-device rule engine over real Yahoo bars) ----
    private var tradeScanAt = 0L
    private var tradeScanBusy = false
    private val tradeScanTtlMs = 6 * 3600_000L
    private val _tradeScanRunning = MutableStateFlow(false)
    val tradeScanRunning: Flow<Boolean> = _tradeScanRunning.asStateFlow()

    suspend fun scanTradeIdeas(force: Boolean = false): List<TradeIdea> = withContext(Dispatchers.IO) {
        if (!force && System.currentTimeMillis() - tradeScanAt < tradeScanTtlMs && _tradeIdeas.value.isNotEmpty()) {
            return@withContext _tradeIdeas.value
        }
        if (tradeScanBusy) return@withContext _tradeIdeas.value
        tradeScanBusy = true
        _tradeScanRunning.value = true
        try {
            val universe = try {
                IndexConstituentsProvider.getConstituentsForIndex("NIFTY 50").take(50)
            } catch (_: Exception) {
                emptyList()
            }
            var posValue = 0.0
            var cash = 1000000.0
            try {
                val acct = paperDao.getAccountOnce()
                if (acct != null) cash = acct.cashBalance
                for (p in paperDao.getAllPositions().first()) posValue += p.quantity * p.currentPrice
            } catch (_: Exception) {
            }
            val equity = cash + posValue
            val niftyChg = _indices.value.find { it.symbol == "NIFTY 50" }?.percentChange ?: 0.0
            val out = mutableListOf<TradeIdea>()
            for ((i, c) in universe.withIndex()) {
                if (i > 0) delay(400)
                try {
                    val bars = yahooService.fetchCandles(c.symbol, "1y")
                    if (bars.size < 60) continue
                    val o = TechnicalAnalysisEngine.computeOutlook(c.symbol, bars, "", "Neutral", niftyChg)
                    if (o.signal != "BUY" && o.signal != "SELL" && !o.signal.startsWith("STRONG")) continue
                    val entry = bars.last().close
                    val risk = max(abs(entry - o.stopLoss), 1.0)
                    fun qty(pct: Double) = max(1, (equity * pct / 100.0 / risk).toInt())
                    val rr = if (risk > 0) abs(o.target1 - entry) / risk else 0.0
                    out.add(
                        TradeIdea(
                            symbol = c.symbol, name = c.name,
                            signal = if (o.signal.contains("BUY")) "BUY" else "SELL",
                            currentPrice = entry, entryPrice = entry,
                            stopLoss = o.stopLoss, targetPrice = o.target1, targetPrice2 = o.target2,
                            riskReward = "1:%.2f".format(rr), horizonDays = 10,
                            accuracy = 0.0, score = o.score, driftPercent = 0.0, ageDays = 0.0,
                            suggestedQty05Pct = qty(0.5), suggestedQty1Pct = qty(1.0), suggestedQty2Pct = qty(2.0),
                            reason = o.reasons.take(2).joinToString(" · ").ifBlank { "Rule-based technical setup" }
                        )
                    )
                } catch (_: Exception) {
                }
            }
            out.sortByDescending { abs(it.score) }
            _tradeIdeas.value = out
            tradeScanAt = System.currentTimeMillis()
            out
        } finally {
            tradeScanBusy = false
            _tradeScanRunning.value = false
        }
    }

    suspend fun refreshFiiDii(): List<FiiDiiFlow> = withContext(Dispatchers.IO) {
        try {
            val flows = FiiDiiService().fetchFlows()
            if (flows.isNotEmpty()) _fiiDiiData.value = flows
            flows
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Last refresh timestamp
    private val _lastRefreshTime = MutableStateFlow(System.currentTimeMillis())
    val lastRefreshTime: Flow<Long> = _lastRefreshTime.asStateFlow()

    init {
        loadInitialMarketData()
        CoroutineScope(Dispatchers.IO).launch {
            refreshMarket()
        }
    }

    private fun loadInitialMarketData() {
        // Index cards seed at zero until the first live Yahoo refresh lands.
        // The previous hardcoded quotes (fake-precise values like 24964.25)
        // were indistinguishable from live data when offline — removed.
        val initialIndices = listOf(
            MarketIndex("NIFTY 50", "NIFTY 50", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0),
            MarketIndex("SENSEX", "BSE SENSEX", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0),
            MarketIndex("BANKNIFTY", "NIFTY BANK", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0),
            MarketIndex("NIFTYIT", "NIFTY IT", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0),
            MarketIndex("NIFTYAUTO", "NIFTY AUTO", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0),
            MarketIndex("NIFTYMETAL", "NIFTY METAL", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0),
            MarketIndex("INDIAVIX", "INDIA VIX", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0)
        )
        _indices.value = initialIndices

        // Initialize with all 50 NIFTY 50 constituent stocks
        _stocks.value = IndexConstituentsProvider.nifty50Constituents.map { it.toStockQuote() }

        // IPO list starts empty: the previous demo seeds carried invented
        // subscription multiples and GMP figures. The live GMP feed
        // (refreshMarket -> gmpService.fetchLiveGmpData) fills this.
        _ipos.value = emptyList()

        // GMP board starts empty: the previous seeds carried fabricated
        // premiums labeled "Today, Live". Live refresh fills this.
        _gmpItems.value = emptyList()

        // Past listings start empty: the previous seeds showed invented
        // listing gains (up to +120%) as history. Live refresh fills this.
        _pastIpos.value = emptyList()

        val initialSectors = listOf(
            SectorHeatmapItem("NIFTY Metal", 0.0, "—"),
            SectorHeatmapItem("NIFTY IT", 0.0, "—"),
            SectorHeatmapItem("NIFTY Auto", 0.0, "—"),
            SectorHeatmapItem("NIFTY Energy", 0.0, "—"),
            SectorHeatmapItem("NIFTY FMCG", 0.0, "—"),
            SectorHeatmapItem("NIFTY Bank", 0.0, "—"),
            SectorHeatmapItem("NIFTY Pharma", 0.0, "—"),
            SectorHeatmapItem("NIFTY Realty", 0.0, "—")
        )
        _sectorHeatmap.value = initialSectors

        // FII/DII and trade ideas start EMPTY: the previous hardcoded demo
        // rows (fixed crore figures, 2024-era demo trades) are gone. Live
        // refresh fills them; empty renders as "unavailable".

        // Trade ideas are computed live by scanTradeIdeas() from real Yahoo
        // bars. The previous hardcoded 2024 demo trades are gone: showing
        // stale prices as live signals was the worst offender in this file.
        _tradeIdeas.value = emptyList()
        tradeScanAt = 0L

        // Mutual funds start empty: the previous seeds showed invented NAVs,
        // AUMs and CAGR returns (e.g. +46.2% 1Y) as live data. The mfapi.in
        // feed (refreshMarket -> mfService.fetchPopularSchemes) fills this.
        _mutualFunds.value = emptyList()

        // Health starts unmeasured: entries flip to OPERATIONAL/DEGRADED only
        // from real lookup outcomes, never from constants.
        rebuildAllotHealth()
    }

    // Real Market & IPO API Refresh
    suspend fun refreshMarket() = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()

        // 1. Fetch real stock quotes from Yahoo Finance concurrently
        val currentStocks = _stocks.value.toMutableList()
        var yahooSuccessCount = 0

        val tickersToFetch = listOf(
            Triple("RELIANCE", "Reliance Industries Ltd", "Energy"),
            Triple("TCS", "Tata Consultancy Services", "IT"),
            Triple("HDFCBANK", "HDFC Bank Ltd", "Banking"),
            Triple("BHARTIARTL", "Bharti Airtel Ltd", "Telecom"),
            Triple("ICICIBANK", "ICICI Bank Ltd", "Banking"),
            Triple("INFY", "Infosys Ltd", "IT"),
            Triple("TATAMOTORS", "Tata Motors Ltd", "Automobile"),
            Triple("ITC", "ITC Ltd", "FMCG"),
            Triple("LT", "Larsen & Toubro Ltd", "Infrastructure"),
            Triple("SBIN", "State Bank of India", "Banking"),
            Triple("MARUTI", "Maruti Suzuki India", "Automobile"),
            Triple("WIPRO", "Wipro Ltd", "IT")
        )

        val fetchedQuotes = tickersToFetch.map { (sym, name, sec) ->
            async {
                try {
                    yahooService.fetchStockQuote(sym, name, sec)
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll()

        for (quote in fetchedQuotes) {
            if (quote != null) {
                yahooSuccessCount++
                val idx = currentStocks.indexOfFirst { it.symbol == quote.symbol }
                if (idx >= 0) {
                    currentStocks[idx] = quote
                } else {
                    currentStocks.add(quote)
                }
            }
        }
        if (currentStocks.isNotEmpty()) {
            _stocks.value = currentStocks
        }

        // 2. Fetch real indices from Yahoo Finance concurrently
        val indicesToFetch = listOf(
            Triple("^NSEI", "NIFTY 50", "NIFTY 50"),
            Triple("^BSESN", "SENSEX", "BSE SENSEX"),
            Triple("^NSEBANK", "BANKNIFTY", "NIFTY BANK"),
            Triple("^CNXIT", "NIFTYIT", "NIFTY IT"),
            Triple("^CNXAUTO", "NIFTYAUTO", "NIFTY AUTO"),
            Triple("^CNXMETAL", "NIFTYMETAL", "NIFTY METAL"),
            Triple("^INDIAVIX", "INDIAVIX", "INDIA VIX")
        )

        val fetchedIndices = indicesToFetch.map { (ticker, sym, name) ->
            async {
                try {
                    yahooService.fetchMarketIndex(ticker, sym, name)
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()

        if (fetchedIndices.isNotEmpty()) {
            _indices.value = fetchedIndices
        }

        // Dynamic Sector Heatmap update based on latest live stocks
        val itStocks = currentStocks.filter { it.sector == "IT" }
        val bankStocks = currentStocks.filter { it.sector == "Banking" }
        val autoStocks = currentStocks.filter { it.sector == "Automobile" }
        val energyStocks = currentStocks.filter { it.sector == "Energy" }
        val fmcgStocks = currentStocks.filter { it.sector == "FMCG" }

        _sectorHeatmap.value = listOf(
            SectorHeatmapItem("NIFTY IT", (itStocks.map { it.percentChange }.average().takeIf { !it.isNaN() } ?: 0.0).let { (it * 100).roundToInt() / 100.0 }, itStocks.maxByOrNull { it.percentChange }?.symbol ?: "—"),
            SectorHeatmapItem("NIFTY Bank", (bankStocks.map { it.percentChange }.average().takeIf { !it.isNaN() } ?: 0.0).let { (it * 100).roundToInt() / 100.0 }, bankStocks.maxByOrNull { it.percentChange }?.symbol ?: "—"),
            SectorHeatmapItem("NIFTY Auto", (autoStocks.map { it.percentChange }.average().takeIf { !it.isNaN() } ?: 0.0).let { (it * 100).roundToInt() / 100.0 }, autoStocks.maxByOrNull { it.percentChange }?.symbol ?: "—"),
            SectorHeatmapItem("NIFTY Energy", (energyStocks.map { it.percentChange }.average().takeIf { !it.isNaN() } ?: 0.0).let { (it * 100).roundToInt() / 100.0 }, energyStocks.maxByOrNull { it.percentChange }?.symbol ?: "—"),
            SectorHeatmapItem("NIFTY FMCG", (fmcgStocks.map { it.percentChange }.average().takeIf { !it.isNaN() } ?: 0.0).let { (it * 100).roundToInt() / 100.0 }, fmcgStocks.maxByOrNull { it.percentChange }?.symbol ?: "—")
        )

        // 3. Fetch real IPOs & Live GMP from Investorgain / IPOWatch
        try {
            val (liveIpos, liveGmps) = gmpService.fetchLiveGmpData()
            if (liveIpos.isNotEmpty()) {
                _ipos.value = liveIpos
            }
            if (liveGmps.isNotEmpty()) {
                _gmpItems.value = liveGmps
            }
        } catch (e: Exception) {
            // keep existing fallback
        }

        // 4. Fetch past IPO listings
        try {
            val pastListings = gmpService.fetchPastListings()
            if (pastListings.isNotEmpty()) {
                _pastIpos.value = pastListings
            }
        } catch (e: Exception) {
            // keep existing fallback
        }

        // 5. Fetch live Mutual Fund NAVs from mfapi.in
        try {
            val liveMfs = mfService.fetchPopularSchemes()
            if (liveMfs.isNotEmpty()) {
                _mutualFunds.value = liveMfs
            }
        } catch (e: Exception) {
            // keep existing fallback
        }

        // 5b. Fetch live FII/DII flows (empty on failure — never placeholders).
        try {
            refreshFiiDii()
        } catch (e: Exception) {
            // keep existing fallback
        }

        // 6. Source health from measured outcomes (plus live Yahoo/GMP state).
        // BSE/NSE stay manual-handoff: they were previously hardcoded as
        // OPERATIONAL, which was false — both are captcha/bot-walled.
        val t1 = System.currentTimeMillis()
        rebuildAllotHealth(
            listOf(
                RegistrarSourceHealth(
                    "yahoo", "Yahoo Finance API",
                    if (yahooSuccessCount > 0) "OPERATIONAL" else "DEGRADED",
                    (t1 - t0).coerceAtLeast(0L).toInt(),
                    "Real-time quotes & OHLCV candles"
                ),
                RegistrarSourceHealth(
                    "gmp", "Live GMP Aggregator",
                    if (_gmpItems.value.isNotEmpty()) "OPERATIONAL" else "DEGRADED",
                    0, "Investorgain & IPOWatch live feed"
                )
            )
        )

        _lastRefreshTime.value = System.currentTimeMillis()
    }

    // Candlestick retrieval: cached real candles or empty (UI shows a loading
    // placeholder). Random-walk filler was removed — a chart must never show
    // invented price history as if it were market data.
    fun getCandlesForStock(symbol: String, timeframe: String = "1M"): List<CandleBar> {
        val cached = candleCache[symbol]
        if (cached != null && cached.isNotEmpty()) {
            return cached
        }
        return emptyList()
    }

    suspend fun fetchFreshCandles(symbol: String, timeframe: String = "1M"): List<CandleBar> = withContext(Dispatchers.IO) {
        val range = when (timeframe) {
            "1D" -> "1d"
            "1W" -> "5d"
            "3M" -> "3mo"
            "1Y" -> "1y"
            else -> "1mo"
        }
        val realCandles = yahooService.fetchCandles(symbol, range)
        if (realCandles.isNotEmpty()) {
            candleCache[symbol] = realCandles
            return@withContext realCandles
        }
        emptyList()
    }

    // Quantitative Outlook Engine (Axewatch multi-factor scoring model).
    // Synchronous path serves only previously computed outlooks; null means
    // "not analyzed yet" (UI shows a loading state, never a guess).
    fun getStockOutlook(symbol: String): TradeOutlook? {
        return outlookCache[symbol]
    }

    suspend fun fetchFreshOutlook(symbol: String, candles: List<CandleBar>): TradeOutlook = withContext(Dispatchers.IO) {
        val newsResult = newsService.fetchStockNews(symbol)
        val niftyChange = _indices.value.find { it.symbol == "NIFTY 50" }?.percentChange ?: 0.5
        val outlook = TechnicalAnalysisEngine.computeOutlook(
            symbol = symbol,
            candles = candles,
            newsHeadline = newsResult.headline,
            newsSentiment = newsResult.sentimentLabel,
            marketNiftyChange = niftyChange
        )
        outlookCache[symbol] = outlook
        outlook
    }

    // Real IPO Allotment Checker (Link Intime AES token & KFintech API)
    suspend fun checkIpoAllotment(pan: String, ipoSymbol: String, holderName: String = "Self"): AllotmentRecordEntity = withContext(Dispatchers.IO) {
        val cleanPan = pan.trim().uppercase()
        require(IpoAllotmentService.isValidPan(cleanPan)) { "Invalid PAN format" }
        val ipo = _ipos.value.find { it.symbol == ipoSymbol } ?: _ipos.value.firstOrNull() ?: IpoIssue(
            symbol = ipoSymbol,
            companyName = ipoSymbol,
            category = "Mainboard",
            status = "Active",
            issueOpenDate = "",
            issueCloseDate = "",
            priceBand = "",
            issuePrice = 0.0,
            lotSize = 0,
            issueSizeCr = 0.0,
            registrar = "Unknown"
        )

        // Mask PAN strictly for PII safety (AGENTS.md mandate)
        val masked = IpoAllotmentService.maskPan(cleanPan)

        // Remembered MUFG IDs let checks hit issues that rotated off the
        // live dropdown. Health flows from the service callback per attempt.
        val remembered = readIdMemory("mufg_ids")
            .mapValues { (_, v) -> v.id to v.name }
        val queryResult = allotmentService.queryAllotment(
            cleanPan, ipo.symbol, ipo.companyName, ipo.status, remembered
        )

        val sharesAllotted = queryResult.sharesAllotted
        // Applied unknown unless the registrar reported it: 0 renders "—".
        val sharesApplied = if (queryResult.sharesApplied > 0) queryResult.sharesApplied else 0
        val status = queryResult.status
        val registrar = if (queryResult.source.isNotBlank()) queryResult.source else ipo.registrar
        val appNo = if (queryResult.applicationNo.isNotBlank() && queryResult.applicationNo != "N/A") {
            queryResult.applicationNo
        } else {
            ""
        }

        val record = AllotmentRecordEntity(
            maskedPan = masked,
            ipoSymbol = ipo.symbol,
            ipoName = ipo.companyName,
            sharesApplied = sharesApplied,
            sharesAllotted = sharesAllotted,
            status = status,
            registrar = registrar,
            applicationNo = appNo
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

    suspend fun savePan(pan: String, holderName: String, relation: String = "Self"): Boolean = withContext(Dispatchers.IO) {
        val clean = pan.trim().uppercase()
        if (!IpoAllotmentService.isValidPan(clean) || holderName.isBlank()) return@withContext false
        panVaultDao.insertPan(PanVaultEntity(clean, holderName.trim(), relation))
        true
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
        for ((index, pan) in pans.withIndex()) {
            // Paced: registrars throttle bursts, and a bulk check must not
            // look like an attack. First PAN goes immediately.
            if (index > 0) delay(1500)
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
            // Applied count is unknown for hand-logged rows: 0 renders "—".
            sharesApplied = if (sharesAllotted > 0) sharesAllotted else 0,
            sharesAllotted = sharesAllotted,
            status = status,
            registrar = ipo.registrar,
            applicationNo = applicationNo
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
        RegistrarLink("BSE — Application Status", "Official BSE application verification", "https://www.bseindia.com/investors/appli_check", "Exchange"),
        RegistrarLink("NSE — Verify IPO Bids", "Official NSE bid verification portal", "https://www.nseindia.com/invest/check-trades-bids-verify-ipo-bids", "Exchange"),
        RegistrarLink("MUFG Intime (Link Intime)", "Link Intime public issues allotment portal", "https://in.mpms.mufg.com/Initial_Offer/public-issues.html", "Registrar"),
        RegistrarLink("KFintech IPO Status", "KFintech investor query & allotment portal", "https://ipostatus.kfintech.com/", "Registrar"),
        RegistrarLink("Bigshare Services", "Bigshare SME & Mainboard status (captcha)", "https://ipo.bigshareonline.com/ipo_status.html", "Registrar"),
        RegistrarLink("Skyline Financial", "Skyline RTA IPO query portal", "https://www.skylinerta.com/ipo.php", "Registrar"),
        RegistrarLink("Cameo Corporate", "Cameo India IPO status checker", "https://ipostatus.cameoindia.com/", "Registrar"),
        RegistrarLink("Maashitla Securities", "Maashitla allotment status check", "https://maashitla.com/allotment-status", "Registrar"),
        RegistrarLink("Purva Sharegistry", "Purva Sharegistry investor query", "https://www.purvashare.com/investor-service/ipo-query", "Registrar"),
        RegistrarLink("Beetal Financial", "Beetal Financial computer services", "https://www.beetalfinancial.com/", "Registrar")
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
