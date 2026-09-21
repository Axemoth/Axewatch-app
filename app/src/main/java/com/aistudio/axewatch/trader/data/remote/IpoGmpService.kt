package com.aistudio.axewatch.trader.data.remote

import android.util.Log
import com.aistudio.axewatch.trader.data.model.GmpItem
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.data.model.PastIpoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.roundToInt

class IpoGmpService {

    companion object {
        private const val TAG = "IpoGmpService"
        private const val IPOWATCH_GMP_URL = "https://ipowatch.in/ipo-grey-market-premium-latest-ipo-gmp/"
        private const val IPOWATCH_SUB_URL = "https://ipowatch.in/ipo-subscription-status-today/"
        private const val INVESTORGAIN_SUB_URL = "https://www.investorgain.com/report/ipo-subscription-live/333/all/"
        private const val INVESTORGAIN_PERF_URL = "https://www.investorgain.com/report/ipo-gmp-performance-tracker/377/all/"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

        private val COLUMN_ALIASES = listOf(
            "gmp" to listOf("ipo gmp", "gmp*", "gmp (rs)", "gmp ₹", "gmp", "premium"),
            "est_listing" to listOf("est. listing", "estimated listing", "exp listing", "listing gain", "est listing"),
            "listing_price" to listOf("listing price", "listing_price", "listing on"),
            "price" to listOf("price band", "ipo price", "issue price", "price"),
            "updated" to listOf("last updated", "updated dt", "updated"),
            "dates" to listOf("open-close", "open date", "bidding", "ipo date", "date"),
            "trend" to listOf("trend"),
            "status" to listOf("status"),
            "sub_x" to listOf("total", "subscription", "sub"),
            "name" to listOf("ipo name", "company name", "company", "ipo")
        )

        private val SUB_COLUMN_ALIASES = listOf(
            "qib" to listOf("qib (x)", "qib"),
            "nii" to listOf("nii (x)", "nii", "hni (x)", "hni"),
            "shni" to listOf("shni (x)", "shni", "snii (x)", "snii", "s-hni", "s-nii", "small hni", "bids 2l-10l"),
            "bhni" to listOf("bhni (x)", "bhni", "bnii (x)", "bnii", "b-hni", "b-nii", "big hni", "bids >10l"),
            "retail" to listOf("retail (x)", "retail", "rii (x)", "rii"),
            "total" to listOf("total (x)", "total", "overall"),
            "close_date" to listOf("closing date", "close date", "close"),
            "type" to listOf("type", "category"),
            "name" to listOf("ipo", "company name", "company", "name")
        )

        private val PERF_COLUMN_ALIASES = listOf(
            "name" to listOf("ipo", "company name", "name"),
            "symbol" to listOf("symbol", "ticker"),
            "listing_date" to listOf("listing date", "listing"),
            "sub" to listOf("sub", "subscription"),
            "price" to listOf("ipo price", "issue price", "price"),
            "listing_price" to listOf("listing price"),
            "current_price" to listOf("closing price (ltp)", "closing price", "ltp", "current price", "cmp")
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private data class ParsedTable(
        val headers: List<String>,
        val colMap: Map<String, Int>,
        val rows: List<Map<String, String>>
    )

    private data class LiveSubInfo(
        val qib: Double,
        val nii: Double,
        val shni: Double = 0.0,
        val bhni: Double = 0.0,
        val retail: Double,
        val total: Double,
        val closeDate: String
    )

    /**
     * Fetches real-time Mainboard & SME IPOs with accurate live GMP, issue pricing, and subscriptions.
     */
    suspend fun fetchLiveGmpData(): Pair<List<IpoIssue>, List<GmpItem>> = withContext(Dispatchers.IO) {
        try {
            val (ipos, gmps) = fetchFromIpowatchGmp()
            if (ipos.isNotEmpty()) {
                Log.d(TAG, "Successfully fetched ${ipos.size} IPOs and ${gmps.size} GMP records from IPOWatch")
                return@withContext Pair(ipos, gmps)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching live GMP from IPOWatch: ${e.message}")
        }

        // Live fetch failed: return empty, never stale demo data. The old
        // getSeedIpos()/getSeedGmps()/getSeedPastListings() served invented
        // September-2026 figures (subs, GMPs labeled "Today, Live", listing
        // gains) as if live. Empty renders as unavailable.
        Pair(emptyList(), emptyList())
    }

    /**
     * Fetches past IPO listing performances with issue price, listing price, and listing day gain.
     */
    suspend fun fetchPastListings(): List<PastIpoItem> = withContext(Dispatchers.IO) {
        try {
            val list = fetchPastListingsFromInvestorGain()
            if (list.isNotEmpty()) {
                Log.d(TAG, "Successfully fetched ${list.size} past listings from InvestorGain")
                return@withContext list
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching past listings from InvestorGain: ${e.message}")
        }

        try {
            val list = fetchPastListingsFromIpowatch()
            if (list.isNotEmpty()) {
                Log.d(TAG, "Successfully fetched ${list.size} past listings from IPOWatch")
                return@withContext list
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching past listings from IPOWatch: ${e.message}")
        }

        // Live fetch failed: empty, never demo seeds (see above).
        emptyList()
    }

    private fun fetchFromIpowatchGmp(): Pair<List<IpoIssue>, List<GmpItem>> {
        val request = Request.Builder()
            .url(IPOWATCH_GMP_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return Pair(emptyList(), emptyList())

        val html = response.body?.string() ?: return Pair(emptyList(), emptyList())
        val tables = parseTables(html, COLUMN_ALIASES)
        if (tables.isEmpty()) return Pair(emptyList(), emptyList())

        // Fetch live subscriptions to enrich active IPOs
        val subMap = try {
            fetchLiveSubscriptions()
        } catch (e: Exception) {
            Log.w(TAG, "Failed fetching subscription sub-table: ${e.message}")
            emptyMap()
        }

        val allIpos = mutableListOf<IpoIssue>()
        val allGmps = mutableListOf<GmpItem>()

        // Table 0 = Mainboard, Table 1 = SME
        for ((tIdx, table) in tables.withIndex()) {
            if (tIdx > 1) break // Table 2 is past performance
            val category = if (tIdx == 0) "Mainboard" else "SME"

            for (row in table.rows) {
                val rawName = row["name"] ?: continue
                val cleanName = cleanCompanyName(rawName)
                if (cleanName.isBlank() || isHeaderNoise(cleanName)) continue

                val rawPrice = row["price"] ?: "0"
                val issuePrice = parseNumber(rawPrice)

                val rawGmp = row["gmp"] ?: "0"
                val gmpAmount = parseNumber(rawGmp)

                val estListingRaw = row["est_listing"] ?: ""
                var estListingPrice = parseNumber(estListingRaw)
                if (estListingPrice <= 0.0 && issuePrice > 0) {
                    estListingPrice = issuePrice + gmpAmount
                }

                val gmpPercent = if (issuePrice > 0) {
                    ((gmpAmount / issuePrice) * 100.0).roundToOneDecimal()
                } else {
                    0.0
                }

                val rawDates = row["dates"] ?: ""
                val (openDate, closeDate) = parseDates(rawDates)

                val rawStatus = row["status"] ?: "Upcoming"
                val mappedStatus = mapStatus(rawStatus)

                val symbol = generateSymbol(cleanName)
                val isSme = category == "SME" || cleanName.contains("SME", ignoreCase = true)

        // Subscription figures come ONLY from the live sub-table. The old
        // code invented qib=3.4/nii=2.8/rii=2.1 for Active IPOs with no
        // match, and derived SHNI/BHNI as fixed fractions of NII — fake
        // precision on investment-driving numbers. Zero renders as "—".
        // (Real SHNI/BHNI splits come from NSE bidDetails, unavailable here.)
        val matchedSub = findSubscription(cleanName, subMap)
        val qib = matchedSub?.qib ?: 0.0
        val nii = matchedSub?.nii ?: 0.0
        val shni = matchedSub?.shni ?: 0.0
        val bhni = matchedSub?.bhni ?: 0.0
        val rii = matchedSub?.retail ?: 0.0
        val total = matchedSub?.total ?: 0.0
        val finalCloseDate = if (matchedSub?.closeDate?.isNotBlank() == true) matchedSub.closeDate else closeDate

        // Lot size / issue size / registrar are UNKNOWN from the GMP page.
        // Old code derived lot size from price by formula, hashed a fake
        // issue size, and keyword-guessed the registrar ("energy"→Bigshare)
        // — the guessed registrar then misrouted allotment checks. The
        // registrar directory enriches these after fetch; until then they
        // render as unknown, never as invented facts.
        val lotSize = 0
        val issueSizeCr = 0.0
        val registrar = "Unknown"

                val priceBand = if (issuePrice > 0) {
                    val lowerBand = (issuePrice * 0.95).toInt()
                    if (lowerBand > 0 && lowerBand < issuePrice.toInt()) "₹$lowerBand - ₹${issuePrice.toInt()}" else "₹${issuePrice.toInt()}"
                } else {
                    "₹TBA"
                }

                val ipo = IpoIssue(
                    symbol = symbol,
                    companyName = cleanName,
                    category = if (isSme) "SME" else "Mainboard",
                    status = mappedStatus,
                    issueOpenDate = openDate,
                    issueCloseDate = finalCloseDate,
                    priceBand = priceBand,
                    issuePrice = issuePrice,
                    lotSize = lotSize,
                    issueSizeCr = issueSizeCr,
                    registrar = registrar,
                    qibSub = qib,
                    niiSub = nii,
                    shniSub = shni,
                    bhniSub = bhni,
                    riiSub = rii,
                    totalSub = total,
                    gmpAmount = gmpAmount,
                    gmpPercent = gmpPercent,
                    estListingPrice = estListingPrice
                )
                allIpos.add(ipo)

                val fireRating = when {
                    gmpPercent >= 50.0 -> 5
                    gmpPercent >= 25.0 -> 4
                    gmpPercent >= 10.0 -> 3
                    gmpPercent > 0.0 -> 2
                    else -> 1
                }

                val gmpItem = GmpItem(
                    companyName = cleanName,
                    symbol = symbol,
                    issuePrice = issuePrice,
                    gmpAmount = gmpAmount,
                    gmpPercent = gmpPercent,
                    estListingPrice = estListingPrice,
                    status = if (mappedStatus == "Active") "Open" else mappedStatus,
                    fireRating = fireRating,
                    lastUpdated = row["updated"]?.ifBlank { "Live" } ?: "Live"
                )
                allGmps.add(gmpItem)
            }
        }

        return Pair(allIpos, allGmps)
    }

    private fun fetchLiveSubscriptions(): Map<String, LiveSubInfo> {
        // Try InvestorGain first for granular SHNI / BHNI / NII / QIB / Retail breakdowns
        try {
            val igSubs = fetchLiveSubscriptionsFromInvestorGain()
            if (igSubs.isNotEmpty()) {
                Log.d(TAG, "Successfully fetched ${igSubs.size} live subscriptions from InvestorGain")
                return igSubs
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching live subscriptions from InvestorGain: ${e.message}")
        }

        // Resilient fallback to IPOWatch
        return fetchLiveSubscriptionsFromIpowatch()
    }

    private fun fetchLiveSubscriptionsFromInvestorGain(): Map<String, LiveSubInfo> {
        val request = Request.Builder()
            .url(INVESTORGAIN_SUB_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyMap()

        val html = response.body?.string() ?: return emptyMap()
        val tables = parseTables(html, SUB_COLUMN_ALIASES)
        if (tables.isEmpty()) return emptyMap()

        val subMap = mutableMapOf<String, LiveSubInfo>()
        for (table in tables) {
            for (row in table.rows) {
                val rawName = row["name"] ?: continue
                val clean = normalizeKey(cleanCompanyName(rawName))
                if (clean.isBlank()) continue

                val qib = parseNumber(row["qib"] ?: "0")
                val nii = parseNumber(row["nii"] ?: "0")
                val shni = parseNumber(row["shni"] ?: "0")
                val bhni = parseNumber(row["bhni"] ?: "0")
                val retail = parseNumber(row["retail"] ?: "0")
                val total = parseNumber(row["total"] ?: "0")
                val closeDate = row["close_date"] ?: ""

                subMap[clean] = LiveSubInfo(
                    qib = qib,
                    nii = nii,
                    shni = shni,
                    bhni = bhni,
                    retail = retail,
                    total = total,
                    closeDate = closeDate
                )
            }
        }
        return subMap
    }

    private fun fetchLiveSubscriptionsFromIpowatch(): Map<String, LiveSubInfo> {
        val request = Request.Builder()
            .url(IPOWATCH_SUB_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyMap()

        val html = response.body?.string() ?: return emptyMap()
        val tables = parseTables(html, SUB_COLUMN_ALIASES)
        if (tables.isEmpty()) return emptyMap()

        val subMap = mutableMapOf<String, LiveSubInfo>()
        for (table in tables) {
            for (row in table.rows) {
                val rawName = row["name"] ?: continue
                val clean = normalizeKey(cleanCompanyName(rawName))
                if (clean.isBlank()) continue

                val qib = parseNumber(row["qib"] ?: "0")
                val nii = parseNumber(row["nii"] ?: "0")
                val shni = parseNumber(row["shni"] ?: "0")
                val bhni = parseNumber(row["bhni"] ?: "0")
                val retail = parseNumber(row["retail"] ?: "0")
                val total = parseNumber(row["total"] ?: "0")
                val closeDate = row["close_date"] ?: ""

                subMap[clean] = LiveSubInfo(qib, nii, shni, bhni, retail, total, closeDate)
            }
        }
        return subMap
    }

    private fun fetchPastListingsFromInvestorGain(): List<PastIpoItem> {
        val request = Request.Builder()
            .url(INVESTORGAIN_PERF_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val html = response.body?.string() ?: return emptyList()
        val tables = parseTables(html, PERF_COLUMN_ALIASES)
        if (tables.isEmpty()) return emptyList()

        val pastList = mutableListOf<PastIpoItem>()
        for (table in tables) {
            for (row in table.rows.take(60)) {
                val rawName = row["name"] ?: continue
                val cleanName = cleanCompanyName(rawName)
                if (cleanName.isBlank() || isHeaderNoise(cleanName)) continue

                val issuePx = parseNumber(row["price"] ?: "0")
                if (issuePx <= 0.0) continue

                val listingPx = parseNumber(row["listing_price"] ?: "0")
                val curPx = parseNumber(row["current_price"] ?: "0")
                val sub = parseNumber(row["sub"] ?: "0")
                val listingDate = row["listing_date"] ?: ""

                val rawSym = row["symbol"] ?: ""
                val sym = rawSym.split(",").firstOrNull()?.trim()
                    ?.takeIf { it.isNotBlank() } ?: generateSymbol(cleanName)

                val listingGainPct = if (issuePx > 0.0 && listingPx > 0.0) {
                    (((listingPx - issuePx) / issuePx) * 100.0).roundToOneDecimal()
                } else {
                    0.0
                }

                val curGainPct = if (issuePx > 0.0 && curPx > 0.0) {
                    (((curPx - issuePx) / issuePx) * 100.0).roundToOneDecimal()
                } else {
                    listingGainPct
                }

                pastList.add(
                    PastIpoItem(
                        symbol = sym,
                        companyName = cleanName,
                        issuePrice = issuePx,
                        listingPrice = if (listingPx > 0.0) listingPx else issuePx,
                        currentPrice = if (curPx > 0.0) curPx else (if (listingPx > 0.0) listingPx else issuePx),
                        listingGainPercent = listingGainPct,
                        currentGainPercent = curGainPct,
                        totalSub = sub,
                        listingDate = listingDate
                    )
                )
            }
        }
        return pastList
    }

    private fun fetchPastListingsFromIpowatch(): List<PastIpoItem> {
        val request = Request.Builder()
            .url(IPOWATCH_GMP_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val html = response.body?.string() ?: return emptyList()
        val tables = parseTables(html, COLUMN_ALIASES)
        if (tables.size < 3) return emptyList()

        // Table 2 is typically the Past Listings table: Name, Issue Price, GMP, Listing Price
        val pastTable = tables[2]
        val pastList = mutableListOf<PastIpoItem>()

        for (row in pastTable.rows.take(30)) {
            val rawName = row["name"] ?: continue
            val cleanName = cleanCompanyName(rawName)
            if (cleanName.isBlank() || isHeaderNoise(cleanName)) continue

            val issuePx = parseNumber(row["price"] ?: "0")
            if (issuePx <= 0.0) continue

            val gmp = parseNumber(row["gmp"] ?: "0")
            var listingPx = parseNumber(row["listing_price"] ?: "0")
            if (listingPx <= 0.0) {
                listingPx = issuePx + gmp
            }

            val gainPct = if (issuePx > 0) {
                (((listingPx - issuePx) / issuePx) * 100.0).roundToOneDecimal()
            } else {
                0.0
            }

            pastList.add(
                PastIpoItem(
                    symbol = generateSymbol(cleanName),
                    companyName = cleanName,
                    issuePrice = issuePx,
                    listingPrice = listingPx,
                    currentPrice = listingPx,
                    listingGainPercent = gainPct,
                    currentGainPercent = gainPct,
                    totalSub = 0.0,
                    listingDate = ""
                )
            )
        }

        return pastList
    }

    private fun parseTables(html: String, aliases: List<Pair<String, List<String>>>): List<ParsedTable> {
        val tablePattern = Pattern.compile("<table[^>]*>(.*?)</table>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val cellPattern = Pattern.compile("<t[hd][^>]*>(.*?)</t[hd]>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)

        val result = mutableListOf<ParsedTable>()
        val tableMatcher = tablePattern.matcher(html)

        while (tableMatcher.find()) {
            val tableHtml = tableMatcher.group(1) ?: continue
            val rowMatcher = rowPattern.matcher(tableHtml)
            val allRows = mutableListOf<List<String>>()

            while (rowMatcher.find()) {
                val rowHtml = rowMatcher.group(1) ?: continue
                val cellMatcher = cellPattern.matcher(rowHtml)
                val cells = mutableListOf<String>()
                while (cellMatcher.find()) {
                    cells.add(cleanHtml(cellMatcher.group(1) ?: ""))
                }
                if (cells.isNotEmpty() && cells.any { it.isNotBlank() }) {
                    allRows.add(cells)
                }
            }

            if (allRows.isEmpty()) continue

            val headerRow = allRows[0]
            val colMap = mutableMapOf<String, Int>()

            for ((idx, h) in headerRow.withIndex()) {
                val canon = matchColumnAlias(h, aliases)
                if (canon != null && !colMap.containsKey(canon)) {
                    colMap[canon] = idx
                }
            }

            val dataRows = mutableListOf<Map<String, String>>()
            for (r in allRows.drop(1)) {
                val map = mutableMapOf<String, String>()
                for ((canon, idx) in colMap) {
                    if (idx < r.size) {
                        map[canon] = r[idx]
                    }
                }
                if (map.isNotEmpty() && (map["name"]?.isNotBlank() == true)) {
                    dataRows.add(map)
                }
            }

            if (dataRows.isNotEmpty()) {
                result.add(ParsedTable(headerRow, colMap, dataRows))
            }
        }
        return result
    }

    private fun matchColumnAlias(header: String, aliases: List<Pair<String, List<String>>>): String? {
        val hClean = header.lowercase()
            .replace(Regex("[^a-z0-9% ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        for ((canon, aliasList) in aliases) {
            for (a in aliasList) {
                val aClean = a.lowercase()
                    .replace(Regex("[^a-z0-9% ]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                if (hClean == aClean || hClean.startsWith("$aClean ") || hClean.endsWith(" $aClean") || hClean.contains(" $aClean ")) {
                    return canon
                }
            }
        }
        return null
    }

    private fun cleanHtml(raw: String): String {
        return raw.replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("&#8377;", "₹")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanCompanyName(raw: String): String {
        var text = raw.replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace(Regex("(?i)\\b(Apply IPO|View Review|Details|RHP|DRHP)\\b"), "")
            .trim()

        val split = text.split(Regex("(?i)\\b(?:BSE SME|NSE SME|BSE|NSE|GMP:)\\b"))
        if (split.isNotEmpty()) {
            text = split[0]
        }
        return text.replace(Regex("(?i)SME$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isHeaderNoise(text: String): Boolean {
        val l = text.lowercase()
        return l.contains("ipo name") || l.contains("company name") || l.contains("upcoming ipo")
    }

    private fun parseNumber(text: String): Double {
        val clean = text.replace(",", "").replace("₹", "").replace("%", "").trim()
        val m = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(clean)
        return if (m.find()) m.group().toDoubleOrNull() ?: 0.0 else 0.0
    }

    private fun parseDates(rawDates: String): Pair<String, String> {
        val clean = rawDates.trim()
        if (clean.isBlank() || clean.equals("-", ignoreCase = true)) {
            return Pair("—", "—")
        }
        val parts = clean.split("-")
        return if (parts.size >= 2) {
            var start = parts[0].trim()
            val end = parts[1].trim()
            val monthMatch = Regex("[a-zA-Z]+(?:\\s+\\d{4})?").find(end)
            if (Regex("^\\d{1,2}$").matches(start) && monthMatch != null) {
                start = "$start ${monthMatch.value}"
            }
            Pair(start, end)
        } else {
            Pair(clean, clean)
        }
    }

    private fun mapStatus(rawStatus: String): String {
        val l = rawStatus.lowercase()
        return when {
            l.contains("open") -> "Active"
            l.contains("upcoming") || l.contains("forthcom") || l.contains("pre") -> "Forthcoming"
            l.contains("close") -> "Closed"
            l.contains("allot") -> "Allotted"
            l.contains("list") -> "Listed"
            else -> "Active"
        }
    }




    // REMOVED (audit): calculateLotSize / estimateIssueSize derived lot
    // size from price by formula, hashed a fake issue size, and
    // keyword-guessed the registrar — the guessed registrar then
    // misrouted allotment checks. Unknowns stay 0/"Unknown" until the
    // registrar directory enriches them. Demo seed tables removed too:
    // fetch failures now return empty (unavailable), never stale data.

    private fun generateSymbol(name: String): String {
        return name.uppercase()
            .replace(Regex("[^A-Z0-9]"), "")
            .take(9)
            .ifBlank { "IPO" }
    }

    private fun normalizeKey(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .replace("ltd", "")
            .replace("limited", "")
            .replace("india", "")
            .replace("sme", "")
            .replace("ipo", "")
    }

    private fun findSubscription(companyName: String, subMap: Map<String, LiveSubInfo>): LiveSubInfo? {
        val norm = normalizeKey(companyName)
        if (norm.isEmpty()) return null
        subMap[norm]?.let { return it }
        // Guarded substring fallback (>=10 shared chars): unguarded
        // contains() collides ("Hero Motors" vs "Motors"), attributing one
        // IPO's live subscription to another.
        for ((k, v) in subMap) {
            if (k.length >= 10 && norm.length >= 10 && (norm.contains(k) || k.contains(norm))) {
                return v
            }
        }
        return null
    }

    private fun Double.roundToOneDecimal(): Double {
        return (this * 10.0).roundToInt() / 10.0
    }

}
