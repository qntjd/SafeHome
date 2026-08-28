package com.safehome.app.api

import com.google.android.gms.common.api.Api
import com.safehome.app.model.ApiResponse
import com.safehome.app.model.SosCreateLogRequest
import com.safehome.app.model.SosLogResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SosApi {

    @POST("sos/log")
    suspend fun createLog(@Body request: SosCreateLogRequest): Response<ApiResponse<SosLogResponse>>

    @GET("sos/log")
    suspend fun getMyLogs(): Response<ApiResponse<List<SosLogResponse>>>
}