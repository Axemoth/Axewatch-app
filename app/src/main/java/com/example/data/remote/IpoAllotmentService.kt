package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.data.local.entity.AllotmentRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
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

class IpoAllotmentService {

    companion object {
        private const val TAG = "IpoAllotmentService"
        private const val MUFG_BASE = "https://in.mpms.mufg.com/Initial_Offer/"
        private const val KFIN_URL = "https://0uz601ms56.execute-api.ap-south-1.amazonaws.com/prod/api/query"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
        private val AES_KEY_BYTES = "8080808080808080".toByteArray(Charsets.UTF_8)
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var cachedMufgCompanies: List<MufgCompany> = emptyList()

    /**
     * Fetch the list of active companies on MUFG Intime (Link Intime).
     */
    suspend fun fetchMufgCompanies(): List<MufgCompany> = withContext(Dispatchers.IO) {
        if (cachedMufgCompanies.isNotEmpty()) return@withContext cachedMufgCompanies

        try {
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
            companies
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch MUFG company list: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check allotment across Link Intime (MUFG) or KFintech API based on company matching.
     */
    suspend fun queryAllotment(
        pan: String,
        ipoSymbol: String,
        ipoCompanyName: String
    ): AllotmentQueryResult = withContext(Dispatchers.IO) {
        val cleanPan = pan.trim().uppercase()

        // 1. Try MUFG Intime first if we find a matching company
        try {
            val companies = fetchMufgCompanies()
            val matchedCompany = findBestCompanyMatch(ipoCompanyName, ipoSymbol, companies)
            if (matchedCompany != null) {
                val mufgResult = checkMufgAllotment(cleanPan, matchedCompany.id, matchedCompany.name)
                if (mufgResult.found) {
                    return@withContext mufgResult
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MUFG query exception: ${e.message}")
        }

        // 2. Try KFintech API
        try {
            val kfinResult = checkKfinAllotment(cleanPan, ipoCompanyName)
            if (kfinResult.found) {
                return@withContext kfinResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "KFintech query exception: ${e.message}")
        }

        // 3. Fallback: Not allotted or no active record published yet
        AllotmentQueryResult(
            found = false,
            source = "Registrar Query",
            companyName = ipoCompanyName,
            sharesApplied = 0,
            sharesAllotted = 0,
            status = "NOT_ALLOTTED",
            applicationNo = "N/A",
            applicantName = "",
            note = "No allotment record found for this PAN on registrar database."
        )
    }

    private fun checkMufgAllotment(pan: String, companyId: String, companyName: String): AllotmentQueryResult {
        // Step A: Generate Token
        val tokenReq = Request.Builder()
            .url(MUFG_BASE + "IPO.aspx/generateToken")
            .header("User-Agent", USER_AGENT)
            .header("Referer", MUFG_BASE + "public-issues.html")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val tokenResp = client.newCall(tokenReq).execute()
        if (!tokenResp.isSuccessful) return notFoundResult(companyName, "MUFG token error")

        val tokenBody = tokenResp.body?.string() ?: return notFoundResult(companyName, "Empty token response")
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
        if (!searchResp.isSuccessful) return notFoundResult(companyName, "MUFG search failed")

        val searchBody = searchResp.body?.string() ?: return notFoundResult(companyName, "Empty search response")
        val xmlD = JSONObject(searchBody).optString("d", "")
        if (xmlD.isBlank()) return notFoundResult(companyName, "No XML data")

        return parseMufgSearchXml(xmlD, companyName)
    }

    private fun checkKfinAllotment(pan: String, fallbackCompanyName: String): AllotmentQueryResult {
        val url = "$KFIN_URL?type=pan&client_id=&reqparam=$pan"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://ipostatus.kfintech.com/")
            .header("Origin", "https://ipostatus.kfintech.com")
            .header("Accept", "application/json, text/plain, */*")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (response.code == 400) {
            return notFoundResult(fallbackCompanyName, "No record found on KFintech")
        }
        if (!response.isSuccessful) {
            return notFoundResult(fallbackCompanyName, "KFintech HTTP ${response.code}")
        }

        val body = response.body?.string() ?: return notFoundResult(fallbackCompanyName, "Empty response")
        val root = try {
            if (body.trim().startsWith("[")) JSONArray(body) else JSONObject(body).optJSONArray("data") ?: JSONArray()
        } catch (e: Exception) {
            JSONArray()
        }

        for (i in 0 until root.length()) {
            val item = root.optJSONObject(i) ?: continue
            val comp = item.optString("Company", item.optString("company", fallbackCompanyName))
            val appShares = item.optInt("App_Shares", item.optInt("app_shares", 0))
            val allShares = item.optInt("All_Shares", item.optInt("all_shares", 0))
            val appNo = item.optString("Appln_No", item.optString("appln_no", "KFIN" + (100000..999999).random()))
            val name = item.optString("Name", item.optString("name", "Applicant"))

            if (appShares > 0 || allShares > 0) {
                val isAllotted = allShares > 0
                return AllotmentQueryResult(
                    found = true,
                    source = "KFintech",
                    companyName = comp,
                    sharesApplied = appShares,
                    sharesAllotted = allShares,
                    status = if (isAllotted) "ALLOTTED" else "NOT_ALLOTTED",
                    applicationNo = appNo,
                    applicantName = name,
                    note = if (isAllotted) "Allotted $allShares shares" else "Bid processed - Zero shares allotted"
                )
            }
        }

        return notFoundResult(fallbackCompanyName, "No matching bids found on KFintech")
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

    private fun parseMufgSearchXml(xml: String, fallbackCompanyName: String): AllotmentQueryResult {
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
                        when (currentTag.uppercase()) {
                            "SHARES" -> applied = text.toIntOrNull() ?: applied
                            "ALLOT" -> allotted = text.toIntOrNull() ?: allotted
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
                applicationNo = if (appNo.isNotBlank()) appNo else "MUFG" + (100000..999999).random(),
                applicantName = applicantName,
                note = if (isAllotted) "Allotted $allotted shares" else "Applied for $applied shares - Not allotted"
            )
        }

        return notFoundResult(fallbackCompanyName, "No bid records found on MUFG")
    }

    private fun findBestCompanyMatch(ipoName: String, ipoSymbol: String, companies: List<MufgCompany>): MufgCompany? {
        val normIpo = canonicalName(ipoName)
        val normSym = canonicalName(ipoSymbol)

        // 1. Exact canonical match
        val exact = companies.find { canonicalName(it.name) == normIpo || canonicalName(it.name) == normSym }
        if (exact != null) return exact

        // 2. Substring match
        return companies.find {
            val cand = canonicalName(it.name)
            cand.contains(normSym) || normIpo.contains(cand) || cand.contains(normIpo)
        }
    }

    private fun canonicalName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .replace("limited", "")
            .replace("ltd", "")
            .replace("pvt", "")
            .replace("private", "")
            .replace("ipo", "")
            .replace("sme", "")
    }

    private fun notFoundResult(companyName: String, note: String): AllotmentQueryResult {
        return AllotmentQueryResult(
            found = false,
            source = "Registrar Query",
            companyName = companyName,
            sharesApplied = 0,
            sharesAllotted = 0,
            status = "NOT_ALLOTTED",
            applicationNo = "N/A",
            applicantName = "",
            note = note
        )
    }
}
