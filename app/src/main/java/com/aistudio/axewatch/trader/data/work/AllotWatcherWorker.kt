package com.aistudio.axewatch.trader.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aistudio.axewatch.trader.data.local.AppDatabase
import com.aistudio.axewatch.trader.data.local.entity.maskMatches
import com.aistudio.axewatch.trader.data.model.allotNotifyKey
import com.aistudio.axewatch.trader.data.model.AllotNotifyText
import com.aistudio.axewatch.trader.data.model.computeDueIssues
import com.aistudio.axewatch.trader.data.model.pendingAllotNotifications
import com.aistudio.axewatch.trader.data.model.todayLooseDateKey
import com.aistudio.axewatch.trader.data.repository.AxewatchRepository

/**
 * Background declaration watcher: "results are out on the official
 * registrar" -> phone notification, without hammering any API.
 *
 * One run costs at most ONE registrar-directory refresh (the same light
 * tables the app already scrapes; 24h-cached per process) plus paced
 * MUFG/KFin lookups ONLY for declared issues x saved PANs lacking a
 * decisive record. Captcha-walled registrars are never queried — they
 * produce a tap-to-open nudge. Decided or already-notified pairs are
 * skipped, so a run is usually zero network beyond the directory.
 *
 * PII: logs carry counts only; notifications carry holder labels +
 * masked PANs, never full PANs.
 */
class AllotWatcherWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "AllotWatcher"
        const val CHANNEL_ID = "allot_results"
        const val EXTRA_OPEN_TAB = "open_tab"
        const val TAB_ALLOTMENT = "allotment"
        private const val SUMMARY_NOT_ALLOTTED_ID = 1002
        private const val SUMMARY_MANUAL_ID = 1003

        private fun notifiedPrefs(context: Context) =
            context.getSharedPreferences("axewatch_settings", Context.MODE_PRIVATE)

        fun isEnabled(context: Context): Boolean =
            notifiedPrefs(context).getBoolean("allot_watch_enabled", true)

        fun readNotified(context: Context): MutableSet<String> =
            notifiedPrefs(context).getStringSet("allot_watch_notified", emptySet())
                ?.toMutableSet() ?: mutableSetOf()

        fun writeNotified(context: Context, keys: Set<String>) {
            notifiedPrefs(context).edit().putStringSet("allot_watch_notified", keys).apply()
        }
    }

    override suspend fun doWork(): Result {
        if (!isEnabled(applicationContext)) return Result.success()
        return try {
            runOnce()
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Never retry-storm the registrars: the next periodic run (6h)
            // retries naturally. Transport failures persist as LOOKUP_FAILED
            // records for one-tap UI retry.
            Log.w(TAG, "watcher run failed")
            Result.success()
        }
    }

    private suspend fun runOnce() {
        val context = applicationContext
        val idMemory = context.getSharedPreferences("axewatch_id_memory", Context.MODE_PRIVATE)
        val repo = AxewatchRepository(AppDatabase.getDatabase(context), idMemory)

        val pans = repo.pansOnce()
        if (pans.isEmpty()) return
        val notified = readNotified(context)
        val records = repo.allotmentRecordsOnce()
        // A result saved before notification permission was granted is still
        // pending delivery. Replay it without querying the registrar again.
        for ((rec, pan) in pendingAllotNotifications(records, pans, notified)) {
            val key = allotNotifyKey(rec.ipoSymbol, pan.maskedPan)
            if (key in notified) continue
            val text = when (rec.status) {
                "ALLOTTED" -> AllotNotifyText.allotted(rec.ipoName, pan.holderName, pan.maskedPan, rec.sharesAllotted)
                "NOT_ALLOTTED" -> AllotNotifyText.notAllottedSummary(1)
                else -> continue
            }
            if (postNotification(key.hashCode(), text.first, text.second)) {
                notified.add(key)
                writeNotified(context, notified)
            }
        }
        val snapshot = repo.readIpoSnapshot()
        if (snapshot.isEmpty()) return

        // Single scheduled network cost of the run (besides due lookups).
        val dir = try {
            repo.registrarDirectory()
        } catch (e: Exception) {
            Log.w(TAG, "directory refresh failed")
            return
        }

        // maskMatches: pre-unification rows carry the old over-revealing
        // mask, so masked-string equality alone would re-check + re-notify
        // them forever.
        val decisiveRecords = records
            .filter { it.status == "ALLOTTED" || it.status == "NOT_ALLOTTED" }
        fun isDecided(symbol: String, vaultMasked: String): Boolean =
            decisiveRecords.any { it.ipoSymbol == symbol && maskMatches(vaultMasked, it.maskedPan) }
        val todayKey = todayLooseDateKey()

        // Symbol-level skips: every saved PAN decisive, or a manual nudge sent.
        // (Per-PAN keys must NOT skip a symbol for the remaining PANs.)
        val fullyDecided = snapshot
            .map { it.symbol }
            .filter { symbol -> pans.all { isDecided(symbol, it.maskedPan) } }
            .toSet()
        val manualNudged = notified
            .filter { it.endsWith("|MANUAL") }
            .map { it.substringBefore("|") }
            .toSet()
        val due = computeDueIssues(
            snapshot = snapshot,
            dir = dir,
            decidedSymbols = fullyDecided,
            notifiedSymbols = manualNudged,
            todayKey = todayKey
        )
        if (due.auto.isEmpty() && due.manual.isEmpty()) return

        var changed = false

        // Declared + automated: check the saved family PANs (existing pacing).
        val freshNotAllotted = mutableListOf<Triple<String, String, String>>()
        val notAllottedKeys = mutableSetOf<String>()
        for (issue in due.auto) {
            val undecided = pans.filter { pan ->
                val key = allotNotifyKey(issue.symbol, pan.maskedPan)
                !isDecided(issue.symbol, pan.maskedPan) && key !in notified
            }
            if (undecided.isEmpty()) continue
            val results = try {
                repo.checkBulkAllotment(
                    ipoSymbol = issue.symbol,
                    onlyPanNumbers = undecided.map { it.panNumber }.toSet(),
                    knownName = issue.name,
                    knownRegistrar = issue.registrar,
                    knownDeclared = true
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "bulk check failed")
                continue
            }
            for (rec in results) {
                val key = allotNotifyKey(rec.ipoSymbol, rec.maskedPan)
                if (key in notified) continue
                val pan = undecided.find { it.maskedPan == rec.maskedPan }
                val label = pan?.holderName ?: "Saved PAN"
                when (rec.status) {
                    "ALLOTTED" -> {
                        val (t, b) = AllotNotifyText.allotted(
                            rec.ipoName.ifBlank { issue.name },
                            label, rec.maskedPan, rec.sharesAllotted
                        )
                        if (postNotification(key.hashCode(), t, b)) {
                            notified.add(key); changed = true
                            writeNotified(context, notified)
                        }
                    }
                    "NOT_ALLOTTED" -> {
                        freshNotAllotted.add(Triple(rec.ipoName.ifBlank { issue.name }, label, rec.maskedPan))
                        notAllottedKeys.add(key)
                    }
                    // RESULTS_NOT_OUT / LOOKUP_FAILED / NOT_APPLIED / manual:
                    // no notify, no mark — a later run retries naturally.
                }
            }
        }
        if (freshNotAllotted.isNotEmpty()) {
            val (t, b) = AllotNotifyText.notAllottedSummary(freshNotAllotted.size)
            if (postNotification(SUMMARY_NOT_ALLOTTED_ID, t, b)) {
                notified.addAll(notAllottedKeys); changed = true
                writeNotified(context, notified)
            }
        }

        // Declared + manual portal: nudge once per issue, never queried.
        val nudges = due.manual.filter { issue ->
            val key = allotNotifyKey(issue.symbol, "MANUAL")
            if (key in notified) return@filter false
            // Skip when every saved PAN already has a decisive record here.
            val decidedHere = pans.count { pan ->
                decisiveRecords.any { it.ipoSymbol == issue.symbol && maskMatches(pan.maskedPan, it.maskedPan) }
            }
            if (decidedHere >= pans.size) return@filter false
            true
        }
        if (nudges.isNotEmpty()) {
            val first = nudges.first()
            val reg = first.registrar.ifBlank { "Registrar" }
            val delivered = if (nudges.size == 1) {
                val (t, b) = AllotNotifyText.manualNudge(first.name.ifBlank { first.symbol }, reg)
                postNotification(SUMMARY_MANUAL_ID, t, b)
            } else {
                postNotification(
                    SUMMARY_MANUAL_ID,
                    "Results out: ${nudges.size} IPOs",
                    "${first.name.ifBlank { first.symbol }} and ${nudges.size - 1} more need a portal check. Tap to open."
                )
            }
            if (delivered) {
                notified.addAll(nudges.map { allotNotifyKey(it.symbol, "MANUAL") })
                changed = true
            }
        }

        if (changed) writeNotified(context, notified)
        Log.d(TAG, "run done: auto=${due.auto.size} manual=${due.manual.size}")
    }

    private fun postNotification(id: Int, title: String, body: String): Boolean =
        AllotNotifications.postNotification(applicationContext, id, title, body)
}
