package com.musicplayer.data

data class AppColorSettings(
    val seedColorArgb: Long = DEFAULT_SEED_COLOR,
    val recentColorArgs: List<Long> = emptyList(),
    val followNowPlayingAccentEnabled: Boolean = false,
    val lastNowPlayingAccentArgb: Long = DEFAULT_SEED_COLOR
) {
    companion object {
        const val DEFAULT_SEED_COLOR: Long = 0xFF6750A4
    }
}
