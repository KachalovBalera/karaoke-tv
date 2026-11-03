package com.karaoke.tv.domain.model

enum class QueueStatus {
    WAITING,
    CURRENT,
    FINISHED,
    SKIPPED
}

data class QueueItem(
    val id: Long = 0,
    val songId: String,
    val songTitle: String,
    val songArtist: String,
    val coverUrl: String,
    val selectedVersionId: String,
    val selectedVersion: KaraokeVersion? = null,
    val participantName: String = "Гость",
    val position: Int = 0,
    val status: QueueStatus = QueueStatus.WAITING,
    val addedAt: Long = System.currentTimeMillis()
)
