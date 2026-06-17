package com.lingdict.app.data.remote

import com.lingdict.app.data.remote.dto.FreeDictionaryEntry
import retrofit2.http.GET
import retrofit2.http.Path

interface FreeDictionaryApiService {

    @GET("api/v2/entries/en/{word}")
    suspend fun lookup(@Path("word") word: String): List<FreeDictionaryEntry>

    companion object {
        const val BASE_URL = "https://api.dictionaryapi.dev/"
    }
}
