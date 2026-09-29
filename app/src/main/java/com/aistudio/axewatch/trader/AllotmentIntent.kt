package com.aistudio.axewatch.trader

import android.content.Intent
import com.aistudio.axewatch.trader.data.work.AllotWatcherWorker

/** Activity recreation may replay its launch intent; remove only the handled command. */
internal fun consumeAllotmentIntent(intent: Intent?): Boolean {
    if (intent?.getStringExtra(AllotWatcherWorker.EXTRA_OPEN_TAB) != AllotWatcherWorker.TAB_ALLOTMENT) return false
    intent.removeExtra(AllotWatcherWorker.EXTRA_OPEN_TAB)
    return true
}
