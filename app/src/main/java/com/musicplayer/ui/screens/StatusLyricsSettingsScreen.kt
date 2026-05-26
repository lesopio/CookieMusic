package com.musicplayer.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Subtitles
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.musicplayer.data.OverlayLyricsSettings
import com.musicplayer.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusLyricsSettingsScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val context = LocalContext.current
    val settings by viewModel.overlayLyricsSettings.collectAsState()
    var overlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) overlayPermission = Settings.canDrawOverlays(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun update(next: OverlayLyricsSettings) = viewModel.updateOverlayLyricsSettings(next)
    fun requestOverlayPermission() {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text("状态栏歌词", fontWeight = FontWeight.SemiBold)
                        Text("悬浮窗歌词与控制中心歌词", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("悬浮窗歌词", style = MaterialTheme.typography.titleMedium)
                            Text(if (overlayPermission) "后台顶部显示当前歌词" else "需要授予悬浮窗权限", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = settings.enabled && overlayPermission,
                            onCheckedChange = { checked ->
                                if (checked && !overlayPermission) requestOverlayPermission() else update(settings.copy(enabled = checked))
                            }
                        )
                    }
                    SwitchRow("应用内隐藏", "应用在前台时不显示悬浮窗", settings.hideInApp) {
                        update(settings.copy(hideInApp = it))
                    }
                    SwitchRow("控制中心显示歌词", "媒体标题显示当前歌词，作者显示歌名和歌手", settings.mediaMetadataLyricsEnabled) {
                        update(settings.copy(mediaMetadataLyricsEnabled = it))
                    }
                }
            }

            ColorPickerCard(
                title = "歌词文字颜色",
                colorArgb = settings.textColorArgb,
                presets = listOf(0xFFFFFFFF, 0xFFBFC7FF, 0xFFFFE082, 0xFF80CBC4, 0xFFFFAB91, 0xFFFF80AB),
                onColorChange = { update(settings.copy(textColorArgb = it)) }
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SliderSetting("上偏移", settings.offsetTopDp, 0f..120f, "dp") { update(settings.copy(offsetTopDp = it)) }
                    SliderSetting("下偏移", settings.offsetBottomDp, 0f..120f, "dp") { update(settings.copy(offsetBottomDp = it)) }
                    SliderSetting("左偏移", settings.offsetLeftDp, 0f..120f, "dp") { update(settings.copy(offsetLeftDp = it)) }
                    SliderSetting("右偏移", settings.offsetRightDp, 0f..120f, "dp") { update(settings.copy(offsetRightDp = it)) }
                    SliderSetting("宽度", settings.widthDp, 160f..420f, "dp") { update(settings.copy(widthDp = it)) }
                    SliderSetting("字体大小", settings.fontSizeSp, 10f..24f, "sp") { update(settings.copy(fontSizeSp = it)) }
                    SliderSetting("暂停隐藏延时", settings.pauseHideDelaySeconds, 0f..10f, "秒") { update(settings.copy(pauseHideDelaySeconds = it)) }
                }
            }
        }
    }
}

@Composable
internal fun ColorPickerCard(
    title: String,
    colorArgb: Long,
    presets: List<Long>,
    onColorChange: (Long) -> Unit
) {
    val initialHsv = remember(colorArgb) { FloatArray(3).also { android.graphics.Color.colorToHSV(colorArgb.toInt(), it) } }
    var hue by remember(colorArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(colorArgb) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(colorArgb) { mutableFloatStateOf(initialHsv[2]) }

    fun commit() {
        onColorChange(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)).toLong() and 0xFFFFFFFFL)
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(colorArgb.toInt()))
                )
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(preset.toInt()))
                            .clickable { onColorChange(preset) }
                    )
                }
            }
            SliderSetting("色相", hue, 0f..360f, "") {
                hue = it
                commit()
            }
            SliderSetting("饱和度", saturation, 0f..1f, "%") {
                saturation = it
                commit()
            }
            SliderSetting("亮度", value, 0f..1f, "%") {
                value = it
                commit()
            }
        }
    }
}

@Composable
internal fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            val displayValue = if (unit == "%") (value * 100).toInt() else value.toInt()
            Text("$displayValue$unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
