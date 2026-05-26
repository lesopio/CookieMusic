package com.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.data.AppColorSettings
import com.musicplayer.data.ThemeMode
import com.musicplayer.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSettingsScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val settings by viewModel.appColorSettings.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val effectiveColor = if (settings.followNowPlayingAccentEnabled) settings.lastNowPlayingAccentArgb else settings.seedColorArgb
    val presets = listOf(
        AppColorSettings.DEFAULT_SEED_COLOR,
        0xFF1E88E5,
        0xFF00897B,
        0xFFD81B60,
        0xFFF57C00,
        0xFF7CB342,
        0xFF546E7A
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text("整体配色", fontWeight = FontWeight.SemiBold)
                        Text("预设、取色板和跟随播放配色", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("显示模式", style = MaterialTheme.typography.titleMedium)
                    Text("可跟随系统，也可以固定日间或夜间。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.title) }
                            )
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("当前强调色", style = MaterialTheme.typography.titleMedium)
                            Text(if (settings.followNowPlayingAccentEnabled) "实验性：跟随当前歌曲封面色" else "来自自定义主色", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(effectiveColor.toInt())))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { viewModel.setCustomSeedColor(AppColorSettings.DEFAULT_SEED_COLOR) }) {
                            Text("恢复默认")
                        }
                        OutlinedButton(onClick = { viewModel.setFollowNowPlayingAccent(!settings.followNowPlayingAccentEnabled) }) {
                            Text(if (settings.followNowPlayingAccentEnabled) "关闭跟随" else "跟随播放")
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("预设颜色", style = MaterialTheme.typography.titleMedium)
                    ColorSwatchRow(presets) { viewModel.setCustomSeedColor(it) }
                }
            }

            if (settings.recentColorArgs.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("最近颜色", style = MaterialTheme.typography.titleMedium)
                        ColorSwatchRow(settings.recentColorArgs) { viewModel.setCustomSeedColor(it) }
                    }
                }
            }

            ColorPickerCard(
                title = "取色板",
                colorArgb = settings.seedColorArgb,
                presets = presets,
                onColorChange = { viewModel.setCustomSeedColor(it) }
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("实验性：跟随正在播放配色", style = MaterialTheme.typography.titleMedium)
                        Text("从专辑图取色，只替换按钮、进度条和选中态等强调色。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = settings.followNowPlayingAccentEnabled, onCheckedChange = viewModel::setFollowNowPlayingAccent)
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    colors: List<Long>,
    onClick: (Long) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(color.toInt()))
                    .clickable { onClick(color) }
            )
        }
    }
}
