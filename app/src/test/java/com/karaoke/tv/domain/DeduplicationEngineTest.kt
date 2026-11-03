package com.karaoke.tv.domain

import com.karaoke.tv.domain.engine.DeduplicationEngine
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduplicationEngineTest {

    @Test
    fun testParseVideoTitleNormalizesArtistAndTitle() {
        val raw = "Кино - Группа крови (Караоке с текстом 1080p)"
        val parsed = DeduplicationEngine.parseVideoTitle(raw)

        assertEquals("Кино", parsed.artist)
        assertEquals("Группа Крови", parsed.title)
        assertFalse(parsed.isRemix)
        assertFalse(parsed.isAcoustic)
    }

    @Test
    fun testRemixDistinctionPreventsMergingWithOriginal() {
        val original = DeduplicationEngine.parseVideoTitle("Руки Вверх - Крошка моя (Караоке)")
        val remix = DeduplicationEngine.parseVideoTitle("Руки Вверх - Крошка моя (Club Remix Karaoke)")

        assertTrue(remix.isRemix)
        assertFalse(original.isRemix)
        assertFalse("Remix and original should not be merged", DeduplicationEngine.isSameSong(original, remix))
    }

    @Test
    fun testAcousticDistinctionPreventsMergingWithOriginal() {
        val original = DeduplicationEngine.parseVideoTitle("Любэ - Конь (Караоке с текстом)")
        val acoustic = DeduplicationEngine.parseVideoTitle("Любэ - Конь (Акустика фортепиано минусовка)")

        assertTrue(acoustic.isAcoustic)
        assertFalse(original.isAcoustic)
        assertFalse("Acoustic version should not be merged into regular version", DeduplicationEngine.isSameSong(original, acoustic))
    }

    @Test
    fun testDifferentCompositionsAreNotMerged() {
        val song1 = DeduplicationEngine.parseVideoTitle("Кино - Группа крови")
        val song2 = DeduplicationEngine.parseVideoTitle("Кино - Звезда по имени Солнце")

        assertFalse("Different songs by same artist must not merge", DeduplicationEngine.isSameSong(song1, song2))
    }

    @Test
    fun testFuzzyMatchingUnifiesVersionsAcrossSources() {
        val driveVersion = KaraokeVersion(
            id = "drive_1",
            songId = "",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "v1",
            title = "Кино - Группа крови Караоке версия HQ",
            quality = "1080p"
        )
        val localVersion = KaraokeVersion(
            id = "drive_2",
            songId = "",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "v2",
            title = "Кино — Группа крови (Караоке с текстом на экране)",
            quality = "1080p"
        )
        val localVersion2 = KaraokeVersion(
            id = "drive_3",
            songId = "",
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "v3",
            title = "Группа крови - Кино (минусовка с бегущей строкой)",
            quality = "720p"
        )

        val grouped = DeduplicationEngine.groupVersionsIntoSongs(listOf(driveVersion, localVersion, localVersion2))

        assertEquals("Should collapse into a single unified Song card", 1, grouped.size)
        val song = grouped.first()
        assertEquals("Кино", song.artist)
        assertEquals("Группа Крови", song.title)
        assertEquals("Should contain all 3 source versions inside", 3, song.versions.size)
    }

    @Test
    fun testCleanStopWordsStripsNoise() {
        val raw = "Queen - Bohemian Rhapsody (Official Karaoke Lyrics Backing Track HD 4K)"
        val parsed = DeduplicationEngine.parseVideoTitle(raw)

        assertEquals("Queen", parsed.artist)
        assertEquals("Bohemian Rhapsody", parsed.title)
    }
}
