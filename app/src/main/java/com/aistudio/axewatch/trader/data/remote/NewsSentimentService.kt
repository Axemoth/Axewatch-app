package com.aistudio.axewatch.trader.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class NewsItem(
    val title: String,
    val link: String,
    val published: String,
    val source: String,
    val isNegative: Boolean = false,
    val isPositive: Boolean = false,
    val publishedAtMillis: Long = 0L
)

data class NewsAnalysisResult(
    val headline: String,
    val sentimentScore: Double, // -100 to +100
    val sentimentLabel: String,
    val items: List<NewsItem>,
    val redFlags: List<String>
)

class NewsSentimentService {

    companion object {
        private const val TAG = "NewsSentimentService"
        private const val BASE_RSS = "https://news.google.com/rss/search"
        private const val RBI_PRESS = "https://rbi.org.in/pressreleases_rss.xml"
        private const val RBI_NOTIFICATIONS = "https://rbi.org.in/notifications_rss.xml"
        private const val SEBI_RSS = "https://www.sebi.gov.in/sebirss.xml"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

        private val NEG_PATTERN = Pattern.compile(
            "resign|stepped down|fired|sacked|probe|investigat|fraud|scam|cheat|loss|weak results|misses|profit fell|drop|slump|plunge|crash|tank|default|bankrupt|downgrade|layoff|sebi|penalty|fine|court|pledge",
            Pattern.CASE_INSENSITIVE
        )

        private val POS_PATTERN = Pattern.compile(
            "surge|soar|jump|rally|record high|record profit|beats|rises|strong results|upgrade|order win|contract|expansion|acquisition|dividend|bonus|buyback",
            Pattern.CASE_INSENSITIVE
        )
        private val MARKET_PATTERN = Pattern.compile(
            "monetary policy|policy rate|repo rate|inflation|liquidity|securities|stock market|equity|derivative|margin|foreign exchange|rupee|tariff|trade policy|tax|budget|banking|investment|fpi|fii|sebi board|regulation|market crash|market rally",
            Pattern.CASE_INSENSITIVE
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun fetchStockNews(symbol: String, limit: Int = 8): NewsAnalysisResult = withContext(Dispatchers.IO) {
        val sym = symbol.trim().uppercase()
        try {
            val query = URLEncoder.encode("$sym NSE stock when:7d", "UTF-8")
            val url = "$BASE_RSS?q=$query&hl=en-IN&gl=IN&ceid=IN:en"
            val items = fetchFeed(url, limit).filter(::isRecent)

            if (items.isEmpty()) return@withContext fallbackAnalysis()

            var posCount = 0
            var negCount = 0
            val redFlags = mutableListOf<String>()

            val analyzedItems = items.map { item ->
                val isNeg = NEG_PATTERN.matcher(item.title).find()
                val isPos = POS_PATTERN.matcher(item.title).find()
                if (isNeg) {
                    negCount++
                    redFlags.add(item.title)
                }
                if (isPos) {
                    posCount++
                }
                item.copy(isNegative = isNeg, isPositive = isPos)
            }

            val rawScore = (posCount - negCount * 1.5) * 25.0
            val clampedScore = rawScore.coerceIn(-100.0, 100.0)
            val sentimentLabel = when {
                clampedScore >= 20.0 -> "Bullish"
                clampedScore <= -20.0 -> "Cautious"
                else -> "Neutral"
            }

            val bestHeadline = items.first().title

            NewsAnalysisResult(
                headline = bestHeadline,
                sentimentScore = clampedScore,
                sentimentLabel = sentimentLabel,
                items = analyzedItems,
                redFlags = redFlags
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch news for $sym: ${e.message}")
            fallbackAnalysis()
        }
    }

    /** Recent policy and market headlines, with direct source links and dates.
     *  The keyword filter is editorial relevance, never a claim of market impact.
     */
    suspend fun fetchMarketNews(limit: Int = 10): List<NewsItem> = coroutineScope {
        val feeds = listOf(
            RBI_PRESS to "RBI", RBI_NOTIFICATIONS to "RBI", SEBI_RSS to "SEBI",
            "$BASE_RSS?q=${URLEncoder.encode("India stocks RBI SEBI budget tariff policy when:7d", "UTF-8")}&hl=en-IN&gl=IN&ceid=IN:en" to ""
        )
        feeds.map { (url, source) -> async(Dispatchers.IO) { fetchFeed(url, 30, source) } }
            .flatMap { it.await() }
            .filter { isRecent(it) && MARKET_PATTERN.matcher(it.title).find() }
            .distinctBy { it.title.lowercase().replace(Regex("[^a-z0-9]+"), "") }
            .sortedWith(compareByDescending<NewsItem> { marketRelevance(it.title) }
                .thenByDescending { it.publishedAtMillis })
            .take(limit)
    }

    private fun marketRelevance(title: String): Int {
        val t = title.lowercase()
        return when {
            listOf("monetary policy", "repo rate", "sebi board", "budget", "tariff").any(t::contains) -> 3
            listOf("regulation", "derivative", "inflation", "tax", "foreign exchange").any(t::contains) -> 2
            else -> 1
        }
    }

    private fun isRecent(item: NewsItem): Boolean = item.publishedAtMillis > 0 &&
        System.currentTimeMillis() - item.publishedAtMillis in 0L..7L * 24 * 60 * 60 * 1000

    private fun fetchFeed(url: String, limit: Int, source: String = ""): List<NewsItem> = try {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) emptyList()
            else response.body?.string()?.let { parseRssItems(it, limit, source) } ?: emptyList()
        }
    } catch (e: Exception) {
        Log.w(TAG, "News feed unavailable: ${e.javaClass.simpleName}")
        emptyList()
    }

    internal fun parseRssItems(xml: String, limit: Int, sourceOverride: String = ""): List<NewsItem> {
        val list = mutableListOf<NewsItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false
            var currentTag = ""
            var title = ""
            var link = ""
            var pubDate = ""
            var source = ""

            while (eventType != XmlPullParser.END_DOCUMENT && list.size < limit) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("item", ignoreCase = true)) {
                            inItem = true
                            title = ""
                            link = ""
                            pubDate = ""
                            source = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inItem) {
                            val text = parser.text.trim()
                            when (currentTag.lowercase()) {
                                "title" -> title = text
                                "link" -> link = text
                                "pubdate" -> pubDate = text
                                "source" -> source = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("item", ignoreCase = true)) {
                            inItem = false
                            if (title.isNotBlank()) {
                                val cleanTitle = if (source.isNotBlank() && title.endsWith(" - $source")) {
                                    title.removeSuffix(" - $source").trim()
                                } else title
                                list.add(NewsItem(cleanTitle, link, pubDate,
                                    sourceOverride.ifBlank { source.ifBlank { "Media" } },
                                    publishedAtMillis = parseNewsDate(pubDate)))
                            }
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "RSS parse error: ${e.message}")
        }
        return list
    }

    internal fun parseNewsDate(value: String): Long {
        for (pattern in listOf("EEE, dd MMM yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss",
            "dd MMM, yyyy Z", "dd MMM yyyy HH:mm:ss z")) {
            try {
                return SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }.parse(value.trim())?.time ?: 0L
            } catch (_: Exception) { }
        }
        return 0L
    }

    private fun fallbackAnalysis(): NewsAnalysisResult {
        return NewsAnalysisResult(
            headline = "",
            sentimentScore = 0.0,
            sentimentLabel = "Neutral",
            items = emptyList(),
            redFlags = emptyList()
        )
    }
}
