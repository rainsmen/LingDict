package com.lingdict.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MerriamEntry(
    @SerializedName("hwi")
    val headword: MerriamHeadword?,

    @SerializedName("fl")
    val functionalLabel: String?,

    @SerializedName("shortdef")
    val shortDefinitions: List<String>?
)

data class MerriamHeadword(
    @SerializedName("hw")
    val text: String?,

    @SerializedName("prs")
    val pronunciations: List<MerriamPronunciation>?
)

data class MerriamPronunciation(
    @SerializedName("mw")
    val written: String?,

    @SerializedName("sound")
    val sound: MerriamSound?
)

data class MerriamSound(
    @SerializedName("audio")
    val audio: String?
)
