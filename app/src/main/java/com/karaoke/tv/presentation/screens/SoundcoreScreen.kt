package com.karaoke.tv.presentation.screens

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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoke.tv.presentation.components.TvButton
import com.karaoke.tv.presentation.theme.AccentPink
import com.karaoke.tv.presentation.theme.BackgroundDark
import com.karaoke.tv.presentation.theme.GoldStar
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.PrimaryPurpleGlow
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SuccessGreen
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary
import com.karaoke.tv.soundcore.AudioOutputStatus
import com.karaoke.tv.soundcore.SoundcoreManager

@Composable
fun SoundcoreScreen(
    audioStatus: AudioOutputStatus,
    latencyOffsetMs: Int,
    onAdjustLatency: (Int) -> Unit,
    onResetLatency: () -> Unit,
    onPlayTestTrack: () -> Unit,
    onRefreshStatus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(36.dp)
    ) {
        // Left Column: Status & Sync Calibration
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Speaker, contentDescription = null, tint = AccentPink, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SOUNDCORE RAVE 3S",
                        color = PrimaryPurple,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Настройка Bluetooth-аудиовыхода и синхронизации с микрофонами",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(
                        1.5.dp,
                        if (audioStatus.isSoundcoreDetected) SuccessGreen else if (audioStatus.isBluetoothConnected) SecondaryCyan else GoldStar,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (audioStatus.isBluetoothConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (audioStatus.isBluetoothConnected) SuccessGreen else GoldStar,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (audioStatus.isBluetoothConnected) "Bluetooth аудиовыход активен" else "Bluetooth колонка не обнаружена",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Устройство: ${audioStatus.deviceName}",
                        color = SecondaryCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = audioStatus.description,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Latency offset calibration card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = SecondaryCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Калибровка задержки Bluetooth (A/V Sync)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Если звук в колонке опережает или отстает от бегущей строки текста, настройте смещение задержки:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvButton(text = "-50 мс", onClick = { onAdjustLatency(latencyOffsetMs - 50) }, isPrimary = false)
                            TvButton(text = "-10 мс", onClick = { onAdjustLatency(latencyOffsetMs - 10) }, isPrimary = false)
                        }

                        Text(
                            text = if (latencyOffsetMs >= 0) "+$latencyOffsetMs мс" else "$latencyOffsetMs мс",
                            color = if (latencyOffsetMs == 0) TextPrimary else GoldStar,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvButton(text = "+10 мс", onClick = { onAdjustLatency(latencyOffsetMs + 10) }, isPrimary = false)
                            TvButton(text = "+50 мс", onClick = { onAdjustLatency(latencyOffsetMs + 50) }, isPrimary = false)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TvButton(
                            text = "Сбросить задержку (0 мс)",
                            onClick = onResetLatency,
                            isPrimary = false,
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = TextMuted)
                            }
                        )

                        TvButton(
                            text = "Проверить звук",
                            onClick = onPlayTestTrack,
                            isPrimary = true,
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(36.dp))

        // Right Column: Setup Instructions for Soundcore Rave 3S + Mics
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ИНСТРУКЦИЯ ПОДКЛЮЧЕНИЯ RAVE 3S",
                    color = PrimaryPurple,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(SoundcoreManager.CONNECTION_GUIDE) { stepText ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stepText,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            TvButton(
                text = "Обновить статус подключения",
                onClick = onRefreshStatus,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
