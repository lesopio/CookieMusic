package com.musicplayer.data

data class AudioVisualizationState(
    val bass: Float = 0f,
    val mid: Float = 0f,
    val treble: Float = 0f,
    val energy: Float = 0f,
    val beatPulse: Float = 0f,
    val active: Boolean = false,
    val usingFallback: Boolean = true
)
