package com.karaoke.tv.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karaoke.tv.data.local.CategoryCatalogue
import com.karaoke.tv.data.local.KaraokeDatabase
import com.karaoke.tv.data.local.entity.FavoriteEntity
import com.karaoke.tv.data.local.entity.PlaybackHistoryEntity
import com.karaoke.tv.data.local.entity.toDomain
import com.karaoke.tv.data.remote.gdrive.EncryptedKeyStoreManager
import com.karaoke.tv.data.remote.gdrive.GoogleDriveRepository
import com.karaoke.tv.data.remote.gdrive.GoogleDriveSyncManager
import com.karaoke.tv.data.remote.gdrive.GoogleServiceAccountAuthManager
import com.karaoke.tv.data.repository.QueueRepositoryImpl
import com.karaoke.tv.data.repository.RatingRepositoryImpl
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.RatingReason
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
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
import kotlinx.coroutines.withContext
import android.util.Log

data class UiState(
    val selectedNavIndex: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val sourceFilter: SourceType? = SourceType.GOOGLE_DRIVE,
    val sourcePriority: SourceType = SourceType.GOOGLE_DRIVE,
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
    val currentAppVersion: String = "1.3.0",
    val customManifestUrl: String = "",
    val searchSuggestions: List<Song> = emptyList(),
    val songsByCategory: Map<String, List<Song>> = emptyMap()
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

    private var latencyOffset: Int
        get() = prefs.getInt(SoundcoreManager.PREF_KEY_LATENCY_OFFSET, 0)
        set(v) = prefs.edit().putInt(SoundcoreManager.PREF_KEY_LATENCY_OFFSET, v).apply()

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
            latencyOffsetMs = latencyOffset,
            sourcePriority = SourceType.GOOGLE_DRIVE,
            sourceFilter = SourceType.GOOGLE_DRIVE,
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
                    val matchingVer = versions.firstOrNull { it.source == SourceType.GOOGLE_DRIVE } ?: versions.firstOrNull()
                    MockPopularItem(
                        title = songEntity.title,
                        artist = songEntity.artist,
                        duration = "MP4",
                        songId = songEntity.id,
                        coverUrl = "",
                        source = SourceType.GOOGLE_DRIVE,
                        sourceVideoId = matchingVer?.sourceVideoId.orEmpty()
                    )
                }
                _popularSongs.value = items
            } else {
                _popularSongs.value = CategoryCatalogue.getShuffledPopularKaraoke(6, SourceType.GOOGLE_DRIVE)
            }
        }
    }

    fun refreshCategorySongs() {
        viewModelScope.launch {
            val map = withContext(Dispatchers.IO) {
                CategoryCatalogue.getAllCategorySongs(db)
            }
            _uiState.value = _uiState.value.copy(songsByCategory = map)
        }
    }

    init {
        phoneServerManager.start()
        refreshAudioStatus()
        loadSearchHistory()
        checkForUpdates(isAutoCheck = true)
        CategoryCatalogue.initDatabase(db)
        refreshCategorySongs()

        viewModelScope.launch {
            checkAndInitializeGoogleDrive()
        }

        // Periodic Soundcore BT state monitor
        viewModelScope.launch {
            while (true) {
                val status = soundcoreManager.checkAudioOutput()
                if (status != _uiState.value.audioStatus) {
                    _uiState.value = _uiState.value.copy(audioStatus = status)
                }
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    private suspend fun checkAndInitializeGoogleDrive() {
        val hasCreds = withContext(Dispatchers.IO) {
            googleAuthManager.tryAutoLoadCredentials(getApplication())
        }
        val count = withContext(Dispatchers.IO) {
            db.karaokeVersionDao().getDriveSongsCount()
        }
        _uiState.value = _uiState.value.copy(
            isGoogleDriveConnected = hasCreds,
            googleDriveAccountEmail = googleAuthManager.clientEmail.orEmpty(),
            totalDriveSongsCount = count
        )
        refreshPopularSongs()
        refreshCategorySongs()
        if (hasCreds && count == 0) {
            Log.i(TAG, "Google Drive credentials found, but Room catalog is empty. Auto-starting initial sync...")
            syncGoogleDrive(DEFAULT_GOOGLE_DRIVE_FOLDER_ID)
        }
    }

    // --- Search & Autocomplete ---

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), searchSuggestions = emptyList())
            return
        }

        viewModelScope.launch {
            val normalized = com.karaoke.tv.domain.engine.SongFilenameParser.normalize(query.trim())
            val matchedEntities = withContext(Dispatchers.IO) {
                db.songDao().searchSongsNormalized(normalized).ifEmpty {
                    db.songDao().searchSongs(query.trim())
                }
            }
            val mapped = matchedEntities.take(5).map { entity ->
                val versions = db.karaokeVersionDao().getVersionsForSong(entity.id).map { it.toDomain() }
                Song(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    coverUrl = "",
                    category = entity.category,
                    versions = versions,
                    bestVersionId = versions.firstOrNull()?.id
                )
            }
            _uiState.value = _uiState.value.copy(searchSuggestions = mapped)
        }
    }

    fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        _uiState.value = _uiState.value.copy(isSearching = true, searchQuery = trimmed)
        viewModelScope.launch {
            val result = searchUseCase.execute(
                query = trimmed,
                sourceFilter = SourceType.GOOGLE_DRIVE,
                rankingPreferences = RankingPreferences(preferredSource = SourceType.GOOGLE_DRIVE)
            )
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                searchResults = result.songs,
                sourceErrors = result.errorsBySource,
                searchSuggestions = emptyList()
            )
        }
    }

    fun triggerVoiceSearch(query: String) {
        _uiState.value = _uiState.value.copy(
            selectedNavIndex = 1,
            searchQuery = query
        )
        executeSearch(query)
    }

    // --- Navigation & Details ---

    fun selectNavIndex(index: Int) {
        _uiState.value = _uiState.value.copy(selectedNavIndex = index)
    }

    fun openSongDetails(song: Song) {
        _uiState.value = _uiState.value.copy(selectedSongForDetails = song)
    }

    fun closeSongDetails() {
        _uiState.value = _uiState.value.copy(selectedSongForDetails = null)
    }

    // --- Playback & Queue Management ---

    fun playSongVersion(song: Song, version: KaraokeVersion, participantName: String = "Гость") {
        playSongNow(song, version, participantName)
    }

    fun playSongNow(song: Song, version: KaraokeVersion? = null, participantName: String = "Гость") {
        viewModelScope.launch {
            var effectiveVersion = version ?: song.versions.firstOrNull { it.source == SourceType.GOOGLE_DRIVE }
                ?: song.versions.firstOrNull()
                ?: withContext(Dispatchers.IO) {
                    db.karaokeVersionDao().getVersionsForSong(song.id).firstOrNull()?.toDomain()
                }
                ?: KaraokeVersion(
                    id = "${song.id}_gdrive",
                    songId = song.id,
                    source = SourceType.GOOGLE_DRIVE,
                    sourceVideoId = song.bestVersionId?.removePrefix("gdrive_ver_")?.removePrefix("gdrive_").orEmpty(),
                    title = "${song.artist} — ${song.title}",
                    thumbnailUrl = song.coverUrl,
                    durationSeconds = 240,
                    quality = "MP4 / HD",
                    karaokeScore = 0.99f
                )

            val streamUrl = if (effectiveVersion.videoUrl.isNotBlank() && effectiveVersion.videoUrl.startsWith("http")) {
                effectiveVersion.videoUrl
            } else {
                "https://www.googleapis.com/drive/v3/files/${effectiveVersion.sourceVideoId}?alt=media"
            }
            effectiveVersion = effectiveVersion.copy(
                source = SourceType.GOOGLE_DRIVE,
                videoUrl = streamUrl,
                embedUrl = streamUrl
            )

            val item = queueRepository.playImmediately(song, effectiveVersion, participantName)

            _uiState.value = _uiState.value.copy(
                activePlayingItem = item,
                isPlayerOpen = true,
                selectedSongForDetails = null
            )
            phoneServerManager.updateCurrentPlayback(song.title, song.artist, participantName, true)

            db.historyDao().insertHistory(
                PlaybackHistoryEntity(
                    songId = song.id,
                    versionId = effectiveVersion.id,
                    title = song.title,
                    artist = song.artist,
                    participantName = participantName,
                    source = SourceType.GOOGLE_DRIVE
                )
            )
            db.songDao().incrementTimesSung(song.id)
            db.participantDao().recordSongSung(participantName)
        }
    }

    fun addToQueue(song: Song, version: KaraokeVersion? = null, participantName: String = "Гость") {
        viewModelScope.launch {
            var effectiveVersion = version ?: song.versions.firstOrNull { it.source == SourceType.GOOGLE_DRIVE }
                ?: song.versions.firstOrNull()
                ?: withContext(Dispatchers.IO) {
                    db.karaokeVersionDao().getVersionsForSong(song.id).firstOrNull()?.toDomain()
                }
                ?: KaraokeVersion(
                    id = "${song.id}_gdrive",
                    songId = song.id,
                    source = SourceType.GOOGLE_DRIVE,
                    sourceVideoId = song.bestVersionId?.removePrefix("gdrive_ver_")?.removePrefix("gdrive_").orEmpty(),
                    title = "${song.artist} — ${song.title}",
                    thumbnailUrl = song.coverUrl,
                    durationSeconds = 240,
                    quality = "MP4 / HD",
                    karaokeScore = 0.99f
                )

            val streamUrl = if (effectiveVersion.videoUrl.isNotBlank() && effectiveVersion.videoUrl.startsWith("http")) {
                effectiveVersion.videoUrl
            } else {
                "https://www.googleapis.com/drive/v3/files/${effectiveVersion.sourceVideoId}?alt=media"
            }
            effectiveVersion = effectiveVersion.copy(
                source = SourceType.GOOGLE_DRIVE,
                videoUrl = streamUrl,
                embedUrl = streamUrl
            )

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

    fun closePlayer(removeFromQueue: Boolean = false, goToSearchIfEmptyQueue: Boolean = false) {
        viewModelScope.launch {
            val current = _uiState.value.activePlayingItem
            if (removeFromQueue && current != null) {
                queueRepository.removeFromQueue(current.id)
            }
            _uiState.value = _uiState.value.copy(
                isPlayerOpen = false,
                activePlayingItem = null,
                selectedNavIndex = 0
            )
            phoneServerManager.updateCurrentPlayback("", "", "", false)
        }
    }

    fun onSongEnded() {
        viewModelScope.launch {
            val current = _uiState.value.activePlayingItem
            if (current != null) {
                runCatching {
                    db.historyDao().insertHistory(
                        PlaybackHistoryEntity(
                            songId = current.songId,
                            versionId = current.selectedVersion?.id ?: current.selectedVersionId,
                            title = current.songTitle,
                            artist = current.songArtist,
                            participantName = current.participantName,
                            source = SourceType.GOOGLE_DRIVE,
                            playedAt = System.currentTimeMillis()
                        )
                    )
                    db.songDao().incrementTimesSung(current.songId)
                }
                queueRepository.removeFromQueue(current.id)
            }

            val remainingQueue = queueRepository.getQueue()
            val nextItem = remainingQueue.firstOrNull()

            if (nextItem != null) {
                _uiState.value = _uiState.value.copy(
                    activePlayingItem = nextItem,
                    isPlayerOpen = true
                )
                phoneServerManager.updateCurrentPlayback(nextItem.songTitle, nextItem.songArtist, nextItem.participantName, true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isPlayerOpen = false,
                    activePlayingItem = null,
                    selectedNavIndex = 0
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

    fun setSourcePriority(source: SourceType?) {
        // Sole source is always Google Drive
    }

    fun setSelectedSource(source: SourceType) {
        // Sole source is always Google Drive
    }

    fun setSourceFilter(source: SourceType?) {
        // Sole source is always Google Drive
    }

    fun clearSuggestions() {
        _uiState.value = _uiState.value.copy(searchSuggestions = emptyList())
    }

    suspend fun getGoogleDriveAccessToken(forceRefresh: Boolean = false): String {
        return googleAuthManager.getAccessToken(forceRefresh)
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
                    db.karaokeVersionDao().getDriveSongsCount()
                }
                _uiState.value = _uiState.value.copy(
                    isSyncingGoogleDrive = false,
                    syncProgressText = "Синхронизировано ${result.totalUniqueSongs} песен (${result.totalVersions} версий)",
                    totalDriveSongsCount = count,
                    isGoogleDriveConnected = true,
                    googleDriveAccountEmail = googleAuthManager.clientEmail.orEmpty()
                )
                refreshPopularSongs()
                refreshCategorySongs()
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
                syncProgressText = "Ключ удален из Keystore",
                totalDriveSongsCount = 0
            )
        }
    }

    // --- Settings & Diagnostics ---

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

    private fun loadSearchHistory() {
        viewModelScope.launch {
            db.historyDao().getSearchHistory().collect { historyEntities ->
                _uiState.value = _uiState.value.copy(
                    searchHistory = historyEntities.map { it.query }
                )
            }
        }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            db.clearAllTables()
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                activePlayingItem = null,
                isPlayerOpen = false,
                totalDriveSongsCount = 0
            )
            if (googleAuthManager.hasCredentials()) {
                syncGoogleDrive(DEFAULT_GOOGLE_DRIVE_FOLDER_ID)
            }
        }
    }

    fun adjustLatency(deltaMs: Int) {
        val next = (_uiState.value.latencyOffsetMs + deltaMs).coerceIn(-500, 500)
        latencyOffset = next
        _uiState.value = _uiState.value.copy(latencyOffsetMs = next)
    }

    fun resetLatency() {
        latencyOffset = 0
        _uiState.value = _uiState.value.copy(latencyOffsetMs = 0)
    }

    fun refreshAudioStatus() {
        _uiState.value = _uiState.value.copy(audioStatus = soundcoreManager.checkAudioOutput())
    }

    // --- OTA Update Operations ---

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
                updateStatus = UpdateStatus.Error("Файл обновления не найден на устройстве")
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
