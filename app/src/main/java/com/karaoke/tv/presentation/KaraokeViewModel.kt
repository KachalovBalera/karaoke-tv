package com.karaoke.tv.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karaoke.tv.data.local.CategoryCatalogue
import com.karaoke.tv.data.local.KaraokeDatabase
import com.karaoke.tv.data.local.entity.FavoriteEntity
import com.karaoke.tv.data.local.entity.PlaybackHistoryEntity
import com.karaoke.tv.data.remote.rutube.RutubeSource
import com.karaoke.tv.data.remote.vk.VkVideoSource
import com.karaoke.tv.data.remote.youtube.YouTubeSource
import com.karaoke.tv.data.repository.QueueRepositoryImpl
import com.karaoke.tv.data.repository.RatingRepositoryImpl
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.RatingReason
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.repository.QueueRepository
import com.karaoke.tv.domain.repository.RatingRepository
import com.karaoke.tv.domain.source.KaraokeSource
import com.karaoke.tv.domain.source.SourceManager
import com.karaoke.tv.domain.usecase.RankingPreferences
import com.karaoke.tv.domain.usecase.SearchKaraokeUseCase
import com.karaoke.tv.presentation.screens.MockPopularItem
import com.karaoke.tv.server.PhoneServerManager
import com.karaoke.tv.soundcore.AudioOutputStatus
import com.karaoke.tv.soundcore.SoundcoreManager
import com.karaoke.tv.update.AppUpdateInfo
import com.karaoke.tv.update.UpdateManager
import com.karaoke.tv.update.UpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import com.karaoke.tv.data.remote.gdrive.EncryptedKeyStoreManager
import com.karaoke.tv.data.remote.gdrive.GoogleDriveRepository
import com.karaoke.tv.data.remote.gdrive.GoogleDriveSyncManager
import com.karaoke.tv.data.remote.gdrive.GoogleServiceAccountAuthManager
import kotlinx.coroutines.withContext

data class UiState(
    val selectedNavIndex: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val sourceFilter: SourceType? = null,
    val sourcePriority: SourceType? = null,
    val searchHistory: List<String> = emptyList(),
    val sourceErrors: Map<SourceType, String> = emptyMap(),
    val selectedSongForDetails: Song? = null,
    val activePlayingItem: QueueItem? = null,
    val isPlayerOpen: Boolean = false,
    val isPartyMode: Boolean = false,
    val enableAnimations: Boolean = true,
    val performanceHistory: List<String> = emptyList(),
    val audioStatus: AudioOutputStatus = AudioOutputStatus(false, "", false, ""),
    val latencyOffsetMs: Int = 0,
    val youtubeApiKey: String = "",
    val vkAccessToken: String = "",
    // Google Drive Sync & Auth State
    val isGoogleDriveConnected: Boolean = false,
    val googleDriveAccountEmail: String = "",
    val totalDriveSongsCount: Int = 0,
    val isSyncingGoogleDrive: Boolean = false,
    val syncProgressText: String = "",
    // OTA Update State
    val updateInfo: AppUpdateInfo? = null,
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    val showUpdateDialog: Boolean = false,
    val currentAppVersion: String = "1.0.0",
    val customManifestUrl: String = ""
)

class KaraokeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "KaraokeViewModel"
        const val DEFAULT_GOOGLE_DRIVE_FOLDER_ID = "1c4A8nQFB4lF-QayBn8bELY1X__twQLXC"
    }

    private val db = KaraokeDatabase.getInstance(application)
    val queueRepository: QueueRepository = QueueRepositoryImpl(db)
    val ratingRepository: RatingRepository = RatingRepositoryImpl(db)
    val soundcoreManager = SoundcoreManager(application)
    val updateManager = UpdateManager(application)

    // Google Drive Backend Services
    val googleAuthManager = GoogleServiceAccountAuthManager()
    val googleDriveRepo = GoogleDriveRepository(googleAuthManager)
    val googleSyncManager = GoogleDriveSyncManager(db, googleDriveRepo)

    private val prefs = application.getSharedPreferences("karaoke_tv_prefs", Application.MODE_PRIVATE)

    private var youtubeApiKey: String
        get() {
            val fromPrefs = prefs.getString("yt_api_key", "") ?: ""
            return fromPrefs.ifBlank { com.karaoke.tv.BuildConfig.YOUTUBE_API_KEY }
        }
        set(v) = prefs.edit().putString("yt_api_key", v).apply()

    private var vkAccessToken: String
        get() = prefs.getString("vk_token", "") ?: ""
        set(v) = prefs.edit().putString("vk_token", v).apply()

    private var latencyOffset: Int
        get() = prefs.getInt(SoundcoreManager.PREF_KEY_LATENCY_OFFSET, 0)
        set(v) = prefs.edit().putInt(SoundcoreManager.PREF_KEY_LATENCY_OFFSET, v).apply()

    private var savedSourcePriority: SourceType?
        get() = SourceType.GOOGLE_DRIVE
        set(_) {
            prefs.edit().putString("preferred_source", SourceType.GOOGLE_DRIVE.name).apply()
        }

    // Google Drive is the sole active source. Songs are indexed locally into Room.
    private val sources: Map<SourceType, KaraokeSource> = emptyMap()

    val sourceManager = SourceManager(sources)
    val searchUseCase = SearchKaraokeUseCase(sourceManager, db)

    val phoneServerManager = PhoneServerManager(
        context = application,
        queueRepository = queueRepository,
        ratingRepository = ratingRepository,
        searchUseCase = searchUseCase,
        db = db,
        onPlayerAction = { action ->
            when (action) {
                "skip" -> skipCurrentSong()
                "pause" -> { /* Handled via UI */ }
                "play" -> resumeCurrentPlayback()
            }
        }
    )

    private val _uiState = MutableStateFlow(
        UiState(
            youtubeApiKey = youtubeApiKey,
            vkAccessToken = vkAccessToken,
            latencyOffsetMs = latencyOffset,
            sourcePriority = savedSourcePriority,
            sourceFilter = savedSourcePriority,
            audioStatus = soundcoreManager.checkAudioOutput(),
            currentAppVersion = updateManager.getCurrentVersionName(),
            customManifestUrl = updateManager.customManifestUrl
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val queueFlow = queueRepository.getQueueFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteIdsFlow = db.favoriteDao().getFavoriteSongIdsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _popularSongs = MutableStateFlow<List<MockPopularItem>>(emptyList())
    val popularSongs: StateFlow<List<MockPopularItem>> = _popularSongs.asStateFlow()

    fun refreshPopularSongs() {
        viewModelScope.launch {
            val dbSongs = withContext(Dispatchers.IO) {
                db.songDao().getAllSongs().shuffled().take(6)
            }
            if (dbSongs.isNotEmpty()) {
                val items = dbSongs.map { songEntity ->
                    val versions = db.karaokeVersionDao().getVersionsForSong(songEntity.id)
                    val bestVer = versions.maxByOrNull { it.quality } ?: versions.firstOrNull()
                    MockPopularItem(
                        title = songEntity.title,
                        artist = songEntity.artist,
                        duration = "MP4",
                        songId = songEntity.id,
                        coverUrl = songEntity.coverUrl,
                        source = SourceType.GOOGLE_DRIVE,
                        sourceVideoId = bestVer?.sourceVideoId.orEmpty()
                    )
                }
                _popularSongs.value = items
            } else {
                _popularSongs.value = CategoryCatalogue.getShuffledPopularKaraoke(6, SourceType.GOOGLE_DRIVE)
            }
        }
    }

    init {
        phoneServerManager.start()
        refreshAudioStatus()
        loadSearchHistory()
        checkForUpdates(isAutoCheck = true)
        viewModelScope.launch {
            checkAndInitializeGoogleDrive()
        }
    }

    private suspend fun checkAndInitializeGoogleDrive() {
        val hasCreds = withContext(Dispatchers.IO) {
            googleAuthManager.tryAutoLoadCredentials(getApplication())
        }
        val count = withContext(Dispatchers.IO) {
            db.songDao().getSongCount()
        }
        _uiState.value = _uiState.value.copy(
            isGoogleDriveConnected = hasCreds,
            googleDriveAccountEmail = googleAuthManager.clientEmail.orEmpty(),
            totalDriveSongsCount = count
        )
        refreshPopularSongs()
        if (hasCreds && count == 0) {
            Log.i(TAG, "Google Drive credentials found, but Room catalog is empty. Auto-starting initial sync...")
            syncGoogleDrive(DEFAULT_GOOGLE_DRIVE_FOLDER_ID)
        }
    }

    fun syncGoogleDrive(folderId: String = DEFAULT_GOOGLE_DRIVE_FOLDER_ID) {
        if (_uiState.value.isSyncingGoogleDrive) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncingGoogleDrive = true,
                syncProgressText = "Подключение к Google Диску..."
            )
            try {
                if (!googleAuthManager.hasCredentials()) {
                    withContext(Dispatchers.IO) {
                        googleAuthManager.tryAutoLoadCredentials(getApplication())
                    }
                }
                val result = googleSyncManager.syncFolder(folderId) { progress ->
                    _uiState.value = _uiState.value.copy(
                        syncProgressText = "Страница ${progress.currentPage} (${progress.totalScannedFiles} файлов)..."
                    )
                }
                val count = withContext(Dispatchers.IO) {
                    db.songDao().getSongCount()
                }
                _uiState.value = _uiState.value.copy(
                    isSyncingGoogleDrive = false,
                    syncProgressText = "Синхронизировано ${result.totalUniqueSongs} песен (${result.totalVersions} версий)",
                    totalDriveSongsCount = count,
                    isGoogleDriveConnected = true,
                    googleDriveAccountEmail = googleAuthManager.clientEmail.orEmpty()
                )
                refreshPopularSongs()
            } catch (e: Exception) {
                Log.e(TAG, "Google Drive sync error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isSyncingGoogleDrive = false,
                    syncProgressText = "Ошибка синхронизации: ${e.localizedMessage ?: "Сбой соединения"}"
                )
            }
        }
    }

    fun deleteGoogleDriveCredentials() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                EncryptedKeyStoreManager.deleteCredentials(getApplication())
                googleAuthManager.clearCredentials()
            }
            _uiState.value = _uiState.value.copy(
                isGoogleDriveConnected = false,
                googleDriveAccountEmail = "",
                syncProgressText = "Ключ удален из Keystore"
            )
        }
    }

    suspend fun getGoogleDriveAccessToken(forceRefresh: Boolean = false): String {
        return googleAuthManager.getAccessToken(forceRefresh)
    }

    override fun onCleared() {
        super.onCleared()
        phoneServerManager.stop()
    }

    fun selectNavIndex(index: Int) {
        if (index == 0) {
            refreshPopularSongs()
        }
        _uiState.value = _uiState.value.copy(selectedNavIndex = index)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun executeSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, sourceErrors = emptyMap())
            val result = withContext(Dispatchers.IO) {
                searchUseCase.execute(
                    query = query,
                    sourceFilter = SourceType.GOOGLE_DRIVE,
                    rankingPreferences = RankingPreferences(preferredSource = SourceType.GOOGLE_DRIVE)
                )
            }
            _uiState.value = _uiState.value.copy(
                searchResults = result.songs,
                sourceErrors = result.errorsBySource,
                isSearching = false
            )
            loadSearchHistory()
        }
    }

    fun setSourceFilter(source: SourceType?) {
        _uiState.value = _uiState.value.copy(sourceFilter = source)
        if (_uiState.value.searchQuery.isNotBlank()) {
            executeSearch(_uiState.value.searchQuery)
        }
    }

    fun setSourcePriority(priority: SourceType?) {
        savedSourcePriority = priority
        _uiState.value = _uiState.value.copy(sourcePriority = priority, sourceFilter = priority)
        refreshPopularSongs()
        if (_uiState.value.searchQuery.isNotBlank()) {
            executeSearch(_uiState.value.searchQuery)
        }
    }

    private fun isCataloguePlaceholder(id: String): Boolean {
        return id.isBlank() || id in listOf(
            "tgbNymZ7vqY", "r_0iWqW9n40", "k7Xm_a9rR3g", "qHk9YkKk1hQ",
            "2eG4eX5YgM8", "n0YF_gA1W_0", "w7G1Yt19x0Q", "1_gG1zQyV2k",
            "d6y6w_x8LgU", "b08381273934d4dd8867a070eb35a7da"
        )
    }

    fun openSongDetails(song: Song) {
        _uiState.value = _uiState.value.copy(selectedSongForDetails = song)
        // If song contains placeholder versions, fetch real live versions from YouTube & Rutube
        if (song.versions.any { isCataloguePlaceholder(it.sourceVideoId) }) {
            viewModelScope.launch(Dispatchers.IO) {
                val query = "${song.artist} ${song.title}".trim()
                val result = searchUseCase.execute(query = query, sourceFilter = _uiState.value.sourcePriority)
                val bestMatch = result.songs.firstOrNull { s ->
                    s.title.contains(song.title, ignoreCase = true) ||
                    song.title.contains(s.title, ignoreCase = true) ||
                    s.artist.contains(song.artist, ignoreCase = true)
                }
                if (bestMatch != null && bestMatch.versions.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        if (_uiState.value.selectedSongForDetails?.id == song.id) {
                            _uiState.value = _uiState.value.copy(
                                selectedSongForDetails = song.copy(
                                    versions = bestMatch.versions,
                                    bestVersionId = bestMatch.bestVersionId,
                                    coverUrl = bestMatch.coverUrl.ifBlank { song.coverUrl }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun closeSongDetails() {
        _uiState.value = _uiState.value.copy(selectedSongForDetails = null)
    }

    fun playSongVersion(song: Song, version: KaraokeVersion, participantName: String = "Гость") {
        viewModelScope.launch {
            var effectiveVersion = version
            if (effectiveVersion.source != SourceType.GOOGLE_DRIVE && effectiveVersion.source != SourceType.RUTUBE) {
                val gdriveVer = song.versions.firstOrNull { it.source == SourceType.GOOGLE_DRIVE }
                if (gdriveVer != null) {
                    effectiveVersion = gdriveVer
                }
            }
            if (effectiveVersion.source == SourceType.RUTUBE) {
                val rawId = effectiveVersion.sourceVideoId.removePrefix("rutube_").trim()
                if (isCataloguePlaceholder(rawId) || rawId.length < 20) {
                    val resolvedRutubeId = CategoryCatalogue.resolveRutubeId(song.artist, song.title)
                    if (!resolvedRutubeId.isNullOrBlank()) {
                        val realThumbnail = CategoryCatalogue.resolveRutubeThumbnail(song.artist, song.title)
                        effectiveVersion = effectiveVersion.copy(
                            source = SourceType.RUTUBE,
                            sourceVideoId = resolvedRutubeId,
                            thumbnailUrl = realThumbnail.ifBlank { effectiveVersion.thumbnailUrl },
                            embedUrl = "https://rutube.ru/play/embed/$resolvedRutubeId",
                            videoUrl = "https://rutube.ru/video/$resolvedRutubeId/"
                        )
                    } else {
                        // Dynamic live search on RUTUBE to resolve real video ID
                        val searchResult = withContext(Dispatchers.IO) {
                            searchUseCase.execute("${song.artist} ${song.title} караоке", sourceFilter = SourceType.RUTUBE)
                        }
                        val foundVer = searchResult.songs.firstOrNull()?.versions?.firstOrNull {
                            it.source == SourceType.RUTUBE && it.sourceVideoId.isNotBlank() && it.sourceVideoId.length >= 20
                        }
                        if (foundVer != null) {
                            effectiveVersion = foundVer
                        }
                    }
                }
            }

            // Remove any currently playing item so the new song takes over immediately
            val currentQueue = queueRepository.getQueue()
            val existingCurrent = currentQueue.firstOrNull { it.status == com.karaoke.tv.domain.model.QueueStatus.CURRENT }
            if (existingCurrent != null) {
                queueRepository.removeFromQueue(existingCurrent.id)
            }

            val item = queueRepository.addToQueue(song, effectiveVersion, participantName)
            // Ensure the newly started song is at the top of the queue
            while (true) {
                val idx = queueRepository.getQueue().indexOfFirst { it.id == item.id }
                if (idx <= 0) break
                queueRepository.moveUp(item.id)
            }
            _uiState.value = _uiState.value.copy(
                activePlayingItem = item,
                isPlayerOpen = true,
                selectedSongForDetails = null
            )
            phoneServerManager.updateCurrentPlayback(song.title, song.artist, participantName, true)

            // Record to DB history
            db.historyDao().insertHistory(
                PlaybackHistoryEntity(
                    songId = song.id,
                    versionId = effectiveVersion.id,
                    title = song.title,
                    artist = song.artist,
                    participantName = participantName,
                    source = effectiveVersion.source
                )
            )
            db.songDao().incrementTimesSung(song.id)
            db.participantDao().recordSongSung(participantName)
        }
    }

    fun addToQueue(song: Song, version: KaraokeVersion, participantName: String = "Гость") {
        viewModelScope.launch {
            var effectiveVersion = version
            if (effectiveVersion.source != SourceType.GOOGLE_DRIVE && effectiveVersion.source != SourceType.RUTUBE) {
                val gdriveVer = song.versions.firstOrNull { it.source == SourceType.GOOGLE_DRIVE }
                if (gdriveVer != null) {
                    effectiveVersion = gdriveVer
                }
            }
            if (effectiveVersion.source == SourceType.RUTUBE) {
                val rawId = effectiveVersion.sourceVideoId.removePrefix("rutube_").trim()
                if (isCataloguePlaceholder(rawId) || rawId.length < 20) {
                    val resolvedRutubeId = CategoryCatalogue.resolveRutubeId(song.artist, song.title)
                    if (!resolvedRutubeId.isNullOrBlank()) {
                        val realThumbnail = CategoryCatalogue.resolveRutubeThumbnail(song.artist, song.title)
                        effectiveVersion = effectiveVersion.copy(
                            source = SourceType.RUTUBE,
                            sourceVideoId = resolvedRutubeId,
                            thumbnailUrl = realThumbnail.ifBlank { effectiveVersion.thumbnailUrl },
                            embedUrl = "https://rutube.ru/play/embed/$resolvedRutubeId",
                            videoUrl = "https://rutube.ru/video/$resolvedRutubeId/"
                        )
                    } else {
                        // Dynamic live search on RUTUBE to resolve real video ID
                        val searchResult = withContext(Dispatchers.IO) {
                            searchUseCase.execute("${song.artist} ${song.title} караоке", sourceFilter = SourceType.RUTUBE)
                        }
                        val foundVer = searchResult.songs.firstOrNull()?.versions?.firstOrNull {
                            it.source == SourceType.RUTUBE && it.sourceVideoId.isNotBlank() && it.sourceVideoId.length >= 20
                        }
                        if (foundVer != null) {
                            effectiveVersion = foundVer
                        }
                    }
                }
            }
            queueRepository.addToQueue(song, effectiveVersion, participantName)
            _uiState.value = _uiState.value.copy(selectedSongForDetails = null)
        }
    }

    fun resumeCurrentPlayback() {
        val current = queueFlow.value.firstOrNull()
        if (current != null) {
            _uiState.value = _uiState.value.copy(
                activePlayingItem = current,
                isPlayerOpen = true
            )
        }
    }

    fun closePlayer(removeFromQueue: Boolean = false, goToSearchIfEmptyQueue: Boolean = true) {
        viewModelScope.launch {
            val current = _uiState.value.activePlayingItem
            if (removeFromQueue && current != null) {
                queueRepository.removeFromQueue(current.id)
            }
            val remaining = queueRepository.getQueue()
            _uiState.value = _uiState.value.copy(
                isPlayerOpen = false,
                activePlayingItem = if (removeFromQueue) null else current,
                selectedNavIndex = if (goToSearchIfEmptyQueue && remaining.isEmpty()) 1 else _uiState.value.selectedNavIndex
            )
            phoneServerManager.updateCurrentPlayback("", "", "", false)
        }
    }

    fun onSongEnded() {
        viewModelScope.launch {
            val current = _uiState.value.activePlayingItem
            if (current != null) {
                // Удаляем завершенный трек из очереди
                queueRepository.removeFromQueue(current.id)
            }

            val remaining = queueRepository.getQueue()
            val next = remaining.firstOrNull()

            if (next != null) {
                var effectiveNext = next
                val ver = effectiveNext.selectedVersion
                if (ver != null && ver.source == SourceType.RUTUBE) {
                    val rawId = ver.sourceVideoId.removePrefix("rutube_").trim()
                    if (isCataloguePlaceholder(rawId) || rawId.length < 20) {
                        val resolvedRutubeId = CategoryCatalogue.resolveRutubeId(effectiveNext.songArtist, effectiveNext.songTitle)
                        if (!resolvedRutubeId.isNullOrBlank()) {
                            val realThumb = CategoryCatalogue.resolveRutubeThumbnail(effectiveNext.songArtist, effectiveNext.songTitle) ?: ""
                            val resolvedVersion = ver.copy(
                                source = SourceType.RUTUBE,
                                sourceVideoId = resolvedRutubeId,
                                thumbnailUrl = realThumb.ifBlank { ver.thumbnailUrl },
                                embedUrl = "https://rutube.ru/play/embed/$resolvedRutubeId",
                                videoUrl = "https://rutube.ru/video/$resolvedRutubeId/"
                            )
                            effectiveNext = effectiveNext.copy(selectedVersion = resolvedVersion)
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    activePlayingItem = effectiveNext,
                    isPlayerOpen = true
                )
                phoneServerManager.updateCurrentPlayback(effectiveNext.songTitle, effectiveNext.songArtist, effectiveNext.participantName, true)
            } else {
                // Если очередь пуста: закрываем плеер и автоматически открываем меню поиска песен!
                _uiState.value = _uiState.value.copy(
                    isPlayerOpen = false,
                    activePlayingItem = null,
                    selectedNavIndex = 1
                )
                phoneServerManager.updateCurrentPlayback("", "", "", false)
            }
        }
    }

    fun skipCurrentSong() {
        onSongEnded()
    }

    fun switchActiveVersion(version: KaraokeVersion) {
        val current = _uiState.value.activePlayingItem ?: return
        val updated = current.copy(
            selectedVersionId = version.id,
            selectedVersion = version
        )
        _uiState.value = _uiState.value.copy(activePlayingItem = updated)
    }

    fun submitRating(versionId: String, stars: Int, reasons: List<RatingReason>) {
        viewModelScope.launch {
            ratingRepository.addRating(versionId, stars, reasons)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isFav = db.favoriteDao().isFavorite(song.id)
            if (isFav) {
                db.favoriteDao().removeFavorite(song.id)
            } else {
                db.favoriteDao().addFavorite(FavoriteEntity(song.id))
            }
        }
    }

    fun moveQueueUp(id: Long) = viewModelScope.launch { queueRepository.moveUp(id) }
    fun moveQueueDown(id: Long) = viewModelScope.launch { queueRepository.moveDown(id) }
    fun removeQueueItem(id: Long) = viewModelScope.launch { queueRepository.removeFromQueue(id) }
    fun clearQueue() = viewModelScope.launch { queueRepository.clearQueue() }

    fun adjustLatency(newOffsetMs: Int) {
        val clamped = newOffsetMs.coerceIn(-500, 500)
        latencyOffset = clamped
        _uiState.value = _uiState.value.copy(latencyOffsetMs = clamped)
    }

    fun resetLatency() {
        latencyOffset = 0
        _uiState.value = _uiState.value.copy(latencyOffsetMs = 0)
    }

    fun refreshAudioStatus() {
        _uiState.value = _uiState.value.copy(audioStatus = soundcoreManager.checkAudioOutput())
    }

    fun updateYoutubeApiKey(key: String) {
        youtubeApiKey = key
        _uiState.value = _uiState.value.copy(youtubeApiKey = key)
    }

    fun updateVkAccessToken(token: String) {
        vkAccessToken = token
        _uiState.value = _uiState.value.copy(vkAccessToken = token)
    }

    fun toggleAnimations() {
        val next = !_uiState.value.enableAnimations
        _uiState.value = _uiState.value.copy(enableAnimations = next)
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            db.historyDao().clearSearchHistory()
            _uiState.value = _uiState.value.copy(searchHistory = emptyList())
        }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            db.clearAllTables()
            // Re-populate default songs
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                activePlayingItem = null,
                isPlayerOpen = false
            )
        }
    }

    private fun loadSearchHistory() {
        viewModelScope.launch {
            db.historyDao().getSearchHistory().collect { historyEntities ->
                _uiState.value = _uiState.value.copy(
                    searchHistory = historyEntities.map { it.query }
                )
            }
        }
    }

    // ==========================================
    // OTA Update Functions (GitHub Releases)
    // ==========================================

    fun checkForUpdates(isAutoCheck: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.Checking)
            val result = updateManager.checkForUpdates()
            if (result.hasUpdate && result.updateInfo != null) {
                val cached = updateManager.getCachedApk(result.updateInfo.version)
                val status = if (cached != null) {
                    UpdateStatus.ReadyToInstall(cached, result.updateInfo)
                } else {
                    UpdateStatus.UpdateAvailable(result.updateInfo)
                }
                _uiState.value = _uiState.value.copy(
                    updateInfo = result.updateInfo,
                    updateStatus = status,
                    showUpdateDialog = true
                )
            } else {
                if (isAutoCheck) {
                    _uiState.value = _uiState.value.copy(
                        updateStatus = UpdateStatus.Idle
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        updateStatus = UpdateStatus.UpToDate(
                            result.errorMessage ?: "Установлена последняя версия Karaoke TV (${result.currentVersion})"
                        )
                    )
                }
            }
        }
    }

    fun startDownloadUpdate() {
        val info = _uiState.value.updateInfo ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                updateStatus = UpdateStatus.Downloading(0f, 0L, 0L)
            )
            val downloadResult = updateManager.downloadApk(info.apkUrl) { progress, downloaded, total ->
                _uiState.value = _uiState.value.copy(
                    updateStatus = UpdateStatus.Downloading(progress, downloaded, total)
                )
            }

            downloadResult.fold(
                onSuccess = { apkFile ->
                    _uiState.value = _uiState.value.copy(
                        updateStatus = UpdateStatus.ReadyToInstall(apkFile, info)
                    )
                    // Сразу пробуем запустить установку после завершения загрузки
                    installUpdate()
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        updateStatus = UpdateStatus.Error(
                            err.localizedMessage ?: "Ошибка скачивания обновления"
                        )
                    )
                }
            )
        }
    }

    fun installUpdate() {
        val apkFile = when (val s = _uiState.value.updateStatus) {
            is UpdateStatus.ReadyToInstall -> s.apkFile
            else -> updateManager.getCachedApk()
        }

        if (apkFile == null || !apkFile.exists()) {
            _uiState.value = _uiState.value.copy(
                updateStatus = UpdateStatus.Error("Файл обновления не найден на устройстве. Попробуйте скачать снова.")
            )
            return
        }

        val result = updateManager.installApk(apkFile)
        result.onFailure { err ->
            _uiState.value = _uiState.value.copy(
                updateStatus = UpdateStatus.Error(err.localizedMessage ?: "Не удалось запустить установщик")
            )
        }
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(showUpdateDialog = false)
    }

    fun openUpdateDialogManually() {
        if (_uiState.value.updateInfo != null) {
            _uiState.value = _uiState.value.copy(showUpdateDialog = true)
        } else {
            checkForUpdates(isAutoCheck = false)
        }
    }

    fun updateCustomManifestUrl(url: String) {
        updateManager.customManifestUrl = url
        _uiState.value = _uiState.value.copy(customManifestUrl = url)
    }
}
