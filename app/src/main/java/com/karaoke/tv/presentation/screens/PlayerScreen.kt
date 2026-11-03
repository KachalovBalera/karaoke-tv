package com.karaoke.tv.presentation.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import com.karaoke.tv.data.local.CategoryCatalogue
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.RatingReason
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.presentation.components.TvButton
import com.karaoke.tv.presentation.theme.AccentPink
import com.karaoke.tv.presentation.theme.GoldStar
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "KaraokePlayback"

enum class PlaybackErrorType(val userMessage: String) {
    NETWORK_ERROR("Ошибка сети: проверьте подключение к Интернету на приставке"),
    EMBEDDING_DISABLED("Автор видео ограничил встроенное воспроизведение (встраивание запрещено)"),
    AUTOPLAY_BLOCKED("Автовоспроизведение заблокировано системой: нажмите кнопку «Воспроизвести» на пульте"),
    UNSUPPORTED_FORMAT("Формат видеопотока не поддерживается"),
    VIDEO_NOT_FOUND("Видео удалено или недоступно в источнике"),
    SOURCE_RESTRICTION("Воспроизведение ограничено источником"),
    UNKNOWN("Не удалось запустить видео")
}

enum class ActivePlayerType {
    EXOPLAYER,
    YOUTUBE_IFRAME,
    WEBVIEW_EMBED
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
    var hasEndedTriggered by remember(currentQueueItem.id) { mutableStateOf(false) }
    var isTransitioning by remember(currentQueueItem.id) { mutableStateOf(false) }

    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var hasPlaybackStarted by remember { mutableStateOf(false) }

    var hasPlaybackError by remember { mutableStateOf(false) }
    var playbackErrorType by remember { mutableStateOf(PlaybackErrorType.UNKNOWN) }
    var errorMessage by remember { mutableStateOf("") }

    val playPauseFocusRequester = remember { FocusRequester() }
    var lastInteractionTime by remember { mutableStateOf(SystemClock.uptimeMillis()) }

    fun transitionTo(newState: PlaybackState, reason: String) {
        val oldState = playbackState
        if (oldState != newState) {
            playbackState = newState
            Log.d("KaraokeDiagnostic", "PLAYER_STATE_TRANSITION | $oldState -> $newState | reason: $reason | song: '${currentQueueItem.songArtist} - ${currentQueueItem.songTitle}' (queueId: ${currentQueueItem.id})")
            when (newState) {
                PlaybackState.PLAYING -> {
                    isPlaying = true
                    hasPlaybackStarted = true
                }
                PlaybackState.PAUSED -> {
                    isPlaying = false
                }
                PlaybackState.ENDED -> {
                    isPlaying = false
                }
                PlaybackState.ERROR -> {
                    isPlaying = false
                    hasPlaybackError = true
                }
                else -> {}
            }
        }
    }

    // 25-second loading timeout watchdog: prevents infinite loading hangs while allowing sufficient buffer time
    LaunchedEffect(currentQueueItem.id, playbackState) {
        if (playbackState == PlaybackState.LOADING) {
            delay(25000)
            if (playbackState == PlaybackState.LOADING) {
                Log.w("KaraokeDiagnostic", "PLAYER_TIMEOUT | Loading song '${currentQueueItem.songTitle}' exceeded 25 seconds")
                playbackErrorType = PlaybackErrorType.NETWORK_ERROR
                errorMessage = "Видео не запустилось за 25 секунд. Проверьте интернет или повторите попытку."
                transitionTo(PlaybackState.ERROR, "25s Loading timeout exceeded")
            }
        }
    }

    // Index-aware queue lookup so we never skip ahead inappropriately
    val currentIndex = queueList.indexOfFirst { it.id == currentQueueItem.id }
    val nextInQueue = remember(queueList, currentQueueItem.id) {
        if (currentIndex >= 0 && currentIndex + 1 < queueList.size) {
            queueList[currentIndex + 1]
        } else {
            queueList.firstOrNull { it.id != currentQueueItem.id }
        }
    }

    BackHandler {
        Log.d("KaraokeDiagnostic", "PLAYER_BACK_PRESSED | state=$playbackState | controls=$isControlsVisible")
        if (isControlsVisible) {
            isControlsVisible = false
        } else {
            onClosePlayer(false)
        }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val activeVersion = currentQueueItem.selectedVersion

    // Verify version integrity immediately to avoid black screen on invalid data
    LaunchedEffect(currentQueueItem.id, activeVersion) {
        if (activeVersion == null) {
            Log.e("KaraokeDiagnostic", "PLAYER_ERROR | No activeVersion for queue item ${currentQueueItem.id} (${currentQueueItem.songTitle})")
            playbackErrorType = PlaybackErrorType.VIDEO_NOT_FOUND
            errorMessage = "Для этой песни не выбрана рабочая версия караоке"
            transitionTo(PlaybackState.ERROR, "Missing activeVersion")
        } else {
            val videoUrl = activeVersion.videoUrl.ifBlank { activeVersion.embedUrl }
            if (videoUrl.isBlank() && activeVersion.sourceVideoId.isBlank()) {
                Log.e("KaraokeDiagnostic", "PLAYER_ERROR | Empty media URL/ID for ${currentQueueItem.songTitle}")
                playbackErrorType = PlaybackErrorType.VIDEO_NOT_FOUND
                errorMessage = "Ссылка на видео караоке отсутствует или повреждена"
                transitionTo(PlaybackState.ERROR, "Blank URL and videoId")
            } else {
                transitionTo(PlaybackState.LOADING, "Loading new track")
            }
        }
    }

    // Determine active player engine
    val playerType = remember(activeVersion) {
        if (activeVersion == null) {
            ActivePlayerType.WEBVIEW_EMBED
        } else {
            val url = (activeVersion.videoUrl.ifBlank { activeVersion.embedUrl }).lowercase()
            val isGoogleDrive = activeVersion.source == SourceType.GOOGLE_DRIVE ||
                    url.contains("googleapis.com/drive") ||
                    url.contains("drive.google.com")
            val isDirectMedia = isGoogleDrive || url.endsWith(".mp4") || url.endsWith(".m3u8") ||
                    url.endsWith(".mpd") || url.startsWith("file://") ||
                    url.startsWith("content://") || activeVersion.source == SourceType.LOCAL

            when {
                isDirectMedia -> ActivePlayerType.EXOPLAYER
                activeVersion.source == SourceType.YOUTUBE && activeVersion.sourceVideoId.isNotBlank() -> ActivePlayerType.YOUTUBE_IFRAME
                else -> ActivePlayerType.WEBVIEW_EMBED
            }
        }
    }

    // References to underlying active player controllers
    var youTubePlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var exoPlayerRef by remember { mutableStateOf<ExoPlayer?>(null) }

    val safelyDestroyWebView: (WebView?) -> Unit = remember {
        { wv ->
            if (wv != null && wv.tag != "DESTROYED") {
                wv.tag = "DESTROYED"
                try {
                    Log.d("KaraokeDiagnostic", "PLAYER_EVENT | WEBVIEW_CLEANUP | Safely releasing WebView")
                    wv.clearFocus()
                    wv.stopLoading()
                    wv.pauseTimers()
                    wv.onPause()
                    wv.loadUrl("about:blank")
                    wv.clearHistory()
                    wv.removeJavascriptInterface("KaraokeBridge")
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.removeAllViews()
                    wv.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "Error disposing WebView: ${e.message}")
                }
            }
        }
    }

    // CRITICAL: Robust cleanup when PlayerScreen leaves composition or switches queue item
    DisposableEffect(currentQueueItem.id) {
        hasEndedTriggered = false
        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | START | Song='${currentQueueItem.songArtist} - ${currentQueueItem.songTitle}' | QueueId=${currentQueueItem.id} | Engine=$playerType")
        onDispose {
            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | STOP_CLEANUP | Song='${currentQueueItem.songTitle}' | Disposing all player engines and releasing audio focus")
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

            try {
                youTubePlayerRef?.pause()
                youTubePlayerRef = null
            } catch (e: Exception) {
                Log.e(TAG, "Error disposing YouTubePlayer: ${e.message}")
            }

            safelyDestroyWebView(webViewRef)
            webViewRef = null

            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioManager?.abandonAudioFocusRequest(
                        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    audioManager?.abandonAudioFocus(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error abandoning audio focus: ${e.message}")
            }
            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | DISPOSED | All media resources released")
        }
    }

    // Unified playback control actions
    val triggerPlay: () -> Unit = {
        transitionTo(PlaybackState.PLAYING, "triggerPlay called")
        when (playerType) {
            ActivePlayerType.YOUTUBE_IFRAME -> {
                Log.d(TAG, "Command: YouTube play")
                youTubePlayerRef?.play()
            }
            ActivePlayerType.WEBVIEW_EMBED -> {
                Log.d(TAG, "Command: WebView postMessage play")
                webViewRef?.let { wv ->
                    val now = SystemClock.uptimeMillis()
                    val x = wv.width / 2f
                    val y = wv.height / 2f
                    val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
                    val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, x, y, 0)
                    wv.dispatchTouchEvent(down)
                    wv.dispatchTouchEvent(up)
                    down.recycle()
                    up.recycle()
                }
                webViewRef?.evaluateJavascript(
                    """
                    (function() {
                        window.userPaused = false;
                        try {
                            var ifr = document.getElementById('karaokeIframe');
                            if (ifr && ifr.contentWindow) {
                                ifr.contentWindow.postMessage(JSON.stringify({ type: 'player:play', data: {} }), '*');
                                ifr.contentWindow.postMessage(JSON.stringify({ type: 'player:play' }), '*');
                            }
                            var vids = document.getElementsByTagName('video');
                            for (var i = 0; i < vids.length; i++) {
                                vids[i].play();
                            }
                        } catch(e) {}
                    })();
                    """.trimIndent(),
                    null
                )
            }
            ActivePlayerType.EXOPLAYER -> {
                Log.d(TAG, "Command: ExoPlayer play")
                exoPlayerRef?.play()
            }
        }
    }

    val triggerPause: () -> Unit = {
        transitionTo(PlaybackState.PAUSED, "triggerPause called")
        when (playerType) {
            ActivePlayerType.YOUTUBE_IFRAME -> {
                Log.d(TAG, "Command: YouTube pause")
                youTubePlayerRef?.pause()
            }
            ActivePlayerType.WEBVIEW_EMBED -> {
                Log.d(TAG, "Command: WebView postMessage pause")
                webViewRef?.evaluateJavascript(
                    """
                    (function() {
                        window.userPaused = true;
                        try {
                            var ifr = document.getElementById('karaokeIframe');
                            if (ifr && ifr.contentWindow) {
                                ifr.contentWindow.postMessage(JSON.stringify({ type: 'player:pause', data: {} }), '*');
                                ifr.contentWindow.postMessage(JSON.stringify({ type: 'player:pause' }), '*');
                            }
                            var vids = document.getElementsByTagName('video');
                            for (var i = 0; i < vids.length; i++) {
                                vids[i].pause();
                            }
                        } catch(e) {}
                    })();
                    """.trimIndent(),
                    null
                )
            }
            ActivePlayerType.EXOPLAYER -> {
                Log.d(TAG, "Command: ExoPlayer pause")
                exoPlayerRef?.pause()
            }
        }
    }

    val togglePlayPause: () -> Unit = {
        if (isPlaying) triggerPause() else triggerPlay()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    Log.d("KaraokeDiagnostic", "LIFECYCLE | ${event.name} -> pausing playback without destroying state")
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

    val handleSongFinished: (Boolean) -> Unit = { _ ->
        if (!isTransitioning && !hasEndedTriggered) {
            hasEndedTriggered = true
            isTransitioning = true
            transitionTo(PlaybackState.ENDED, "Media ended")
            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | FINISHED | Auto-advancing queue without rating screen")
            onSongEnded()
        }
    }

    val performSkipNext: () -> Unit = {
        if (!isTransitioning) {
            isTransitioning = true
            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | SKIP_NEXT | User initiated skip to next song")
            onSkipNext()
        }
    }

    val triggerReplay: () -> Unit = {
        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | REPLAY | User requested to sing again: ${currentQueueItem.songTitle}")
        hasEndedTriggered = false
        transitionTo(PlaybackState.PLAYING, "Replay initiated")
        when (playerType) {
            ActivePlayerType.WEBVIEW_EMBED -> {
                webViewRef?.evaluateJavascript(
                    """
                    (function() {
                        window.hasEnded = false;
                        window.isPlayingState = false;
                        try {
                            var ifr = document.getElementById('karaokeIframe');
                            if (ifr && ifr.contentWindow) {
                                ifr.contentWindow.postMessage(JSON.stringify({ type: 'player:setCurrentTime', data: { time: 0 } }), '*');
                                ifr.contentWindow.postMessage(JSON.stringify({ type: 'player:play', data: {} }), '*');
                            }
                            var vids = document.getElementsByTagName('video');
                            for (var i = 0; i < vids.length; i++) {
                                vids[i].currentTime = 0;
                                vids[i].play();
                            }
                        } catch(e) {}
                    })();
                    """.trimIndent(),
                    null
                )
            }
            ActivePlayerType.YOUTUBE_IFRAME -> {
                youTubePlayerRef?.seekTo(0f)
                youTubePlayerRef?.play()
            }
            ActivePlayerType.EXOPLAYER -> {
                exoPlayerRef?.seekTo(0)
                exoPlayerRef?.play()
            }
        }
    }

    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(100)
            try { playPauseFocusRequester.requestFocus() } catch (e: Exception) {}
        } else {
            try { mainFocusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    // Auto-hide TV controls overlay after 5 seconds of continuous playback without interaction
    LaunchedEffect(isControlsVisible, isPlaying, lastInteractionTime) {
        if (isControlsVisible && isPlaying && !hasPlaybackError) {
            delay(5000)
            isControlsVisible = false
        }
    }

    LaunchedEffect(currentQueueItem.id) {
        delay(100)
        try {
            mainFocusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(mainFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    lastInteractionTime = SystemClock.uptimeMillis()
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter -> {
                            if (!isControlsVisible) {
                                togglePlayPause()
                                isControlsVisible = true
                                coroutineScope.launch {
                                    delay(50)
                                    try { playPauseFocusRequester.requestFocus() } catch (e: Exception) {}
                                }
                                true
                            } else {
                                // Controls are open: let the focused TvButton handle Enter/Center click
                                false
                            }
                        }
                        Key.MediaPlayPause -> {
                            togglePlayPause()
                            isControlsVisible = true
                            coroutineScope.launch {
                                delay(50)
                                try { playPauseFocusRequester.requestFocus() } catch (e: Exception) {}
                            }
                            true
                        }
                        Key.DirectionLeft, Key.DirectionRight -> {
                            if (!isControlsVisible) {
                                isControlsVisible = true
                                coroutineScope.launch {
                                    delay(50)
                                    try { playPauseFocusRequester.requestFocus() } catch (e: Exception) {}
                                }
                                true
                            } else {
                                // Controls visible: let focus system move between onscreen buttons without seeking!
                                false
                            }
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            if (!isControlsVisible) {
                                isControlsVisible = true
                                coroutineScope.launch {
                                    delay(50)
                                    try { playPauseFocusRequester.requestFocus() } catch (e: Exception) {}
                                }
                                true
                            } else {
                                false
                            }
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
                            performSkipNext()
                            true
                        }
                        Key.Back -> {
                            if (isControlsVisible) {
                                isControlsVisible = false
                                true
                            } else {
                                onClosePlayer(false)
                                true
                            }
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. VIDEO ENGINE CONTAINER
        if (activeVersion != null && !hasPlaybackError) {
            when (playerType) {
                ActivePlayerType.EXOPLAYER -> {
                    // AndroidX Media3 ExoPlayer for direct streams (Google Drive MP4, HLS, DASH, local files)
                    var bearerToken by remember(activeVersion.id) { mutableStateOf<String?>(null) }
                    var isAuthPending by remember(activeVersion.id) {
                        mutableStateOf(activeVersion.source == SourceType.GOOGLE_DRIVE)
                    }

                    LaunchedEffect(activeVersion.id) {
                        if (activeVersion.source == SourceType.GOOGLE_DRIVE) {
                            try {
                                Log.d(TAG, "Fetching Google Drive Bearer token for playback of: ${activeVersion.title}")
                                val token = getAccessToken?.invoke(false)
                                bearerToken = token
                                isAuthPending = false
                                Log.d(TAG, "Google Drive Bearer token successfully acquired for playback")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to get Google Drive access token: ${e.message}", e)
                                playbackErrorType = PlaybackErrorType.SOURCE_RESTRICTION
                                errorMessage = "Ошибка доступа к Google Диску: ${e.localizedMessage ?: "Сбой авторизации"}"
                                transitionTo(PlaybackState.ERROR, "Bearer token acquisition failed")
                            }
                        }
                    }

                    if (!isAuthPending) {
                        DisposableEffect(activeVersion.id, bearerToken) {
                            Log.d(TAG, "Initializing ExoPlayer for version: ${activeVersion.title}")
                            val mediaUrl = activeVersion.videoUrl.ifBlank { activeVersion.embedUrl }

                            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                                .setUserAgent("KaraokeTV/1.2.4 (Android TV; Media3 ExoPlayer)")
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

                                    if (responseCode == 401 && activeVersion.source == SourceType.GOOGLE_DRIVE) {
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

                                    if (responseCode == 403) {
                                        playbackErrorType = PlaybackErrorType.SOURCE_RESTRICTION
                                        errorMessage = "Доступ ограничен или квота Google Drive исчерпана"
                                        transitionTo(PlaybackState.ERROR, "HTTP 403 Quota exceeded")
                                        return
                                    }

                                    if (responseCode == 404) {
                                        playbackErrorType = PlaybackErrorType.VIDEO_NOT_FOUND
                                        errorMessage = "Файл караоке не найден на Google Диске"
                                        transitionTo(PlaybackState.ERROR, "HTTP 404 File not found")
                                        return
                                    }

                                    playbackErrorType = when (error.errorCode) {
                                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                                            PlaybackErrorType.NETWORK_ERROR
                                        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                                        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
                                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                                            PlaybackErrorType.UNSUPPORTED_FORMAT
                                        else -> PlaybackErrorType.UNKNOWN
                                    }
                                    errorMessage = playbackErrorType.userMessage
                                    transitionTo(PlaybackState.ERROR, "ExoPlayer error: ${error.errorCodeName}")
                                }
                            }
                            exoPlayer.addListener(listener)

                            onDispose {
                                Log.d(TAG, "Releasing ExoPlayer")
                                exoPlayer.removeListener(listener)
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

                ActivePlayerType.YOUTUBE_IFRAME -> {
                    // Official YouTube IFrame Player API via androidyoutubeplayer
                    AndroidView(
                        factory = { ctx ->
                            YouTubePlayerView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                enableAutomaticInitialization = false

                                val playerOptions = IFramePlayerOptions.Builder()
                                    .controls(0) // Hide YouTube's built-in controls in favor of TV UI
                                    .autoplay(1)
                                    .rel(0)
                                    .ivLoadPolicy(3)
                                    .ccLoadPolicy(0)
                                    .build()

                                initialize(
                                    object : AbstractYouTubePlayerListener() {
                                        override fun onReady(youTubePlayer: YouTubePlayer) {
                                            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | YOUTUBE_READY | videoId=${activeVersion.sourceVideoId}")
                                            youTubePlayerRef = youTubePlayer
                                            youTubePlayer.loadVideo(activeVersion.sourceVideoId, 0f)
                                        }

                                        override fun onStateChange(
                                            youTubePlayer: YouTubePlayer,
                                            state: PlayerConstants.PlayerState
                                        ) {
                                            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | YOUTUBE_STATE | state=$state")
                                            when (state) {
                                                PlayerConstants.PlayerState.BUFFERING -> {
                                                    transitionTo(PlaybackState.BUFFERING, "YouTube buffering")
                                                }
                                                PlayerConstants.PlayerState.PLAYING -> {
                                                    transitionTo(PlaybackState.PLAYING, "YouTube playing")
                                                }
                                                PlayerConstants.PlayerState.PAUSED -> {
                                                    transitionTo(PlaybackState.PAUSED, "YouTube paused")
                                                }
                                                PlayerConstants.PlayerState.ENDED -> {
                                                    handleSongFinished(false)
                                                }
                                                else -> {}
                                            }
                                        }

                                        override fun onError(
                                            youTubePlayer: YouTubePlayer,
                                            error: PlayerConstants.PlayerError
                                        ) {
                                            Log.e("KaraokeDiagnostic", "PLAYER_EVENT | YOUTUBE_ERROR | $error for video ${activeVersion.sourceVideoId}")
                                            playbackErrorType = when (error) {
                                                PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
                                                    PlaybackErrorType.EMBEDDING_DISABLED
                                                PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
                                                    PlaybackErrorType.VIDEO_NOT_FOUND
                                                PlayerConstants.PlayerError.HTML_5_PLAYER ->
                                                    PlaybackErrorType.UNSUPPORTED_FORMAT
                                                else -> PlaybackErrorType.SOURCE_RESTRICTION
                                            }
                                            errorMessage = playbackErrorType.userMessage
                                            transitionTo(PlaybackState.ERROR, "YouTube error: $error")
                                        }
                                    },
                                    playerOptions
                                )
                            }
                        },
                        onRelease = { view ->
                            Log.d("KaraokeDiagnostic", "PLAYER_EVENT | YOUTUBE_RELEASE | onRelease called")
                            try {
                                view.release()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in YouTube onRelease: ${e.message}")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ActivePlayerType.WEBVIEW_EMBED -> {
                    // RUTUBE & VK Video official embed in optimized Android TV WebView
                    val effectiveEmbedUrl = when {
                        activeVersion.source == SourceType.RUTUBE -> {
                            val rawId = activeVersion.sourceVideoId.removePrefix("rutube_").trim()
                            val cleanId = when {
                                rawId.length >= 20 -> rawId
                                activeVersion.embedUrl.contains("/embed/") -> activeVersion.embedUrl.substringAfter("/embed/").substringBefore("?").trim().trimEnd('/')
                                activeVersion.videoUrl.contains("/video/") -> activeVersion.videoUrl.substringAfter("/video/").substringBefore("?").trim().trimEnd('/')
                                else -> CategoryCatalogue.resolveRutubeId(currentQueueItem.songArtist, currentQueueItem.songTitle) ?: ""
                            }.removePrefix("rutube_").trim()

                            if (cleanId.isNotBlank()) {
                                "https://rutube.ru/play/embed/$cleanId?autoplay=1&autoStart=true&skinColor=9b51e0"
                            } else if (activeVersion.embedUrl.isNotBlank()) {
                                val separator = if (activeVersion.embedUrl.contains("?")) "&" else "?"
                                if (activeVersion.embedUrl.contains("autoplay")) activeVersion.embedUrl else "${activeVersion.embedUrl}${separator}autoplay=1"
                            } else {
                                activeVersion.videoUrl
                            }
                        }
                        activeVersion.embedUrl.isNotBlank() -> {
                            val separator = if (activeVersion.embedUrl.contains("?")) "&" else "?"
                            if (activeVersion.embedUrl.contains("autoplay")) activeVersion.embedUrl else "${activeVersion.embedUrl}${separator}autoplay=1"
                        }
                        activeVersion.videoUrl.isNotBlank() -> activeVersion.videoUrl
                        else -> ""
                    }

                    val baseUrl = when (activeVersion.source) {
                        SourceType.VK -> "https://vk.com/"
                        else -> "https://rutube.ru/"
                    }

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewRef = this
                                keepScreenOn = true
                                isFocusable = false
                                isFocusableInTouchMode = false
                                clearFocus()

                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    allowContentAccess = true
                                    allowFileAccess = true
                                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                                }

                                webChromeClient = WebChromeClient()

                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        Log.e("KaraokeDiagnostic", "PLAYER_EVENT | WEBVIEW_ERROR | code=$errorCode desc=$description url=$failingUrl")
                                        playbackErrorType = PlaybackErrorType.NETWORK_ERROR
                                        errorMessage = PlaybackErrorType.NETWORK_ERROR.userMessage
                                        transitionTo(PlaybackState.ERROR, "WebView onReceivedError: $errorCode")
                                    }
                                }

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onVideoStarted() {
                                        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | RESUME | Web playback active")
                                        coroutineScope.launch {
                                            transitionTo(PlaybackState.PLAYING, "Web playback active")
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onVideoPaused() {
                                        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | PAUSE_DETECTED | Web playback paused")
                                        coroutineScope.launch {
                                            transitionTo(PlaybackState.PAUSED, "Web playback paused")
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onVideoBuffering() {
                                        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | BUFFERING | Web playback buffering")
                                        coroutineScope.launch {
                                            transitionTo(PlaybackState.BUFFERING, "Web playback buffering")
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onAdStarted() {
                                        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | AD_STARTED | RUTUBE advertisement started, suppressing song end")
                                    }

                                    @JavascriptInterface
                                    fun onAdEnded() {
                                        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | AD_ENDED | RUTUBE advertisement ended, resuming normal karaoke monitoring")
                                    }

                                    @JavascriptInterface
                                    fun onVideoEnded() {
                                        Log.d("KaraokeDiagnostic", "PLAYER_EVENT | ENDED | Web playback ended")
                                        coroutineScope.launch {
                                            handleSongFinished(false)
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onVideoError(msg: String) {
                                        Log.e("KaraokeDiagnostic", "PLAYER_EVENT | WEBVIEW_BRIDGE_ERROR | $msg")
                                        coroutineScope.launch {
                                            playbackErrorType = PlaybackErrorType.SOURCE_RESTRICTION
                                            errorMessage = "Ошибка плеера: $msg"
                                            transitionTo(PlaybackState.ERROR, "Bridge error: $msg")
                                        }
                                    }
                                }, "KaraokeBridge")

                                val html = buildEmbedHtml(effectiveEmbedUrl)
                                loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                            }
                        },
                        update = { wv ->
                            // Ensure webViewRef points to active view
                            webViewRef = wv
                        },
                        onRelease = { view ->
                            safelyDestroyWebView(view)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 1.5. LOADING INDICATOR OVERLAY (eliminates black screen during track transition and buffering)
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${currentQueueItem.songArtist} — ${currentQueueItem.songTitle}",
                        color = SecondaryCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Исполнитель: ${currentQueueItem.participantName}",
                        color = TextMuted,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // 2. TV TOP BAR: Singer, Song title & Queue info (43-75" TV Large Typography)
        AnimatedVisibility(
            visible = isControlsVisible && !hasPlaybackError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xF0000000), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 44.dp, vertical = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎤 СЕЙЧАС ПОЕТ: ",
                                color = GoldStar,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = currentQueueItem.participantName,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${currentQueueItem.songArtist} — ${currentQueueItem.songTitle}",
                            color = SecondaryCyan,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        activeVersion?.let { ver ->
                            Text(
                                text = "Источник: ${ver.source.displayName} (${ver.quality})",
                                color = TextMuted,
                                fontSize = 18.sp
                            )
                        }
                        if (nextInQueue != null) {
                            Text(
                                text = "Далее: ${nextInQueue.songArtist} — ${nextInQueue.songTitle} (${nextInQueue.participantName})",
                                color = SecondaryCyan.copy(alpha = 0.9f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "Очередь: пуста (далее — поиск)",
                                color = TextMuted,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. TV BOTTOM BAR: Playback controls (Large touch targets for TV remotes)
        AnimatedVisibility(
            visible = isControlsVisible && !hasPlaybackError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xF0000000))
                        )
                    )
                    .padding(horizontal = 44.dp, vertical = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = null,
                                    tint = SecondaryCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        )

                        TvButton(
                            text = if (isPlaying) "Пауза" else "Играть",
                            onClick = togglePlayPause,
                            isPrimary = true,
                            modifier = Modifier.focusRequester(playPauseFocusRequester),
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        )

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
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = null,
                                    tint = SecondaryCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        )

                        TvButton(
                            text = "Следующая",
                            onClick = performSkipNext,
                            isPrimary = false,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    tint = SecondaryCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        )

                        TvButton(
                            text = "Завершить",
                            onClick = { onClosePlayer(false) },
                            isPrimary = false,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = AccentPink,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        )
                    }

                    Text(
                        text = "ОК — Пауза/Плей • Стрелки — Навигация • Назад — Закрыть",
                        color = TextMuted,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 5. ERROR FALLBACK DIALOG (Detailed Diagnosis & Explicit User Actions)
        if (hasPlaybackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE0F0F1A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(580.dp)
                        .background(SurfaceCard, RoundedCornerShape(16.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = GoldStar,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ошибка воспроизведения видео",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage.ifBlank { playbackErrorType.userMessage },
                        color = TextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TvButton(
                        text = "Повторить попытку воспроизведения",
                        onClick = {
                            hasPlaybackError = false
                            transitionTo(PlaybackState.LOADING, "User retry playback")
                            triggerReplay()
                        },
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val otherVersions = alternativeVersions.filter { it.id != activeVersion?.id }
                    if (otherVersions.isNotEmpty()) {
                        Text(
                            text = "Доступны альтернативные версии этой песни:",
                            color = SecondaryCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val bestAlternative = otherVersions.first()
                        TvButton(
                            text = "Переключить на ${bestAlternative.source.displayName} (${bestAlternative.quality})",
                            onClick = {
                                hasPlaybackError = false
                                onSwitchVersion(bestAlternative)
                            },
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fallback to external application
                    activeVersion?.let { ver ->
                        if (ver.videoUrl.isNotBlank()) {
                            TvButton(
                                text = "Открыть во внешнем приложении ${ver.source.displayName}",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ver.videoUrl))
                                    context.startActivity(intent)
                                },
                                isPrimary = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    TvButton(
                        text = "Вернуться к поиску песен",
                        onClick = { onClosePlayer(false) },
                        isPrimary = otherVersions.isEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TvButton(
                        text = "Пропустить песню",
                        onClick = onSkipNext,
                        isPrimary = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun buildEmbedHtml(embedUrl: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body, html { width: 100%; height: 100%; background: #000; overflow: hidden; }
                iframe { width: 100%; height: 100%; border: none; display: block; }

                /* Eliminate all Rutube recommendations and pause/end overlays */
                [class*="pauseScreen"], [class*="pause_recomendation"], [class*="recs"],
                [class*="endScreen"], [class*="recommendation"], .zergnet-recommend,
                [class*="raichu-pause-recs"], [class*="raichu-recs"],
                [class*="pauseRecs"], [class*="nextVideo"], [class*="upNext"],
                .raichu-pause-screen, .raichu-end-screen {
                    display: none !important;
                    visibility: hidden !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                    z-index: -9999 !important;
                    height: 0 !important;
                    width: 0 !important;
                }
            </style>
        </head>
        <body>
            <iframe id="karaokeIframe" src="$embedUrl" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen></iframe>
            <script>
                window.userPaused = false;
                window.isAdPlaying = false;
                var isPlayingState = false;
                var hasStarted = false;
                var hasEnded = false;

                // Override Page Visibility & suppress blur events so RUTUBE doesn't pause when TV UI elements gain focus
                try {
                    Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                    Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                    window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                    window.addEventListener('blur', function(e) { e.stopImmediatePropagation(); }, true);
                } catch(e) {}

                // Rutube Raichu flags: "1" === ue.getItem("raichu-pause-recs-hidden")
                try {
                    localStorage.setItem('raichu-pause-recs-hidden', '1');
                    localStorage.setItem('raichu-autoplay-next', '0');
                    localStorage.setItem('raichu-end-recs-hidden', '1');
                    localStorage.setItem('raichu-player-volume', '1');
                    localStorage.setItem('raichu-player-muted', 'false');
                } catch(e) {}

                function notifyStarted() {
                    hasStarted = true;
                    if (!isPlayingState) {
                        isPlayingState = true;
                        try { window.KaraokeBridge.onVideoStarted(); } catch(e) {}
                    }
                }
                function notifyPaused() {
                    if (isPlayingState) {
                        isPlayingState = false;
                        try { window.KaraokeBridge.onVideoPaused(); } catch(e) {}
                    }
                }
                function notifyEnded() {
                    if (window.isAdPlaying) {
                        return; // Discard advertisement completion events so songs are never terminated early
                    }
                    if (hasStarted && !hasEnded) {
                        hasEnded = true;
                        try { window.KaraokeBridge.onVideoEnded(); } catch(e) {}
                    }
                }

                function sendRutubePlay() {
                    try {
                        var ifr = document.getElementById('karaokeIframe');
                        if (ifr && ifr.contentWindow) {
                            ifr.contentWindow.postMessage(JSON.stringify({ type: 'player:play', data: {} }), '*');
                        }
                    } catch(e) {}
                }

                // Initial start triggers to ensure autoplay starts on TV
                setTimeout(sendRutubePlay, 500);
                setTimeout(sendRutubePlay, 1500);

                // Listen for iframe postMessage events (Rutube, VK, etc.)
                window.addEventListener('message', function(event) {
                    if (!event.data) return;
                    try {
                        var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                        if (!data) return;

                        // RUTUBE Advertisement events: shield karaoke from preroll/midroll ads
                        if (data.type === 'player:adStart' || data.type === 'ad_block_start' ||
                            data.type === 'ad_preroll_block_start' || data.type === 'ad_midroll_block_start') {
                            window.isAdPlaying = true;
                            try { window.KaraokeBridge.onAdStarted(); } catch(e) {}
                        }
                        if (data.type === 'player:adEnd' || data.type === 'player:adError' ||
                            data.type === 'ad_block_end' || data.type === 'ad_postroll_block_end') {
                            window.isAdPlaying = false;
                            try { window.KaraokeBridge.onAdEnded(); } catch(e) {}
                        }

                        // Rutube currentTime tracking: detect near-end before endScreen triggers!
                        if (data.type === 'player:currentTime' && data.data) {
                            var cur = typeof data.data.time === 'number' ? data.data.time : (typeof data.data.currentTime === 'number' ? data.data.currentTime : 0);
                            var dur = typeof data.data.duration === 'number' ? data.data.duration : 0;
                            if (dur > 10 && cur >= (dur - 1.2)) {
                                notifyEnded();
                            }
                        }

                        // Rutube & VK playing states
                        if (data.type === 'player:playStart' || data.type === 'player:canPlay' ||
                            (data.type === 'player:changeState' && (data.data && (data.data.state === 'playing' || data.data.status === 'playing' || data.data.state === 1) || data.state === 'playing' || data.state === 1))) {
                            notifyStarted();
                        }

                        // Rutube pause event
                        if (data.type === 'player:changeState' && (data.data && (data.data.state === 'pause' || data.data.status === 'pause' || data.data.state === 2) || data.state === 'pause' || data.state === 2)) {
                            if (window.userPaused) {
                                notifyPaused();
                            }
                        }

                        // Rutube buffering state
                        if (data.type === 'player:buffering' || (data.type === 'player:changeState' && (data.data && (data.data.state === 'buffering' || data.data.state === 3)))) {
                            try { window.KaraokeBridge.onVideoBuffering(); } catch(e) {}
                        }

                        // Rutube & VK ended states (CRITICAL: Never treat state 0 as ended - in RUTUBE API 0 is unstarted/buffering)
                        if (data.type === 'player:ended' || data.type === 'player:playComplete' ||
                            (data.type === 'player:changeState' && (data.data && (data.data.state === 'ended' || data.data.state === 'completed' || data.data.status === 'completed') || data.state === 'ended' || data.state === 'completed'))) {
                            notifyEnded();
                        }

                        // Error states
                        if (data.type === 'player:error' || data.type === 'player:startError') {
                            try { window.KaraokeBridge.onVideoError(data.data ? data.data.message : 'Ошибка воспроизведения'); } catch(e) {}
                        }
                    } catch(e) {}
                });

                // Attach listeners to any HTML5 video elements that appear inside embed
                setInterval(function() {
                    try {
                        var vids = document.getElementsByTagName('video');
                        for (var i = 0; i < vids.length; i++) {
                            var v = vids[i];
                            if (!v.dataset.karaokeAttached) {
                                v.dataset.karaokeAttached = 'true';
                                v.addEventListener('playing', notifyStarted);
                                v.addEventListener('pause', function() {
                                    if (window.userPaused) notifyPaused();
                                });
                                v.addEventListener('ended', function() {
                                    if (!window.isAdPlaying && v.currentTime > 5) {
                                        notifyEnded();
                                    }
                                });
                            }
                        }
                    } catch(e) {}
                }, 1000);
            </script>
        </body>
        </html>
    """.trimIndent()
}
