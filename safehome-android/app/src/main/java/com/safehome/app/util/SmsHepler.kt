package com.safehome.app.util

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.util.Log
import com.safehome.app.api.KakaoRetrofitClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    ): Pair<Boolean, String?> {
        val mapLink = "https://map.kakao.com/link/map/$lat,$lng"
        val message = """
            [SafeHome 안심귀가 알림]
            ${nickname}님이 예상 도착 시간을 넘겼습니다.

            📍 마지막 위치: $address
            🗺 위치 보기: $mapLink

            연락이 되지 않으면 확인 부탁드립니다.
        """.trimIndent()

        return sendSms(context, phoneNumber, message)
    }

    fun sendSosAlert(
        context: Context,
        nickname: String,
        phoneNumber: String,
        lat: Double,
        lng: Double,
        address: String,
        recordingPath: String? = null
    ): Pair<Boolean, String?> {
        val mapLink = "https://map.kakao.com/link/map/$lat,$lng"
        val coordText = "위도 $lat, 경도 $lng"
        val recordingInfo = if (recordingPath != null) {
            "\n🎙 녹음 파일: Downloads/SafeHome/SOS 폴더에 저장됐어요."
        } else ""

        val message = """
[SafeHome 긴급 SOS]
🚨 ${nickname}님이 긴급 SOS를 발동했습니다!

📍 현재 위치: $address
📌 좌표: $coordText
🗺 위치 보기: $mapLink$recordingInfo

즉시 연락하거나 112에 신고해주세요.
        """.trimIndent()

        return sendSms(context, phoneNumber, message)
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String): Pair<Boolean, String?> {
        return try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)

            var resultCode = -999
            val latch = CountDownLatch(1)
            val action = "com.safehome.app.SMS_SENT_${System.currentTimeMillis()}"

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    resultCode = this.resultCode
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}
                    latch.countDown()
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, IntentFilter(action))
            }

            val sentIntent = PendingIntent.getBroadcast(
                context, 0, Intent(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, null)
            } else {
                val sentIntents = ArrayList<PendingIntent>()
                repeat(parts.size) { sentIntents.add(sentIntent) }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
            }

            latch.await(5, TimeUnit.SECONDS)

            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "SMS 발송 성공: $phoneNumber")
                true to null
            } else {
                val errorMsg = when (resultCode) {
                    SmsManager.RESULT_ERROR_NO_SERVICE -> "서비스 없음 (유심 없음/신호 없음)"
                    SmsManager.RESULT_ERROR_RADIO_OFF -> "라디오 꺼짐"
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "일반 실패"
                    -999 -> "응답 시간 초과"
                    else -> "알 수 없는 오류 ($resultCode)"
                }
                Log.e(TAG, "SMS 발송 실패: $errorMsg")
                false to errorMsg
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS 발송 실패: ${e.message}")
            false to e.message
        }
    }
}