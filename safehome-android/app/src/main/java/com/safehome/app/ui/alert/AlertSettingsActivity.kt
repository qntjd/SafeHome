package com.safehome.app.ui.alert

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.safehome.app.api.AlertApi
import com.safehome.app.api.KakaoRetrofitClient
import com.safehome.app.api.RetrofitClient
import com.safehome.app.databinding.ActivityAlertSettingsBinding
import com.safehome.app.model.SubscribeRequest
import com.safehome.app.model.SubscriptionResponse
import kotlinx.coroutines.launch
import com.safehome.app.util.RegionData

class AlertSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertSettingsBinding
    private val alertApi by lazy { RetrofitClient.create(AlertApi::class.java) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAddRegion.setOnClickListener { showAddRegionDialog() }

        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        lifecycleScope.launch {
            try {
                val response = alertApi.getSubscriptions()
                if (response.isSuccessful) {
                    val subscriptions = response.body()?.data ?: emptyList()
                    showSubscriptions(subscriptions)
                }
            } catch (_: Exception) {}
        }
    }

    private fun showSubscriptions(subscriptions: List<SubscriptionResponse>) {
        binding.layoutMyLocation.removeAllViews()
        binding.layoutFamily.removeAllViews()
        binding.layoutFriend.removeAllViews()
        binding.layoutOther.removeAllViews()

        subscriptions.forEach { sub ->
            val layout = when {
                sub.isMyLocation -> binding.layoutMyLocation
                sub.category == "FAMILY" -> binding.layoutFamily
                sub.category == "FRIEND" -> binding.layoutFriend
                else -> binding.layoutOther
            }
            layout.addView(createSubscriptionCard(sub))
        }

        // 빈 카테고리 안내
        listOf(
            binding.layoutMyLocation,
            binding.layoutFamily,
            binding.layoutFriend,
            binding.layoutOther
        ).forEach { layout ->
            if (layout.childCount == 0) {
                val tv = TextView(this).apply {
                    text = "등록된 지역이 없어요"
                    textSize = 13f
                    setTextColor(Color.parseColor("#555A70"))
                    setPadding(0, 8, 0, 8)
                }
                layout.addView(tv)
            }
        }
    }

    private fun createSubscriptionCard(sub: SubscriptionResponse): CardView {
        val card = CardView(this).apply {
            radius = 48f
            setCardBackgroundColor(Color.parseColor("#1E2130"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
            layoutParams = params
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(40, 32, 40, 32)
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        val labelTv = TextView(this).apply {
            text = sub.label ?: "${sub.sidoName} ${sub.sigunguName ?: ""}"
            textSize = 15f
            setTextColor(Color.parseColor("#F0F2F8"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val addressTv = TextView(this).apply {
            text = "${sub.sidoName} ${sub.sigunguName ?: ""}".trim()
            textSize = 12f
            setTextColor(Color.parseColor("#555A70"))
        }

        textContainer.addView(labelTv)
        textContainer.addView(addressTv)

        val deleteTv = TextView(this).apply {
            text = "삭제"
            textSize = 13f
            setTextColor(Color.parseColor("#F87171"))
            setOnClickListener {
                AlertDialog.Builder(this@AlertSettingsActivity)
                    .setTitle("구독 삭제")
                    .setMessage("${sub.label ?: sub.sidoName} 알림을 삭제할까요?")
                    .setPositiveButton("삭제") { _, _ ->
                        deleteSubscription(sub.id)
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }

        container.addView(textContainer)
        container.addView(deleteTv)
        card.addView(container)
        return card
    }

    private fun showAddRegionDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        // 시도 선택
        val tvSidoLabel = TextView(this).apply {
            text = "시도 선택"
            textSize = 13f
            setTextColor(Color.parseColor("#8B90A7"))
            setPadding(0, 0, 0, 8)
        }

        val sidoSpinner = android.widget.Spinner(this).apply {
            val adapter = android.widget.ArrayAdapter(
                this@AlertSettingsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                RegionData.sidoList
            )
            this.adapter = adapter
        }

        // 시군구 선택
        val tvSigunguLabel = TextView(this).apply {
            text = "시군구 선택"
            textSize = 13f
            setTextColor(Color.parseColor("#8B90A7"))
            setPadding(0, 16, 0, 8)
        }

        val sigunguSpinner = android.widget.Spinner(this)

        fun updateSigungu(sido: String) {
            val sigunguList = listOf("전체") + (RegionData.sigunguMap[sido] ?: emptyList())
            val adapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                sigunguList
            )
            sigunguSpinner.adapter = adapter
        }

        updateSigungu(RegionData.sidoList[0])

        sidoSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateSigungu(RegionData.sidoList[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 별명 입력
        val tvLabelHint = TextView(this).apply {
            text = "별명 (예: 부모님댁, 친구집)"
            textSize = 13f
            setTextColor(Color.parseColor("#8B90A7"))
            setPadding(0, 16, 0, 8)
        }

        val etLabel = EditText(this).apply {
            hint = "별명 입력"
            setTextColor(Color.parseColor("#F0F2F8"))
            setHintTextColor(Color.parseColor("#555A70"))
        }

        // 카테고리 선택
        val tvCategory = TextView(this).apply {
            text = "카테고리"
            textSize = 13f
            setTextColor(Color.parseColor("#8B90A7"))
            setPadding(0, 16, 0, 8)
        }

        val radioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
        }

        listOf("👨‍👩‍👧 가족" to "FAMILY", "👫 친구" to "FRIEND", "📌 기타" to "OTHER").forEach { (label, value) ->
            val radio = RadioButton(this).apply {
                text = label
                tag = value
                setTextColor(Color.parseColor("#F0F2F8"))
            }
            radioGroup.addView(radio)
        }
        (radioGroup.getChildAt(2) as RadioButton).isChecked = true

        dialogView.addView(tvSidoLabel)
        dialogView.addView(sidoSpinner)
        dialogView.addView(tvSigunguLabel)
        dialogView.addView(sigunguSpinner)
        dialogView.addView(tvLabelHint)
        dialogView.addView(etLabel)
        dialogView.addView(tvCategory)
        dialogView.addView(radioGroup)

        AlertDialog.Builder(this)
            .setTitle("관심 지역 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val sido = sidoSpinner.selectedItem as String
                val sigungu = (sigunguSpinner.selectedItem as String).let {
                    if (it == "전체") null else it
                }
                val label = etLabel.text.toString().trim()
                val selectedId = radioGroup.checkedRadioButtonId
                val category = radioGroup.findViewById<RadioButton>(selectedId)?.tag as? String ?: "OTHER"

                subscribe(sido, sigungu, label.ifEmpty { "$sido ${sigungu ?: "전체"}" }, category)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun searchAndSubscribe(keyword: String, label: String, category: String) {
        lifecycleScope.launch {
            try {
                val response = KakaoRetrofitClient.kakaoApi.searchKeyword(
                    KakaoRetrofitClient.AUTH_HEADER, keyword
                )
                if (response.isSuccessful) {
                    val place = response.body()?.documents?.firstOrNull()
                    if (place != null) {
                        // 주소에서 시도/시군구 추출
                        val addressParts = place.address_name?.split(" ") ?: emptyList()
                        val sidoName = addressParts.getOrNull(0) ?: keyword
                        val sigunguName = addressParts.getOrNull(1)

                        subscribe(sidoName, sigunguName, label.ifEmpty { place.place_name }, category)
                    } else {
                        // 직접 입력으로 처리
                        subscribe(keyword, null, label.ifEmpty { keyword }, category)
                    }
                }
            } catch (_: Exception) {
                subscribe(keyword, null, label.ifEmpty { keyword }, category)
            }
        }
    }

    private fun subscribe(
        sidoName: String,
        sigunguName: String?,
        label: String,
        category: String
    ) {
        lifecycleScope.launch {
            try {
                val response = alertApi.subscribe(
                    SubscribeRequest(
                        sidoName = sidoName,
                        sigunguName = sigunguName,
                        label = label,
                        category = category
                    )
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@AlertSettingsActivity, "추가됐어요!", Toast.LENGTH_SHORT).show()
                    loadSubscriptions()
                }
            } catch (_: Exception) {
                Toast.makeText(this@AlertSettingsActivity, "추가 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteSubscription(id: String) {
        lifecycleScope.launch {
            try {
                alertApi.unsubscribe(id)
                Toast.makeText(this@AlertSettingsActivity, "삭제됐어요!", Toast.LENGTH_SHORT).show()
                loadSubscriptions()
            } catch (_: Exception) {}
        }
    }
}