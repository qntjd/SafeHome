package com.safehome.app

import android.app.Application
import com.safehome.app.api.RetrofitClient
import com.safehome.app.util.TokenManager
import com.kakao.vectormap.KakaoMapSdk
import android.content.pm.PackageManager
import com.safehome.app.util.ContactRepository

class SafeHomeApp : Application() {

    lateinit var tokenManager: TokenManager
    val contactRepository by lazy { ContactRepository(tokenManager) }

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        RetrofitClient.init(tokenManager)

        // 카카오맵 SDK 초기화
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val kakaoAppKey = appInfo.metaData.getString("com.kakao.vectormap.APP_KEY") ?:""
        KakaoMapSdk.init(this, kakaoAppKey)
    }
}