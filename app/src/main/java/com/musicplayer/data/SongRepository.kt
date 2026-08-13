package com.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class SongRepository(private val context: Context) {

    private val preferences = context.getSharedPreferences("music_library", Context.MODE_PRIVATE)
    private val dao = MusicDatabase.get(context).libraryDao()
    private val libraryMutex = Mutex()

    suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                -1
            }
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val sourcePath = dataCol.takeIf { it >= 0 }?.let { cursor.getString(it) }
                val displayName = cursor.getString(displayNameCol).orEmpty()
                val fileSize = cursor.getLong(sizeCol).takeIf { it > 0L }
                val folderName = relativePathCol.takeIf { it >= 0 }?.let { cursor.getString(it) }
                    ?.trimEnd('/')
                    ?.substringAfterLast('/')
                    ?.takeIf { it.isNotBlank() }
                    ?: sourcePath?.substringBeforeLast('/', "")?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                    ?: "本地音乐"
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                )
                songs.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "未知歌曲",
                        artist = cursor.getString(artistCol) ?: "未知艺术家",
                        album = cursor.getString(albumCol) ?: "未知专辑",
                        duration = cursor.getLong(durationCol),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                        albumArtUri = albumArtUri,
                        trackNumber = cursor.getInt(trackCol),
                        sourcePath = sourcePath,
                        folderName = folderName,
                        audioInfo = audioInfoFromMediaStore(sourcePath, cursor.getLong(durationCol))
                    )
                )
            }
        }
        libraryMutex.withLock {
            canonicalizeSongs(songs, SourceKind.MediaStore, parentRootUri = null)
        }
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        scanSongs().filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    suspend fun loadCachedSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = dao.songs().associateBy { it.canonicalId }
        val roots = dao.importRoots().associateBy { it.uri }
        dao.sources()
            .filter { it.availability == SourceAvailability.Available.name }
            .groupBy { it.canonicalId }
            .mapNotNull { (canonicalId, sources) ->
                val metadata = songs[canonicalId] ?: return@mapNotNull null
                val source = sources.firstOrNull { it.kind == SourceKind.MediaStore.name } ?: sources.firstOrNull() ?: return@mapNotNull null
                val uri = Uri.parse(source.sourceUri)
                Song(
                    id = legacySourceId(source.sourceUri),
                    canonicalId = canonicalId,
                    title = metadata.title,
                    artist = metadata.artist,
                    album = metadata.album,
                    duration = metadata.durationMs,
                    uri = uri,
                    sourcePath = source.displayName,
                    folderName = source.parentRootUri?.let { root -> roots[root]?.displayName }
                        ?: if (source.kind == SourceKind.MediaStore.name) "本地音乐" else "导入歌曲",
                    imported = source.kind != SourceKind.MediaStore.name,
                    audioInfo = AudioInfo(fileSizeBytes = metadata.fileSizeBytes)
                )
            }
    }

    suspend fun importSongs(uris: List<Uri>): List<Song> = withContext(Dispatchers.IO) {
        libraryMutex.withLock {
            val now = System.currentTimeMillis()
            val imported = uris.mapNotNull { uri ->
                if (!persistReadPermission(uri)) return@mapNotNull null
                runCatching { uri to songFromUri(uri) }.getOrNull()
            }
            saveImportedUris(imported.map { it.first })
            imported.forEach { (uri, song) ->
                dao.upsertRoot(
                    ImportedRootEntity(
                        uri = uri.toString(),
                        kind = ImportRootKind.File.name,
                        displayName = song.sourcePath ?: song.title,
                        availability = SourceAvailability.Available.name,
                        lastError = null,
                        createdAt = now,
                        lastValidatedAt = now
                    )
                )
            }
            canonicalizeSongs(imported.map { it.second }, SourceKind.SafFile, parentRootUri = null)
        }
    }

    suspend fun importFolder(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        libraryMutex.withLock {
            if (!persistReadPermission(uri)) throw SecurityException("无法保留文件夹读取权限")
            saveImportedFolder(uri)
            val now = System.currentTimeMillis()
            dao.upsertRoot(
                ImportedRootEntity(
                    uri = uri.toString(),
                    kind = ImportRootKind.Tree.name,
                    displayName = queryDisplayName(uri),
                    availability = SourceAvailability.Available.name,
                    lastError = null,
                    createdAt = now,
                    lastValidatedAt = now
                )
            )
            val scanned = scanFolderUri(uri)
            scanned.copy(songs = canonicalizeSongs(scanned.songs, SourceKind.SafTreeChild, uri.toString()))
        }
    }

    suspend fun importLyrics(uris: List<Uri>, songs: List<Song>): Int = withContext(Dispatchers.IO) {
        var imported = 0
        uris.forEach { uri ->
            runCatching {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val name = queryDisplayName(uri)
                val key = normalizeName(name.substringBeforeLast('.', name))
                val extension = name.substringAfterLast('.', "").lowercase()
                val values = preferences.getStringSet(KEY_LYRIC_URIS, emptySet()).orEmpty().toMutableSet()
                values.add("$key|$extension|$uri")
                preferences.edit().putStringSet(KEY_LYRIC_URIS, values).apply()
                val matchedSong = songs.firstOrNull { song -> normalizeSong(song) == key || normalizeName(song.title) == key }
                if (matchedSong != null) bindLyrics(matchedSong.id, uri)
                imported += 1
            }
        }
        imported
    }

    suspend fun loadImportedSongs(): List<Song> = withContext(Dispatchers.IO) {
        libraryMutex.withLock {
            ensureLegacyImportsMigrated()
            val roots = dao.importRoots()
            val directSongs = mutableListOf<Song>()
            val folderSongs = mutableListOf<Song>()
            roots.forEach { root ->
                val result = runCatching {
                    when (ImportRootKind.valueOf(root.kind)) {
                        ImportRootKind.File -> directSongs += canonicalizeSongs(
                            listOf(songFromUri(Uri.parse(root.uri))), SourceKind.SafFile, null
                        )
                        ImportRootKind.Tree -> folderSongs += canonicalizeSongs(
                            scanFolderUri(Uri.parse(root.uri)).songs, SourceKind.SafTreeChild, root.uri
                        )
                    }
                }
                val error = result.exceptionOrNull()
                dao.upsertRoot(root.copy(
                    availability = if (error == null) SourceAvailability.Available.name else sourceAvailability(error).name,
                    lastError = error?.message,
                    lastValidatedAt = System.currentTimeMillis()
                ))
            }
            (directSongs + folderSongs).distinctBy { it.uri.toString() }
        }
    }

    suspend fun loadImportedFolders(): List<ImportedFolder> = withContext(Dispatchers.IO) {
        ensureLegacyImportsMigrated()
        dao.importRoots().filter { it.kind == ImportRootKind.Tree.name }.map {
            ImportedFolder(uri = Uri.parse(it.uri), name = it.displayName, availability = it.availability, lastError = it.lastError)
        }
    }

    suspend fun removeImportedSong(song: Song): List<Song> = withContext(Dispatchers.IO) {
        val values = preferences.getStringSet(KEY_IMPORTED_URIS, emptySet()).orEmpty().toMutableSet()
        values.remove(song.uri.toString())
        preferences.edit().putStringSet(KEY_IMPORTED_URIS, values).apply()
        dao.deleteSource(song.uri.toString())
        dao.deleteRoot(song.uri.toString())
        releaseReadPermissionIfUnused(song.uri)
        loadImportedSongs()
    }

    suspend fun removeImportedFolder(folder: ImportedFolder): List<Song> = withContext(Dispatchers.IO) {
        val values = preferences.getStringSet(KEY_IMPORTED_FOLDER_URIS, emptySet()).orEmpty().toMutableSet()
        values.remove(folder.uri.toString())
        preferences.edit().putStringSet(KEY_IMPORTED_FOLDER_URIS, values).apply()
        dao.deleteSourcesForRoot(folder.uri.toString())
        dao.deleteRoot(folder.uri.toString())
        releaseReadPermissionIfUnused(folder.uri)
        loadImportedSongs()
    }

    private fun songFromUri(uri: Uri, folderName: String = "导入歌曲"): Song {
        val displayName = queryDisplayName(uri)
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: displayName.substringBeforeLast('.', displayName)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: "未知艺术家"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?: "未知专辑"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val audioInfo = buildAudioInfo(uri, retriever, displayName, duration)
            Song(
                id = -kotlin.math.abs(uri.toString().hashCode().toLong()),
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                uri = uri,
                sourcePath = displayName,
                folderName = folderName,
                imported = true,
                audioInfo = audioInfo
            )
        } finally {
            retriever.release()
        }
    }

    suspend fun loadFavorites(): Set<String> = withContext(Dispatchers.IO) {
        val existing = dao.favorites().map { it.canonicalId }.toSet()
        if (existing.isNotEmpty()) return@withContext existing
        val legacy = preferences.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        val migrated = dao.sources().filter { source -> legacySourceId(source.sourceUri) in legacy }
            .map { it.canonicalId }.toSet()
        migrated.forEach { dao.upsertFavorite(FavoriteEntity(it)) }
        migrated
    }

    suspend fun saveFavorites(ids: Set<String>) = withContext(Dispatchers.IO) {
        dao.clearFavorites()
        ids.forEach { dao.upsertFavorite(FavoriteEntity(it)) }
    }

    suspend fun loadPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val stored = dao.playlists()
        if (stored.isNotEmpty()) {
            val songs = dao.playlistSongs().groupBy { it.playlistId }
            return@withContext stored.map { playlist ->
                Playlist(
                    id = playlist.id,
                    name = playlist.name,
                    songIds = songs[playlist.id].orEmpty().sortedBy { it.position }.map { it.canonicalId },
                    createdAt = playlist.createdAt,
                    updatedAt = playlist.updatedAt
                )
            }
        }
        val file = playlistFile()
        if (!file.exists()) return@withContext emptyList()
        val legacyToCanonical = dao.sources().associate { legacySourceId(it.sourceUri) to it.canonicalId }
        val migrated = runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val ids = item.optJSONArray("songIds")
                Playlist(
                    id = item.optLong("id"),
                    name = item.optString("name", "未命名歌单"),
                    songIds = if (ids == null) emptyList() else (0 until ids.length()).mapNotNull { position ->
                        val raw = ids.optString(position)
                        raw.takeIf { it.isNotBlank() && !it.all(Char::isDigit) }
                            ?: raw.toLongOrNull()?.let { legacyToCanonical[it] }
                    },
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            }
        }.getOrDefault(emptyList())
        if (migrated.isNotEmpty()) dao.replacePlaylists(migrated)
        migrated
    }

    suspend fun savePlaylists(playlists: List<Playlist>): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            dao.replacePlaylists(playlists)
            val file = playlistFile()
            val dir = file.parentFile ?: return@runCatching false
            if (!dir.exists()) dir.mkdirs()
            val array = JSONArray()
            playlists.forEach { playlist ->
                array.put(JSONObject().apply {
                    put("id", playlist.id)
                    put("name", playlist.name)
                    put("createdAt", playlist.createdAt)
                    put("updatedAt", playlist.updatedAt)
                    put("songIds", JSONArray(playlist.songIds))
                })
            }
            file.writeText(array.toString(2))
            true
        }.getOrDefault(false)
    }

    suspend fun loadPlayHistory(): List<SongPlayHistory> = withContext(Dispatchers.IO) {
        readHistoryFile().mapNotNull { item ->
            runCatching {
                SongPlayHistory(
                    songId = item.getLong("songId"),
                    title = item.optString("title", "未知歌曲"),
                    artist = item.optString("artist", "未知艺术家"),
                    playCount = item.optInt("playCount", 0),
                    lastPlayedAt = item.optLong("lastPlayedAt", 0L)
                )
            }.getOrNull()
        }.sortedWith(compareByDescending<SongPlayHistory> { it.playCount }.thenByDescending { it.lastPlayedAt })
    }

    suspend fun recordPlayHistory(song: Song): Boolean = withContext(Dispatchers.IO) {
        val items = readHistoryFile()
        val byId = linkedMapOf<Long, JSONObject>()
        items.forEach { item -> byId[item.optLong("songId")] = item }
        val current = byId[song.id]
        byId[song.id] = JSONObject().apply {
            put("songId", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("uri", song.uri.toString())
            put("playCount", (current?.optInt("playCount") ?: 0) + 1)
            put("lastPlayedAt", System.currentTimeMillis())
        }
        writeHistoryFile(JSONArray(byId.values))
    }

    fun bindLyrics(songId: Long, uri: Uri) {
        preferences.edit().putString(KEY_LYRIC_BINDING_PREFIX + songId, uri.toString()).apply()
    }

    suspend fun loadLyricsFor(song: Song?): List<LyricLine> = withContext(Dispatchers.IO) {
        if (song == null) return@withContext emptyList()
        val boundUri = preferences.getString(KEY_LYRIC_BINDING_PREFIX + song.id, null)?.let { Uri.parse(it) }
        val autoUri = boundUri ?: findAutoMatchedLyrics(song)
        autoUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val content = BufferedReader(InputStreamReader(input)).readText()
                    parseStructuredLyrics(content, queryDisplayName(uri))
                }.orEmpty()
            }.getOrDefault(emptyList())
        }?.takeIf { it.isNotEmpty() } ?: loadEmbeddedLyrics(song)
    }

    suspend fun indexBilingualLyrics(
        songs: List<Song>,
        onProgress: suspend (processed: Int, total: Int, currentTitle: String) -> Unit
    ): BilingualLyricsIndexSummary = withContext(Dispatchers.IO) {
        val total = songs.size
        var bilingualSongs = 0
        var totalLyricLines = 0
        var bilingualLines = 0
        var failedSongs = 0
        val items = JSONArray()
        songs.forEachIndexed { index, song ->
            onProgress(index, total, song.title)
            val lyrics = runCatching { loadLyricsFor(song) }
                .onFailure { failedSongs += 1 }
                .getOrDefault(emptyList())
            val splitLines = lyrics.map { line ->
                val split = line.translation?.let { line.primaryText to it } ?: splitBilingualText(line.text)
                line to split
            }
            val songBilingualLines = splitLines.count { it.second != null }
            totalLyricLines += lyrics.size
            bilingualLines += songBilingualLines
            if (songBilingualLines > 0) {
                bilingualSongs += 1
                items.put(JSONObject().apply {
                    put("songId", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("uri", song.uri.toString())
                    put("lineCount", lyrics.size)
                    put("bilingualLineCount", songBilingualLines)
                    put("samples", JSONArray().apply {
                        splitLines.asSequence()
                            .mapNotNull { it.second }
                            .take(3)
                            .forEach { (primary, translation) ->
                                put(JSONObject().apply {
                                    put("primary", primary)
                                    put("translation", translation)
                                })
                            }
                    })
                })
            }
        }
        onProgress(total, total, "")
        val finishedAt = System.currentTimeMillis()
        writeBilingualLyricsIndex(
            JSONObject().apply {
                put("finishedAt", finishedAt)
                put("totalSongs", total)
                put("bilingualSongs", bilingualSongs)
                put("totalLyricLines", totalLyricLines)
                put("bilingualLines", bilingualLines)
                put("failedSongs", failedSongs)
                put("items", items)
            }
        )
        BilingualLyricsIndexSummary(
            totalSongs = total,
            bilingualSongs = bilingualSongs,
            totalLyricLines = totalLyricLines,
            bilingualLines = bilingualLines,
            failedSongs = failedSongs,
            finishedAt = finishedAt
        )
    }

    private fun loadEmbeddedLyrics(song: Song): List<LyricLine> {
        return runCatching {
            context.contentResolver.openInputStream(song.uri)?.use { input ->
                parseEmbeddedLyrics(input)
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun queryDisplayName(uri: Uri): String {
        val queryUri = if (DocumentsContract.isTreeUri(uri)) {
            runCatching {
                DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            }.getOrNull() ?: uri
        } else {
            uri
        }
        runCatching {
            context.contentResolver.query(queryUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        return cursor.getString(index) ?: "导入歌曲"
                    }
                }
            }
        }
        if (DocumentsContract.isTreeUri(uri)) {
            runCatching {
                DocumentsContract.getTreeDocumentId(uri)
                    .substringAfter(':')
                    .substringAfterLast('/')
                    .takeIf { it.isNotBlank() }
            }.getOrNull()?.let { return it }
        }
        return uri.lastPathSegment ?: "导入歌曲"
    }

    private fun saveImportedUris(uris: List<Uri>) {
        val values = preferences.getStringSet(KEY_IMPORTED_URIS, emptySet()).orEmpty().toMutableSet()
        values.addAll(uris.map { it.toString() })
        preferences.edit().putStringSet(KEY_IMPORTED_URIS, values).apply()
    }

    private fun saveImportedFolder(uri: Uri) {
        val values = preferences.getStringSet(KEY_IMPORTED_FOLDER_URIS, emptySet()).orEmpty().toMutableSet()
        values.add(uri.toString())
        preferences.edit().putStringSet(KEY_IMPORTED_FOLDER_URIS, values).apply()
    }

    private fun persistReadPermission(uri: Uri): Boolean {
        val uriText = uri.toString()
        val alreadyPersisted = context.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission && permission.uri.toString() == uriText
        }
        if (alreadyPersisted) return true
        return runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        }.getOrDefault(false)
    }

    private fun scanFolderUri(treeUri: Uri): ImportResult {
        val rootDocumentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return ImportResult(emptyList(), skipped = 1)
        val songs = mutableListOf<Song>()
        val skipped = scanDocumentChildren(treeUri, rootDocumentId, queryDisplayName(treeUri), songs)
        return ImportResult(mergeByUri(songs), skipped)
    }

    private fun scanDocumentChildren(treeUri: Uri, documentId: String, folderName: String, songs: MutableList<Song>): Int {
        var skipped = 0
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val cursor = runCatching {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )
        }.getOrNull() ?: return 1
        cursor.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (runCatching { cursor.moveToNext() }.getOrDefault(false)) {
                val childId = runCatching { cursor.getString(idCol) }.getOrNull()
                val name = runCatching { cursor.getString(nameCol) }.getOrNull() ?: "未知文件"
                val mime = runCatching { cursor.getString(mimeCol) }.getOrNull().orEmpty()
                if (childId == null) {
                    skipped += 1
                    continue
                }
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    skipped += scanDocumentChildren(treeUri, childId, name, songs)
                } else if (isAudioDocument(name, mime)) {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    val song = runCatching { songFromUri(uri, folderName = folderName) }.getOrNull()
                    if (song != null) songs.add(song) else skipped += 1
                } else {
                    skipped += 1
                }
            }
        }
        return skipped
    }

    private fun isAudioDocument(name: String, mime: String): Boolean {
        if (mime.startsWith("audio/")) return true
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension in setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma")) return true
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.startsWith("audio/") == true
    }

    private fun buildAudioInfo(
        uri: Uri,
        retriever: MediaMetadataRetriever,
        displayName: String,
        duration: Long
    ): AudioInfo? {
        val fileSize = queryFileSize(uri)
        val retrieverFormat = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            ?.substringAfterLast('/')
            ?.removePrefix("x-")
            ?.takeIf { it.isNotBlank() }
        val extensionFormat = displayName.substringAfterLast('.', "").takeIf { it.isNotBlank() }
        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.div(1000)
        val estimatedBitrate = bitrate ?: if (duration > 0 && fileSize != null) ((fileSize * 8L) / duration).toInt() else null
        val flacInfo = runCatching {
            context.contentResolver.openInputStream(uri)?.use { parseFlacAudioInfo(it) }
        }.getOrNull()
        return AudioInfo(
            format = flacInfo?.format ?: retrieverFormat ?: extensionFormat,
            bitrateKbps = estimatedBitrate,
            sampleRateHz = flacInfo?.sampleRateHz,
            bitDepth = flacInfo?.bitDepth,
            channels = flacInfo?.channels,
            fileSizeBytes = fileSize
        ).takeIf { it.displayText().isNotBlank() }
    }

    private fun audioInfoFromMediaStore(sourcePath: String?, duration: Long): AudioInfo? {
        val file = sourcePath?.let { File(it) }?.takeIf { it.exists() }
        val fileSize = file?.length()
        val format = sourcePath?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
        val bitrate = if (duration > 0 && fileSize != null) ((fileSize * 8L) / duration).toInt() else null
        val flacInfo = if (format.equals("flac", ignoreCase = true)) {
            runCatching { file?.inputStream()?.use { parseFlacAudioInfo(it) } }.getOrNull()
        } else {
            null
        }
        return AudioInfo(
            format = flacInfo?.format ?: format,
            bitrateKbps = bitrate,
            sampleRateHz = flacInfo?.sampleRateHz,
            bitDepth = flacInfo?.bitDepth,
            channels = flacInfo?.channels,
            fileSizeBytes = fileSize
        )
            .takeIf { it.displayText().isNotBlank() }
    }

    private fun queryFileSize(uri: Uri): Long? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0) cursor.getLong(index).takeIf { it > 0 } else null
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun readHistoryFile(): List<JSONObject> {
        val privateFile = privateHistoryFile()
        if (privateFile.exists()) {
            return runCatching {
                parseHistoryJson(privateFile.readText())
            }.getOrDefault(emptyList())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = findHistoryDownloadUri()
            val text = uri?.let {
                runCatching {
                    context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                }.getOrNull()
            }
            if (!text.isNullOrBlank()) {
                val migrated = parseHistoryJson(text)
                if (migrated.isNotEmpty()) {
                    writeHistoryFile(JSONArray(migrated))
                }
                return migrated
            }
        }
        val file = historyFile()
        if (!file.exists()) return emptyList()
        val legacy = runCatching {
            parseHistoryJson(file.readText())
        }.getOrDefault(emptyList())
        if (legacy.isNotEmpty()) {
            writeHistoryFile(JSONArray(legacy))
        }
        return legacy
    }

    private fun writeHistoryFile(array: JSONArray): Boolean {
        return runCatching {
            val file = privateHistoryFile()
            val dir = file.parentFile ?: return@runCatching false
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(file).use { output ->
                output.write(array.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        }.getOrDefault(false)
    }

    private fun privateHistoryFile(): File {
        return File(context.filesDir, HISTORY_FILE_NAME)
    }

    private fun playlistFile(): File {
        return File(context.filesDir, PLAYLIST_FILE_NAME)
    }

    private fun bilingualLyricsIndexFile(): File {
        return File(context.filesDir, BILINGUAL_LYRICS_INDEX_FILE_NAME)
    }

    private fun writeBilingualLyricsIndex(json: JSONObject): Boolean {
        return runCatching {
            val file = bilingualLyricsIndexFile()
            val dir = file.parentFile ?: return@runCatching false
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(file).use { output ->
                output.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        }.getOrDefault(false)
    }

    private fun historyFile(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$HISTORY_DIR/$HISTORY_FILE_NAME"
        )
    }

    private fun parseHistoryJson(text: String): List<JSONObject> {
        return runCatching {
            val array = JSONArray(text)
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index) }
        }.getOrDefault(emptyList())
    }

    private fun findHistoryDownloadUri(): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
        val args = arrayOf(HISTORY_FILE_NAME, "${Environment.DIRECTORY_DOWNLOADS}/$HISTORY_DIR/")
        return runCatching {
            context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private suspend fun ensureLegacyImportsMigrated() {
        if (dao.importRoots().isNotEmpty()) return
        val now = System.currentTimeMillis()
        preferences.getStringSet(KEY_IMPORTED_URIS, emptySet()).orEmpty().forEach { value ->
            val uri = Uri.parse(value)
            dao.upsertRoot(
                ImportedRootEntity(
                    uri = value,
                    kind = ImportRootKind.File.name,
                    displayName = queryDisplayName(uri),
                    availability = SourceAvailability.Available.name,
                    lastError = null,
                    createdAt = now,
                    lastValidatedAt = 0L
                )
            )
        }
        preferences.getStringSet(KEY_IMPORTED_FOLDER_URIS, emptySet()).orEmpty().forEach { value ->
            val uri = Uri.parse(value)
            dao.upsertRoot(
                ImportedRootEntity(
                    uri = value,
                    kind = ImportRootKind.Tree.name,
                    displayName = queryDisplayName(uri),
                    availability = SourceAvailability.Available.name,
                    lastError = null,
                    createdAt = now,
                    lastValidatedAt = 0L
                )
            )
        }
    }

    private suspend fun canonicalizeSongs(
        songs: List<Song>,
        kind: SourceKind,
        parentRootUri: String?
    ): List<Song> {
        if (songs.isEmpty()) return emptyList()
        val knownSources = dao.sources().toMutableList()
        val now = System.currentTimeMillis()
        return songs.map { raw ->
            val uriText = raw.uri.toString()
            val exact = knownSources.firstOrNull { it.sourceUri == uriText }
            val fileSize = raw.audioInfo?.fileSizeBytes
            val titleKey = SongIdentityMatcher.normalize(raw.title)
            val artistKey = SongIdentityMatcher.normalize(raw.artist).takeUnless { it in UNKNOWN_ARTIST_KEYS }.orEmpty()
            val strongCandidates = if (exact == null && fileSize != null && fileSize > 0L && raw.duration > 0L) {
                knownSources.filter { source ->
                    SongIdentityMatcher.isStrongMatch(
                        SongIdentitySignature(fileSize, raw.duration, raw.title, raw.artist),
                        SongIdentitySignature(source.fileSizeBytes, source.durationMs, source.titleKey, source.artistKey)
                    )
                }.map { it.canonicalId }.distinct()
            } else {
                emptyList()
            }
            val canonicalId = exact?.canonicalId ?: strongCandidates.singleOrNull() ?: UUID.randomUUID().toString()
            dao.upsertSong(
                LibrarySongEntity(
                    canonicalId = canonicalId,
                    title = raw.title,
                    artist = raw.artist,
                    album = raw.album,
                    durationMs = raw.duration,
                    fileSizeBytes = fileSize,
                    updatedAt = now
                )
            )
            val source = SongSourceEntity(
                sourceUri = uriText,
                canonicalId = canonicalId,
                kind = kind.name,
                parentRootUri = parentRootUri,
                displayName = raw.sourcePath?.substringAfterLast('/') ?: raw.title,
                fileSizeBytes = fileSize,
                durationMs = raw.duration,
                titleKey = titleKey,
                artistKey = artistKey,
                availability = SourceAvailability.Available.name,
                lastError = null,
                lastSeenAt = now
            )
            dao.upsertSource(source)
            knownSources.removeAll { it.sourceUri == uriText }
            knownSources += source
            raw.copy(canonicalId = canonicalId)
        }
    }

    private fun sourceAvailability(error: Throwable): SourceAvailability = when (error) {
        is SecurityException -> SourceAvailability.PermissionLost
        is java.io.FileNotFoundException -> SourceAvailability.Missing
        else -> SourceAvailability.ParseFailed
    }

    private fun legacySourceId(uriText: String): Long {
        val uri = Uri.parse(uriText)
        return if (uri.authority == MediaStore.AUTHORITY) {
            uri.lastPathSegment?.toLongOrNull() ?: -kotlin.math.abs(uriText.hashCode().toLong())
        } else {
            -kotlin.math.abs(uriText.hashCode().toLong())
        }
    }

    private fun releaseReadPermissionIfUnused(uri: Uri) {
        val stillUsed = preferences.getStringSet(KEY_IMPORTED_URIS, emptySet()).orEmpty().contains(uri.toString()) ||
            preferences.getStringSet(KEY_IMPORTED_FOLDER_URIS, emptySet()).orEmpty().contains(uri.toString())
        if (!stillUsed) runCatching {
            context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun mergeByUri(songs: List<Song>): List<Song> = songs.distinctBy { it.uri.toString() }

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

    private fun findAutoMatchedLyrics(song: Song): Uri? {
        val candidates = preferences.getStringSet(KEY_LYRIC_URIS, emptySet()).orEmpty()
        val songKeys = setOf(normalizeSong(song), normalizeName(song.title))
        return candidates
            .mapNotNull { value -> LyricCandidate.from(value) }
            .filter { it.key in songKeys }
            .sortedBy { lyricPriority(it.extension) }
            .firstOrNull()
            ?.uri
    }

    private fun normalizeSong(song: Song): String {
        val filename = song.sourcePath?.substringAfterLast('/')?.substringBeforeLast('.')
        return normalizeName(filename ?: song.title)
    }

    private fun normalizeName(value: String): String {
        return value.lowercase()
            .replace(Regex("""\s+"""), "")
            .replace(Regex("""[《》<>\[\]【】()（）_-]"""), "")
    }

    private companion object {
        const val KEY_IMPORTED_URIS = "imported_uris"
        const val KEY_IMPORTED_FOLDER_URIS = "imported_folder_uris"
        const val KEY_LYRIC_URIS = "lyric_uris"
        const val KEY_FAVORITES = "favorite_song_ids"
        const val KEY_LYRIC_BINDING_PREFIX = "lyric_binding_"
        const val HISTORY_DIR = "AndroidMusicPlayerData"
        const val HISTORY_FILE_NAME = "play_history.json"
        const val PLAYLIST_FILE_NAME = "playlists.json"
        const val BILINGUAL_LYRICS_INDEX_FILE_NAME = "bilingual_lyrics_index.json"
        val UNKNOWN_ARTIST_KEYS = setOf("", "未知艺术家", "unknownartist", "unknown")
    }

    private data class LyricCandidate(
        val key: String,
        val extension: String,
        val uri: Uri
    ) {
        companion object {
            fun from(value: String): LyricCandidate? {
                val parts = value.split('|', limit = 3)
                return when (parts.size) {
                    3 -> LyricCandidate(parts[0], parts[1].lowercase(), Uri.parse(parts[2]))
                    2 -> {
                        val uri = Uri.parse(parts[1])
                        LyricCandidate(parts[0], uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase().orEmpty(), uri)
                    }
                    else -> null
                }
            }
        }
    }

    private fun lyricPriority(extension: String): Int = when (extension.lowercase()) {
        "ttml", "xml" -> 0
        "lrc" -> 1
        "srt" -> 2
        else -> 3
    }
}

data class ImportedFolder(
    val uri: Uri,
    val name: String,
    val availability: String = SourceAvailability.Available.name,
    val lastError: String? = null
)

data class ImportResult(
    val songs: List<Song>,
    val skipped: Int = 0
)

data class BilingualLyricsIndexSummary(
    val totalSongs: Int = 0,
    val bilingualSongs: Int = 0,
    val totalLyricLines: Int = 0,
    val bilingualLines: Int = 0,
    val failedSongs: Int = 0,
    val finishedAt: Long = 0L
)
