package com.karaoke.tv.domain.source

import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SourceManager(
    private val sources: Map<SourceType, KaraokeSource>
) {
    fun getSource(type: SourceType): KaraokeSource? = sources[type]

    /**
     * Searches all active sources concurrently.
     * If one source fails, other sources still return their results!
     */
    suspend fun searchAll(
        query: String,
        filterSource: SourceType? = null,
        maxResultsPerSource: Int = 15
    ): Map<SourceType, Result<List<KaraokeVersion>>> = coroutineScope {
        val targetSources = if (filterSource != null) {
            listOfNotNull(sources[filterSource])
        } else {
            sources.values.toList()
        }

        val deferredResults = targetSources.map { source ->
            async {
                val result = if (!source.isConfigured) {
                    Result.failure(IllegalStateException("${source.sourceType.displayName} не настроен в Настройках"))
                } else {
                    source.searchSongs(query, maxResultsPerSource)
                }
                source.sourceType to result
            }
        }

        deferredResults.awaitAll().toMap()
    }

    suspend fun checkAvailability(version: KaraokeVersion): PlaybackAvailability {
        val source = sources[version.source] ?: return PlaybackAvailability.UNKNOWN
        return source.checkAvailability(version.sourceVideoId)
    }
}
