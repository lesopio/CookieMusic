package com.musicplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.musicplayer.data.AppColorSettings
import com.musicplayer.data.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    surfaceTint = md_theme_light_surfaceTint
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    surfaceTint = md_theme_dark_surfaceTint
)

@Composable
fun MusicPlayerTheme(
    themeMode: ThemeMode = ThemeMode.System,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    },
    dynamicColor: Boolean = true,
    appColorSettings: AppColorSettings = AppColorSettings(),
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val seed = if (appColorSettings.followNowPlayingAccentEnabled) {
        appColorSettings.lastNowPlayingAccentArgb
    } else {
        appColorSettings.seedColorArgb
    }
    val colorScheme = baseScheme.withSeedAccent(seed, darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun ColorScheme.withSeedAccent(argb: Long, darkTheme: Boolean): ColorScheme {
    val primary = Color(argb.toInt())
    val primaryContainer = primary.blend(if (darkTheme) Color.Black else Color.White, if (darkTheme) 0.38f else 0.72f)
    val secondary = primary.blend(if (darkTheme) Color.White else Color.Black, if (darkTheme) 0.22f else 0.18f)
    val secondaryContainer = secondary.blend(if (darkTheme) Color.Black else Color.White, if (darkTheme) 0.42f else 0.78f)
    return copy(
        primary = primary,
        onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = if (primaryContainer.luminance() > 0.5f) Color.Black else Color.White,
        secondary = secondary,
        onSecondary = if (secondary.luminance() > 0.5f) Color.Black else Color.White,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = if (secondaryContainer.luminance() > 0.5f) Color.Black else Color.White,
        surfaceTint = primary
    )
}

private fun Color.blend(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = 1f
    )
}
