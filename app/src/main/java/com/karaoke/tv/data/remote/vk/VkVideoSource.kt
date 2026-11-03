package com.karaoke.tv.data.remote.vk

import android.content.Context
import android.content.Intent
import com.karaoke.tv.domain.engine.KaraokeScorer
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.source.KaraokeSource
import com.karaoke.tv.domain.source.PlaybackLaunchResult
import com.karaoke.tv.domain.source.SourceCapability
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface VkApiService {
    @GET("method/video.search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("access_token") accessToken: String,
        @Query("v") version: String = "5.199",
        @Query("count") count: Int = 20,
        @Query("extended") extended: Int = 1
    ): VkSearchResponse

    @GET("method/video.get")
    suspend fun getVideoDetails(
        @Query("videos") videos: String,
        @Query("access_token") accessToken: String,
        @Query("v") version: String = "5.199",
        @Query("extended") extended: Int = 1
    ): VkSearchResponse
}

class VkVideoSource(
    private var tokenProvider: () -> String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : KaraokeSource {

    override val sourceType: SourceType = SourceType.VK

    override val isConfigured: Boolean
        get() = tokenProvider().isNotBlank()

    private val api: VkApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.vk.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VkApiService::class.java)
    }

    override suspend fun searchSongs(query: String, maxResults: Int): Result<List<KaraokeVersion>> {
        val token = tokenProvider()
        if (token.isBlank()) {
            return Result.failure(IllegalStateException("VK токен доступа не настроен. Укажите токен в Настройках приложения."))
        }

        return runCatching {
            val effectiveQuery = if (query.contains("караоке", ignoreCase = true) || query.contains("karaoke", ignoreCase = true)) {
                query
            } else {
                "$query караоке"
            }

            val response = api.searchVideos(
                query = effectiveQuery,
                accessToken = token,
                count = maxResults
            )

            if (response.error != null) {
                throw IllegalStateException("VK API Error ${response.error.errorCode}: ${response.error.errorMsg}")
            }

            val items = response.response?.items ?: emptyList()
            items.map { item ->
                val scoreResult = KaraokeScorer.evaluate(
                    title = item.title,
                    description = item.description,
                    channelAuthor = "VK"
                )

                val bestImage = item.images.maxByOrNull { it.width }?.url ?: ""
                val videoUrl = "https://vk.com/video${item.ownerId}_${item.id}"
                val embedUrl = item.player.ifBlank { "https://vk.com/video_ext.php?oid=${item.ownerId}&id=${item.id}&autoplay=1" }

                KaraokeVersion(
                    id = "vk_${item.ownerId}_${item.id}",
                    songId = "",
                    source = SourceType.VK,
                    sourceVideoId = "${item.ownerId}_${item.id}",
                    title = item.title,
                    channelOrAuthor = "VK Видео",
                    thumbnailUrl = bestImage,
                    durationSeconds = item.duration,
                    videoUrl = videoUrl,
                    embedUrl = embedUrl,
                    quality = "1080p",
                    karaokeScore = scoreResult.score,
                    isEmbeddable = true,
                    verificationStatus = scoreResult.verificationStatus,
                    availabilityStatus = PlaybackAvailability.AVAILABLE
                )
            }
        }
    }

    override suspend fun getVideoDetails(videoId: String): Result<KaraokeVersion> {
        val token = tokenProvider()
        if (token.isBlank()) return Result.failure(IllegalStateException("VK токен не настроен"))

        return runCatching {
            val response = api.getVideoDetails(
                videos = videoId,
                accessToken = token
            )

            if (response.error != null) {
                throw IllegalStateException("VK Error: ${response.error.errorMsg}")
            }

            val item = response.response?.items?.firstOrNull() ?: throw NoSuchElementException("Видео не найдено")
            val scoreResult = KaraokeScorer.evaluate(title = item.title, description = item.description)
            val bestImage = item.images.maxByOrNull { it.width }?.url ?: ""

            KaraokeVersion(
                id = "vk_${item.ownerId}_${item.id}",
                songId = "",
                source = SourceType.VK,
                sourceVideoId = "${item.ownerId}_${item.id}",
                title = item.title,
                channelOrAuthor = "VK Видео",
                thumbnailUrl = bestImage,
                durationSeconds = item.duration,
                videoUrl = "https://vk.com/video${item.ownerId}_${item.id}",
                embedUrl = item.player,
                quality = "1080p",
                karaokeScore = scoreResult.score,
                isEmbeddable = true,
                verificationStatus = scoreResult.verificationStatus,
                availabilityStatus = PlaybackAvailability.AVAILABLE
            )
        }
    }

    override suspend fun checkAvailability(videoId: String): PlaybackAvailability {
        val token = tokenProvider()
        if (token.isBlank()) return PlaybackAvailability.SOURCE_UNCONFIGURED

        return try {
            val response = api.getVideoDetails(videos = videoId, accessToken = token)
            if (response.response?.items?.isNotEmpty() == true) {
                PlaybackAvailability.AVAILABLE
            } else {
                PlaybackAvailability.UNAVAILABLE
            }
        } catch (e: Exception) {
            PlaybackAvailability.UNKNOWN
        }
    }

    override fun getPlaybackCapability(): SourceCapability {
        return SourceCapability.Supported
    }

    override fun openPlayback(context: Context, version: KaraokeVersion): PlaybackLaunchResult {
        return if (version.embedUrl.isNotBlank()) {
            PlaybackLaunchResult.InAppPlayer(
                embedUrl = version.embedUrl,
                videoId = version.sourceVideoId
            )
        } else {
            PlaybackLaunchResult.ExternalIntent(
                intentAction = Intent.ACTION_VIEW,
                uriString = version.videoUrl.ifBlank { "https://vk.com/video${version.sourceVideoId}" }
            )
        }
    }
}
