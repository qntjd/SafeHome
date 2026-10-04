package com.safehome.app.api

import com.google.android.gms.common.api.Api
import com.safehome.app.model.ApiResponse
import com.safehome.app.model.ContactCreateRequest
import com.safehome.app.model.ContactResponse
import retrofit2.Response
import retrofit2.http.*

interface ContactApi {
    @GET("contacts")
    suspend fun getContacts(): Response<ApiResponse<List<ContactResponse>>>

    @POST("contacts")
    suspend fun addContact(@Body request: ContactCreateRequest): Response<ApiResponse<ContactResponse>>

    @DELETE("contacts/{contactId}")
    suspend fun deleteContact(@Path("contactId") contactId: String): Response<ApiResponse<Unit>>

}