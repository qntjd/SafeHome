package com.safehome.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.safehome.app.R
import com.safehome.app.util.VideoRecordHelper

class SosRecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "sos_recording_channel"
        const val NOTIFICATION_ID = 1003
        private const val MAX_DURATION_MS = 120_000L // 2분
    }

    private val stopHandler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        VideoRecordHelper.startRecording(applicationContext)

        // 2분 뒤 자동 종료 (MediaRecorder의 setMaxDuration과 이중 안전장치)
        stopHandler.postDelayed(stopRunnable, MAX_DURATION_MS)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SOS 영상 녹화",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "긴급 상황 시 영상을 녹화합니다."
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SOS 영상 녹화 중")
            .setContentText("긴급 상황을 위해 영상을 녹화하고 있어요.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHandler.removeCallbacks(stopRunnable)
        VideoRecordHelper.stopRecording()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}