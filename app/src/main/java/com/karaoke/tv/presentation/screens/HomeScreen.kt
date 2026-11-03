package com.karaoke.tv.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.karaoke.tv.R
import com.karaoke.tv.data.local.CategoryCatalogue
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import com.karaoke.tv.presentation.theme.AccentPink
import com.karaoke.tv.presentation.theme.BackgroundDark
import com.karaoke.tv.presentation.theme.ChipActiveBorder
import com.karaoke.tv.presentation.theme.ChipBg
import com.karaoke.tv.presentation.theme.ChipBorder
import com.karaoke.tv.presentation.theme.NeonCyan
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.PrimaryPurpleGlow
import com.karaoke.tv.presentation.theme.SearchBarBg
import com.karaoke.tv.presentation.theme.SearchBarBorder
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SuccessGreen
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    songsByCategory: Map<String, List<Song>>,
    currentQueue: List<QueueItem>,
    popularSongs: List<MockPopularItem> = remember { CategoryCatalogue.getShuffledPopularKaraoke(6) },
    selectedSource: SourceType? = null,
    onSelectSource: (SourceType?) -> Unit = {},
    onSongSelected: (Song) -> Unit,
    onOpenSearch: () -> Unit,
    onVoiceSearch: () -> Unit,
    onOpenSoundcore: () -> Unit,
    onSelectCategory: (String) -> Unit
) {
    // Current time for top status bar
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(30000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Top Status Bar & Hero Banner Section (Clean concert stage background shines through)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
            ) {
                // Content inside Hero Banner
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Status Bar (Wi-Fi, Time, Soundcore Rave 3S Chip)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wi-Fi Icon
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Clock
                        Text(
                            text = currentTime,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(18.dp))

                        // Soundcore Rave 3S Pill Button
                        SoundcoreStatusPill(onClick = onOpenSoundcore)
                    }

                    // Hero Text
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Караоке для всей семьи",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Тысячи караоке-минусовок с текстами",
                            color = Color(0xFFDDD6FE),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        // 2. Search Bar Capsule (Enlarged 64dp for TV)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 12.dp)
            ) {
                TvSearchBarCapsule(
                    onClick = onOpenSearch,
                    onVoiceClick = onVoiceSearch
                )
            }
        }

        // 3. "🔥 Популярные караоке" (Popular Karaoke Row)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "🔥 Популярные караоке",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(popularSongs) { item ->
                        MockKaraokeCard(
                            item = item,
                            onClick = { onSongSelected(item.toSong()) },
                            onLongClick = { onSongSelected(item.toSong()) }
                        )
                    }
                }
            }
        }

        // 4. "Категории" (Categories Row)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 20.dp)
            ) {
                Text(
                    text = "Категории",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 8.dp)
                )

                val categories = remember { getMockCategories() }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(categories) { cat ->
                        CategoryCard(
                            item = cat,
                            onClick = { onSelectCategory(cat.title.replace("\n", " ")) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Top Status Bar - Soundcore Status Pill
// -------------------------------------------------------------

@Composable
fun SoundcoreStatusPill(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1.0f, label = "btScale")

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0A192F))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) NeonCyan else Color(0xFF0284C7),
                shape = RoundedCornerShape(18.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = "Bluetooth",
            tint = NeonCyan,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Soundcore Rave 3S",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------
// Component: Search Bar Capsule
// -------------------------------------------------------------

@Composable
fun TvSearchBarCapsule(
    onClick: () -> Unit,
    onVoiceClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.02f else 1.0f, label = "searchScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(32.dp))
            .background(SearchBarBg)
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) PrimaryPurpleGlow else SearchBarBorder,
                shape = RoundedCornerShape(32.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Поиск",
                tint = Color(0xFFA78BFA),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Поиск караоке...",
                color = Color(0xFFC4B5FD),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Voice search mic icon
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Голосовой поиск",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable { onVoiceClick() }
        )
    }
}

// -------------------------------------------------------------
// Component: Source Filter Pill ("Источники поиска")
// -------------------------------------------------------------

@Composable
fun SourceFilterPill(
    name: String,
    badgeText: String? = null,
    badgeColor: Color = Color.Transparent,
    icon: ImageVector? = null,
    iconTint: Color = Color.White,
    hasCheckmark: Boolean = true,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.06f else 1.0f, label = "sourcePillScale")

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isActive) Color(0xFF1E143E) else ChipBg
            )
            .border(
                width = if (isFocused || isActive) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> PrimaryPurpleGlow
                    isActive -> ChipActiveBorder
                    else -> ChipBorder
                },
                shape = RoundedCornerShape(18.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        if (hasCheckmark) {
            Spacer(modifier = Modifier.width(7.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Активен",
                tint = SuccessGreen,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Popular Karaoke Card (16:9 4 cards row)
// -------------------------------------------------------------

data class MockPopularItem(
    val artist: String,
    val title: String,
    val duration: String,
    val songId: String,
    val coverUrl: String,
    val source: SourceType = SourceType.GOOGLE_DRIVE,
    val sourceVideoId: String = ""
) {
    fun toSong(): Song {
        val streamingUrl = if (sourceVideoId.isNotBlank()) "https://www.googleapis.com/drive/v3/files/$sourceVideoId?alt=media" else ""
        val gdriveVer = KaraokeVersion(
            id = "${songId}_gdrive",
            songId = songId,
            source = SourceType.GOOGLE_DRIVE,
            sourceVideoId = sourceVideoId,
            title = "$artist — $title",
            durationSeconds = 240,
            quality = "MP4 / HD",
            karaokeScore = 0.99f,
            thumbnailUrl = coverUrl,
            videoUrl = streamingUrl,
            embedUrl = streamingUrl,
            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
            availabilityStatus = PlaybackAvailability.AVAILABLE
        )
        return Song(
            id = songId,
            title = title,
            artist = artist,
            category = "Google Drive",
            coverUrl = coverUrl,
            versions = listOf(gdriveVer),
            bestVersionId = gdriveVer.id
        )
    }
}

fun getMockPopularKaraokeSongs(): List<MockPopularItem> {
    return CategoryCatalogue.getShuffledPopularKaraoke(5)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MockKaraokeCard(
    item: MockPopularItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.06f else 1.0f, label = "cardScale")

    Box(
        modifier = Modifier
            .width(260.dp)
            .height(160.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131122))
            .border(
                width = if (isFocused) 3.5.dp else 1.dp,
                color = if (isFocused) PrimaryPurpleGlow else Color(0xFF26203D),
                shape = RoundedCornerShape(16.dp)
            )
            .focusable(interactionSource = interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick() },
                onLongClick = onLongClick
            )
    ) {
        // 1. Universal Concert Stage Card Background
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.card_concert_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. High-contrast vertical gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x44000000),
                            Color(0xCC070A18),
                            Color(0xF8070A18)
                        ),
                        startY = 20f
                    )
                )
        )

        // 3. Source / Quality Badge (Top Right)
        val badgeText = if (item.duration.isNotBlank() && item.duration != "MP4") item.duration else "1080p / MP4"
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xDD1E1B4B))
                .border(1.dp, SecondaryCyan.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // 4. Large Contrasting Song Title and Artist (Inside card, bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.artist,
                color = SecondaryCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Category Card (8 categories)
// -------------------------------------------------------------

data class MockCategory(
    val title: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

fun getMockCategories(): List<MockCategory> {
    return listOf(
        MockCategory(
            title = "Русский\nпоп",
            icon = Icons.Default.Mic,
            gradientColors = listOf(Color(0xFF6B21A8), Color(0xFF4C1D95))
        ),
        MockCategory(
            title = "Рок",
            icon = Icons.Default.MusicNote,
            gradientColors = listOf(Color(0xFF9D174D), Color(0xFF700C32))
        ),
        MockCategory(
            title = "Дискотека\n90-х",
            icon = Icons.Default.Public,
            gradientColors = listOf(Color(0xFF881337), Color(0xFF4C0519))
        ),
        MockCategory(
            title = "Хиты\n2000-х",
            icon = Icons.Default.Equalizer,
            gradientColors = listOf(Color(0xFF581C87), Color(0xFF3B0764))
        ),
        MockCategory(
            title = "Современные\nхиты",
            icon = Icons.Default.Star,
            gradientColors = listOf(Color(0xFF78350F), Color(0xFF451A03))
        ),
        MockCategory(
            title = "Детские\nпесни",
            icon = Icons.Default.ChildCare,
            gradientColors = listOf(Color(0xFF713F12), Color(0xFF3A2006))
        ),
        MockCategory(
            title = "Любовные\nхиты",
            icon = Icons.Default.Favorite,
            gradientColors = listOf(Color(0xFF9F1239), Color(0xFF4C0519))
        ),
        MockCategory(
            title = "Из кино\nи мультфильмов",
            icon = Icons.Default.Movie,
            gradientColors = listOf(Color(0xFF0369A1), Color(0xFF082F49))
        )
    )
}

@Composable
fun CategoryCard(
    item: MockCategory,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.07f else 1.0f, label = "catScale")

    Box(
        modifier = Modifier
            .width(135.dp)
            .height(95.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(item.gradientColors)
            )
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) NeonCyan else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
