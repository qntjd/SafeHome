package com.safehome.app.api

import com.safehome.app.model.ApiResponse
import com.safehome.app.model.TripResponse
import com.safehome.app.model.StartTripRequest
import retrofit2.Response
import retrofit2.http.*

interface TripApi {

    @POST("trips")
    suspend fun startTrip(@Body request: StartTripRequest): Response<ApiResponse<TripResponse>>

    @PATCH("trips/{tripId}/arrive")
    suspend fun arrive(@Path("tripId") tripId: String): Response<ApiResponse<TripResponse>>

    @POST("trips/{tripId}/sos")
    suspend fun sos(@Path("tripId") tripId: String): Response<ApiResponse<TripResponse>>

    @DELETE("trips/{tripId}")
    suspend fun cancel(@Path("tripId") tripId: String): Response<ApiResponse<Void>>
}