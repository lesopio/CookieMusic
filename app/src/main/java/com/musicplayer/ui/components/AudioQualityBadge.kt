package com.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.data.Song

@Immutable
data class AudioQualityBadgeSpec(
    val label: String,
    val background: Color,
    val foreground: Color
)

fun audioQualityBadgeSpec(song: Song?): AudioQualityBadgeSpec? {
    val info = song?.audioInfo ?: return null
    val format = info.format.orEmpty().uppercase()
    val sampleRate = info.sampleRateHz ?: 0
    val bitDepth = info.bitDepth ?: 0
    val bitrate = info.bitrateKbps ?: 0
    val lossless = format in setOf("FLAC", "WAV", "ALAC", "APE", "AIFF")
    val hiRes = sampleRate >= 88_200 || bitDepth >= 24
    val highBitrate = bitrate >= 900

    return when {
        hiRes -> AudioQualityBadgeSpec(
            label = "Hi-Res",
            background = Color(0xFFFFC83D),
            foreground = Color(0xFF2B2100)
        )
        lossless || highBitrate -> AudioQualityBadgeSpec(
            label = "SQ",
            background = Color(0xFF22C7E8),
            foreground = Color(0xFF002632)
        )
        else -> null
    }
}

@Composable
fun AudioQualityBadge(
    song: Song?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val spec = audioQualityBadgeSpec(song) ?: return
    Box(
        modifier = modifier
            .background(spec.background, RoundedCornerShape(8.dp))
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 2.dp else 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = spec.label,
            color = spec.foreground,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black
        )
    }
}
