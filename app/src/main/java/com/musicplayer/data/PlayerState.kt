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
    val text: String
)
