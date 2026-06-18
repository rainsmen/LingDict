package com.lingdict.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DatamuseWord(
    @SerializedName("word")
    val word: String?,

    @SerializedName("tags")
    val tags: List<String>?,

    @SerializedName("defs")
    val definitions: List<String>?
)
