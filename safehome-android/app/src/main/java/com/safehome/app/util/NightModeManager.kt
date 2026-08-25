package com.safehome.app.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.safehome.app.R
import com.safehome.app.service.VoiceDetectionService
import java.util.Calendar

object NightModeManager {

    private const val CHANNEL_ID = "night_mode_channel"
    private const val NOTIFICATION_ID = 8888
    private const val PREF_NIGHT_MODE = "night_mode_enabled"

    fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 6
    }

    fun activate(context: Context) {
        val tokenManager = (context.applicationContext as com.safehome.app.SafeHomeApp).tokenManager
        if (!tokenManager.isNightModeEnabled()) return

        // 1. 음성 감지 자동 ON
        context.startForegroundService(Intent(context, VoiceDetectionService::class.java))

        // 2. 잠금화면 SOS 자동 ON
        LockScreenNotificationHelper.show(context)

        // 3. 진동/소리 강도 높이기
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)

        // 4. 야간 모드 알림 표시
        showNightModeNotification(context, true)
    }

    fun deactivate(context: Context) {
        // 야간 모드 알림 제거
        showNightModeNotification(context, false)
    }

    private fun showNightModeNotification(context: Context, active: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)

        if (!active) {
            manager.cancel(NOTIFICATION_ID)
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "야간 안전 모드",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "야간 안전 모드가 활성화되어 있습니다."
        }
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🌙 야간 안전 모드 활성화")
            .setContentText("음성 감지 및 잠금화면 SOS가 자동으로 켜졌어요.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun scheduleNightMode(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 오후 10시 알람
        scheduleAlarm(context, alarmManager, 22, 0, NightModeReceiver.ACTION_ACTIVATE)
        // 오전 6시 알람
        scheduleAlarm(context, alarmManager, 6, 0, NightModeReceiver.ACTION_DEACTIVATE)
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        hour: Int,
        minute: Int,
        action: String
    ) {
        val intent = Intent(context, NightModeReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            hour,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}