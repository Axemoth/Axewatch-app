package com.example.data.remote

import android.util.Log
import com.example.data.model.CandleBar
import com.example.data.model.MarketIndex
import com.example.data.model.StockQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

class YahooFinanceService {

    companion object {
        private const val TAG = "YahooFinanceService"
        private const val BASE_CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/"
        private const val SEARCH_URL = "https://query1.finance.yahoo.com/v1/finance/search"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)

    suspend fun fetchStockQuote(
        symbol: String,
        companyName: String = "",
        sector: String = "General"
    ): StockQuote? = withContext(Dispatchers.IO) {
        try {
            val ticker = if (symbol.startsWith("^")) symbol else "$symbol.NS"
            val url = "$BASE_CHART_URL$ticker?range=1mo&interval=1d"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Yahoo quote $ticker failed HTTP ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val chart = root.optJSONObject("chart") ?: return@withContext null
            val resultArr = chart.optJSONArray("result") ?: return@withContext null
            if (resultArr.length() == 0) return@withContext null

            val resultObj = resultArr.getJSONObject(0)
            val meta = resultObj.optJSONObject("meta") ?: return@withContext null
            val curPrice = meta.optDouble("regularMarketPrice", 0.0)
            if (curPrice <= 0.0) return@withContext null

            val indicators = resultObj.optJSONObject("indicators")
            val quoteArr = indicators?.optJSONArray("quote")
            val quoteObj = quoteArr?.optJSONObject(0)
            val closeArr = quoteObj?.optJSONArray("close")

            var prevClose = meta.optDouble("chartPreviousClose", curPrice)
            if (closeArr != null && closeArr.length() >= 2) {
                for (i in closeArr.length() - 2 downTo 0) {
                    if (!closeArr.isNull(i)) {
                        val c = closeArr.optDouble(i, 0.0)
                        if (c > 0) {
                            prevClose = c
                            break
                        }
                    }
                }
            }

            val change = curPrice - prevClose
            val pctChange = if (prevClose > 0) (change / prevClose) * 100.0 else 0.0
            val dayHigh = meta.optDouble("regularMarketDayHigh", curPrice)
            val dayLow = meta.optDouble("regularMarketDayLow", curPrice)
            val high52 = meta.optDouble("fiftyTwoWeekHigh", dayHigh)
            val low52 = meta.optDouble("fiftyTwoWeekLow", dayLow)
            val volRaw = meta.optLong("regularMarketVolume", 1000000L)
            val volStr = formatVolume(volRaw)

            val cleanName = if (companyName.isNotBlank()) companyName else symbol

            StockQuote(
                symbol = symbol.removeSuffix(".NS"),
                name = cleanName,
                lastPrice = (curPrice * 100.0).roundToInt() / 100.0,
                change = (change * 100.0).roundToInt() / 100.0,
                percentChange = (pctChange * 100.0).roundToInt() / 100.0,
                dayHigh = (dayHigh * 100.0).roundToInt() / 100.0,
                dayLow = (dayLow * 100.0).roundToInt() / 100.0,
                volume = volStr,
                sector = sector,
                week52High = (high52 * 100.0).roundToInt() / 100.0,
                week52Low = (low52 * 100.0).roundToInt() / 100.0,
                peRatio = 24.5,
                marketCapCr = (curPrice * 120.0).roundToInt().toDouble()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching Yahoo quote for $symbol: ${e.message}")
            null
        }
    }

    suspend fun fetchMarketIndex(
        ticker: String,
        symbolName: String,
        displayName: String
    ): MarketIndex? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_CHART_URL$ticker?range=1mo&interval=1d"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val resultArr = root.optJSONObject("chart")?.optJSONArray("result") ?: return@withContext null
            if (resultArr.length() == 0) return@withContext null

            val resultObj = resultArr.getJSONObject(0)
            val meta = resultObj.optJSONObject("meta") ?: return@withContext null
            val curPrice = meta.optDouble("regularMarketPrice", 0.0)
            if (curPrice <= 0.0) return@withContext null

            val closeArr = resultObj.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0)?.optJSONArray("close")
            var prevClose = meta.optDouble("chartPreviousClose", curPrice)
            if (closeArr != null && closeArr.length() >= 2) {
                for (i in closeArr.length() - 2 downTo 0) {
                    if (!closeArr.isNull(i)) {
                        val c = closeArr.optDouble(i, 0.0)
                        if (c > 0) {
                            prevClose = c
                            break
                        }
                    }
                }
            }

            val change = curPrice - prevClose
            val pctChange = if (prevClose > 0) (change / prevClose) * 100.0 else 0.0
            val dayHigh = meta.optDouble("regularMarketDayHigh", curPrice)
            val dayLow = meta.optDouble("regularMarketDayLow", curPrice)
            val openPrice = meta.optDouble("regularMarketDayOpen", prevClose)

            MarketIndex(
                symbol = symbolName,
                name = displayName,
                lastPrice = (curPrice * 100.0).roundToInt() / 100.0,
                change = (change * 100.0).roundToInt() / 100.0,
                percentChange = (pctChange * 100.0).roundToInt() / 100.0,
                open = (openPrice * 100.0).roundToInt() / 100.0,
                high = (dayHigh * 100.0).roundToInt() / 100.0,
                low = (dayLow * 100.0).roundToInt() / 100.0,
                advances = if (change >= 0) 32 else 18,
                declines = if (change >= 0) 18 else 32
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching index $ticker: ${e.message}")
            null
        }
    }

    suspend fun fetchCandles(
        symbol: String,
        range: String = "3mo"
    ): List<CandleBar> = withContext(Dispatchers.IO) {
        try {
            val ticker = if (symbol.startsWith("^")) symbol else "$symbol.NS"
            val url = "$BASE_CHART_URL$ticker?range=$range&interval=1d"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(body)
            val resultArr = root.optJSONObject("chart")?.optJSONArray("result") ?: return@withContext emptyList()
            if (resultArr.length() == 0) return@withContext emptyList()

            val resultObj = resultArr.getJSONObject(0)
            val timestamps = resultObj.optJSONArray("timestamp") ?: return@withContext emptyList()
            val quoteObj = resultObj.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0) ?: return@withContext emptyList()

            val openArr = quoteObj.optJSONArray("open") ?: return@withContext emptyList()
            val highArr = quoteObj.optJSONArray("high") ?: return@withContext emptyList()
            val lowArr = quoteObj.optJSONArray("low") ?: return@withContext emptyList()
            val closeArr = quoteObj.optJSONArray("close") ?: return@withContext emptyList()

            val candles = mutableListOf<CandleBar>()
            val len = timestamps.length()
            for (i in 0 until len) {
                if (openArr.isNull(i) || highArr.isNull(i) || lowArr.isNull(i) || closeArr.isNull(i)) continue
                val o = openArr.optDouble(i, 0.0)
                val h = highArr.optDouble(i, 0.0)
                val l = lowArr.optDouble(i, 0.0)
                val c = closeArr.optDouble(i, 0.0)
                val ts = timestamps.optLong(i, 0L)
                if (c <= 0.0 || ts == 0L) continue

                val dateLabel = dateFormat.format(Date(ts * 1000L))
                candles.add(
                    CandleBar(
                        dateLabel = dateLabel,
                        open = (o * 10.0).roundToInt() / 10.0,
                        high = (h * 10.0).roundToInt() / 10.0,
                        low = (l * 10.0).roundToInt() / 10.0,
                        close = (c * 10.0).roundToInt() / 10.0
                    )
                )
            }

            // Compute running 5-period SMA
            val window = 5
            candles.mapIndexed { idx, bar ->
                val startIdx = max(0, idx - window + 1)
                val sub = candles.subList(startIdx, idx + 1)
                val avg = sub.map { it.close }.average()
                bar.copy(sma = (avg * 10.0).roundToInt() / 10.0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching Yahoo candles for $symbol: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchSymbols(query: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val q = query.trim().uppercase()
        if (q.isBlank()) return@withContext emptyList()

        try {
            val url = "$SEARCH_URL?q=$q&quotesCount=16&newsCount=0"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(body)
            val quotes = root.optJSONArray("quotes") ?: return@withContext emptyList()

            val results = mutableListOf<Pair<String, String>>()
            val seen = mutableSetOf<String>()
            for (i in 0 until quotes.length()) {
                val item = quotes.getJSONObject(i)
                val sym = item.optString("symbol", "")
                if (!sym.endsWith(".NS")) continue
                val baseSym = sym.removeSuffix(".NS")
                if (seen.add(baseSym)) {
                    val name = item.optString("shortname", item.optString("longname", baseSym))
                    results.add(Pair(baseSym, name))
                }
            }
            results
        } catch (e: Exception) {
            Log.w(TAG, "Search failed for $query: ${e.message}")
            emptyList()
        }
    }

    private fun formatVolume(vol: Long): String {
        return when {
            vol >= 10000000 -> "${(vol / 1000000.0).roundToInt()}M"
            vol >= 1000000 -> "${"%.1f".format(vol / 1000000.0)}M"
            vol >= 1000 -> "${(vol / 1000.0).roundToInt()}K"
            else -> vol.toString()
        }
    }
}
