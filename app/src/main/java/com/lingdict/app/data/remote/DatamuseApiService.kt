package com.lingdict.app.data.remote

import com.lingdict.app.data.remote.dto.DatamuseWord
import retrofit2.http.GET
import retrofit2.http.Query

interface DatamuseApiService {

    @GET("words")
    suspend fun words(
        @Query("sp") spelling: String,
        @Query("md") metadata: String = "dps",
        @Query("max") max: Int = 1
    ): List<DatamuseWord>

    companion object {
        const val BASE_URL = "https://api.datamuse.com/"
    }
}
