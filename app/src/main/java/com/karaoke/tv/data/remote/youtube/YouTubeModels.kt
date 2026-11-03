package com.karaoke.tv.data.remote.youtube

import com.google.gson.annotations.SerializedName

data class YouTubeSearchResponse(
    @SerializedName("items") val items: List<YouTubeSearchResultItem> = emptyList()
)

data class YouTubeSearchResultItem(
    @SerializedName("id") val id: YouTubeVideoId,
    @SerializedName("snippet") val snippet: YouTubeSnippet
)

data class YouTubeVideoId(
    @SerializedName("videoId") val videoId: String = ""
)

data class YouTubeSnippet(
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("channelTitle") val channelTitle: String = "",
    @SerializedName("thumbnails") val thumbnails: YouTubeThumbnails? = null
)

data class YouTubeThumbnails(
    @SerializedName("high") val high: YouTubeThumbnail? = null,
    @SerializedName("medium") val medium: YouTubeThumbnail? = null,
    @SerializedName("default") val default: YouTubeThumbnail? = null
)

data class YouTubeThumbnail(
    @SerializedName("url") val url: String = ""
)

data class YouTubeVideoDetailsResponse(
    @SerializedName("items") val items: List<YouTubeVideoDetailItem> = emptyList()
)

data class YouTubeVideoDetailItem(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: YouTubeSnippet,
    @SerializedName("contentDetails") val contentDetails: YouTubeContentDetails? = null,
    @SerializedName("status") val status: YouTubeStatus? = null
)

data class YouTubeContentDetails(
    @SerializedName("duration") val duration: String = "" // ISO 8601 (PT4M20S)
)

data class YouTubeStatus(
    @SerializedName("embeddable") val embeddable: Boolean = true,
    @SerializedName("privacyStatus") val privacyStatus: String = "public"
)

data class YouTubeErrorResponse(
    @SerializedName("error") val error: YouTubeErrorDetail? = null
)

data class YouTubeErrorDetail(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("errors") val errors: List<YouTubeErrorInfo> = emptyList(),
    @SerializedName("status") val status: String = ""
)

data class YouTubeErrorInfo(
    @SerializedName("message") val message: String = "",
    @SerializedName("domain") val domain: String = "",
    @SerializedName("reason") val reason: String = ""
)
