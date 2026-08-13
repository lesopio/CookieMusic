package com.musicplayer.data

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val playbackMode: PlaybackMode = PlaybackMode.Sequential,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemainingMs: Long = 0L,
    val lyrics: List<LyricLine> = emptyList(),
    val currentLyricIndex: Int = -1
)

enum class PlaybackMode {
    Sequential, RepeatOne, Shuffle
}

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val endTimeMs: Long? = null,
    val translation: String? = null,
    val romanization: String? = null,
    val words: List<LyricWord> = emptyList()
) {
    val primaryText: String get() = text
    val displayText: String get() = listOfNotNull(text, translation, romanization).joinToString("\n")
}

data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)
