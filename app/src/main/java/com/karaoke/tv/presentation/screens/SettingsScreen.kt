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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
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
import com.karaoke.tv.presentation.theme.NeonCyan
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.PrimaryPurpleGlow
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SuccessGreen
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary
import com.karaoke.tv.update.AppUpdateInfo
import com.karaoke.tv.update.UpdateStatus

@Composable
fun SettingsScreen(
    // Google Drive Props
    isGoogleDriveConnected: Boolean = false,
    googleDriveAccountEmail: String = "",
    totalDriveSongsCount: Int = 0,
    isSyncingGoogleDrive: Boolean = false,
    syncProgressText: String = "",
    onSyncGoogleDrive: () -> Unit = {},
    onDeleteGoogleDriveKey: () -> Unit = {},
    // Optimization & Maintenance Props
    enableAnimations: Boolean,
    onToggleAnimations: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onResetDatabase: () -> Unit,
    // OTA Update Props
    currentAppVersion: String,
    updateStatus: UpdateStatus,
    updateInfo: AppUpdateInfo?,
    customManifestUrl: String,
    onCustomManifestUrlChange: (String) -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 44.dp, vertical = 24.dp),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(34.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "НАСТРОЙКИ СИСТЕМЫ",
                    color = PrimaryPurple,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Karaoke TV v$currentAppVersion — Единственный источник: Google Drive",
                color = TextSecondary,
                fontSize = 17.sp
            )
        }

        // Section 1: Google Drive (Единственный активный источник)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.5.dp, PrimaryPurpleGlow, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "ОБЛАЧНЫЙ ИСТОЧНИК (GOOGLE DRIVE)",
                                color = TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1EA362))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "АКТИВНЫЙ ИСТОЧНИК",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Воспроизведение личной коллекции MP4 песен напрямую с Google Диска через Google Drive API и защищенный Service Account.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )

                    // Connection status badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isGoogleDriveConnected) Color(0xFF0F2B1F) else Color(0xFF2E2210))
                            .border(
                                1.5.dp,
                                if (isGoogleDriveConnected) SuccessGreen.copy(alpha = 0.7f) else GoldStar.copy(alpha = 0.7f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isGoogleDriveConnected) Icons.Default.CheckCircle else Icons.Default.Key,
                                contentDescription = null,
                                tint = if (isGoogleDriveConnected) SuccessGreen else GoldStar,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isGoogleDriveConnected) {
                                        "Google Service Account: $googleDriveAccountEmail"
                                    } else {
                                        "Ключ Google Drive не найден в Keystore."
                                    },
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "В каталоге Google Drive: $totalDriveSongsCount песен" +
                                            if (syncProgressText.isNotBlank()) " • $syncProgressText" else "",
                                    color = if (isGoogleDriveConnected) NeonCyan else TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TvButton(
                            text = if (isSyncingGoogleDrive) "Синхронизация..." else "Синхронизировать Google Диск",
                            onClick = onSyncGoogleDrive,
                            isPrimary = true,
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        )

                        if (isGoogleDriveConnected) {
                            TvButton(
                                text = "Удалить ключ Google Drive",
                                onClick = onDeleteGoogleDriveKey,
                                isPrimary = false,
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = AccentPink, modifier = Modifier.size(20.dp))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Performance & Optimization
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = AccentPink, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Производительность и оптимизация ТВ",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Плавные анимации и эффекты свечения",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Рекомендуется оставить включенным для Allwinner H618",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        TvButton(
                            text = if (enableAnimations) "ВКЛЮЧЕНО" else "ОТКЛЮЧЕНО",
                            onClick = onToggleAnimations,
                            isPrimary = enableAnimations
                        )
                    }
                }
            }
        }

        // Section 3: OTA Updates
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = SecondaryCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Обновление по воздуху (OTA)",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Версия v$currentAppVersion",
                            color = PrimaryPurpleGlow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Приложение автоматически проверяет наличие свежих релизов с GitHub при каждом запуске. Также можно выполнить поиск вручную.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvButton(
                            text = when (updateStatus) {
                                is UpdateStatus.Checking -> "Проверка..."
                                else -> "Проверить обновление"
                            },
                            onClick = onCheckForUpdates,
                            isPrimary = false
                        )

                        if (updateInfo != null) {
                            TvButton(
                                text = "Доступна v${updateInfo.version}!",
                                onClick = onOpenUpdateDialog,
                                isPrimary = true
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Maintenance & Reset
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = AccentPink, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Обслуживание и хранилище Room",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TvButton(
                            text = "Очистить историю поиска",
                            onClick = onClearSearchHistory,
                            isPrimary = false
                        )

                        TvButton(
                            text = "Сбросить базу песен",
                            onClick = onResetDatabase,
                            isPrimary = false
                        )
                    }
                }
            }
        }
    }
}
