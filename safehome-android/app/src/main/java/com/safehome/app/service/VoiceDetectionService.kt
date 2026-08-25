package com.safehome.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.safehome.app.R
import com.safehome.app.ui.home.HomeActivity
import com.safehome.app.ui.LockScreenActivity
import com.safehome.app.util.AudioRecordHelper
import android.util.Log

class VoiceDetectionService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    companion object {
        const val CHANNEL_ID = "voice_detection_channel"
        const val NOTIFICATION_ID = 1002
        val SOS_KEYWORDS = listOf(
            "살려줘", "도와줘", "help", "위험해", "살려", "도움","살려주세요","도와주세요"
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startListening()
        return START_STICKY
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.forEach { text ->
                    val lower = text.lowercase()
                    if (SOS_KEYWORDS.any { keyword -> lower.contains(keyword) }) {
                        triggerSos()
                        return
                    }
                }
                // 계속 듣기
                if (isListening) restartListening()
            }

            override fun onError(error: Int) {
                // 오류 시 재시작
                if (isListening) restartListening()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun restartListening() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        android.os.Handler(mainLooper).postDelayed({
            if (isListening) startListening()
        }, 1000)
    }

    private fun triggerSos() {
        // 녹음 시작
        val filePath = AudioRecordHelper.startRecording(this)
        Log.d("VoiceDetectionService", "SOS 녹음 시작: $filePath")

        // 잠금화면 SOS 액티비티 시작
        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("recordingPath", filePath)
        }
        startActivity(lockIntent)

        // 홈 화면에도 브로드캐스트
        val broadcastIntent = Intent("com.safehome.app.SOS_TRIGGERED")
        sendBroadcast(broadcastIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "음성 SOS 감지",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "음성으로 SOS를 감지합니다."
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("음성 SOS 감지 중")
            .setContentText("위험 시 '살려줘', '도와줘'라고 말하세요")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}