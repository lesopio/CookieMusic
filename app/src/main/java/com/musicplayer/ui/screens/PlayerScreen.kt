package com.musicplayer.ui.screens

import android.os.Build
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt
import com.musicplayer.data.AudioVisualizationState
import com.musicplayer.data.FlowingBackgroundSettings
import com.musicplayer.data.LyricLine
import com.musicplayer.data.PlaybackMode
import com.musicplayer.data.PlayerPageTheme
import com.musicplayer.data.Song
import com.musicplayer.ui.components.AlbumArtImage
import com.musicplayer.ui.components.AudioQualityBadge
import com.musicplayer.ui.components.StructuredLyricText
import com.musicplayer.ui.components.rememberAlbumAccentColor
import com.musicplayer.ui.components.toDisplayText
import com.musicplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

private enum class PlayerPanel {
    Queue, Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    darkTheme: Boolean = false,
    windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(400.dp, 800.dp))
) {
    val playerState by viewModel.playerState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val flowingBackgroundSettings by viewModel.flowingBackgroundSettings.collectAsState()
    val visualizerState by viewModel.audioVisualizationState.collectAsState()
    val song = playerState.currentSong
    val accent by rememberAlbumAccentColor(song)
    val palette = rememberPlayerVisualPalette(accent, darkTheme)
    PlayerNavigationBarEffect(palette.backgroundBottom, darkTheme)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var activePanel by remember { mutableStateOf<PlayerPanel?>(null) }
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

    BackHandler(enabled = activePanel != null || showingLyricsPage) {
        if (activePanel != null) activePanel = null else animateToPlayer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundBottom)
            .onSizeChanged { pageSize = it }
            .pointerInput(playerState.lyrics.size, showingLyricsPage, pageSize.height, activePanel) {
                if (activePanel != null) return@pointerInput
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
        LyricsBackgroundLayer(
            song = song,
            accent = accent,
            palette = palette,
            isPlaying = playerState.isPlaying,
            animationSettings = flowingBackgroundSettings,
            visualizerState = visualizerState,
            darkTheme = darkTheme
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
                onQueueClick = { activePanel = PlayerPanel.Queue },
                onSettingsClick = { activePanel = PlayerPanel.Settings },
                onProgressBurst = {},
                visualizerState = visualizerState,
                compactLyricsPreviewEnabled = flowingBackgroundSettings.compactLyricsPreviewEnabled,
                songChangeTransitionEnabled = flowingBackgroundSettings.songChangeTransitionEnabled,
                pageTheme = flowingBackgroundSettings.pageTheme,
                palette = palette,
                modifier = Modifier.fillMaxSize()
            )
            LyricsPageContent(
                state = playerState,
                viewModel = viewModel,
                song = song,
                accent = accent,
                darkTheme = darkTheme,
                animationSettings = flowingBackgroundSettings,
                visualizerState = visualizerState,
                drawBackground = false,
                isLandscape = isLandscape,
                onBackClick = { animateToPlayer() },
                onMiniPlayerClick = { animateToPlayer() },
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, pageSize.height) }
            )
        }
        PlayerSidePanel(
            panel = activePanel,
            state = playerState,
            accent = accent,
            viewModel = viewModel,
            animationSettings = flowingBackgroundSettings,
            visualizerState = visualizerState,
            onClose = { activePanel = null }
        )
    }
}

@Composable
private fun PlayerPageContent(
    song: Song?,
    playerState: com.musicplayer.data.PlayerState,
    favorites: Set<String>,
    viewModel: PlayerViewModel,
    accent: Color,
    isLandscape: Boolean,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProgressBurst: () -> Unit,
    visualizerState: AudioVisualizationState,
    compactLyricsPreviewEnabled: Boolean,
    songChangeTransitionEnabled: Boolean,
    pageTheme: PlayerPageTheme,
    palette: PlayerVisualPalette,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.statusBarsPadding()) {
        PlayerTopBar(
            song = song,
            onBackClick = onBackClick,
            onSettingsClick = onSettingsClick,
            palette = palette
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
                onQueueClick = onQueueClick,
                onSettingsClick = onSettingsClick,
                onProgressBurst = onProgressBurst,
                visualizerState = visualizerState,
                compactLyricsPreviewEnabled = compactLyricsPreviewEnabled,
                songChangeTransitionEnabled = songChangeTransitionEnabled,
                minimal = pageTheme == PlayerPageTheme.Minimal,
                palette = palette
            )
        } else {
            if (pageTheme == PlayerPageTheme.Minimal) {
                HtmlReplicaPortraitPlayer(
                    song = song,
                    state = playerState,
                    viewModel = viewModel,
                    accent = accent,
                    onLyricsClick = onLyricsClick,
                    onQueueClick = onQueueClick,
                    visualizerState = visualizerState,
                    compactLyricsPreviewEnabled = compactLyricsPreviewEnabled,
                    palette = palette
                )
            } else PortraitPlayer(
                song = song,
                state = playerState,
                viewModel = viewModel,
                accent = accent,
                favorites = favorites,
                onLyricsClick = onLyricsClick,
                onSleepClick = onSleepClick,
                onEqualizerClick = onEqualizerClick,
                onHistoryClick = onHistoryClick,
                onQueueClick = onQueueClick,
                onSettingsClick = onSettingsClick,
                onProgressBurst = onProgressBurst,
                visualizerState = visualizerState,
                compactLyricsPreviewEnabled = compactLyricsPreviewEnabled,
                songChangeTransitionEnabled = songChangeTransitionEnabled,
                minimal = pageTheme == PlayerPageTheme.Minimal,
                palette = palette
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    song: Song?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    palette: PlayerVisualPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = palette.icon
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "正在播放",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = palette.primaryText
            )
            if (song != null) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, color = palette.secondaryText)
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Menu, contentDescription = "侧栏", tint = palette.icon)
        }
    }
}

@Composable
private fun HtmlReplicaPortraitPlayer(
    song: Song,
    state: com.musicplayer.data.PlayerState,
    viewModel: PlayerViewModel,
    accent: Color,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    visualizerState: AudioVisualizationState,
    compactLyricsPreviewEnabled: Boolean,
    palette: PlayerVisualPalette
) {
    Column(
        Modifier.fillMaxSize().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mirrors the reference page's full-width coverflow zone, but remains tied to the playing song.
        AlbumArtImage(
            song = song,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.08f),
            contentScale = ContentScale.Crop,
            fallbackModifier = Modifier.size(92.dp)
        )
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    song.audioInfo?.displayText().orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryText.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AudioQualityBadge(song = song, compact = true)
            }
            Spacer(Modifier.height(26.dp))
            Text(
                song.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            CurrentLyricLine(
                state = state,
                accent = accent,
                visualizerState = visualizerState,
                palette = palette,
                compactLyricsPreviewEnabled = compactLyricsPreviewEnabled,
                modifier = Modifier.fillMaxWidth(),
                onLyricsClick = onLyricsClick
            )
            Spacer(Modifier.weight(1f))
            ProgressSlider(
                currentPosition = state.currentPosition,
                duration = state.duration,
                accent = accent,
                palette = palette,
                onSeek = viewModel::seekTo,
                modifier = Modifier.fillMaxWidth()
            )
            HtmlReplicaControls(
                isPlaying = state.isPlaying,
                playbackMode = state.playbackMode,
                accent = accent,
                palette = palette,
                onPlayPause = viewModel::playPause,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onPlaybackModeToggle = viewModel::togglePlaybackMode,
                onQueueClick = onQueueClick
            )
        }
    }
}

@Composable
private fun HtmlReplicaControls(
    isPlaying: Boolean,
    playbackMode: PlaybackMode,
    accent: Color,
    palette: PlayerVisualPalette,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlaybackModeToggle: () -> Unit,
    onQueueClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(88.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPlaybackModeToggle) {
            Icon(playbackMode.icon(), contentDescription = playbackMode.label(), tint = palette.icon)
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(32.dp), tint = palette.icon)
        }
        IconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(44.dp),
                tint = accent
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.SkipNext, contentDescription = "下一首", modifier = Modifier.size(32.dp), tint = palette.icon)
        }
        IconButton(onClick = onQueueClick) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "播放队列", tint = palette.icon)
        }
    }
}

@Composable
private fun PortraitPlayer(
    song: Song,
    state: com.musicplayer.data.PlayerState,
    viewModel: PlayerViewModel,
    accent: Color,
    favorites: Set<String>,
    onLyricsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProgressBurst: () -> Unit,
    visualizerState: AudioVisualizationState,
    compactLyricsPreviewEnabled: Boolean,
    songChangeTransitionEnabled: Boolean,
    minimal: Boolean,
    palette: PlayerVisualPalette
) {
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 22.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SongArtworkTransition(
                song = song,
                palette = palette,
                enabled = songChangeTransitionEnabled,
                modifier = Modifier.fillMaxWidth(if (minimal) 0.70f else 0.78f)
            )
            Spacer(Modifier.height(if (minimal) 28.dp else 18.dp))
            SongInfoTransition(song, palette, songChangeTransitionEnabled, Modifier.fillMaxWidth())
            Spacer(Modifier.height(if (minimal) 26.dp else 18.dp))
            CurrentLyricLine(
                state,
                accent,
                visualizerState,
                palette,
                compactLyricsPreviewEnabled,
                Modifier.fillMaxWidth().offset(y = (-10).dp),
                onLyricsClick
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            ProgressSlider(
                currentPosition = state.currentPosition,
                duration = state.duration,
                accent = accent,
                palette = palette,
                onSeek = viewModel::seekTo,
                onProgressBurst = onProgressBurst,
                modifier = Modifier.fillMaxWidth()
            )
            PlayerControlPanel(
                isPlaying = state.isPlaying,
                playbackMode = state.playbackMode,
                isFavorite = favorites.contains(song.canonicalId),
                accent = accent,
                onPlayPause = viewModel::playPause,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onPlaybackModeToggle = viewModel::togglePlaybackMode,
                onFavoriteToggle = { viewModel.toggleFavorite(song.canonicalId) },
                onHistoryClick = onHistoryClick,
                onSleepClick = onSleepClick,
                onEqualizerClick = onEqualizerClick,
                onQueueClick = onQueueClick,
                onSettingsClick = onSettingsClick,
                palette = palette
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LandscapePlayer(
    song: Song,
    state: com.musicplayer.data.PlayerState,
    viewModel: PlayerViewModel,
    accent: Color,
    favorites: Set<String>,
    onLyricsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProgressBurst: () -> Unit,
    visualizerState: AudioVisualizationState,
    compactLyricsPreviewEnabled: Boolean,
    songChangeTransitionEnabled: Boolean,
    minimal: Boolean,
    palette: PlayerVisualPalette
) {
    Row(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 32.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(0.44f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            SongArtworkTransition(
                song = song,
                palette = palette,
                enabled = songChangeTransitionEnabled,
                modifier = Modifier.fillMaxWidth(if (minimal) 0.72f else 0.82f)
            )
            Spacer(Modifier.height(14.dp))
            CurrentLyricLine(state, accent, visualizerState, palette, compactLyricsPreviewEnabled, Modifier.fillMaxWidth(), onLyricsClick)
        }
        Column(Modifier.weight(0.56f).fillMaxHeight().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Spacer(Modifier.weight(1f))
            SongInfoTransition(song, palette, songChangeTransitionEnabled, Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            ProgressSlider(
                currentPosition = state.currentPosition,
                duration = state.duration,
                accent = accent,
                palette = palette,
                onSeek = viewModel::seekTo,
                onProgressBurst = onProgressBurst,
                modifier = Modifier.fillMaxWidth(0.92f)
            )
            PlayerControlPanel(
                isPlaying = state.isPlaying,
                playbackMode = state.playbackMode,
                isFavorite = favorites.contains(song.canonicalId),
                accent = accent,
                onPlayPause = viewModel::playPause,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onPlaybackModeToggle = viewModel::togglePlaybackMode,
                onFavoriteToggle = { viewModel.toggleFavorite(song.canonicalId) },
                onHistoryClick = onHistoryClick,
                onSleepClick = onSleepClick,
                onEqualizerClick = onEqualizerClick,
                onQueueClick = onQueueClick,
                onSettingsClick = onSettingsClick,
                palette = palette
            )
            Spacer(Modifier.weight(0.8f))
        }
    }
}

@Composable
private fun AlbumArt(
    song: Song,
    palette: PlayerVisualPalette,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, palette.controlStroke.copy(alpha = 0.42f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(Modifier.fillMaxSize()) {
                AlbumArtImage(
                    song = song,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
                    fallbackModifier = Modifier.size(92.dp)
                )
                AudioQualityBadge(
                    song = song,
                    compact = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SongArtworkTransition(
    song: Song,
    palette: PlayerVisualPalette,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) {
        Box(modifier, contentAlignment = Alignment.Center) {
            AlbumArt(song, palette, Modifier.fillMaxWidth())
        }
        return
    }
    key(song.canonicalId) {
        val reveal = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            reveal.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
        Box(
            modifier = modifier.graphicsLayer {
                alpha = reveal.value
                scaleX = 0.96f + reveal.value * 0.04f
                scaleY = 0.96f + reveal.value * 0.04f
            },
            contentAlignment = Alignment.Center
        ) {
            AlbumArt(song, palette, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SongInfo(song: Song, palette: PlayerVisualPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            song.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = palette.primaryText
        )
        Text(
            song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SongInfoTransition(song: Song, palette: PlayerVisualPalette, enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) {
        SongInfo(song, palette)
        return
    }
    key(song.canonicalId) {
        val reveal = remember { Animatable(0f) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        LaunchedEffect(Unit) {
            reveal.animateTo(1f, tween(320, delayMillis = 80, easing = FastOutSlowInEasing))
        }
        Box(
            modifier = modifier.graphicsLayer {
                alpha = reveal.value
                translationY = with(density) { (10.dp * (1f - reveal.value)).toPx() }
            },
            contentAlignment = Alignment.Center
        ) {
            SongInfo(song, palette)
        }
    }
}

@Composable
private fun CurrentLyricLine(
    state: com.musicplayer.data.PlayerState,
    accent: Color,
    visualizerState: AudioVisualizationState,
    palette: PlayerVisualPalette,
    compactLyricsPreviewEnabled: Boolean,
    modifier: Modifier = Modifier,
    onLyricsClick: () -> Unit
) {
    if (state.lyrics.isEmpty()) return
    val index = state.currentLyricIndex.coerceIn(0, state.lyrics.lastIndex)
    val hasTranslation = state.lyrics[index].toDisplayText().secondary != null
    val contentHeight = if (hasTranslation) 90.dp else 74.dp
    val focusedHeight = if (hasTranslation) 54.dp else 38.dp
    val surroundingHeight = 18.dp
    Box(
        modifier = modifier
            .height(116.dp)
            .clickable(onClick = onLyricsClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            modifier = Modifier.fillMaxWidth().height(contentHeight),
            targetState = index,
            transitionSpec = {
                if (targetState >= initialState) {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 2 } + fadeOut())
                } else {
                    (slideInVertically { -it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { it / 2 } + fadeOut())
                }
            },
            label = "player_lyric_change"
        ) { activeIndex ->
            if (compactLyricsPreviewEnabled) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(surroundingHeight), contentAlignment = Alignment.Center) {
                        state.lyrics.getOrNull(activeIndex - 1)?.let {
                            SurroundingLyricLine(it, palette.secondaryText.copy(alpha = 0.42f))
                        }
                    }
                    Box(Modifier.height(focusedHeight), contentAlignment = Alignment.Center) {
                        FocusedLyricLine(
                            line = state.lyrics[activeIndex],
                            state = state,
                            lineIndex = activeIndex,
                            accent = accent,
                            visualizerState = visualizerState,
                            palette = palette
                        )
                    }
                    Box(Modifier.height(surroundingHeight), contentAlignment = Alignment.Center) {
                        state.lyrics.getOrNull(activeIndex + 1)?.let {
                            SurroundingLyricLine(it, palette.secondaryText.copy(alpha = 0.32f))
                        }
                    }
                }
            } else {
                FocusedLyricLine(
                    line = state.lyrics[activeIndex],
                    state = state,
                    lineIndex = activeIndex,
                    accent = accent,
                    visualizerState = visualizerState,
                    palette = palette
                )
            }
        }
    }
}

@Composable
private fun FocusedLyricLine(
    line: LyricLine,
    state: com.musicplayer.data.PlayerState,
    lineIndex: Int,
    accent: Color,
    visualizerState: AudioVisualizationState,
    palette: PlayerVisualPalette
) {
    val nextTime = state.lyrics.drop(lineIndex + 1).firstOrNull { it.timeMs > line.timeMs }?.timeMs
        ?: state.duration.takeIf { it > line.timeMs }
        ?: (line.timeMs + 4_000L)
    val progress = rememberSmoothLyricProgress(
        lineKey = "${line.timeMs}:${line.text}",
        isPlaying = state.isPlaying,
        currentPosition = state.currentPosition,
        startTime = line.timeMs,
        endTime = nextTime
    )
    StructuredLyricText(
        line = line,
        active = true,
        currentPosition = state.currentPosition,
        fallbackProgress = progress,
        accent = accent,
        primaryColor = palette.primaryText.copy(alpha = if (palette.dark) 0.78f else 0.74f),
        secondaryColor = palette.secondaryText.copy(alpha = if (palette.dark) 0.82f else 0.76f),
        darkTheme = palette.dark,
        visualizerState = visualizerState,
        modifier = Modifier.fillMaxWidth(),
        primaryStyle = MaterialTheme.typography.titleLarge,
        secondaryStyle = MaterialTheme.typography.bodyMedium,
        activeBackground = false,
        primaryMaxLines = 1,
        translationMaxLines = 1,
        activeTranslationSpacing = 0.dp,
        activeTranslationOffsetY = (-4).dp
    )
}

@Composable
private fun SurroundingLyricLine(line: LyricLine, color: Color) {
    val displayText = line.toDisplayText()
    val style = MaterialTheme.typography.bodyMedium
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val availableWidthPx = with(density) { maxWidth.roundToPx() }
        val textWidthPx = remember(displayText.primary, style) {
            textMeasurer.measure(
                text = displayText.primary,
                style = style.copy(fontWeight = FontWeight.Medium),
                softWrap = false,
                maxLines = 1
            ).size.width
        }
        val overflowing = textWidthPx > availableWidthPx
        Text(
            text = displayText.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = if (overflowing) TextAlign.Start else TextAlign.Center,
            style = style,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
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
    visualizerState: AudioVisualizationState = AudioVisualizationState(),
    inactiveColor: Color = Color.White.copy(alpha = 0.58f),
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight
) {
    val glowAlpha = (0.10f + visualizerState.beatPulse * 0.16f + visualizerState.treble * 0.08f).coerceIn(0.08f, 0.30f)
    var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = glowAlpha),
            textAlign = TextAlign.Center,
            style = style.copy(color = accent),
            fontWeight = fontWeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
private fun ProgressSlider(
    currentPosition: Long,
    duration: Long,
    accent: Color,
    palette: PlayerVisualPalette,
    onSeek: (Long) -> Unit,
    onProgressBurst: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    var seekPreview by remember { mutableFloatStateOf(-1f) }
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    val displayProgress = if (seekPreview >= 0f) seekPreview else progress
    val glow = 0f
    fun seekToOffset(x: Float) {
        if (duration <= 0 || trackSize.width <= 0) return
        val value = (x / trackSize.width).coerceIn(0f, 1f)
        seekPreview = value
        onSeek((value * duration).toLong())
        onProgressBurst()
    }
    Column(modifier.padding(top = 4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(displayProgress.coerceIn(0f, 1f), 0f..1f)
                    setProgress { requested ->
                        val value = requested.coerceIn(0f, 1f)
                        onSeek((value * duration).toLong())
                        true
                    }
                }
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
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            ) {
                val radius = size.height / 2f
                drawRoundRect(
                    color = palette.progressTrack,
                    size = size,
                    cornerRadius = CornerRadius(radius, radius)
                )
                val progressWidth = size.width * displayProgress.coerceIn(0f, 1f)
                if (progressWidth > 0f) {
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset.Zero,
                        size = Size(progressWidth, size.height),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(if (seekPreview >= 0f) (seekPreview * duration).toLong() else currentPosition), style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
            Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
        }
    }
}

@Composable
private fun PlayerControlPanel(
    isPlaying: Boolean,
    playbackMode: PlaybackMode,
    isFavorite: Boolean,
    accent: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlaybackModeToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onHistoryClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSettingsClick: () -> Unit,
    palette: PlayerVisualPalette
) {
    Column(
        Modifier.fillMaxWidth().height(172.dp).padding(bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        StablePlaybackControls(
            isPlaying = isPlaying,
            playbackMode = playbackMode,
            isFavorite = isFavorite,
            accent = accent,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onPlaybackModeToggle = onPlaybackModeToggle,
            onFavoriteToggle = onFavoriteToggle,
            palette = palette
        )
        StableFeatureButtons(
            onHistoryClick = onHistoryClick,
            onSleepClick = onSleepClick,
            onEqualizerClick = onEqualizerClick,
            onQueueClick = onQueueClick,
            onSettingsClick = onSettingsClick,
            palette = palette
        )
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
    onFavoriteToggle: () -> Unit,
    palette: PlayerVisualPalette
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPlaybackModeToggle) {
            Icon(
                imageVector = playbackMode.icon(),
                contentDescription = playbackMode.label(),
                tint = if (playbackMode == PlaybackMode.Sequential) palette.icon.copy(alpha = 0.62f) else accent
            )
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(34.dp), tint = accent)
        }
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(66.dp).clip(CircleShape).background(accent)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(38.dp),
                tint = Color.White
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.SkipNext, contentDescription = "下一首", modifier = Modifier.size(34.dp), tint = accent)
        }
        IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) accent else palette.icon
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
    onSettingsClick: () -> Unit,
    palette: PlayerVisualPalette
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onHistoryClick) {
            Icon(Icons.Default.History, contentDescription = "播放历史", tint = palette.icon.copy(alpha = 0.78f))
        }
        IconButton(onClick = onSleepClick) {
            Icon(Icons.Default.Bedtime, contentDescription = "睡眠", tint = palette.icon.copy(alpha = 0.78f))
        }
        IconButton(onClick = onEqualizerClick) {
            Icon(Icons.Default.GraphicEq, contentDescription = "音效", tint = palette.icon.copy(alpha = 0.78f))
        }
        IconButton(onClick = onQueueClick) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "队列", tint = palette.icon.copy(alpha = 0.78f))
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = palette.icon.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun StablePlaybackControls(
    isPlaying: Boolean,
    playbackMode: PlaybackMode,
    isFavorite: Boolean,
    accent: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlaybackModeToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    palette: PlayerVisualPalette
) {
    Row(
        Modifier.fillMaxWidth().height(88.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlSlot {
            IconButton(onClick = onPlaybackModeToggle) {
                Icon(
                    imageVector = playbackMode.icon(),
                    contentDescription = playbackMode.label(),
                    tint = if (playbackMode == PlaybackMode.Sequential) palette.icon.copy(alpha = 0.62f) else accent
                )
            }
        }
        ControlSlot {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(34.dp), tint = palette.icon)
            }
        }
        ControlSlot {
            Surface(
                modifier = Modifier.size(76.dp).aspectRatio(1f),
                shape = CircleShape,
                color = accent
            ) {
                IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(42.dp),
                        tint = if (accent.luminance() > 0.179f) Color.Black else Color.White
                    )
                }
            }
        }
        ControlSlot {
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一首", modifier = Modifier.size(34.dp), tint = palette.icon)
            }
        }
        ControlSlot {
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint = if (isFavorite) accent else palette.icon
                )
            }
        }
    }
}

@Composable
private fun StableFeatureButtons(
    onHistoryClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSettingsClick: () -> Unit,
    palette: PlayerVisualPalette
) {
    Row(
        Modifier.fillMaxWidth().height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val secondaryIcon = palette.icon.copy(alpha = 0.78f)
        ControlSlot {
            IconButton(onClick = onHistoryClick, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.History, contentDescription = "播放历史", modifier = Modifier.size(32.dp), tint = secondaryIcon)
            }
        }
        ControlSlot {
            IconButton(onClick = onSleepClick, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.Bedtime, contentDescription = "睡眠", modifier = Modifier.size(32.dp), tint = secondaryIcon)
            }
        }
        ControlSlot {
            IconButton(onClick = onEqualizerClick, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.GraphicEq, contentDescription = "音效", modifier = Modifier.size(32.dp), tint = secondaryIcon)
            }
        }
        ControlSlot {
            IconButton(onClick = onQueueClick, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "队列", modifier = Modifier.size(32.dp), tint = secondaryIcon)
            }
        }
        ControlSlot {
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(32.dp), tint = secondaryIcon)
            }
        }
    }
}

@Composable
private fun RowScope.ControlSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
    ) {
        content()
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
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
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
    panel: PlayerPanel?,
    state: com.musicplayer.data.PlayerState,
    accent: Color,
    viewModel: PlayerViewModel,
    animationSettings: FlowingBackgroundSettings,
    visualizerState: AudioVisualizationState,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = panel != null,
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
                    .clickable(onClick = {}),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 18.dp,
                shape = androidx.compose.ui.graphics.RectangleShape
            ) {
                AnimatedVisibility(
                    visible = panel != null,
                    enter = slideInHorizontally(initialOffsetX = { it }),
                    exit = slideOutHorizontally(targetOffsetX = { it })
                ) {
                    Column(
                        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (panel == PlayerPanel.Queue) "播放列表" else "播放器设置",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "关闭")
                            }
                        }
                        when (panel) {
                            PlayerPanel.Queue -> {
                                QueuePanel(state = state, accent = accent, modifier = Modifier.weight(1f), onSongClick = { index ->
                                    viewModel.playQueueIndex(index)
                                    onClose()
                                })
                                AudioInfoLine(state.currentSong)
                            }
                            PlayerPanel.Settings -> {
                                Column(
                                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SleepTimerPanel(viewModel = viewModel, accent = accent)
                                    PlayerAnimationSettingsContent(
                                        settings = animationSettings,
                                        onChange = viewModel::updateFlowingBackgroundSettings
                                    )
                                    AudioInfoLine(state.currentSong)
                                }
                            }
                            null -> Unit
                        }
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
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = accent)
            Spacer(Modifier.width(8.dp))
            Text("播放队列", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (state.queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("队列为空", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(state.queue, key = { _, song -> song.id }) { index, song ->
                    val active = index == state.queueIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(index) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtImage(
                            song = song,
                            modifier = Modifier.size(54.dp),
                            fallbackModifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                song.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (active) accent else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${song.artist} · ${song.album}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (active) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "正在播放", tint = accent, modifier = Modifier.size(22.dp))
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

@Composable
private fun PlayerNavigationBarEffect(color: Color, darkTheme: Boolean) {
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
