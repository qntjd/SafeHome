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
import com.kakao.vectormap.label.Label
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


    private val MARKER_DRAWABLES: Map<Pair<String, MarkerState>, Int> = mapOf(
        ("CCTV" to MarkerState.DEFAULT) to com.safehome.app.R.drawable.ic_marker_cctv_default,
        ("CCTV" to MarkerState.SELECTED) to com.safehome.app.R.drawable.ic_marker_cctv_selected,
        ("CCTV" to MarkerState.INACTIVE) to com.safehome.app.R.drawable.ic_marker_cctv_inactive,
        ("EMERGENCY_BELL" to MarkerState.DEFAULT) to com.safehome.app.R.drawable.ic_marker_emergency_bell_default,
        ("EMERGENCY_BELL" to MarkerState.SELECTED) to com.safehome.app.R.drawable.ic_marker_emergency_bell_selected,
        ("EMERGENCY_BELL" to MarkerState.INACTIVE) to com.safehome.app.R.drawable.ic_marker_emergency_bell_inactive,
        ("POLICE" to MarkerState.DEFAULT) to com.safehome.app.R.drawable.ic_marker_police_default,
        ("POLICE" to MarkerState.SELECTED) to com.safehome.app.R.drawable.ic_marker_police_selected,
        ("POLICE" to MarkerState.INACTIVE) to com.safehome.app.R.drawable.ic_marker_police_inactive
    )


    private val facilityIdByLabel = mutableMapOf<Label, String>()
    private val labelByFacilityId = mutableMapOf<String, Label>()
    private val facilityById = mutableMapOf<String, FacilityResponse>()
    private var selectedFacilityId: String? = null

    private enum class MarkerState { DEFAULT, SELECTED, INACTIVE }

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


                map.setOnLabelClickListener { _, _, label ->
                    facilityIdByLabel[label]?.let { onFacilityMarkerClicked(it) }
                }

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
        facilityIdByLabel.clear()
        labelByFacilityId.clear()
        facilityById.clear()


        val filtered = if (currentFilter == "ALL") allFacilites
        else allFacilites.filter { it.type == currentFilter }

        filtered.forEach { facility ->
            val state = markerStateFor(facility)

            val styles = labelManager.addLabelStyles(
                LabelStyles.from(LabelStyle.from(getFacilityIcon(facility.type, state)))
            )

            val label = labelManager.layer?.addLabel(
                LabelOptions.from(LatLng.from(facility.lat, facility.lng))
                    .setStyles(styles)
            )

            if (label != null) {
                facilityIdByLabel[label] = facility.id
                labelByFacilityId[facility.id] = label
                facilityById[facility.id] = facility
            }
        }
    }

    private fun markerStateFor(facility: FacilityResponse): MarkerState = when {
        facility.isActive == false -> MarkerState.INACTIVE
        facility.id == selectedFacilityId -> MarkerState.SELECTED
        else -> MarkerState.DEFAULT
    }

    private fun onFacilityMarkerClicked(facilityId: String) {
        val facility = facilityById[facilityId] ?: return


        if (facility.isActive == false) return

        val labelManager = kakaoMap?.labelManager ?: return
        val previousSelected = selectedFacilityId
        selectedFacilityId = if (previousSelected == facilityId) null else facilityId


        if (previousSelected != null && previousSelected != facilityId) {
            facilityById[previousSelected]?.let { prevFacility ->
                labelByFacilityId[previousSelected]?.changeStyles(
                    labelManager.addLabelStyles(
                        LabelStyles.from(
                            LabelStyle.from(getFacilityIcon(prevFacility.type, markerStateFor(prevFacility)))
                        )
                    )
                )
            }
        }


        labelByFacilityId[facilityId]?.changeStyles(
            labelManager.addLabelStyles(
                LabelStyles.from(
                    LabelStyle.from(getFacilityIcon(facility.type, markerStateFor(facility)))
                )
            )
        )

        // TODO: 여기서 하단 시설 정보 카드(주소, 타입 등)를 띄우는 로직 연결
    }

    private fun getFacilityIcon(type: String, state: MarkerState): android.graphics.Bitmap {
        val key = "$type|$state"
        return iconCache.getOrPut(key) {
            val resId = MARKER_DRAWABLES[type to state]
                ?: MARKER_DRAWABLES["CCTV" to MarkerState.DEFAULT]!!
            drawableToBitmap(resId)
        }
    }


    private fun drawableToBitmap(resId: Int): android.graphics.Bitmap {
        val drawable = androidx.core.content.ContextCompat.getDrawable(this, resId)!!
        (drawable as? android.graphics.drawable.BitmapDrawable)?.let { return it.bitmap }

        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
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