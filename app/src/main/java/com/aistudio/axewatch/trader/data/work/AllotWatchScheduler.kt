package com.aistudio.axewatch.trader.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Declaration-watcher scheduling. Gentle by design: one periodic run every
 * hour, only with connectivity and a non-low battery, and each run
 * short-circuits to zero network when there are no saved PANs, no cached
 * IPO snapshot, or nothing declared. KEEP policy never stacks duplicates.
 */
object AllotWatchScheduler {
    const val UNIQUE_WORK = "allot_declaration_watch"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    private fun request() = PeriodicWorkRequestBuilder<AllotWatcherWorker>(1, TimeUnit.HOURS)
        .setConstraints(constraints)
        .addTag("allotment")
        .build()

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, request()
        )
    }

    /** Recheck soon after an in-app IPO refresh, capped to avoid portal bursts. */
    fun runNow(context: Context, feedChanged: Boolean = false) {
        if (!AllotWatcherWorker.isEnabled(context)) return
        val prefs = context.getSharedPreferences("axewatch_settings", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!feedChanged && now - prefs.getLong("allot_watch_last_enqueued", 0L) < 15 * 60_000L) return
        prefs.edit().putLong("allot_watch_last_enqueued", now).apply()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "allot_declaration_refresh", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AllotWatcherWorker>()
                .setConstraints(constraints).addTag("allotment").build()
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        WorkManager.getInstance(context).cancelUniqueWork("allot_declaration_refresh")
    }
}
