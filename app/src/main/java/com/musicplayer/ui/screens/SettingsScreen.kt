package com.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    onImportManagerClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onStatusLyricsClick: () -> Unit,
    onColorSettingsClick: () -> Unit,
    onAnimationSettingsClick: () -> Unit,
    onBilingualIndexClick: () -> Unit,
    onScanClick: () -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val playerPageSettings by viewModel.flowingBackgroundSettings.collectAsState()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text("设置", fontWeight = FontWeight.SemiBold)
                        Text("音乐库、音效和显示", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            PlayerPageThemeSelector(settings = playerPageSettings, onChange = viewModel::updateFlowingBackgroundSettings)
            SettingsItem("导入管理", "导入歌曲、导入文件夹、管理已导入内容", Icons.Default.LibraryAdd, onImportManagerClick)
            SettingsItem("音效与音质", "均衡器、HiFi 模式和系统音效入口", Icons.Default.GraphicEq, onEqualizerClick)
            SettingsItem("播放历史", "查看最近播放、播放次数和常听歌曲", Icons.Default.History, onHistoryClick)
            SettingsItem("状态栏歌词", "悬浮窗歌词、位置、颜色和控制中心歌词", Icons.Default.Subtitles, onStatusLyricsClick)
            SettingsItem("双语分词索引", "按字符集字库自动识别原文和译文", Icons.Default.Translate, onBilingualIndexClick)
            SettingsItem("整体配色", "预设、取色板、最近颜色和跟随播放配色", Icons.Default.Palette, onColorSettingsClick)
            SettingsItem("播放器动画", "专辑模糊背景、柔和流光和歌词高亮", Icons.Default.Animation, onAnimationSettingsClick)
            SettingsItem("全盘扫描", "重新扫描系统媒体库", Icons.Default.Settings, onScanClick)
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
