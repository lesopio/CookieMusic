package com.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search as SearchOutlined
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicplayer.data.ThemeMode
import com.musicplayer.ui.screens.BilingualLyricsIndexScreen
import com.musicplayer.ui.components.MiniPlayer
import com.musicplayer.ui.components.rememberAlbumAccentColor
import com.musicplayer.ui.screens.ColorSettingsScreen
import com.musicplayer.ui.screens.EqualizerScreen
import com.musicplayer.ui.screens.FavoritesScreen
import com.musicplayer.ui.screens.HomeScreen
import com.musicplayer.ui.screens.ImportManagerScreen
import com.musicplayer.ui.screens.LyricsScreen
import com.musicplayer.ui.screens.PlayerScreen
import com.musicplayer.ui.screens.PlayerAnimationSettingsScreen
import com.musicplayer.ui.screens.PlayHistoryScreen
import com.musicplayer.ui.screens.SearchScreen
import com.musicplayer.ui.screens.SettingsScreen
import com.musicplayer.ui.screens.SleepTimerScreen
import com.musicplayer.ui.screens.StatusLyricsSettingsScreen
import com.musicplayer.ui.theme.MusicPlayerTheme
import com.musicplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var permissionRefresh: (() -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) permissionRefresh?.invoke()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val viewModel: PlayerViewModel = viewModel()
            permissionRefresh = viewModel::scanSongs
            val themeMode by viewModel.themeMode.collectAsState()
            val appColorSettings by viewModel.appColorSettings.collectAsState()
            val playerState by viewModel.playerState.collectAsState()
            val albumAccent by rememberAlbumAccentColor(playerState.currentSong)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            LaunchedEffect(appColorSettings.followNowPlayingAccentEnabled, albumAccent) {
                if (appColorSettings.followNowPlayingAccentEnabled) {
                    viewModel.updateLastNowPlayingAccent(albumAccent.toArgb())
                }
            }
            SystemBarsEffect(darkTheme)
            MusicPlayerTheme(themeMode = themeMode, darkTheme = darkTheme, dynamicColor = false, appColorSettings = appColorSettings) {
                val windowSizeClass = calculateWindowSizeClass(this)
                MusicPlayerApp(viewModel = viewModel, windowSizeClass = windowSizeClass, darkTheme = darkTheme)
            }
        }
        checkPermission()
    }

    private fun checkPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }
}

@Composable
private fun HighRefreshRateEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as ComponentActivity).window
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.display
        } else {
            @Suppress("DEPRECATION")
            window.windowManager.defaultDisplay
        }
        val bestMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
        if (bestMode != null) {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = bestMode.modeId
            }
        }
        onDispose {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = 0
            }
        }
    }
}

@Composable
private fun SystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    DisposableEffect(darkTheme, view) {
        val window = (view.context as ComponentActivity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        onDispose {}
    }
}

sealed class Screen {
    data object Home : Screen()
    data object Favorites : Screen()
    data object Equalizer : Screen()
    data object Settings : Screen()
    data object ImportManager : Screen()
    data object Lyrics : Screen()
    data object Player : Screen()
    data object SleepTimer : Screen()
    data object Search : Screen()
    data object History : Screen()
    data object StatusLyricsSettings : Screen()
    data object ColorSettings : Screen()
    data object PlayerAnimationSettings : Screen()
    data object BilingualLyricsIndex : Screen()
}

data class BottomNavItem(
    val label: String,
    val rootIndex: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalAnimationApi::class)
@Composable
fun MusicPlayerApp(
    viewModel: PlayerViewModel,
    windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(400.dp, 800.dp)),
    darkTheme: Boolean = false
) {
    val playerState by viewModel.playerState.collectAsState()
    val flowingBackgroundSettings by viewModel.flowingBackgroundSettings.collectAsState()
    val isLandscape = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
    val lifecycleOwner = LocalLifecycleOwner.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val pagerScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var miniPlayerHeightPx by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setAppInForeground(true)
                Lifecycle.Event.ON_STOP -> viewModel.setAppInForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var secondaryStack by rememberSaveable(
        stateSaver = listSaver(
            save = { stack -> stack.map { it.storageKey() } },
            restore = { keys -> keys.mapNotNull(::screenFromStorageKey) }
        )
    ) { mutableStateOf(emptyList()) }
    val currentSecondary = secondaryStack.lastOrNull()
    fun animateRootTo(index: Int) {
        if (index == pagerState.currentPage) return
        pagerScope.launch {
            pagerState.animateScrollToPage(
                page = index,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
    }
    fun navigate(screen: Screen) {
        secondaryStack = secondaryStack + screen
    }
    fun navigateBack() {
        secondaryStack = secondaryStack.dropLast(1)
    }

    BackHandler(enabled = secondaryStack.isNotEmpty()) {
        navigateBack()
    }

    val bottomNavItems = listOf(
        BottomNavItem("音乐", 0, Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("搜索", 1, Icons.Filled.Search, Icons.Outlined.SearchOutlined),
        BottomNavItem("收藏", 2, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
        BottomNavItem("设置", 3, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val showMiniPlayer = playerState.currentSong != null && currentSecondary !is Screen.Player
    val miniPlayerReservedHeight = if (showMiniPlayer) {
        val measuredHeight = with(density) { miniPlayerHeightPx.toDp() }
        if (measuredHeight > 0.dp) measuredHeight + 8.dp else 112.dp
    } else {
        0.dp
    }

    if (isLandscape) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (currentSecondary == null) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(end = 0.dp)
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = pagerState.currentPage == item.rootIndex
                            NavigationRailItem(
                                selected = selected,
                                onClick = { animateRootTo(item.rootIndex) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
                RootPagerContent(
                    pagerState = pagerState,
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    darkTheme = darkTheme,
                    onNavigate = ::navigate,
                    miniPlayerReservedHeight = miniPlayerReservedHeight,
                    modifier = Modifier.weight(1f)
                )
            }
            SecondaryScreenOverlay(
                screen = currentSecondary,
                viewModel = viewModel,
                windowSizeClass = windowSizeClass,
                darkTheme = darkTheme,
                onNavigate = ::navigate,
                onBack = ::navigateBack
            )
            MiniPlayerOverlay(
                visible = showMiniPlayer,
                playerState = playerState,
                rotateCover = flowingBackgroundSettings.capsuleCoverRotationEnabled,
                viewModel = viewModel,
                bottomPadding = 0.dp,
                onMeasured = { miniPlayerHeightPx = it },
                onClick = { navigate(Screen.Player) }
            )
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (currentSecondary == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = pagerState.currentPage == item.rootIndex
                            NavigationBarItem(
                                selected = selected,
                                onClick = { animateRootTo(item.rootIndex) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    RootPagerContent(
                        pagerState = pagerState,
                        viewModel = viewModel,
                        windowSizeClass = windowSizeClass,
                        darkTheme = darkTheme,
                        onNavigate = ::navigate,
                        miniPlayerReservedHeight = miniPlayerReservedHeight,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                SecondaryScreenOverlay(
                    screen = currentSecondary,
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    darkTheme = darkTheme,
                    onNavigate = ::navigate,
                    onBack = ::navigateBack
                )
                MiniPlayerOverlay(
                    visible = showMiniPlayer,
                    playerState = playerState,
                    rotateCover = flowingBackgroundSettings.capsuleCoverRotationEnabled,
                    viewModel = viewModel,
                    bottomPadding = padding.calculateBottomPadding(),
                    onMeasured = { miniPlayerHeightPx = it },
                    onClick = { navigate(Screen.Player) }
                )
            }
        }
    }
}

private fun Screen.storageKey(): String = this::class.simpleName.orEmpty()

private fun screenFromStorageKey(key: String): Screen? = when (key) {
    Screen.ImportManager::class.simpleName -> Screen.ImportManager
    Screen.Equalizer::class.simpleName -> Screen.Equalizer
    Screen.Lyrics::class.simpleName -> Screen.Lyrics
    Screen.Player::class.simpleName -> Screen.Player
    Screen.SleepTimer::class.simpleName -> Screen.SleepTimer
    Screen.Search::class.simpleName -> Screen.Search
    Screen.History::class.simpleName -> Screen.History
    Screen.StatusLyricsSettings::class.simpleName -> Screen.StatusLyricsSettings
    Screen.ColorSettings::class.simpleName -> Screen.ColorSettings
    Screen.PlayerAnimationSettings::class.simpleName -> Screen.PlayerAnimationSettings
    Screen.BilingualLyricsIndex::class.simpleName -> Screen.BilingualLyricsIndex
    else -> null
}

@Composable
private fun RootPagerContent(
    pagerState: androidx.compose.foundation.pager.PagerState,
    viewModel: PlayerViewModel,
    windowSizeClass: WindowSizeClass,
    darkTheme: Boolean,
    onNavigate: (Screen) -> Unit,
    miniPlayerReservedHeight: Dp,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 3,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = miniPlayerReservedHeight)
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                viewModel = viewModel,
                onSongClick = { song ->
                    viewModel.playSong(song)
                    onNavigate(Screen.Player)
                },
                windowSizeClass = windowSizeClass
            )
            1 -> SearchScreen(
                viewModel = viewModel,
                onSongClick = { song ->
                    viewModel.playSong(song)
                    onNavigate(Screen.Player)
                },
                windowSizeClass = windowSizeClass
            )
            2 -> FavoritesScreen(
                viewModel = viewModel,
                onBackClick = {},
                onSongClick = { song ->
                    viewModel.playSong(song)
                    onNavigate(Screen.Player)
                },
                onOpenPlayer = { onNavigate(Screen.Player) },
                windowSizeClass = windowSizeClass
            )
            3 -> SettingsScreen(
                viewModel = viewModel,
                onImportManagerClick = { onNavigate(Screen.ImportManager) },
                onEqualizerClick = { onNavigate(Screen.Equalizer) },
                onHistoryClick = { onNavigate(Screen.History) },
                onStatusLyricsClick = { onNavigate(Screen.StatusLyricsSettings) },
                onColorSettingsClick = { onNavigate(Screen.ColorSettings) },
                onAnimationSettingsClick = { onNavigate(Screen.PlayerAnimationSettings) },
                onBilingualIndexClick = { onNavigate(Screen.BilingualLyricsIndex) },
                onScanClick = { viewModel.scanSongs() },
                windowSizeClass = windowSizeClass
            )
        }
    }
}

@Composable
private fun MiniPlayerOverlay(
    visible: Boolean,
    playerState: com.musicplayer.data.PlayerState,
    rotateCover: Boolean,
    viewModel: PlayerViewModel,
    bottomPadding: Dp,
    onMeasured: (Int) -> Unit,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
            slideInVertically(animationSpec = tween(260, easing = FastOutSlowInEasing), initialOffsetY = { it / 3 }),
        exit = fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
            slideOutVertically(animationSpec = tween(220, easing = FastOutSlowInEasing), targetOffsetY = { it / 3 }),
        modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            val progress = if (playerState.duration > 0) {
                playerState.currentPosition.toFloat() / playerState.duration.toFloat()
            } else 0f
            MiniPlayer(
                song = playerState.currentSong,
                isPlaying = playerState.isPlaying,
                progress = progress,
                onPlayPause = { viewModel.playPause() },
                onPrevious = { viewModel.previous() },
                onNext = { viewModel.next() },
                onClick = onClick,
                rotateCover = rotateCover && playerState.isPlaying,
                modifier = Modifier.onSizeChanged { onMeasured(it.height) }
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SecondaryScreenOverlay(
    screen: Screen?,
    viewModel: PlayerViewModel,
    windowSizeClass: WindowSizeClass,
    darkTheme: Boolean,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                slideInVertically(animationSpec = tween(340, easing = FastOutSlowInEasing)) { it })
                .togetherWith(
                    fadeOut(animationSpec = tween(170, easing = FastOutSlowInEasing)) +
                        slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it }
                )
        },
        label = "secondary_screen_transition"
    ) { target ->
        if (target != null) {
            SecondaryScreenContent(
                screen = target,
                viewModel = viewModel,
                windowSizeClass = windowSizeClass,
                darkTheme = darkTheme,
                onNavigate = onNavigate,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun SecondaryScreenContent(
    screen: Screen,
    viewModel: PlayerViewModel,
    windowSizeClass: WindowSizeClass,
    darkTheme: Boolean,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when (screen) {
        is Screen.Equalizer -> EqualizerScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            windowSizeClass = windowSizeClass
        )
        is Screen.Settings -> SettingsScreen(
            viewModel = viewModel,
            onImportManagerClick = { onNavigate(Screen.ImportManager) },
            onEqualizerClick = { onNavigate(Screen.Equalizer) },
            onHistoryClick = { onNavigate(Screen.History) },
            onStatusLyricsClick = { onNavigate(Screen.StatusLyricsSettings) },
            onColorSettingsClick = { onNavigate(Screen.ColorSettings) },
            onAnimationSettingsClick = { onNavigate(Screen.PlayerAnimationSettings) },
            onBilingualIndexClick = { onNavigate(Screen.BilingualLyricsIndex) },
            onScanClick = { viewModel.scanSongs() },
            windowSizeClass = windowSizeClass
        )
        is Screen.ImportManager -> ImportManagerScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            windowSizeClass = windowSizeClass
        )
        is Screen.Player -> PlayerScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            onLyricsClick = {},
            onSleepClick = { onNavigate(Screen.SleepTimer) },
            onEqualizerClick = { onNavigate(Screen.Equalizer) },
            onHistoryClick = { onNavigate(Screen.History) },
            darkTheme = darkTheme,
            windowSizeClass = windowSizeClass
        )
        is Screen.Lyrics -> LyricsScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            darkTheme = darkTheme,
            windowSizeClass = windowSizeClass
        )
        is Screen.SleepTimer -> SleepTimerScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            windowSizeClass = windowSizeClass
        )
        is Screen.History -> PlayHistoryScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            windowSizeClass = windowSizeClass
        )
        is Screen.StatusLyricsSettings -> StatusLyricsSettingsScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            windowSizeClass = windowSizeClass
        )
        is Screen.ColorSettings -> ColorSettingsScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            windowSizeClass = windowSizeClass
        )
        is Screen.PlayerAnimationSettings -> PlayerAnimationSettingsScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            windowSizeClass = windowSizeClass
        )
        is Screen.BilingualLyricsIndex -> BilingualLyricsIndexScreen(
            viewModel = viewModel,
            onBackClick = onBack,
            windowSizeClass = windowSizeClass
        )
            is Screen.Home, is Screen.Search, is Screen.Favorites, is Screen.Settings -> Unit
        }
    }
}
