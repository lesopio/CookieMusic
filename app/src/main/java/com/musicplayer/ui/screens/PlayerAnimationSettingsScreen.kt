package com.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.data.FlowingBackgroundSettings
import com.musicplayer.data.PlayerAnimationPerformanceMode
import com.musicplayer.data.PlayerPageTheme
import com.musicplayer.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerAnimationSettingsScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val settings by viewModel.flowingBackgroundSettings.collectAsState()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text("播放器动画", fontWeight = FontWeight.SemiBold)
                        Text(
                            "动态水彩背景、逐词高亮和可读性保护",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        PlayerAnimationSettingsContent(
            settings = settings,
            onChange = viewModel::updateFlowingBackgroundSettings,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        )
    }
}

@Composable
fun PlayerAnimationSettingsContent(
    settings: FlowingBackgroundSettings,
    onChange: (FlowingBackgroundSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlayerPageThemeSelector(settings = settings, onChange = onChange)
        PresetCard(settings = settings, onChange = onChange)
        AnimationSwitchCard(
            title = "动态色背景",
            subtitle = if (settings.enabled) "使用专辑色、补色流光和柔和模糊背景" else "关闭后保留静态背景，降低视觉负载",
            checked = settings.enabled,
            onCheckedChange = { onChange(settings.copy(enabled = it)) }
        )
        AnimationIntensityCard(
            value = settings.intensity,
            enabled = settings.enabled,
            onValueChange = { onChange(settings.copy(intensity = it.coerceIn(0f, 1f))) }
        )
        AnimationSwitchCard(
            title = "低频脉冲",
            subtitle = "播放时让背景随音频能量轻微呼吸，低功耗模式会自动关闭",
            checked = settings.beatReactiveEnabled,
            onCheckedChange = { onChange(settings.copy(beatReactiveEnabled = it)) }
        )
        AnimationSwitchCard(
            title = "胶囊封面旋转",
            subtitle = "播放时让首页和歌词页底部胶囊里的封面缓慢旋转",
            checked = settings.capsuleCoverRotationEnabled,
            onCheckedChange = { onChange(settings.copy(capsuleCoverRotationEnabled = it)) }
        )
        AnimationSwitchCard(
            title = "高级切歌动画",
            subtitle = "切歌时让封面分层缩放淡入、歌曲信息错峰滑入；默认关闭",
            checked = settings.songChangeTransitionEnabled,
            onCheckedChange = { onChange(settings.copy(songChangeTransitionEnabled = it)) }
        )
        AnimationSwitchCard(
            title = "播放页三段歌词",
            subtitle = "显示上一句、当前逐字高亮句和下一句；关闭后只显示当前句",
            checked = settings.compactLyricsPreviewEnabled,
            onCheckedChange = { onChange(settings.copy(compactLyricsPreviewEnabled = it)) }
        )
    }
}

@Composable
fun PlayerPageThemeSelector(
    settings: FlowingBackgroundSettings,
    onChange: (FlowingBackgroundSettings) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("播放页主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("不改变专辑模糊取色背景和强调色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PlayerPageTheme.entries.forEach { theme ->
                val selected = settings.pageTheme == theme
                Row(
                    Modifier.fillMaxWidth().clickable { onChange(settings.copy(pageTheme = theme)) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(theme.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        Text(theme.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (selected) "已启用" else "选择", color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    settings: FlowingBackgroundSettings,
    onChange: (FlowingBackgroundSettings) -> Unit
) {
    val presets = listOf(
        PlayerAnimationPerformanceMode.Clear to "清晰",
        PlayerAnimationPerformanceMode.Balanced to "均衡",
        PlayerAnimationPerformanceMode.Atmosphere to "氛围",
        PlayerAnimationPerformanceMode.Low to "低功耗"
    )
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("动画预设", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            presets.forEach { (mode, label) ->
                val selected = settings.performanceMode == mode
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onChange(
                                settings.copy(
                                    performanceMode = mode,
                                    intensity = when (mode) {
                                        PlayerAnimationPerformanceMode.Clear -> 0.42f
                                        PlayerAnimationPerformanceMode.Balanced -> 0.58f
                                        PlayerAnimationPerformanceMode.Atmosphere, PlayerAnimationPerformanceMode.High -> 0.78f
                                        PlayerAnimationPerformanceMode.Low -> 0.30f
                                    },
                                    stageParticlesEnabled = false,
                                    beatReactiveEnabled = mode != PlayerAnimationPerformanceMode.Low,
                                    lyricParticlesEnabled = false
                                )
                            )
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        Text(
                            mode.description(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (selected) "已选" else "选择",
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimationIntensityCard(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("动态强度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "控制背景流光、模糊层和高亮底光的视觉幅度",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(value.coerceIn(0f, 1f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = value.coerceIn(0f, 1f),
                onValueChange = onValueChange,
                enabled = enabled,
                valueRange = 0f..1f
            )
        }
    }
}

@Composable
private fun AnimationSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun PlayerAnimationPerformanceMode.description(): String = when (this) {
    PlayerAnimationPerformanceMode.Clear -> "压暗背景，优先看清歌词"
    PlayerAnimationPerformanceMode.Balanced -> "保留流光，控制动效强度"
    PlayerAnimationPerformanceMode.Atmosphere -> "增强专辑色、流光和节拍氛围"
    PlayerAnimationPerformanceMode.High -> "最高动态效果"
    PlayerAnimationPerformanceMode.Low -> "关闭高成本动效，降低耗电"
}
