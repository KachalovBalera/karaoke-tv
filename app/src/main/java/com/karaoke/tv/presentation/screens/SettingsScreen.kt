package com.karaoke.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoke.tv.domain.model.SourceType
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
import com.karaoke.tv.presentation.theme.SurfaceCardFocused
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary
import com.karaoke.tv.update.AppUpdateInfo
import com.karaoke.tv.update.UpdateStatus

@Composable
fun SettingsScreen(
    youtubeApiKey: String,
    onYoutubeApiKeyChange: (String) -> Unit,
    vkAccessToken: String,
    onVkAccessTokenChange: (String) -> Unit,
    sourcePriority: SourceType?,
    onSourcePriorityChange: (SourceType?) -> Unit,
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
                text = "Karaoke TV v1.1.0 — RUTUBE Edition: единый каталог караоке с текстом",
                color = TextSecondary,
                fontSize = 17.sp
            )
        }

        // Section: Active Video Source (RUTUBE Exclusive)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Api,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ЕДИНЫЙ ИСТОЧНИК ВИДЕО — RUTUBE",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "В версии v1.1.0 поиск и воспроизведение караоке полностью переведены на RUTUBE. Это обеспечивает максимальную стабильность в РФ, встроенные субтитры/текст и непрерывное воспроизведение без рекламных пауз.",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F2238))
                            .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "RUTUBE активен и работает без API-ключей, авторизации и ограничений скорости.",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Section: Performance & Optimization
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
                                text = "Отключите на недорогих ТВ-приставках с объемом ОЗУ 1-2 ГБ для максимального быстродействия",
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

        // Section: OTA Updates (GitHub Releases)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdateAlt,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Обновление по воздуху (OTA)",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Current version tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E143E))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Версия v$currentAppVersion",
                                color = PrimaryPurpleGlow,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Приложение автоматически проверяет наличие свежих релизов с GitHub при каждом запуске. Также можно выполнить поиск вручную.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    // Current update status display
                    when (updateStatus) {
                        is UpdateStatus.Checking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Связываемся с GitHub для проверки новой версии...",
                                    color = NeonCyan,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        is UpdateStatus.UpToDate -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = updateStatus.message,
                                    color = SuccessGreen,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        is UpdateStatus.UpdateAvailable -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = GoldStar,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Найдено обновление: v${updateStatus.info.version}!",
                                        color = GoldStar,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        is UpdateStatus.ReadyToInstall -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Пакет v${updateStatus.info.version} загружен и готов к установке.",
                                    color = SuccessGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        is UpdateStatus.Downloading -> {
                            val percent = (updateStatus.progress * 100).toInt()
                            Text(
                                text = "Идёт скачивание APK: $percent%",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        is UpdateStatus.Error -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = updateStatus.message,
                                    color = Color(0xFFEF4444),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        else -> Unit
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvButton(
                            text = if (updateStatus is UpdateStatus.Checking) "Проверяем..." else "Проверить обновление",
                            onClick = onCheckForUpdates,
                            isPrimary = updateStatus !is UpdateStatus.UpdateAvailable && updateStatus !is UpdateStatus.ReadyToInstall
                        )

                        if (updateInfo != null || updateStatus is UpdateStatus.UpdateAvailable || updateStatus is UpdateStatus.ReadyToInstall) {
                            TvButton(
                                text = "Открыть окно обновления",
                                onClick = onOpenUpdateDialog,
                                isPrimary = true
                            )
                        }
                    }

                    // Optional Custom Manifest URL
                    Column {
                        Text(
                            text = "URL манифеста обновлений (по умолчанию GitHub Releases):",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingsInputField(
                            value = customManifestUrl,
                            onValueChange = onCustomManifestUrlChange,
                            placeholder = "https://raw.githubusercontent.com/KachalovBalera/karaoke-tv/main/version.json"
                        )
                    }
                }
            }
        }

        // Section: Maintenance
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
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Обслуживание и хранилище Room",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvButton(
                            text = "Очистить историю поиска",
                            onClick = onClearSearchHistory,
                            isPrimary = false
                        )
                        TvButton(
                            text = "Сбросить к заводским песням",
                            onClick = onResetDatabase,
                            isPrimary = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) SurfaceCardFocused else Color(0xFF141424))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) PrimaryPurpleGlow else Color(0xFF33334D),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(PrimaryPurple),
            modifier = Modifier
                .fillMaxWidth()
                .focusable(interactionSource = interactionSource),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(text = placeholder, color = TextMuted, fontSize = 13.sp)
                }
                inner()
            }
        )
    }
}
