package com.lingdict.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 有道词典API响应
 */
data class YoudaoResponse(
    @SerializedName("errorCode")
    val errorCode: String,

    @SerializedName("query")
    val query: String?,

    @SerializedName("translation")
    val translation: List<String>?,

    @SerializedName("basic")
    val basic: YoudaoBasic?,

    @SerializedName("web")
    val web: List<YoudaoWebTranslation>?
)

data class YoudaoBasic(
    @SerializedName("phonetic")
    val phonetic: String?,

    @SerializedName("us-phonetic")
    val usPhonetic: String?,

    @SerializedName("uk-phonetic")
    val ukPhonetic: String?,

    @SerializedName("explains")
    val explains: List<String>?
)

data class YoudaoWebTranslation(
    @SerializedName("key")
    val key: String,

    @SerializedName("value")
    val value: List<String>
)
