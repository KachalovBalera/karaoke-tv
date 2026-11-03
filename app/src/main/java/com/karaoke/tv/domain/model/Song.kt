package com.karaoke.tv.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String = "",
    val category: String = "Хиты",
    val versions: List<KaraokeVersion> = emptyList(),
    val isFavorite: Boolean = false,
    val timesSung: Int = 0,
    val bestVersionId: String? = null
) {
    val bestVersion: KaraokeVersion?
        get() = versions.find { it.id == bestVersionId } ?: versions.firstOrNull()

    val displayTitle: String
        get() = "$artist — $title"
}
