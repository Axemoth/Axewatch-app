package com.aistudio.axewatch.trader.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Registrar directory: which RTA handles which IPO, plus declared
 * allotment dates. Ports the Axewatch web backend's registrar_directory():
 *
 * - MUFG live company API + Bigshare public dropdowns (3 mirrors) are
 *   AUTHORITATIVE (never replaced by weaker sources).
 * - ipomarket.in + IPOWatch allotment tables fill the rest — notably
 *   KFintech and the smaller SME registrars, which publish no company
 *   list — and backfill declared allotment dates.
 * - Individual source failures are tolerated: attribution just gets
 *   sparser, checks still run.
 *
 * Parsers are pure (regex + string ops, no Android APIs) so they are
 * unit-tested against saved table shapes. Network lives in [fetchers].
 */
data class DirectoryEntry(
    /** "mufg" | "kfin" | "bigshare" | "skyline" | "cameo" | "maashitla" | "purva" | "beetal" */
    val registrar: String,
    val name: String,
    /** Basis-of-allotment date as published ("18 Sep 2026", "" = unknown). */
    val allotmentDate: String = "",
    /** MUFG API / Bigshare dropdowns. Non-authoritative rows never win. */
    val authoritative: Boolean = false
) {
    fun displayName(): String = when (registrar) {
        "mufg" -> "MUFG Intime"
        "kfin" -> "KFintech"
        "bigshare" -> "Bigshare"
        "skyline" -> "Skyline"
        "cameo" -> "Cameo"
        "maashitla" -> "Maashitla"
        "purva" -> "Purva Sharegistry"
        "beetal" -> "Beetal Financial"
        else -> "Unknown"
    }

    /** Only MUFG + KFintech have automated PAN search on-device. */
    fun automated(): Boolean = registrar == "mufg" || registrar == "kfin"
}

object RegistrarDirectory {

    /** Refuse ambiguous truncated names rather than route a PAN to another IPO. */
    fun lookup(directory: Map<String, DirectoryEntry>, name: String): DirectoryEntry? {
        val key = IpoAllotmentService.canonIpoName(name)
        if (key.isBlank()) return null
        directory[key]?.let { return it }
        val exact = directory.values.filter { IpoAllotmentService.canonIpoName(it.name) == key }
        if (exact.isNotEmpty()) return exact.singleOrNull()
        return directory.values.filter { IpoAllotmentService.ipoNamesMatch(it.name, name) }.singleOrNull()
    }

    const val IPOMARKET_URL = "https://ipomarket.in/allotment"
    const val IPOWATCH_ALLOT_URL = "https://ipowatch.in/ipo-allotment-status-how-to-check/"
    val BIGSHARE_URLS = listOf(
        "https://ipo.bigshareonline.com/ipo_status.html",
        "https://ipo1.bigshareonline.com/ipo_status.html",
        "https://ipo2.bigshareonline.com/ipo_status.html"
    )
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

    /** Mirror of the backend's _ipomarket_registrar_key(). Keep in sync. */
    fun registrarKeyFromText(text: String?): String? {
        val t = (text ?: "").lowercase()
        if ("mufg" in t || "link intime" in t || "linkintime" in t) return "mufg"
        if ("kfin" in t) return "kfin"
        if ("bigshare" in t || "big share" in t) return "bigshare"
        if ("skyline" in t) return "skyline"
        if ("cameo" in t) return "cameo"
        if ("purva" in t) return "purva"
        if ("maashitla" in t) return "maashitla"
        if ("beetal" in t) return "beetal"
        return null
    }

    /** Mirror of the backend's _registrar_key_from_href(). Keep in sync. */
    fun registrarKeyFromHref(url: String?): String? {
        val u = (url ?: "").lowercase()
        if ("bigshareonline.com" in u) return "bigshare"
        if ("kfintech.com" in u) return "kfin"
        if ("mpms.mufg.com" in u || "linkintime" in u) return "mufg"
        if ("maashitla.com" in u) return "maashitla"
        if ("purvashare.com" in u) return "purva"
        if ("skylinerta.com" in u) return "skyline"
        if ("cameoindia.com" in u) return "cameo"
        if ("beetalfinancial.com" in u) return "beetal"
        return null
    }

    private val TAG_RE = Regex("<[^>]+>")
    private val WS_RE = Regex("\\s+")

    /** Strip tags + a small set of entities (enough for tracker tables). */
    fun cellText(html: String): String {
        var s = TAG_RE.replace(html, "")
        s = s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
        return WS_RE.replace(s, " ").trim()
    }

    private fun tableCells(rowHtml: String): List<String> {
        val cellRe = Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        return cellRe.findAll(rowHtml).map { cellText(it.groupValues[1]) }.toList()
    }

    private fun tableRows(tableHtml: String): List<String> {
        val rowRe = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        return rowRe.findAll(tableHtml).map { it.groupValues[1] }.toList()
    }

    /**
     * ipomarket.in/allotment: <h3> sections each followed by a table whose
     * rows are [company, allotment date, registrar]. Pure, unit-tested.
     */
    fun parseIpomarket(html: String): List<DirectoryEntry> {
        val out = mutableListOf<DirectoryEntry>()
        val sectionRe = Regex("<h3[^>]*>", RegexOption.IGNORE_CASE)
        val parts = sectionRe.split(html)
        if (parts.size < 2) return out
        val tableRe = Regex("<table[^>]*>(.*?)</table>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        for (section in parts.drop(1)) {
            val table = tableRe.find(section)?.groupValues?.get(1) ?: continue
            for (row in tableRows(table)) {
                val cells = tableCells(row)
                if (cells.size >= 3 && "company" !in cells[0].lowercase()) {
                    val key = registrarKeyFromText(cells[2])
                    if (key != null) {
                        out.add(DirectoryEntry(key, cells[0], cells[1]))
                    }
                }
            }
        }
        return out
    }

    /**
     * IPOWatch allotment guide: only tables shaped
     * [IPO, IPO Date, Allotment Date, Allotment Status]. The strict header
     * guard matters — a loose check once admitted a GMP-style table and
     * mapped junk on the backend. Pure, unit-tested.
     */
    fun parseIpowatch(html: String): List<DirectoryEntry> {
        val out = mutableListOf<DirectoryEntry>()
        val tableRe = Regex("<table[^>]*>(.*?)</table>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val hrefRe = Regex("href=\"([^\"]+)\"")
        for (table in tableRe.findAll(html).map { it.groupValues[1] }) {
            val rows = tableRows(table)
            if (rows.isEmpty()) continue
            val hdr = tableCells(rows[0]).map { it.lowercase() }
            if (hdr.size < 4 || hdr[0] != "ipo" || "allot" !in hdr[2]) continue
            for (row in rows.drop(1)) {
                val rawCells = Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                    .findAll(row).map { it.groupValues[1] }.toList()
                if (rawCells.size < 4) continue
                val name = cellText(rawCells[0])
                val date = cellText(rawCells[2])
                val key = hrefRe.findAll(rawCells[3]).map { it.groupValues[1] }
                    .firstNotNullOfOrNull { registrarKeyFromHref(it) }
                    ?: registrarKeyFromText(cellText(rawCells[3]))
                if (name.isNotEmpty() && key != null) {
                    out.add(DirectoryEntry(key, name, date))
                }
            }
        }
        return out
    }

    /** Bigshare public company dropdown (captcha guards search, not this). */
    fun parseBigshareOptions(html: String): List<Pair<String, String>> {
        val rawSel = Regex("<select[^>]*id=\"ddlCompany\"[^>]*>(.*?)</select>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(html)?.groupValues?.get(1) ?: return emptyList()
        val sel = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL).replace(rawSel, "")
        val optRe = Regex("<option[^>]*value=\"([^\"]*)\"[^>]*>([^<]{2,100})</option>")
        return optRe.findAll(sel).mapNotNull { m ->
            val v = m.groupValues[1].trim()
            val n = m.groupValues[2].trim()
            if (v.isNotEmpty() && v != "0" && n.isNotEmpty() && "select" !in n.lowercase()) v to n else null
        }.toList()
    }

    /**
     * Merge rows into the directory keyed by canon name. Authoritative
     * entries (MUFG API, Bigshare dropdowns) are never replaced; weaker
     * sources only fill gaps and backfill missing allotment dates.
     */
    fun mergeInto(
        directory: MutableMap<String, DirectoryEntry>,
        entries: List<DirectoryEntry>,
        canon: (String) -> String
    ) {
        for (e in entries) {
            val key = canon(e.name)
            if (key.isEmpty() || e.registrar.isEmpty()) continue
            val cur = directory[key]
            if (cur == null || (e.authoritative && !cur.authoritative)) {
                directory[key] = e
            } else if (e.allotmentDate.isNotBlank() && cur.allotmentDate.isBlank()) {
                directory[key] = cur.copy(allotmentDate = e.allotmentDate)
            }
        }
    }

    // ---- Network (thin wrappers; failures are the caller's to tolerate) ----

    private fun sharedClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun fetchText(url: String, client: OkHttpClient = sharedClient()): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string() ?: throw IllegalStateException("empty body")
            }
        }
}
