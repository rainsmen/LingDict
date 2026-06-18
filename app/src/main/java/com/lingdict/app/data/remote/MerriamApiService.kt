package com.lingdict.app.data.remote

import com.lingdict.app.data.remote.dto.MerriamEntry
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MerriamApiService {

    @GET("api/v3/references/learners/json/{word}")
    suspend fun lookup(
        @Path("word") word: String,
        @Query("key") apiKey: String
    ): List<MerriamEntry>

    companion object {
        const val BASE_URL = "https://www.dictionaryapi.com/"
    }
}
