package com.karaoke.tv.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karaoke.tv.data.local.entity.KaraokeVersionEntity
import com.karaoke.tv.data.local.entity.SongEntity
import com.karaoke.tv.domain.engine.SongFilenameParser
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseAndroidTest {

    private lateinit var db: KaraokeDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, KaraokeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testRussianCharactersAndYoSearchEquivalence() = runBlocking {
        val song1 = createSong("song_1", "Алёна Апина", "Лёха")
        val song2 = createSong("song_2", "Валерий Меладзе", "Мотылёк")
        val song3 = createSong("song_3", "Кино", "Группа крови")

        db.songDao().insertSongs(listOf(song1, song2, song3))

        assertEquals(3, db.songDao().getSongCount())

        // Search with 'е' must match title containing 'ё'
        val hitsMeladze = db.songDao().searchSongsNormalized("мотылек")
        assertEquals(1, hitsMeladze.size)
        assertEquals("Мотылёк", hitsMeladze.first().title)

        // Search with 'ё' must match title containing 'ё'
        val hitsMeladzeYo = db.songDao().searchSongsNormalized("мотылёк")
        assertEquals(1, hitsMeladzeYo.size)
        assertEquals("Мотылёк", hitsMeladzeYo.first().title)

        // Search "алена" matches "Алёна Апина"
        val hitsApina = db.songDao().searchSongsNormalized("алена")
        assertEquals(1, hitsApina.size)
        assertEquals("Алёна Апина", hitsApina.first().artist)
    }

    @Test
    fun testArtistWithNumbersAndSpecialCharacters() = runBlocking {
        val song1 = createSong("song_7b", "7Б", "Молодые ветра")
        val song2 = createSong("song_2mashi", "2Маши", "Босая")
        val song3 = createSong("song_50cent", "50 Cent", "In Da Club")
        val song4 = createSong("song_3002", "30.02", "Призмой звёзд")
        val song5 = createSong("song_kish", "Король и Шут", "Кукла колдуна")

        db.songDao().insertSongs(listOf(song1, song2, song3, song4, song5))

        assertEquals(5, db.songDao().getSongCount())

        // Search by numeric artists
        val search7b = db.songDao().searchSongsNormalized("7б")
        assertEquals(1, search7b.size)
        assertEquals("7Б", search7b.first().artist)

        val search2mashi = db.songDao().searchSongsNormalized("2маши")
        assertEquals(1, search2mashi.size)
        assertEquals("2Маши", search2mashi.first().artist)

        val search50cent = db.songDao().searchSongsNormalized("50 cent")
        assertEquals(1, search50cent.size)
        assertEquals("50 Cent", search50cent.first().artist)

        val search3002 = db.songDao().searchSongsNormalized("30.02")
        assertEquals(1, search3002.size)
        assertEquals("30.02", search3002.first().artist)
    }

    @Test
    fun testDeduplicationAndMultipleVersionsUnderOneSong() = runBlocking {
        val normalizedKey = SongFilenameParser.buildNormalizedKey("Кино", "Группа крови")
        val songId = "gdrive_song_kino_gk"

        val song = SongEntity(
            id = songId,
            title = "Группа крови",
            artist = "Кино",
            coverUrl = "",
            category = "Google Drive",
            timesSung = 0,
            bestVersionId = "ver_kino_4k",
            normalizedKey = normalizedKey,
            searchKeywords = "кино группа крови"
        )
        db.songDao().insertSong(song)

        // Version 1: 1080p
        val ver1 = KaraokeVersionEntity(
            id = "ver_kino_1080p",
            songId = songId,
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "drive_file_1",
            title = "Группа крови",
            channelOrAuthor = "Кино",
            thumbnailUrl = "",
            durationSeconds = 240,
            videoUrl = "https://example.com/1",
            embedUrl = "https://example.com/1",
            quality = "1080p",
            karaokeScore = 0.9f,
            userRatingAverage = 5.0f,
            ratingCount = 1,
            isEmbeddable = true,
            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
            availabilityStatus = PlaybackAvailability.AVAILABLE,
            lastCheckedTs = System.currentTimeMillis()
        )

        // Version 2: 4K
        val ver2 = KaraokeVersionEntity(
            id = "ver_kino_4k",
            songId = songId,
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = "drive_file_2",
            title = "Группа крови",
            channelOrAuthor = "Кино",
            thumbnailUrl = "",
            durationSeconds = 240,
            videoUrl = "https://example.com/2",
            embedUrl = "https://example.com/2",
            quality = "4K",
            karaokeScore = 0.95f,
            userRatingAverage = 5.0f,
            ratingCount = 1,
            isEmbeddable = true,
            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
            availabilityStatus = PlaybackAvailability.AVAILABLE,
            lastCheckedTs = System.currentTimeMillis()
        )

        db.karaokeVersionDao().insertVersions(listOf(ver1, ver2))

        // Verify: exactly 1 song in catalog
        assertEquals(1, db.songDao().getSongCount())

        // Verify: 2 versions under this song
        val versions = db.karaokeVersionDao().getVersionsForSong(songId)
        assertEquals(2, versions.size)

        // Best version is 4K
        val loadedSong = db.songDao().getSongById(songId)
        assertNotNull(loadedSong)
        assertEquals("ver_kino_4k", loadedSong!!.bestVersionId)
    }

    @Test
    fun testReSyncPreservesTimesSungWithoutDuplicates() = runBlocking {
        val songId = "song_zveri"
        val song = createSong(songId, "Звери", "Районы-кварталы")
        db.songDao().insertSong(song)

        // User sings the song 5 times
        repeat(5) {
            db.songDao().incrementTimesSung(songId)
        }

        val sungSong = db.songDao().getSongById(songId)
        assertEquals(5, sungSong?.timesSung)

        // Re-sync occurs: we insert updated entity preserving existing stats
        val existing = db.songDao().getSongById(songId)
        val resyncedSong = song.copy(
            timesSung = existing?.timesSung ?: 0,
            category = "Google Drive"
        )
        db.songDao().insertSong(resyncedSong)

        // Verify: still exactly 1 song, timesSung is STILL 5!
        assertEquals(1, db.songDao().getSongCount())
        val afterSync = db.songDao().getSongById(songId)
        assertEquals(5, afterSync?.timesSung)
    }

    @Test
    fun testAlphabeticalSorting() = runBlocking {
        val s1 = createSong("1", "Кино", "Пачка сигарет")
        val s2 = createSong("2", "Ария", "Штиль")
        val s3 = createSong("3", "Звери", "Дожди-пистолеты")
        val s4 = createSong("4", "Би-2", "Полковнику никто не пишет")

        db.songDao().insertSongs(listOf(s1, s2, s3, s4))

        val sorted = db.songDao().getAllSongs()
        assertEquals(4, sorted.size)
        assertEquals("Ария", sorted[0].artist)
        assertEquals("Би-2", sorted[1].artist)
        assertEquals("Звери", sorted[2].artist)
        assertEquals("Кино", sorted[3].artist)
    }

    private fun createSong(id: String, artist: String, title: String): SongEntity {
        val normKey = SongFilenameParser.buildNormalizedKey(artist, title)
        val keywords = "${SongFilenameParser.normalizeText(artist)} ${SongFilenameParser.normalizeText(title)}"
        return SongEntity(
            id = id,
            title = title,
            artist = artist,
            coverUrl = "",
            category = "Google Drive",
            timesSung = 0,
            bestVersionId = null,
            normalizedKey = normKey,
            searchKeywords = keywords
        )
    }
}
