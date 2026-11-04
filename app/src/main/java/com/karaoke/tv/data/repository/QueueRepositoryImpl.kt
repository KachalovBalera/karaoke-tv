package com.karaoke.tv.data.repository

import com.karaoke.tv.data.local.KaraokeDatabase
import com.karaoke.tv.data.local.entity.QueueItemEntity
import com.karaoke.tv.data.local.entity.toDomain
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.QueueStatus
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.repository.QueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QueueRepositoryImpl(
    private val database: KaraokeDatabase
) : QueueRepository {

    override fun getQueueFlow(): Flow<List<QueueItem>> {
        return database.queueDao().getQueueFlow().map { entities ->
            entities.map { entity ->
                val version = (database.karaokeVersionDao().getVersionById(entity.selectedVersionId)
                    ?: database.karaokeVersionDao().getVersionsForSong(entity.songId).firstOrNull())?.toDomain()
                QueueItem(
                    id = entity.id,
                    songId = entity.songId,
                    songTitle = entity.songTitle,
                    songArtist = entity.songArtist,
                    coverUrl = entity.coverUrl,
                    selectedVersionId = version?.id ?: entity.selectedVersionId,
                    selectedVersion = version,
                    participantName = entity.participantName,
                    position = entity.position,
                    status = entity.status,
                    addedAt = entity.addedAt
                )
            }
        }
    }

    override suspend fun getQueue(): List<QueueItem> {
        val entities = database.queueDao().getQueue()
        return entities.map { entity ->
            val version = (database.karaokeVersionDao().getVersionById(entity.selectedVersionId)
                ?: database.karaokeVersionDao().getVersionsForSong(entity.songId).firstOrNull())?.toDomain()
            QueueItem(
                id = entity.id,
                songId = entity.songId,
                songTitle = entity.songTitle,
                songArtist = entity.songArtist,
                coverUrl = entity.coverUrl,
                selectedVersionId = version?.id ?: entity.selectedVersionId,
                selectedVersion = version,
                participantName = entity.participantName,
                position = entity.position,
                status = entity.status,
                addedAt = entity.addedAt
            )
        }
    }

    override suspend fun addToQueue(
        song: Song,
        version: KaraokeVersion,
        participantName: String
    ): QueueItem {
        val maxPos = database.queueDao().getMaxPosition() ?: -1
        val newPos = maxPos + 1

        val entity = QueueItemEntity(
            songId = song.id,
            songTitle = song.title,
            songArtist = song.artist,
            coverUrl = song.coverUrl,
            selectedVersionId = version.id,
            participantName = participantName.ifBlank { "Гость" },
            position = newPos,
            status = if (newPos == 0) QueueStatus.CURRENT else QueueStatus.WAITING
        )

        // Make sure version is saved in DB
        database.karaokeVersionDao().insertVersion(
            com.karaoke.tv.data.local.entity.KaraokeVersionEntity(
                id = version.id,
                songId = song.id,
                source = version.source,
                sourceVideoId = version.sourceVideoId,
                title = version.title,
                channelOrAuthor = version.channelOrAuthor,
                thumbnailUrl = version.thumbnailUrl,
                durationSeconds = version.durationSeconds,
                videoUrl = version.videoUrl,
                embedUrl = version.embedUrl,
                quality = version.quality,
                karaokeScore = version.karaokeScore,
                userRatingAverage = version.userRatingAverage,
                ratingCount = version.ratingCount,
                isEmbeddable = version.isEmbeddable,
                verificationStatus = version.verificationStatus,
                availabilityStatus = version.availabilityStatus,
                lastCheckedTs = version.lastCheckedTs
            )
        )

        val generatedId = database.queueDao().insertItem(entity)
        reindexQueue()

        return QueueItem(
            id = generatedId,
            songId = song.id,
            songTitle = song.title,
            songArtist = song.artist,
            coverUrl = song.coverUrl,
            selectedVersionId = version.id,
            selectedVersion = version,
            participantName = entity.participantName,
            position = newPos,
            status = entity.status
        )
    }

    override suspend fun playImmediately(
        song: Song,
        version: KaraokeVersion,
        participantName: String
    ): QueueItem {
        // Shift existing items down so existing user queue is fully preserved
        val existing = database.queueDao().getQueue()
        for (item in existing) {
            database.queueDao().updateItem(item.copy(position = item.position + 1, status = QueueStatus.WAITING))
        }

        database.karaokeVersionDao().insertVersion(
            com.karaoke.tv.data.local.entity.KaraokeVersionEntity(
                id = version.id,
                songId = song.id,
                source = version.source,
                sourceVideoId = version.sourceVideoId,
                title = version.title,
                channelOrAuthor = version.channelOrAuthor,
                thumbnailUrl = version.thumbnailUrl,
                durationSeconds = version.durationSeconds,
                videoUrl = version.videoUrl,
                embedUrl = version.embedUrl,
                quality = version.quality,
                karaokeScore = version.karaokeScore,
                userRatingAverage = version.userRatingAverage,
                ratingCount = version.ratingCount,
                isEmbeddable = version.isEmbeddable,
                verificationStatus = version.verificationStatus,
                availabilityStatus = version.availabilityStatus,
                lastCheckedTs = version.lastCheckedTs
            )
        )

        val entity = QueueItemEntity(
            songId = song.id,
            songTitle = song.title,
            songArtist = song.artist,
            coverUrl = song.coverUrl,
            selectedVersionId = version.id,
            participantName = participantName.ifBlank { "Гость" },
            position = 0,
            status = QueueStatus.CURRENT
        )

        val generatedId = database.queueDao().insertItem(entity)
        reindexQueue()

        return QueueItem(
            id = generatedId,
            songId = song.id,
            songTitle = song.title,
            songArtist = song.artist,
            coverUrl = song.coverUrl,
            selectedVersionId = version.id,
            selectedVersion = version,
            participantName = entity.participantName,
            position = 0,
            status = QueueStatus.CURRENT
        )
    }

    override suspend fun removeFromQueue(itemId: Long) {
        database.queueDao().deleteItemById(itemId)
        reindexQueue()
    }

    override suspend fun moveUp(itemId: Long) {
        val items = database.queueDao().getQueue().toMutableList()
        val index = items.indexOfFirst { it.id == itemId }
        if (index > 0) {
            val prev = items[index - 1]
            val curr = items[index]

            val updatedCurr = curr.copy(position = prev.position)
            val updatedPrev = prev.copy(position = curr.position)

            database.queueDao().updateItem(updatedCurr)
            database.queueDao().updateItem(updatedPrev)
            reindexQueue()
        }
    }

    override suspend fun moveDown(itemId: Long) {
        val items = database.queueDao().getQueue().toMutableList()
        val index = items.indexOfFirst { it.id == itemId }
        if (index >= 0 && index < items.size - 1) {
            val curr = items[index]
            val next = items[index + 1]

            val updatedCurr = curr.copy(position = next.position)
            val updatedNext = next.copy(position = curr.position)

            database.queueDao().updateItem(updatedCurr)
            database.queueDao().updateItem(updatedNext)
            reindexQueue()
        }
    }

    override suspend fun skipCurrent(): QueueItem? {
        val items = database.queueDao().getQueue()
        val current = items.firstOrNull() ?: return null

        database.queueDao().deleteItemById(current.id)
        reindexQueue()

        val nextItems = database.queueDao().getQueue()
        val nextCurrent = nextItems.firstOrNull() ?: return null
        val updatedNext = nextCurrent.copy(status = QueueStatus.CURRENT)
        database.queueDao().updateItem(updatedNext)

        val version = database.karaokeVersionDao().getVersionById(updatedNext.selectedVersionId)?.toDomain()
        return QueueItem(
            id = updatedNext.id,
            songId = updatedNext.songId,
            songTitle = updatedNext.songTitle,
            songArtist = updatedNext.songArtist,
            coverUrl = updatedNext.coverUrl,
            selectedVersionId = updatedNext.selectedVersionId,
            selectedVersion = version,
            participantName = updatedNext.participantName,
            position = updatedNext.position,
            status = updatedNext.status
        )
    }

    override suspend fun repeatCurrent(): QueueItem? {
        val items = database.queueDao().getQueue()
        val current = items.firstOrNull() ?: return null
        val updated = current.copy(status = QueueStatus.CURRENT)
        database.queueDao().updateItem(updated)
        val version = database.karaokeVersionDao().getVersionById(updated.selectedVersionId)?.toDomain()
        return QueueItem(
            id = updated.id,
            songId = updated.songId,
            songTitle = updated.songTitle,
            songArtist = updated.songArtist,
            coverUrl = updated.coverUrl,
            selectedVersionId = updated.selectedVersionId,
            selectedVersion = version,
            participantName = updated.participantName,
            position = updated.position,
            status = updated.status
        )
    }

    override suspend fun clearQueue() {
        database.queueDao().clearQueue()
    }

    override suspend fun updateParticipantName(itemId: Long, newName: String) {
        val items = database.queueDao().getQueue()
        val item = items.find { it.id == itemId } ?: return
        database.queueDao().updateItem(item.copy(participantName = newName))
    }

    private suspend fun reindexQueue() {
        val remaining = database.queueDao().getQueue()
        remaining.forEachIndexed { idx, entity ->
            val expectedStatus = if (idx == 0) QueueStatus.CURRENT else QueueStatus.WAITING
            if (entity.position != idx || entity.status != expectedStatus) {
                database.queueDao().updateItem(entity.copy(position = idx, status = expectedStatus))
            }
        }
    }
}
