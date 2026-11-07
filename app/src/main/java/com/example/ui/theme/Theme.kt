package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = DarkPurpleCard,
    onPrimaryContainer = SoftPurple,
    secondary = BrightViolet,
    onSecondary = Color.White,
    secondaryContainer = DarkPurpleBorder,
    onSecondaryContainer = SoftPurple,
    tertiary = ElectricPink,
    background = DeepSpaceBlack,
    onBackground = TextWhite,
    surface = DarkPurpleSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkPurpleCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkPurpleBorder
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightCard,
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = NeonPurple,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightCard,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFDDD6FE)
)

@Composable
fun AbidMusicTheme(
    darkTheme: Boolean = true, // Default to stunning dark & purple AMOLED look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
