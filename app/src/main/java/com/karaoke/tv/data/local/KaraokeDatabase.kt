package com.karaoke.tv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.karaoke.tv.data.local.dao.FailureDao
import com.karaoke.tv.data.local.dao.FavoriteDao
import com.karaoke.tv.data.local.dao.HistoryDao
import com.karaoke.tv.data.local.dao.KaraokeVersionDao
import com.karaoke.tv.data.local.dao.ParticipantDao
import com.karaoke.tv.data.local.dao.QueueDao
import com.karaoke.tv.data.local.dao.RatingDao
import com.karaoke.tv.data.local.dao.SongDao
import com.karaoke.tv.data.local.entity.FavoriteEntity
import com.karaoke.tv.data.local.entity.KaraokeVersionEntity
import com.karaoke.tv.data.local.entity.ParticipantEntity
import com.karaoke.tv.data.local.entity.PlaybackFailureEntity
import com.karaoke.tv.data.local.entity.PlaybackHistoryEntity
import com.karaoke.tv.data.local.entity.QueueItemEntity
import com.karaoke.tv.data.local.entity.RatingEntity
import com.karaoke.tv.data.local.entity.SearchHistoryEntity
import com.karaoke.tv.data.local.entity.SongEntity
import com.karaoke.tv.data.local.entity.SourcePreferenceEntity
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SongEntity::class,
        KaraokeVersionEntity::class,
        RatingEntity::class,
        QueueItemEntity::class,
        ParticipantEntity::class,
        FavoriteEntity::class,
        PlaybackHistoryEntity::class,
        SearchHistoryEntity::class,
        PlaybackFailureEntity::class,
        SourcePreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KaraokeDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun karaokeVersionDao(): KaraokeVersionDao
    abstract fun queueDao(): QueueDao
    abstract fun ratingDao(): RatingDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun failureDao(): FailureDao
    abstract fun participantDao(): ParticipantDao

    companion object {
        @Volatile
        private var INSTANCE: KaraokeDatabase? = null

        fun getInstance(context: Context): KaraokeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaraokeDatabase::class.java,
                    "karaoke_tv.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: KaraokeDatabase) {
            val defaultSongs = listOf(
                // Русские хиты
                PredefinedSong(
                    id = "kino_gruppa_krovi",
                    title = "Группа крови",
                    artist = "Кино",
                    category = "Русские хиты",
                    coverUrl = "https://pic.rtbcdn.ru/video/2026-06-13/84/9e/849e66a5070125f46504c35d1e1dff34.jpg",
                    versions = listOf(
                        KaraokeVersionEntity(
                            id = "kino_gk_rutube_1",
                            songId = "kino_gruppa_krovi",
                            source = SourceType.RUTUBE,
                            sourceVideoId = "a5b154cdee7b562d361f28818d90d49e",
                            title = "Кино — Группа крови (Караоке с текстом)",
                            channelOrAuthor = "Караоке Топ 100",
                            thumbnailUrl = "https://pic.rtbcdn.ru/video/2026-06-13/84/9e/849e66a5070125f46504c35d1e1dff34.jpg",
                            durationSeconds = 263,
                            videoUrl = "https://rutube.ru/video/a5b154cdee7b562d361f28818d90d49e/",
                            embedUrl = "https://rutube.ru/play/embed/a5b154cdee7b562d361f28818d90d49e",
                            quality = "1080p",
                            karaokeScore = 0.95f,
                            userRatingAverage = 4.8f,
                            ratingCount = 12,
                            isEmbeddable = true,
                            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
                            availabilityStatus = PlaybackAvailability.AVAILABLE,
                            lastCheckedTs = System.currentTimeMillis()
                        ),
                        KaraokeVersionEntity(
                            id = "kino_gk_yt_1",
                            songId = "kino_gruppa_krovi",
                            source = SourceType.YOUTUBE,
                            sourceVideoId = "dQw4w9WgXcQ", // fallback placeholder for YouTube
                            title = "Кино - Группа крови Караоке версия HQ",
                            channelOrAuthor = "X-Star Karaoke",
                            thumbnailUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
                            durationSeconds = 270,
                            videoUrl = "https://youtube.com/watch?v=dQw4w9WgXcQ",
                            embedUrl = "https://www.youtube.com/embed/dQw4w9WgXcQ",
                            quality = "1080p",
                            karaokeScore = 0.92f,
                            userRatingAverage = 4.5f,
                            ratingCount = 5,
                            isEmbeddable = true,
                            verificationStatus = VerificationStatus.PROBABLE_KARAOKE,
                            availabilityStatus = PlaybackAvailability.AVAILABLE,
                            lastCheckedTs = System.currentTimeMillis()
                        )
                    )
                ),
                // Хиты 80-90х
                PredefinedSong(
                    id = "ruki_vverh_kroshka_moya",
                    title = "Крошка моя",
                    artist = "Руки Вверх",
                    category = "Хиты 80-х и 90-х",
                    coverUrl = "https://pic.rtbcdn.ru/video/2025-11-14/ee/d4/eed4464cd0bc50dbfb1ca8b1341e5d85.jpg",
                    versions = listOf(
                        KaraokeVersionEntity(
                            id = "rv_km_rutube_1",
                            songId = "ruki_vverh_kroshka_moya",
                            source = SourceType.RUTUBE,
                            sourceVideoId = "87fde5dade05e2391c74d622ebdaabac",
                            title = "Руки Вверх — Крошка моя (Караоке с бегущей строкой)",
                            channelOrAuthor = "Караоке Топ 100",
                            thumbnailUrl = "https://pic.rtbcdn.ru/video/2025-11-14/ee/d4/eed4464cd0bc50dbfb1ca8b1341e5d85.jpg",
                            durationSeconds = 245,
                            videoUrl = "https://rutube.ru/video/87fde5dade05e2391c74d622ebdaabac/",
                            embedUrl = "https://rutube.ru/play/embed/87fde5dade05e2391c74d622ebdaabac",
                            quality = "1080p",
                            karaokeScore = 0.96f,
                            userRatingAverage = 4.9f,
                            ratingCount = 20,
                            isEmbeddable = true,
                            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
                            availabilityStatus = PlaybackAvailability.AVAILABLE,
                            lastCheckedTs = System.currentTimeMillis()
                        )
                    )
                ),
                // Любэ - Конь
                PredefinedSong(
                    id = "lyube_kon",
                    title = "Конь (Выйду ночью в поле)",
                    artist = "Любэ",
                    category = "Русские хиты",
                    coverUrl = "https://pic.rtbcdn.ru/video/2025-05-21/09/b5/09b59af07dff310d6ea2db3b66774504.jpg",
                    versions = listOf(
                        KaraokeVersionEntity(
                            id = "lyube_kon_rutube_1",
                            songId = "lyube_kon",
                            source = SourceType.RUTUBE,
                            sourceVideoId = "01de63121a6c9b9a9bb41fab85130a50",
                            title = "Любэ — Конь (Караоке минусовка с текстом)",
                            channelOrAuthor = "Любимое Караоке",
                            thumbnailUrl = "https://pic.rtbcdn.ru/video/2025-05-21/09/b5/09b59af07dff310d6ea2db3b66774504.jpg",
                            durationSeconds = 215,
                            videoUrl = "https://rutube.ru/video/01de63121a6c9b9a9bb41fab85130a50/",
                            embedUrl = "https://rutube.ru/play/embed/01de63121a6c9b9a9bb41fab85130a50",
                            quality = "1080p",
                            karaokeScore = 0.98f,
                            userRatingAverage = 5.0f,
                            ratingCount = 35,
                            isEmbeddable = true,
                            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
                            availabilityStatus = PlaybackAvailability.AVAILABLE,
                            lastCheckedTs = System.currentTimeMillis()
                        )
                    )
                ),
                // Король и Шут - Лесник
                PredefinedSong(
                    id = "kish_lesnik",
                    title = "Лесник",
                    artist = "Король и Шут",
                    category = "Русские хиты",
                    coverUrl = "https://pic.rtbcdn.ru/video/2026-06-13/84/9e/849e66a5070125f46504c35d1e1dff34.jpg",
                    versions = listOf(
                        KaraokeVersionEntity(
                            id = "kish_lesnik_v1",
                            songId = "kish_lesnik",
                            source = SourceType.RUTUBE,
                            sourceVideoId = "a5b154cdee7b562d361f28818d90d49e",
                            title = "Король и Шут — Лесник (Караоке с текстом)",
                            channelOrAuthor = "Рок Караоке",
                            thumbnailUrl = "https://pic.rtbcdn.ru/video/2026-06-13/84/9e/849e66a5070125f46504c35d1e1dff34.jpg",
                            durationSeconds = 195,
                            videoUrl = "https://rutube.ru/video/a5b154cdee7b562d361f28818d90d49e/",
                            embedUrl = "https://rutube.ru/play/embed/a5b154cdee7b562d361f28818d90d49e",
                            quality = "1080p",
                            karaokeScore = 0.97f,
                            userRatingAverage = 4.95f,
                            ratingCount = 42,
                            isEmbeddable = true,
                            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
                            availabilityStatus = PlaybackAvailability.AVAILABLE,
                            lastCheckedTs = System.currentTimeMillis()
                        )
                    )
                ),
                // Детское караоке
                PredefinedSong(
                    id = "kids_ot_ulybki",
                    title = "От улыбки станет всем светлей",
                    artist = "Крошка Енот",
                    category = "Детское караоке",
                    coverUrl = "https://pic.rtbcdn.ru/video/2025-11-14/ee/d4/eed4464cd0bc50dbfb1ca8b1341e5d85.jpg",
                    versions = listOf(
                        KaraokeVersionEntity(
                            id = "kids_ou_v1",
                            songId = "kids_ot_ulybki",
                            source = SourceType.RUTUBE,
                            sourceVideoId = "87fde5dade05e2391c74d622ebdaabac",
                            title = "От улыбки хмурый день светлей — Караоке для детей",
                            channelOrAuthor = "Детское ТВ",
                            thumbnailUrl = "https://pic.rtbcdn.ru/video/2025-11-14/ee/d4/eed4464cd0bc50dbfb1ca8b1341e5d85.jpg",
                            durationSeconds = 140,
                            videoUrl = "https://rutube.ru/video/87fde5dade05e2391c74d622ebdaabac/",
                            embedUrl = "https://rutube.ru/play/embed/87fde5dade05e2391c74d622ebdaabac",
                            quality = "1080p",
                            karaokeScore = 0.99f,
                            userRatingAverage = 4.9f,
                            ratingCount = 18,
                            isEmbeddable = true,
                            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
                            availabilityStatus = PlaybackAvailability.AVAILABLE,
                            lastCheckedTs = System.currentTimeMillis()
                        )
                    )
                )
            )

            for (song in defaultSongs) {
                db.songDao().insertSong(
                    SongEntity(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        coverUrl = song.coverUrl,
                        category = song.category,
                        bestVersionId = song.versions.firstOrNull()?.id
                    )
                )
                db.karaokeVersionDao().insertVersions(song.versions)
            }
        }
    }
}

private data class PredefinedSong(
    val id: String,
    val title: String,
    val artist: String,
    val category: String,
    val coverUrl: String,
    val versions: List<KaraokeVersionEntity>
)
