package com.musicplayer.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.musicplayer.data.LyricLine
import com.musicplayer.data.PlaybackMode
import com.musicplayer.data.Song
import com.musicplayer.ui.components.AlbumArtImage
import com.musicplayer.ui.components.FlowingBlurBackground
import com.musicplayer.ui.components.rememberAlbumAccentColor
import com.musicplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(400.dp, 800.dp))
) {
    val playerState by viewModel.playerState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val flowingBackgroundSettings by viewModel.flowingBackgroundSettings.collectAsState()
    val song = playerState.currentSong
    val accent by rememberAlbumAccentColor(song)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var sidePanelOpen by remember { mutableStateOf(false) }
    var showingLyricsPage by remember { mutableStateOf(false) }
    var pageSize by remember { mutableStateOf(IntSize.Zero) }
    var pageOffsetPx by remember { mutableFloatStateOf(0f) }
    val pageSettle = remember { Animatable(0f) }
    val pageScope = rememberCoroutineScope()

    fun settlePageTo(target: Float) {
        pageScope.launch {
            pageSettle.stop()
            pageSettle.snapTo(pageOffsetPx)
            pageSettle.animateTo(
                target,
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) {
                pageOffsetPx = value
            }
        }
    }

    fun animateToPlayer() {
        showingLyricsPage = false
        settlePageTo(0f)
    }

    fun animateToLyrics() {
        val height = pageSize.height.toFloat()
        if (height <= 0f || playerState.lyrics.isEmpty()) return
        showingLyricsPage = true
        settlePageTo(-height)
    }

    LaunchedEffect(pageSize.height) {
        val height = pageSize.height.toFloat()
        if (height > 0f) {
            pageSettle.stop()
            pageOffsetPx = if (showingLyricsPage) -height else 0f
        }
    }

    LaunchedEffect(playerState.lyrics.isEmpty()) {
        if (playerState.lyrics.isEmpty() && showingLyricsPage) {
            showingLyricsPage = false
            settlePageTo(0f)
        }
    }

    BackHandler(enabled = showingLyricsPage) {
        animateToPlayer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { pageSize = it }
            .pointerInput(playerState.lyrics.size, showingLyricsPage, pageSize.height) {
                detectVerticalDragGestures(
                    onDragStart = {
                        pageScope.launch { pageSettle.stop() }
                    },
                    onDragEnd = {
                        val height = pageSize.height.toFloat().takeIf { it > 0f } ?: size.height.toFloat()
                        val targetLyrics = if (showingLyricsPage) {
                            pageOffsetPx < -height * 0.84f
                        } else {
                            playerState.lyrics.isNotEmpty() && pageOffsetPx < -height * 0.16f
                        }
                        showingLyricsPage = targetLyrics
                        settlePageTo(if (targetLyrics) -height else 0f)
                    },
                    onDragCancel = {
                        val height = pageSize.height.toFloat().takeIf { it > 0f } ?: size.height.toFloat()
                        settlePageTo(if (showingLyricsPage) -height else 0f)
                    }
                ) { _, dragAmount ->
                    val height = pageSize.height.toFloat().takeIf { it > 0f } ?: size.height.toFloat()
                    if (height <= 0f || (playerState.lyrics.isEmpty() && !showingLyricsPage)) return@detectVerticalDragGestures
                    pageOffsetPx = (pageOffsetPx + dragAmount).coerceIn(-height, 0f)
                }
            }
    ) {
        FlowingBlurBackground(
            song = song,
            accent = accent,
            isPlaying = playerState.isPlaying,
            enabled = flowingBackgroundSettings.enabled,
            intensity = flowingBackgroundSettings.intensity,
            subduedArtwork = isLandscape
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, pageOffsetPx.roundToInt()) }
        ) {
            PlayerPageContent(
                song = song,
                playerState = playerState,
                favorites = favorites,
                viewModel = viewModel,
                accent = accent,
                isLandscape = isLandscape,
                onBackClick = onBackClick,
                onLyricsClick = { animateToLyrics() },
                onSleepClick = onSleepClick,
                onEqualizerClick = onEqualizerClick,
                onHistoryClick = onHistoryClick,
                onSidePanelClick = { sidePanelOpen = true },
                modifier = Modifier.fillMaxSize()
            )
            LyricsPageContent(
                state = playerState,
                viewModel = viewModel,
                song = song,
                accent = accent,
                isLandscape = isLandscape,
                onBackClick = { animateToPlayer() },
                onMiniPlayerClick = { animateToPlayer() },
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, pageSize.height) }
            )
        }
        PlayerSidePanel(
            visible = sidePanelOpen,
            state = playerState,
            accent = accent,
            viewModel = viewModel,
            onClose = { sidePanelOpen = false }
        )
    }
}

@Composable
private fun PlayerPageContent(
    song: Song?,
    playerState: com.musicplayer.data.PlayerState,
    favorites: Set<Long>,
    viewModel: PlayerViewModel,
    accent: Color,
    isLandscape: Boolean,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSidePanelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.statusBarsPadding()) {
        PlayerTopBar(
            song = song,
            onBackClick = onBackClick,
            onSidePanelClick = onSidePanelClick
        )
        if (song == null) {
            EmptyPlayer()
        } else if (isLandscape) {
            LandscapePlayer(
                song = song,
                state = playerState,
                viewModel = viewModel,
                accent = accent,
                favorites = favorites,
                onLyricsClick = onLyricsClick,
                onSleepClick = onSleepClick,
                onEqualizerClick = onEqualizerClick,
                onHistoryClick = onHistoryClick,
                onMoreClick = onSidePanelClick
            )
        } else {
            PortraitPlayer(
                song = song,
                state = playerState,
                viewModel = viewModel,
                accent = accent,
                favorites = favorites,
                onLyricsClick = onLyricsClick,
                onSleepClick = onSleepClick,
                onEqualizerClick = onEqualizerClick,
                onHistoryClick = onHistoryClick,
                onMoreClick = onSidePanelClick
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    song: Song?,
    onBackClick: () -> Unit,
    onSidePanelClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("正在播放", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (song != null) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onSidePanelClick) {
            Icon(Icons.Default.Menu, contentDescription = "侧栏")
        }
    }
}

@Composable
private fun PortraitPlayer(
    song: Song,
    state: com.musicplayer.data.PlayerState,
    viewModel: PlayerViewModel,
    accent: Color,
    favorites: Set<Long>,
    onLyricsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlbumArt(song = song, isPlaying = state.isPlaying, modifier = Modifier.fillMaxWidth(0.78f))
        Spacer(Modifier.height(18.dp))
        SongInfo(song)
        Spacer(Modifier.height(18.dp))
        CurrentLyricLine(state, accent, Modifier.fillMaxWidth(), onLyricsClick)
        Spacer(Modifier.weight(1f))
        ProgressSlider(state.currentPosition, state.duration, accent, viewModel::seekTo)
        PlaybackControls(
            isPlaying = state.isPlaying,
            playbackMode = state.playbackMode,
            isFavorite = favorites.contains(song.id),
            accent = accent,
            onPlayPause = viewModel::playPause,
            onPrevious = viewModel::previous,
            onNext = viewModel::next,
            onPlaybackModeToggle = viewModel::togglePlaybackMode,
            onFavoriteToggle = { viewModel.toggleFavorite(song.id) }
        )
        LandscapeFeatureButtons(
            playbackMode = state.playbackMode,
            accent = accent,
            onHistoryClick = onHistoryClick,
            onSleepClick = onSleepClick,
            onEqualizerClick = onEqualizerClick,
            onQueueClick = onMoreClick,
            onMoreClick = onMoreClick
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LandscapePlayer(
    song: Song,
    state: com.musicplayer.data.PlayerState,
    viewModel: PlayerViewModel,
    accent: Color,
    favorites: Set<Long>,
    onLyricsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 32.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(0.44f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AlbumArt(song = song, isPlaying = state.isPlaying, modifier = Modifier.size(250.dp))
            Spacer(Modifier.height(14.dp))
            CurrentLyricLine(state, accent, Modifier.fillMaxWidth(), onLyricsClick)
        }
        Column(Modifier.weight(0.56f).fillMaxHeight().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Spacer(Modifier.weight(1f))
            SongInfo(song)
            Spacer(Modifier.height(8.dp))
            ProgressSlider(state.currentPosition, state.duration, accent, viewModel::seekTo)
            PlaybackControls(
                isPlaying = state.isPlaying,
                playbackMode = state.playbackMode,
                isFavorite = favorites.contains(song.id),
                accent = accent,
                onPlayPause = viewModel::playPause,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onPlaybackModeToggle = viewModel::togglePlaybackMode,
                onFavoriteToggle = { viewModel.toggleFavorite(song.id) }
            )
            LandscapeFeatureButtons(
                playbackMode = state.playbackMode,
                accent = accent,
                onHistoryClick = onHistoryClick,
                onSleepClick = onSleepClick,
                onEqualizerClick = onEqualizerClick,
                onQueueClick = onMoreClick,
                onMoreClick = onMoreClick
            )
            Spacer(Modifier.weight(0.8f))
        }
    }
}

@Composable
private fun AlbumArt(song: Song, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "album_rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 2.2f else 0f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), AnimationRepeatMode.Reverse),
        label = "album_breathe"
    )
    Card(
        modifier = modifier.aspectRatio(1f).rotate(rotation),
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
    ) {
        AlbumArtImage(
            song = song,
            modifier = Modifier.fillMaxSize(),
            fallbackModifier = Modifier.size(92.dp)
        )
    }
}

@Composable
private fun SongInfo(song: Song) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(song.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CurrentLyricLine(
    state: com.musicplayer.data.PlayerState,
    accent: Color,
    modifier: Modifier = Modifier,
    onLyricsClick: () -> Unit
) {
    if (state.lyrics.isEmpty()) return
    val index = state.currentLyricIndex.coerceIn(0, state.lyrics.lastIndex)
    val line = state.lyrics[index]
    val nextTime = state.lyrics.getOrNull(index + 1)?.timeMs ?: state.duration.takeIf { it > line.timeMs } ?: (line.timeMs + 4_000L)
    val progress = rememberSmoothLyricProgress(
        lineKey = "${line.timeMs}:${line.text}",
        isPlaying = state.isPlaying,
        currentPosition = state.currentPosition,
        startTime = line.timeMs,
        endTime = nextTime
    )
    SmoothKaraokeText(
        text = line.text,
        progress = progress,
        accent = accent,
        modifier = modifier.clickable(onClick = onLyricsClick).padding(horizontal = 6.dp, vertical = 10.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
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
            fontWeight = fontWeight,
            maxLines = 2,
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayout = it }
        )
    }
}

@Composable
private fun LyricsPanel(lines: List<LyricLine>, currentIndex: Int, accent: Color, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex, lines.size) {
        if (currentIndex >= 0) listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        tonalElevation = 2.dp
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 24.dp, horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                val active = index == currentIndex
                Text(
                    text = line.text,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = if (active) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ProgressSlider(currentPosition: Long, duration: Long, accent: Color, onSeek: (Long) -> Unit) {
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    var seekPreview by remember { mutableFloatStateOf(-1f) }
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    val displayProgress = if (seekPreview >= 0f) seekPreview else progress
    fun seekToOffset(x: Float) {
        if (duration <= 0 || trackSize.width <= 0) return
        val value = (x / trackSize.width).coerceIn(0f, 1f)
        seekPreview = value
        onSeek((value * duration).toLong())
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .onSizeChanged { trackSize = it }
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        seekToOffset(offset.x)
                        seekPreview = -1f
                    }
                }
                .pointerInput(duration) {
                    detectDragGestures(
                        onDragEnd = { seekPreview = -1f },
                        onDragCancel = { seekPreview = -1f }
                    ) { change, _ ->
                        seekToOffset(change.position.x)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.18f))
            )
            Box(
                Modifier
                    .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(if (seekPreview >= 0f) (seekPreview * duration).toLong() else currentPosition), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    playbackMode: PlaybackMode,
    isFavorite: Boolean,
    accent: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlaybackModeToggle: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPlaybackModeToggle) {
            Icon(
                imageVector = playbackMode.icon(),
                contentDescription = playbackMode.label(),
                tint = if (playbackMode == PlaybackMode.Sequential) MaterialTheme.colorScheme.onSurfaceVariant else accent
            )
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(34.dp))
        }
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(76.dp).clip(CircleShape).background(accent)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(42.dp),
                tint = Color.White
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.SkipNext, contentDescription = "下一首", modifier = Modifier.size(34.dp))
        }
        IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LandscapeFeatureButtons(
    playbackMode: PlaybackMode,
    accent: Color,
    onHistoryClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onHistoryClick) {
            Icon(Icons.Default.History, contentDescription = "播放历史")
        }
        IconButton(onClick = onSleepClick) {
            Icon(Icons.Default.Bedtime, contentDescription = "睡眠")
        }
        IconButton(onClick = onEqualizerClick) {
            Icon(Icons.Default.GraphicEq, contentDescription = "音效")
        }
        IconButton(onClick = onQueueClick) {
            Icon(Icons.Default.QueueMusic, contentDescription = "队列")
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多")
        }
    }
}

private fun PlaybackMode.icon() = when (this) {
    PlaybackMode.Sequential -> Icons.Default.Repeat
    PlaybackMode.RepeatOne -> Icons.Default.RepeatOne
    PlaybackMode.Shuffle -> Icons.Default.Shuffle
}

private fun PlaybackMode.label() = when (this) {
    PlaybackMode.Sequential -> "顺序播放"
    PlaybackMode.RepeatOne -> "单曲循环"
    PlaybackMode.Shuffle -> "随机播放"
}

@Composable
private fun QueuePreview(queue: List<Song>, index: Int) {
    if (queue.isEmpty()) return
    val nextSongs = queue.drop(index + 1).take(2)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("播放队列", style = MaterialTheme.typography.titleSmall)
            }
            if (nextSongs.isEmpty()) {
                Text("已经是队列最后一首", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                nextSongs.forEach { song ->
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PlayerSidePanel(
    visible: Boolean,
    state: com.musicplayer.data.PlayerState,
    accent: Color,
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.24f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.84f)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .clickable(onClick = {}),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 18.dp,
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInHorizontally(initialOffsetX = { it }),
                    exit = slideOutHorizontally(targetOffsetX = { it })
                ) {
                    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("播放侧栏", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "关闭")
                            }
                        }
                        SleepTimerPanel(viewModel = viewModel, accent = accent)
                        QueuePanel(state = state, accent = accent, modifier = Modifier.weight(1f), onSongClick = { index ->
                            viewModel.playQueueIndex(index)
                            onClose()
                        })
                        AudioInfoLine(state.currentSong)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioInfoLine(song: Song?) {
    val text = song?.audioInfo?.displayText().orEmpty()
    if (text.isBlank()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SleepTimerPanel(viewModel: PlayerViewModel, accent: Color) {
    val remainingMs by viewModel.sleepTimerRemainingMs.collectAsState()
    val options = listOf(5, 10, 15, 30, 45, 60)
    Surface(shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = 0.10f)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bedtime, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text("睡眠定时", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (remainingMs > 0) {
                Text("剩余 ${formatDuration(remainingMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                options.take(3).forEach { minutes ->
                    Surface(
                        modifier = Modifier.weight(1f).clickable { viewModel.setSleepTimer(minutes) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text("$minutes 分钟", modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                options.drop(3).forEach { minutes ->
                    Surface(
                        modifier = Modifier.weight(1f).clickable { viewModel.setSleepTimer(minutes) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text("$minutes 分钟", modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center)
                    }
                }
            }
            if (remainingMs > 0) {
                Text(
                    "取消定时",
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.cancelSleepTimer() }.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QueuePanel(
    state: com.musicplayer.data.PlayerState,
    accent: Color,
    modifier: Modifier = Modifier,
    onSongClick: (Int) -> Unit
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = accent)
            Spacer(Modifier.width(8.dp))
            Text("播放队列", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (state.queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("队列为空", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.queue, key = { _, song -> song.id }) { index, song ->
                    val active = index == state.queueIndex
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSongClick(index) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlayer() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("当前没有播放歌曲", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
