package com.safehome.app.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object KakaoRetrofitClient {

    private const val KAKAO_API_URL = "https://dapi.kakao.com/"
    private const val KAKAO_REST_API_KEY = "eb4dfc738cb0ef3ef53b7ac022c17363"

    const val AUTH_HEADER = "KakaoAK $KAKAO_REST_API_KEY"

    private val retrofit = Retrofit.Builder()
        .baseUrl(KAKAO_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val kakaoApi: KakaoApi = retrofit.create(KakaoApi::class.java)
}