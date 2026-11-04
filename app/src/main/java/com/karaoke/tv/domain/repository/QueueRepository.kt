package com.karaoke.tv.domain.repository

import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface QueueRepository {
    fun getQueueFlow(): Flow<List<QueueItem>>
    suspend fun getQueue(): List<QueueItem>
    suspend fun addToQueue(song: Song, version: KaraokeVersion, participantName: String): QueueItem
    suspend fun playImmediately(song: Song, version: KaraokeVersion, participantName: String): QueueItem
    suspend fun removeFromQueue(itemId: Long)
    suspend fun moveUp(itemId: Long)
    suspend fun moveDown(itemId: Long)
    suspend fun skipCurrent(): QueueItem?
    suspend fun repeatCurrent(): QueueItem?
    suspend fun clearQueue()
    suspend fun updateParticipantName(itemId: Long, newName: String)
}
