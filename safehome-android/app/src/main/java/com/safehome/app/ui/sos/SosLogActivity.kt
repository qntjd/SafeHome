package com.safehome.app.ui.sos

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.safehome.app.api.RetrofitClient
import com.safehome.app.api.SosApi
import com.safehome.app.databinding.ActivitySosLogBinding
import com.safehome.app.model.SosLogResponse
import kotlinx.coroutines.launch

class SosLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySosLogBinding
    private val sosApi by lazy { RetrofitClient.create(SosApi::class.java) }

    private val triggerLabels = mapOf(
        "VOICE" to "🎙 음성 SOS",
        "LOCK_SCREEN" to "🔒 잠금화면 SOS",
        "MANUAL" to "🚨 수동 SOS",
        "WATCHDOG" to "🚶 안심귀가 워치독"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySosLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.swipeRefresh.setOnRefreshListener { loadLogs() }

        loadLogs()
    }

    private fun loadLogs() {
        lifecycleScope.launch {
            try {
                val response = sosApi.getMyLogs()
                if (response.isSuccessful) {
                    val logs = response.body()?.data ?: emptyList()
                    showLogs(logs)
                }
            } catch (_: Exception) {
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showLogs(logs: List<SosLogResponse>) {
        binding.layoutLogs.removeAllViews()

        if (logs.isEmpty()) {
            binding.tvEmpty.visibility = android.view.View.VISIBLE
            return
        }
        binding.tvEmpty.visibility = android.view.View.GONE

        logs.forEach { log -> binding.layoutLogs.addView(buildLogCard(log)) }
    }

    private fun buildLogCard(log: SosLogResponse): CardView {
        val card = CardView(this).apply {
            radius = 16f.dp
            setCardBackgroundColor(Color.parseColor("#16203A"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16.dp) }
            layoutParams = params
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp, 18.dp, 20.dp, 18.dp)
        }

        // 헤더: 트리거 타입 + 시간
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val titleTv = TextView(this).apply {
            text = triggerLabels[log.triggerType] ?: log.triggerType
            textSize = 15f
            setTextColor(Color.parseColor("#F5F6FA"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = params
        }
        val timeTv = TextView(this).apply {
            text = formatTime(log.createdAt)
            textSize = 12f
            setTextColor(Color.parseColor("#8B90A7"))
        }
        headerRow.addView(titleTv)
        headerRow.addView(timeTv)

        // 주소
        val addressTv = TextView(this).apply {
            text = log.address ?: "위치 정보 없음"
            textSize = 12f
            setTextColor(Color.parseColor("#8B90A7"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4.dp, 0, 14.dp) }
            layoutParams = params
        }

        // 구분선
        val divider = android.view.View(this).apply {
            setBackgroundColor(Color.parseColor("#2A2D3A"))
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1.dp)
                .apply { setMargins(0, 0, 0, 14.dp) }
            layoutParams = params
        }

        // 발송 결과 라벨
        val recipientLabel = TextView(this).apply {
            text = "발송 결과"
            textSize = 12f
            setTextColor(Color.parseColor("#8B90A7"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8.dp) }
            layoutParams = params
        }

        container.addView(headerRow)
        container.addView(addressTv)
        container.addView(divider)
        container.addView(recipientLabel)

        // 수신자별 성공/실패
        log.recipients.forEach { r ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 6.dp) }
                layoutParams = params
            }
            val isSuccess = r.status == "SUCCESS"
            val statusDot = TextView(this).apply {
                text = if (isSuccess) "✓" else "✗"
                setTextColor(Color.parseColor(if (isSuccess) "#34D399" else "#F87171"))
                textSize = 13f
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 8.dp, 0) }
                layoutParams = params
            }
            val nameTv = TextView(this).apply {
                text = r.contactName ?: r.phoneNumber
                textSize = 13f
                setTextColor(Color.parseColor("#F5F6FA"))
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = params
            }
            val statusTv = TextView(this).apply {
                text = if (isSuccess) "성공" else "실패"
                textSize = 12f
                setTextColor(Color.parseColor(if (isSuccess) "#34D399" else "#F87171"))
            }
            row.addView(statusDot)
            row.addView(nameTv)
            row.addView(statusTv)
            container.addView(row)
        }

        // 112 신고 여부
        if (log.policeReported) {
            val policeTv = TextView(this).apply {
                text = "🚔 112 자동신고 접수됨"
                textSize = 12f
                setTextColor(Color.parseColor("#E8A33D"))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 12.dp, 0, 0) }
                layoutParams = params
            }
            container.addView(policeTv)
        }

        card.addView(container)
        return card
    }

    private fun formatTime(issuedAt: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.KOREA)
            val outputFormat = java.text.SimpleDateFormat("MM월 dd일 HH:mm", java.util.Locale.KOREA)
            val date = inputFormat.parse(issuedAt)
            outputFormat.format(date!!)
        } catch (_: Exception) {
            issuedAt
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private val Float.dp: Float
        get() = this * resources.displayMetrics.density
}