package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Abid Music Theme Palette - Premium Black & Neon Purple
val DeepSpaceBlack = Color(0xFF080511)
val DarkPurpleSurface = Color(0xFF120B22)
val DarkPurpleCard = Color(0xFF1A112E)
val DarkPurpleBorder = Color(0xFF2E1F4D)

val NeonPurple = Color(0xFFA855F7)
val BrightViolet = Color(0xFFC084FC)
val ElectricPink = Color(0xFFEC4899)
val NeonFuchsia = Color(0xFFD946EF)
val SoftPurple = Color(0xFFDDD6FE)
val MutedPurpleText = Color(0xFFA78BFA)

val TextWhite = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Light theme colors
val LightBg = Color(0xFFF7F5FC)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFEDE9FE)
val LightPrimary = Color(0xFF7C3AED)

// Gradients
val PurpleNeonGradient = Brush.horizontalGradient(
    listOf(NeonPurple, NeonFuchsia)
)

val PlayerBackgroundGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF2A104E),
        Color(0xFF140828),
        Color(0xFF080410)
    )
)

val CardGlowGradient = Brush.linearGradient(
    listOf(
        Color(0x33A855F7),
        Color(0x05D946EF)
    )
)
