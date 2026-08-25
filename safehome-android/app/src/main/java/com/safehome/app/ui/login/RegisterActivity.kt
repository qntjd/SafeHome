package com.safehome.app.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.safehome.app.SafeHomeApp
import com.safehome.app.api.AuthApi
import com.safehome.app.api.RetrofitClient
import com.safehome.app.databinding.ActivityRegisterBinding
import com.safehome.app.model.RegisterRequest
import com.safehome.app.ui.home.HomeActivity
import android.content.Intent
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authApi by lazy { RetrofitClient.create(AuthApi::class.java) }
    private val tokenManager by lazy { (application as SafeHomeApp).tokenManager }
    private var isEmailVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                showError("이메일을 입력해주세요.")
                return@setOnClickListener
            }
            sendVerificationCode(email)
        }

        binding.btnVerifyCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val code = binding.etVerifyCode.text.toString().trim()
            if (code.isEmpty()) {
                showError("인증코드를 입력해주세요.")
                return@setOnClickListener
            }
            verifyCode(email, code)
        }

        binding.btnRegister.setOnClickListener {
            if (!isEmailVerified) {
                showError("이메일 인증을 완료해주세요.")
                return@setOnClickListener
            }
            val email    = binding.etEmail.text.toString().trim()
            val nickname = binding.etNickname.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            when {
                email.isEmpty()     -> showError("이메일을 입력해주세요.")
                nickname.isEmpty()  -> showError("닉네임을 입력해주세요.")
                password.length < 8 -> showError("비밀번호는 8자 이상 입력해주세요.")
                else -> register(email, nickname, password)
            }
        }

        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun sendVerificationCode(email: String) {
        binding.btnSendCode.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = authApi.sendVerificationCode(mapOf("email" to email))
                if (response.isSuccessful) {
                    binding.layoutVerifyCode.visibility = View.VISIBLE
                    Toast.makeText(this@RegisterActivity, "인증코드를 발송했어요!", Toast.LENGTH_SHORT).show()
                } else {
                    showError("인증코드 발송에 실패했습니다.")
                }
            } catch (_: Exception) {
                showError("서버 연결에 실패했습니다.")
            } finally {
                binding.btnSendCode.isEnabled = true
            }
        }
    }

    private fun verifyCode(email: String, code: String) {
        binding.btnVerifyCode.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = authApi.verifyCode(
                    mapOf("email" to email, "code" to code)
                )
                if (response.isSuccessful && response.body()?.data == true) {
                    isEmailVerified = true
                    binding.layoutVerifyCode.visibility = View.GONE
                    binding.tvEmailVerified.visibility = View.VISIBLE
                    Toast.makeText(this@RegisterActivity, "이메일 인증 완료!", Toast.LENGTH_SHORT).show()
                } else {
                    showError("인증코드가 올바르지 않습니다.")
                }
            } catch (_: Exception) {
                showError("서버 연결에 실패했습니다.")
            } finally {
                binding.btnVerifyCode.isEnabled = true
            }
        }
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
                    showError(response.body()?.message ?: "회원가입에 실패했습니다.")
                }
            } catch (_: Exception) {
                showError("서버 연결에 실패했습니다.")
            } finally {
                binding.btnRegister.isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}