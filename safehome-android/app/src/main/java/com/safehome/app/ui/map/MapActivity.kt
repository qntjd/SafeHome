package com.safehome.app.ui.map

import android.content.pm.PackageManager
import android.graphics.Color
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelOptions
import com.safehome.app.api.RetrofitClient
import com.safehome.app.api.SafetyApi
import com.safehome.app.databinding.ActivityMapBinding
import com.safehome.app.model.FacilityResponse
import kotlinx.coroutines.launch

import android.os.Bundle
import android.Manifest
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kakao.vectormap.KakaoMap



class MapActivity :  AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private val safetyApi by lazy { RetrofitClient.create(SafetyApi::class.java) }
    private lateinit var  fusedLocationClient: FusedLocationProviderClient

    private var kakaoMap: KakaoMap? = null
    private var isMapReady = false
    private var currentLat = 35.8714
    private var currentLng = 128.6014
    private var allFacilites = listOf<FacilityResponse>()
    private var currentFilter = "ALL"

    private val iconCache = mutableMapOf<String, android.graphics.Bitmap>()

    override fun onCreate(saveInstanceState: Bundle?) {
        super.onCreate(saveInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnMyLocation.setOnClickListener {
            kakaoMap?.moveCamera(
                com.kakao.vectormap.camera.CameraUpdateFactory
                    .newCenterPosition(LatLng.from(currentLat, currentLng))
            )
        }

        listOf(
            binding.btnFilterAll to "ALL",
            binding.btnFilterCctv to "CCTV",
            binding.btnFilterBell to "EMERGENCY_BELL",
            binding.btnFilterPolice to "POLICE"
        ).forEach { (btn, filter) ->
            btn.setOnClickListener {
                currentFilter = filter
                updateFilterUI()
                showMarkers()
            }
        }
    }

    private fun updateFilterUI(){
        listOf(
            binding.btnFilterAll to "ALL",
            binding.btnFilterCctv to "CCTV",
            binding.btnFilterBell to "EMERGENCY_BELL",
            binding.btnFilterPolice to "POLICE"
        ).forEach { (btn, filter) ->
            if (filter == currentFilter) {
                btn.setBackgroundResource(com.safehome.app.R.drawable.bg_filter_selected)
                btn.setTextColor(Color.WHITE)

            } else {
                btn.setBackgroundResource(com.safehome.app.R.drawable.bg_filter_normal)
                btn.setTextColor(Color.parseColor("#8B90A7"))
            }
        }
    }

    private  fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED){
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLat = it.latitude
                    currentLng = it.longitude
                }
                setupMap()
            }.addOnFailureListener { setupMap() }
        } else {
            setupMap()
        }
    }

    private fun setupMap() {
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(e: Exception) {
                Toast.makeText(this@MapActivity, "지도 오류: ${e.message}", Toast.LENGTH_SHORT).show()

            }
        }, object : KakaoMapReadyCallback() {
            override fun getPosition(): LatLng = LatLng.from(currentLat, currentLng)
            override fun getZoomLevel(): Int = 15

            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                isMapReady = true
                loadFacilities()

                map.setOnCameraMoveEndListener { _, position, _ ->
                    val lat = position.position.latitude
                    val lng = position.position.longitude
                    loadFacilitiesByPosition(lat, lng)
                }
            }
        })

    }

    private fun loadFacilities() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = safetyApi.getFacilities(currentLat, currentLng, 1000)
                if (response.isSuccessful) {
                    allFacilites = response.body()?.data ?: emptyList()
                    showMarkers()
                }
                loadFacilityCounts(currentLat, currentLng)
            } catch (e: Exception) {
                Toast.makeText(this@MapActivity, "시설 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadFacilitiesByPosition(lat: Double, lng: Double) {
        lifecycleScope.launch {
            try {
                val response = safetyApi.getFacilities(lat, lng, 1000)
                if (response.isSuccessful) {
                    allFacilites = response.body()?.data ?: emptyList()
                    showMarkers()
                }
                loadFacilityCounts(lat, lng)
            } catch (_: Exception) {}
        }
    }

    private fun loadFacilityCounts(lat: Double, lng: Double) {
        lifecycleScope.launch {
            try {
                val response = safetyApi.getFacilityCounts(lat, lng, 1000)
                if (response.isSuccessful) {
                    val counts = response.body()?.data
                    binding.tvCctvCount.text = (counts?.cctvCount ?: 0).toString()
                    binding.tvBellCount.text = (counts?.bellCount ?: 0).toString()
                    binding.tvPoliceCount.text = (counts?.policeCount ?: 0).toString()
                }
            } catch (_: Exception) {}
        }
    }

    private fun showMarkers() {
        val map = kakaoMap ?: return
        val labelManager = map.labelManager ?: return

        labelManager.clearAll()

        val filtered = if (currentFilter == "ALL") allFacilites
                        else allFacilites.filter { it.type == currentFilter }

        filtered.forEach { facility ->
            val color = when (facility.type) {
                "CCTV" -> "#4F7EF8"
                "EMERGENCY_BELL" -> "#F87171"
                "POLICE" -> "#A78BFA"
                else -> "#888888"
            }

            val styles = labelManager.addLabelStyles(
                LabelStyles.from(
                    LabelStyle.from(getFacilityIcon(facility.type))
                )
            )

            labelManager.layer?.addLabel(
                LabelOptions.from(LatLng.from(facility.lat, facility.lng))
                    .setStyles(styles)
            )
        }
    }

    private fun getFacilityIcon(type: String): android.graphics.Bitmap {
        return iconCache.getOrPut(type) {
            when (type) {
                "CCTV" -> createBadgedIcon(
                    resId = com.safehome.app.R.drawable.ic_cctv,
                    bgColor = android.graphics.Color.parseColor("#4F7EF8")
                )
                "EMERGENCY_BELL" -> loadPlainIcon(com.safehome.app.R.drawable.ic_bell)
                "POLICE" -> loadPlainIcon(com.safehome.app.R.drawable.ic_police)
                else -> loadPlainIcon(com.safehome.app.R.drawable.ic_cctv)
            }
        }
    }

    // CCTV용: 원형 배경 + 흰 아이콘
    private fun createBadgedIcon(resId: Int, bgColor: Int): android.graphics.Bitmap {
        val size = 72
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // 원형 배경
        val paint = android.graphics.Paint().apply {
            color = bgColor
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // 흰 테두리
        val strokePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, strokePaint)

        // 아이콘
        val drawable = androidx.core.content.ContextCompat.getDrawable(this, resId)!!
        val iconSize = (size * 0.6).toInt()
        val offset = (size - iconSize) / 2
        drawable.setBounds(offset, offset, offset + iconSize, offset + iconSize)
        drawable.setTint(android.graphics.Color.WHITE)
        drawable.draw(canvas)

        return bitmap
    }

    // 비상벨/경찰서용:(배경 없이 아이콘만)
    private fun loadPlainIcon(resId: Int): android.graphics.Bitmap {
        val drawable = androidx.core.content.ContextCompat.getDrawable(this, resId)!!
        val size = 64
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private fun updateBottomCard() {
        lifecycleScope.launch {
            try {
                val address = com.safehome.app.util.SmsHelper.getAddress(currentLat, currentLng)
                binding.tvAreaName.text = address
            } catch (_: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        if (isMapReady) binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        if(isMapReady) binding.mapView.pause()
    }
}