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
import com.safehome.app.api.RetrofitClient
import com.safehome.app.api.SosApi
import com.safehome.app.model.SosCreateLogRequest
import com.safehome.app.model.SosRecipientRequest
import com.safehome.app.util.SosLocationHelper
import kotlinx.coroutines.withContext


class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding
    private val tokenManager by lazy { (application as SafeHomeApp).tokenManager }
    private var countDownTimer: CountDownTimer? = null
    private var recordingPath: String? = null

    private val sosApi by lazy { RetrofitClient.create(SosApi::class.java) }

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
            callPolice()
            lifecycleScope.launch {
                sendAlerts()
                finish()
            }
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
                if (autoReportEnabled) {
                    callPolice()
                }
                lifecycleScope.launch {
                    sendAlerts()
                    finish()
                }


            }
        }.start()
    }

    private suspend fun sendAlerts() {
        withContext(Dispatchers.IO) {
            val location = SosLocationHelper.getCurrentLocation(this@LockScreenActivity)
            val lat = location?.latitude ?: TripTrackingService.currentLat
            val lng = location?.longitude ?: TripTrackingService.currentLng
            val address = SmsHelper.getAddress(lat, lng)
            val nickname = tokenManager.getNickname() ?: "사용자"

            val recipientResults = mutableListOf<SosRecipientRequest>()

            tokenManager.getContacts().forEach { (name, phone) ->
                val (success, error) = SmsHelper.sendSosAlert(
                    this@LockScreenActivity, nickname, phone, lat, lng, address, recordingPath
                )
                recipientResults.add(
                    SosRecipientRequest(
                        contactName = name,
                        phoneNumber = phone,
                        status = if (success) "SUCCESS" else "FAILED",
                        errorMessage = error
                    )
                )
            }

            // 서버에 이력 보고
            try {
                sosApi.createLog(
                    SosCreateLogRequest(
                        triggerType = "LOCK_SCREEN",
                        lat = lat,
                        lng = lng,
                        address = address,
                        policeReported = tokenManager.isAutoPoliceReportEnabled(),
                        recipients = recipientResults
                    )
                )
            } catch (_: Exception) {
                // 로그 보고 실패해도 SOS 자체는 이미 발송된 상태라 무시
            }
        }
    }

    private fun callPolice() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:112"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        // VideoRecordHelper.stopRecording() 호출 안 함 — 서비스가 독립적으로 계속 녹화
    }
}