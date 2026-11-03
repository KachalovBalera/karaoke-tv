package com.karaoke.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.presentation.components.TvButton
import com.karaoke.tv.presentation.components.TvVersionItem
import com.karaoke.tv.presentation.theme.AccentPink
import com.karaoke.tv.presentation.theme.BackgroundDark
import com.karaoke.tv.presentation.theme.GoldStar
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary

@Composable
fun SongDetailScreen(
    song: Song,
    onPlayVersion: (KaraokeVersion, participantName: String) -> Unit,
    onAddToQueue: (KaraokeVersion, participantName: String) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    isFavorite: Boolean,
    onBack: () -> Unit
) {
    val availableVersions = remember(song) {
        if (song.versions.isNotEmpty()) song.versions else listOfNotNull(song.bestVersion)
    }
    var selectedVersion by remember(song) {
        mutableStateOf(availableVersions.firstOrNull())
    }
    var participantName by remember { mutableStateOf("Гость") }

    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        playFocusRequester.requestFocus()
    }

    BackHandler {
        onBack()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(44.dp)
    ) {
        // Left Column: Song Cover, Title, and Action Buttons
        Column(
            modifier = Modifier
                .width(380.dp)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
            ) {
                if (song.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.karaoke.tv.R.drawable.card_concert_bg),
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = song.title,
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = song.artist,
                color = SecondaryCyan,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            // TV Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(
                    text = "Петь сейчас (ОК)",
                    onClick = {
                        selectedVersion?.let { onPlayVersion(it, participantName) }
                    },
                    isPrimary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(playFocusRequester),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                )

                TvButton(
                    text = "Добавить в очередь",
                    onClick = {
                        selectedVersion?.let { onAddToQueue(it, participantName) }
                    },
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                )

                TvButton(
                    text = if (isFavorite) "В избранном" else "В избранное",
                    onClick = { onToggleFavorite(song) },
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = AccentPink,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(44.dp))

        // Right Column: Karaoke Track Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ВЕРСИИ КАРАОКЕ (MP4)",
                    color = PrimaryPurple,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "HD 720p / 1080p • С текстом",
                    color = TextMuted,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(availableVersions) { version ->
                    val isCurrent = version.id == selectedVersion?.id
                    TvVersionItem(
                        version = version,
                        isSelected = isCurrent,
                        onSelect = {
                            selectedVersion = version
                            onPlayVersion(version, participantName)
                        }
                    )
                }
            }
        }
    }
}
