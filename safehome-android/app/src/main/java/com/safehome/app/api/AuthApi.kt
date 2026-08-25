package com.safehome.app.api

import com.safehome.app.model.ApiResponse
import com.safehome.app.model.LoginRequest
import com.safehome.app.model.RegisterRequest
import com.safehome.app.model.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<TokenResponse>>

    data class GoogleTokenRequest(
        val idToken: String
    )

    @POST("auth/google/token")
    suspend fun googleLogin(
        @Body body: GoogleTokenRequest
    ): Response<ApiResponse<TokenResponse>>

    @POST("auth/email/send")
    suspend fun sendVerificationCode(
        @Body body: Map<String, String>
    ): Response<ApiResponse<Void>>

    @POST("auth/email/verify")
    suspend fun verifyCode(
        @Body body: Map<String, String>
    ): Response<ApiResponse<Boolean>>
}