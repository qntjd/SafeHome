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
import android.graphics.drawable.Animatable

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
        playLogoIntro()
    }

    private fun playLogoIntro() {
        lifecycleScope.launch {

            binding.ivFrame.visibility = View.VISIBLE
            (binding.ivFrame.drawable as? Animatable)?.start()

            delay(650)


            binding.vBadge.animate()
                .alpha(1f)
                .setDuration(350)
                .start()

            delay(150)


            binding.ivS.visibility = View.VISIBLE
            (binding.ivS.drawable as? Animatable)?.start()

            delay(850)


            binding.vDot.visibility = View.VISIBLE
            binding.vDot.scaleX = 0f
            binding.vDot.scaleY = 0f
            binding.vDot.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(3f))
                .start()

            delay(300)


            binding.layoutAppName.visibility = View.VISIBLE
            binding.layoutAppName.alpha = 0f
            binding.layoutAppName.translationY = 40f
            binding.layoutAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()

            delay(250)


            binding.tvSlogan.visibility = View.VISIBLE
            binding.tvSlogan.alpha = 0f
            binding.tvSlogan.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

            binding.vUnderline.animate()
                .scaleX(1f)
                .setStartDelay(200)
                .setDuration(400)
                .start()

            delay(500)


            binding.layoutDots.visibility = View.VISIBLE
            binding.layoutDots.alpha = 0f
            binding.layoutDots.animate()
                .alpha(1f)
                .setDuration(400)
                .start()

            animateLoadingDots()

            delay(1000)

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