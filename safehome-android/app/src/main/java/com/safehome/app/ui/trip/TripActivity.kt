package com.safehome.app.ui.trip

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.LatLng
import com.safehome.app.SafeHomeApp
import com.safehome.app.api.RetrofitClient
import com.safehome.app.api.TripApi
import com.safehome.app.databinding.ActivityTripBinding
import com.safehome.app.model.StartTripRequest
import com.safehome.app.model.TripResponse
import com.safehome.app.service.TripTrackingService
import com.safehome.app.ui.settings.SettingsActivity
import kotlinx.coroutines.launch
import com.safehome.app.api.KakaoRetrofitClient

class TripActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripBinding
    private val tripApi by lazy { RetrofitClient.create(TripApi::class.java) }
    private val tokenManager by lazy { (application as SafeHomeApp).tokenManager }
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLat = 35.8714
    private var currentLng = 128.6014
    private var endLat = 0.0
    private var endLng = 0.0
    private var estimatedMinutes = 15
    private var activeTrip: TripResponse? = null
    private var kakaoMap: KakaoMap? = null

    private var isMapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()  // 위치 받으면 자동으로 setupMap 호출
        setupUI()
    }

    private fun setupMap() {
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(e: Exception) {
                Toast.makeText(this@TripActivity, "지도 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, object : KakaoMapReadyCallback() {

            // 현재 위치로 초기 중심 설정
            override fun getPosition(): LatLng {
                return LatLng.from(currentLat, currentLng)
            }

            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                isMapReady = true
            }
        })
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.seekBarMinutes.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                estimatedMinutes = progress
                binding.tvMinutes.text = "${progress}분"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 귀가 시작 버튼
        binding.btnStartTrip.setOnClickListener {
            if (activeTrip != null) return@setOnClickListener

            val destination = binding.etDestination.text.toString().trim()
            if (destination.isEmpty()) {
                Toast.makeText(this, "목적지를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 카카오 장소 검색으로 좌표 가져오기
            searchDestination(destination)
        }
    }

    private fun searchDestination(keyword: String) {
        lifecycleScope.launch {
            try {
                val response = KakaoRetrofitClient.kakaoApi.searchKeyword(
                    KakaoRetrofitClient.AUTH_HEADER,
                    keyword
                )
                if (response.isSuccessful) {
                    val place = response.body()?.documents?.firstOrNull()
                    if (place != null) {
                        endLat = place.y.toDouble()
                        endLng = place.x.toDouble()
                        Toast.makeText(
                            this@TripActivity,
                            "${place.place_name}으로 귀가 시작!",
                            Toast.LENGTH_SHORT
                        ).show()
                        startTrip()
                    } else {
                        Toast.makeText(this@TripActivity, "목적지를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                endLat = currentLat + 0.01
                endLng = currentLng + 0.01
                startTrip()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLat = it.latitude
                    currentLng = it.longitude
                }
                // 위치 받은 후 지도 시작
                setupMap()
            }.addOnFailureListener {
                // 위치 실패해도 지도 시작
                setupMap()
            }
        } else {
            setupMap()
        }
    }


    private fun startTrip() {
        lifecycleScope.launch {
            try {
                val response = tripApi.startTrip(
                    StartTripRequest(currentLat, currentLng, endLat, endLng, estimatedMinutes)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    activeTrip = response.body()!!.data

                    // 서비스에 연락처 전달
                    TripTrackingService.emergencyContacts = tokenManager.getContacts()
                    TripTrackingService.nickname =
                        tokenManager.getNickname()

                    val serviceIntent = Intent(this@TripActivity, TripTrackingService::class.java).apply {
                        putExtra("tripId", activeTrip?.id)
                        putExtra("expectedArrivalAt", activeTrip?.expectedArrivalAt)
                        putExtra("nickname", tokenManager.getNickname())
                    }
                    startForegroundService(serviceIntent)
                    showTripStatus()
                } else {
                    Toast.makeText(this@TripActivity, "귀가 시작 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@TripActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTripStatus() {
        Toast.makeText(this, "귀가가 시작됐어요!", Toast.LENGTH_SHORT).show()
        binding.btnStartTrip.text = "도착 완료"
        binding.btnStartTrip.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#34D399"))

        binding.btnStartTrip.setOnClickListener {
            activeTrip?.let { trip ->
                lifecycleScope.launch {
                    try {
                        tripApi.arrive(trip.id)
                        Toast.makeText(this@TripActivity, "도착 완료!", Toast.LENGTH_SHORT).show()
                        finish()
                    } catch (_: Exception) {
                        Toast.makeText(this@TripActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }
}