package com.safehome.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.safehome.app.SafeHomeApp
import com.safehome.app.databinding.ActivityHomeBinding
import com.safehome.app.service.VoiceDetectionService
import com.safehome.app.ui.login.LoginActivity
import com.safehome.app.ui.settings.SettingsActivity
import com.safehome.app.ui.trip.TripActivity
import com.safehome.app.service.TripTrackingService
import com.safehome.app.util.SmsHelper
import com.safehome.app.util.LockScreenNotificationHelper
import com.safehome.app.util.AudioRecordHelper
import com.safehome.app.ui.map.MapActivity
import com.safehome.app.ui.news.NewsActivity
import com.safehome.app.ui.crime.CrimeStatsActivity
import android.content.pm.PackageManager
import android.Manifest
import com.safehome.app.ui.alert.AlertActivity
import com.safehome.app.util.NightModeManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.safehome.app.api.AlertApi
import com.safehome.app.api.RetrofitClient
import com.safehome.app.model.SubscribeRequest


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val tokenManager by lazy { (application as SafeHomeApp).tokenManager }

    private val alertApi by lazy { RetrofitClient.create(AlertApi::class.java) }
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupProfile()
        setupDrawer()
        setupUI()
        requestPermissions()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        registerMyLocationIfNeeded()
        restoreLockScreenSos()
        applyNightMode()
        showSosGuideIfFirst()

        if (tokenManager.isLockScreenSosEnabled()) {
            LockScreenNotificationHelper.show(this)
        }

        if (intent.getBooleanExtra("sos_triggered", false)) {
            showSosDialog()
        }
    }

    private fun setupProfile() {
        val nickname = tokenManager.getNickname() ?: "사용자"
        val email    = tokenManager.getEmail() ?: ""

        binding.tvGreeting.text = "${nickname}님, 안녕하세요!"
        binding.tvDrawerNickname.text = nickname
        binding.tvDrawerEmail.text = email
        binding.tvDrawerInitial.text = nickname.firstOrNull()?.uppercase() ?: "S"
    }

    private fun setupDrawer() {
        // 프로필 버튼 → 사이드바 열기
        binding.btnProfile.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        // 설정 메뉴
        binding.drawerMenuSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 닉네임 변경
        binding.drawerMenuNickname.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            showNicknameDialog()
        }

        // 비상연락처
        binding.drawerMenuContacts.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 로그아웃
        binding.drawerMenuLogout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃") { _, _ ->
                    stopService(Intent(this, VoiceDetectionService::class.java))
                    tokenManager.clear()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("취소", null)
                .show()
        }

    }

    private fun showNicknameDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etNickname = EditText(this).apply {
            hint = "새 닉네임"
            setText(tokenManager.getNickname())
            setTextColor(0xFFF0F2F8.toInt())
            setHintTextColor(0xFF555A70.toInt())
        }
        dialogView.addView(etNickname)

        AlertDialog.Builder(this)
            .setTitle("닉네임 변경")
            .setView(dialogView)
            .setPositiveButton("변경") { _, _ ->
                val newNickname = etNickname.text.toString().trim()
                if (newNickname.isNotEmpty()) {
                    tokenManager.saveNickname(newNickname)
                    setupProfile()
                    Toast.makeText(this, "닉네임이 변경됐어요!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupUI() {
        binding.cardTrip.setOnClickListener {
            startActivity(Intent(this, TripActivity::class.java))
        }
        binding.cardMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        binding.cardAlert.setOnClickListener {
            startActivity(Intent(this, AlertActivity::class.java))
        }
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.cardNews?.setOnClickListener {
            startActivity(Intent(this, NewsActivity::class.java))
        }
        binding.btnSos.setOnClickListener {
            startActivity(Intent(this, com.safehome.app.ui.guide.GuideActivity::class.java))
        }

        binding.btnNormalGuide.setOnClickListener {
            startActivity(Intent(this, com.safehome.app.ui.guide.NormalGuideActivity::class.java))
        }

        binding.cardSafetyScore.setOnClickListener {
            val intent = Intent(this, com.safehome.app.ui.crime.CrimeStatsActivity::class.java)
            intent.putExtra("districtCode", "22")  // 대구 시도코드, 추후 동적으로 변경 가능
            startActivity(intent)
        }
    }

    private fun showSosDialog() {
        AlertDialog.Builder(this)
            .setTitle("🚨 긴급 SOS")
            .setMessage(
                "정말 위급한 상황인가요?\n\n" +
                        "확인 시 비상연락처로 즉시 문자가 발송되고\n" +
                        "112 신고 화면이 열립니다.\n\n" +
                        "⚠️ 오발신 시 비상연락처에 혼란을 줄 수 있습니다."
            )
            .setPositiveButton("🚨 SOS 발동") { _, _ ->
                val recordingPath = AudioRecordHelper.startRecording(this)
                val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:112"))
                startActivity(intent)
                sendSosAlerts(recordingPath)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun sendSosAlerts(recordingPath: String? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            val lat = TripTrackingService.currentLat
            val lng = TripTrackingService.currentLng
            val address = SmsHelper.getAddress(lat, lng)
            val nickname = tokenManager.getNickname() ?: "사용자"

            tokenManager.getContacts().forEach { (_, phone) ->
                SmsHelper.sendSosAlert(
                    this@HomeActivity, nickname, phone, lat, lng, address, recordingPath
                )
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)

        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), 100
            )
        } else {
            startVoiceDetection()
        }
    }

    private fun registerMyLocationIfNeeded() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {

                val subsResponse = alertApi.getSubscriptions()
                val existing = subsResponse.body()?.data ?: emptyList()
                if (existing.any { it.isMyLocation}) return@launch

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if(location == null) return@addOnSuccessListener

                    lifecycleScope.launch(Dispatchers.IO){
                        try {
                            val address = SmsHelper.getAddress(location.latitude, location.longitude)
                            val parts = address.split(" ")
                            val sido = parts.getOrNull(0) ?: return@launch
                            val sigungu = parts.getOrNull(1)

                            alertApi.subscribe(
                                SubscribeRequest(
                                    sidoName = sido,
                                    sigunguName = sigungu,
                                    label = "내 위치",
                                    isMyLocation = true,
                                    category = "OTHER"
                                )
                            )
                        } catch (_: Exception){

                        }
                    }
                }
            } catch (_: Exception){}
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
            startVoiceDetection()
        }
    }
    private fun restoreLockScreenSos() {
        if (tokenManager.isLockScreenSosEnabled()) {
            LockScreenNotificationHelper.show(this)
        }
    }

    private fun startVoiceDetection() {
        startForegroundService(Intent(this, VoiceDetectionService::class.java))
    }

    private fun applyNightMode() {
        if (!tokenManager.isNightModeEnabled()) return
        if (!NightModeManager.isNightTime()) return

        // 화면 밝기 낮추기
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.3f  // 30% 밝기
        window.attributes = layoutParams

        // UI 색상 보라색으로 변경
        binding.drawerLayout.setBackgroundColor(android.graphics.Color.parseColor("#0D0A1A"))
    }

    private fun showSosGuideIfFirst() {
        val prefs = getSharedPreferences("safehome_guide", MODE_PRIVATE)
        if (prefs.getBoolean("sos_guide_shown", false)) return

        AlertDialog.Builder(this)
            .setTitle("🚨 SOS 기능 안내")
            .setMessage(
                "SafeHome의 SOS 기능은 생명이 위급한 긴급 상황을 위한 기능입니다.\n\n" +
                        "⚠️ 오발송 주의\n" +
                        "SOS 발동 시 등록된 비상연락처로 즉시 문자가 발송되고 112 신고가 연결됩니다.\n\n" +
                        "생명에 위급한 상황이 아니거나 테스트 목적이라면 " +
                        "음성 감지 SOS 기능을 꺼놓는 것을 권장합니다.\n\n" +
                        "설정 → 음성 SOS 감지 OFF"
            )
            .setPositiveButton("확인했습니다") { _, _ ->
                prefs.edit().putBoolean("sos_guide_shown", true).apply()
            }
            .setCancelable(false)
            .show()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}