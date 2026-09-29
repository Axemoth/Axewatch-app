package com.aistudio.axewatch.trader.data.repository

import android.content.SharedPreferences
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.data.model.PastIpoItem
import com.aistudio.axewatch.trader.data.model.daysBetweenDateKeys
import com.aistudio.axewatch.trader.data.model.parseLooseDate
import com.aistudio.axewatch.trader.data.model.todayLooseDateKey
import com.aistudio.axewatch.trader.data.remote.IpoAllotmentService
import org.json.JSONArray
import org.json.JSONObject

/** A bounded local record of observed IPO metadata and measured subscription multiples. */
internal class IpoHistoryCache(private val prefs: SharedPreferences?) {
    private val key = "ipo_history_v1"
    private var history = read()

    fun issues(): List<IpoIssue> = history.values.map { it.first }

    fun record(issues: List<IpoIssue>, todayKey: Long = todayLooseDateKey()) {
        for (issue in issues) {
            val id = IpoAllotmentService.canonIpoName(issue.companyName)
            if (id.isBlank()) continue
            val old = history[id]?.first
            history[id] = (if (old == null) issue else mergeIssueHistory(old, issue)) to todayKey
        }
        // Recent closes remain available through the entire allotment/listing period.
        // The 60-day window exceeds the requested one-week subscription history.
        history = history.filterValues { (issue, seen) ->
            val close = parseLooseDate(issue.issueCloseDate, (todayKey / 10000).toInt())
            val anchor = if (close > 0 && close <= todayKey) close else seen
            daysBetweenDateKeys(anchor, todayKey) in 0..60
        }.toMutableMap()
        save()
    }

    private fun read(): MutableMap<String, Pair<IpoIssue, Long>> {
        val result = mutableMapOf<String, Pair<IpoIssue, Long>>()
        val raw = prefs?.getString(key, null) ?: return result
        try {
            val arr = JSONArray(raw)
            for (index in 0 until arr.length()) {
                val o = arr.optJSONObject(index) ?: continue
                val name = o.optString("name")
                val id = IpoAllotmentService.canonIpoName(name)
                if (id.isBlank()) continue
                val issue = IpoIssue(
                    symbol = o.optString("symbol"), companyName = name,
                    category = o.optString("category", "Unknown"),
                    status = o.optString("status"),
                    issueOpenDate = o.optString("open"), issueCloseDate = o.optString("close"),
                    priceBand = o.optString("band"), issuePrice = o.optDouble("price"),
                    lotSize = o.optInt("lot"), issueSizeCr = o.optDouble("size"),
                    registrar = o.optString("registrar", "Unknown"),
                    qibSub = o.optDouble("qib"), niiSub = o.optDouble("nii"),
                    shniSub = o.optDouble("shni"), bhniSub = o.optDouble("bhni"),
                    riiSub = o.optDouble("rii"), totalSub = o.optDouble("total"),
                    allotmentDate = o.optString("allotment")
                )
                result[id] = issue to o.optLong("seen")
            }
        } catch (_: Exception) {
            // Corrupt or older cache data never prevents a fresh network refresh.
        }
        return result
    }

    private fun save() {
        val store = prefs ?: return
        val arr = JSONArray()
        history.values.sortedByDescending { it.second }.take(200).forEach { (i, seen) ->
            arr.put(JSONObject()
                .put("name", i.companyName).put("symbol", i.symbol)
                .put("category", i.category).put("status", i.status)
                .put("open", i.issueOpenDate).put("close", i.issueCloseDate)
                .put("band", i.priceBand).put("price", i.issuePrice)
                .put("lot", i.lotSize).put("size", i.issueSizeCr)
                .put("registrar", i.registrar).put("allotment", i.allotmentDate)
                .put("qib", i.qibSub).put("nii", i.niiSub)
                .put("shni", i.shniSub).put("bhni", i.bhniSub)
                .put("rii", i.riiSub).put("total", i.totalSub)
                .put("seen", seen))
        }
        store.edit().putString(key, arr.toString()).apply()
    }
}

internal fun mergeIssueHistory(old: IpoIssue, live: IpoIssue): IpoIssue {
    val oldClose = parseLooseDate(old.issueCloseDate)
    val newOpen = parseLooseDate(live.issueOpenDate)
    if ((oldClose > 0 && newOpen > oldClose) ||
        (oldClose <= 0 && old.status.equals("Listed", true) &&
            old.issuePrice > 0 && live.issuePrice > 0 &&
            kotlin.math.abs(old.issuePrice - live.issuePrice) >= 0.01)) return live
    fun text(new: String, previous: String) = new.takeIf { it.isNotBlank() && it != "—" && it != "Unknown" } ?: previous
    fun measured(new: Double, previous: Double) = if (new > 0.0) new else previous
    return live.copy(
        symbol = text(live.symbol, old.symbol),
        category = text(live.category, old.category),
        issueOpenDate = text(live.issueOpenDate, old.issueOpenDate),
        issueCloseDate = text(live.issueCloseDate, old.issueCloseDate),
        priceBand = text(live.priceBand, old.priceBand),
        issuePrice = measured(live.issuePrice, old.issuePrice),
        lotSize = live.lotSize.takeIf { it > 0 } ?: old.lotSize,
        issueSizeCr = measured(live.issueSizeCr, old.issueSizeCr),
        registrar = text(live.registrar, old.registrar),
        allotmentDate = text(live.allotmentDate, old.allotmentDate),
        qibSub = measured(live.qibSub, old.qibSub),
        niiSub = measured(live.niiSub, old.niiSub),
        shniSub = measured(live.shniSub, old.shniSub),
        bhniSub = measured(live.bhniSub, old.bhniSub),
        riiSub = measured(live.riiSub, old.riiSub),
        totalSub = measured(live.totalSub, old.totalSub),
        status = if (old.status.equals("Listed", true) && live.status.equals("Active", true)) old.status else live.status
    )
}

/** Join only one matching issue; unrelated or ambiguous company records never share figures. */
internal fun mergePastIpoHistory(
    feed: List<PastIpoItem>, known: List<IpoIssue>, todayKey: Long = todayLooseDateKey()
): List<PastIpoItem> {
    fun matching(past: PastIpoItem): IpoIssue? {
        val canon = IpoAllotmentService.canonIpoName(past.companyName)
        val listed = parseLooseDate(past.listingDate, (todayKey / 10000).toInt())
        val candidates = known.filter { issue ->
            val opened = parseLooseDate(issue.issueOpenDate, (todayKey / 10000).toInt())
            listed <= 0 || opened <= 0 || listed >= opened
        }
        val exact = candidates.filter { IpoAllotmentService.canonIpoName(it.companyName) == canon }
        if (exact.size == 1) return exact.single()
        return if (exact.isEmpty()) candidates.filter { IpoAllotmentService.ipoNamesMatch(it.companyName, past.companyName) }.singleOrNull() else null
    }
    val enriched = feed.map { past ->
        val issue = matching(past) ?: return@map past
        past.copy(
            category = issue.category.takeIf { it == "SME" || it == "Mainboard" } ?: past.category,
            registrar = issue.registrar.takeIf { it.isNotBlank() && it != "Unknown" } ?: past.registrar,
            issueCloseDate = issue.issueCloseDate,
            qibSub = issue.qibSub, niiSub = issue.niiSub,
            shniSub = issue.shniSub, bhniSub = issue.bhniSub,
            riiSub = issue.riiSub,
            totalSub = past.totalSub.takeIf { it > 0.0 } ?: issue.totalSub
        )
    }.toMutableList()
    for (issue in known) {
        val close = parseLooseDate(issue.issueCloseDate, (todayKey / 10000).toInt())
        if (close <= 0 || daysBetweenDateKeys(close, todayKey) !in 1..60) continue
        if (enriched.any { IpoAllotmentService.canonIpoName(it.companyName) == IpoAllotmentService.canonIpoName(issue.companyName) }) continue
        enriched.add(PastIpoItem(
            symbol = issue.symbol, companyName = issue.companyName,
            issuePrice = issue.issuePrice, listingPrice = 0.0, listingGainPercent = 0.0,
            totalSub = issue.totalSub, category = issue.category,
            registrar = issue.registrar, issueCloseDate = issue.issueCloseDate,
            qibSub = issue.qibSub, niiSub = issue.niiSub,
            shniSub = issue.shniSub, bhniSub = issue.bhniSub, riiSub = issue.riiSub
        ))
    }
    return enriched
}
