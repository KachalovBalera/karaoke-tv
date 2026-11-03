package com.karaoke.tv.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveDeduplicationTest {

    @Test
    fun testRussianCharactersAndYoNormalization() {
        // 'ё' and 'е' must produce identical normalized keys
        val key1 = SongFilenameParser.buildNormalizedKey("Алёна Апина", "Леха")
        val key2 = SongFilenameParser.buildNormalizedKey("Алена Апина", "Лёха")
        assertEquals(key1, key2)

        // Case insensitivity
        val key3 = SongFilenameParser.buildNormalizedKey("КИНО", "ГРУППА КРОВИ")
        val key4 = SongFilenameParser.buildNormalizedKey("кино", "группа крови")
        assertEquals(key3, key4)
    }

    @Test
    fun testArtistWithNumbersAndSpecialCharacters() {
        val s1 = SongFilenameParser.parse("7Б - Молодые ветра.mp4")
        assertEquals("7Б", s1.artist)
        assertEquals("Молодые ветра", s1.title)

        val s2 = SongFilenameParser.parse("#2Маши - Босая 4K.mp4")
        assertEquals("2Маши", s2.artist)
        assertEquals("Босая", s2.title)
        assertEquals("4K", s2.quality)

        val s3 = SongFilenameParser.parse("50 Cent - In Da Club (Караоке).mp4")
        assertEquals("50 Cent", s3.artist)
        assertEquals("In Da Club", s3.title)

        val s4 = SongFilenameParser.parse("30.02 - Призмой звёзд.mp4")
        assertEquals("30.02", s4.artist)
        assertEquals("призмой звезд", SongFilenameParser.normalizeText(s4.title))
    }

    @Test
    fun testDuplicateDetectionAndBestVersionSelection() {
        val files = listOf(
            "Кино - Группа крови.mp4",
            "#Кино - Группа крови 720p.mp4",
            "Кино - Группа крови 4K.mp4",
            "Кино - Группа крови (1080p).mp4"
        )

        // Group by normalized key
        val grouped = files.groupBy { filename ->
            val parsed = SongFilenameParser.parse(filename)
            parsed.normalizedKey
        }

        // All 4 files must be recognized as versions of the exact same song
        assertEquals("All 4 files must group into exactly 1 song", 1, grouped.size)

        val groupFiles = grouped.values.first()
        assertEquals(4, groupFiles.size)

        // Find best version
        val bestFile = groupFiles.maxByOrNull { f ->
            val q = SongFilenameParser.parse(f).quality.lowercase()
            when {
                q.contains("4k") -> 400
                q.contains("1080") -> 300
                q.contains("720") -> 200
                else -> 100
            }
        }

        assertNotNull(bestFile)
        assertTrue("Best version must be 4K", bestFile!!.contains("4K"))
    }

    @Test
    fun testSearchKeywordMatching() {
        val parsed = SongFilenameParser.parse("Король и Шут - Лесник.mp4")
        val keywords = "${SongFilenameParser.normalizeText(parsed.artist)} ${SongFilenameParser.normalizeText(parsed.title)}"

        // Match queries
        assertTrue(keywords.contains(SongFilenameParser.normalizeText("король")))
        assertTrue(keywords.contains(SongFilenameParser.normalizeText("шут")))
        assertTrue(keywords.contains(SongFilenameParser.normalizeText("лесник")))
        assertTrue(keywords.contains(SongFilenameParser.normalizeText("КОРОЛЬ И ШУТ")))
        assertFalse(keywords.contains(SongFilenameParser.normalizeText("би-2")))
    }
}
