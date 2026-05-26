package com.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.data.Playlist
import com.musicplayer.data.Song
import com.musicplayer.ui.components.SongItem
import com.musicplayer.viewmodel.PlayerViewModel

private enum class FavoriteRootTab(val title: String) {
    Favorites("收藏"),
    Playlists("歌单")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onOpenPlayer: () -> Unit,
    windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(400.dp, 800.dp))
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var currentTab by remember { mutableStateOf(FavoriteRootTab.Favorites) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var playlistDialog by remember { mutableStateOf<Playlist?>(null) }
    var createDialog by remember { mutableStateOf(false) }
    val selectedPlaylist = selectedPlaylistId?.let { id -> playlists.firstOrNull { it.id == id } }
    val isLandscape = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(selectedPlaylist?.name ?: "我的收藏") },
                navigationIcon = {
                    if (selectedPlaylist != null) {
                        IconButton(onClick = { selectedPlaylistId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (selectedPlaylist != null) {
                        IconButton(onClick = { viewModel.playPlaylist(selectedPlaylist.id, 0); onOpenPlayer() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "播放歌单")
                        }
                        IconButton(onClick = { playlistDialog = selectedPlaylist }) {
                            Icon(Icons.Default.Edit, contentDescription = "重命名")
                        }
                        IconButton(onClick = { viewModel.deletePlaylist(selectedPlaylist.id); selectedPlaylistId = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (currentTab == FavoriteRootTab.Playlists && selectedPlaylist == null) {
                FloatingActionButton(onClick = { createDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "新建歌单")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (selectedPlaylist == null) {
                TabRow(selectedTabIndex = currentTab.ordinal) {
                    FavoriteRootTab.entries.forEach { tab ->
                        Tab(selected = currentTab == tab, onClick = { currentTab = tab }, text = { Text(tab.title) })
                    }
                }
            }
            when {
                selectedPlaylist != null -> PlaylistDetail(selectedPlaylist, allSongs, viewModel, onOpenPlayer)
                currentTab == FavoriteRootTab.Favorites -> FavoriteSongs(allSongs.filter { favorites.contains(it.id) }, favorites, viewModel, onSongClick, isLandscape)
                else -> PlaylistList(playlists, onOpen = { selectedPlaylistId = it.id })
            }
        }
    }

    if (createDialog) {
        PlaylistNameDialog(
            title = "新建歌单",
            initialName = "",
            onDismiss = { createDialog = false },
            onConfirm = {
                viewModel.createPlaylist(it)
                createDialog = false
            }
        )
    }
    playlistDialog?.let { playlist ->
        PlaylistNameDialog(
            title = "重命名歌单",
            initialName = playlist.name,
            onDismiss = { playlistDialog = null },
            onConfirm = {
                viewModel.renamePlaylist(playlist.id, it)
                playlistDialog = null
            }
        )
    }
}

@Composable
private fun FavoriteSongs(
    songs: List<Song>,
    favorites: Set<Long>,
    viewModel: PlayerViewModel,
    onSongClick: (Song) -> Unit,
    isLandscape: Boolean
) {
    if (songs.isEmpty()) {
        EmptyFavoriteState("还没有收藏歌曲")
    } else if (isLandscape) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(200.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongItem(song, favorites.contains(song.id), { onSongClick(song) }, { viewModel.toggleFavorite(song.id) })
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongItem(song, favorites.contains(song.id), { onSongClick(song) }, { viewModel.toggleFavorite(song.id) })
            }
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    onOpen: (Playlist) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyFavoriteState("还没有歌单")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(playlist) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        Text("${playlist.songIds.size} 首歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    playlist: Playlist,
    allSongs: List<Song>,
    viewModel: PlayerViewModel,
    onOpenPlayer: () -> Unit
) {
    val songsById = remember(allSongs) { allSongs.associateBy { it.id } }
    val songs = playlist.songIds.mapNotNull { songsById[it] }
    if (songs.isEmpty()) {
        EmptyFavoriteState("歌单里还没有可播放歌曲")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            val index = songs.indexOf(song)
            var dragOffset by remember(song.id, index) { mutableFloatStateOf(0f) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.pointerInput(playlist.id, song.id, index, songs.size) {
                    detectDragGesturesAfterLongPress(
                        onDragEnd = { dragOffset = 0f },
                        onDragCancel = { dragOffset = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                        val threshold = 72.dp.toPx()
                        if (dragOffset > threshold && index < songs.lastIndex) {
                            viewModel.moveSongInPlaylist(playlist.id, index, index + 1)
                            dragOffset = 0f
                        } else if (dragOffset < -threshold && index > 0) {
                            viewModel.moveSongInPlaylist(playlist.id, index, index - 1)
                            dragOffset = 0f
                        }
                    }
                }
            ) {
                Column(Modifier.weight(1f)) {
                    SongItem(
                        song = song,
                        isFavorite = viewModel.isFavorite(song.id),
                        onClick = {
                            viewModel.playPlaylist(playlist.id, index)
                            onOpenPlayer()
                        },
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                    )
                }
                Column {
                    IconButton(onClick = { if (index > 0) viewModel.moveSongInPlaylist(playlist.id, index, index - 1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                    }
                    IconButton(onClick = { if (index < songs.lastIndex) viewModel.moveSongInPlaylist(playlist.id, index, index + 1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                    }
                    IconButton(onClick = { viewModel.removeSongFromPlaylist(playlist.id, song.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "移除")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoriteState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("歌单名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
