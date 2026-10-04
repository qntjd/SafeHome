package com.safehome.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.safehome.app.R
import com.safehome.app.SafeHomeApp
import com.safehome.app.api.AuthApi
import com.safehome.app.api.RetrofitClient
import com.safehome.app.databinding.ActivityRegisterBinding
import com.safehome.app.model.RegisterRequest
import com.safehome.app.ui.home.HomeActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authApi by lazy { RetrofitClient.create(AuthApi::class.java) }
    private val tokenManager by lazy { (application as SafeHomeApp).tokenManager }

    private var isEmailVerified = false
    private var passwordVisible = false
    private var resendTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.tvLogin.setOnClickListener { finish() }

        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            when {
                email.isEmpty() -> showError("이메일을 입력해주세요.")
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    showError("이메일 형식을 확인해주세요.")
                else -> sendVerificationCode(email)
            }
        }

        binding.btnVerifyCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val code = binding.etVerifyCode.text.toString().trim()
            if (code.length != 6) {
                showError("인증번호 6자리를 입력해주세요.")
                return@setOnClickListener
            }
            verifyCode(email, code)
        }

        // 6자리를 다 입력하면 키보드의 완료로 바로 확인
        binding.etVerifyCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnVerifyCode.performClick(); true
            } else false
        }

        binding.btnTogglePassword.setOnClickListener { togglePasswordVisibility() }

        binding.etPasswordConfirm.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnRegister.performClick(); true
            } else false
        }

        // 입력을 고치기 시작하면 오류 문구를 지움
        listOf(binding.etEmail, binding.etVerifyCode, binding.etNickname,
            binding.etPassword, binding.etPasswordConfirm).forEach { field ->
            field.doAfterTextChanged { hideError() }
        }

        binding.btnRegister.setOnClickListener { submit() }
    }

    private fun submit() {
        val email = binding.etEmail.text.toString().trim()
        val nickname = binding.etNickname.text.toString().trim()
        // 비밀번호는 trim 하지 않음: 공백은 아래에서 명시적으로 막음
        val password = binding.etPassword.text.toString()
        val confirm = binding.etPasswordConfirm.text.toString()

        when {
            !isEmailVerified -> showError("이메일 인증을 먼저 완료해주세요.")
            nickname.isEmpty() -> showError("닉네임을 입력해주세요.", binding.etNickname)
            password.any { it.isWhitespace() } ->
                showError("비밀번호에는 공백을 쓸 수 없어요.", binding.etPassword)
            password.length < 8 ->
                showError("비밀번호는 8자 이상 입력해주세요.", binding.etPassword)
            password != confirm ->
                showError("비밀번호가 서로 달라요. 다시 확인해주세요.", binding.etPasswordConfirm)
            else -> register(email, nickname, password)
        }
    }

    private fun sendVerificationCode(email: String) {
        binding.btnSendCode.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = authApi.sendVerificationCode(mapOf("email" to email))
                if (response.isSuccessful) {
                    binding.layoutVerifyCode.visibility = View.VISIBLE
                    binding.etVerifyCode.requestFocus()
                    startResendCooldown()
                } else {
                    showError("인증번호를 보내지 못했어요. 이메일 주소를 확인해주세요.")
                    binding.btnSendCode.isEnabled = true
                }
            } catch (_: Exception) {
                showError("서버에 연결할 수 없어요. 인터넷 연결을 확인해주세요.")
                binding.btnSendCode.isEnabled = true
            }
        }
    }

    /** 메일 폭주를 막기 위해 30초 뒤에 다시 받을 수 있게 함 */
    private fun startResendCooldown() {
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(30_000L, 1_000L) {
            override fun onTick(ms: Long) {
                binding.btnSendCode.text = "${ms / 1000 + 1}초"
            }
            override fun onFinish() {
                binding.btnSendCode.text = getString(R.string.register_resend_code)
                binding.btnSendCode.isEnabled = true
            }
        }.start()
    }

    private fun verifyCode(email: String, code: String) {
        binding.btnVerifyCode.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = authApi.verifyCode(mapOf("email" to email, "code" to code))
                if (response.isSuccessful && response.body()?.data == true) {
                    onEmailVerified()
                } else {
                    showError("인증번호가 맞지 않거나 시간이 지났어요.", binding.etVerifyCode)
                }
            } catch (_: Exception) {
                showError("서버에 연결할 수 없어요. 인터넷 연결을 확인해주세요.")
            } finally {
                binding.btnVerifyCode.isEnabled = true
            }
        }
    }

    private fun onEmailVerified() {
        isEmailVerified = true
        resendTimer?.cancel()
        hideError()

        // 인증한 이메일을 바꾸지 못하게 잠금 (다른 주소로 가입하는 것 방지)
        binding.etEmail.isEnabled = false
        binding.etEmail.alpha = 0.7f
        binding.btnSendCode.visibility = View.GONE
        binding.layoutVerifyCode.visibility = View.GONE
        binding.tvEmailVerified.visibility = View.VISIBLE

        binding.etNickname.requestFocus()
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        val type = if (passwordVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // 두 칸을 함께 전환해서 확인 칸도 같이 보이게
        listOf(binding.etPassword, binding.etPasswordConfirm).forEach { field ->
            val cursor = field.selectionEnd
            val typeface = field.typeface
            field.inputType = type
            field.typeface = typeface
            field.setSelection(cursor.coerceAtLeast(0))
        }

        binding.btnTogglePassword.setImageResource(
            if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
        )
        binding.btnTogglePassword.contentDescription = getString(
            if (passwordVisible) R.string.login_hide_password else R.string.login_show_password
        )
    }

    private fun register(email: String, nickname: String, password: String) {
        binding.btnRegister.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = authApi.register(RegisterRequest(email, password, nickname, null, null))
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data!!
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    tokenManager.saveNickname(data.nickname)
                    tokenManager.saveEmail(data.email)
                    startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
                    finishAffinity()
                } else {
                    showError(response.body()?.message ?: "가입하지 못했어요. 잠시 후 다시 시도해주세요.")
                }
            } catch (_: Exception) {
                showError("서버에 연결할 수 없어요. 인터넷 연결을 확인해주세요.")
            } finally {
                binding.btnRegister.isEnabled = true
            }
        }
    }

    private fun showError(message: String, focus: EditText? = null) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        focus?.requestFocus()
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }
}