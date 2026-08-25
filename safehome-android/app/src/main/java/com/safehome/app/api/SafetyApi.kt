package com.safehome.app.api

import com.safehome.app.model.FacilityResponse
import com.safehome.app.model.FacilityCountResponse
import com.safehome.app.model.ApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SafetyApi {

    @GET("safety/facilities")
    suspend fun getFacilities(
        @Query("lat") lat:Double,
        @Query("lng") lng:Double,
        @Query("radius") radius: Int = 500
    ): Response<ApiResponse<List<FacilityResponse>>>

    @GET("safety/facilities/count")
    suspend fun getFacilityCounts(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int = 500
    ): Response<ApiResponse<FacilityCountResponse>>
}