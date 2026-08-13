package com.musicplayer.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.musicplayer.data.AppColorSettings
import com.musicplayer.data.AudioVisualizationState
import com.musicplayer.data.BilingualLyricsIndexSummary
import com.musicplayer.data.EqualizerPreset
import com.musicplayer.data.EqualizerState
import com.musicplayer.data.FlowingBackgroundSettings
import com.musicplayer.data.ImportedFolder
import com.musicplayer.data.LyricLine
import com.musicplayer.data.OverlayLyricsSettings
import com.musicplayer.data.PlayerAnimationPerformanceMode
import com.musicplayer.data.PlayerPageTheme
import com.musicplayer.data.PlaybackMode
import com.musicplayer.data.Playlist
import com.musicplayer.data.PlayerState
import com.musicplayer.data.Song
import com.musicplayer.data.SongPlayHistory
import com.musicplayer.data.SongRepository
import com.musicplayer.data.ThemeMode
import com.musicplayer.service.MusicController
import com.musicplayer.service.StatusLyricOverlayService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = SongRepository(application)
    private val preferences = application.getSharedPreferences("music_library", Context.MODE_PRIVATE)
    val musicController = MusicController(application)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _importedSongs = MutableStateFlow<List<Song>>(emptyList())
    val importedSongs: StateFlow<List<Song>> = _importedSongs.asStateFlow()

    private val _importedFolders = MutableStateFlow<List<ImportedFolder>>(emptyList())
    val importedFolders: StateFlow<List<ImportedFolder>> = _importedFolders.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _libraryUiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val libraryUiState: StateFlow<LibraryUiState> = _libraryUiState.asStateFlow()

    private val _isImportingSongs = MutableStateFlow(false)
    val isImportingSongs: StateFlow<Boolean> = _isImportingSongs.asStateFlow()

    private val _equalizerState = MutableStateFlow(loadEqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _sleepTimerRemainingMs = MutableStateFlow(0L)
    val sleepTimerRemainingMs: StateFlow<Long> = _sleepTimerRemainingMs.asStateFlow()

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _appColorSettings = MutableStateFlow(loadAppColorSettings())
    val appColorSettings: StateFlow<AppColorSettings> = _appColorSettings.asStateFlow()

    private val _overlayLyricsSettings = MutableStateFlow(loadOverlayLyricsSettings())
    val overlayLyricsSettings: StateFlow<OverlayLyricsSettings> = _overlayLyricsSettings.asStateFlow()

    private val _flowingBackgroundSettings = MutableStateFlow(loadFlowingBackgroundSettings())
    val flowingBackgroundSettings: StateFlow<FlowingBackgroundSettings> = _flowingBackgroundSettings.asStateFlow()

    private val _audioVisualizationState = MutableStateFlow(AudioVisualizationState())
    val audioVisualizationState: StateFlow<AudioVisualizationState> = _audioVisualizationState.asStateFlow()

    private val _isImportingFolder = MutableStateFlow(false)
    val isImportingFolder: StateFlow<Boolean> = _isImportingFolder.asStateFlow()

    private val _snackbarEvent = MutableStateFlow<String?>(null)
    val snackbarEvent: StateFlow<String?> = _snackbarEvent.asStateFlow()

    private val _playHistory = MutableStateFlow<List<SongPlayHistory>>(emptyList())
    val playHistory: StateFlow<List<SongPlayHistory>> = _playHistory.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _bilingualLyricsIndexState = MutableStateFlow(BilingualLyricsIndexState())
    val bilingualLyricsIndexState: StateFlow<BilingualLyricsIndexState> = _bilingualLyricsIndexState.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var overlayPauseHideJob: Job? = null
    private var historyRecordJob: Job? = null
    private var searchJob: Job? = null
    private var appInForeground: Boolean = true
    private var lastOverlayText: String? = null
    private var lastMetadataText: String? = null
    private var metadataLyricsApplied = false
    private var lastHistorySongId: Long? = null
    private var lastHistoryAt: Long = 0L
    private val importedSongMap = linkedMapOf<String, Song>()
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId?.toLongOrNull()
            if (mediaId != null && mediaId == _playerState.value.currentSong?.id) {
                _playerState.value = _playerState.value.copy(
                    currentPosition = musicController.currentPosition,
                    duration = musicController.duration,
                    isPlaying = musicController.isPlaying
                )
                return
            }
            val idx = musicController.currentMediaItemIndex
            val queue = _playerState.value.queue
            if (idx >= 0 && idx < queue.size) {
                setCurrentSong(queue[idx], idx)
            } else {
                syncFromController()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _playerState.value = _playerState.value.copy(duration = musicController.duration)
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playerState.value = _playerState.value.copy(playbackMode = musicController.playbackMode)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playerState.value = _playerState.value.copy(playbackMode = musicController.playbackMode)
        }
    }

    init {
        musicController.connect()
        startPositionUpdates()
        setupPlayerListener()
        observeControllerConnection()
        scanSongs()
        viewModelScope.launch { _favorites.value = repository.loadFavorites() }
        viewModelScope.launch { _playHistory.value = repository.loadPlayHistory() }
    }

    private fun setupPlayerListener() {
        musicController.addListener(playerListener)
    }

    private fun observeControllerConnection() {
        viewModelScope.launch {
            musicController.isConnected.collectLatest { connected ->
                if (connected) syncFromController()
            }
        }
    }

    private fun syncFromController() {
        val current = musicController.currentSongSnapshot() ?: return
        val queue = musicController.queueSnapshot().ifEmpty { listOf(current) }
        val index = musicController.currentMediaItemIndex.takeIf { it in queue.indices } ?: 0
        val syncedSong = queue.getOrElse(index) { current }
        _playerState.value = _playerState.value.copy(
            currentSong = syncedSong,
            queue = queue,
            queueIndex = index,
            currentPosition = musicController.currentPosition,
            duration = musicController.duration,
            isPlaying = musicController.isPlaying,
            playbackMode = musicController.playbackMode
        )
        if (musicController.isPlaying) {
            scheduleHistoryRecord(syncedSong)
        }
        viewModelScope.launch {
            reloadLyricsForCurrentSong()
        }
    }

    fun scanSongs() {
        viewModelScope.launch {
            if (_allSongs.value.isEmpty()) _libraryUiState.value = LibraryUiState.Loading
            val cached = repository.loadCachedSongs()
            if (cached.isNotEmpty()) {
                _allSongs.value = cached.sortedBy { it.title.lowercase() }
                _libraryUiState.value = LibraryUiState.Content(cached.size)
            }
            val imported = runCatching { reloadImportedLibrary() }.getOrDefault(emptyList())
            if (imported.isNotEmpty()) {
                _allSongs.value = mergeSongs(emptyList(), imported)
                _libraryUiState.value = LibraryUiState.Content(imported.size)
            }
            runCatching { repository.scanSongs() }
                .onSuccess { scannedSongs ->
                    _allSongs.value = mergeSongs(scannedSongs, importedSongMap.values)
                    _libraryUiState.value = if (_allSongs.value.isEmpty()) LibraryUiState.Empty else LibraryUiState.Content(_allSongs.value.size)
                    viewModelScope.launch { _favorites.value = repository.loadFavorites() }
                    viewModelScope.launch { _playlists.value = repository.loadPlaylists() }
                }
                .onFailure { error ->
                    _libraryUiState.value = if (_allSongs.value.isEmpty()) LibraryUiState.Error(error.message ?: "扫描失败") else LibraryUiState.Content(_allSongs.value.size)
                    _snackbarEvent.value = "扫描失败：${error.message ?: "请检查媒体权限"}"
                }
        }
    }

    fun importSongs(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            if (_isImportingSongs.value) return@launch
            _isImportingSongs.value = true
            val imported = repository.importSongs(uris)
            val importedSongs = reloadImportedLibrary()
            _allSongs.value = mergeSongs(repository.scanSongs(), importedSongs)
            val failed = (uris.size - imported.size).coerceAtLeast(0)
            _snackbarEvent.value = when {
                imported.isEmpty() -> "导入失败：未获得持久读取权限或文件无法解析"
                failed > 0 -> "导入完成：${imported.size} 首，失败 $failed 首"
                else -> "导入完成：${imported.size} 首"
            }
            _isImportingSongs.value = false
        }
    }

    fun importFolder(uri: Uri) {
        if (_isImportingFolder.value) return
        viewModelScope.launch {
            _isImportingFolder.value = true
            val result = runCatching { repository.importFolder(uri) }.getOrElse {
                _isImportingFolder.value = false
                _snackbarEvent.value = "导入失败：${it.message ?: "无法扫描文件夹"}"
                return@launch
            }
            result.songs.forEach { song ->
                importedSongMap[song.canonicalId] = song
            }
            val imported = reloadImportedLibrary()
            _allSongs.value = mergeSongs(repository.scanSongs(), imported)
            _isImportingFolder.value = false
            _snackbarEvent.value = "导入完成：${result.songs.size} 首，跳过 ${result.skipped} 项"
        }
    }

    fun removeImportedSong(song: Song) {
        viewModelScope.launch {
            repository.removeImportedSong(song)
            reloadImportedLibrary()
            _allSongs.value = mergeSongs(repository.scanSongs(), importedSongMap.values)
        }
    }

    fun removeImportedFolder(folder: ImportedFolder) {
        viewModelScope.launch {
            repository.removeImportedFolder(folder)
            reloadImportedLibrary()
            _allSongs.value = mergeSongs(repository.scanSongs(), importedSongMap.values)
        }
    }

    fun importLyrics(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            repository.importLyrics(uris, _allSongs.value)
            reloadLyricsForCurrentSong()
        }
    }

    fun indexBilingualLyrics() {
        if (_bilingualLyricsIndexState.value.running) return
        viewModelScope.launch {
            val songs = _allSongs.value
            if (songs.isEmpty()) {
                _bilingualLyricsIndexState.value = BilingualLyricsIndexState(message = "曲库为空，先导入或扫描歌曲")
                return@launch
            }
            _bilingualLyricsIndexState.value = BilingualLyricsIndexState(
                running = true,
                total = songs.size,
                message = "正在准备索引"
            )
            val summary = runCatching {
                repository.indexBilingualLyrics(songs) { processed, total, currentTitle ->
                    _bilingualLyricsIndexState.value = _bilingualLyricsIndexState.value.copy(
                        running = processed < total,
                        processed = processed,
                        total = total,
                        currentTitle = currentTitle,
                        message = if (processed < total) "正在识别歌词双语结构" else "正在保存索引"
                    )
                }
            }.getOrElse { error ->
                _bilingualLyricsIndexState.value = _bilingualLyricsIndexState.value.copy(
                    running = false,
                    message = "索引失败：${error.message ?: "未知错误"}"
                )
                return@launch
            }
            _bilingualLyricsIndexState.value = BilingualLyricsIndexState(
                running = false,
                processed = summary.totalSongs,
                total = summary.totalSongs,
                summary = summary,
                message = "索引完成"
            )
        }
    }

    fun searchSongs(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            _searchResults.value = repository.searchSongs(query)
        }
    }

    fun playSong(song: Song) {
        val songs = _allSongs.value
        val idx = songs.indexOf(song)
        if (idx >= 0) {
            setCurrentSong(song, idx, songs)
            musicController.playQueue(songs, idx)
        }
    }

    fun playPause() { musicController.playPause() }

    fun next() { musicController.next() }

    fun previous() { musicController.previous() }

    fun playQueueIndex(index: Int) {
        val queue = _playerState.value.queue
        if (index in queue.indices) {
            setCurrentSong(queue[index], index, queue)
            musicController.playQueueIndex(index)
        }
    }

    fun seekTo(positionMs: Long) { musicController.seekTo(positionMs) }

    fun seekToLyric(line: LyricLine) {
        seekTo(line.timeMs)
    }

    fun togglePlaybackMode() {
        val newMode = when (_playerState.value.playbackMode) {
            PlaybackMode.Sequential -> PlaybackMode.RepeatOne
            PlaybackMode.RepeatOne -> PlaybackMode.Shuffle
            PlaybackMode.Shuffle -> PlaybackMode.Sequential
        }
        _playerState.value = _playerState.value.copy(playbackMode = newMode)
        musicController.setPlaybackMode(newMode)
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playerState.value = _playerState.value.copy(playbackMode = mode)
        musicController.setPlaybackMode(mode)
    }

    fun consumeSnackbar() {
        _snackbarEvent.value = null
    }

    fun toggleFavorite(songId: String) {
        val favs = _favorites.value.toMutableSet()
        if (favs.contains(songId)) favs.remove(songId) else favs.add(songId)
        _favorites.value = favs
        viewModelScope.launch { repository.saveFavorites(favs) }
    }

    fun isFavorite(songId: String): Boolean = _favorites.value.contains(songId)

    fun setEqualizerPreset(preset: EqualizerPreset) {
        val current = _equalizerState.value
        updateEqualizerState(current.copy(enabled = true, hifiModeEnabled = false, bandLevels = preset.bandLevels, preset = preset.name))
    }

    fun setBandLevel(band: Int, level: Float) {
        val current = _equalizerState.value
        val newLevels = current.bandLevels.toMutableList()
        if (band in newLevels.indices) {
            newLevels[band] = level
            updateEqualizerState(current.copy(enabled = true, hifiModeEnabled = false, bandLevels = newLevels, preset = "自定义"))
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        updateEqualizerState(_equalizerState.value.copy(enabled = enabled, hifiModeEnabled = if (enabled) false else _equalizerState.value.hifiModeEnabled))
    }

    fun toggleHifiMode(enabled: Boolean) {
        val current = _equalizerState.value
        updateEqualizerState(
            current.copy(
                hifiModeEnabled = enabled,
                enabled = if (enabled) false else current.enabled,
                bassBoostEnabled = if (enabled) false else current.bassBoostEnabled,
                virtualizerEnabled = if (enabled) false else current.virtualizerEnabled,
                loudnessEnabled = if (enabled) false else current.loudnessEnabled
            )
        )
    }

    fun updateBassBoost(enabled: Boolean, strength: Float = _equalizerState.value.bassBoostStrength) {
        updateEqualizerState(_equalizerState.value.copy(hifiModeEnabled = false, bassBoostEnabled = enabled, bassBoostStrength = strength.coerceIn(0f, 1f)))
    }

    fun updateVirtualizer(enabled: Boolean, strength: Float = _equalizerState.value.virtualizerStrength) {
        updateEqualizerState(_equalizerState.value.copy(hifiModeEnabled = false, virtualizerEnabled = enabled, virtualizerStrength = strength.coerceIn(0f, 1f)))
    }

    fun updateLoudness(enabled: Boolean, gain: Float = _equalizerState.value.loudnessGain) {
        updateEqualizerState(_equalizerState.value.copy(hifiModeEnabled = false, loudnessEnabled = enabled, loudnessGain = gain.coerceIn(0f, 1f)))
    }

    private fun updateEqualizerState(state: EqualizerState) {
        val normalized = state.copy(
            bandLevels = state.bandLevels.take(5).let { levels ->
                if (levels.size == 5) levels else levels + List(5 - levels.size) { 0f }
            }.map { it.coerceIn(-10f, 10f) },
            bassBoostStrength = state.bassBoostStrength.coerceIn(0f, 1f),
            virtualizerStrength = state.virtualizerStrength.coerceIn(0f, 1f),
            loudnessGain = state.loudnessGain.coerceIn(0f, 1f)
        )
        _equalizerState.value = normalized
        saveEqualizerState(normalized)
    }

    fun setSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        _playerState.value = _playerState.value.copy(sleepTimerMinutes = minutes, sleepTimerRemainingMs = minutes * 60_000L)
        _sleepTimerRemainingMs.value = minutes * 60_000L
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            val totalMs = minutes * 60_000L
            val step = 1000L
            var elapsed = 0L
            while (isActive && elapsed < totalMs) {
                delay(step)
                elapsed += step
                _sleepTimerRemainingMs.value = totalMs - elapsed
            }
            if (elapsed >= totalMs) {
                musicController.pause()
                _sleepTimerRemainingMs.value = 0L
                _playerState.value = _playerState.value.copy(sleepTimerMinutes = 0, sleepTimerRemainingMs = 0L)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMs.value = 0L
        _playerState.value = _playerState.value.copy(sleepTimerMinutes = 0, sleepTimerRemainingMs = 0L)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun updateAppColorSettings(settings: AppColorSettings) {
        val normalized = settings.copy(
            seedColorArgb = settings.seedColorArgb.withOpaqueAlpha(),
            lastNowPlayingAccentArgb = settings.lastNowPlayingAccentArgb.withOpaqueAlpha(),
            recentColorArgs = settings.recentColorArgs.map { it.withOpaqueAlpha() }.distinct().take(8)
        )
        _appColorSettings.value = normalized
        saveAppColorSettings(normalized)
    }

    fun setCustomSeedColor(argb: Long) {
        val color = argb.withOpaqueAlpha()
        val current = _appColorSettings.value
        updateAppColorSettings(
            current.copy(
                seedColorArgb = color,
                followNowPlayingAccentEnabled = false,
                recentColorArgs = listOf(color) + current.recentColorArgs.filterNot { it == color }
            )
        )
    }

    fun setFollowNowPlayingAccent(enabled: Boolean) {
        val current = _appColorSettings.value
        updateAppColorSettings(
            current.copy(
                followNowPlayingAccentEnabled = enabled,
                seedColorArgb = if (enabled) current.seedColorArgb else AppColorSettings.DEFAULT_SEED_COLOR
            )
        )
    }

    fun updateLastNowPlayingAccent(argb: Int) {
        val current = _appColorSettings.value
        if (!current.followNowPlayingAccentEnabled) return
        val color = argb.toLong().withOpaqueAlpha()
        if (current.lastNowPlayingAccentArgb != color) {
            updateAppColorSettings(current.copy(lastNowPlayingAccentArgb = color))
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        appInForeground = inForeground
        updateOverlayLyrics()
    }

    fun updateOverlayLyricsSettings(settings: OverlayLyricsSettings) {
        _overlayLyricsSettings.value = settings
        saveOverlayLyricsSettings(settings)
        if (!settings.enabled) {
            StatusLyricOverlayService.stop(appContext)
            lastOverlayText = null
        } else if (Settings.canDrawOverlays(appContext)) {
            StatusLyricOverlayService.config(appContext, settings)
        }
        updateOverlayLyrics()
        updateMediaMetadataLyrics()
    }

    fun updateFlowingBackgroundSettings(settings: FlowingBackgroundSettings) {
        val normalized = normalizeFlowingBackgroundSettings(settings)
        _flowingBackgroundSettings.value = normalized
        preferences.edit()
            .putBoolean(KEY_FLOWING_BACKGROUND_ENABLED, normalized.enabled)
            .putFloat(KEY_FLOWING_BACKGROUND_INTENSITY, normalized.intensity)
            .putBoolean(KEY_STAGE_PARTICLES_ENABLED, normalized.stageParticlesEnabled)
            .putBoolean(KEY_BEAT_REACTIVE_ENABLED, normalized.beatReactiveEnabled)
            .putBoolean(KEY_COVER_MOTION_ENABLED, normalized.coverMotionEnabled)
            .putBoolean(KEY_LYRIC_PARTICLES_ENABLED, normalized.lyricParticlesEnabled)
            .putBoolean(KEY_PROGRESS_PARTICLES_ENABLED, normalized.progressParticlesEnabled)
            .putBoolean(KEY_CAPSULE_COVER_ROTATION_ENABLED, normalized.capsuleCoverRotationEnabled)
            .putBoolean(KEY_COMPACT_LYRICS_PREVIEW_ENABLED, normalized.compactLyricsPreviewEnabled)
            .putBoolean(KEY_SONG_CHANGE_TRANSITION_ENABLED, normalized.songChangeTransitionEnabled)
            .putFloat(KEY_VISUALIZER_SENSITIVITY, normalized.visualizerSensitivity)
            .putString(KEY_ANIMATION_PERFORMANCE_MODE, normalized.performanceMode.name)
            .putString(KEY_PLAYER_PAGE_THEME, normalized.pageTheme.name)
            .apply()
    }

    private fun startPositionUpdates() {
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (musicController.isPlaying) {
                    val position = musicController.currentPosition
                    val duration = musicController.duration
                    val lyrics = _playerState.value.lyrics
                    _playerState.value = _playerState.value.copy(
                        isPlaying = true,
                        currentPosition = position,
                        duration = duration,
                        currentLyricIndex = currentLyricIndex(lyrics, position)
                    )
                } else {
                    _playerState.value = _playerState.value.copy(isPlaying = false)
                }
                updateOverlayLyrics()
                updateMediaMetadataLyrics()
                delay(180)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
        sleepTimerJob?.cancel()
        overlayPauseHideJob?.cancel()
        historyRecordJob?.cancel()
        StatusLyricOverlayService.stop(appContext)
        musicController.removeListener(playerListener)
        musicController.disconnect()
    }

    private fun setCurrentSong(song: Song, index: Int, queue: List<Song> = _playerState.value.queue) {
        _playerState.value = _playerState.value.copy(
            queue = if (queue.isEmpty()) _allSongs.value else queue,
            queueIndex = index,
            currentSong = song,
            currentPosition = 0L,
            lyrics = emptyList(),
            currentLyricIndex = -1
        )
        lastOverlayText = null
        lastMetadataText = null
        metadataLyricsApplied = false
        scheduleHistoryRecord(song)
        viewModelScope.launch {
            reloadLyricsForCurrentSong()
        }
    }

    private fun scheduleHistoryRecord(song: Song) {
        historyRecordJob?.cancel()
        historyRecordJob = viewModelScope.launch {
            delay(10_000L)
            val state = _playerState.value
            val now = System.currentTimeMillis()
            if (state.currentSong?.id == song.id &&
                musicController.currentPosition >= 9_000L &&
                (lastHistorySongId != song.id || now - lastHistoryAt > 10_000L)
            ) {
                lastHistorySongId = song.id
                lastHistoryAt = now
                if (!repository.recordPlayHistory(song)) {
                    _snackbarEvent.value = "播放历史写入失败"
                }
                _playHistory.value = repository.loadPlayHistory()
            }
        }
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim().ifBlank { "新建歌单" }
        val now = System.currentTimeMillis()
        val playlist = Playlist(id = now, name = trimmed, createdAt = now, updatedAt = now)
        updatePlaylists(_playlists.value + playlist)
    }

    fun createPlaylistWithSong(name: String, songId: String) {
        val trimmed = name.trim().ifBlank { "新建歌单" }
        val now = System.currentTimeMillis()
        val playlist = Playlist(id = now, name = trimmed, songIds = listOf(songId), createdAt = now, updatedAt = now)
        updatePlaylists(_playlists.value + playlist)
    }

    fun renamePlaylist(id: Long, name: String) {
        val trimmed = name.trim().ifBlank { return }
        updatePlaylists(_playlists.value.map { playlist ->
            if (playlist.id == id) playlist.copy(name = trimmed, updatedAt = System.currentTimeMillis()) else playlist
        })
    }

    fun deletePlaylist(id: Long) {
        updatePlaylists(_playlists.value.filterNot { it.id == id })
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        updatePlaylists(_playlists.value.map { playlist ->
            if (playlist.id == playlistId && songId !in playlist.songIds) {
                playlist.copy(songIds = playlist.songIds + songId, updatedAt = System.currentTimeMillis())
            } else {
                playlist
            }
        })
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        updatePlaylists(_playlists.value.map { playlist ->
            if (playlist.id == playlistId) playlist.copy(songIds = playlist.songIds.filterNot { it == songId }, updatedAt = System.currentTimeMillis()) else playlist
        })
    }

    fun moveSongInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
        updatePlaylists(_playlists.value.map { playlist ->
            if (playlist.id != playlistId || fromIndex !in playlist.songIds.indices || toIndex !in playlist.songIds.indices) {
                playlist
            } else {
                val ids = playlist.songIds.toMutableList()
                val moved = ids.removeAt(fromIndex)
                ids.add(toIndex, moved)
                playlist.copy(songIds = ids, updatedAt = System.currentTimeMillis())
            }
        })
    }

    fun playPlaylist(playlistId: Long, startIndex: Int = 0) {
        val playlist = _playlists.value.firstOrNull { it.id == playlistId } ?: return
        val songsById = _allSongs.value.associateBy { it.canonicalId }
        val songs = playlist.songIds.mapNotNull { songsById[it] }
        if (songs.isEmpty()) return
        val index = startIndex.coerceIn(songs.indices)
        setCurrentSong(songs[index], index, songs)
        musicController.playQueue(songs, index)
    }

    private fun updatePlaylists(playlists: List<Playlist>) {
        _playlists.value = playlists
        viewModelScope.launch {
            if (!repository.savePlaylists(playlists)) {
                _snackbarEvent.value = "歌单保存失败"
            }
        }
    }

    private suspend fun reloadLyricsForCurrentSong() {
        val song = _playerState.value.currentSong
        val lyrics = repository.loadLyricsFor(song)
        _playerState.value = _playerState.value.copy(
            lyrics = lyrics,
            currentLyricIndex = currentLyricIndex(lyrics, _playerState.value.currentPosition)
        )
        updateOverlayLyrics()
        updateMediaMetadataLyrics()
    }

    private fun updateOverlayLyrics() {
        val settings = _overlayLyricsSettings.value
        if (!settings.enabled || !Settings.canDrawOverlays(appContext)) {
            StatusLyricOverlayService.stop(appContext)
            lastOverlayText = null
            return
        }
        val state = _playerState.value
        if (settings.hideInApp && appInForeground) {
            StatusLyricOverlayService.hide(appContext)
            lastOverlayText = null
            return
        }
        val lyric = currentLyricText(state)
        if (state.currentSong == null || lyric.isBlank()) {
            StatusLyricOverlayService.hide(appContext)
            lastOverlayText = null
            return
        }
        if (state.isPlaying) {
            overlayPauseHideJob?.cancel()
            if (lastOverlayText != lyric) {
                StatusLyricOverlayService.update(appContext, lyric, visible = true, settings = settings)
                lastOverlayText = lyric
            }
        } else if (lastOverlayText != PAUSED_OVERLAY_TEXT) {
            StatusLyricOverlayService.update(appContext, PAUSED_OVERLAY_TEXT, visible = true, settings = settings)
            lastOverlayText = PAUSED_OVERLAY_TEXT
            overlayPauseHideJob?.cancel()
            overlayPauseHideJob = viewModelScope.launch {
                delay((settings.pauseHideDelaySeconds * 1000).toLong().coerceAtLeast(0L))
                StatusLyricOverlayService.hide(appContext)
                lastOverlayText = null
            }
        }
    }

    private fun updateMediaMetadataLyrics() {
        val settings = _overlayLyricsSettings.value
        val state = _playerState.value
        val song = state.currentSong
        val lyric = currentLyricText(state)
        if (!settings.mediaMetadataLyricsEnabled || song == null || lyric.isBlank()) {
            if (metadataLyricsApplied && song != null) {
                musicController.restoreCurrentMetadata(song)
            }
            lastMetadataText = null
            metadataLyricsApplied = false
            return
        }
        if (lastMetadataText != lyric) {
            if (musicController.updateCurrentMetadata(song, title = lyric, artist = "${song.title} · ${song.artist}")) {
                lastMetadataText = lyric
                metadataLyricsApplied = true
            }
        }
    }

    private fun currentLyricText(state: PlayerState): String {
        val index = state.currentLyricIndex
        return state.lyrics.getOrNull(index)?.primaryText.orEmpty().trim()
    }

    private fun currentLyricIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        val index = lines.indexOfLast { it.timeMs <= positionMs }
        return index.coerceAtLeast(0)
    }

    private fun mergeSongs(first: Iterable<Song>, second: Iterable<Song>): List<Song> {
        val merged = linkedMapOf<String, Song>()
        fun putSong(song: Song) {
            val existing = merged[song.canonicalId]
            if (existing?.imported == true && !song.imported) {
                merged[song.canonicalId] = song
            } else if (existing == null) {
                merged[song.canonicalId] = song
            }
        }
        first.forEach(::putSong)
        second.forEach(::putSong)
        return merged.values.sortedBy { it.title.lowercase() }
    }

    private fun Song.identityKeys(): List<String> {
        val titleKey = title.identityPart()
        val artistKey = artist.identityPart().takeUnless { it in UNKNOWN_ARTIST_KEYS }.orEmpty()
        val fileKey = sourcePath
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.identityPart()
            .orEmpty()
        val durationBucket = if (duration > 0) (duration / 1000L).toString() else "0"
        val durationBuckets = durationBuckets()
        return (
            durationBuckets.map { "meta:$titleKey:$artistKey:$it" } +
                durationBuckets.map { "file:$fileKey:$it" } +
                listOf(
                    "file:$fileKey",
                    "meta:$titleKey:$artistKey:$durationBucket",
                    "file:$fileKey:$durationBucket",
                    "uri:${uri}"
                )
            ).filterNot { key ->
            key.startsWith("meta::") || key.startsWith("file::")
        }
    }

    private fun Song.durationBuckets(): List<String> {
        if (duration <= 0) return listOf("0")
        val seconds = duration / 1000L
        return ((seconds - 2)..(seconds + 2)).map { it.coerceAtLeast(1L).toString() }.distinct()
    }

    private fun String.identityPart(): String {
        return lowercase()
            .replace(Regex("""\.[a-z0-9]{2,5}$"""), "")
            .replace(Regex("""\s+"""), "")
            .replace(Regex("""[《》<>\[\]【】()（）_\-.,，。'"]"""), "")
    }

    private suspend fun reloadImportedLibrary(): List<Song> {
        val imported = repository.loadImportedSongs()
        importedSongMap.clear()
        imported.forEach { song ->
            importedSongMap[song.canonicalId] = song
        }
        _importedSongs.value = imported
        _importedFolders.value = repository.loadImportedFolders()
        return imported
    }

    private fun loadThemeMode(): ThemeMode {
        val value = preferences.getString(KEY_THEME_MODE, ThemeMode.System.name)
        return ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.System
    }

    private fun loadEqualizerState(): EqualizerState {
        val levels = preferences.getString(KEY_EQ_BANDS, null)
            ?.split(',')
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 5 }
            ?: List(5) { 0f }
        return EqualizerState(
            enabled = preferences.getBoolean(KEY_EQ_ENABLED, false),
            bandLevels = levels,
            preset = preferences.getString(KEY_EQ_PRESET, "自定义") ?: "自定义",
            hifiModeEnabled = preferences.getBoolean(KEY_HIFI_MODE, false),
            bassBoostEnabled = preferences.getBoolean(KEY_BASS_ENABLED, false),
            bassBoostStrength = preferences.getFloat(KEY_BASS_STRENGTH, 0.45f),
            virtualizerEnabled = preferences.getBoolean(KEY_VIRTUALIZER_ENABLED, false),
            virtualizerStrength = preferences.getFloat(KEY_VIRTUALIZER_STRENGTH, 0.35f),
            loudnessEnabled = preferences.getBoolean(KEY_LOUDNESS_ENABLED, false),
            loudnessGain = preferences.getFloat(KEY_LOUDNESS_GAIN, 0.35f)
        )
    }

    private fun saveEqualizerState(state: EqualizerState) {
        preferences.edit()
            .putBoolean(KEY_EQ_ENABLED, state.enabled)
            .putString(KEY_EQ_BANDS, state.bandLevels.joinToString(","))
            .putString(KEY_EQ_PRESET, state.preset)
            .putBoolean(KEY_HIFI_MODE, state.hifiModeEnabled)
            .putBoolean(KEY_BASS_ENABLED, state.bassBoostEnabled)
            .putFloat(KEY_BASS_STRENGTH, state.bassBoostStrength)
            .putBoolean(KEY_VIRTUALIZER_ENABLED, state.virtualizerEnabled)
            .putFloat(KEY_VIRTUALIZER_STRENGTH, state.virtualizerStrength)
            .putBoolean(KEY_LOUDNESS_ENABLED, state.loudnessEnabled)
            .putFloat(KEY_LOUDNESS_GAIN, state.loudnessGain)
            .apply()
    }

    private fun loadOverlayLyricsSettings() = OverlayLyricsSettings(
        enabled = preferences.getBoolean(KEY_OVERLAY_ENABLED, false),
        hideInApp = preferences.getBoolean(KEY_OVERLAY_HIDE_IN_APP, true),
        mediaMetadataLyricsEnabled = preferences.getBoolean(KEY_METADATA_LYRICS_ENABLED, false),
        textColorArgb = preferences.getLong(KEY_OVERLAY_TEXT_COLOR, 0xFFFFFFFF),
        offsetTopDp = preferences.getFloat(KEY_OVERLAY_TOP, 0f),
        offsetBottomDp = preferences.getFloat(KEY_OVERLAY_BOTTOM, 0f),
        offsetLeftDp = preferences.getFloat(KEY_OVERLAY_LEFT, 0f),
        offsetRightDp = preferences.getFloat(KEY_OVERLAY_RIGHT, 0f),
        widthDp = preferences.getFloat(KEY_OVERLAY_WIDTH, 320f),
        fontSizeSp = preferences.getFloat(KEY_OVERLAY_FONT, 14f),
        pauseHideDelaySeconds = preferences.getFloat(KEY_OVERLAY_DELAY, 2f)
    )

    private fun loadFlowingBackgroundSettings() = FlowingBackgroundSettings(
        enabled = preferences.getBoolean(KEY_FLOWING_BACKGROUND_ENABLED, true),
        intensity = preferences.getFloat(KEY_FLOWING_BACKGROUND_INTENSITY, 0.72f).coerceIn(0.2f, 1f),
        stageParticlesEnabled = false,
        beatReactiveEnabled = preferences.getBoolean(KEY_BEAT_REACTIVE_ENABLED, true),
        coverMotionEnabled = false,
        lyricParticlesEnabled = false,
        progressParticlesEnabled = false,
        capsuleCoverRotationEnabled = preferences.getBoolean(KEY_CAPSULE_COVER_ROTATION_ENABLED, true),
        compactLyricsPreviewEnabled = preferences.getBoolean(KEY_COMPACT_LYRICS_PREVIEW_ENABLED, true),
        songChangeTransitionEnabled = preferences.getBoolean(KEY_SONG_CHANGE_TRANSITION_ENABLED, false),
        visualizerSensitivity = preferences.getFloat(KEY_VISUALIZER_SENSITIVITY, 0.72f).coerceIn(0.2f, 1.5f),
        performanceMode = preferences.getString(KEY_ANIMATION_PERFORMANCE_MODE, PlayerAnimationPerformanceMode.Atmosphere.name)
            ?.let { value -> PlayerAnimationPerformanceMode.entries.firstOrNull { it.name == value } }
            ?: PlayerAnimationPerformanceMode.Atmosphere,
        pageTheme = preferences.getString(KEY_PLAYER_PAGE_THEME, PlayerPageTheme.Current.name)
            ?.let { value -> PlayerPageTheme.entries.firstOrNull { it.name == value } }
            ?: PlayerPageTheme.Current
    ).let(::normalizeFlowingBackgroundSettings)

    private fun normalizeFlowingBackgroundSettings(settings: FlowingBackgroundSettings): FlowingBackgroundSettings {
        val mode = settings.performanceMode
        val lowPower = mode == PlayerAnimationPerformanceMode.Low
        return settings.copy(
            enabled = settings.enabled && !lowPower,
            intensity = settings.intensity.coerceIn(0.2f, 1f),
            stageParticlesEnabled = false,
            beatReactiveEnabled = settings.beatReactiveEnabled && !lowPower,
            coverMotionEnabled = false,
            lyricParticlesEnabled = false,
            progressParticlesEnabled = false,
            capsuleCoverRotationEnabled = settings.capsuleCoverRotationEnabled && !lowPower,
            compactLyricsPreviewEnabled = settings.compactLyricsPreviewEnabled,
            songChangeTransitionEnabled = settings.songChangeTransitionEnabled,
            pageTheme = settings.pageTheme,
            visualizerSensitivity = settings.visualizerSensitivity.coerceIn(0.2f, 1.5f)
        )
    }

    private fun saveOverlayLyricsSettings(settings: OverlayLyricsSettings) {
        preferences.edit()
            .putBoolean(KEY_OVERLAY_ENABLED, settings.enabled)
            .putBoolean(KEY_OVERLAY_HIDE_IN_APP, settings.hideInApp)
            .putBoolean(KEY_METADATA_LYRICS_ENABLED, settings.mediaMetadataLyricsEnabled)
            .putLong(KEY_OVERLAY_TEXT_COLOR, settings.textColorArgb)
            .putFloat(KEY_OVERLAY_TOP, settings.offsetTopDp)
            .putFloat(KEY_OVERLAY_BOTTOM, settings.offsetBottomDp)
            .putFloat(KEY_OVERLAY_LEFT, settings.offsetLeftDp)
            .putFloat(KEY_OVERLAY_RIGHT, settings.offsetRightDp)
            .putFloat(KEY_OVERLAY_WIDTH, settings.widthDp)
            .putFloat(KEY_OVERLAY_FONT, settings.fontSizeSp)
            .putFloat(KEY_OVERLAY_DELAY, settings.pauseHideDelaySeconds)
            .apply()
    }

    private fun loadAppColorSettings(): AppColorSettings {
        val recent = preferences.getString(KEY_RECENT_COLORS, null)
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()
        return AppColorSettings(
            seedColorArgb = preferences.getLong(KEY_APP_SEED_COLOR, AppColorSettings.DEFAULT_SEED_COLOR),
            recentColorArgs = recent,
            followNowPlayingAccentEnabled = preferences.getBoolean(KEY_FOLLOW_NOW_PLAYING_ACCENT, false),
            lastNowPlayingAccentArgb = preferences.getLong(KEY_LAST_NOW_PLAYING_ACCENT, AppColorSettings.DEFAULT_SEED_COLOR)
        )
    }

    private fun saveAppColorSettings(settings: AppColorSettings) {
        preferences.edit()
            .putLong(KEY_APP_SEED_COLOR, settings.seedColorArgb)
            .putString(KEY_RECENT_COLORS, settings.recentColorArgs.joinToString(","))
            .putBoolean(KEY_FOLLOW_NOW_PLAYING_ACCENT, settings.followNowPlayingAccentEnabled)
            .putLong(KEY_LAST_NOW_PLAYING_ACCENT, settings.lastNowPlayingAccentArgb)
            .apply()
    }

    private fun Long.withOpaqueAlpha(): Long = (this and 0x00FFFFFFL) or 0xFF000000L

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_APP_SEED_COLOR = "app_seed_color"
        const val KEY_RECENT_COLORS = "recent_colors"
        const val KEY_FOLLOW_NOW_PLAYING_ACCENT = "follow_now_playing_accent"
        const val KEY_LAST_NOW_PLAYING_ACCENT = "last_now_playing_accent"
        const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        const val KEY_OVERLAY_HIDE_IN_APP = "overlay_hide_in_app"
        const val KEY_METADATA_LYRICS_ENABLED = "metadata_lyrics_enabled"
        const val KEY_OVERLAY_TEXT_COLOR = "overlay_text_color"
        const val KEY_OVERLAY_TOP = "overlay_top"
        const val KEY_OVERLAY_BOTTOM = "overlay_bottom"
        const val KEY_OVERLAY_LEFT = "overlay_left"
        const val KEY_OVERLAY_RIGHT = "overlay_right"
        const val KEY_OVERLAY_WIDTH = "overlay_width"
        const val KEY_OVERLAY_FONT = "overlay_font"
        const val KEY_OVERLAY_DELAY = "overlay_delay"
        const val KEY_EQ_ENABLED = "eq_enabled"
        const val KEY_EQ_BANDS = "eq_bands"
        const val KEY_EQ_PRESET = "eq_preset"
        const val KEY_HIFI_MODE = "hifi_mode_enabled"
        const val KEY_BASS_ENABLED = "bass_boost_enabled"
        const val KEY_BASS_STRENGTH = "bass_boost_strength"
        const val KEY_VIRTUALIZER_ENABLED = "virtualizer_enabled"
        const val KEY_VIRTUALIZER_STRENGTH = "virtualizer_strength"
        const val KEY_LOUDNESS_ENABLED = "loudness_enabled"
        const val KEY_LOUDNESS_GAIN = "loudness_gain"
        const val KEY_FLOWING_BACKGROUND_ENABLED = "flowing_background_enabled"
        const val KEY_FLOWING_BACKGROUND_INTENSITY = "flowing_background_intensity"
        const val KEY_STAGE_PARTICLES_ENABLED = "stage_particles_enabled"
        const val KEY_BEAT_REACTIVE_ENABLED = "beat_reactive_enabled"
        const val KEY_COVER_MOTION_ENABLED = "cover_motion_enabled"
        const val KEY_LYRIC_PARTICLES_ENABLED = "lyric_particles_enabled"
        const val KEY_PROGRESS_PARTICLES_ENABLED = "progress_particles_enabled"
        const val KEY_CAPSULE_COVER_ROTATION_ENABLED = "capsule_cover_rotation_enabled"
        const val KEY_COMPACT_LYRICS_PREVIEW_ENABLED = "compact_lyrics_preview_enabled"
        const val KEY_SONG_CHANGE_TRANSITION_ENABLED = "song_change_transition_enabled"
        const val KEY_VISUALIZER_SENSITIVITY = "visualizer_sensitivity"
        const val KEY_ANIMATION_PERFORMANCE_MODE = "animation_performance_mode"
        const val KEY_PLAYER_PAGE_THEME = "player_page_theme"
        const val PAUSED_OVERLAY_TEXT = "播放已暂停，将关闭状态栏歌词"
        val UNKNOWN_ARTIST_KEYS = setOf("", "未知艺术家", "unknownartist", "unknown")
    }
}

data class BilingualLyricsIndexState(
    val running: Boolean = false,
    val processed: Int = 0,
    val total: Int = 0,
    val currentTitle: String = "",
    val summary: BilingualLyricsIndexSummary? = null,
    val message: String = "尚未索引"
)

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Content(val count: Int) : LibraryUiState
    data class PermissionDenied(val permission: String = "媒体读取权限") : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}
