package com.musicplayer.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.data.AudioInfo
import com.musicplayer.data.EqualizerPreset
import com.musicplayer.data.EqualizerState
import com.musicplayer.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(400.dp, 800.dp))
) {
    val eqState by viewModel.equalizerState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    var presetExpanded by remember { mutableStateOf(false) }
    val isLandscape = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
    val context = LocalContext.current
    val effectsEnabled = !eqState.hifiModeEnabled

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("音效与音质") },
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
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AudioQualityCard(
                hifiEnabled = eqState.hifiModeEnabled,
                onHifiChange = viewModel::toggleHifiMode,
                audioInfo = playerState.currentSong?.audioInfo
            )

            EqualizerCard(
                eqState = eqState,
                presetExpanded = presetExpanded,
                onPresetExpandedChange = { if (effectsEnabled) presetExpanded = it },
                onPresetSelected = {
                    viewModel.setEqualizerPreset(it)
                    presetExpanded = false
                },
                onToggle = viewModel::toggleEqualizer,
                effectsEnabled = effectsEnabled,
                isLandscape = isLandscape,
                viewModel = viewModel
            )

            AdvancedEffectsCard(
                hifiLocked = eqState.hifiModeEnabled,
                bassEnabled = eqState.bassBoostEnabled,
                bassStrength = eqState.bassBoostStrength,
                virtualizerEnabled = eqState.virtualizerEnabled,
                virtualizerStrength = eqState.virtualizerStrength,
                loudnessEnabled = eqState.loudnessEnabled,
                loudnessGain = eqState.loudnessGain,
                onBassEnabled = { viewModel.updateBassBoost(it) },
                onBassStrength = { viewModel.updateBassBoost(true, it) },
                onVirtualizerEnabled = { viewModel.updateVirtualizer(it) },
                onVirtualizerStrength = { viewModel.updateVirtualizer(true, it) },
                onLoudnessEnabled = { viewModel.updateLoudness(it) },
                onLoudnessGain = { viewModel.updateLoudness(true, it) }
            )

            SystemAudioCard(
                onSoundSettings = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                onBluetoothSettings = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqualizerCard(
    eqState: EqualizerState,
    presetExpanded: Boolean,
    onPresetExpandedChange: (Boolean) -> Unit,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onToggle: (Boolean) -> Unit,
    effectsEnabled: Boolean,
    isLandscape: Boolean,
    viewModel: PlayerViewModel
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("均衡器", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (effectsEnabled) "5 段系统音效调节" else "HiFi 模式下已停用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = eqState.enabled, enabled = effectsEnabled, onCheckedChange = onToggle)
            }

            ExposedDropdownMenuBox(expanded = presetExpanded, onExpandedChange = onPresetExpandedChange) {
                OutlinedTextField(
                    value = eqState.preset,
                    onValueChange = {},
                    readOnly = true,
                    enabled = effectsEnabled,
                    label = { Text("预设") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = presetExpanded, onDismissRequest = { onPresetExpandedChange(false) }) {
                    EqualizerPreset.PRESETS.forEach { preset ->
                        DropdownMenuItem(text = { Text(preset.name) }, onClick = { onPresetSelected(preset) })
                    }
                }
            }

            if (isLandscape) {
                LandscapeEqualizerBands(eqState = eqState, viewModel = viewModel, enabled = effectsEnabled)
            } else {
                PortraitEqualizerBands(eqState = eqState, viewModel = viewModel, enabled = effectsEnabled)
            }
        }
    }
}

@Composable
private fun LandscapeEqualizerBands(
    eqState: EqualizerState,
    viewModel: PlayerViewModel,
    enabled: Boolean
) {
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        eqState.bandLevels.forEachIndexed { index, level ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(bandLabels.getOrElse(index) { "频段 ${index + 1}" }, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                Slider(value = level, onValueChange = { viewModel.setBandLevel(index, it) }, valueRange = -10f..10f, steps = 19, enabled = enabled)
                Text("${(level * 10).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PortraitEqualizerBands(
    eqState: EqualizerState,
    viewModel: PlayerViewModel,
    enabled: Boolean
) {
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    eqState.bandLevels.forEachIndexed { index, level ->
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(bandLabels.getOrElse(index) { "频段 ${index + 1}" }, style = MaterialTheme.typography.bodyMedium)
                Text("${(level * 10).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(value = level, onValueChange = { viewModel.setBandLevel(index, it) }, valueRange = -10f..10f, steps = 19, enabled = enabled)
        }
    }
}

@Composable
private fun AudioQualityCard(
    hifiEnabled: Boolean,
    onHifiChange: (Boolean) -> Unit,
    audioInfo: AudioInfo?
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("HiFi 模式", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "关闭应用内音效，尽量少处理地交给系统音频链。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = hifiEnabled, onCheckedChange = onHifiChange)
            }
            HorizontalDivider()
            InfoLine("格式", audioInfo?.format?.uppercase() ?: "未知")
            InfoLine("码率", audioInfo?.bitrateKbps?.takeIf { it > 0 }?.let { "${it}kbps" } ?: "未知")
            InfoLine(
                "采样率",
                audioInfo?.sampleRateHz?.takeIf { it > 0 }?.let { hz ->
                    if (hz % 1000 == 0) "${hz / 1000}kHz" else "${hz / 1000f}kHz"
                } ?: "未知"
            )
            InfoLine(
                "位深/声道",
                listOfNotNull(audioInfo?.bitDepth?.let { "${it}bit" }, audioInfo?.channels?.let { "${it}ch" })
                    .ifEmpty { listOf("未知") }
                    .joinToString(" · ")
            )
            InfoLine("文件大小", audioInfo?.fileSizeBytes?.let(::formatFileSize) ?: "未知")
        }
    }
}

@Composable
private fun AdvancedEffectsCard(
    hifiLocked: Boolean,
    bassEnabled: Boolean,
    bassStrength: Float,
    virtualizerEnabled: Boolean,
    virtualizerStrength: Float,
    loudnessEnabled: Boolean,
    loudnessGain: Float,
    onBassEnabled: (Boolean) -> Unit,
    onBassStrength: (Float) -> Unit,
    onVirtualizerEnabled: (Boolean) -> Unit,
    onVirtualizerStrength: (Float) -> Unit,
    onLoudnessEnabled: (Boolean) -> Unit,
    onLoudnessGain: (Float) -> Unit
) {
    val enabled = !hifiLocked
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("高级音效", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (hifiLocked) "HiFi 模式下已停用应用内增强。" else "依赖系统 AudioEffect，部分设备可能不可用。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            EffectSlider("低音增强", bassEnabled, bassStrength, enabled, onBassEnabled, onBassStrength)
            EffectSlider("虚拟环绕", virtualizerEnabled, virtualizerStrength, enabled, onVirtualizerEnabled, onVirtualizerStrength)
            EffectSlider("响度增强", loudnessEnabled, loudnessGain, enabled, onLoudnessEnabled, onLoudnessGain)
        }
    }
}

@Composable
private fun EffectSlider(
    title: String,
    checked: Boolean,
    value: Float,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
        }
        Slider(value = value, onValueChange = onValueChange, enabled = enabled && checked, valueRange = 0f..1f)
    }
}

@Composable
private fun SystemAudioCard(
    onSoundSettings: () -> Unit,
    onBluetoothSettings: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("系统增强与蓝牙编码", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "杜比全景声、设备 HiFi 芯片和 LDAC 由系统、耳机和蓝牙设置决定。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onSoundSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("声音设置")
                }
                OutlinedButton(onClick = onBluetoothSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("蓝牙设置")
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(
            label,
            modifier = Modifier.weight(0.38f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            value,
            modifier = Modifier.weight(0.62f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "未知"
    val mb = bytes / 1024f / 1024f
    return if (mb >= 1024f) String.format("%.2fGB", mb / 1024f) else String.format("%.1fMB", mb)
}
