package com.karaoke.tv.data.remote.gdrive

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.karaoke.tv.domain.engine.SongFilenameParser
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Repository for interacting with Google Drive API v3.
 * Supports file listing, pagination, metadata retrieval, and streaming endpoint URLs.
 */
class GoogleDriveRepository(
    private val authManager: GoogleServiceAccountAuthManager,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    companion object {
        private const val TAG = "GoogleDriveRepo"
        private const val BASE_DRIVE_API = "https://www.googleapis.com/drive/v3"
    }

    data class DriveFileItem(
        @SerializedName("id") val id: String,
        @SerializedName("name") val name: String,
        @SerializedName("size") val size: Long = 0L,
        @SerializedName("mimeType") val mimeType: String = "",
        @SerializedName("thumbnailLink") val thumbnailLink: String? = null,
        @SerializedName("webViewLink") val webViewLink: String? = null
    )

    data class FileListResponse(
        @SerializedName("nextPageToken") val nextPageToken: String? = null,
        @SerializedName("files") val files: List<DriveFileItem> = emptyList()
    )

    data class AboutResponse(
        @SerializedName("user") val user: DriveUser? = null
    )

    data class DriveUser(
        @SerializedName("displayName") val displayName: String = "",
        @SerializedName("emailAddress") val emailAddress: String = ""
    )

    /**
     * Gets user/account info for the current credentials.
     */
    suspend fun getAboutUser(): DriveUser = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken()
        val url = "$BASE_DRIVE_API/about?fields=user(displayName,emailAddress)"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()

        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw IOException("Failed to get Drive about info: HTTP ${response.code} - $body")
        }

        val about = gson.fromJson(body, AboutResponse::class.java)
        about.user ?: DriveUser(displayName = "Service Account", emailAddress = authManager.clientEmail.orEmpty())
    }

    /**
     * Lists files from Google Drive with optional folder filtering and pagination.
     */
    suspend fun listFiles(
        folderId: String? = null,
        pageSize: Int = 100,
        pageToken: String? = null,
        onlyVideos: Boolean = true
    ): FileListResponse = withContext(Dispatchers.IO) {
        var attempt = 0
        val maxAttempts = 3
        var backoffMs = 1000L

        while (attempt < maxAttempts) {
            attempt++
            val token = authManager.getAccessToken(forceRefresh = (attempt > 1))

            val queryParts = mutableListOf<String>()
            queryParts.add("trashed = false")

            if (!folderId.isNullOrBlank()) {
                queryParts.add("'$folderId' in parents")
            }

            if (onlyVideos) {
                queryParts.add("(mimeType contains 'video/' or name contains '.mp4' or name contains '.mkv' or name contains '.avi')")
            }

            val query = queryParts.joinToString(" and ")
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val fields = URLEncoder.encode("nextPageToken,files(id,name,size,mimeType,thumbnailLink,webViewLink)", "UTF-8")

            val urlBuilder = StringBuilder("$BASE_DRIVE_API/files?q=$encodedQuery&pageSize=$pageSize&fields=$fields")
            if (!pageToken.isNullOrBlank()) {
                urlBuilder.append("&pageToken=").append(URLEncoder.encode(pageToken, "UTF-8"))
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("Authorization", "Bearer $token")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                return@withContext gson.fromJson(body, FileListResponse::class.java)
            }

            val code = response.code
            Log.w(TAG, "Drive listFiles attempt $attempt failed [HTTP $code]: $body")

            if ((code == 401 || code == 429 || code >= 500) && attempt < maxAttempts) {
                kotlinx.coroutines.delay(backoffMs)
                backoffMs *= 2
                continue
            }

            throw IOException("Failed to list files from Drive: HTTP $code - $body")
        }

        throw IOException("Failed to list files from Drive after $maxAttempts attempts")
    }

    /**
     * Scans all video files in the specified folder (iterating through all pages).
     */
    suspend fun scanAllSongs(
        folderId: String? = null,
        onProgress: ((count: Int) -> Unit)? = null
    ): List<Song> = withContext(Dispatchers.IO) {
        val allDriveFiles = mutableListOf<DriveFileItem>()
        var pageToken: String? = null

        do {
            val response = listFiles(
                folderId = folderId,
                pageSize = 1000,
                pageToken = pageToken,
                onlyVideos = true
            )
            allDriveFiles.addAll(response.files)
            onProgress?.invoke(allDriveFiles.size)
            pageToken = response.nextPageToken
        } while (!pageToken.isNullOrBlank())

        Log.i(TAG, "Completed scanning Drive folder: found ${allDriveFiles.size} video files")

        // Map Drive files to domain Song objects using SongFilenameParser
        allDriveFiles.map { file ->
            mapDriveFileToSong(file)
        }
    }

    /**
     * Converts a DriveFileItem into a Song model using SongFilenameParser.
     */
    fun mapDriveFileToSong(file: DriveFileItem): Song {
        val parsed = SongFilenameParser.parse(file.name)
        val songId = "gdrive_${file.id}"
        val versionId = "gdrive_ver_${file.id}"
        val streamingUrl = getMediaStreamUrl(file.id)

        val version = KaraokeVersion(
            id = versionId,
            songId = songId,
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = file.id,
            title = parsed.title,
            channelOrAuthor = parsed.artist,
            thumbnailUrl = file.thumbnailLink.orEmpty(),
            durationSeconds = 0,
            videoUrl = streamingUrl,
            embedUrl = streamingUrl,
            quality = parsed.quality,
            karaokeScore = 0.95f,
            userRatingAverage = 5f,
            ratingCount = 1,
            isEmbeddable = true,
            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
            availabilityStatus = PlaybackAvailability.AVAILABLE
        )

        return Song(
            id = songId,
            title = parsed.title,
            artist = parsed.artist,
            coverUrl = file.thumbnailLink.orEmpty(),
            category = "Google Drive",
            versions = listOf(version),
            bestVersionId = versionId
        )
    }

    /**
     * Returns the direct media streaming URL for a Google Drive file ID.
     */
    fun getMediaStreamUrl(fileId: String): String {
        return "$BASE_DRIVE_API/files/$fileId?alt=media"
    }
}
