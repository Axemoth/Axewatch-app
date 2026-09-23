package com.aistudio.axewatch.trader.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Declaration-watcher scheduling. Gentle by design: one periodic run every
 * 6 hours, only with connectivity and a non-low battery, and each run
 * short-circuits to zero network when there are no saved PANs, no cached
 * IPO snapshot, or nothing declared. KEEP policy never stacks duplicates.
 */
object AllotWatchScheduler {
    const val UNIQUE_WORK = "allot_declaration_watch"

    private fun request() = PeriodicWorkRequestBuilder<AllotWatcherWorker>(6, TimeUnit.HOURS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .addTag("allotment")
        .build()

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request()
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }
}
