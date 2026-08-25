package com.safehome.app.api
import com.safehome.app.model.ApiResponse
import com.safehome.app.model.NewsPageResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {

    @GET("news")
    suspend fun  getNews(
        @Query("page") page : Int = 0,
        @Query("size") size: Int = 20,
        @Query("keyword") keyword: String? = null
    ): Response<ApiResponse<NewsPageResponse>>
}