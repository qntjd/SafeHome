package com.safehome.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.safehome.app.R
import com.safehome.app.ui.LockScreenActivity

object LockScreenNotificationHelper {

    private const val CHANNEL_ID = "lockscreen_sos_channel"
    private const val NOTIFICATION_ID = 9999

    fun show(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "잠금화면 SOS",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "잠금화면에 SOS 버튼을 표시합니다."
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(context, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🚨 긴급 SOS")
            .setContentText("탭하면 즉시 SOS가 발동됩니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun hide(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
    }
}