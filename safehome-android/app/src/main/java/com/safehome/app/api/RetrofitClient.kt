package com.safehome.app.api

import com.safehome.app.util.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://safehome-api.duckdns.org/api/"
    private lateinit var tokenManager: TokenManager
    private lateinit var retrofit: Retrofit

    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .apply {
                    tokenManager.getAccessToken()?.let {
                        addHeader("Authorization", "Bearer $it")
                    }
                }
                .build()
            chain.proceed(request)
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(clazz: Class<T>): T = retrofit.create(clazz)
}