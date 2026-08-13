package com.musicplayer.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

enum class SourceAvailability { Available, PermissionLost, Missing, ParseFailed }
enum class SourceKind { MediaStore, SafFile, SafTreeChild }
enum class ImportRootKind { File, Tree }

@Entity(tableName = "library_songs")
data class LibrarySongEntity(
    @PrimaryKey val canonicalId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val fileSizeBytes: Long?,
    val updatedAt: Long
)

@Entity(
    tableName = "song_sources",
    foreignKeys = [ForeignKey(
        entity = LibrarySongEntity::class,
        parentColumns = ["canonicalId"],
        childColumns = ["canonicalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("canonicalId"), Index("parentRootUri")]
)
data class SongSourceEntity(
    @PrimaryKey val sourceUri: String,
    val canonicalId: String,
    val kind: String,
    val parentRootUri: String?,
    val displayName: String,
    val fileSizeBytes: Long?,
    val durationMs: Long,
    val titleKey: String,
    val artistKey: String,
    val availability: String,
    val lastError: String?,
    val lastSeenAt: Long
)

@Entity(tableName = "import_roots")
data class ImportedRootEntity(
    @PrimaryKey val uri: String,
    val kind: String,
    val displayName: String,
    val availability: String,
    val lastError: String?,
    val createdAt: Long,
    val lastValidatedAt: Long
)

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val canonicalId: String)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "canonicalId"],
    indices = [Index("canonicalId")],
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val canonicalId: String,
    val position: Int
)

@Dao
interface MusicLibraryDao {
    @Query("SELECT * FROM library_songs") suspend fun songs(): List<LibrarySongEntity>
    @Query("SELECT * FROM song_sources") suspend fun sources(): List<SongSourceEntity>
    @Query("SELECT * FROM song_sources WHERE sourceUri = :uri LIMIT 1") suspend fun source(uri: String): SongSourceEntity?
    @Query("SELECT * FROM import_roots ORDER BY createdAt") suspend fun importRoots(): List<ImportedRootEntity>
    @Query("SELECT * FROM favorites") suspend fun favorites(): List<FavoriteEntity>
    @Query("SELECT * FROM playlists ORDER BY createdAt") suspend fun playlists(): List<PlaylistEntity>
    @Query("SELECT * FROM playlist_songs ORDER BY playlistId, position") suspend fun playlistSongs(): List<PlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSong(song: LibrarySongEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSource(source: SongSourceEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertRoot(root: ImportedRootEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertFavorite(favorite: FavoriteEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPlaylist(playlist: PlaylistEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPlaylistSongs(songs: List<PlaylistSongEntity>)

    @Query("DELETE FROM favorites WHERE canonicalId = :canonicalId") suspend fun deleteFavorite(canonicalId: String)
    @Query("DELETE FROM favorites") suspend fun clearFavorites()
    @Query("DELETE FROM playlist_songs") suspend fun clearPlaylistSongs()
    @Query("DELETE FROM playlists") suspend fun clearPlaylists()
    @Query("DELETE FROM song_sources WHERE sourceUri = :uri") suspend fun deleteSource(uri: String)
    @Query("DELETE FROM song_sources WHERE parentRootUri = :uri") suspend fun deleteSourcesForRoot(uri: String)
    @Query("DELETE FROM import_roots WHERE uri = :uri") suspend fun deleteRoot(uri: String)

    @Transaction
    suspend fun replacePlaylists(values: List<Playlist>) {
        clearPlaylistSongs()
        clearPlaylists()
        values.forEach { playlist ->
            upsertPlaylist(PlaylistEntity(playlist.id, playlist.name, playlist.createdAt, playlist.updatedAt))
            upsertPlaylistSongs(playlist.songIds.mapIndexed { index, id -> PlaylistSongEntity(playlist.id, id, index) })
        }
    }
}

@Database(
    entities = [
        LibrarySongEntity::class,
        SongSourceEntity::class,
        ImportedRootEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun libraryDao(): MusicLibraryDao

    companion object {
        @Volatile private var instance: MusicDatabase? = null

        fun get(context: Context): MusicDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MusicDatabase::class.java,
                "music-library.db"
            ).build().also { instance = it }
        }
    }
}
