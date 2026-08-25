package com.safehome.app.ui.news

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.safehome.app.api.NewsApi
import com.safehome.app.api.RetrofitClient
import com.safehome.app.databinding.ActivityNewsBinding
import com.safehome.app.model.NewsResponse
import kotlinx.coroutines.launch

class NewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsBinding
    private val newsApi by lazy { RetrofitClient.create(NewsApi::class.java) }

    private var currentKeyword: String? = null
    private var currentPage = 0
    private var isLastPage = false

    private val categories = listOf(
        "전체" to null,
        "범죄" to "검거",
        "재난" to "재난",
        "화재" to "화재",
        "교통사고" to "교통사고",
        "안전사고" to "사고"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadNews()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // 검색
        binding.btnSearch.setOnClickListener {
            val keyword = binding.etSearch.text.toString().trim()
            currentKeyword = keyword.ifEmpty { null }
            currentPage = 0
            loadNews()
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = binding.etSearch.text.toString().trim()
                currentKeyword = keyword.ifEmpty { null }
                currentPage = 0
                loadNews()
                true
            } else false
        }

        // 카테고리 버튼
        categories.forEach { (label, keyword) ->
            val btn = TextView(this).apply {
                text = label
                textSize = 13f
                setTextColor(if (keyword == currentKeyword) Color.WHITE else Color.parseColor("#8B90A7"))
                background = if (keyword == currentKeyword) {
                    getDrawable(com.safehome.app.R.drawable.bg_filter_selected)
                } else {
                    getDrawable(com.safehome.app.R.drawable.bg_filter_normal)
                }
                setPadding(40, 16, 40, 16)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 16, 0) }
                layoutParams = params
                setOnClickListener {
                    currentKeyword = keyword
                    currentPage = 0
                    updateCategoryUI()
                    loadNews()
                }
            }
            binding.layoutCategories.addView(btn)
        }

        // 스와이프 새로고침
        binding.swipeRefresh.setOnRefreshListener {
            currentPage = 0
            loadNews()
        }
    }

    private fun updateCategoryUI() {
        binding.layoutCategories.children.forEachIndexed { index, view ->
            val tv = view as TextView
            val keyword = categories[index].second
            if (keyword == currentKeyword) {
                tv.setTextColor(Color.WHITE)
                tv.background = getDrawable(com.safehome.app.R.drawable.bg_filter_selected)
            } else {
                tv.setTextColor(Color.parseColor("#8B90A7"))
                tv.background = getDrawable(com.safehome.app.R.drawable.bg_filter_normal)
            }
        }
    }

    private fun loadNews() {
        lifecycleScope.launch {
            try {
                val response = newsApi.getNews(
                    page = currentPage,
                    size = 20,
                    keyword = currentKeyword
                )
                if (response.isSuccessful) {
                    val page = response.body()?.data
                    if (page != null) {
                        if (currentPage == 0) binding.layoutNews.removeAllViews()
                        isLastPage = currentPage >= page.totalPages - 1
                        showNews(page.articles)
                    }
                }
            } catch (_: Exception) {
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showNews(newsList: List<NewsResponse>) {
        if (newsList.isEmpty() && currentPage == 0) {
            val tv = TextView(this).apply {
                text = "뉴스가 없어요"
                textSize = 14f
                setTextColor(Color.parseColor("#555A70"))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 80, 0, 0)
            }
            binding.layoutNews.addView(tv)
            return
        }

        newsList.forEach { news ->
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
                orientation = LinearLayout.VERTICAL
                setPadding(40, 36, 40, 36)
            }

            // 출처 + 시간
            val headerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
                layoutParams = params
            }

            val sourceTv = TextView(this).apply {
                text = news.source ?: "뉴스"
                textSize = 12f
                setTextColor(Color.parseColor("#4F7EF8"))
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val timeTv = TextView(this).apply {
                text = formatTime(news.publishedAt)
                textSize = 11f
                setTextColor(Color.parseColor("#555A70"))
            }

            headerLayout.addView(sourceTv)
            headerLayout.addView(timeTv)

            // 제목
            val titleTv = TextView(this).apply {
                text = news.title
                textSize = 15f
                setTextColor(Color.parseColor("#F0F2F8"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
                layoutParams = params
            }

            // 설명
            val descTv = TextView(this).apply {
                text = news.description ?: ""
                textSize = 13f
                setTextColor(Color.parseColor("#8B90A7"))
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            container.addView(headerLayout)
            container.addView(titleTv)
            if (!news.description.isNullOrEmpty()) container.addView(descTv)

            card.addView(container)

            // 클릭 시 브라우저로 열기
            card.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url))
                    startActivity(intent)
                } catch (_: Exception) {}
            }

            binding.layoutNews.addView(card)
        }

        // 더보기 버튼
        if (!isLastPage) {
            val moreBtn = TextView(this).apply {
                text = "더보기"
                textSize = 14f
                setTextColor(Color.parseColor("#4F7EF8"))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 24, 0, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    binding.layoutNews.removeView(this)
                    currentPage++
                    loadNews()
                }
            }
            binding.layoutNews.addView(moreBtn)
        }
    }

    private fun formatTime(publishedAt: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.KOREA)
            val outputFormat = java.text.SimpleDateFormat("MM.dd HH:mm", java.util.Locale.KOREA)
            val date = inputFormat.parse(publishedAt.take(19))
            outputFormat.format(date!!)
        } catch (_: Exception) {
            publishedAt
        }
    }
}

// LinearLayout children 확장 함수
val android.view.ViewGroup.children: Sequence<android.view.View>
    get() = sequence {
        for (i in 0 until childCount) yield(getChildAt(i))
    }