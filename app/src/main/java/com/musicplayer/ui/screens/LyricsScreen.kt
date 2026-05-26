package com.musicplayer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.musicplayer.data.LyricLine
import com.musicplayer.data.PlayerState
import com.musicplayer.data.Song
import com.musicplayer.ui.components.FlowingBlurBackground
import com.musicplayer.ui.components.MiniPlayer
import com.musicplayer.ui.components.rememberAlbumAccentColor
import com.musicplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LyricsScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val state by viewModel.playerState.collectAsState()
    val flowingBackgroundSettings by viewModel.flowingBackgroundSettings.collectAsState()
    val song = state.currentSong
    val accent by rememberAlbumAccentColor(song)
    val isLandscape = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
    val dragOffset = remember { Animatable(0f) }
    val dragScope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        dragScope.launch {
                            dragOffset.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                        }
                    }
                ) { _, dragAmount ->
                    dragScope.launch {
                        dragOffset.snapTo((dragOffset.value + dragAmount).coerceIn(0f, size.height * 0.42f))
                    }
                }
            }
    ) {
        FlowingBlurBackground(
            song = song,
            accent = accent,
            isPlaying = state.isPlaying,
            enabled = flowingBackgroundSettings.enabled,
            intensity = flowingBackgroundSettings.intensity
        )
        LyricsPageContent(
            state = state,
            viewModel = viewModel,
            song = song,
            accent = accent,
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
    onMiniPlayerClick: () -> Unit = onBackClick,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("歌词", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (song != null) {
                        Text(
                            song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.size(48.dp))
            }
            if (state.lyrics.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("当前歌曲没有歌词", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                FullLyricsList(
                    lines = state.lyrics,
                    currentIndex = state.currentLyricIndex,
                    isPlaying = state.isPlaying,
                    currentPosition = state.currentPosition,
                    duration = state.duration,
                    accent = accent,
                    isLandscape = isLandscape,
                    hasMiniPlayer = song != null,
                    onLineClick = viewModel::seekToLyric,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (song != null) {
            val progress = if (state.duration > 0) {
                state.currentPosition.toFloat() / state.duration.toFloat()
            } else {
                0f
            }
            MiniPlayer(
                song = song,
                isPlaying = state.isPlaying,
                progress = progress,
                onPlayPause = viewModel::playPause,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onClick = onMiniPlayerClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun FullLyricsList(
    lines: List<LyricLine>,
    currentIndex: Int,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    accent: Color,
    isLandscape: Boolean,
    hasMiniPlayer: Boolean,
    onLineClick: (LyricLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex, lines.size) {
        if (currentIndex >= 0 && !listState.isScrollInProgress) {
            val centerOffset = if (isLandscape) 3 else 4
            listState.animateScrollToItem((currentIndex - centerOffset).coerceAtLeast(0))
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = if (isLandscape) 72.dp else 28.dp,
            end = if (isLandscape) 72.dp else 28.dp,
            top = if (isLandscape) 42.dp else 96.dp,
            bottom = if (hasMiniPlayer) 176.dp else if (isLandscape) 42.dp else 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 18.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            val active = index == currentIndex.coerceIn(0, lines.lastIndex)
            val nextTime = lines.getOrNull(index + 1)?.timeMs
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
            } else {
                0f
            }
            if (active) {
                SmoothKaraokeText(
                    text = line.text,
                    progress = progress,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth().clickable { onLineClick(line) },
                    style = if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = line.text,
                    modifier = Modifier.fillMaxWidth().clickable { onLineClick(line) },
                    textAlign = TextAlign.Center,
                    style = if (isLandscape) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                    fontWeight = FontWeight.Normal
                )
            }
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
private fun SmoothKaraokeText(
    text: String,
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f)
    var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = style.copy(color = inactiveColor),
            fontWeight = fontWeight
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
                                clipRect(
                                    left = left,
                                    top = layout.getLineTop(line),
                                    right = right,
                                    bottom = layout.getLineBottom(line)
                                ) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                        }
                    }
                },
            textAlign = TextAlign.Center,
            style = style.copy(color = accent),
            fontWeight = fontWeight,
            onTextLayout = { textLayout = it }
        )
    }
}
