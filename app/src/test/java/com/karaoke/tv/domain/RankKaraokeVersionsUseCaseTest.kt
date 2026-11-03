package com.karaoke.tv.domain

import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import com.karaoke.tv.domain.usecase.RankKaraokeVersionsUseCase
import com.karaoke.tv.domain.usecase.RankingPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankKaraokeVersionsUseCaseTest {

    private val rankUseCase = RankKaraokeVersionsUseCase()

    @Test
    fun testVerifiedVersionOutranksUnverifiedVersion() {
        val unverified = KaraokeVersion(
            id = "v1",
            songId = "s1",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "id1",
            title = "Песня 1",
            verificationStatus = VerificationStatus.UNVERIFIED,
            userRatingAverage = 0.0f,
            ratingCount = 0
        )
        val verified = KaraokeVersion(
            id = "v2",
            songId = "s1",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "id2",
            title = "Песня 2",
            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
            userRatingAverage = 4.8f,
            ratingCount = 10
        )

        val ranked = rankUseCase.rank(listOf(unverified, verified))
        assertEquals("Verified version with high ratings must rank #1", "v2", ranked.first().id)
    }

    @Test
    fun testUnavailableVersionIsPenalized() {
        val unavailable = KaraokeVersion(
            id = "v_dead",
            songId = "s1",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "id_dead",
            title = "Песня (заблокирована)",
            availabilityStatus = PlaybackAvailability.UNAVAILABLE,
            karaokeScore = 0.9f
        )
        val available = KaraokeVersion(
            id = "v_alive",
            songId = "s1",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "id_alive",
            title = "Песня",
            availabilityStatus = PlaybackAvailability.AVAILABLE,
            karaokeScore = 0.7f
        )

        val ranked = rankUseCase.rank(listOf(unavailable, available))
        assertEquals("Playable version must rank higher than dead/restricted video", "v_alive", ranked.first().id)
    }

    @Test
    fun testUserPreferredSourceBoostsRank() {
        val lowerScore = KaraokeVersion(
            id = "v_lower",
            songId = "s1",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "id1",
            title = "Песня",
            karaokeScore = 0.6f
        )
        val higherScore = KaraokeVersion(
            id = "v_higher",
            songId = "s1",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "id2",
            title = "Песня",
            karaokeScore = 0.95f
        )

        val prefGdrive = rankUseCase.rank(
            listOf(lowerScore, higherScore),
            RankingPreferences(preferredSource = SourceType.GOOGLE_DRIVE)
        )
        assertEquals("Higher score must rank #1", "v_higher", prefGdrive.first().id)
    }

    @Test
    fun testNonKaraokeVideoIsDemoted() {
        val realKaraoke = KaraokeVersion(
            id = "v_karaoke",
            songId = "s1",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "id1",
            title = "Караоке с текстом",
            karaokeScore = 0.95f,
            verificationStatus = VerificationStatus.PROBABLE_KARAOKE
        )
        val clipVideo = KaraokeVersion(
            id = "v_clip",
            songId = "s1",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "id2",
            title = "Официальный клип (не караоке)",
            karaokeScore = 0.15f,
            verificationStatus = VerificationStatus.NOT_KARAOKE
        )

        val ranked = rankUseCase.rank(listOf(clipVideo, realKaraoke))
        assertEquals("Real karaoke must rank first", "v_karaoke", ranked.first().id)
    }
}
