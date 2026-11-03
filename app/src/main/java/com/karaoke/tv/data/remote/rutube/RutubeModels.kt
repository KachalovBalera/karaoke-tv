package com.karaoke.tv.data.remote.rutube

import com.google.gson.annotations.SerializedName

data class RutubeSearchResponse(
    @SerializedName("has_next") val hasNext: Boolean = false,
    @SerializedName("results") val results: List<RutubeVideoItem> = emptyList()
)

data class RutubeVideoItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("thumbnail_url") val thumbnailUrl: String = "",
    @SerializedName("video_url") val videoUrl: String = "",
    @SerializedName("embed_url") val embedUrl: String = "",
    @SerializedName("hits") val hits: Long = 0,
    @SerializedName("is_deleted") val isDeleted: Boolean = false,
    @SerializedName("is_locked") val isLocked: Boolean = false,
    @SerializedName("is_adult") val isAdult: Boolean = false,
    @SerializedName("author") val author: RutubeAuthor? = null
)

data class RutubeAuthor(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String = "",
    @SerializedName("avatar_url") val avatarUrl: String = ""
)
