package com.musicplayer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.data.ImportedFolder
import com.musicplayer.data.Song
import com.musicplayer.ui.components.AlbumArtImage
import com.musicplayer.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportManagerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val importedSongs by viewModel.importedSongs.collectAsState()
    val importedFolders by viewModel.importedFolders.collectAsState()
    val isImportingFolder by viewModel.isImportingFolder.collectAsState()
    val snackbarEvent by viewModel.snackbarEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val directSongs = importedSongs.filter { it.folderName == "导入歌曲" }
    val importSongsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importSongs(uris)
    }
    val importLyricsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importLyrics(uris)
    }
    val importFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.importFolder(uri)
    }

    LaunchedEffect(snackbarEvent) {
        val message = snackbarEvent ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeSnackbar()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text("导入管理", fontWeight = FontWeight.SemiBold)
                        Text("${importedSongs.size} 首导入歌曲 · ${importedFolders.size} 个文件夹", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { importSongsLauncher.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AudioFile, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("导入歌曲")
                        }
                        FilledTonalButton(
                            onClick = { importFolderLauncher.launch(null) },
                            enabled = !isImportingFolder,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(if (isImportingFolder) "扫描中" else "导入文件夹")
                        }
                    }
                    OutlinedButton(
                        onClick = { importLyricsLauncher.launch(arrayOf("application/octet-stream", "text/*", "audio/x-lrc", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("导入歌词")
                    }
                }
            }
            item { SectionTitle("已导入文件夹") }
            if (importedFolders.isEmpty()) {
                item { EmptyImportCard("还没有导入文件夹") }
            } else {
                items(importedFolders, key = { it.uri.toString() }) { folder ->
                    FolderRow(folder = folder, onRemove = { viewModel.removeImportedFolder(folder) })
                }
            }
            item { SectionTitle("已导入单曲") }
            if (directSongs.isEmpty()) {
                item { EmptyImportCard("还没有导入单曲") }
            } else {
                items(directSongs, key = { it.id }) { song ->
                    ImportedSongRow(song = song, onRemove = { viewModel.removeImportedSong(song) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun EmptyImportCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FolderRow(folder: ImportedFolder, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(folder.uri.toString(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "移除文件夹")
            }
        }
    }
}

@Composable
private fun ImportedSongRow(song: Song, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumArtImage(song = song, modifier = Modifier.size(52.dp), fallbackModifier = Modifier.size(30.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "移除歌曲")
            }
        }
    }
}
