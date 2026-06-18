package com.lingdict.app.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class WordsApiResponse(
    @SerializedName("word")
    val word: String?,

    @SerializedName("pronunciation")
    val pronunciation: JsonElement?,

    @SerializedName("results")
    val results: List<WordsApiResult>?
)

data class WordsApiResult(
    @SerializedName("definition")
    val definition: String?,

    @SerializedName("partOfSpeech")
    val partOfSpeech: String?,

    @SerializedName("examples")
    val examples: List<String>?
)
