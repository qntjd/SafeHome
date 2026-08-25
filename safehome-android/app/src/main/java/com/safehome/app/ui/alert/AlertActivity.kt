package com.safehome.app.ui.alert

import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.widget.LinearLayout
import kotlin.collections.emptyList
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.safehome.app.api.AlertApi
import com.safehome.app.api.RetrofitClient
import com.safehome.app.databinding.ActivityAlertBinding
import com.safehome.app.model.AlertResponse
import kotlinx.coroutines.launch

class AlertActivity : AppCompatActivity(){

    private lateinit var binding: ActivityAlertBinding
    private  val alertApi by lazy { RetrofitClient.create(AlertApi::class.java) }
    private  var currentTab = "MY"

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadAlerts()
    }

    private fun setupUI(){
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, AlertSettingsActivity::class.java))
        }

        binding.tabMy.setOnClickListener {
            currentTab ="MY"
            updateTabUI()
            loadAlerts()
        }

        binding.tabAll.setOnClickListener {
            currentTab = "INTERESTED"
            updateTabUI()
            loadAlerts()
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadAlerts()
        }
    }

    private fun updateTabUI() {
        if (currentTab == "MY") {
            binding.tabMy.setBackgroundResource(com.safehome.app.R.drawable.bg_tab_selected)
            binding.tabMy.setTextColor(Color.WHITE)
            binding.tabAll.setBackgroundResource(com.safehome.app.R.drawable.bg_tab_normal)
            binding.tabAll.setTextColor(Color.parseColor("#8B90A7"))

        } else {
            binding.tabAll.setBackgroundResource(com.safehome.app.R.drawable.bg_tab_selected)
            binding.tabAll.setTextColor(Color.WHITE)
            binding.tabMy.setBackgroundResource(com.safehome.app.R.drawable.bg_tab_normal)
            binding.tabMy.setTextColor(Color.parseColor("#8B90A7"))
        }
    }

    private fun loadAlerts() {
        lifecycleScope.launch {
            try {
                val response = if(currentTab == "MY"){
                    alertApi.getMyAlertHistory()
                } else {
                    alertApi.getInterestedAlertHistory()
                }
                if (response.isSuccessful) {
                    val alerts = response.body()?.data ?: emptyList()
                    showAlerts(alerts)
                }
            } catch (_: Exception) {

            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showAlerts(alerts: List<AlertResponse>) {
        binding.layoutAlerts.removeAllViews()

        if (alerts.isEmpty()) {
            val tv = TextView(this).apply {
                text = "알림 내역이 없어요"
                textSize = 14f
                setTextColor(Color.parseColor("#555A70"))
                gravity = android.view.Gravity.CENTER
                setPadding(0,80,0,0)
            }
            binding.layoutAlerts.addView(tv)
            return
        }

        alerts.forEach { alert ->
            val card = CardView(this).apply {
                radius = 48f
                setCardBackgroundColor(Color.parseColor("#1E2130"))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0,0,0,16) }
                layoutParams = params
            }

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48,40,48,40)
            }

            val headerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val levelColor = when (alert.level) {
                "DANGER" -> "#F87171"
                "WARNING" -> "#FBBF24"
                else -> "#4F7EF8"
            }

            val levelBadge = TextView(this).apply {
                text = when (alert.level) {
                    "DANGER" -> "위험"
                    "WARNING" -> "주의"
                    else -> "정보"
                }
                textSize = 11f
                setTextColor(Color.parseColor((levelColor)))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor(levelColor + "33"))
                    cornerRadius = 20f
                }
                setPadding(20,8,20,8)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0,0,16,0) }
                layoutParams = params
            }

            val districTv = TextView(this).apply {
                text = alert.districtName
                textSize = 12f
                setTextColor(Color.parseColor("#8B90A7"))

            }

            headerLayout.addView(levelBadge)
            headerLayout.addView(districTv)

            val titleTv = TextView(this).apply {
                text = alert.title
                textSize = 15f
                setTextColor(Color.parseColor("#F0F2F8"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0,16,0,8) }
                layoutParams = params
            }

            val messageTv = TextView(this).apply {
                text =alert.message
                textSize = 12f
                setTextColor(Color.parseColor("#8B90A7"))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0,0,0,12) }
                layoutParams =params
            }

            val timeTv = TextView(this).apply {
                text = formatTime(alert.issuedAt)
                textSize = 11f
                setTextColor(Color.parseColor("#555A70"))
            }

            container.addView(headerLayout)
            container.addView(titleTv)
            container.addView(messageTv)
            card.addView(container)
            binding.layoutAlerts.addView(card)
        }
    }

    private fun formatTime(issuedAt:String): String {
        return  try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",java.util.Locale.KOREA)
            val outputFormat = java.text.SimpleDateFormat("MM월 dd일 HH:mm", java.util.Locale.KOREA)
            val date = inputFormat.parse(issuedAt)
            outputFormat.format(date!!)
        } catch (_: Exception) {
            issuedAt
        }
    }

}