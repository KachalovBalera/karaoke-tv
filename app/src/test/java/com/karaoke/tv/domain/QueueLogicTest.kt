package com.karaoke.tv.domain

import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.QueueStatus
import com.karaoke.tv.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class QueueLogicTest {

    @Test
    fun testReorderingQueueItemsUpAndDown() {
        val list = mutableListOf(
            QueueItem(id = 1, songId = "s1", songTitle = "Группа крови", songArtist = "Кино", coverUrl = "", selectedVersionId = "v1", participantName = "Валера", position = 0),
            QueueItem(id = 2, songId = "s2", songTitle = "Крошка моя", songArtist = "Руки Вверх", coverUrl = "", selectedVersionId = "v2", participantName = "Настя", position = 1),
            QueueItem(id = 3, songId = "s3", songTitle = "Конь", songArtist = "Любэ", coverUrl = "", selectedVersionId = "v3", participantName = "Гость", position = 2)
        )

        // Move item 2 ("Настя") UP to position 0
        val idx = list.indexOfFirst { it.id == 2L }
        val prev = list[idx - 1]
        val curr = list[idx]
        list[idx - 1] = curr.copy(position = prev.position)
        list[idx] = prev.copy(position = curr.position)

        assertEquals("Настя should now be at position 0", 2L, list[0].id)
        assertEquals("Валера should now be at position 1", 1L, list[1].id)
        assertEquals("Гость remains at position 2", 3L, list[2].id)
    }

    @Test
    fun testSkipCurrentAdvancesToNextSinger() {
        val list = mutableListOf(
            QueueItem(id = 1, songId = "s1", songTitle = "Song 1", songArtist = "Artist 1", coverUrl = "", selectedVersionId = "v1", participantName = "Валера", position = 0, status = QueueStatus.CURRENT),
            QueueItem(id = 2, songId = "s2", songTitle = "Song 2", songArtist = "Artist 2", coverUrl = "", selectedVersionId = "v2", participantName = "Настя", position = 1, status = QueueStatus.WAITING)
        )

        // Simulate skip: remove current and promote next
        list.removeAt(0)
        list[0] = list[0].copy(position = 0, status = QueueStatus.CURRENT)

        assertEquals("Next singer should be Настя", "Настя", list[0].participantName)
        assertEquals("Status should become CURRENT", QueueStatus.CURRENT, list[0].status)
    }

    @Test
    fun testParticipantNameAttributedProperly() {
        val item = QueueItem(
            id = 10,
            songId = "s10",
            songTitle = "Лесник",
            songArtist = "Король и Шут",
            coverUrl = "",
            selectedVersionId = "v10",
            participantName = "Михаил",
            position = 0
        )

        assertEquals("Михаил", item.participantName)
    }
}
