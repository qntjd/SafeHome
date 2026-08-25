package com.safehome.app.api

import com.safehome.app.model.AllDistrictCrimeResponse
import com.safehome.app.model.ApiResponse
import com.safehome.app.model.DistrictCrimeResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
interface CrimeStatApi {

    @GET("crime")
    suspend fun getAllCrimes(
        @Query("year") year: Int = 2024
    ): Response<ApiResponse<AllDistrictCrimeResponse>>

    @GET("crime/{districtCode}")
    suspend fun getDistrictCrimes(
        @Path("districtCode") districtCode: String,
        @Query("year") year: Int = 2024
    ): Response<ApiResponse<DistrictCrimeResponse>>
}