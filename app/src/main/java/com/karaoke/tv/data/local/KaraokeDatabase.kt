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
    version = 2,
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

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN normalizedKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE songs ADD COLUMN searchKeywords TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): KaraokeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaraokeDatabase::class.java,
                    "karaoke_tv.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getInstance(context))
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    db.execSQL("DELETE FROM karaoke_versions WHERE source != 'GOOGLE_DRIVE'")
                                    db.execSQL("DELETE FROM songs WHERE id NOT IN (SELECT DISTINCT songId FROM karaoke_versions WHERE source = 'GOOGLE_DRIVE')")
                                } catch (_: Exception) {}
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: KaraokeDatabase) {
            // Catalog is dynamically synced from Google Drive or Local Media Server
        }
    }
}
