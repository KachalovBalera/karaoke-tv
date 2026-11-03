package com.karaoke.tv.data.remote.youtube

import com.karaoke.tv.domain.engine.KaraokeScorer
import com.karaoke.tv.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeSourceTest {

    @Test
    fun parseIsoDuration_validFormats() {
        assertEquals(260, YouTubeSource.parseIsoDuration("PT4M20S"))
        assertEquals(3665, YouTubeSource.parseIsoDuration("PT1H1M5S"))
        assertEquals(180, YouTubeSource.parseIsoDuration("PT3M"))
        assertEquals(45, YouTubeSource.parseIsoDuration("PT45S"))
        assertEquals(0, YouTubeSource.parseIsoDuration(""))
        assertEquals(0, YouTubeSource.parseIsoDuration("invalid"))
    }

    @Test
    fun isConfigured_reflectsKeyPresence() {
        var key = ""
        val source = YouTubeSource(apiKeyProvider = { key })
        assertFalse(source.isConfigured)

        key = "AIzaSyDummyKey12345"
        assertTrue(source.isConfigured)

        key = "   "
        assertFalse(source.isConfigured)
    }

    @Test
    fun lyricsAndKaraoke_scoredHigherThanClips() {
        val karaokeWithLyrics = KaraokeScorer.evaluate(
            title = "Баста - Сансара (Караоке с текстом)",
            description = "Минусовка с текстом песни Баста Сансара для караоке",
            channelAuthor = "Караоке для всех"
        )
        assertTrue(karaokeWithLyrics.score >= 0.70f)
        assertTrue(karaokeWithLyrics.isKaraoke)
        assertEquals(VerificationStatus.PROBABLE_KARAOKE, karaokeWithLyrics.verificationStatus)

        val officialClip = KaraokeScorer.evaluate(
            title = "Баста - Сансара (Official Video) [Премьера клипа]",
            description = "Официальный клип Баста",
            channelAuthor = "Gazgolder"
        )
        assertTrue(officialClip.score < 0.30f)
        assertFalse(officialClip.isKaraoke)
        assertEquals(VerificationStatus.NOT_KARAOKE, officialClip.verificationStatus)

        val guitarTutorial = KaraokeScorer.evaluate(
            title = "Баста - Сансара (Разбор на гитаре, урок)",
            description = "Урок как играть Сансару",
            channelAuthor = "Гитарист"
        )
        assertEquals(VerificationStatus.NOT_KARAOKE, guitarTutorial.verificationStatus)
    }
}
