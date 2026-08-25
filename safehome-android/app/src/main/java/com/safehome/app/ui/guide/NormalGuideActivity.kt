package com.safehome.app.ui.guide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safehome.app.databinding.ActivityNormalGuideBinding

class NormalGuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNormalGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNormalGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupLinkClicks()
    }

    private fun setupLinkClicks() {
        val links = mapOf(
            binding.linkMsafer to "https://msafer.or.kr",
            binding.linkPayinfo to "https://www.payinfo.or.kr",
            binding.linkCreditinfo to "https://www.creditinfo.or.kr",
            binding.linkPrivacy to "https://www.privacy.go.kr",
            binding.linkGov to "https://www.gov.kr"
        )
        links.forEach { (view, url) ->
            view.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }
}