package com.aistudio.axewatch.trader.data.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aistudio.axewatch.trader.MainActivity

import com.aistudio.axewatch.trader.data.work.AllotWatcherWorker.Companion.CHANNEL_ID
import com.aistudio.axewatch.trader.data.work.AllotWatcherWorker.Companion.EXTRA_OPEN_TAB
import com.aistudio.axewatch.trader.data.work.AllotWatcherWorker.Companion.TAB_ALLOTMENT
import com.aistudio.axewatch.trader.data.work.AllotWatcherWorker.Companion.TAG

object AllotNotifications {
    internal fun postNotification(context: Context, id: Int, title: String, body: String): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return false
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Allotment results", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_TAB, TAB_ALLOTMENT)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val note = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, note)
            return true
        } catch (e: SecurityException) {
            Log.w(TAG, "notification permission missing")
            return false
        }
    }
}
