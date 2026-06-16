package com.lingdict.app.data.remote

import com.lingdict.app.data.remote.dto.PexelsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Pexels图片API服务
 * API文档：https://www.pexels.com/api/documentation/
 */
interface PexelsApiService {

    @GET("v1/search")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Query("page") page: Int = 1,
        @Header("Authorization") apiKey: String
    ): PexelsResponse

    companion object {
        const val BASE_URL = "https://api.pexels.com/"
    }
}
