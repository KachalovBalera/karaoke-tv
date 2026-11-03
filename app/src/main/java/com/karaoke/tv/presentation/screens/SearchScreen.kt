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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.presentation.components.TvSongCard
import com.karaoke.tv.presentation.theme.BackgroundDark
import com.karaoke.tv.presentation.theme.GoldStar
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.PrimaryPurpleGlow
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.SurfaceCardFocused
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary

@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<Song>,
    isLoading: Boolean,
    sourceFilter: SourceType?,
    onFilterChange: (SourceType?) -> Unit,
    searchHistory: List<String>,
    sourceErrors: Map<SourceType, String>,
    onSongSelected: (Song) -> Unit,
    onVoiceInputRequested: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 44.dp, vertical = 24.dp)
    ) {
        // Search Header
        Text(
            text = "ПОИСК КАРАОКЕ RUTUBE",
            color = PrimaryPurple,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Search Input Bar (Enlarged 64dp for TV)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isFocused) SurfaceCardFocused else SurfaceCard)
                    .border(
                        width = if (isFocused) 3.dp else 1.dp,
                        color = if (isFocused) PrimaryPurpleGlow else Color(0xFF33334D),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isFocused) PrimaryPurpleGlow else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(PrimaryPurple),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) }),
                        modifier = Modifier
                            .weight(1f)
                            .focusable(interactionSource = interactionSource),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Например: «Кино Группа крови», «Руки Вверх», «Баста»...",
                                    color = TextMuted,
                                    fontSize = 20.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Очистить",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { onQueryChange("") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Voice input button
            val micInteraction = remember { MutableInteractionSource() }
            val micFocused by micInteraction.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (micFocused) PrimaryPurpleGlow else SurfaceCard)
                    .border(
                        width = if (micFocused) 3.dp else 1.dp,
                        color = if (micFocused) SecondaryCyan else Color(0xFF33334D),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .focusable(interactionSource = micInteraction)
                    .clickable(interactionSource = micInteraction, indication = null) { onVoiceInputRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Голосовой поиск",
                    tint = if (micFocused) Color.White else SecondaryCyan,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RUTUBE Source Status Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⭐ Источник поиска: RUTUBE Караоке с текстом (Full HD)",
                color = SecondaryCyan,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Errors/Warnings from non-responding sources
        if (sourceErrors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                sourceErrors.forEach { (src, errorMsg) ->
                    Text(
                        text = "⚠ ${src.displayName}: $errorMsg",
                        color = GoldStar,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recent searches if no query
        if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
            Text(
                text = "Недавние запросы:",
                color = TextSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                searchHistory.take(5).forEach { historyQuery ->
                    val hInteraction = remember { MutableInteractionSource() }
                    val hFocused by hInteraction.collectIsFocusedAsState()

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (hFocused) SurfaceCardFocused else SurfaceCard)
                            .border(
                                width = if (hFocused) 2.dp else 1.dp,
                                color = if (hFocused) PrimaryPurpleGlow else Color(0xFF33334D),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .focusable(interactionSource = hInteraction)
                            .clickable(interactionSource = hInteraction, indication = null) {
                                onQueryChange(historyQuery)
                                onSearch(historyQuery)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = historyQuery,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Loading or Results Grid
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryPurple)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Поиск караоке-минусовок с текстом...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "По запросу «$searchQuery» ничего не найдено.\nПопробуйте изменить запрос или переключить источник.",
                    color = TextMuted,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 210.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults) { song ->
                    TvSongCard(
                        song = song,
                        onClick = { onSongSelected(song) },
                        onLongClick = { onSongSelected(song) }
                    )
                }
            }
        }
    }
}
