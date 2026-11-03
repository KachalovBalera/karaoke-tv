package com.karaoke.tv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.QueueStatus
import com.karaoke.tv.domain.model.RatingReason
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val category: String = "Хиты",
    val timesSung: Int = 0,
    val bestVersionId: String? = null,
    val normalizedKey: String = "",
    val searchKeywords: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "karaoke_versions")
data class KaraokeVersionEntity(
    @PrimaryKey val id: String,
    val songId: String,
    val source: SourceType,
    val sourceVideoId: String,
    val title: String,
    val channelOrAuthor: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val videoUrl: String,
    val embedUrl: String,
    val quality: String,
    val karaokeScore: Float,
    val userRatingAverage: Float,
    val ratingCount: Int,
    val isEmbeddable: Boolean,
    val verificationStatus: VerificationStatus,
    val availabilityStatus: PlaybackAvailability,
    val lastCheckedTs: Long
)

fun KaraokeVersionEntity.toDomain(): KaraokeVersion {
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

@Entity(tableName = "ratings")
data class RatingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val versionId: String,
    val stars: Int, // 1 to 5
    val reasonsCsv: String, // Comma-separated RatingReason names
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val songTitle: String,
    val songArtist: String,
    val coverUrl: String,
    val selectedVersionId: String,
    val participantName: String,
    val position: Int,
    val status: QueueStatus,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val songsSung: Int = 0,
    val isHost: Boolean = false,
    val lastActive: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val versionId: String,
    val title: String,
    val artist: String,
    val participantName: String,
    val source: SourceType,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_failures")
data class PlaybackFailureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val versionId: String,
    val source: SourceType,
    val reason: String,
    val failedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "source_preferences")
data class SourcePreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val preferredSource: SourceType = SourceType.GOOGLE_DRIVE,
    val autoFallback: Boolean = true
)
