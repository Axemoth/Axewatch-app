package com.aistudio.axewatch.trader.data.remote

import android.util.Log
import com.aistudio.axewatch.trader.data.model.FiiDiiFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.CookieManager
import java.net.CookiePolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * FII/DII cash-market flows from NSE's fiidiiTradeNse JSON.
 * NSE is aggressively bot-walled: cookie warmup first, empty list on any
 * failure. Callers must render empty as "unavailable", never fall back to
 * placeholder numbers.
 */
class FiiDiiService {

    companion object {
        private const val TAG = "FiiDiiService"
        private const val BASE = "https://www.nseindia.com"
        private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(CookieManager(null, CookiePolicy.ACCEPT_ALL)))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun num(v: Any?): Double {
        return v?.toString()?.replace(",", "")?.trim()?.toDoubleOrNull() ?: 0.0
    }

    suspend fun fetchFlows(): List<FiiDiiFlow> = withContext(Dispatchers.IO) {
        try {
            client.newCall(
                Request.Builder().url("$BASE/").header("User-Agent", UA).get().build()
            ).execute().close()
            val req = Request.Builder()
                .url("$BASE/api/fiidiiTradeNse")
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .header("Referer", "$BASE/market-data/fii-dii-activity")
                .get()
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val body = resp.body?.string() ?: return@withContext emptyList()
            // Response is either a bare list or {"data": [...]}.
            val arr: JSONArray = body.trim().let {
                if (it.startsWith("[")) JSONArray(it)
                else org.json.JSONObject(it).optJSONArray("data") ?: JSONArray()
            }
            var fiiBuy = 0.0; var fiiSell = 0.0
            var diiBuy = 0.0; var diiSell = 0.0
            var date = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val cat = o.optString("category", "")
                // Some payloads nest the latest day; prefer an explicit date field.
                o.optString("date", "").ifBlank { null }?.let { date = it }
                o.optString("tradingDate", "").ifBlank { null }?.let { date = it }
                when {
                    cat.startsWith("FII", ignoreCase = true) -> {
                        fiiBuy = num(o.opt("buyValue")); fiiSell = num(o.opt("sellValue"))
                    }
                    cat.startsWith("DII", ignoreCase = true) -> {
                        diiBuy = num(o.opt("buyValue")); diiSell = num(o.opt("sellValue"))
                    }
                }
            }
            if (fiiBuy == 0.0 && diiBuy == 0.0) return@withContext emptyList()
            listOf(FiiDiiFlow(date, fiiBuy, fiiSell, fiiBuy - fiiSell, diiBuy, diiSell, diiBuy - diiSell))
        } catch (e: Exception) {
            Log.w(TAG, "FII/DII fetch failed: ${e.message}")
            emptyList()
        }
    }
}
