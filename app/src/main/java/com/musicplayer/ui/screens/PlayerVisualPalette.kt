package com.musicplayer.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

data class PlayerVisualPalette(
    val dark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val controlGlass: Color,
    val controlStroke: Color,
    val progressTrack: Color,
    val icon: Color,
    val glow: Color
)

@Composable
fun rememberPlayerVisualPalette(accent: Color, darkTheme: Boolean): PlayerVisualPalette {
    return remember(accent, darkTheme) {
        if (darkTheme) {
            PlayerVisualPalette(
                dark = true,
                backgroundTop = accent.mix(Color(0xFF07080D), 0.74f),
                backgroundBottom = accent.mix(Color(0xFF101018), 0.86f),
                primaryText = Color.White.copy(alpha = 0.94f),
                secondaryText = Color.White.copy(alpha = 0.70f),
                mutedText = Color.White.copy(alpha = 0.46f),
                controlGlass = Color(0xFF15161D).copy(alpha = 0.34f),
                controlStroke = Color.White.copy(alpha = 0.12f),
                progressTrack = Color.White.copy(alpha = 0.16f),
                icon = Color.White.copy(alpha = 0.76f),
                glow = accent.copy(alpha = 0.32f)
            )
        } else {
            PlayerVisualPalette(
                dark = false,
                backgroundTop = accent.mix(Color.White, 0.84f),
                backgroundBottom = accent.mix(Color(0xFFFFF7FB), 0.96f),
                primaryText = Color(0xFF171A22),
                secondaryText = Color(0xFF4D5260),
                mutedText = Color(0xFF7B8290),
                controlGlass = Color.White.copy(alpha = 0.28f),
                controlStroke = Color.White.copy(alpha = 0.42f),
                progressTrack = Color(0xFF171A22).copy(alpha = 0.12f),
                icon = Color(0xFF2C303A).copy(alpha = 0.78f),
                glow = accent.copy(alpha = 0.24f)
            )
        }
    }
}

fun Color.mix(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - t) + other.red * t,
        green = green * (1f - t) + other.green * t,
        blue = blue * (1f - t) + other.blue * t,
        alpha = alpha * (1f - t) + other.alpha * t
    )
}
