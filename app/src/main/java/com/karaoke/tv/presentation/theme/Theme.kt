package com.karaoke.tv.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

// Deep dark background palette from mockup
val BackgroundDark = Color(0xFF07060F)
val SurfaceDark = Color(0xFF0C0A1A)
val SurfaceCard = Color(0xFF131024)
val SurfaceCardFocused = Color(0xFF221C3C)

// Sidebar palette
val SidebarBackground = Color(0xFF0A0818)
val SidebarBorder = Color(0xFF1A152E)

// Vibrant accents
val PrimaryPurple = Color(0xFF7C3AED)
val PrimaryPurpleGlow = Color(0xFF8B5CF6)
val AccentViolet = Color(0xFFA855F7)
val SecondaryCyan = Color(0xFF06B6D4)
val NeonCyan = Color(0xFF00F0FF)
val AccentPink = Color(0xFFEC4899)
val GoldStar = Color(0xFFFBBF24)

// Text colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFCBD5E1)
val TextMuted = Color(0xFF7B7898)

// Status colors
val SuccessGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFEF4444)

// UI elements
val SearchBarBg = Color(0xFF121023)
val SearchBarBorder = Color(0xFF2C2346)
val ChipBg = Color(0xFF100E22)
val ChipBorder = Color(0xFF27203E)
val ChipActiveBorder = Color(0xFF6D28D9)

private val KaraokeDarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = SecondaryCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun KaraokeTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KaraokeDarkColorScheme,
        content = content
    )
}
