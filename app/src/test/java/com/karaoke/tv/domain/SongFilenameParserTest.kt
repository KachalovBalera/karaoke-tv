package com.karaoke.tv.domain

import com.karaoke.tv.domain.engine.SongFilenameParser
import org.junit.Assert.assertEquals
import org.junit.Test

class SongFilenameParserTest {

    @Test
    fun testGivenFilenames() {
        val testCases = mapOf(
            "#2Маши - Босая.mp4" to ("2Маши" to "Босая"),
            "2Маши - Босая.mp4" to ("2Маши" to "Босая"),
            "2Маши - Инея.mp4" to ("2Маши" to "Инея"),
            "2Маши - Мама, я танцую.mp4" to ("2Маши" to "Мама, я танцую"),
            "4G - Звонки.mp4" to ("4G" to "Звонки"),
            "5sta Family - Вместе мы.mp4" to ("5sta Family" to "Вместе мы"),
            "7 Hills - Танцы.mp4" to ("7 Hills" to "Танцы"),
            "7Б - Молодые ветра.mp4" to ("7Б" to "Молодые ветра"),
            "10AGE - Нет интереса.mp4" to ("10AGE" to "Нет интереса"),
            "30.02 - Примером.mp4" to ("30.02" to "Примером"),
            "50 Cent - In Da Club 4K.mp4" to ("50 Cent" to "In Da Club"),
            "Баста - Сансара (Караоке).mp4" to ("Баста" to "Сансара"),
            "Звери — Районы-кварталы.mp4" to ("Звери" to "Районы-кварталы")
        )

        testCases.forEach { (filename, expected) ->
            val parsed = SongFilenameParser.parse(filename)
            assertEquals("Artist failed for $filename", expected.first, parsed.artist)
            assertEquals("Title failed for $filename", expected.second, parsed.title)
        }
    }

    @Test
    fun testNormalizeText() {
        val s1 = SongFilenameParser.normalizeText("Моё сердце")
        val s2 = SongFilenameParser.normalizeText("Мое сердце")
        assertEquals(s1, s2)
    }

    @Test
    fun testFallbackWhenNoDash() {
        val parsed = SongFilenameParser.parse("Калинка Малинка.mp4")
        assertEquals("Неизвестен", parsed.artist)
        assertEquals("Калинка Малинка", parsed.title)
    }
}
