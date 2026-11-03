package com.karaoke.tv.domain.source

import android.content.Context
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType

sealed interface SourceCapability {
    data object Supported : SourceCapability
    data class Unsupported(val reason: String) : SourceCapability
    data object Unknown : SourceCapability
}

sealed interface PlaybackLaunchResult {
    data class InAppPlayer(val embedUrl: String, val videoId: String) : PlaybackLaunchResult
    data class ExternalIntent(val intentAction: String, val uriString: String) : PlaybackLaunchResult
    data class Error(val message: String) : PlaybackLaunchResult
}

interface KaraokeSource {
    val sourceType: SourceType
    val isConfigured: Boolean

    suspend fun searchSongs(query: String, maxResults: Int = 20): Result<List<KaraokeVersion>>
    suspend fun getVideoDetails(videoId: String): Result<KaraokeVersion>
    suspend fun checkAvailability(videoId: String): PlaybackAvailability
    fun getPlaybackCapability(): SourceCapability
    fun openPlayback(context: Context, version: KaraokeVersion): PlaybackLaunchResult
}
