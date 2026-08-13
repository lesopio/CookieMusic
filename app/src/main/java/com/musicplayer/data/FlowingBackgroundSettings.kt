package com.musicplayer.data

data class FlowingBackgroundSettings(
    val enabled: Boolean = true,
    val intensity: Float = 0.72f,
    val stageParticlesEnabled: Boolean = true,
    val beatReactiveEnabled: Boolean = true,
    val coverMotionEnabled: Boolean = false,
    val lyricParticlesEnabled: Boolean = true,
    val progressParticlesEnabled: Boolean = false,
    val capsuleCoverRotationEnabled: Boolean = true,
    val compactLyricsPreviewEnabled: Boolean = true,
    val songChangeTransitionEnabled: Boolean = false,
    val visualizerSensitivity: Float = 0.72f,
    val performanceMode: PlayerAnimationPerformanceMode = PlayerAnimationPerformanceMode.Atmosphere,
    val pageTheme: PlayerPageTheme = PlayerPageTheme.Current
)

enum class PlayerPageTheme(val title: String, val subtitle: String) {
    Current("当前", "保留现有的沉浸式播放页布局"),
    Minimal("极简", "更克制的留白、信息层级和控制区")
}

enum class PlayerAnimationPerformanceMode {
    Clear,
    Atmosphere,
    High,
    Balanced,
    Low
}
