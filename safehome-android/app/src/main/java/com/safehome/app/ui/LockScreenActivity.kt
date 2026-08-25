package com.safehome.app.ui

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.safehome.app.SafeHomeApp
import com.safehome.app.databinding.ActivityLockScreenBinding
import com.safehome.app.service.SosRecordingService
import com.safehome.app.service.TripTrackingService
import com.safehome.app.util.AudioRecordHelper
import com.safehome.app.util.SmsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding
    private val tokenManager by lazy { (application as SafeHomeApp).tokenManager }
    private var countDownTimer: CountDownTimer? = null
    private var recordingPath: String? = null

    companion object {
        private const val COUNTDOWN_SECONDS = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)

        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recordingPath = intent.getStringExtra("recordingPath")

        // 음성 단독 녹음은 멈추고, 영상(오디오 포함) 녹화 서비스 시작
        AudioRecordHelper.stopRecording()
        startForegroundService(Intent(this, SosRecordingService::class.java))

        // 즉시 112 신고 (사용자가 직접 누르면 카운트다운 무시하고 바로 실행)
        binding.btnSosCall.setOnClickListener {
            countDownTimer?.cancel()
            sendAlerts()
            callPolice()
            finish()
        }

        // 취소 (오감지) — 카운트다운 중단, 녹화도 즉시 중단, 아무것도 전송 안 함
        binding.btnDismiss.setOnClickListener {
            countDownTimer?.cancel()
            stopService(Intent(this, SosRecordingService::class.java))
            finish()
        }

        startCountdown()
    }

    private fun startCountdown() {
        val autoReportEnabled = tokenManager.isAutoPoliceReportEnabled()

        countDownTimer = object : CountDownTimer(COUNTDOWN_SECONDS * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                binding.tvCountdown.text = secondsLeft.toString()
                binding.tvSubtitle.text = if (autoReportEnabled) {
                    "${secondsLeft}초 후 비상연락처 알림 및 112 자동신고가 전송돼요"
                } else {
                    "${secondsLeft}초 후 비상연락처에 알림이 전송돼요"
                }
            }

            override fun onFinish() {
                binding.tvCountdown.text = "0"
                sendAlerts()
                if (autoReportEnabled) {
                    callPolice()
                }
                // 녹화는 중단하지 않음 — SosRecordingService가 독립적으로 최대 2분간 계속 녹화
                finish()
            }
        }.start()
    }

    private fun sendAlerts() {
        lifecycleScope.launch(Dispatchers.IO) {
            val lat = TripTrackingService.currentLat
            val lng = TripTrackingService.currentLng
            val address = SmsHelper.getAddress(lat, lng)
            val nickname = tokenManager.getNickname() ?: "사용자"

            tokenManager.getContacts().forEach { (_, phone) ->
                SmsHelper.sendSosAlert(
                    this@LockScreenActivity, nickname, phone, lat, lng, address, recordingPath
                )
            }
        }
    }

    private fun callPolice() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:01063744916"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:01063744916"))
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        // VideoRecordHelper.stopRecording() 호출 안 함 — 서비스가 독립적으로 계속 녹화
    }
}