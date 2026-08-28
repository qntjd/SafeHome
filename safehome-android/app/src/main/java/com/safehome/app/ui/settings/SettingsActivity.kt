package com.safehome.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
        val context = this

        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 8)
        }

        val titleTv = TextView(context).apply {
            text = "비상연락처 추가"
            textSize = 18f
            setTextColor(0xFFF5F6FA.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 24)
        }

        fun makeInputField(hintText: String, inputType: Int, digitsOnly: Boolean = false): EditText {
            return EditText(context).apply {
                hint = hintText
                this.inputType = inputType
                textSize = 15f
                setTextColor(0xFFF5F6FA.toInt())
                setHintTextColor(0xFF8B90A7.toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#0E1526"))
                    cornerRadius = 24f
                }
                setPadding(36, 28, 36, 28)

                if (digitsOnly) {
                    keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
                    filters = arrayOf(android.text.InputFilter.LengthFilter(11))
                }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
                layoutParams = params
            }
        }

        val etName = makeInputField("이름 (예: 엄마)", android.text.InputType.TYPE_CLASS_TEXT)
        val etPhone = makeInputField("전화번호 (예: 01012345678)", android.text.InputType.TYPE_CLASS_PHONE, digitsOnly = true)

        dialogLayout.addView(titleTv)
        dialogLayout.addView(etName)
        dialogLayout.addView(etPhone)

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
            .setView(dialogLayout)
            .setPositiveButton("추가") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim().replace("-", "")

                when {
                    name.isEmpty() -> {
                        Toast.makeText(context, "이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                    }
                    phone.isEmpty() -> {
                        Toast.makeText(context, "전화번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                    }
                    !isValidPhoneNumber(phone) -> {
                        Toast.makeText(context, "올바른 휴대폰 번호 형식이 아니에요 (예: 01012345678)", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        contacts.add(Pair(name, phone))
                        tokenManager.saveContacts(contacts)
                        refreshContactList()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .create()

        dialog.show()

        // 다이얼로그 배경 및 버튼 색상 커스터마이징
        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#16203A"))
                cornerRadius = 32f
            }
        )
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#E8A33D"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#8B90A7"))
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // 010, 011, 016, 017, 018, 019로 시작하고 총 10~11자리 숫자
        val regex = Regex("^01[016789]\\d{7,8}$")
        return regex.matches(phone)
    }


    private fun setupSettings() {
        binding.switchVoice.isChecked = tokenManager.isVoiceDetectionEnabled()
        binding.switchVoice.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 켤 때 안내 팝업
                AlertDialog.Builder(this)
                    .setTitle("🎤 음성 SOS 감지 활성화")
                    .setMessage(
                        "음성 SOS 감지 기능을 켜시겠습니까?\n\n" +
                                "이 기능은 '살려줘', '도와줘' 등의 키워드를 감지하면 " +
                                "즉시 비상연락처로 문자를 발송하고 영상을 녹화합니다.\n\n" +
                                "⚠️ 주의사항\n" +
                                "• 오감지 시 비상연락처에 문자가 발송될 수 있습니다.\n" +
                                "• 생명에 위급한 상황이 아니면 기능을 꺼놓는 것을 권장합니다.\n" +
                                "• 배터리 소모가 증가할 수 있습니다."
                    )
                    .setPositiveButton("켜기") { _, _ ->
                        val missingPermissions = mutableListOf<String>()
                        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED){
                            missingPermissions.add(Manifest.permission.CAMERA)
                        }
                        if (missingPermissions.isNotEmpty()) {
                            ActivityCompat.requestPermissions(
                                this, missingPermissions.toTypedArray(), 400
                            )
                        }
                        tokenManager.saveVoiceDetectionEnabled(true)
                        startForegroundService(Intent(this, VoiceDetectionService::class.java))
                    }
                    .setNegativeButton("취소") { _, _ ->
                        binding.switchVoice.isChecked = false
                    }
                    .setCancelable(false)
                    .show()
            } else {
                tokenManager.saveVoiceDetectionEnabled(false)
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
                        val missingPermissions = mutableListOf<String>()
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                            != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(
                                this, arrayOf(Manifest.permission.CALL_PHONE), 200
                            )
                        }
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                            missingPermissions.add(Manifest.permission.CAMERA)
                        }
                        if(missingPermissions.isNotEmpty()) {
                            ActivityCompat.requestPermissions(
                                this, missingPermissions.toTypedArray(), 200
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

        binding.btnSosLog.setOnClickListener {
            startActivity(Intent(this, com.safehome.app.ui.sos.SosLogActivity::class.java))
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