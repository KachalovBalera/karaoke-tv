package com.karaoke.tv.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.karaoke.tv.presentation.theme.BackgroundDark
import com.karaoke.tv.presentation.theme.ErrorRed
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
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    currentVersion: String,
    updateStatus: UpdateStatus,
    onStartDownload: () -> Unit,
    onInstallApk: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = !updateInfo.forceUpdate && updateStatus !is UpdateStatus.Downloading) {
        onDismiss()
    }

    val primaryActionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(updateStatus) {
        delay(150)
        runCatching {
            primaryActionFocusRequester.requestFocus()
        }
    }

    // Dimmed full-screen backdrop (non-clickable so D-pad cannot steal focus)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        // Centered Card Container
        Box(
            modifier = Modifier
                .width(620.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCard)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryPurpleGlow, SecondaryCyan)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(26.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Update Icon & Versions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdateAlt,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Доступно обновление по воздуху (OTA)",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Текущая: v$currentVersion",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "→", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryPurple)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Новая: v${updateInfo.version}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Release Notes Section
                Text(
                    text = "Что нового в этой версии:",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C0A1A))
                        .border(1.dp, Color(0xFF221C3C), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val notes = if (updateInfo.releaseNotes.isNotBlank()) {
                        updateInfo.releaseNotes
                    } else {
                        "• Улучшена стабильность работы\n• Оптимизация каталога минусовок\n• Обновления производительности для Android TV"
                    }

                    Text(
                        text = notes,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                // Dynamic Status / Progress Section
                when (updateStatus) {
                    is UpdateStatus.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Скачивание обновления...",
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                val percent = (updateStatus.progress * 100).toInt()
                                val mbDownloaded = updateStatus.downloadedBytes / (1024f * 1024f)
                                val mbTotal = updateStatus.totalBytes / (1024f * 1024f)
                                val downStr = String.format(java.util.Locale.US, "%.1f", mbDownloaded)
                                val totalStr = String.format(java.util.Locale.US, "%.1f", mbTotal)
                                Text(
                                    text = if (mbTotal > 0) {
                                        "$percent% ($downStr / $totalStr МБ)"
                                    } else {
                                        "$downStr МБ"
                                    },
                                    color = NeonCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Custom smooth Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF221C3C))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = updateStatus.progress.coerceIn(0.01f, 1f))
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(PrimaryPurple, SecondaryCyan)
                                            )
                                        )
                                )
                            }
                        }
                    }

                    is UpdateStatus.ReadyToInstall -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Пакет обновления загружен! Нажмите «Установить обновление».",
                                color = SuccessGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    is UpdateStatus.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = updateStatus.message,
                                color = ErrorRed,
                                fontSize = 12.5.sp,
                                maxLines = 2
                            )
                        }
                    }

                    else -> {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                // Action Buttons Row (D-Pad TV remote friendly)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!updateInfo.forceUpdate && updateStatus !is UpdateStatus.Downloading) {
                        OtaDialogButton(
                            text = "Напомнить позже",
                            isPrimary = false,
                            onClick = onDismiss
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                    }

                    when (updateStatus) {
                        is UpdateStatus.Downloading -> {
                            OtaDialogButton(
                                text = "Загрузка...",
                                isPrimary = true,
                                enabled = false,
                                onClick = {}
                            )
                        }

                        is UpdateStatus.ReadyToInstall -> {
                            OtaDialogButton(
                                text = "Установить обновление",
                                isPrimary = true,
                                focusRequester = primaryActionFocusRequester,
                                onClick = onInstallApk
                            )
                        }

                        is UpdateStatus.Error -> {
                            OtaDialogButton(
                                text = "Повторить попытку",
                                isPrimary = true,
                                focusRequester = primaryActionFocusRequester,
                                onClick = onStartDownload
                            )
                        }

                        else -> {
                            OtaDialogButton(
                                text = "Обновить сейчас",
                                isPrimary = true,
                                focusRequester = primaryActionFocusRequester,
                                onClick = onStartDownload
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtaDialogButton(
    text: String,
    isPrimary: Boolean,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1.0f, label = "btnScale")

    val backgroundBrush = when {
        !enabled -> Brush.linearGradient(listOf(Color(0xFF2A283E), Color(0xFF2A283E)))
        isPrimary && isFocused -> Brush.horizontalGradient(listOf(NeonCyan, SecondaryCyan))
        isPrimary -> Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryPurpleGlow))
        isFocused -> Brush.linearGradient(listOf(Color(0xFF26203D), Color(0xFF26203D)))
        else -> Brush.linearGradient(listOf(Color(0xFF161426), Color(0xFF161426)))
    }

    var buttonModifier = Modifier
        .scale(scale)
        .clip(RoundedCornerShape(12.dp))
        .background(backgroundBrush)
        .border(
            width = if (isFocused) 2.dp else 1.dp,
            color = when {
                isFocused -> NeonCyan
                isPrimary -> PrimaryPurpleGlow
                else -> Color(0xFF332D4F)
            },
            shape = RoundedCornerShape(12.dp)
        )

    if (focusRequester != null) {
        buttonModifier = buttonModifier.focusRequester(focusRequester)
    }

    buttonModifier = buttonModifier
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) {
            onClick()
        }
        .padding(horizontal = 20.dp, vertical = 11.dp)

    Box(
        modifier = buttonModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPrimary && isFocused) Color.Black else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
