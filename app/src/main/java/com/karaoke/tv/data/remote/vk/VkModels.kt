package com.karaoke.tv.data.remote.vk

import com.google.gson.annotations.SerializedName

data class VkSearchResponse(
    @SerializedName("response") val response: VkVideoList? = null,
    @SerializedName("error") val error: VkError? = null
)

data class VkVideoList(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("items") val items: List<VkVideoItem> = emptyList()
)

data class VkVideoItem(
    @SerializedName("id") val id: Long,
    @SerializedName("owner_id") val ownerId: Long,
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("image") val images: List<VkImage> = emptyList(),
    @SerializedName("player") val player: String = "",
    @SerializedName("views") val views: Long = 0,
    @SerializedName("is_favorite") val isFavorite: Boolean = false
)

data class VkImage(
    @SerializedName("url") val url: String = "",
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0
)

data class VkError(
    @SerializedName("error_code") val errorCode: Int,
    @SerializedName("error_msg") val errorMsg: String
)
