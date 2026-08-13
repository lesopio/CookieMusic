package com.musicplayer.ui.components

import com.musicplayer.data.LyricLine
import com.musicplayer.data.splitBilingualText

data class LyricDisplayText(
    val primary: String,
    val secondary: String? = null,
    val romanization: String? = null
)

fun LyricLine.toDisplayText(): LyricDisplayText {
    if (!translation.isNullOrBlank() || !romanization.isNullOrBlank()) {
        return LyricDisplayText(
            primary = primaryText.trim(),
            secondary = translation?.trim()?.takeIf { it.isNotBlank() },
            romanization = romanization?.trim()?.takeIf { it.isNotBlank() }
        )
    }
    return splitLyricDisplayText(text)
}

fun splitLyricDisplayText(text: String): LyricDisplayText {
    val normalized = text.trim()
    if (normalized.isBlank()) return LyricDisplayText("")

    splitBilingualText(normalized)?.let { (primary, secondary) ->
        return LyricDisplayText(primary, secondary)
    }

    return LyricDisplayText(normalized)
}
