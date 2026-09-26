package com.aistudio.axewatch.trader.data.remote

import android.util.Base64
import android.util.Log
import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class MufgCompany(
    val id: String,
    val name: String
)

data class AllotmentQueryResult(
    val found: Boolean,
    val source: String,
    val companyName: String,
    val sharesApplied: Int,
    val sharesAllotted: Int,
    val status: String,
    val applicationNo: String,
    val applicantName: String,
    val note: String = ""
)

/** Transport trouble (HTTP 5xx/429/IO). Never confused with a business
 *  outcome: callers must surface it as a failed check, not "not applied". */
class AllotmentTransportException(message: String) : Exception(message)

class IpoAllotmentService(
    private val kfinEndpoint: String = KFIN_URL,
    private val maashitlaApiBase: String = MAASHITLA_API_BASE
) {

    /** (sourceId, ok, latencyMs) — wired by the repository into health stats. */
    var onSourceResult: (String, Boolean, Int) -> Unit = { _, _, _ -> }

    companion object {
        private const val TAG = "IpoAllotmentService"
        private const val MUFG_BASE = "https://in.mpms.mufg.com/Initial_Offer/"
        private const val KFIN_URL = "https://0uz601ms56.execute-api.ap-south-1.amazonaws.com/prod/api/query"
        private const val MAASHITLA_API_BASE = "https://api.maashitla.com/api"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
        private val AES_KEY_BYTES = "8080808080808080".toByteArray(Charsets.UTF_8)
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")

        /** Strict PAN check — callers must refuse to store or query anything else. */
        fun isValidPan(pan: String): Boolean = PAN_REGEX.matches(pan.trim().uppercase())

        /** "ABCDE1234F" -> "AB*****F". Full PANs never reach logs or UI text. */
        fun maskPan(pan: String): String {
            val p = pan.trim().uppercase()
            return if (p.length == 10) "${p.take(2)}*****${p.takeLast(1)}" else "***"
        }

        /** Applicant names from registrars display masked (first 4 chars). */
        fun maskApplicantName(name: String): String {
            val t = name.trim()
            return if (t.length > 4) t.take(4) + "***" else if (t.isEmpty()) "" else "***"
        }

        /** Join key shared with the Axewatch web backend (canon_ipo_name mirror). */
        fun canonIpoName(name: String?): String {
            var s = (name ?: "").lowercase().replace("&", " and ")
            s = s.replace(Regex("[^a-z0-9]"), "")
            repeat(4) {
                val before = s
                s = s.replace(Regex("(open|closed|upcoming|listed|live|active|forthcoming)$"), "")
                    .replace(Regex("(sme|ipo|limited|ltd|pvt|private|india)$"), "")
                if (s == before) return s
            }
            return s
        }

        /** Lenient match for truncated tracker names. Minimum 10 shared chars
         *  so short names like "ARCIL" can never collide. */
        fun ipoNamesMatch(a: String?, b: String?): Boolean {
            val na = canonIpoName(a)
            val nb = canonIpoName(b)
            if (na.isEmpty() || nb.isEmpty()) return false
            if (na == nb) return true
            val short = if (na.length < nb.length) na else nb
            val long = if (na.length < nb.length) nb else na
            return short.length >= 10 && long.contains(short)
        }

        /**
         * Registrar share counts arrive as ints, "1,234" strings, or
         * "150.0" decimals depending on registrar and row. org.json optInt
         * returns 0 for all string forms — which silently dropped real
         * allotments (records skipped as "no shares"). Mirrors the web
         * backend's _num(). Pure function, unit-tested.
         */
        fun parseShareCount(v: Any?): Int {
            if (v is Number) return v.toInt()
            val s = v?.toString()?.replace(",", "")?.trim() ?: return 0
            if (s.isEmpty() || s.equals("null", ignoreCase = true)) return 0
            return s.toIntOrNull() ?: s.toDoubleOrNull()?.toInt() ?: 0
        }
    }

    // CookieJar is REQUIRED: MUFG's token endpoint validates the session that
    // fetched the landing page. Without it every lookup silently misbehaves.
    private val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)

    private val client = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieManager))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val lastCallAt = mutableMapOf<String, Long>()

    /** Polite pacing per source (mirrors the web backend budget). */
    private suspend fun pace(source: String, gapMs: Long) {
        val now = System.currentTimeMillis()
        val wait = (lastCallAt[source] ?: 0L) + gapMs - now
        if (wait > 0) delay(wait)
        lastCallAt[source] = System.currentTimeMillis()
    }

    private var cachedMufgCompanies: List<MufgCompany> = emptyList()
    private var cachedMufgCompaniesAt = 0L
    private val mufgCompaniesTtlMs = 3600_000L

    /**
     * Fetch the list of active companies on MUFG Intime (Link Intime).
     * Warms cookies with the landing page first — the token endpoint
     * validates the session, and calls without it silently misbehave.
     */
    suspend fun fetchMufgCompanies(): List<MufgCompany> = withContext(Dispatchers.IO) {
        if (cachedMufgCompanies.isNotEmpty() &&
            System.currentTimeMillis() - cachedMufgCompaniesAt < mufgCompaniesTtlMs
        ) {
            return@withContext cachedMufgCompanies
        }

        try {
            pace("allot_mufg", 1500)
            client.newCall(
                Request.Builder().url(MUFG_BASE + "public-issues.html")
                    .header("User-Agent", USER_AGENT).get().build()
            ).execute().close()
            val request = Request.Builder()
                .url(MUFG_BASE + "IPO.aspx/GetDetails")
                .header("User-Agent", USER_AGENT)
                .header("Referer", MUFG_BASE + "public-issues.html")
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(body)
            val xmlData = root.optString("d", "")
            if (xmlData.isBlank()) return@withContext emptyList()

            val companies = parseMufgCompaniesXml(xmlData)
            cachedMufgCompanies = companies
            cachedMufgCompaniesAt = System.currentTimeMillis()
            companies
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch MUFG company list: ${e.javaClass.simpleName}")
            emptyList()
        }
    }

    /**
     * Check allotment across Link Intime (MUFG) or KFintech API based on company matching.
     * Accurately distinguishes between:
     * - "ALLOTTED": Application found with shares allotted > 0
     * - "NOT_ALLOTTED": Application found in allotment lottery with 0 shares allotted
     * - "NOT_APPLIED": Results declared by registrar, but no bid record found under this PAN
     * - "RESULTS_NOT_OUT": Issue is active/forthcoming or registrar has not finalized basis of allotment yet
     */
    suspend fun queryAllotment(
        pan: String,
        ipoSymbol: String,
        ipoCompanyName: String,
        ipoStatus: String = "Active",
        rememberedMufgIds: Map<String, Pair<String, String>> = emptyMap(),
        // True when the scheduled allotment date has passed or the tracker
        // explicitly reports allotted/listed. A registrar listing alone does
        // not prove that results have been published.
        allotmentDeclared: Boolean = false,
        registrarHint: String = ""
    ): AllotmentQueryResult = withContext(Dispatchers.IO) {
        val cleanPan = pan.trim().uppercase()

        // 1. If IPO status is Forthcoming, Upcoming, or Active, allotment CANNOT be out yet
        val isForthcoming = ipoStatus.equals("Forthcoming", ignoreCase = true) ||
            ipoStatus.equals("Upcoming", ignoreCase = true) ||
            ipoStatus.contains("pre", ignoreCase = true)
        if (isForthcoming && !allotmentDeclared) {
            return@withContext AllotmentQueryResult(
                found = false,
                source = "Registrar Schedule",
                companyName = ipoCompanyName,
                sharesApplied = 0,
                sharesAllotted = 0,
                status = "RESULTS_NOT_OUT",
                applicationNo = "N/A",
                applicantName = "",
                note = "Results are not out yet. This IPO issue is in pre-apply stage and bidding has not opened yet."
            )
        }

        if (ipoStatus.equals("Active", ignoreCase = true) && !allotmentDeclared) {
            return@withContext AllotmentQueryResult(
                found = false,
                source = "Registrar Schedule",
                companyName = ipoCompanyName,
                sharesApplied = 0,
                sharesAllotted = 0,
                status = "RESULTS_NOT_OUT",
                applicationNo = "N/A",
                applicantName = "",
                note = "Results are not out yet. Bidding is currently open. Basis of allotment is declared after issue closure."
            )
        }

        val regLower = registrarHint.trim().lowercase()
        val isBigshare = regLower.contains("bigshare")
        val isOtherManual = regLower.contains("skyline") || regLower.contains("cameo") ||
            regLower.contains("purva") || regLower.contains("beetal")

        if (regLower.contains("maashitla")) {
            val started = System.currentTimeMillis()
            try {
                val result = checkMaashitlaAllotment(cleanPan, ipoSymbol, ipoCompanyName)
                onSourceResult("allot_maashitla", true, (System.currentTimeMillis() - started).toInt())
                if (result == null) return@withContext if (allotmentDeclared) uncoveredResult(ipoCompanyName, "Maashitla")
                    else pendingResult(ipoCompanyName)
                return@withContext if (result.status == "ALLOTTED" || allotmentDeclared) result
                    else pendingResult(ipoCompanyName)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onSourceResult("allot_maashitla", false, (System.currentTimeMillis() - started).toInt())
                Log.w(TAG, "Maashitla lookup failed: ${e.javaClass.simpleName}")
                return@withContext failedResult(ipoCompanyName)
            }
        }

        // 2. Bigshare is CAPTCHA-walled; other portals need manual handoff.
        if (isBigshare || isOtherManual) {
            val regName = if (isBigshare) "Bigshare" else registrarHint
            val msg = if (isBigshare) {
                "Handled by Bigshare — official portal requires server CAPTCHA verification. Tap 'Open Portal' to check on Bigshare and log your outcome."
            } else {
                "Handled by $regName — official portal verification required. Tap 'Open Portal' to check on their official site and record your outcome."
            }
            return@withContext AllotmentQueryResult(
                found = false,
                source = regName,
                companyName = ipoCompanyName,
                sharesApplied = 0,
                sharesAllotted = 0,
                status = "MANUAL_CHECK_REQUIRED",
                applicationNo = "",
                applicantName = "",
                note = msg
            )
        }

        // 3. Known KFintech Issue (e.g. SS Retail)
        val isKfin = regLower.contains("kfin")
        var kfinAttempted = false
        if (isKfin) {
            kfinAttempted = true
            val t0 = System.currentTimeMillis()
            try {
                val kfinResult = checkKfinAllotment(cleanPan, ipoCompanyName)
                onSourceResult("allot_kfin", true, (System.currentTimeMillis() - t0).toInt())
                if (kfinResult.found) return@withContext if (
                    kfinResult.status == "ALLOTTED" || allotmentDeclared
                ) kfinResult else pendingResult(ipoCompanyName)
                // A negative is only final after the allotment is declared.
                return@withContext if (allotmentDeclared) kfinResult else pendingResult(ipoCompanyName)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onSourceResult("allot_kfin", false, (System.currentTimeMillis() - t0).toInt())
                Log.w(TAG, "KFintech lookup failed: ${e.javaClass.simpleName}")
                // A known KFin issue cannot be answered by another registrar.
                return@withContext failedResult(ipoCompanyName)
            }
        }

        var registrarCompanyFound = false
        var transportFailures = 0

        // 4. Try MUFG Intime (Link Intime): live list first, remembered IDs
        // for issues that rotated off the dropdown (SearchOnPan keeps
        // answering old IDs — verified live).
        try {
            val want = canonIpoName(ipoCompanyName.ifBlank { ipoSymbol })
            val remembered = rememberedMufgIds[want]?.let { (id, name) -> MufgCompany(id, name) }
            val companies = try { fetchMufgCompanies() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (remembered == null) throw e
                emptyList()
            }
            val matchedCompany = findBestCompanyMatch(ipoCompanyName, ipoSymbol, companies) ?: remembered
            if (matchedCompany != null) {
                registrarCompanyFound = true
                val t0 = System.currentTimeMillis()
                try {
                    val mufgResult = checkMufgAllotment(cleanPan, matchedCompany.id, matchedCompany.name)
                    onSourceResult("allot_mufg", true, (System.currentTimeMillis() - t0).toInt())
                    return@withContext if (mufgResult.status == "ALLOTTED" || allotmentDeclared) mufgResult
                        else pendingResult(ipoCompanyName)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    onSourceResult("allot_mufg", false, (System.currentTimeMillis() - t0).toInt())
                    transportFailures++
                    Log.w(TAG, "MUFG lookup failed: ${e.javaClass.simpleName}")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            transportFailures++
            Log.w(TAG, "MUFG query exception: ${e.javaClass.simpleName}")
        }

        if (regLower.contains("mufg") || regLower.contains("intime")) {
            return@withContext if (transportFailures > 0) failedResult(ipoCompanyName)
                else uncoveredResult(ipoCompanyName, registrarHint)
        }

        // 5. Try KFintech API if not already tried above
        try {
            val t0 = System.currentTimeMillis()
            try {
                if (!kfinAttempted) {
                    val kfinResult = checkKfinAllotment(cleanPan, ipoCompanyName)
                    onSourceResult("allot_kfin", true, (System.currentTimeMillis() - t0).toInt())
                    if (kfinResult.found) return@withContext if (
                        kfinResult.status == "ALLOTTED" || allotmentDeclared
                    ) kfinResult else pendingResult(ipoCompanyName)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (!kfinAttempted) {
                    onSourceResult("allot_kfin", false, (System.currentTimeMillis() - t0).toInt())
                }
                transportFailures++
                Log.w(TAG, "KFintech lookup failed: ${e.javaClass.simpleName}")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "KFintech query exception: ${e.javaClass.simpleName}")
        }

        // Transport died on every attempted source: say so explicitly instead
        // of mislabeling it "not applied".
        if (transportFailures > 0 && !registrarCompanyFound) return@withContext failedResult(ipoCompanyName)

        // 6. If neither automated registrar has published the company in their active database
        if (!registrarCompanyFound) {
            return@withContext if (allotmentDeclared) uncoveredResult(ipoCompanyName, registrarHint)
                else pendingResult(ipoCompanyName)
        }

        // 7. Company results are published on registrar, but PAN not found -> NOT_APPLIED
        if (allotmentDeclared) notFoundResult(ipoCompanyName, "No application record found on the registrar")
        else pendingResult(ipoCompanyName)
    }

    private fun failedResult(company: String) = AllotmentQueryResult(
        false, "Network", company, 0, 0, "LOOKUP_FAILED", "", "",
        "Lookup failed — network or registrar trouble. Nothing was concluded; retry."
    )

    private fun pendingResult(company: String) = AllotmentQueryResult(
        false, "Registrar Schedule", company, 0, 0, "RESULTS_NOT_OUT", "", "",
        "The allotment result is not confirmed yet; check again after declaration."
    )

    private fun uncoveredResult(company: String, registrar: String) = AllotmentQueryResult(
        false, registrar.ifBlank { "Official Registrar" }, company, 0, 0, "UNCOVERED", "", "",
        "Automated coverage is not confirmed for this IPO — use the official registrar portal."
    )

    /** Maashitla's official public-issues page uses these two JSON endpoints.
     *  Match its company dropdown first so one issuer's PAN result cannot be
     *  attributed to another. Never include the request URL in logs/errors. */
    private suspend fun checkMaashitlaAllotment(
        pan: String, symbol: String, companyName: String
    ): AllotmentQueryResult? {
        pace("allot_maashitla", 1500)
        val listRequest = Request.Builder()
            .url("$maashitlaApiBase/public-issue/companies")
            .header("User-Agent", USER_AGENT)
            .header("ngrok-skip-browser-warning", "true")
            .build()
        val companies = client.newCall(listRequest).execute().use { response ->
            if (!response.isSuccessful) throw AllotmentTransportException("Maashitla company list HTTP ${response.code}")
            val body = response.body?.string() ?: throw AllotmentTransportException("Maashitla company list empty")
            val rows = JSONArray(body)
            (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val name = row.optString("company_name").trim()
                val id = row.optString("company_id").trim()
                if (name.isBlank() || id.isBlank()) null else MufgCompany(id, name)
            }
        }
        val company = findBestCompanyMatch(companyName, symbol, companies) ?: return null
        val url = "$maashitlaApiBase/public-issue/search".toHttpUrl().newBuilder()
            .addQueryParameter("company_name", company.name)
            .addQueryParameter("pan", pan)
            .build()
        val request = Request.Builder().url(url)
            .header("User-Agent", USER_AGENT)
            .header("ngrok-skip-browser-warning", "true")
            .build()
        return client.newCall(request).execute().use { response ->
            if (response.code == 404) return@use notFoundResult(companyName, "No application record found on Maashitla")
            if (!response.isSuccessful) throw AllotmentTransportException("Maashitla search HTTP ${response.code}")
            val body = response.body?.string() ?: throw AllotmentTransportException("Maashitla search empty")
            parseMaashitlaJson(body, companyName)
        }
    }

    internal fun parseMaashitlaJson(body: String, companyName: String): AllotmentQueryResult {
        val row = try { JSONObject(body) } catch (_: Exception) {
            throw AllotmentTransportException("Maashitla malformed result")
        }
        if (!row.has("shares_alloted") && !row.has("shares_allotted")) {
            throw AllotmentTransportException("Maashitla missing allotment field")
        }
        val applied = parseShareCount(row.opt("shares_applied"))
        val allotted = parseShareCount(row.opt("shares_alloted") ?: row.opt("shares_allotted"))
        return AllotmentQueryResult(
            true, "Maashitla", companyName, applied, allotted,
            if (allotted > 0) "ALLOTTED" else "NOT_ALLOTTED",
            "", maskApplicantName(row.optString("name"))
        )
    }

    private suspend fun checkMufgAllotment(pan: String, companyId: String, companyName: String): AllotmentQueryResult {
        // Warm the session: token issuance is cookie-bound.
        pace("allot_mufg", 1500)
        try {
            client.newCall(
                Request.Builder().url(MUFG_BASE + "public-issues.html")
                    .header("User-Agent", USER_AGENT).get().build()
            ).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "MUFG landing warmup failed: ${e.javaClass.simpleName}")
        }
        // Step A: Generate Token
        val tokenReq = Request.Builder()
            .url(MUFG_BASE + "IPO.aspx/generateToken")
            .header("User-Agent", USER_AGENT)
            .header("Referer", MUFG_BASE + "public-issues.html")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val tokenResp = client.newCall(tokenReq).execute()
        if (!tokenResp.isSuccessful) throw AllotmentTransportException("MUFG token HTTP ${tokenResp.code}")

        val tokenBody = tokenResp.body?.string() ?: throw AllotmentTransportException("MUFG token empty")
        val rawToken = JSONObject(tokenBody).optString("d", "").trim()
        if (rawToken.isBlank()) throw AllotmentTransportException("MUFG invalid token")

        val encryptedToken = encryptMufgToken(rawToken)

        // Step B: Search on PAN
        val searchPayload = JSONObject().apply {
            put("clientid", companyId)
            put("PAN", pan)
            put("IFSC", "")
            put("CHKVAL", "1")
            put("token", encryptedToken)
        }

        val searchReq = Request.Builder()
            .url(MUFG_BASE + "IPO.aspx/SearchOnPan")
            .header("User-Agent", USER_AGENT)
            .header("Referer", MUFG_BASE + "public-issues.html")
            .post(searchPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val searchResp = client.newCall(searchReq).execute()
        if (!searchResp.isSuccessful) throw AllotmentTransportException("MUFG search HTTP ${searchResp.code}")

        val searchBody = searchResp.body?.string() ?: throw AllotmentTransportException("MUFG search empty")
        val xmlD = JSONObject(searchBody).optString("d", "")
        if (xmlD.isBlank()) throw AllotmentTransportException("MUFG search returned no XML")

        return parseMufgSearchXml(xmlD, companyName)
    }

    /**
     * KFintech PAN search across all its IPOs in one call. The PAN travels in
     * the `reqparam` HEADER exactly like their own page sends it — never in
     * the URL (URLs end up in logs and caches). Records are attributed to the
     * requested IPO only, so one IPO's result can never leak into another's.
     */
    private suspend fun checkKfinAllotment(pan: String, requestedCompanyName: String): AllotmentQueryResult {
        pace("allot_kfin", 2000)
        var lastCode = -1
        var transportError: AllotmentTransportException? = null
        repeat(2) { attempt ->
            try {
                val request = Request.Builder()
                    .url("$kfinEndpoint?type=pan")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://ipostatus.kfintech.com/")
                    .header("Origin", "https://ipostatus.kfintech.com")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("access-control-allow-origin", "*")
                    .header("sec-ch-ua", "\"Not=A?Brand\";v=\"99\", \"Chromium\";v=\"138\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Android\"")
                    .header("reqparam", pan)
                    .header("client_id", "")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                lastCode = response.code
                if (response.code == 400) {
                    response.close()
                    return notFoundResult(requestedCompanyName, "No record found on KFintech")
                }
                if (response.code == 429 || response.code == 503) {
                    response.close()
                    throw AllotmentTransportException("KFintech throttled (HTTP ${response.code})")
                }
                if (response.code in listOf(500, 502, 504)) {
                    response.close()
                    if (attempt == 0) {
                        delay(4000)
                        return@repeat
                    }
                    throw AllotmentTransportException("KFintech HTTP ${response.code} after retry")
                }
                if (!response.isSuccessful) {
                    response.close()
                    throw AllotmentTransportException("KFintech HTTP ${response.code}")
                }

                val body = response.body?.string()
                    ?: throw AllotmentTransportException("KFintech empty response")
                return parseKfinJson(body, requestedCompanyName)
            } catch (e: AllotmentTransportException) {
                transportError = e
                Log.w(TAG, "KFintech transport failure (attempt ${attempt + 1}): ${e.javaClass.simpleName}")
                if ("throttled" in e.message.orEmpty()) throw e
                if (attempt == 0) delay(2000)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                transportError = AllotmentTransportException("KFintech request failed")
                Log.w(TAG, "KFintech attempt ${attempt + 1} failed: ${e.javaClass.simpleName}")
                if (attempt == 0) delay(2000)
            }
        }
        transportError?.let { throw it }
        throw AllotmentTransportException("KFintech unreachable (HTTP $lastCode)")
    }

    internal fun parseKfinJson(body: String, requestedCompanyName: String): AllotmentQueryResult {
        val root = try {
            val t = body.trim()
            if (t.startsWith("[")) JSONArray(t)
            else {
                val obj = JSONObject(t)
                obj.optJSONArray("data") ?: obj.optJSONArray("records")
                    ?: throw AllotmentTransportException("KFintech payload has no records array")
            }
        } catch (e: AllotmentTransportException) {
            throw e
        } catch (e: Exception) {
            throw AllotmentTransportException("KFintech bad payload")
        }

        val candidates = (0 until root.length()).mapNotNull { root.optJSONObject(it) }
            .map { it.optString("Company", it.optString("company", "")) }
            .filter { ipoNamesMatch(it, requestedCompanyName) }
            .map { canonIpoName(it) }.distinct()
        val wanted = canonIpoName(requestedCompanyName)
        val matchedCompany = if (wanted in candidates) wanted else candidates.singleOrNull()
        if (candidates.size > 1 && matchedCompany == null) {
            throw AllotmentTransportException("KFintech company match is ambiguous")
        }
        var matchedApplied = 0
        var matchedAllotted = 0
        var matchedAppNo = ""
        var matchedName = ""
        for (i in 0 until root.length()) {
            val item = root.optJSONObject(i) ?: continue
            val comp = item.optString("Company", item.optString("company", ""))
            if (canonIpoName(comp) != matchedCompany) continue
            val appShares = parseShareCount(
                if (item.has("App_Shares")) item.get("App_Shares")
                else if (item.has("app_shares")) item.get("app_shares") else 0
            )
            val allShares = parseShareCount(
                if (item.has("All_Shares")) item.get("All_Shares")
                else if (item.has("all_shares")) item.get("all_shares") else 0
            )
            if (appShares <= 0 && allShares <= 0) continue
            matchedApplied += appShares
            matchedAllotted += allShares
            if (matchedAppNo.isEmpty()) {
                matchedAppNo = item.optString("Appln_No", item.optString("appln_no", ""))
                matchedName = item.optString("Name", item.optString("name", ""))
            }
        }

        if (matchedApplied > 0 || matchedAllotted > 0) {
            val isAllotted = matchedAllotted > 0
            return AllotmentQueryResult(
                found = true,
                source = "KFintech",
                companyName = requestedCompanyName,
                sharesApplied = matchedApplied,
                sharesAllotted = matchedAllotted,
                status = if (isAllotted) "ALLOTTED" else "NOT_ALLOTTED",
                applicationNo = matchedAppNo,
                applicantName = maskApplicantName(matchedName),
                note = if (isAllotted) "Allotted $matchedAllotted shares" else "Bid processed - Zero shares allotted"
            )
        }
        return notFoundResult(requestedCompanyName, "No matching bids found on KFintech")
    }

    private fun encryptMufgToken(token: String): String {
        val keySpec = SecretKeySpec(AES_KEY_BYTES, "AES")
        val ivSpec = IvParameterSpec(AES_KEY_BYTES)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun parseMufgCompaniesXml(xml: String): List<MufgCompany> {
        val list = mutableListOf<MufgCompany>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentId = ""
            var currentName = ""
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                    }
                    XmlPullParser.TEXT -> {
                        when (currentTag.lowercase()) {
                            "company_id" -> currentId = parser.text.trim()
                            "companyname" -> currentName = parser.text.trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("Table", ignoreCase = true)) {
                            if (currentId.isNotBlank() && currentName.isNotBlank()) {
                                list.add(MufgCompany(currentId, currentName))
                            }
                            currentId = ""
                            currentName = ""
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing MUFG XML: ${e.javaClass.simpleName}")
        }
        return list
    }

    // Internal (not private) for white-box unit tests: the XML contract is
    // the heart of correctness — a misread tag once flipped outcomes.
    internal fun parseMufgSearchXml(xml: String, fallbackCompanyName: String): AllotmentQueryResult {
        var applied = 0
        var allotted = 0
        var appNo = ""
        var applicantName = ""
        var errorMsg = ""

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        // Counts may arrive comma-formatted: tolerant parse
                        // (same reason as KFin parseShareCount).
                        fun tolerantCount(raw: String, prev: Int): Int =
                            raw.toIntOrNull()
                                ?: raw.replace(",", "").toIntOrNull()
                                ?: raw.replace(",", "").toDoubleOrNull()?.toInt()
                                ?: prev
                        when (currentTag.uppercase()) {
                            "SHARES" -> applied = tolerantCount(text, applied)
                            "ALLOT" -> allotted = tolerantCount(text, allotted)
                            "PEMNDG" -> appNo = text
                            "NAME1" -> applicantName = text
                            "MSG" -> errorMsg = text
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "XML parse error in MUFG search: ${e.javaClass.simpleName}")
            throw AllotmentTransportException("MUFG search XML malformed")
        }

        if (errorMsg.isNotBlank() && applied == 0) {
            val noRecord = listOf("no record", "no application", "not found", "no bid")
                .any { it in errorMsg.lowercase() }
            if (!noRecord) throw AllotmentTransportException("MUFG search returned an error")
            return notFoundResult(fallbackCompanyName, "No application record found on MUFG")
        }

        if (applied > 0 || allotted > 0) {
            val isAllotted = allotted > 0
            return AllotmentQueryResult(
                found = true,
                source = "MUFG Intime",
                companyName = fallbackCompanyName,
                sharesApplied = applied,
                sharesAllotted = allotted,
                status = if (isAllotted) "ALLOTTED" else "NOT_ALLOTTED",
                // Real application number or blank (UI renders "—"). Never
                // invent one: a fabricated number looks like your real data.
                applicationNo = appNo,
                applicantName = maskApplicantName(applicantName),
                note = if (isAllotted) "Allotted $allotted shares" else "Applied for $applied shares - Not allotted"
            )
        }

        return notFoundResult(fallbackCompanyName, "No bid records found on MUFG")
    }

    // Internal for unit tests: mis-attribution sends one IPO's result to
    // another, so the matching guards are pinned by tests.
    internal fun findBestCompanyMatch(ipoName: String, ipoSymbol: String, companies: List<MufgCompany>): MufgCompany? {
        val normIpo = canonIpoName(ipoName)
        val normSym = canonIpoName(ipoSymbol)

        // 1. Exact canonical match
        val exact = companies.filter { normIpo.isNotBlank() && canonIpoName(it.name) == normIpo }.singleOrNull()
            ?: companies.filter { normSym.isNotBlank() && canonIpoName(it.name) == normSym }.singleOrNull()
        if (exact != null) return exact

        // 2. Guarded substring match (10+ shared chars, like the web backend).
        //    Un-guarded contains() collides on short names, attributing one
        //    IPO's result to another.
        return companies.filter {
            ipoNamesMatch(it.name, ipoName) || ipoNamesMatch(it.name, ipoSymbol)
        }.singleOrNull()
    }

    private fun notFoundResult(companyName: String, note: String, status: String = "NOT_APPLIED"): AllotmentQueryResult {
        return AllotmentQueryResult(
            found = false,
            source = "Registrar Query",
            companyName = companyName,
            sharesApplied = 0,
            sharesAllotted = 0,
            status = status,
            applicationNo = "N/A",
            applicantName = "",
            note = note
        )
    }
}
