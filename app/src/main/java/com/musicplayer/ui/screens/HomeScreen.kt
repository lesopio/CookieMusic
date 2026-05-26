package com.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.data.Playlist
import com.musicplayer.data.Song
import com.musicplayer.ui.components.SongItem
import com.musicplayer.viewmodel.PlayerViewModel

private enum class LibraryTab(val title: String, val icon: ImageVector) {
    Songs("歌曲", Icons.Default.MusicNote),
    Artists("歌手", Icons.Default.Person),
    Albums("专辑", Icons.Default.Album),
    Folders("文件夹", Icons.Default.Folder)
}

@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    onSongClick: (Song) -> Unit,
    windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(400.dp, 800.dp))
) {
    val songs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var currentTab by remember { mutableStateOf(LibraryTab.Songs) }
    var addSong by remember { mutableStateOf<Song?>(null) }
    val isLandscape = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded

    val sortedSongs = remember(songs) { songs.sortedBy { it.title.lowercase() } }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text("饼干音乐", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("${songs.size} 首歌曲", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ScrollableTabRow(selectedTabIndex = currentTab.ordinal, edgePadding = 16.dp) {
            LibraryTab.entries.forEach { tab ->
                Tab(
                    selected = currentTab == tab,
                    onClick = { currentTab = tab },
                    text = { Text(tab.title) },
                    icon = { Icon(tab.icon, contentDescription = null) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (songs.isEmpty()) {
                EmptyState()
            } else {
                when (currentTab) {
                    LibraryTab.Songs -> SongGrid(sortedSongs, favorites, viewModel, onSongClick, { addSong = it }, isLandscape)
                    LibraryTab.Artists -> GroupList(sortedSongs.groupBy { it.artist.ifBlank { "未知艺术家" } }, Icons.Default.Person, favorites, viewModel, onSongClick) { addSong = it }
                    LibraryTab.Albums -> GroupList(sortedSongs.groupBy { it.album.ifBlank { "未知专辑" } }, Icons.Default.Album, favorites, viewModel, onSongClick) { addSong = it }
                    LibraryTab.Folders -> GroupList(sortedSongs.groupBy { it.folderName.ifBlank { "本地音乐" } }, Icons.Default.Folder, favorites, viewModel, onSongClick) { addSong = it }
                }
            }
        }
    }

    addSong?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = { addSong = null },
            onAdd = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, song.id)
                addSong = null
            },
            onCreate = { name ->
                viewModel.createPlaylistWithSong(name, song.id)
                addSong = null
            }
        )
    }
}

@Composable
fun SearchScreen(
    viewModel: PlayerViewModel,
    onSongClick: (Song) -> Unit,
    windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(400.dp, 800.dp))
) {
    val songs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var query by remember { mutableStateOf("") }
    var addSong by remember { mutableStateOf<Song?>(null) }
    val isLandscape = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded

    val results = remember(songs, query) {
        songs.filter { song ->
            query.isBlank() ||
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true) ||
                song.album.contains(query, ignoreCase = true) ||
                song.folderName.contains(query, ignoreCase = true)
        }.sortedBy { it.title.lowercase() }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 12.dp)) {
        SearchHeader(query = query, onQueryChange = { query = it })
        SongGrid(results, favorites, viewModel, onSongClick, { addSong = it }, isLandscape)
    }

    addSong?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = { addSong = null },
            onAdd = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, song.id)
                addSong = null
            },
            onCreate = { name ->
                viewModel.createPlaylistWithSong(name, song.id)
                addSong = null
            }
        )
    }
}

@Composable
private fun SearchHeader(query: String, onQueryChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("搜索歌曲、歌手、专辑或文件夹") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
private fun SongGrid(
    songs: List<Song>,
    favorites: Set<Long>,
    viewModel: PlayerViewModel,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    isLandscape: Boolean
) {
    if (songs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有找到匹配的歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    if (isLandscape) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(220.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongItem(song, favorites.contains(song.id), { onSongClick(song) }, { viewModel.toggleFavorite(song.id) }, onLongClick = { onSongLongClick(song) })
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongItem(song, favorites.contains(song.id), { onSongClick(song) }, { viewModel.toggleFavorite(song.id) }, onLongClick = { onSongLongClick(song) })
            }
        }
    }
}

@Composable
private fun GroupList(
    groups: Map<String, List<Song>>,
    icon: ImageVector,
    favorites: Set<Long>,
    viewModel: PlayerViewModel,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    var expandedGroup by remember { mutableStateOf<String?>(null) }
    val sortedEntries = remember(groups) { groups.toSortedMap().entries.toList() }
    val expandedSongs = remember(expandedGroup, groups) {
        expandedGroup?.let { key -> groups[key]?.sortedBy { it.title.lowercase() } }.orEmpty()
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sortedEntries, key = { it.key }) { entry ->
            GroupCard(entry.key, entry.value.size, icon, expandedGroup == entry.key) {
                expandedGroup = if (expandedGroup == entry.key) null else entry.key
            }
        }
        if (expandedSongs.isNotEmpty()) {
            itemsIndexed(expandedSongs, key = { _, song -> "expanded-${song.id}" }) { _, song ->
                SongItem(
                    song = song,
                    isFavorite = favorites.contains(song.id),
                    onClick = { onSongClick(song) },
                    onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                    modifier = Modifier.padding(start = 12.dp),
                    onLongClick = { onSongLongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun GroupCard(title: String, count: Int, icon: ImageVector, expanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(if (expanded) "已展开 · $count 首歌曲" else "$count 首歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.height(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text("还没有歌曲", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text("前往设置中的导入管理添加本地音乐，或执行全盘扫描。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onAdd: (Long) -> Unit,
    onCreate: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加到歌单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                playlists.forEach { playlist ->
                    DropdownMenuItem(
                        text = { Text(playlist.name) },
                        onClick = { onAdd(playlist.id) }
                    )
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("新建歌单") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(newName.ifBlank { "新建歌单" }) }) {
                Text("新建并添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
