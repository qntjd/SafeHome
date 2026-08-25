package com.safehome.app.api

import com.safehome.app.model.ApiResponse
import com.safehome.app.model.AlertResponse
import com.safehome.app.model.SubscribeRequest
import com.safehome.app.model.SubscriptionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Path


interface AlertApi {

    @GET("alerts/history/my")
    suspend fun getMyAlertHistory(): Response<ApiResponse<List<AlertResponse>>>

    @GET("alerts/history/interested")
    suspend fun getInterestedAlertHistory(): Response<ApiResponse<List<AlertResponse>>>

    @GET("alerts/subscriptions")
    suspend fun getSubscriptions(): Response<ApiResponse<List<SubscriptionResponse>>>

    @POST("alerts/subscribe")
    suspend fun subscribe(
        @Body request: SubscribeRequest
    ): Response<ApiResponse<SubscriptionResponse>>

    @DELETE("alerts/subscribe/{id}")
    suspend fun  unsubscribe(
        @Path("id") id: String
    ): Response<ApiResponse<Void>>
}