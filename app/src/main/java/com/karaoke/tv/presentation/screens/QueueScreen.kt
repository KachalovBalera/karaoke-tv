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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.QueueStatus
import com.karaoke.tv.presentation.components.SourceBadge
import com.karaoke.tv.presentation.components.TvButton
import com.karaoke.tv.presentation.theme.BackgroundDark
import com.karaoke.tv.presentation.theme.GoldStar
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SuccessGreen
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary

@Composable
fun QueueScreen(
    queue: List<QueueItem>,
    onPlayCurrent: () -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onSkip: () -> Unit,
    onRepeat: () -> Unit,
    onClearQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ОЧЕРЕДЬ КАРАОКЕ",
                    color = PrimaryPurple,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (queue.isEmpty()) "Очередь пуста" else "Всего композиций: ${queue.size}",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            if (queue.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvButton(
                        text = "Петь сейчас",
                        onClick = onPlayCurrent,
                        isPrimary = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                    )
                    TvButton(
                        text = "Пропустить",
                        onClick = onSkip,
                        isPrimary = false,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, tint = SecondaryCyan)
                        }
                    )
                    TvButton(
                        text = "Очистить очередь",
                        onClick = onClearQueue,
                        isPrimary = false,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "В очереди пока нет песен.",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Найдите песню через поиск или подключите смартфон по QR-коду!",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                itemsIndexed(queue) { index, item ->
                    val isCurrent = index == 0 && item.status == QueueStatus.CURRENT

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Order number / Status
                        Box(
                            modifier = Modifier
                                .width(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                Text(
                                    text = "▶",
                                    color = SuccessGreen,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            } else {
                                Text(
                                    text = "#${index + 1}",
                                    color = TextMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .size(80.dp, 56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            if (item.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = item.songTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Title, Artist, Singer
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.songTitle,
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                item.selectedVersion?.let { ver ->
                                    SourceBadge(source = ver.source)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.songArtist,
                                color = SecondaryCyan,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                                Text(
                                    text = "🎤 Поет: ",
                                    color = TextMuted,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = item.participantName,
                                    color = GoldStar,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Management buttons (D-pad operable)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (index > 0) {
                                TvButton(
                                    text = "▲",
                                    onClick = { onMoveUp(item.id) },
                                    isPrimary = false
                                )
                            }
                            if (index < queue.size - 1) {
                                TvButton(
                                    text = "▼",
                                    onClick = { onMoveDown(item.id) },
                                    isPrimary = false
                                )
                            }
                            TvButton(
                                text = "✕",
                                onClick = { onRemove(item.id) },
                                isPrimary = false
                            )
                        }
                    }
                }
            }
        }
    }
}
