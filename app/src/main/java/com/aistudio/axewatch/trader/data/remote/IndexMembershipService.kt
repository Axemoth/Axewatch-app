package com.aistudio.axewatch.trader.data.remote

import com.aistudio.axewatch.trader.data.model.IndexConstituent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Official NSE Indices constituent downloads. Membership carries no live price or weight. */
class IndexMembershipService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    companion object {
        private val files = mapOf(
            "NIFTY 50" to "ind_nifty50list.csv",
            "BANKNIFTY" to "ind_niftybanklist.csv",
            "NIFTYIT" to "ind_niftyitlist.csv",
            "NIFTYAUTO" to "ind_niftyautolist.csv",
            "NIFTYMETAL" to "ind_niftymetallist.csv"
        )

        fun supported(symbol: String): Boolean = files.containsKey(symbol)

        /** CSV quoting matters: company names may contain commas. Reject error-page HTML. */
        internal fun parseConstituents(csv: String): List<IndexConstituent> {
            val lines = csv.removePrefix("\uFEFF").lineSequence().filter { it.isNotBlank() }.toList()
            if (lines.isEmpty()) return emptyList()
            val headers = parseLine(lines.first()).map { it.trim().lowercase() }
            val nameAt = headers.indexOf("company name")
            val sectorAt = headers.indexOf("industry")
            val symbolAt = headers.indexOf("symbol")
            if (nameAt < 0 || sectorAt < 0 || symbolAt < 0) return emptyList()
            return lines.drop(1).mapNotNull { line ->
                val cells = parseLine(line)
                val name = cells.getOrNull(nameAt)?.trim().orEmpty()
                val sector = cells.getOrNull(sectorAt)?.trim().orEmpty()
                val symbol = cells.getOrNull(symbolAt)?.trim().orEmpty().uppercase()
                if (name.isBlank() || symbol.isBlank() || !symbol.matches(Regex("[A-Z0-9&.-]+"))) null
                else IndexConstituent(symbol, name, sector.ifBlank { "Unknown" }, 0.0, 0.0, 0.0, 0.0)
            }.distinctBy { it.symbol }
        }

        private fun parseLine(line: String): List<String> {
            val result = mutableListOf<String>()
            val cell = StringBuilder()
            var quoted = false
            var i = 0
            while (i < line.length) {
                when (val c = line[i]) {
                    '"' -> if (quoted && i + 1 < line.length && line[i + 1] == '"') {
                        cell.append('"'); i++
                    } else quoted = !quoted
                    ',' -> if (quoted) cell.append(c) else { result.add(cell.toString()); cell.clear() }
                    else -> cell.append(c)
                }
                i++
            }
            result.add(cell.toString())
            return result
        }
    }

    suspend fun fetchCsv(symbol: String): String? = withContext(Dispatchers.IO) {
        val filename = files[symbol] ?: return@withContext null
        try {
            val request = Request.Builder()
                .url("https://www.niftyindices.com/IndexConstituent/$filename")
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://www.niftyindices.com/")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string()?.takeIf { parseConstituents(it).isNotEmpty() }
            }
        } catch (_: Exception) { null }
    }
}
