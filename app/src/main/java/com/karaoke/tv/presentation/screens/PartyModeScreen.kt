package com.karaoke.tv.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.presentation.components.TvButton
import com.karaoke.tv.presentation.theme.AccentPink
import com.karaoke.tv.presentation.theme.BackgroundDark
import com.karaoke.tv.presentation.theme.GoldStar
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.PrimaryPurpleGlow
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary

@Composable
fun PartyModeScreen(
    currentQueue: List<QueueItem>,
    performanceHistory: List<String>,
    enableAnimations: Boolean,
    onToggleAnimations: () -> Unit,
    onStartSinging: () -> Unit,
    onConnectPhones: () -> Unit
) {
    val nextItem = currentQueue.firstOrNull()

    // Smooth pulse animation for next singer card (disabled on low-end TV boxes)
    val infiniteTransition = rememberInfiniteTransition(label = "partyPulse")
    val pulseScale by if (enableAnimations) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(36.dp)
    ) {
        // Left Column: Upcoming Singer Spotlight
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = AccentPink,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "РЕЖИМ ВЕЧЕРИНКИ",
                        color = PrimaryPurple,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "Подключайте смартфоны по QR-коду и пойте по очереди!",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Spotlight card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2E1065), Color(0xFF1E1B4B), Color(0xFF0C4A6E))
                            )
                        )
                        .border(2.dp, PrimaryPurpleGlow, RoundedCornerShape(20.dp))
                        .padding(28.dp)
                ) {
                    if (nextItem != null) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(AccentPink)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "СЛЕДУЮЩИЙ НА СЦЕНЕ:",
                                    color = GoldStar,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = nextItem.participantName,
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Песня: ${nextItem.songArtist} — ${nextItem.songTitle}",
                                color = SecondaryCyan,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            TvButton(
                                text = "НАЧАТЬ ВЫСТУПЛЕНИЕ 🎤",
                                onClick = onStartSinging,
                                isPrimary = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                        ) {
                            Text(
                                text = "Очередь вечеринки свободна!",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Отсканируйте QR-код на телефоне, чтобы добавить первую песню",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Quick Actions Bar
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TvButton(
                    text = "Подключить телефон (QR)",
                    onClick = onConnectPhones,
                    isPrimary = false
                )
                TvButton(
                    text = if (enableAnimations) "Анимации: ВКЛ" else "Анимации: ВЫКЛ (для слабых ТВ)",
                    onClick = onToggleAnimations,
                    isPrimary = false,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = SecondaryCyan)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(36.dp))

        // Right Column: Performance History & Up Next List
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            Text(
                text = "ИСТОРИЯ ВЫСТУПЛЕНИЙ СЕГОДНЯ",
                color = PrimaryPurple,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (performanceHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCard)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Вечеринка только началась!\nПервые спевшие участники появятся здесь.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(performanceHistory) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceCard)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = SecondaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = entry,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
