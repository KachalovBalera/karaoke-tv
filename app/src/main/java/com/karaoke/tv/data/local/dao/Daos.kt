package com.karaoke.tv.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY createdAt DESC")
    fun getAllSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY artist COLLATE NOCASE ASC, title COLLATE NOCASE ASC")
    fun getAllSongsSortedAlphabeticalFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY timesSung DESC, title COLLATE NOCASE ASC")
    fun getAllSongsSortedByPopularityFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY artist COLLATE NOCASE ASC, title COLLATE NOCASE ASC")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT * FROM songs WHERE category = :category ORDER BY timesSung DESC")
    suspend fun getSongsByCategory(category: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE normalizedKey = :normalizedKey LIMIT 1")
    suspend fun getSongByNormalizedKey(normalizedKey: String): SongEntity?

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR searchKeywords LIKE '%' || :query || '%'")
    suspend fun searchSongs(query: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE searchKeywords LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY artist COLLATE NOCASE ASC, title COLLATE NOCASE ASC")
    suspend fun searchSongsNormalized(query: String): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET timesSung = timesSung + 1 WHERE id = :songId")
    suspend fun incrementTimesSung(songId: String)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: String)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()
}

@Dao
interface KaraokeVersionDao {
    @Query("SELECT * FROM karaoke_versions WHERE songId = :songId")
    suspend fun getVersionsForSong(songId: String): List<KaraokeVersionEntity>

    @Query("SELECT * FROM karaoke_versions WHERE id = :versionId LIMIT 1")
    suspend fun getVersionById(versionId: String): KaraokeVersionEntity?

    @Query("SELECT COUNT(*) FROM karaoke_versions")
    suspend fun getVersionCount(): Int

    @Query("SELECT * FROM karaoke_versions")
    suspend fun getAllVersions(): List<KaraokeVersionEntity>

    @Query("SELECT * FROM karaoke_versions WHERE source = :source")
    suspend fun getVersionsBySource(source: com.karaoke.tv.domain.model.SourceType): List<KaraokeVersionEntity>

    @Query("SELECT COUNT(*) FROM karaoke_versions WHERE source = :source")
    suspend fun getCountBySource(source: com.karaoke.tv.domain.model.SourceType): Int

    @Query("SELECT COUNT(DISTINCT songId) FROM karaoke_versions WHERE source = 'GOOGLE_DRIVE'")
    suspend fun getDriveSongsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<KaraokeVersionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: KaraokeVersionEntity)

    @Update
    suspend fun updateVersion(version: KaraokeVersionEntity)

    @Query("DELETE FROM karaoke_versions WHERE id = :versionId")
    suspend fun deleteVersion(versionId: String)

    @Query("DELETE FROM karaoke_versions WHERE songId = :songId")
    suspend fun deleteVersionsForSong(songId: String)

    @Query("DELETE FROM karaoke_versions")
    suspend fun deleteAllVersions()
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position ASC, id ASC")
    fun getQueueFlow(): Flow<List<QueueItemEntity>>

    @Query("SELECT * FROM queue_items ORDER BY position ASC, id ASC")
    suspend fun getQueue(): List<QueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: QueueItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QueueItemEntity>)

    @Update
    suspend fun updateItem(item: QueueItemEntity)

    @Delete
    suspend fun deleteItem(item: QueueItemEntity)

    @Query("DELETE FROM queue_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Query("SELECT MAX(position) FROM queue_items")
    suspend fun getMaxPosition(): Int?
}

@Dao
interface RatingDao {
    @Query("SELECT * FROM ratings WHERE versionId = :versionId ORDER BY timestamp DESC")
    suspend fun getRatingsForVersion(versionId: String): List<RatingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: RatingEntity): Long

    @Query("DELETE FROM ratings WHERE id = :id")
    suspend fun deleteRating(id: Long)

    @Query("SELECT AVG(stars) FROM ratings WHERE versionId = :versionId")
    suspend fun getAverageRating(versionId: String): Float?

    @Query("SELECT COUNT(*) FROM ratings WHERE versionId = :versionId")
    suspend fun getRatingCount(versionId: String): Int
}

@Dao
interface FavoriteDao {
    @Query("SELECT songId FROM favorites")
    fun getFavoriteSongIdsFlow(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(query: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}

@Dao
interface FailureDao {
    @Insert
    suspend fun recordFailure(failure: PlaybackFailureEntity)

    @Query("SELECT COUNT(*) FROM playback_failures WHERE versionId = :versionId AND failedAt > :sinceTimestamp")
    suspend fun getRecentFailureCount(versionId: String, sinceTimestamp: Long): Int
}

@Dao
interface ParticipantDao {
    @Query("SELECT * FROM participants ORDER BY lastActive DESC")
    fun getAllParticipantsFlow(): Flow<List<ParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity): Long

    @Query("UPDATE participants SET songsSung = songsSung + 1, lastActive = :now WHERE name = :name")
    suspend fun recordSongSung(name: String, now: Long = System.currentTimeMillis())
}
