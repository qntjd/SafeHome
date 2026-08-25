package com.safehome.app.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoApi {

    @GET("v2/local/geo/coord2address.json")
    suspend fun getAddress(
        @Header("Authorization") auth: String,
        @Query("x") lng: Double,
        @Query("y") lat: Double
    ): Response<KakaoAddressResponse>

    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Header("Authorization") auth: String,
        @Query("query") query: String
    ): Response<KakaoSearchResponse>
}

data class KakaoAddressResponse(
    val documents: List<KakaoAddressDocument>
)

data class KakaoAddressDocument(
    val road_address: KakaoRoadAddress?,
    val address: KakaoAddress?
)

data class KakaoRoadAddress(
    val address_name: String
)

data class KakaoAddress(
    val address_name: String
)

data class KakaoSearchResponse(
    val documents: List<KakaoPlace>
)

data class KakaoPlace(
    val place_name: String,
    val x: String,
    val y: String,
    val address_name: String?
)