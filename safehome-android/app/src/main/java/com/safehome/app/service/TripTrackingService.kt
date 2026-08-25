package com.safehome.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.safehome.app.R
import com.safehome.app.SafeHomeApp
import com.safehome.app.ui.trip.TripActivity
import com.safehome.app.util.SmsHelper
import com.safehome.app.util.NightModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TripTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "trip_tracking_channel"
        const val NOTIFICATION_ID = 1001
        var currentLat = 0.0
        var currentLng = 0.0
        var tripId: String? = null
        var expectedArrivalAt: String? = null
        var nickname: String? = null
        var emergencyContacts: List<Pair<String, String>> = emptyList()
        var alertSent = false
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tripId = intent?.getStringExtra("tripId")
        expectedArrivalAt = intent?.getStringExtra("expectedArrivalAt")
        nickname = intent?.getStringExtra("nickname")
        alertSent = false

        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        startWatchdog()
        return START_STICKY
    }

    private fun startWatchdog() {
        serviceScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000) // 1분마다 체크
                checkArrival()
            }
        }
    }

    private suspend fun checkArrival() {
        if (alertSent) return
        val expected = expectedArrivalAt ?: return
        val now = java.time.LocalDateTime.now()

        try {
            val expectedTime = java.time.LocalDateTime.parse(expected)
            if (now.isAfter(expectedTime.plusMinutes(5))) {
                // 5분 초과 시 알림
                sendWatchdogAlert()
                alertSent = true
            }
        } catch (_: Exception) {}
    }

    private suspend fun sendWatchdogAlert() {
        val address = SmsHelper.getAddress(currentLat, currentLng)
        val tokenManager = (application as SafeHomeApp).tokenManager
        val nick = nickname ?: tokenManager.getNickname() ?: "사용자"

        emergencyContacts.forEach { (_, phone) ->
            SmsHelper.sendWatchdogAlert(
                this, nick, phone, currentLat, currentLng, address
            )
        }
    }

    private fun startLocationUpdates() {
        val tokenManager = (application as com.safehome.app.SafeHomeApp).tokenManager
        val interval = if (tokenManager.isNightModeEnabled() && NightModeManager.isNightTime()) {
            5000L  // 야간 5초
        } else {
            10000L // 주간 10초
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, interval
        ).setMinUpdateIntervalMillis(interval / 2).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    currentLat = it.latitude
                    currentLng = it.longitude
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (_: SecurityException) {}
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "안심 귀가 추적",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "귀가 중 위치를 추적합니다." }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, TripActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("안심 귀가 진행 중")
            .setContentText("위치를 추적하고 있어요. 안전하게 귀가하세요!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}