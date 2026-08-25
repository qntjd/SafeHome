package com.safehome.app.util

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.safehome.app.api.KakaoRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsHelper {

    private const val TAG = "SmsHelper"

    suspend fun getAddress(lat: Double, lng: Double): String {
        return try {
            val response = KakaoRetrofitClient.kakaoApi.getAddress(
                KakaoRetrofitClient.AUTH_HEADER, lng, lat
            )
            if (response.isSuccessful) {
                val doc = response.body()?.documents?.firstOrNull()
                doc?.road_address?.address_name
                    ?: doc?.address?.address_name
                    ?: "위치 정보 없음"
            } else {
                "위치 정보 없음"
            }
        } catch (e: Exception) {
            Log.e(TAG, "주소 변환 실패: ${e.message}")
            "위치 정보 없음"
        }
    }

    fun sendWatchdogAlert(
        context: Context,
        nickname: String,
        phoneNumber: String,
        lat: Double,
        lng: Double,
        address: String
    ) {
        val mapLink = "https://map.kakao.com/link/map/$lat,$lng"
        val message = """
            [SafeHome 안심귀가 알림]
            ${nickname}님이 예상 도착 시간을 넘겼습니다.
            
            📍 마지막 위치: $address
            🗺 위치 보기: $mapLink
            
            연락이 되지 않으면 확인 부탁드립니다.
                    """.trimIndent()

        sendSms(phoneNumber, message)
    }

    fun sendSosAlert(
        context: Context,
        nickname: String,
        phoneNumber: String,
        lat: Double,
        lng: Double,
        address: String,
        recordingPath: String? = null  // 추가
    ) {
        val mapLink = "https://map.kakao.com/link/map/$lat,$lng"
        val recordingInfo = if (recordingPath != null) {
            "\n🎙 녹음 파일: Downloads/SafeHome/SOS 폴더에 저장됐어요."
        } else ""

        val message = """
[SafeHome 긴급 SOS]
🚨 ${nickname}님이 긴급 SOS를 발동했습니다!

📍 현재 위치: $address
🗺 위치 보기: $mapLink$recordingInfo

즉시 연락하거나 112에 신고해주세요.
    """.trimIndent()

        sendSms(phoneNumber, message)
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            // 메시지가 길면 분할 발송
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            Log.d(TAG, "SMS 발송 성공: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "SMS 발송 실패: ${e.message}")
        }
    }
}