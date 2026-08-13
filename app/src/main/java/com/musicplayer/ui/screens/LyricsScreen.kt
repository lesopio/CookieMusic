package com.musicplayer.ui.screens

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import com.musicplayer.data.AudioVisualizationState
import com.musicplayer.data.FlowingBackgroundSettings
import com.musicplayer.data.LyricLine
import com.musicplayer.data.PlayerState
import com.musicplayer.data.Song
import com.musicplayer.ui.components.AlbumArtImage
import com.musicplayer.ui.components.FlowingBlurBackground
import com.musicplayer.ui.components.MiniPlayer
import com.musicplayer.ui.components.StructuredLyricText
import com.musicplayer.ui.components.rememberAlbumAccentColor
import com.musicplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LyricsScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    darkTheme: Boolean = false,
    windowSizeClass: WindowSizeClass
) {
    val state by viewModel.playerState.collectAsState()
    val settings by viewModel.flowingBackgroundSettings.collectAsState()
    val visualizer by viewModel.audioVisualizationState.collectAsState()
    val song = state.currentSong
    val accent by rememberAlbumAccentColor(song)
    val isLandscape = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
    val dragOffset = remember { Animatable(0f) }
    val dragScope = rememberCoroutineScope()
    val palette = rememberPlayerVisualPalette(accent, darkTheme)
    LyricsNavigationBarEffect(palette.backgroundBottom, darkTheme)

    Box(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        dragScope.launch {
                            if (dragOffset.value > size.height * 0.16f) {
                                dragOffset.animateTo(size.height.toFloat(), tween(180, easing = FastOutSlowInEasing))
                                onBackClick()
                                dragOffset.snapTo(0f)
                            } else {
                                dragOffset.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                            }
                        }
                    },
                    onDragCancel = {
                        dragScope.launch { dragOffset.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
                    }
                ) { _, dragAmount ->
                    dragScope.launch {
                        dragOffset.snapTo((dragOffset.value + dragAmount).coerceIn(0f, size.height * 0.42f))
                    }
                }
            }
    ) {
        LyricsPageContent(
            state = state,
            viewModel = viewModel,
            song = song,
            accent = accent,
            darkTheme = darkTheme,
            animationSettings = settings,
            visualizerState = visualizer,
            isLandscape = isLandscape,
            onBackClick = onBackClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun LyricsPageContent(
    state: PlayerState,
    viewModel: PlayerViewModel,
    song: Song?,
    accent: Color,
    isLandscape: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
    animationSettings: FlowingBackgroundSettings = FlowingBackgroundSettings(),
    visualizerState: AudioVisualizationState = AudioVisualizationState(),
    drawBackground: Boolean = true,
    onMiniPlayerClick: () -> Unit = onBackClick
) {
    val palette = rememberPlayerVisualPalette(accent, darkTheme)
    val density = LocalDensity.current
    var miniDockHeightPx by remember { mutableIntStateOf(0) }
    val lyricsBottomPadding = if (song != null) {
        with(density) { miniDockHeightPx.toDp() } + 24.dp
    } else {
        112.dp
    }
    Box(modifier) {
        if (drawBackground) {
            LyricsBackgroundLayer(
                song = song,
                accent = accent,
                palette = palette,
                isPlaying = state.isPlaying,
                animationSettings = animationSettings,
                visualizerState = visualizerState,
                darkTheme = darkTheme
            )
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            LyricsTopBar(song = song, palette = palette, onBackClick = onBackClick)
            if (state.lyrics.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("当前歌曲没有歌词", color = palette.secondaryText)
                }
            } else {
                FocusLyricsList(
                    lines = state.lyrics,
                    currentIndex = state.currentLyricIndex,
                    isPlaying = state.isPlaying,
                    currentPosition = state.currentPosition,
                    duration = state.duration,
                    accent = accent,
                    palette = palette,
                    visualizerState = visualizerState,
                    isLandscape = isLandscape,
                    bottomPadding = lyricsBottomPadding,
                    onLineClick = viewModel::seekToLyric,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (song != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                palette.backgroundBottom.copy(alpha = if (palette.dark) 0.78f else 0.82f)
                            )
                        )
                    )
            )
            LyricsMiniDock(
                song = song,
                isPlaying = state.isPlaying,
                progress = if (state.duration > 0) state.currentPosition.toFloat() / state.duration.toFloat() else 0f,
                accent = accent,
                palette = palette,
                onPlayPause = viewModel::playPause,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onClick = onMiniPlayerClick,
                rotateCover = animationSettings.capsuleCoverRotationEnabled && state.isPlaying,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { miniDockHeightPx = it.height }
            )
        }
    }
}

@Composable
fun LyricsBackgroundLayer(
    song: Song?,
    accent: Color,
    palette: PlayerVisualPalette,
    isPlaying: Boolean,
    animationSettings: FlowingBackgroundSettings,
    visualizerState: AudioVisualizationState,
    darkTheme: Boolean
) {
    FlowingBlurBackground(
        song = song,
        accent = accent,
        isPlaying = isPlaying,
        enabled = animationSettings.enabled,
        intensity = animationSettings.intensity,
        stageParticlesEnabled = false,
        beatReactiveEnabled = animationSettings.beatReactiveEnabled,
        visualizerState = visualizerState,
        darkTheme = darkTheme
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.backgroundTop.copy(alpha = if (darkTheme) 0.48f else 0.28f),
                        Color.Transparent,
                        palette.backgroundBottom.copy(alpha = if (darkTheme) 0.66f else 0.78f)
                    )
                )
            )
    )
}

@Composable
private fun LyricsTopBar(song: Song?, palette: PlayerVisualPalette, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = palette.icon)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("歌词", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.primaryText)
            if (song != null) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, color = palette.secondaryText)
            }
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun FocusLyricsList(
    lines: List<LyricLine>,
    currentIndex: Int,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    accent: Color,
    palette: PlayerVisualPalette,
    visualizerState: AudioVisualizationState,
    isLandscape: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onLineClick: (LyricLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val displayIndexes = remember(lines) {
        lines.indices.filter { index -> index == 0 || lines[index].timeMs != lines[index - 1].timeMs }
    }
    val safeIndex = currentIndex.coerceIn(0, lines.lastIndex)
    val currentDisplayIndex = displayIndexes.indexOfLast { it <= safeIndex }.coerceAtLeast(0)
    LaunchedEffect(currentDisplayIndex, displayIndexes.size) {
        if (currentIndex >= 0 && !listState.isScrollInProgress) {
            val centerOffset = if (isLandscape) 2 else 2
            listState.animateScrollToItem((currentDisplayIndex - centerOffset).coerceAtLeast(0))
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = if (isLandscape) 84.dp else 30.dp,
            end = if (isLandscape) 84.dp else 30.dp,
            top = if (isLandscape) 64.dp else 160.dp,
            bottom = bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 18.dp else 28.dp)
    ) {
        itemsIndexed(displayIndexes) { displayIndex, sourceIndex ->
            val line = lines[sourceIndex]
            val active = line.timeMs == lines.getOrNull(safeIndex)?.timeMs
            val distance = kotlin.math.abs(displayIndex - currentDisplayIndex)
            val nextTime = lines.drop(sourceIndex + 1).firstOrNull { it.timeMs > line.timeMs }?.timeMs
                ?: duration.takeIf { it > line.timeMs }
                ?: (line.timeMs + 4_000L)
            val progress = if (active) {
                rememberSmoothLyricProgress(
                    lineKey = "${line.timeMs}:${line.text}",
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    startTime = line.timeMs,
                    endTime = nextTime
                )
            } else 0f
            LyricsLineRow(
                line = line,
                active = active,
                distance = distance,
                progress = progress,
                currentPosition = currentPosition,
                accent = accent,
                palette = palette,
                visualizerState = visualizerState,
                isLandscape = isLandscape,
                onClick = { onLineClick(line) }
            )
        }
    }
}

@Composable
private fun LyricsLineRow(
    line: LyricLine,
    active: Boolean,
    distance: Int,
    progress: Float,
    currentPosition: Long,
    accent: Color,
    palette: PlayerVisualPalette,
    visualizerState: AudioVisualizationState,
    isLandscape: Boolean,
    onClick: () -> Unit
) {
    val focusScale by animateFloatAsState(
        targetValue = if (active) 1f else 0.92f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "lyric_focus_scale"
    )
    val alpha = when {
        active -> 1f
        distance == 1 -> 0.48f
        distance == 2 -> 0.28f
        else -> 0.16f
    }
    val style = when {
        active && isLandscape -> MaterialTheme.typography.headlineMedium
        active -> MaterialTheme.typography.headlineSmall
        isLandscape -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleLarge
    }
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = focusScale, scaleY = focusScale)
            .clickable(onClick = onClick)
            .padding(vertical = if (active) 10.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            StructuredLyricText(
                line = line,
                active = true,
                currentPosition = currentPosition,
                fallbackProgress = progress,
                accent = accent,
                primaryColor = palette.primaryText.copy(alpha = if (palette.dark) 0.74f else 0.76f),
                secondaryColor = palette.secondaryText.copy(alpha = if (palette.dark) 0.82f else 0.78f),
                darkTheme = palette.dark,
                visualizerState = visualizerState,
                modifier = Modifier.fillMaxWidth(),
                primaryStyle = style,
                secondaryStyle = MaterialTheme.typography.titleMedium,
                activeBackground = true
            )
        } else {
            StructuredLyricText(
                line = line,
                active = false,
                currentPosition = 0L,
                fallbackProgress = 0f,
                accent = accent,
                primaryColor = palette.secondaryText.copy(alpha = alpha.coerceAtLeast(0.22f)),
                secondaryColor = palette.secondaryText.copy(alpha = (alpha + 0.08f).coerceAtMost(0.56f)),
                darkTheme = palette.dark,
                visualizerState = visualizerState,
                modifier = Modifier.fillMaxWidth(),
                primaryStyle = style,
                secondaryStyle = MaterialTheme.typography.bodyMedium,
                activeBackground = false
            )
        }
    }
}

@Composable
private fun LyricsMiniDock(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    accent: Color,
    palette: PlayerVisualPalette,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    rotateCover: Boolean,
    modifier: Modifier = Modifier
) {
    MiniPlayer(
        song = song,
        isPlaying = isPlaying,
        progress = progress,
        onPlayPause = onPlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onClick = null,
        accent = accent,
        rotateCover = rotateCover,
        modifier = modifier.navigationBarsPadding()
    )
    return

    val glass = if (palette.dark) {
        palette.controlGlass.copy(alpha = 0.36f)
    } else {
        accent.copy(alpha = 0.12f)
    }
    val stroke = palette.controlStroke.copy(alpha = if (palette.dark) 0.30f else 0.18f)
    val shape = RoundedCornerShape(34.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(glass)
            .border(BorderStroke(1.dp, stroke), shape)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AlbumArtImage(
                    song = song,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)),
                    fallbackModifier = Modifier.size(24.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = palette.primaryText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        song.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = palette.secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", tint = palette.icon)
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(accent)
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", tint = Color.White)
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一首", tint = palette.icon)
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(10.dp)),
                color = accent,
                trackColor = palette.progressTrack
            )
        }
    }
}

@Composable
private fun rememberSmoothLyricProgress(
    lineKey: String,
    isPlaying: Boolean,
    currentPosition: Long,
    startTime: Long,
    endTime: Long
): Float {
    val animatable = remember(lineKey) { Animatable(0f) }
    LaunchedEffect(lineKey, isPlaying, startTime, endTime) {
        val duration = (endTime - startTime).coerceAtLeast(1L)
        val current = ((currentPosition - startTime).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        animatable.snapTo(current)
        if (isPlaying && current < 1f) {
            val remaining = (endTime - currentPosition).coerceAtLeast(80L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            animatable.animateTo(1f, animationSpec = tween(durationMillis = remaining, easing = LinearEasing))
        }
    }
    return animatable.value
}

@Composable
private fun LyricsNavigationBarEffect(color: Color, darkTheme: Boolean) {
    val view = LocalView.current
    DisposableEffect(color, darkTheme, view) {
        val window = (view.context as? ComponentActivity)?.window
        if (window != null) {
            window.navigationBarColor = color.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
        onDispose {
            window?.navigationBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && window != null) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
}

@Composable
private fun SmoothKaraokeText(
    text: String,
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    inactiveColor: Color,
    glowAlpha: Float,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight
) {
    var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = glowAlpha),
            textAlign = TextAlign.Center,
            style = style.copy(color = accent),
            fontWeight = fontWeight,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = style.copy(color = inactiveColor),
            fontWeight = fontWeight,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    val layout = textLayout
                    if (layout == null || layout.lineCount <= 1) {
                        clipRect(right = size.width * progress.coerceIn(0f, 1f)) {
                            this@drawWithContent.drawContent()
                        }
                    } else {
                        val lineProgress = progress.coerceIn(0f, 1f) * layout.lineCount
                        for (line in 0 until layout.lineCount) {
                            val part = (lineProgress - line).coerceIn(0f, 1f)
                            if (part > 0f) {
                                val left = layout.getLineLeft(line)
                                val right = left + (layout.getLineRight(line) - left) * part
                                clipRect(left = left, top = layout.getLineTop(line), right = right, bottom = layout.getLineBottom(line)) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                        }
                    }
                },
            textAlign = TextAlign.Center,
            style = style.copy(color = accent),
            fontWeight = fontWeight,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayout = it }
        )
    }
}
