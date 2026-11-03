package com.karaoke.tv.domain.usecase

import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus

data class RankingPreferences(
    val preferredSource: SourceType? = null,
    val preferInAppPlayback: Boolean = true
)

class RankKaraokeVersionsUseCase {

    /**
     * Ranks a list of KaraokeVersion candidates for a given song, returning the list
     * sorted from best to worst, with the first item being the recommended best version.
     */
    fun rank(
        versions: List<KaraokeVersion>,
        preferences: RankingPreferences = RankingPreferences()
    ): List<KaraokeVersion> {
        if (versions.isEmpty()) return emptyList()

        return versions.sortedByDescending { calculateCompositeScore(it, preferences) }
    }

    private fun calculateCompositeScore(
        version: KaraokeVersion,
        preferences: RankingPreferences
    ): Double {
        var score = 0.0

        // 1. Availability check (must be playable!)
        when (version.availabilityStatus) {
            PlaybackAvailability.AVAILABLE -> score += 100.0
            PlaybackAvailability.NEEDS_EXTERNAL_APP -> score += 40.0
            PlaybackAvailability.SOURCE_UNCONFIGURED -> score += 10.0
            PlaybackAvailability.UNKNOWN -> score += 50.0
            PlaybackAvailability.UNAVAILABLE -> score -= 200.0
        }

        // 2. Karaoke probability score (0.0 to 1.0 -> up to 50 pts)
        score += version.karaokeScore * 50.0

        // 3. User verification status
        when (version.verificationStatus) {
            VerificationStatus.VERIFIED_KARAOKE -> score += 40.0
            VerificationStatus.PROBABLE_KARAOKE -> score += 20.0
            VerificationStatus.UNVERIFIED -> score += 5.0
            VerificationStatus.NOT_KARAOKE -> score -= 100.0
        }

        // 4. Local User ratings (stars 1-5, weighted by rating count)
        if (version.ratingCount > 0) {
            // Confidence weight: asymptotically approaches 1.0 with more ratings
            val confidence = 1.0 - (1.0 / (version.ratingCount + 1))
            val starDiff = (version.userRatingAverage - 3.0) // range -2.0 to +2.0
            score += starDiff * 25.0 * confidence
        }

        // 5. Video quality preference (1080p, 4K, 720p)
        when {
            version.quality.contains("4K", ignoreCase = true) || version.quality.contains("2160", ignoreCase = true) -> score += 15.0
            version.quality.contains("1080", ignoreCase = true) || version.quality.contains("HD", ignoreCase = true) -> score += 12.0
            version.quality.contains("720", ignoreCase = true) -> score += 8.0
            else -> score += 3.0
        }

        // 6. User source preference (from Settings)
        if (preferences.preferredSource != null && version.source == preferences.preferredSource) {
            score += 500.0
        }

        // 7. In-app embeddable bonus
        if (preferences.preferInAppPlayback && version.isEmbeddable) {
            score += 15.0
        }

        return score
    }
}
