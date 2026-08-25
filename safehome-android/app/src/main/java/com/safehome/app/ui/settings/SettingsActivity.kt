package com.safehome.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import com.safehome.app.SafeHomeApp
import com.safehome.app.databinding.ActivitySettingsBinding
import com.safehome.app.service.VoiceDetectionService
import com.safehome.app.ui.login.LoginActivity
import com.safehome.app.util.LockScreenNotificationHelper
import com.safehome.app.util.AudioRecordHelper
import com.safehome.app.util.NightModeManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val tokenManager by lazy { (application as SafeHomeApp).tokenManager }

    companion object {
        val contacts = mutableListOf<Pair<String, String>>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 저장된 연락처 불러오기
        contacts.clear()
        contacts.addAll(tokenManager.getContacts())

        setupProfile()
        setupContacts()
        setupSettings()
        setupAccount()
        setupRecordings()
    }


    private fun setupProfile() {
        val nickname = tokenManager.getNickname() ?: "사용자"
        val email = tokenManager.getEmail() ?: ""

        binding.tvNickname.text = nickname
        binding.tvEmail.text = email
        binding.tvProfileInitial.text = nickname.firstOrNull()?.uppercase() ?: "S"
    }

    private fun setupContacts() {
        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }
        refreshContactList()
    }

    private fun refreshContactList() {
        binding.layoutContacts.removeAllViews()
        contacts.forEachIndexed { index, (name, phone) ->
            val view = LayoutInflater.from(this)
                .inflate(android.R.layout.simple_list_item_2, binding.layoutContacts, false)

            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)
            text1.text = name
            text1.setTextColor(0xFFF0F2F8.toInt())
            text1.textSize = 15f
            text2.text = phone
            text2.setTextColor(0xFF555A70.toInt())
            text2.textSize = 13f
            view.setPadding(48, 24, 48, 24)
            view.setBackgroundColor(0x00000000)

            view.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("연락처 삭제")
                    .setMessage("${name}을 삭제할까요?")
                    .setPositiveButton("삭제") { _, _ ->
                        contacts.removeAt(index)
                        tokenManager.saveContacts(contacts)
                        refreshContactList()
                    }
                    .setNegativeButton("취소", null)
                    .show()
                true
            }

            binding.layoutContacts.addView(view)

            if (index < contacts.size - 1) {
                val divider = View(this)
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setMargins(48, 0, 48, 0) }
                divider.setBackgroundColor(0xFF0D0F1A.toInt())
                binding.layoutContacts.addView(divider)
            }
        }
    }

    private fun showAddContactDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val etName = EditText(this).apply {
            hint = "이름"
            setTextColor(0xFFF0F2F8.toInt())
            setHintTextColor(0xFF555A70.toInt())
        }
        val etPhone = EditText(this).apply {
            hint = "전화번호"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setTextColor(0xFFF0F2F8.toInt())
            setHintTextColor(0xFF555A70.toInt())
        }

        dialogView.addView(etName)
        dialogView.addView(etPhone)

        AlertDialog.Builder(this)
            .setTitle("연락처 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    contacts.add(Pair(name, phone))
                    tokenManager.saveContacts(contacts)  // 저장 추가
                    refreshContactList()
                }
            }
            .setNegativeButton("취소", null)
            .show()

    }


    private fun setupSettings() {
        binding.switchVoice.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 켤 때 안내 팝업
                AlertDialog.Builder(this)
                    .setTitle("🎤 음성 SOS 감지 활성화")
                    .setMessage(
                        "음성 SOS 감지 기능을 켜시겠습니까?\n\n" +
                                "이 기능은 '살려줘', '도와줘' 등의 키워드를 감지하면 " +
                                "즉시 비상연락처로 문자를 발송합니다.\n\n" +
                                "⚠️ 주의사항\n" +
                                "• 오감지 시 비상연락처에 문자가 발송될 수 있습니다.\n" +
                                "• 생명에 위급한 상황이 아니면 기능을 꺼놓는 것을 권장합니다.\n" +
                                "• 배터리 소모가 증가할 수 있습니다."
                    )
                    .setPositiveButton("켜기") { _, _ ->
                        startForegroundService(Intent(this, VoiceDetectionService::class.java))
                    }
                    .setNegativeButton("취소") { _, _ ->
                        binding.switchVoice.isChecked = false
                    }
                    .setCancelable(false)
                    .show()
            } else {
                stopService(Intent(this, VoiceDetectionService::class.java))
            }
        }

        binding.switchAutoPoliceReport.isChecked = tokenManager.isAutoPoliceReportEnabled()
        binding.switchAutoPoliceReport.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AlertDialog.Builder(this)
                    .setTitle("🚨 112 자동신고 활성화")
                    .setMessage(
                        "SOS 카운트다운이 끝나면 112로 자동 전화가 발신됩니다.\n\n" +
                                "⚠️ 주의사항\n" +
                                "• 오작동으로 인한 허위 신고는 법적 책임이 따를 수 있습니다.\n" +
                                "• 카운트다운 중 언제든 취소할 수 있습니다.\n" +
                                "• 확실하지 않다면 기능을 꺼두는 것을 권장합니다."
                    )
                    .setPositiveButton("켜기") { _, _ ->
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                            != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(
                                this, arrayOf(Manifest.permission.CALL_PHONE), 200
                            )
                        }
                        tokenManager.saveAutoPoliceReport(true)
                    }
                    .setNegativeButton("취소") { _, _ ->
                        binding.switchAutoPoliceReport.isChecked = false
                    }
                    .setCancelable(false)
                    .show()
            } else {
                tokenManager.saveAutoPoliceReport(false)
            }
        }

        binding.switchLockScreen.isChecked = tokenManager.isLockScreenSosEnabled()
        binding.switchLockScreen.setOnCheckedChangeListener { _, isChecked ->
            tokenManager.saveLockScreenSos(isChecked)
            if (isChecked) LockScreenNotificationHelper.show(this)
            else LockScreenNotificationHelper.hide(this)
        }

        // 야간 안전 모드 토글
        binding.switchNight.isChecked = tokenManager.isNightModeEnabled()
        binding.switchNight.setOnCheckedChangeListener { _, isChecked ->
            tokenManager.saveNightMode(isChecked)
            if (isChecked) {
                // 스케줄 등록
                NightModeManager.scheduleNightMode(this)
                // 지금 야간 시간이면 바로 활성화
                if (NightModeManager.isNightTime()) {
                    NightModeManager.activate(this)
                }
            } else {
                NightModeManager.deactivate(this)
            }
        }
    }

    private fun setupAccount() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnChangeNickname.setOnClickListener {
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
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }

        binding.btnLogout.setOnClickListener {
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

    private fun setupRecordings() {
        val recordings = AudioRecordHelper.getRecordings()

        if (recordings.isEmpty()) {
            binding.tvNoRecordings.visibility = View.VISIBLE
            return
        }

        binding.tvNoRecordings.visibility = View.GONE

        recordings.forEach { file ->
            val view = LayoutInflater.from(this)
                .inflate(android.R.layout.simple_list_item_2, binding.layoutRecordings, false)

            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)

            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.KOREA)
                .format(java.util.Date(file.lastModified()))

            text1.text = file.name
            text1.setTextColor(0xFFF0F2F8.toInt())
            text1.textSize = 14f

            text2.text = "$date · ${file.length() / 1024}KB"
            text2.setTextColor(0xFF555A70.toInt())
            text2.textSize = 12f

            view.setPadding(48, 24, 48, 24)
            view.setBackgroundColor(0x00000000)

            view.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("녹음 파일 삭제")
                    .setMessage("${file.name}을 삭제할까요?")
                    .setPositiveButton("삭제") { _, _ ->
                        AudioRecordHelper.deleteRecording(file)
                        setupRecordings()
                    }
                    .setNegativeButton("취소", null)
                    .show()
                true
            }

            binding.layoutRecordings.addView(view)
        }
    }
}