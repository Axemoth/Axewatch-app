package com.aistudio.axewatch.trader.data.model

import com.aistudio.axewatch.trader.data.remote.DirectoryEntry
import com.aistudio.axewatch.trader.data.remote.IpoAllotmentService

/**
 * Declaration watcher: "results are out on the official registrar, go check".
 *
 * Smart-by-design (no constant API hammering):
 * - The ONLY scheduled network is one registrar-directory refresh per run
 *   (the same light tables the app already scrapes; MUFG/KFin lookups run
 *   solely for declared issues x saved PANs without a decisive record).
 * - Issues with a decisive saved outcome (ALLOTTED / NOT_ALLOTTED) or an
 *   already-sent notification are never re-queried or re-notified.
 * - Captcha-walled registrars (Bigshare, Skyline, Cameo, ...) are NEVER
 *   auto-queried: they produce a tap-to-open nudge, nothing more.
 *
 * Everything here is pure (no Android APIs) and unit-tested. PII rule:
 * notification text carries holder labels + masked PANs only — full PANs
 * never reach this layer (callers pass masked strings).
 */
data class WatchedIssue(
    val symbol: String,
    val name: String,
    val registrar: String,
    val allotmentDate: String = "",
    val status: String = ""
)

data class DueSet(
    /** Declared + auto-checkable (MUFG/KFin): run the saved family PANs. */
    val auto: List<WatchedIssue> = emptyList(),
    /** Declared + manual-handoff: nudge to open the portal. */
    val manual: List<WatchedIssue> = emptyList()
)

/** Notification dedupe key. `panPart` is a masked PAN, or "*" for issue-level nudges. */
fun allotNotifyKey(symbol: String, panPart: String): String = "$symbol|$panPart"

/** True when the registrar offers an automated on-device PAN search. */
fun registrarAutoCheckable(registrar: String): Boolean {
    val l = registrar.lowercase()
    return l == "mufg" || l == "kfin" || "mufg" in l || "intime" in l || l == "kfintech" || "kfin" in l
}

/**
 * Issues whose results are declared on the official registrar: a directory
 * allotment date at/past today (and not pre-bidding), an authoritative
 * directory listing, or an Allotted/Listed tracker status. Pure, tested.
 */
fun isDeclaredOut(
    issue: WatchedIssue,
    dir: Map<String, DirectoryEntry> = emptyMap(),
    todayKey: Long = todayLooseDateKey()
): Boolean {
    val canon = IpoAllotmentService.canonIpoName(issue.name.ifBlank { issue.symbol })
    val hit = if (canon.isNotEmpty()) {
        dir[canon] ?: dir.values.find { IpoAllotmentService.ipoNamesMatch(it.name, issue.name) }
    } else null
    if (hit != null && hit.authoritative) return true
    val s = issue.status.lowercase()
    if (s == "allotted" || s == "listed") return true
    val date = issue.allotmentDate.ifBlank { hit?.allotmentDate.orEmpty() }
    val key = parseLooseDate(date)
    if (key in 1..todayKey) {
        if (s == "forthcoming" || s == "upcoming" || s.contains("pre")) return false
        return true
    }
    return false
}

/**
 * Split declared issues into auto-checkable vs manual-nudge, dropping
 * anything already decided or already notified. `decidedSymbols` holds
 * symbols with a decisive saved record; per-PAN filtering happens in the
 * worker (records carry masked PANs). Pure, tested.
 */
fun computeDueIssues(
    snapshot: List<WatchedIssue>,
    dir: Map<String, DirectoryEntry> = emptyMap(),
    decidedSymbols: Set<String> = emptySet(),
    notifiedSymbols: Set<String> = emptySet(),
    todayKey: Long = todayLooseDateKey()
): DueSet {
    val auto = mutableListOf<WatchedIssue>()
    val manual = mutableListOf<WatchedIssue>()
    for (issue in snapshot) {
        if (issue.symbol.isBlank()) continue
        if (issue.symbol in decidedSymbols || issue.symbol in notifiedSymbols) continue
        if (!isDeclaredOut(issue, dir, todayKey)) continue
        // Fresh directory beats a possibly-stale snapshot registrar: an
        // "Unknown" snapshot row attributed to MUFG/KFin today auto-checks.
        val canon = IpoAllotmentService.canonIpoName(issue.name.ifBlank { issue.symbol })
        val hit = if (canon.isNotEmpty()) {
            dir[canon] ?: dir.values.find { IpoAllotmentService.ipoNamesMatch(it.name, issue.name) }
        } else null
        val effRegistrar = hit?.registrar?.takeIf { it.isNotBlank() } ?: issue.registrar
        val eff = if (effRegistrar != issue.registrar) issue.copy(registrar = effRegistrar) else issue
        if (registrarAutoCheckable(effRegistrar)) auto.add(eff) else manual.add(eff)
    }
    return DueSet(auto, manual)
}

/** Notification copy. Inputs are holder labels + masked PANs — never full PANs. */
object AllotNotifyText {
    fun allotted(issueName: String, holderLabel: String, maskedPan: String, shares: Int): Pair<String, String> =
        "IPO allotted: $issueName" to
            "$holderLabel ($maskedPan) got $shares share${if (shares == 1) "" else "s"}. Tap to view."

    fun notAllottedSummary(count: Int): Pair<String, String> =
        "IPO results declared" to
            "$count saved application${if (count == 1) " was" else "s were"} not allotted. Tap to review."

    fun manualNudge(issueName: String, registrar: String): Pair<String, String> =
        "Results out: $issueName" to
            "$registrar needs one manual check (captcha). Tap to open the Allotment tab."
}
