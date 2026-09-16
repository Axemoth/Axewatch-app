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

class IpoAllotmentService {

    /** (sourceId, ok, latencyMs) — wired by the repository into health stats. */
    var onSourceResult: (String, Boolean, Int) -> Unit = { _, _, _ -> }

    companion object {
        private const val TAG = "IpoAllotmentService"
        private const val MUFG_BASE = "https://in.mpms.mufg.com/Initial_Offer/"
        private const val KFIN_URL = "https://0uz601ms56.execute-api.ap-south-1.amazonaws.com/prod/api/query"
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
            Log.w(TAG, "Failed to fetch MUFG company list: ${e.message}")
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
        // True when the registrar directory carries a declared allotment
        // date for this issue. Tracker statuses lag reality (an issue can
        // show "Active" after its basis is out) — a declared date always
        // wins and the registrars get queried.
        allotmentDeclared: Boolean = false
    ): AllotmentQueryResult = withContext(Dispatchers.IO) {
        val cleanPan = pan.trim().uppercase()

        // 1. If IPO status is Forthcoming or Active, allotment CANNOT be out yet
        if (ipoStatus.equals("Forthcoming", ignoreCase = true) && !allotmentDeclared) {
            return@withContext AllotmentQueryResult(
                found = false,
                source = "Registrar Schedule",
                companyName = ipoCompanyName,
                sharesApplied = 0,
                sharesAllotted = 0,
                status = "RESULTS_NOT_OUT",
                applicationNo = "N/A",
                applicantName = "",
                note = "Results are not out yet. This IPO issue is forthcoming and bidding has not opened."
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

        var registrarCompanyFound = false
        var transportFailures = 0

        // 2. Try MUFG Intime (Link Intime): live list first, remembered IDs
        // for issues that rotated off the dropdown (SearchOnPan keeps
        // answering old IDs — verified live).
        try {
            val companies = fetchMufgCompanies()
            var matchedCompany = findBestCompanyMatch(ipoCompanyName, ipoSymbol, companies)
            if (matchedCompany == null) {
                val want = canonIpoName(ipoCompanyName.ifBlank { ipoSymbol })
                rememberedMufgIds[want]?.let { (id, name) ->
                    matchedCompany = MufgCompany(id, name)
                }
            }
            if (matchedCompany != null) {
                registrarCompanyFound = true
                val t0 = System.currentTimeMillis()
                try {
                    val mufgResult = checkMufgAllotment(cleanPan, matchedCompany.id, matchedCompany.name)
                    onSourceResult("allot_mufg", true, (System.currentTimeMillis() - t0).toInt())
                    return@withContext mufgResult
                } catch (e: Exception) {
                    onSourceResult("allot_mufg", false, (System.currentTimeMillis() - t0).toInt())
                    transportFailures++
                    Log.w(TAG, "MUFG lookup failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MUFG query exception: ${e.message}")
        }

        // 3. Try KFintech API
        try {
            val t0 = System.currentTimeMillis()
            try {
                val kfinResult = checkKfinAllotment(cleanPan, ipoCompanyName)
                onSourceResult("allot_kfin", true, (System.currentTimeMillis() - t0).toInt())
                if (kfinResult.found) {
                    return@withContext kfinResult
                }
            } catch (e: Exception) {
                onSourceResult("allot_kfin", false, (System.currentTimeMillis() - t0).toInt())
                transportFailures++
                Log.w(TAG, "KFintech lookup failed: ${e.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "KFintech query exception: ${e.message}")
        }

        // Transport died on every attempted source: say so explicitly instead
        // of mislabeling it "not applied".
        if (transportFailures > 0 && !registrarCompanyFound) {
            return@withContext AllotmentQueryResult(
                found = false,
                source = "Network",
                companyName = ipoCompanyName,
                sharesApplied = 0,
                sharesAllotted = 0,
                status = "LOOKUP_FAILED",
                applicationNo = "",
                applicantName = "",
                note = "Lookup failed — network or registrar trouble. Nothing was concluded; retry."
            )
        }

        // 4. If neither registrar has published the company in their active database
        if (!registrarCompanyFound) {
            return@withContext AllotmentQueryResult(
                found = false,
                source = "Official Registrar",
                companyName = ipoCompanyName,
                sharesApplied = 0,
                sharesAllotted = 0,
                status = "RESULTS_NOT_OUT",
                applicationNo = "N/A",
                applicantName = "",
                note = "Results are not out yet. The registrar has not finalized or uploaded the allotment basis for $ipoCompanyName."
            )
        }

        // 5. Company results are published on registrar, but PAN not found -> NOT_APPLIED
        AllotmentQueryResult(
            found = false,
            source = "Registrar Query",
            companyName = ipoCompanyName,
            sharesApplied = 0,
            sharesAllotted = 0,
            status = "NOT_APPLIED",
            applicationNo = "N/A",
            applicantName = "",
            // Masked: the full PAN must never appear in stored or displayed text.
            note = "Not Applied: No application record found under PAN ${maskPan(cleanPan)} for $ipoCompanyName."
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
            Log.w(TAG, "MUFG landing warmup failed: ${e.message}")
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
        if (rawToken.isBlank()) return notFoundResult(companyName, "Invalid token")

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
        if (xmlD.isBlank()) return notFoundResult(companyName, "No XML data")

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
                    .url("$KFIN_URL?type=pan")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://ipostatus.kfintech.com/")
                    .header("Origin", "https://ipostatus.kfintech.com")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("reqparam", pan)
                    .header("client_id", "")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                lastCode = response.code
                if (response.code == 400) {
                    return notFoundResult(requestedCompanyName, "No record found on KFintech")
                }
                if (response.code == 429 || response.code in 500..504) {
                    response.close()
                    if (attempt == 0) {
                        delay(4000)
                        return@repeat
                    }
                    throw AllotmentTransportException("KFintech HTTP ${response.code} after retry")
                }
                if (!response.isSuccessful) {
                    throw AllotmentTransportException("KFintech HTTP ${response.code}")
                }

                val body = response.body?.string()
                    ?: throw AllotmentTransportException("KFintech empty response")
                val root = try {
                    val t = body.trim()
                    if (t.startsWith("[")) JSONArray(t)
                    else JSONObject(t).optJSONArray("data")
                        ?: JSONObject(t).optJSONArray("records")
                        ?: JSONArray()
                } catch (e: Exception) {
                    throw AllotmentTransportException("KFintech bad payload")
                }

                var matchedApplied = 0
                var matchedAllotted = 0
                var matchedAppNo = ""
                var matchedName = ""
                for (i in 0 until root.length()) {
                    val item = root.optJSONObject(i) ?: continue
                    val comp = item.optString("Company", item.optString("company", ""))
                    if (!ipoNamesMatch(comp, requestedCompanyName)) continue
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
            } catch (e: AllotmentTransportException) {
                transportError = e
                Log.w(TAG, "KFintech transport failure (attempt ${attempt + 1}): ${e.message}")
                if (attempt == 0) delay(2000)
            } catch (e: Exception) {
                Log.w(TAG, "KFintech attempt ${attempt + 1} failed: ${e.message}")
                if (attempt == 0) delay(2000)
            }
        }
        transportError?.let { throw it }
        return notFoundResult(requestedCompanyName, "KFintech unreachable (HTTP $lastCode)")
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
            Log.w(TAG, "Error parsing MUFG XML: ${e.message}")
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
            Log.w(TAG, "XML parse error in MUFG search: ${e.message}")
        }

        if (errorMsg.isNotBlank() && applied == 0) {
            return notFoundResult(fallbackCompanyName, errorMsg)
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
        val exact = companies.find { canonIpoName(it.name) == normIpo || canonIpoName(it.name) == normSym }
        if (exact != null) return exact

        // 2. Guarded substring match (10+ shared chars, like the web backend).
        //    Un-guarded contains() collides on short names, attributing one
        //    IPO's result to another.
        return companies.find {
            val cand = canonIpoName(it.name)
            cand.length >= 10 && (normIpo.contains(cand) || cand.contains(normIpo) ||
                (normSym.length >= 10 && (cand.contains(normSym) || normSym.contains(cand))))
        }
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
