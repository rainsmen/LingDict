package com.lingdict.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FreeDictionaryEntry(
    @SerializedName("word")
    val word: String?,

    @SerializedName("phonetic")
    val phonetic: String?,

    @SerializedName("phonetics")
    val phonetics: List<FreeDictionaryPhonetic>?,

    @SerializedName("meanings")
    val meanings: List<FreeDictionaryMeaning>?
)

data class FreeDictionaryPhonetic(
    @SerializedName("text")
    val text: String?,

    @SerializedName("audio")
    val audio: String?
)

data class FreeDictionaryMeaning(
    @SerializedName("partOfSpeech")
    val partOfSpeech: String?,

    @SerializedName("definitions")
    val definitions: List<FreeDictionaryDefinition>?
)

data class FreeDictionaryDefinition(
    @SerializedName("definition")
    val definition: String?,

    @SerializedName("example")
    val example: String?
)
