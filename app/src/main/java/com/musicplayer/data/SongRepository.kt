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

class SongRepository(private val context: Context) {

    private val preferences = context.getSharedPreferences("music_library", Context.MODE_PRIVATE)

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
        songs
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        scanSongs().filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    suspend fun importSongs(uris: List<Uri>): List<Song> = withContext(Dispatchers.IO) {
        val imported = uris.mapNotNull { uri ->
            runCatching {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                songFromUri(uri)
            }.getOrNull()
        }
        saveImportedUris(uris)
        imported
    }

    suspend fun importFolder(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        saveImportedFolder(uri)
        scanFolderUri(uri)
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
                val values = preferences.getStringSet(KEY_LYRIC_URIS, emptySet()).orEmpty().toMutableSet()
                values.add("$key|$uri")
                preferences.edit().putStringSet(KEY_LYRIC_URIS, values).apply()
                val matchedSong = songs.firstOrNull { song -> normalizeSong(song) == key || normalizeName(song.title) == key }
                if (matchedSong != null) bindLyrics(matchedSong.id, uri)
                imported += 1
            }
        }
        imported
    }

    suspend fun loadImportedSongs(): List<Song> = withContext(Dispatchers.IO) {
        val directSongs = preferences.getStringSet(KEY_IMPORTED_URIS, emptySet())
            .orEmpty()
            .mapNotNull { value ->
                runCatching { songFromUri(Uri.parse(value)) }.getOrNull()
            }
        val folderSongs = preferences.getStringSet(KEY_IMPORTED_FOLDER_URIS, emptySet())
            .orEmpty()
            .flatMap { value -> runCatching { scanFolderUri(Uri.parse(value)).songs }.getOrDefault(emptyList()) }
        mergeByUri(directSongs + folderSongs)
    }

    suspend fun loadImportedFolders(): List<ImportedFolder> = withContext(Dispatchers.IO) {
        preferences.getStringSet(KEY_IMPORTED_FOLDER_URIS, emptySet())
            .orEmpty()
            .map { Uri.parse(it) }
            .map { uri -> ImportedFolder(uri = uri, name = queryDisplayName(uri)) }
    }

    suspend fun removeImportedSong(song: Song): List<Song> = withContext(Dispatchers.IO) {
        val values = preferences.getStringSet(KEY_IMPORTED_URIS, emptySet()).orEmpty().toMutableSet()
        values.remove(song.uri.toString())
        preferences.edit().putStringSet(KEY_IMPORTED_URIS, values).apply()
        loadImportedSongs()
    }

    suspend fun removeImportedFolder(folder: ImportedFolder): List<Song> = withContext(Dispatchers.IO) {
        val values = preferences.getStringSet(KEY_IMPORTED_FOLDER_URIS, emptySet()).orEmpty().toMutableSet()
        values.remove(folder.uri.toString())
        preferences.edit().putStringSet(KEY_IMPORTED_FOLDER_URIS, values).apply()
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

    fun loadFavorites(): Set<Long> {
        return preferences.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun saveFavorites(ids: Set<Long>) {
        preferences.edit().putStringSet(KEY_FAVORITES, ids.map { it.toString() }.toSet()).apply()
    }

    suspend fun loadPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val file = playlistFile()
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val ids = item.optJSONArray("songIds")
                Playlist(
                    id = item.optLong("id"),
                    name = item.optString("name", "未命名歌单"),
                    songIds = if (ids == null) emptyList() else (0 until ids.length()).map { ids.optLong(it) },
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun savePlaylists(playlists: List<Playlist>): Boolean = withContext(Dispatchers.IO) {
        runCatching {
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
                    parseLrcOrPlainText(content)
                }.orEmpty()
            }.getOrDefault(emptyList())
        }?.takeIf { it.isNotEmpty() } ?: loadEmbeddedLyrics(song)
    }

    private fun loadEmbeddedLyrics(song: Song): List<LyricLine> {
        return runCatching {
            context.contentResolver.openInputStream(song.uri)?.use { input ->
                parseEmbeddedLyrics(input)
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun queryDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index) ?: "导入歌曲"
                }
            }
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
        return AudioInfo(format = format, bitrateKbps = bitrate, fileSizeBytes = fileSize)
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

    private fun mergeByUri(songs: List<Song>): List<Song> {
        return linkedMapOf<String, Song>().apply {
            songs.forEach { put(it.uri.toString(), it) }
        }.values.toList()
    }

    private fun findAutoMatchedLyrics(song: Song): Uri? {
        val candidates = preferences.getStringSet(KEY_LYRIC_URIS, emptySet()).orEmpty()
        val songKeys = setOf(normalizeSong(song), normalizeName(song.title))
        return candidates.firstNotNullOfOrNull { value ->
            val parts = value.split('|', limit = 2)
            if (parts.size == 2 && parts[0] in songKeys) Uri.parse(parts[1]) else null
        }
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
    }
}

data class ImportedFolder(
    val uri: Uri,
    val name: String
)

data class ImportResult(
    val songs: List<Song>,
    val skipped: Int = 0
)
