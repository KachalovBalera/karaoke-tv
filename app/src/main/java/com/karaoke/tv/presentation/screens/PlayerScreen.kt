package com.karaoke.tv.presentation.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.RatingReason
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.presentation.components.TvButton
import com.karaoke.tv.presentation.theme.AccentPink
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "KaraokePlayback"

enum class PlaybackErrorType(val userMessage: String) {
    NETWORK_ERROR("Ошибка сети: проверьте подключение к Интернету или Wi-Fi на приставке"),
    VIDEO_NOT_FOUND("Файл караоке не найден на сервере"),
    SOURCE_RESTRICTION("Воспроизведение ограничено источником или сессия истекла"),
    UNKNOWN("Не удалось запустить видео")
}

enum class ActivePlayerType {
    EXOPLAYER
}

enum class PlaybackState {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    BUFFERING,
    ENDED,
    ERROR
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    currentQueueItem: QueueItem,
    queueList: List<QueueItem> = emptyList(),
    alternativeVersions: List<KaraokeVersion> = emptyList(),
    getAccessToken: (suspend (forceRefresh: Boolean) -> String)? = null,
    onSongEnded: () -> Unit,
    onSkipNext: () -> Unit,
    onSwitchVersion: (KaraokeVersion) -> Unit,
    onRateVersion: (versionId: String, stars: Int, reasons: List<RatingReason>) -> Unit,
    onClosePlayer: (removeFromQueue: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mainFocusRequester = remember { FocusRequester() }

    val activity = context as? Activity

    var playbackState by remember(currentQueueItem.id) { mutableStateOf(PlaybackState.LOADING) }
    var activeVersion by remember(currentQueueItem.id) { mutableStateOf(currentQueueItem.selectedVersion) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var showVersionSwitchDialog by remember { mutableStateOf(false) }

    // Error handling state
    var playbackErrorType by remember(currentQueueItem.id) { mutableStateOf<PlaybackErrorType?>(null) }
    var errorMessage by remember(currentQueueItem.id) { mutableStateOf<String?>(null) }
    var retryCount by remember(currentQueueItem.id) { mutableStateOf(0) }

    // ExoPlayer reference
    var exoPlayerRef by remember { mutableStateOf<ExoPlayer?>(null) }

    // Focus requesters for TV remote buttons
    val playPauseFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val forwardFocusRequester = remember { FocusRequester() }
    val nextFocusRequester = remember { FocusRequester() }
    val stopFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    val isPlaying = playbackState == PlaybackState.PLAYING
    val hasPlaybackError = playbackState == PlaybackState.ERROR

    var hasEndedTriggered by remember(currentQueueItem.id) { mutableStateOf(false) }

    val handleSongFinished: (Boolean) -> Unit = { shouldRemoveFromQueue ->
        if (!hasEndedTriggered) {
            hasEndedTriggered = true
            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | FINISH_TRIGGERED | Song='${currentQueueItem.songTitle}' QueueId=${currentQueueItem.id} | Seamless advancing")
            onSongEnded()
        }
    }

    val transitionTo: (PlaybackState, String) -> Unit = { newState, reason ->
        Log.d("KaraokeDiagnostic", "STATE_TRANSITION | ${playbackState.name} -> ${newState.name} | reason=$reason")
        playbackState = newState
    }

    // Keep screen on while playing
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Soundcore Rave 3S BT Audio Focus Management
    DisposableEffect(currentQueueItem.id) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val focusRequest: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d("KaraokePlayback", "Soundcore AudioFocus changed: $focusChange")
                }
                .build()
        } else null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest is AudioFocusRequest) {
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest is AudioFocusRequest) {
                audioManager?.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        }
    }

    // Verify version integrity immediately
    LaunchedEffect(currentQueueItem.id, activeVersion) {
        if (activeVersion == null) {
            Log.e("KaraokeDiagnostic", "PLAYER_ERROR | No activeVersion for queue item ${currentQueueItem.id} (${currentQueueItem.songTitle})")
            playbackErrorType = PlaybackErrorType.VIDEO_NOT_FOUND
            errorMessage = "Для этой песни не выбрана версия караоке"
            transitionTo(PlaybackState.ERROR, "Missing activeVersion")
        } else {
            val videoUrl = activeVersion?.videoUrl?.ifBlank { activeVersion?.embedUrl }.orEmpty()
            if (videoUrl.isBlank() && activeVersion?.sourceVideoId?.isBlank() == true) {
                Log.e("KaraokeDiagnostic", "PLAYER_ERROR | Empty media URL/ID for ${currentQueueItem.songTitle}")
                playbackErrorType = PlaybackErrorType.VIDEO_NOT_FOUND
                errorMessage = "Ссылка на видео караоке отсутствует"
                transitionTo(PlaybackState.ERROR, "Blank URL and videoId")
            } else {
                transitionTo(PlaybackState.LOADING, "Loading new track")
            }
        }
    }

    // Resource cleanup when PlayerScreen leaves composition or switches queue item
    DisposableEffect(currentQueueItem.id) {
        hasEndedTriggered = false
        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | START | Song='${currentQueueItem.songArtist} - ${currentQueueItem.songTitle}' | QueueId=${currentQueueItem.id}")
        onDispose {
            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | STOP_CLEANUP | Song='${currentQueueItem.songTitle}' | Disposing ExoPlayer")
            try {
                exoPlayerRef?.apply {
                    stop()
                    clearMediaItems()
                    release()
                }
                exoPlayerRef = null
            } catch (e: Exception) {
                Log.e(TAG, "Error disposing ExoPlayer: ${e.message}")
            }
            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | DISPOSED | All media resources released")
        }
    }

    // Playback control actions
    val triggerPlay: () -> Unit = {
        transitionTo(PlaybackState.PLAYING, "triggerPlay called")
        exoPlayerRef?.play()
    }

    val triggerPause: () -> Unit = {
        transitionTo(PlaybackState.PAUSED, "triggerPause called")
        exoPlayerRef?.pause()
    }

    val togglePlayPause: () -> Unit = {
        if (isPlaying) triggerPause() else triggerPlay()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    Log.d("KaraokeDiagnostic", "LIFECYCLE | ${event.name} -> pausing playback")
                    triggerPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val triggerStop: () -> Unit = {
        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | STOP_MANUAL | Song='${currentQueueItem.songTitle}'")
        try {
            exoPlayerRef?.stop()
            exoPlayerRef?.clearMediaItems()
            exoPlayerRef?.release()
            exoPlayerRef = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ExoPlayer: ${e.message}")
        }
        onClosePlayer(true)
    }

    val triggerReplay: () -> Unit = {
        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | REPLAY | Song: ${currentQueueItem.songTitle}")
        hasEndedTriggered = false
        transitionTo(PlaybackState.PLAYING, "Replay initiated")
        exoPlayerRef?.seekTo(0)
        exoPlayerRef?.play()
    }

    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(100)
            try { playPauseFocusRequester.requestFocus() } catch (e: Exception) {}
        } else {
            try { mainFocusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    // Auto-hide TV controls overlay after 5 seconds of continuous playback
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying && !hasPlaybackError && !showVersionSwitchDialog) {
            delay(5000)
            isControlsVisible = false
        }
    }

    // D-PAD & Remote Navigation handling
    BackHandler {
        if (showVersionSwitchDialog) {
            showVersionSwitchDialog = false
        } else if (isControlsVisible) {
            isControlsVisible = false
        } else {
            isControlsVisible = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(mainFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false

                when (keyEvent.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (!isControlsVisible) {
                            isControlsVisible = true
                            true
                        } else false
                    }
                    Key.DirectionUp -> {
                        isControlsVisible = true
                        true
                    }
                    Key.DirectionDown -> {
                        if (isControlsVisible) {
                            isControlsVisible = false
                            true
                        } else false
                    }
                    Key.MediaPlayPause -> {
                        togglePlayPause()
                        isControlsVisible = true
                        true
                    }
                    Key.MediaPlay -> {
                        triggerPlay()
                        isControlsVisible = true
                        true
                    }
                    Key.MediaPause -> {
                        triggerPause()
                        isControlsVisible = true
                        true
                    }
                    Key.MediaStop -> {
                        triggerStop()
                        true
                    }
                    Key.MediaRewind -> {
                        exoPlayerRef?.let { player ->
                            val newPos = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                            player.seekTo(newPos)
                            isControlsVisible = true
                        }
                        true
                    }
                    Key.MediaFastForward -> {
                        exoPlayerRef?.let { player ->
                            val duration = player.duration.coerceAtLeast(0L)
                            val newPos = (player.currentPosition + 10_000L).coerceAtMost(duration)
                            player.seekTo(newPos)
                            isControlsVisible = true
                        }
                        true
                    }
                    Key.MediaNext -> {
                        onSkipNext()
                        true
                    }
                    else -> false
                }
            }
    ) {
        // 1. ANDROIDX MEDIA3 EXOPLAYER ENGINE
        val currentVersion = activeVersion
        if (currentVersion != null && !hasPlaybackError) {
            var bearerToken by remember(currentVersion.id) { mutableStateOf<String?>(null) }
            var isAuthPending by remember(currentVersion.id) {
                mutableStateOf(currentVersion.source == SourceType.GOOGLE_DRIVE)
            }

            LaunchedEffect(currentVersion.id) {
                if (currentVersion.source == SourceType.GOOGLE_DRIVE) {
                    try {
                        Log.d(TAG, "Fetching Google Drive Bearer token for playback of: ${currentVersion.title}")
                        val token = getAccessToken?.invoke(false)
                        bearerToken = token
                        isAuthPending = false
                        Log.d(TAG, "Google Drive Bearer token acquired for playback")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get Google Drive access token: ${e.message}", e)
                        playbackErrorType = PlaybackErrorType.SOURCE_RESTRICTION
                        errorMessage = "Ошибка доступа к Google Диску: ${e.localizedMessage ?: "Сбой авторизации"}"
                        transitionTo(PlaybackState.ERROR, "Bearer token acquisition failed")
                    }
                } else {
                    isAuthPending = false
                }
            }

            if (!isAuthPending) {
                DisposableEffect(currentVersion.id, bearerToken) {
                    Log.d(TAG, "Initializing ExoPlayer for version: ${currentVersion.title}")
                    val rawUrl = currentVersion.videoUrl.ifBlank { currentVersion.embedUrl }
                    val mediaUrl = if (rawUrl.isNotBlank() && rawUrl.startsWith("http")) rawUrl
                        else "https://www.googleapis.com/drive/v3/files/${currentVersion.sourceVideoId}?alt=media"

                    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent("KaraokeTV/1.3.0 (Android TV; Media3 ExoPlayer)")
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(30000)
                        .setAllowCrossProtocolRedirects(true)

                    if (!bearerToken.isNullOrBlank()) {
                        httpDataSourceFactory.setDefaultRequestProperties(
                            mapOf("Authorization" to "Bearer $bearerToken")
                        )
                    }

                    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
                    val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

                    val exoPlayer = ExoPlayer.Builder(context)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .build().apply {
                            val audioAttributes = AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build()
                            setAudioAttributes(audioAttributes, true)

                            val mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl))
                            setMediaItem(mediaItem)
                            prepare()
                            playWhenReady = true
                        }
                    exoPlayerRef = exoPlayer

                    val listener = object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            when (state) {
                                Player.STATE_BUFFERING -> {
                                    Log.d(TAG, "ExoPlayer: STATE_BUFFERING")
                                    transitionTo(PlaybackState.BUFFERING, "ExoPlayer buffering")
                                }
                                Player.STATE_READY -> {
                                    Log.d(TAG, "ExoPlayer: STATE_READY")
                                    transitionTo(if (exoPlayer.isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED, "ExoPlayer ready")
                                }
                                Player.STATE_ENDED -> {
                                    Log.d(TAG, "ExoPlayer: STATE_ENDED")
                                    handleSongFinished(false)
                                }
                                else -> {}
                            }
                        }

                        override fun onIsPlayingChanged(playing: Boolean) {
                            if (playing) {
                                transitionTo(PlaybackState.PLAYING, "ExoPlayer playing")
                            } else if (exoPlayer.playbackState != Player.STATE_ENDED) {
                                transitionTo(PlaybackState.PAUSED, "ExoPlayer not playing")
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "ExoPlayer error: ${error.errorCodeName}: ${error.message} (cause: ${error.cause})")

                            val httpException = error.cause as? HttpDataSource.InvalidResponseCodeException
                            val responseCode = httpException?.responseCode ?: 0

                            if (responseCode == 401) {
                                Log.w(TAG, "ExoPlayer 401 Unauthorized encountered. Refreshing Google Drive token...")
                                coroutineScope.launch {
                                    try {
                                        val refreshed = getAccessToken?.invoke(true)
                                        if (!refreshed.isNullOrBlank()) {
                                            bearerToken = refreshed
                                            return@launch
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed refreshing token after 401: ${e.message}")
                                    }
                                    playbackErrorType = PlaybackErrorType.SOURCE_RESTRICTION
                                    errorMessage = "Сессия Google Drive истекла. Не удалось обновить ключ."
                                    transitionTo(PlaybackState.ERROR, "HTTP 401 Token expired")
                                }
                                return
                            }

                            if (responseCode == 404) {
                                playbackErrorType = PlaybackErrorType.VIDEO_NOT_FOUND
                                errorMessage = "Файл караоке не найден на сервере"
                                transitionTo(PlaybackState.ERROR, "HTTP 404 File not found")
                                return
                            }

                            playbackErrorType = PlaybackErrorType.NETWORK_ERROR
                            errorMessage = error.localizedMessage ?: "Сбой сетевого потока"
                            transitionTo(PlaybackState.ERROR, "ExoPlayer playback error: ${error.errorCodeName}")
                        }
                    }

                    exoPlayer.addListener(listener)

                    onDispose {
                        Log.d(TAG, "Disposing ExoPlayer for ${currentVersion.title}")
                        exoPlayer.removeListener(listener)
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        exoPlayer.release()
                        exoPlayerRef = null
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayerRef
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        view.player = exoPlayerRef
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. LOADING INDICATOR OVERLAY
        if (playbackState == PlaybackState.LOADING && !hasPlaybackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE05050A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
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
                        text = "Загрузка караоке...",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${currentQueueItem.songArtist} — ${currentQueueItem.songTitle}",
                        color = SecondaryCyan,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. BUFFERING INDICATOR
        if (playbackState == PlaybackState.BUFFERING && !hasPlaybackError) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
                    .background(Color(0xCC0D0B18), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = SecondaryCyan,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Буферизация...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 4. ERROR OVERLAY (Friendly, NEVER falls back to Rutube!)
        if (hasPlaybackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF50A0814)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(620.dp)
                        .background(SurfaceCard, RoundedCornerShape(24.dp))
                        .padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Ошибка",
                        tint = AccentPink,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Воспроизведение недоступно",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: (playbackErrorType?.userMessage ?: "Не удалось воспроизвести данный трек"),
                        color = TextSecondary,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TvButton(
                            text = "Повторить",
                            onClick = {
                                retryCount++
                                errorMessage = null
                                playbackErrorType = null
                                transitionTo(PlaybackState.LOADING, "User clicked Retry")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(retryFocusRequester),
                            isPrimary = true,
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        )

                        if (alternativeVersions.size > 1) {
                            TvButton(
                                text = "Другая версия",
                                onClick = { showVersionSwitchDialog = true },
                                modifier = Modifier.weight(1f),
                                isPrimary = false,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = SecondaryCyan, modifier = Modifier.size(20.dp))
                                }
                            )
                        }

                        TvButton(
                            text = "Пропустить",
                            onClick = onSkipNext,
                            modifier = Modifier.weight(1f),
                            isPrimary = false,
                            leadingIcon = {
                                Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }
            }
        }

        // 5. TV D-PAD CONTROLS OVERLAY
        AnimatedVisibility(
            visible = isControlsVisible && !hasPlaybackError && !showVersionSwitchDialog,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC05050A),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xF005050A)
                            )
                        )
                    )
            ) {
                // Top Track Info Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(horizontal = 48.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentQueueItem.songTitle,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentQueueItem.songArtist,
                                color = SecondaryCyan,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Поёт: ${currentQueueItem.participantName}",
                                color = TextMuted,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Source badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xDD1E1B4B), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "GOOGLE DRIVE",
                            color = SecondaryCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Bottom Control Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 48.dp, vertical = 36.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind -10s
                        TvButton(
                            text = "-10 сек",
                            onClick = {
                                exoPlayerRef?.let { player ->
                                    val newPos = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                                    player.seekTo(newPos)
                                }
                            },
                            isPrimary = false,
                            leadingIcon = {
                                Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(rewindFocusRequester)
                        )

                        // Play / Pause
                        TvButton(
                            text = if (isPlaying) "Пауза" else "Петь дальше",
                            onClick = togglePlayPause,
                            isPrimary = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .focusRequester(playPauseFocusRequester)
                        )

                        // Forward +10s
                        TvButton(
                            text = "+10 сек",
                            onClick = {
                                exoPlayerRef?.let { player ->
                                    val dur = player.duration.coerceAtLeast(0L)
                                    val newPos = (player.currentPosition + 10_000L).coerceAtMost(dur)
                                    player.seekTo(newPos)
                                }
                            },
                            isPrimary = false,
                            leadingIcon = {
                                Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(forwardFocusRequester)
                        )

                        // Skip Next
                        TvButton(
                            text = "Следующая",
                            onClick = onSkipNext,
                            isPrimary = false,
                            leadingIcon = {
                                Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .focusRequester(nextFocusRequester)
                        )

                        // Stop & Return to Catalog
                        TvButton(
                            text = "Завершить",
                            onClick = triggerStop,
                            isPrimary = false,
                            leadingIcon = {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = AccentPink, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .focusRequester(stopFocusRequester)
                        )
                    }
                }
            }
        }
    }
}
