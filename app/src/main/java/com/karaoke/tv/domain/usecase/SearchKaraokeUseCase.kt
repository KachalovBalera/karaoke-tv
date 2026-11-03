package com.karaoke.tv.domain.usecase

import com.karaoke.tv.data.local.KaraokeDatabase
import com.karaoke.tv.data.local.entity.SearchHistoryEntity
import com.karaoke.tv.data.local.entity.SongEntity
import com.karaoke.tv.domain.engine.DeduplicationEngine
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.source.SourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SearchResultState(
    val songs: List<Song>,
    val errorsBySource: Map<SourceType, String> = emptyMap(),
    val totalFoundVersions: Int = 0
)

class SearchKaraokeUseCase(
    private val sourceManager: SourceManager,
    private val database: KaraokeDatabase,
    private val rankUseCase: RankKaraokeVersionsUseCase = RankKaraokeVersionsUseCase()
) {
    suspend fun execute(
        query: String,
        sourceFilter: SourceType? = null,
        rankingPreferences: RankingPreferences = RankingPreferences()
    ): SearchResultState = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return@withContext SearchResultState(emptyList())
        }

        // Record search history
        database.historyDao().insertSearchQuery(SearchHistoryEntity(trimmedQuery))

        // First check local DB for instant offline responsiveness (normalized search)
        val normalized = com.karaoke.tv.domain.engine.SongFilenameParser.normalize(trimmedQuery)
        val localEntities = database.songDao().searchSongsNormalized(normalized).ifEmpty {
            database.songDao().searchSongs(trimmedQuery)
        }
        val localSongs = localEntities.map { entity ->
            val versions = database.karaokeVersionDao().getVersionsForSong(entity.id).map { it.toDomain() }
            val rankedVersions = rankUseCase.rank(versions, rankingPreferences)
            val best = rankedVersions.firstOrNull()
            Song(
                id = entity.id,
                title = entity.title,
                artist = entity.artist,
                coverUrl = best?.thumbnailUrl?.takeIf { it.isNotBlank() } ?: entity.coverUrl,
                category = entity.category,
                versions = rankedVersions,
                bestVersionId = best?.id
            )
        }

        // If filtering by GOOGLE_DRIVE or local songs found, return local DB hits immediately
        if (sourceFilter == SourceType.GOOGLE_DRIVE) {
            return@withContext SearchResultState(
                songs = localSongs,
                errorsBySource = emptyMap(),
                totalFoundVersions = localSongs.sumOf { it.versions.size }
            )
        }

        // Query remote sources concurrently
        val sourceResults = sourceManager.searchAll(
            query = trimmedQuery,
            filterSource = sourceFilter
        )

        val allVersions = mutableListOf<KaraokeVersion>()
        val errors = mutableMapOf<SourceType, String>()

        for ((sourceType, result) in sourceResults) {
            result.onSuccess { versions ->
                allVersions.addAll(versions)
            }.onFailure { error ->
                errors[sourceType] = error.localizedMessage ?: "Ошибка поиска в ${sourceType.displayName}"
            }
        }

        // Group into deduplicated Song entities
        val deduplicatedSongs = DeduplicationEngine.groupVersionsIntoSongs(allVersions)

        // Rank versions inside each song according to ranking algorithm
        val fullyRankedSongs = deduplicatedSongs.map { song ->
            val rankedVersions = rankUseCase.rank(song.versions, rankingPreferences)
            val best = rankedVersions.firstOrNull()
            song.copy(
                coverUrl = best?.thumbnailUrl?.takeIf { it.isNotBlank() } ?: song.coverUrl,
                versions = rankedVersions,
                bestVersionId = best?.id
            )
        }

        // Merge local hits with remote hits without duplicating songs
        val mergedSongs = mutableListOf<Song>()
        mergedSongs.addAll(fullyRankedSongs)

        for (localSong in localSongs) {
            val alreadyPresent = mergedSongs.any {
                DeduplicationEngine.calculateSimilarity(it.artist, localSong.artist) > 0.8 &&
                        DeduplicationEngine.calculateSimilarity(it.title, localSong.title) > 0.8
            }
            if (!alreadyPresent) {
                mergedSongs.add(localSong)
            }
        }

        SearchResultState(
            songs = mergedSongs,
            errorsBySource = errors,
            totalFoundVersions = allVersions.size
        )
    }

    private fun com.karaoke.tv.data.local.entity.KaraokeVersionEntity.toDomain(): KaraokeVersion {
        return KaraokeVersion(
            id = id,
            songId = songId,
            source = source,
            sourceVideoId = sourceVideoId,
            title = title,
            channelOrAuthor = channelOrAuthor,
            thumbnailUrl = thumbnailUrl,
            durationSeconds = durationSeconds,
            videoUrl = videoUrl,
            embedUrl = embedUrl,
            quality = quality,
            karaokeScore = karaokeScore,
            userRatingAverage = userRatingAverage,
            ratingCount = ratingCount,
            isEmbeddable = isEmbeddable,
            verificationStatus = verificationStatus,
            availabilityStatus = availabilityStatus,
            lastCheckedTs = lastCheckedTs
        )
    }
}
