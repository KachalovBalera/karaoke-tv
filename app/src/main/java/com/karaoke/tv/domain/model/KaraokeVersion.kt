package com.karaoke.tv.domain.model

data class KaraokeVersion(
    val id: String,
    val songId: String,
    val source: SourceType,
    val sourceVideoId: String,
    val title: String,
    val channelOrAuthor: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val videoUrl: String = "",
    val embedUrl: String = "",
    val quality: String = "1080p",
    val karaokeScore: Float = 0.8f, // 0.0 to 1.0 (heuristic based on title, keywords)
    val userRatingAverage: Float = 0.0f, // 0.0 to 5.0
    val ratingCount: Int = 0,
    val isEmbeddable: Boolean = true,
    val verificationStatus: VerificationStatus = VerificationStatus.PROBABLE_KARAOKE,
    val availabilityStatus: PlaybackAvailability = PlaybackAvailability.AVAILABLE,
    val lastCheckedTs: Long = System.currentTimeMillis()
) {
    val durationFormatted: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
