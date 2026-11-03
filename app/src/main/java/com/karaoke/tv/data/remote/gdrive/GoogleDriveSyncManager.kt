package com.karaoke.tv.data.remote.gdrive

import android.util.Log
import androidx.room.withTransaction
import com.karaoke.tv.data.local.KaraokeDatabase
import com.karaoke.tv.data.local.entity.KaraokeVersionEntity
import com.karaoke.tv.data.local.entity.SongEntity
import com.karaoke.tv.domain.engine.SongFilenameParser
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Manages scanning, parsing, deduplication, and persistent synchronization of Google Drive songs
 * into the local Room database.
 */
class GoogleDriveSyncManager(
    private val database: KaraokeDatabase,
    private val repository: GoogleDriveRepository
) {

    companion object {
        private const val TAG = "GDriveSyncManager"
    }

    data class SyncProgress(
        val currentPage: Int,
        val totalScannedFiles: Int,
        val statusMessage: String
    )

    data class SyncResult(
        val totalDriveFiles: Int,
        val totalUniqueSongs: Int,
        val totalVersions: Int,
        val duplicateVersionsMerged: Int,
        val durationMs: Long
    )

    /**
     * Synchronizes all video files from a Google Drive folder into Room.
     * Fully idempotent: re-syncing updates records without duplicates and preserves user timesSung.
     */
    suspend fun syncFolder(
        folderId: String,
        onProgress: ((SyncProgress) -> Unit)? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "Starting sync for folder: $folderId")

        // 1. Fetch all files via pagination
        val allFiles = mutableListOf<GoogleDriveRepository.DriveFileItem>()
        var pageToken: String? = null
        var pageNum = 0

        do {
            pageNum++
            onProgress?.invoke(
                SyncProgress(
                    currentPage = pageNum,
                    totalScannedFiles = allFiles.size,
                    statusMessage = "Загрузка страницы #$pageNum..."
                )
            )

            val page = repository.listFiles(
                folderId = folderId,
                pageSize = 1000,
                pageToken = pageToken,
                onlyVideos = true
            )

            allFiles.addAll(page.files)
            Log.d(TAG, "Page #$pageNum fetched: ${page.files.size} items (total: ${allFiles.size})")
            pageToken = page.nextPageToken
        } while (!pageToken.isNullOrBlank())

        Log.i(TAG, "Fetched ${allFiles.size} video files from Drive. Processing and deduplicating...")

        // 2. Parse filenames and group by normalized key
        val fileGroupByNormalizedKey = LinkedHashMap<String, MutableList<GoogleDriveRepository.DriveFileItem>>()
        val parsedInfoCache = HashMap<String, com.karaoke.tv.domain.engine.ParsedSongInfo>()

        for (file in allFiles) {
            val parsed = SongFilenameParser.parse(file.name)
            parsedInfoCache[file.id] = parsed
            fileGroupByNormalizedKey.getOrPut(parsed.normalizedKey) { mutableListOf() }.add(file)
        }

        val totalUniqueSongs = fileGroupByNormalizedKey.size
        val duplicateMergedCount = allFiles.size - totalUniqueSongs
        Log.i(TAG, "Detected $totalUniqueSongs unique songs (found $duplicateMergedCount duplicate versions to merge)")

        // 3. Load existing song entities to preserve user stats (timesSung, etc.)
        val existingSongsMap = database.songDao().getAllSongs().associateBy { it.id }

        val songsToInsert = ArrayList<SongEntity>(totalUniqueSongs)
        val versionsToInsert = ArrayList<KaraokeVersionEntity>(allFiles.size)

        for ((normalizedKey, files) in fileGroupByNormalizedKey) {
            val songId = generateDeterministicSongId(normalizedKey)

            val versionsForThisSong = files.map { file ->
                val parsed = parsedInfoCache[file.id] ?: SongFilenameParser.parse(file.name)
                val versionId = "gdrive_ver_${file.id}"
                val mediaUrl = repository.getMediaStreamUrl(file.id)

                KaraokeVersionEntity(
                    id = versionId,
                    songId = songId,
                    source = SourceType.GOOGLE_DRIVE,
                    sourceVideoId = file.id,
                    title = parsed.title,
                    channelOrAuthor = parsed.artist,
                    thumbnailUrl = file.thumbnailLink.orEmpty(),
                    durationSeconds = 0,
                    videoUrl = mediaUrl,
                    embedUrl = mediaUrl,
                    quality = parsed.quality,
                    karaokeScore = 0.95f,
                    userRatingAverage = 5.0f,
                    ratingCount = 1,
                    isEmbeddable = true,
                    verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
                    availabilityStatus = PlaybackAvailability.AVAILABLE,
                    lastCheckedTs = System.currentTimeMillis()
                )
            }

            // Determine best version by quality score: 4K > 1080p > 720p > others
            val bestVersion = versionsForThisSong.maxByOrNull { versionQualityWeight(it.quality) }
                ?: versionsForThisSong.first()

            val existingSong = existingSongsMap[songId]

            val parsedPrimary = parsedInfoCache[files.first().id]
                ?: SongFilenameParser.parse(files.first().name)

            val searchKeywords = buildString {
                append(SongFilenameParser.normalizeText(parsedPrimary.artist))
                append(" ")
                append(SongFilenameParser.normalizeText(parsedPrimary.title))
            }

            val songEntity = SongEntity(
                id = songId,
                title = bestVersion.title,
                artist = bestVersion.channelOrAuthor,
                coverUrl = bestVersion.thumbnailUrl,
                category = "Google Drive",
                timesSung = existingSong?.timesSung ?: 0,
                bestVersionId = bestVersion.id,
                normalizedKey = normalizedKey,
                searchKeywords = searchKeywords,
                createdAt = existingSong?.createdAt ?: System.currentTimeMillis()
            )

            songsToInsert.add(songEntity)
            versionsToInsert.addAll(versionsForThisSong)
        }

        // 4. Batch persist to Room DB in chunked transactions to avoid SQLite bind variable limits
        val chunkSize = 500
        for (i in 0 until songsToInsert.size step chunkSize) {
            val chunk = songsToInsert.subList(i, minOf(i + chunkSize, songsToInsert.size))
            database.songDao().insertSongs(chunk)
        }

        for (i in 0 until versionsToInsert.size step chunkSize) {
            val chunk = versionsToInsert.subList(i, minOf(i + chunkSize, versionsToInsert.size))
            database.karaokeVersionDao().insertVersions(chunk)
        }

        val duration = System.currentTimeMillis() - startTime
        Log.i(TAG, "Sync complete in ${duration}ms: ${songsToInsert.size} songs, ${versionsToInsert.size} versions saved in Room")

        SyncResult(
            totalDriveFiles = allFiles.size,
            totalUniqueSongs = songsToInsert.size,
            totalVersions = versionsToInsert.size,
            duplicateVersionsMerged = duplicateMergedCount,
            durationMs = duration
        )
    }

    /**
     * Assigns quality weights to pick the best version when duplicates exist.
     */
    private fun versionQualityWeight(quality: String): Int {
        val q = quality.lowercase()
        return when {
            q.contains("4k") || q.contains("2160") || q.contains("uhd") -> 400
            q.contains("1080") || q.contains("fhd") -> 300
            q.contains("720") || q.contains("hd") -> 200
            else -> 100
        }
    }

    /**
     * Generates a stable deterministic ID for a song based on its normalized key.
     */
    private fun generateDeterministicSongId(normalizedKey: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(normalizedKey.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "gdrive_song_$hex"
    }
}
