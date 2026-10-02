package com.aistudio.axewatch.trader.data.remote

import com.aistudio.axewatch.trader.data.model.MutualFundScheme
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class MutualFundService(private val baseUrl: String = "https://api.mfapi.in/mf/") {
    companion object {
        val POPULAR_SCHEMES = listOf(
            "122639" to ("Parag Parikh Flexi Cap Fund" to "Flexi Cap"),
            "118834" to ("Mirae Asset Large & Midcap Fund" to "Large & Mid Cap"),
            "120828" to ("Quant Small Cap Fund" to "Small Cap"),
            "118989" to ("HDFC Mid Cap Fund" to "Mid Cap"),
            "120716" to ("UTI Nifty 50 Index Fund" to "Index"),
            "120251" to ("ICICI Prudential Aggressive Hybrid Fund" to "Hybrid"),
            "119598" to ("SBI Large Cap Fund" to "Large Cap"),
            "118778" to ("Nippon India Small Cap Fund" to "Small Cap")
        )

        fun categoryFor(raw: String): String = when {
            raw.contains("index", true) -> "Index"
            raw.contains("hybrid", true) -> "Hybrid"
            raw.contains("debt", true) -> "Debt"
            raw.contains("large & mid", true) -> "Large & Mid Cap"
            raw.contains("flexi", true) -> "Flexi Cap"
            raw.contains("small cap", true) -> "Small Cap"
            raw.contains("mid cap", true) -> "Mid Cap"
            raw.contains("large cap", true) -> "Large Cap"
            else -> raw.ifBlank { "Unknown" }
        }

        /** Returns are anchored to the latest published NAV, never today's date. */
        fun parseScheme(code: String, body: String): MutualFundScheme? {
            val root = JSONObject(body)
            val meta = root.optJSONObject("meta") ?: return null
            if (meta.optString("scheme_code") != code) return null
            val arr = root.optJSONArray("data") ?: return null
            val dateFormat = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            val history = (0 until arr.length()).mapNotNull { i ->
                val row = arr.optJSONObject(i) ?: return@mapNotNull null
                val nav = row.optString("nav").toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
                    ?: return@mapNotNull null
                val date = runCatching { LocalDate.parse(row.optString("date"), dateFormat) }.getOrNull()
                    ?: return@mapNotNull null
                date to nav
            }.distinctBy { it.first }.sortedByDescending { it.first }
            val latest = history.firstOrNull() ?: return null
            val previous = history.getOrNull(1)?.second
            fun cagr(years: Long): Double? {
                val target = latest.first.minusYears(years)
                val old = history.firstOrNull { !it.first.isAfter(target) && !it.first.isBefore(target.minusDays(7)) }
                    ?: return null
                val elapsedYears = java.time.temporal.ChronoUnit.DAYS.between(old.first, latest.first) / 365.25
                return ((latest.second / old.second).pow(1.0 / elapsedYears) - 1.0) * 100
            }
            return MutualFundScheme(
                code = code, name = meta.optString("scheme_name"),
                fundHouse = meta.optString("fund_house"), category = categoryFor(meta.optString("scheme_category")),
                nav = latest.second, navPrev = previous,
                dayChangePercent = previous?.let { (latest.second / it - 1) * 100 },
                return1Yr = cagr(1), return3Yr = cagr(3), return5Yr = cagr(5),
                navDate = latest.first.format(dateFormat)
            )
        }
    }

    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()

    suspend fun fetchScheme(code: String): MutualFundScheme? = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url("$baseUrl$code").build()).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()?.let { parseScheme(code, it) }
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
    }

    /** Bounded concurrency; publish each success so one slow fund doesn't hide the others. */
    suspend fun fetchPopularSchemes(extraCodes: List<String> = emptyList(), onLoaded: (MutualFundScheme) -> Unit = {}): List<MutualFundScheme> = coroutineScope {
        val permits = Semaphore(3)
        (POPULAR_SCHEMES.map { it.first } + extraCodes).distinct().map { code -> async {
            permits.withPermit { fetchScheme(code)?.also(onLoaded) }
        } }.awaitAll().filterNotNull()
    }
}
