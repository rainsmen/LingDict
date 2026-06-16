package com.lingdict.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Pexels API响应
 */
data class PexelsResponse(
    @SerializedName("photos")
    val photos: List<PexelsPhoto>,

    @SerializedName("total_results")
    val totalResults: Int,

    @SerializedName("page")
    val page: Int,

    @SerializedName("per_page")
    val perPage: Int
)

data class PexelsPhoto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("width")
    val width: Int,

    @SerializedName("height")
    val height: Int,

    @SerializedName("url")
    val url: String,

    @SerializedName("photographer")
    val photographer: String,

    @SerializedName("src")
    val src: PexelsPhotoSrc,

    @SerializedName("alt")
    val alt: String?
)

data class PexelsPhotoSrc(
    @SerializedName("original")
    val original: String,

    @SerializedName("large2x")
    val large2x: String,

    @SerializedName("large")
    val large: String,

    @SerializedName("medium")
    val medium: String,

    @SerializedName("small")
    val small: String,

    @SerializedName("portrait")
    val portrait: String,

    @SerializedName("landscape")
    val landscape: String,

    @SerializedName("tiny")
    val tiny: String
)
