package com.musicplayer.data

data class OverlayLyricsSettings(
    val enabled: Boolean = false,
    val hideInApp: Boolean = true,
    val mediaMetadataLyricsEnabled: Boolean = false,
    val textColorArgb: Long = 0xFFFFFFFF,
    val offsetTopDp: Float = 0f,
    val offsetBottomDp: Float = 0f,
    val offsetLeftDp: Float = 0f,
    val offsetRightDp: Float = 0f,
    val widthDp: Float = 320f,
    val fontSizeSp: Float = 14f,
    val pauseHideDelaySeconds: Float = 2f
)
