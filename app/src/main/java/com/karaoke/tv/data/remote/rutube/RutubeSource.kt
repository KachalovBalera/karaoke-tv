package com.karaoke.tv.data.remote.rutube

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface RutubeApiService {
    @GET("api/search/video/")
    suspend fun searchVideos(
        @Query("query") query: String,
        @Query("format") format: String = "json"
    ): RutubeSearchResponse

    @GET("api/video/{id}/")
    suspend fun getVideoDetails(
        @Path("id") id: String,
        @Query("format") format: String = "json"
    ): RutubeVideoItem
}

class RutubeSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : KaraokeSource {

    override val sourceType: SourceType = SourceType.RUTUBE
    override val isConfigured: Boolean = true // Rutube search API is public, no key required!

    private val api: RutubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://rutube.ru/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RutubeApiService::class.java)
    }

    override suspend fun searchSongs(query: String, maxResults: Int): Result<List<KaraokeVersion>> {
        return runCatching {
            // Enrich query if user just searched artist - song
            val effectiveQuery = if (query.contains("караоке", ignoreCase = true) || query.contains("karaoke", ignoreCase = true)) {
                query
            } else {
                "$query караоке"
            }

            val response = api.searchVideos(query = effectiveQuery)
            response.results
                .filter { !it.isDeleted && !it.isLocked }
                .take(maxResults)
                .map { item ->
                    val scoreResult = KaraokeScorer.evaluate(
                        title = item.title,
                        description = item.description,
                        channelAuthor = item.author?.name ?: ""
                    )

                    val embedUrl = if (item.embedUrl.isNotBlank()) item.embedUrl else "https://rutube.ru/play/embed/${item.id}"
                    val videoUrl = if (item.videoUrl.isNotBlank()) item.videoUrl else "https://rutube.ru/video/${item.id}/"

                    KaraokeVersion(
                        id = "rutube_${item.id}",
                        songId = "", // To be assigned by DeduplicationEngine
                        source = SourceType.RUTUBE,
                        sourceVideoId = item.id,
                        title = item.title,
                        channelOrAuthor = item.author?.name ?: "RUTUBE",
                        thumbnailUrl = item.thumbnailUrl,
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
        return runCatching {
            val item = api.getVideoDetails(videoId)
            val scoreResult = KaraokeScorer.evaluate(
                title = item.title,
                description = item.description,
                channelAuthor = item.author?.name ?: ""
            )

            val embedUrl = if (item.embedUrl.isNotBlank()) item.embedUrl else "https://rutube.ru/play/embed/${item.id}"
            val videoUrl = if (item.videoUrl.isNotBlank()) item.videoUrl else "https://rutube.ru/video/${item.id}/"

            KaraokeVersion(
                id = "rutube_${item.id}",
                songId = "",
                source = SourceType.RUTUBE,
                sourceVideoId = item.id,
                title = item.title,
                channelOrAuthor = item.author?.name ?: "RUTUBE",
                thumbnailUrl = item.thumbnailUrl,
                durationSeconds = item.duration,
                videoUrl = videoUrl,
                embedUrl = embedUrl,
                quality = "1080p",
                karaokeScore = scoreResult.score,
                isEmbeddable = !item.isDeleted && !item.isLocked,
                verificationStatus = scoreResult.verificationStatus,
                availabilityStatus = if (item.isDeleted || item.isLocked) PlaybackAvailability.UNAVAILABLE else PlaybackAvailability.AVAILABLE
            )
        }
    }

    override suspend fun checkAvailability(videoId: String): PlaybackAvailability {
        return try {
            val item = api.getVideoDetails(videoId)
            if (item.isDeleted || item.isLocked) {
                PlaybackAvailability.UNAVAILABLE
            } else {
                PlaybackAvailability.AVAILABLE
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
                uriString = version.videoUrl.ifBlank { "https://rutube.ru/video/${version.sourceVideoId}/" }
            )
        }
    }
}
