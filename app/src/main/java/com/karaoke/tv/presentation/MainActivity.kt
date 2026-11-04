package com.karaoke.tv.presentation

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoke.tv.R
import com.karaoke.tv.data.local.CategoryCatalogue
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.presentation.components.UpdateDialog
import com.karaoke.tv.presentation.screens.CategoryScreen
import com.karaoke.tv.presentation.screens.HomeScreen
import com.karaoke.tv.presentation.screens.PartyModeScreen
import com.karaoke.tv.presentation.screens.PhoneConnectScreen
import com.karaoke.tv.presentation.screens.PlayerScreen
import com.karaoke.tv.presentation.screens.QueueScreen
import com.karaoke.tv.presentation.screens.SearchScreen
import com.karaoke.tv.presentation.screens.SettingsScreen
import com.karaoke.tv.presentation.screens.SongDetailScreen
import com.karaoke.tv.presentation.screens.SoundcoreScreen
import com.karaoke.tv.presentation.theme.BackgroundDark
import com.karaoke.tv.presentation.theme.KaraokeTVTheme
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.PrimaryPurpleGlow
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SidebarBackground
import com.karaoke.tv.presentation.theme.SidebarBorder
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: KaraokeViewModel by viewModels()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningVoice by mutableStateOf(false)
    private var voicePromptText by mutableStateOf("Слушаю... Назовите песню или исполнителя")

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.triggerVoiceSearch(spoken)
            }
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startInAppVoiceSearch()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.phoneServerManager.onVoiceSearchQuery = { spokenQuery ->
            viewModel.triggerVoiceSearch(spokenQuery)
        }

        setContent {
            KaraokeTVTheme {
                val uiState by viewModel.uiState.collectAsState()
                val queue by viewModel.queueFlow.collectAsState()
                val favoriteIds by viewModel.favoriteIdsFlow.collectAsState()
                val popularSongs by viewModel.popularSongs.collectAsState()
                val connectedPhonesCount by viewModel.phoneServerManager.connectedCount.collectAsState()
                val connectedClients by viewModel.phoneServerManager.connectedClients.collectAsState()
                var currentCategoryTitle by remember { mutableStateOf("Русский поп") }

                if (!uiState.isPlayerOpen && uiState.selectedSongForDetails == null && uiState.selectedNavIndex != 0) {
                    BackHandler {
                        viewModel.selectNavIndex(0)
                    }
                }

                val contentFocusRequester = remember { FocusRequester() }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Full Concert Stage Background
                    Image(
                        painter = painterResource(id = R.drawable.concert_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Contrast and legibility wash for Android TV
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xEE080B18),
                                        Color(0xBB0A0E22),
                                        Color(0x66070914)
                                    )
                                )
                            )
                    )

                    // Main TV Layout: Left Sidebar + Main Screen Content
                    Row(modifier = Modifier.fillMaxSize()) {
                        TvSidebarNavigation(
                            selectedIndex = uiState.selectedNavIndex,
                            queueCount = queue.size,
                            connectedPhonesCount = connectedPhonesCount,
                            onSelectIndex = { viewModel.selectNavIndex(it) },
                            onExitApp = {
                                viewModel.closePlayer(removeFromQueue = false)
                                finishAffinity()
                            },
                            contentFocusRequester = contentFocusRequester
                        )

                        // Main Content Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color.Transparent)
                                .focusGroup()
                                .focusRequester(contentFocusRequester)
                        ) {
                            when (uiState.selectedNavIndex) {
                                // 0: Главная
                                0 -> HomeScreen(
                                    songsByCategory = uiState.songsByCategory.ifEmpty {
                                        mapOf(
                                            "Русский поп" to CategoryCatalogue.getSongsForCategory("Русский поп", uiState.sourcePriority),
                                            "Рок" to CategoryCatalogue.getSongsForCategory("Рок", uiState.sourcePriority),
                                            "Дискотека 90-х" to CategoryCatalogue.getSongsForCategory("Дискотека 90-х", uiState.sourcePriority),
                                            "Хиты 2000-х" to CategoryCatalogue.getSongsForCategory("Хиты 2000-х", uiState.sourcePriority),
                                            "Детские песни" to CategoryCatalogue.getSongsForCategory("Детские песни", uiState.sourcePriority)
                                        )
                                    },
                                    currentQueue = queue,
                                    popularSongs = popularSongs,
                                    selectedSource = uiState.sourcePriority,
                                    onSelectSource = { viewModel.setSourcePriority(it) },
                                    onSongSelected = { viewModel.openSongDetails(it) },
                                    onOpenSearch = { viewModel.selectNavIndex(1) },
                                    onVoiceSearch = { launchVoiceSearch() },
                                    onOpenSoundcore = { viewModel.selectNavIndex(7) },
                                    onSelectCategory = { cat ->
                                        currentCategoryTitle = cat
                                        viewModel.selectNavIndex(9)
                                    }
                                )

                                // 1: Поиск
                                1 -> SearchScreen(
                                    searchQuery = uiState.searchQuery,
                                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                                    onSearch = { viewModel.executeSearch(it) },
                                    searchResults = uiState.searchResults,
                                    searchSuggestions = uiState.searchSuggestions,
                                    onClearSuggestions = { viewModel.clearSuggestions() },
                                    isLoading = uiState.isSearching,
                                    sourceFilter = uiState.sourceFilter,
                                    onFilterChange = { viewModel.setSourceFilter(it) },
                                    searchHistory = uiState.searchHistory,
                                    sourceErrors = uiState.sourceErrors,
                                    onSongSelected = { viewModel.openSongDetails(it) },
                                    onVoiceInputRequested = { launchVoiceSearch() }
                                )

                                // 2: Очередь
                                2 -> QueueScreen(
                                    queue = queue,
                                    onPlayCurrent = { viewModel.resumeCurrentPlayback() },
                                    onMoveUp = { viewModel.moveQueueUp(it) },
                                    onMoveDown = { viewModel.moveQueueDown(it) },
                                    onRemove = { viewModel.removeQueueItem(it) },
                                    onSkip = { viewModel.skipCurrentSong() },
                                    onRepeat = { /* repeat */ },
                                    onClearQueue = { viewModel.clearQueue() }
                                )

                                // 3: Избранное
                                3 -> FavoritesScreen(
                                    favoriteIds = favoriteIds,
                                    onSongSelected = { viewModel.openSongDetails(it) }
                                )

                                // 4: История
                                4 -> HistoryScreen(
                                    onSongSelected = { viewModel.openSongDetails(it) }
                                )

                                // 5: Рейтинг / Режим вечеринки
                                5 -> PartyModeScreen(
                                    currentQueue = queue,
                                    performanceHistory = uiState.performanceHistory,
                                    enableAnimations = uiState.enableAnimations,
                                    onToggleAnimations = { viewModel.toggleAnimations() },
                                    onStartSinging = { viewModel.resumeCurrentPlayback() },
                                    onConnectPhones = { viewModel.selectNavIndex(8) }
                                )

                                // 6: Настройки
                                6 -> SettingsScreen(
                                    isGoogleDriveConnected = uiState.isGoogleDriveConnected,
                                    googleDriveAccountEmail = uiState.googleDriveAccountEmail,
                                    totalDriveSongsCount = uiState.totalDriveSongsCount,
                                    isSyncingGoogleDrive = uiState.isSyncingGoogleDrive,
                                    syncProgressText = uiState.syncProgressText,
                                    onSyncGoogleDrive = { viewModel.syncGoogleDrive() },
                                    onDeleteGoogleDriveKey = { viewModel.deleteGoogleDriveCredentials() },
                                    enableAnimations = uiState.enableAnimations,
                                    onToggleAnimations = { viewModel.toggleAnimations() },
                                    onClearSearchHistory = { viewModel.clearSearchHistory() },
                                    onResetDatabase = { viewModel.resetDatabase() },
                                    currentAppVersion = uiState.currentAppVersion,
                                    updateStatus = uiState.updateStatus,
                                    updateInfo = uiState.updateInfo,
                                    customManifestUrl = uiState.customManifestUrl,
                                    onCustomManifestUrlChange = { viewModel.updateCustomManifestUrl(it) },
                                    onCheckForUpdates = { viewModel.checkForUpdates(isAutoCheck = false) },
                                    onOpenUpdateDialog = { viewModel.openUpdateDialogManually() }
                                )

                                // 7: Soundcore Rave 3S Setup & Calibration Screen
                                7 -> SoundcoreScreen(
                                    audioStatus = uiState.audioStatus,
                                    latencyOffsetMs = uiState.latencyOffsetMs,
                                    onAdjustLatency = { viewModel.adjustLatency(it) },
                                    onResetLatency = { viewModel.resetLatency() },
                                    onPlayTestTrack = { /* play sync beep track */ },
                                    onRefreshStatus = { viewModel.refreshAudioStatus() }
                                )

                                // 8: Подключить телефон (QR-код)
                                8 -> PhoneConnectScreen(
                                    qrCodeUrl = viewModel.phoneServerManager.getQrCodeUrl(),
                                    localIpAddress = viewModel.phoneServerManager.getLocalIpAddress(),
                                    port = viewModel.phoneServerManager.port,
                                    adminPin = viewModel.phoneServerManager.adminPin,
                                    connectedPhonesCount = connectedPhonesCount,
                                    connectedClients = connectedClients,
                                    onRefreshQrCode = { viewModel.phoneServerManager.refreshSession() },
                                    onDisconnectAll = { viewModel.phoneServerManager.disconnectAll() },
                                    onDisconnectClient = { viewModel.phoneServerManager.disconnectDevice(it) },
                                    onBack = { viewModel.selectNavIndex(0) }
                                )

                                // 9: Категория (Filtered Playlist)
                                9 -> CategoryScreen(
                                    title = currentCategoryTitle,
                                    category = currentCategoryTitle,
                                    songs = uiState.songsByCategory[currentCategoryTitle].orEmpty(),
                                    preferredSource = uiState.sourcePriority,
                                    onSongSelected = { viewModel.openSongDetails(it) },
                                    onBack = { viewModel.selectNavIndex(0) }
                                )
                            }
                        }
                    }

                    // Song Details Dialog Overlay
                    uiState.selectedSongForDetails?.let { song ->
                        SongDetailScreen(
                            song = song,
                            onPlayVersion = { version, participant ->
                                viewModel.playSongVersion(song, version, participant)
                            },
                            onAddToQueue = { version, participant ->
                                viewModel.addToQueue(song, version, participant)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            isFavorite = favoriteIds.contains(song.id),
                            onBack = { viewModel.closeSongDetails() }
                        )
                    }

                    // Fullscreen Karaoke Player Overlay (keyed by unique queue item ID to guarantee fresh lifecycle & WebView on each song)
                    if (uiState.isPlayerOpen && uiState.activePlayingItem != null) {
                        key(uiState.activePlayingItem!!.id) {
                            PlayerScreen(
                                currentQueueItem = uiState.activePlayingItem!!,
                                queueList = queue,
                                alternativeVersions = emptyList(),
                                getAccessToken = { forceRefresh -> viewModel.getGoogleDriveAccessToken(forceRefresh) },
                                onSongEnded = { viewModel.onSongEnded() },
                                onSkipNext = { viewModel.skipCurrentSong() },
                                onSwitchVersion = { viewModel.switchActiveVersion(it) },
                                onRateVersion = { id, stars, reasons -> viewModel.submitRating(id, stars, reasons) },
                                onClosePlayer = { removeFromQueue -> viewModel.closePlayer(removeFromQueue = removeFromQueue) }
                            )
                        }
                    }

                    // OTA Update Dialog Overlay (Android TV)
                    if (uiState.showUpdateDialog && uiState.updateInfo != null) {
                        UpdateDialog(
                            updateInfo = uiState.updateInfo!!,
                            currentVersion = uiState.currentAppVersion,
                            updateStatus = uiState.updateStatus,
                            onStartDownload = { viewModel.startDownloadUpdate() },
                            onInstallApk = { viewModel.installUpdate() },
                            onDismiss = { viewModel.dismissUpdateDialog() }
                        )
                    }

                    // In-app Speech Recognition Dialog Overlay
                    if (isListeningVoice) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xCC000000))
                                .clickable {
                                    speechRecognizer?.stopListening()
                                    isListeningVoice = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(460.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SurfaceCard)
                                    .border(2.dp, PrimaryPurpleGlow, RoundedCornerShape(20.dp))
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = SecondaryCyan,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Голосовой поиск",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = voicePromptText,
                                    color = SecondaryCyan,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Назовите песню или исполнителя в микрофон пульта",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("KaraokeDiagnostic", "LIFECYCLE | onNewIntent | Action=${intent.action}")
    }

    override fun onStop() {
        super.onStop()
        Log.d("KaraokeDiagnostic", "LIFECYCLE | onStop | App moved to background")
        // PlayerScreen's LifecycleEventObserver handles pausing audio without destroying user state
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d("KaraokeDiagnostic", "LIFECYCLE | onDestroy | Activity destroying")
        if (isFinishing) {
            viewModel.closePlayer(removeFromQueue = false, goToSearchIfEmptyQueue = false)
        }
    }

    private fun launchVoiceSearch() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startInAppVoiceSearch()
        } else {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startInAppVoiceSearch() {
        val isAvail = SpeechRecognizer.isRecognitionAvailable(this)
        if (!isAvail) {
            Log.w("MainActivity", "In-app SpeechRecognizer not available, falling back to intent")
            startVoiceSearchIntent()
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningVoice = true
                        voicePromptText = "Слушаю... Говорите"
                    }
                    override fun onBeginningOfSpeech() {
                        voicePromptText = "Распознавание речи..."
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        voicePromptText = "Поиск песни..."
                    }
                    override fun onError(error: Int) {
                        isListeningVoice = false
                        Log.w("VoiceSearch", "SpeechRecognizer error: $error, trying intent fallback")
                        startVoiceSearchIntent()
                    }
                    override fun onResults(results: Bundle?) {
                        isListeningVoice = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()
                        if (!spoken.isNullOrBlank()) {
                            viewModel.triggerVoiceSearch(spoken)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ru-RU", "en-US"))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "SpeechRecognizer failed: ${e.message}", e)
            startVoiceSearchIntent()
        }
    }

    private fun startVoiceSearchIntent() {
        runCatching {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ru-RU", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Назовите песню или исполнителя")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            voiceLauncher.launch(intent)
        }.onFailure { e ->
            Log.e("MainActivity", "Failed to launch voice search: ${e.message}", e)
        }
    }

    private fun getSampleCategorySongs(category: String): List<Song> {
        return CategoryCatalogue.getSongsForCategory(category)
    }
}

// -------------------------------------------------------------
// Component: Left Sidebar Navigation (Matching Screenshot)
// -------------------------------------------------------------

data class NavItem(val label: String, val icon: ImageVector, val targetIndex: Int)

@Composable
fun TvSidebarNavigation(
    selectedIndex: Int,
    queueCount: Int,
    connectedPhonesCount: Int = 0,
    onSelectIndex: (Int) -> Unit,
    onExitApp: () -> Unit,
    contentFocusRequester: FocusRequester
) {
    val navItems = listOf(
        NavItem("Главная", Icons.Default.Home, 0),
        NavItem("Поиск", Icons.Default.Search, 1),
        NavItem("Очередь", Icons.AutoMirrored.Filled.QueueMusic, 2),
        NavItem("Избранное", Icons.Default.FavoriteBorder, 3),
        NavItem("История", Icons.Default.History, 4),
        NavItem("Рейтинг", Icons.Default.EmojiEvents, 5),
        NavItem("Телефон", Icons.Default.Smartphone, 8),
        NavItem("Настройки", Icons.Default.Settings, 6),
        NavItem("Выход", Icons.AutoMirrored.Filled.ExitToApp, -1)
    )

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xDD070A18))
            .border(width = 1.dp, color = Color(0x3338BDF8))
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        // App Logo Header with neon TV & microphone
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Karaoke TV",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Items
        navItems.forEach { item ->
            val isExit = item.targetIndex == -1
            val isSelected = !isExit && selectedIndex == item.targetIndex
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            isExit && isFocused -> Brush.horizontalGradient(
                                colors = listOf(Color(0xFFDC2626), Color(0xFF991B1B))
                            )
                            isSelected && isFocused -> Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                            )
                            isFocused -> Brush.horizontalGradient(
                                colors = listOf(Color(0x5538BDF8), Color(0x402563EB))
                            )
                            isSelected -> Brush.horizontalGradient(
                                colors = listOf(Color(0xFF1D4ED8), Color(0xFF6D28D9))
                            )
                            else -> Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent)
                            )
                        }
                    )
                    .border(
                        width = when {
                            isFocused -> 2.dp
                            isSelected -> 1.5.dp
                            else -> 0.dp
                        },
                        color = when {
                            isExit && isFocused -> Color(0xFFEF4444)
                            isFocused -> SecondaryCyan
                            isSelected -> Color(0xFF60A5FA)
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .focusProperties {
                        right = contentFocusRequester
                    }
                    .focusable(interactionSource = interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        if (isExit) {
                            onExitApp()
                        } else if (isSelected) {
                            contentFocusRequester.requestFocus()
                        } else {
                            onSelectIndex(item.targetIndex)
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = when {
                        isExit && isFocused -> Color.White
                        isExit -> Color(0xFFF87171)
                        isFocused -> Color.White
                        isSelected -> Color.White
                        else -> Color(0xFF94A3B8)
                    },
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = item.label,
                    color = when {
                        isExit && isFocused -> Color.White
                        isExit -> Color(0xFFFCA5A5)
                        isFocused -> Color.White
                        isSelected -> Color.White
                        else -> Color(0xFFCBD5E1)
                    },
                    fontSize = 20.sp,
                    fontWeight = if (isSelected || isFocused || isExit) FontWeight.Bold else FontWeight.SemiBold
                )

                // Circular Counter Badge for "Очередь" (White circle with bold black text)
                if (item.targetIndex == 2) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = queueCount.toString(),
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Glowing indicator for "Телефон"
                if (item.targetIndex == 8) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(if (connectedPhonesCount > 0) 26.dp else 12.dp)
                            .clip(CircleShape)
                            .background(SecondaryCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        if (connectedPhonesCount > 0) {
                            Text(
                                text = connectedPhonesCount.toString(),
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// -------------------------------------------------------------
// Sub-screens for Favorites, History
// -------------------------------------------------------------

@Composable
fun FavoritesScreen(
    favoriteIds: List<String>,
    onSongSelected: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(32.dp)
    ) {
        Text(
            text = "ИЗБРАННОЕ",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (favoriteIds.isEmpty()) {
            Text(
                text = "В избранном пока нет песен. Откройте любую песню и нажмите «В избранное», чтобы добавить её в этот список.",
                color = TextMuted,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun HistoryScreen(
    onSongSelected: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(32.dp)
    ) {
        Text(
            text = "ИСТОРИЯ ИСПОЛНЕНИЙ",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Список песен, которые вы и ваши гости пели в этой караоке-системе ранее.",
            color = Color(0xFFCBD5E1),
            fontSize = 14.sp
        )
    }
}
