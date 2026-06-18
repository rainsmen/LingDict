package com.lingdict.app.data.remote

import com.lingdict.app.data.remote.dto.WordsApiResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface WordsApiService {

    @GET("words/{word}")
    suspend fun lookup(
        @Path("word") word: String,
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") host: String = DEFAULT_HOST
    ): WordsApiResponse

    companion object {
        const val BASE_URL = "https://wordsapiv1.p.rapidapi.com/"
        const val DEFAULT_HOST = "wordsapiv1.p.rapidapi.com"
    }
}
