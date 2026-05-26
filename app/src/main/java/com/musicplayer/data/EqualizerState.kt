package com.musicplayer.data

data class EqualizerState(
    val enabled: Boolean = false,
    val bandLevels: List<Float> = List(5) { 0f },
    val preset: String = "自定义",
    val hifiModeEnabled: Boolean = false,
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Float = 0.45f,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Float = 0.35f,
    val loudnessEnabled: Boolean = false,
    val loudnessGain: Float = 0.35f
)

data class EqualizerPreset(
    val name: String,
    val bandLevels: List<Float>
) {
    companion object {
        val PRESETS = listOf(
            EqualizerPreset("平直", listOf(0f, 0f, 0f, 0f, 0f)),
            EqualizerPreset("低音增强", listOf(6f, 3f, 0f, 0f, 0f)),
            EqualizerPreset("高音增强", listOf(0f, 0f, 0f, 3f, 6f)),
            EqualizerPreset("摇滚", listOf(5f, 3f, -1f, 3f, 5f)),
            EqualizerPreset("流行", listOf(-1f, 3f, 5f, 3f, -1f)),
            EqualizerPreset("爵士", listOf(3f, 1f, -2f, 1f, 3f)),
            EqualizerPreset("古典", listOf(4f, 2f, -1f, 2f, 4f))
        )
    }
}
