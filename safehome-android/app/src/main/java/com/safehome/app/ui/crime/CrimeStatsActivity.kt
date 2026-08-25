package com.safehome.app.ui.crime

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.safehome.app.api.CrimeStatApi
import com.safehome.app.api.RetrofitClient
import com.safehome.app.databinding.ActivityCrimeStatsBinding
import com.safehome.app.model.DistrictCrimeResponse
import kotlinx.coroutines.launch

class CrimeStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrimeStatsBinding
    private val crimeStatApi by lazy { RetrofitClient.create(CrimeStatApi::class.java) }

    private val years = listOf(2020, 2021, 2022, 2023, 2024)
    private var selectedYear = 2024
    private lateinit var districtCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrimeStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        districtCode = intent.getStringExtra("districtCode") ?: "22"

        setupYearTabs()
        loadCrimeStats(districtCode, selectedYear)
    }

    private fun setupYearTabs() {
        binding.layoutYearTabs.removeAllViews()

        years.forEach { year ->
            val tab = TextView(this).apply {
                text = "${year}년"
                textSize = 14f
                setPadding(32, 16, 32, 16)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 12, 0) }
                layoutParams = params

                setOnClickListener {
                    selectedYear = year
                    setupYearTabs()  // 탭 스타일 갱신
                    loadCrimeStats(districtCode, selectedYear)
                }
            }
            styleYearTab(tab, year == selectedYear)
            binding.layoutYearTabs.addView(tab)
        }
    }

    private fun styleYearTab(tab: TextView, isSelected: Boolean) {
        if (isSelected) {
            tab.setTextColor(Color.WHITE)
            tab.typeface = Typeface.DEFAULT_BOLD
            tab.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#4F7EF8"))
                cornerRadius = 20f
            }
        } else {
            tab.setTextColor(Color.parseColor("#8B90A7"))
            tab.typeface = Typeface.DEFAULT
            tab.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#1C1F2A"))
                cornerRadius = 20f
            }
        }
    }

    private fun loadCrimeStats(districtCode: String, year: Int) {
        lifecycleScope.launch {
            try {
                val response = crimeStatApi.getDistrictCrimes(districtCode, year)
                if (response.isSuccessful) {
                    response.body()?.data?.let { showStats(it) }
                }
            } catch (_: Exception) {}
        }
    }

    private fun showStats(data: DistrictCrimeResponse) {
        binding.tvDistrictName.text = data.districtName
        binding.tvTotalCount.text = "${data.totalCount}건"

        binding.layoutCrimeList.removeAllViews()

        val maxCount = data.crimeByType.values.maxOrNull() ?: 1

        data.crimeByType.entries
            .sortedByDescending { it.value }
            .forEach { (label, count) ->
                addCrimeRow(label, count, maxCount)
            }
    }

    private fun addCrimeRow(label: String, count: Int, maxCount: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }
            layoutParams = params
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }
            layoutParams = params
        }

        val labelTv = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.parseColor("#F5F6FA"))
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = params
        }

        val countTv = TextView(this).apply {
            text = "${count}건"
            textSize = 14f
            setTextColor(Color.parseColor("#8B90A7"))
        }

        headerRow.addView(labelTv)
        headerRow.addView(countTv)

        val barBg = android.widget.FrameLayout(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 8
            )
            layoutParams = params
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#1F212B"))
                cornerRadius = 4f
            }
        }

        val ratio = if (maxCount > 0) count.toFloat() / maxCount else 0f
        val barFg = android.view.View(this).apply {
            val params = android.widget.FrameLayout.LayoutParams(
                (200 * ratio).toInt().coerceAtLeast(4), 8
            )
            layoutParams = params
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#4F7EF8"))
                cornerRadius = 4f
            }
        }

        barBg.addView(barFg)

        row.addView(headerRow)
        row.addView(barBg)

        binding.layoutCrimeList.addView(row)
    }
}