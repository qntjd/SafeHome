package com.safehome.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.safehome.app.SafeHomeApp
import com.safehome.app.api.AuthApi
import com.safehome.app.api.RetrofitClient
import com.safehome.app.databinding.ActivityLoginBinding
import com.safehome.app.model.LoginRequest
import com.safehome.app.ui.home.HomeActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authApi by lazy { RetrofitClient.create(AuthApi::class.java) }
    private val tokenManager by lazy { (application as SafeHomeApp).tokenManager }
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (tokenManager.isLoggedIn()) {
            goToHome()
            return
        }

        credentialManager = CredentialManager.create(this)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showError("이메일과 비밀번호를 입력해주세요.")
                return@setOnClickListener
            }
            login(email, password)
        }

        binding.btnGoogleLogin.setOnClickListener {
            startGoogleSignIn()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun startGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("946360271912-ji8inmbs2po1ctv99bnilugs3b6d96r3.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    Log.d("GoogleLogin", "idToken 받음: ${idToken.take(20)}...")
                    handleGoogleLogin(idToken)
                } else {
                    showError("구글 로그인에 실패했습니다.")
                }
            } catch (e: GetCredentialException) {
                Log.e("GoogleLogin", "GetCredentialException: ${e.message}")
                showError("구글 로그인에 실패했습니다.")
            } catch (e: Exception) {
                Log.e("GoogleLogin", "Exception: ${e.message}")
                showError("구글 로그인에 실패했습니다.")
            }
        }
    }

    private fun handleGoogleLogin(idToken: String) {
        binding.btnGoogleLogin.isClickable = false
        lifecycleScope.launch {
            try {
                Log.d("GoogleLogin", "API 요청 시작")
                val response = authApi.googleLogin(AuthApi.GoogleTokenRequest(idToken))
                Log.d("GoogleLogin", "응답 코드: ${response.code()}")
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data!!
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    tokenManager.saveNickname(data.nickname)
                    tokenManager.saveEmail(data.email)
                    goToHome()
                } else {
                    Log.e("GoogleLogin", "에러 바디: ${response.errorBody()?.string()}")
                    showError("구글 로그인에 실패했습니다.")
                }
            } catch (e: Exception) {
                Log.e("GoogleLogin", "예외: ${e.message}")
                showError("서버 연결에 실패했습니다.")
            } finally {
                binding.btnGoogleLogin.isClickable = true
            }
        }
    }

    private fun login(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = authApi.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data!!
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    tokenManager.saveNickname(data.nickname)
                    tokenManager.saveEmail(data.email)
                    goToHome()
                } else {
                    showError("이메일 또는 비밀번호가 올바르지 않습니다.")
                }
            } catch (_: Exception) {
                showError("서버 연결에 실패했습니다.")
            } finally {
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}