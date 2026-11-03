package com.karaoke.tv.data.remote.youtube

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.karaoke.tv.domain.engine.KaraokeScorer
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import com.karaoke.tv.domain.source.KaraokeSource
import com.karaoke.tv.domain.source.PlaybackLaunchResult
import com.karaoke.tv.domain.source.SourceCapability
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

interface YouTubeApiService {
    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("videoSyndicated") videoSyndicated: String = "true",
        @Query("maxResults") maxResults: Int = 20,
        @Query("q") query: String,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse

    @GET("youtube/v3/videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,contentDetails,status",
        @Query("id") id: String,
        @Query("key") apiKey: String
    ): YouTubeVideoDetailsResponse
}

class YouTubeSource(
    private var apiKeyProvider: () -> String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : KaraokeSource {

    override val sourceType: SourceType = SourceType.YOUTUBE

    override val isConfigured: Boolean
        get() = apiKeyProvider().trim().isNotBlank()

    private val gson = Gson()

    private val api: YouTubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YouTubeApiService::class.java)
    }

    private fun parseYouTubeHttpError(e: HttpException): String {
        return try {
            val errorJson = e.response()?.errorBody()?.string() ?: ""
            if (errorJson.isNotBlank()) {
                val errorResponse = gson.fromJson(errorJson, YouTubeErrorResponse::class.java)
                val detail = errorResponse?.error
                val reason = detail?.errors?.firstOrNull()?.reason ?: ""
                val msg = detail?.message ?: ""

                when {
                    reason == "quotaExceeded" || reason == "dailyLimitExceeded" || e.code() == 429 || msg.contains("quota", ignoreCase = true) ->
                        "Превышена суточная квота запросов YouTube Data API v3 (10 000 ед./день). Попробуйте позже или используйте RUTUBE / VK Видео."
                    reason == "keyInvalid" || (e.code() == 400 && (msg.contains("key", ignoreCase = true) || reason.contains("key", ignoreCase = true))) ->
                        "Неверный API-ключ YouTube Data API v3. Проверьте правильность ключа в файле конфигурации или Настройках."
                    reason == "accessNotConfigured" || msg.contains("has not been used", ignoreCase = true) || msg.contains("disabled", ignoreCase = true) ->
                        "YouTube Data API v3 не включен в Google Cloud Console. Включите сервис в консоли разработчика."
                    e.code() == 403 ->
                        "Доступ к YouTube API ограничен (403): ${detail?.message?.take(120) ?: "проверьте разрешения ключа"}."
                    else ->
                        "Ошибка YouTube API (${e.code()}): ${detail?.message?.take(120) ?: e.message()}"
                }
            } else {
                "Ошибка YouTube API (${e.code()}): ${e.message()}"
            }
        } catch (_: Exception) {
            "Ошибка YouTube API (${e.code()}): ${e.message()}"
        }
    }

    private fun buildEffectiveSearchQuery(rawQuery: String): String {
        val trimmed = rawQuery.trim()
        val lower = trimmed.lowercase(Locale.getDefault())

        val hasKaraoke = lower.contains("караоке") || lower.contains("karaoke")
        val hasMinus = lower.contains("минусовка") || lower.contains("минус")
        val hasLyrics = lower.contains("текст") || lower.contains("lyrics") || lower.contains("слова")

        return when {
            hasKaraoke && hasLyrics -> trimmed
            hasMinus && hasLyrics -> trimmed
            hasKaraoke && !hasLyrics -> "$trimmed с текстом"
            hasMinus && !hasLyrics -> "$trimmed с текстом"
            else -> "$trimmed караоке с текстом"
        }
    }

    override suspend fun searchSongs(query: String, maxResults: Int): Result<List<KaraokeVersion>> {
        val key = apiKeyProvider().trim()
        if (key.isBlank()) {
            return Result.failure(IllegalStateException("YouTube API-ключ не настроен. Добавьте его в secrets.properties или в Настройках приложения."))
        }

        return try {
            val primaryQuery = buildEffectiveSearchQuery(query)
            val limit = maxResults.coerceIn(5, 30)

            var searchResponse = api.searchVideos(
                query = primaryQuery,
                maxResults = limit,
                apiKey = key
            )

            // Fallback: If 0 results with strict lyrics query, try broader "$query караоке"
            if (searchResponse.items.isEmpty() && primaryQuery.contains("с текстом")) {
                val fallbackQuery = if (query.contains("караоке", ignoreCase = true) || query.contains("karaoke", ignoreCase = true)) {
                    query.trim()
                } else {
                    "${query.trim()} караоке"
                }
                if (fallbackQuery != primaryQuery) {
                    searchResponse = api.searchVideos(
                        query = fallbackQuery,
                        maxResults = limit,
                        apiKey = key
                    )
                }
            }

            val videoIds = searchResponse.items.mapNotNull { it.id.videoId.ifBlank { null } }
            if (videoIds.isEmpty()) {
                return Result.success(emptyList()) // Clean empty results, no error thrown
            }

            // Fetch details for duration, status and embeddability
            val detailsResponse = api.getVideoDetails(
                id = videoIds.joinToString(","),
                apiKey = key
            )

            val versions = mutableListOf<KaraokeVersion>()

            for (item in detailsResponse.items) {
                val title = item.snippet.title
                val desc = item.snippet.description
                val author = item.snippet.channelTitle

                val scoreResult = KaraokeScorer.evaluate(
                    title = title,
                    description = desc,
                    channelAuthor = author
                )

                // Filter out non-karaoke items (clips, reactions, guitar tutorials, vocal covers)
                if (scoreResult.verificationStatus == VerificationStatus.NOT_KARAOKE) {
                    continue
                }

                val titleLower = title.lowercase(Locale.getDefault())
                val descLower = desc.lowercase(Locale.getDefault())
                val hasLyrics = titleLower.contains("с текстом") ||
                        titleLower.contains("минусовка с текстом") ||
                        titleLower.contains("минус с текстом") ||
                        titleLower.contains("lyrics") ||
                        titleLower.contains("слова") ||
                        descLower.contains("с текстом") ||
                        descLower.contains("lyrics on screen")

                // Boost score for tracks with lyrics on screen
                val finalScore = if (hasLyrics) {
                    (scoreResult.score + 0.20f).coerceAtMost(1.0f)
                } else {
                    scoreResult.score
                }

                val isEmbeddable = item.status?.embeddable ?: true
                val thumbUrl = item.snippet.thumbnails?.high?.url
                    ?: item.snippet.thumbnails?.medium?.url
                    ?: item.snippet.thumbnails?.default?.url ?: ""
                val durationSec = parseIsoDuration(item.contentDetails?.duration ?: "")

                versions.add(
                    KaraokeVersion(
                        id = "yt_${item.id}",
                        songId = "",
                        source = SourceType.YOUTUBE,
                        sourceVideoId = item.id,
                        title = title,
                        channelOrAuthor = author,
                        thumbnailUrl = thumbUrl,
                        durationSeconds = durationSec,
                        videoUrl = "https://www.youtube.com/watch?v=${item.id}",
                        embedUrl = "https://www.youtube.com/embed/${item.id}?enablejsapi=1",
                        quality = "1080p",
                        karaokeScore = finalScore,
                        isEmbeddable = isEmbeddable,
                        verificationStatus = if (hasLyrics && scoreResult.verificationStatus == VerificationStatus.UNVERIFIED) {
                            VerificationStatus.PROBABLE_KARAOKE
                        } else {
                            scoreResult.verificationStatus
                        },
                        availabilityStatus = if (isEmbeddable) PlaybackAvailability.AVAILABLE else PlaybackAvailability.NEEDS_EXTERNAL_APP
                    )
                )
            }

            // Prioritize embeddable tracks and tracks with lyrics / high karaoke score
            val sorted = versions.sortedWith(
                compareByDescending<KaraokeVersion> { it.isEmbeddable }
                    .thenByDescending { it.karaokeScore }
            )

            Result.success(sorted)
        } catch (e: HttpException) {
            val friendlyMsg = parseYouTubeHttpError(e)
            Result.failure(Exception(friendlyMsg))
        } catch (e: IOException) {
            Result.failure(Exception("Ошибка сети при обращении к YouTube: проверьте подключение к Интернету."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getVideoDetails(videoId: String): Result<KaraokeVersion> {
        val key = apiKeyProvider().trim()
        if (key.isBlank()) {
            return Result.failure(IllegalStateException("YouTube API-ключ не настроен"))
        }

        return try {
            val details = api.getVideoDetails(id = videoId, apiKey = key)
            val item = details.items.firstOrNull() ?: throw NoSuchElementException("Видео не найдено")

            val durationSec = parseIsoDuration(item.contentDetails?.duration ?: "")
            val scoreResult = KaraokeScorer.evaluate(
                title = item.snippet.title,
                description = item.snippet.description,
                channelAuthor = item.snippet.channelTitle
            )

            val titleLower = item.snippet.title.lowercase(Locale.getDefault())
            val descLower = item.snippet.description.lowercase(Locale.getDefault())
            val hasLyrics = titleLower.contains("с текстом") ||
                    titleLower.contains("минусовка с текстом") ||
                    titleLower.contains("минус с текстом") ||
                    titleLower.contains("lyrics") ||
                    titleLower.contains("слова") ||
                    descLower.contains("с текстом") ||
                    descLower.contains("lyrics on screen")

            val finalScore = if (hasLyrics) {
                (scoreResult.score + 0.20f).coerceAtMost(1.0f)
            } else {
                scoreResult.score
            }

            val isEmbeddable = item.status?.embeddable ?: true
            val thumbUrl = item.snippet.thumbnails?.high?.url ?: ""

            Result.success(
                KaraokeVersion(
                    id = "yt_${item.id}",
                    songId = "",
                    source = SourceType.YOUTUBE,
                    sourceVideoId = item.id,
                    title = item.snippet.title,
                    channelOrAuthor = item.snippet.channelTitle,
                    thumbnailUrl = thumbUrl,
                    durationSeconds = durationSec,
                    videoUrl = "https://www.youtube.com/watch?v=${item.id}",
                    embedUrl = "https://www.youtube.com/embed/${item.id}?enablejsapi=1",
                    quality = "1080p",
                    karaokeScore = finalScore,
                    isEmbeddable = isEmbeddable,
                    verificationStatus = if (hasLyrics && scoreResult.verificationStatus == VerificationStatus.UNVERIFIED) {
                        VerificationStatus.PROBABLE_KARAOKE
                    } else {
                        scoreResult.verificationStatus
                    },
                    availabilityStatus = if (isEmbeddable) PlaybackAvailability.AVAILABLE else PlaybackAvailability.NEEDS_EXTERNAL_APP
                )
            )
        } catch (e: HttpException) {
            val friendlyMsg = parseYouTubeHttpError(e)
            Result.failure(Exception(friendlyMsg))
        } catch (e: IOException) {
            Result.failure(Exception("Ошибка сети при обращении к YouTube: проверьте подключение к Интернету."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkAvailability(videoId: String): PlaybackAvailability {
        val key = apiKeyProvider().trim()
        if (key.isBlank()) return PlaybackAvailability.SOURCE_UNCONFIGURED

        return try {
            val details = api.getVideoDetails(id = videoId, apiKey = key)
            val item = details.items.firstOrNull() ?: return PlaybackAvailability.UNAVAILABLE
            val isEmbeddable = item.status?.embeddable ?: true
            if (isEmbeddable) PlaybackAvailability.AVAILABLE else PlaybackAvailability.NEEDS_EXTERNAL_APP
        } catch (_: Exception) {
            PlaybackAvailability.UNKNOWN
        }
    }

    override fun getPlaybackCapability(): SourceCapability {
        return SourceCapability.Supported
    }

    override fun openPlayback(context: Context, version: KaraokeVersion): PlaybackLaunchResult {
        return if (version.isEmbeddable) {
            PlaybackLaunchResult.InAppPlayer(
                embedUrl = version.embedUrl,
                videoId = version.sourceVideoId
            )
        } else {
            PlaybackLaunchResult.ExternalIntent(
                intentAction = Intent.ACTION_VIEW,
                uriString = "https://www.youtube.com/watch?v=${version.sourceVideoId}"
            )
        }
    }

    companion object {
        fun parseIsoDuration(isoDuration: String): Int {
            if (isoDuration.isBlank()) return 0
            val pattern = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
            val matcher = pattern.matcher(isoDuration)
            if (matcher.matches()) {
                val hours = matcher.group(1)?.toIntOrNull() ?: 0
                val minutes = matcher.group(2)?.toIntOrNull() ?: 0
                val seconds = matcher.group(3)?.toIntOrNull() ?: 0
                return hours * 3600 + minutes * 60 + seconds
            }
            return 0
        }
    }
}
