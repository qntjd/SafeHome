package com.safehome.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.safehome.app.R
import com.safehome.app.SafeHomeApp
import com.safehome.app.databinding.ActivitySplashBinding
import com.safehome.app.ui.home.HomeActivity
import com.safehome.app.ui.login.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val tokenManager by lazy { (application as SafeHomeApp).tokenManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        showCircleAndLogo()
    }

    private fun showCircleAndLogo() {
        // 원 드로잉 애니메이션
        binding.ivCircleRing.visibility = View.VISIBLE
        val circleDrawable = binding.ivCircleRing.drawable
        if (circleDrawable is android.graphics.drawable.Animatable) {
            circleDrawable.start()
        }

        lifecycleScope.launch {
            delay(900)

            // 로고 팝업
            binding.ivLogo.visibility = View.VISIBLE
            binding.ivLogo.scaleX = 0f
            binding.ivLogo.scaleY = 0f
            binding.ivLogo.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(OvershootInterpolator(1.5f))
                .start()

            delay(500)

            // 앱 이름 슬라이드업
            binding.tvAppName.visibility = View.VISIBLE
            binding.tvAppName.alpha = 0f
            binding.tvAppName.translationY = 40f
            binding.tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()

            delay(250)

            // 슬로건 페이드인
            binding.tvSlogan.visibility = View.VISIBLE
            binding.tvSlogan.alpha = 0f
            binding.tvSlogan.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

            delay(300)

            // 로딩 점 등장
            binding.layoutDots.visibility = View.VISIBLE
            binding.layoutDots.alpha = 0f
            binding.layoutDots.animate()
                .alpha(1f)
                .setDuration(400)
                .start()

            animateLoadingDots()

            delay(1200)

            // 다음 화면으로
            navigateNext()
        }
    }

    private fun animateLoadingDots() {
        listOf(binding.dot1, binding.dot2, binding.dot3).forEachIndexed { i, dot ->
            dot.animate()
                .translationY(-12f)
                .setDuration(400)
                .setStartDelay(i * 150L)
                .withEndAction {
                    dot.animate()
                        .translationY(0f)
                        .setDuration(400)
                        .start()
                }
                .start()
        }
    }

    private fun navigateNext() {
        val intent = if (tokenManager.isLoggedIn()) {
            Intent(this, HomeActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}