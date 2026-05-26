package com.musicplayer.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.musicplayer.data.PlaybackMode
import com.musicplayer.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val pendingListeners = mutableSetOf<Player.Listener>()
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            pendingListeners.forEach { mediaController?.addListener(it) }
            _isConnected.value = true
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        _isConnected.value = false
    }

    fun playSong(song: Song) {
        mediaController?.let { ctrl ->
            val mediaItem = song.toMediaItem()
            ctrl.setMediaItem(mediaItem)
            ctrl.prepare()
            ctrl.play()
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        mediaController?.let { ctrl ->
            val mediaItems = songs.map { song -> song.toMediaItem() }
            ctrl.setMediaItems(mediaItems, startIndex, 0)
            ctrl.prepare()
            ctrl.play()
        }
    }

    fun playPause() {
        mediaController?.let { ctrl ->
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
        }
    }

    fun pause() { mediaController?.pause() }
    fun resume() { mediaController?.play() }

    fun next() { mediaController?.seekToNextMediaItem() }
    fun previous() { mediaController?.seekToPreviousMediaItem() }
    fun playQueueIndex(index: Int) {
        mediaController?.let { ctrl ->
            if (index in 0 until ctrl.mediaItemCount) {
                ctrl.seekToDefaultPosition(index)
                ctrl.play()
            }
        }
    }

    fun seekTo(positionMs: Long) { mediaController?.seekTo(positionMs) }

    fun updateCurrentMetadata(song: Song, title: String, artist: String): Boolean {
        val ctrl = mediaController ?: return false
        return runCatching {
            val index = ctrl.currentMediaItemIndex
            if (index !in 0 until ctrl.mediaItemCount) return false
            ctrl.replaceMediaItem(index, song.toMediaItem(displayTitle = title, displayArtist = artist))
            true
        }.getOrDefault(false)
    }

    fun restoreCurrentMetadata(song: Song) {
        updateCurrentMetadata(song, song.title, song.artist)
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        mediaController?.let { ctrl ->
            ctrl.repeatMode = when (mode) {
                PlaybackMode.Sequential -> Player.REPEAT_MODE_OFF
                PlaybackMode.RepeatOne -> Player.REPEAT_MODE_ONE
                PlaybackMode.Shuffle -> Player.REPEAT_MODE_ALL
            }
            ctrl.shuffleModeEnabled = mode == PlaybackMode.Shuffle
        }
    }

    val isPlaying: Boolean get() = mediaController?.isPlaying ?: false
    val currentPosition: Long get() = mediaController?.currentPosition ?: 0L
    val duration: Long get() = mediaController?.duration ?: 0L
    val currentMediaItemIndex: Int get() = mediaController?.currentMediaItemIndex ?: -1

    fun currentSongSnapshot(): Song? {
        val ctrl = mediaController ?: return null
        val item = ctrl.currentMediaItem ?: return null
        return item.toSongSnapshot(ctrl.duration)
    }

    fun queueSnapshot(): List<Song> {
        val ctrl = mediaController ?: return emptyList()
        return (0 until ctrl.mediaItemCount).mapNotNull { index ->
            ctrl.getMediaItemAt(index).toSongSnapshot(if (index == ctrl.currentMediaItemIndex) ctrl.duration else 0L)
        }
    }

    fun addListener(listener: Player.Listener) {
        pendingListeners.add(listener)
        mediaController?.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        mediaController?.removeListener(listener)
        pendingListeners.remove(listener)
    }

    private fun Song.toMediaItem(displayTitle: String = title, displayArtist: String = artist): MediaItem {
        val extras = Bundle().apply {
            putString(EXTRA_ORIGINAL_TITLE, title)
            putString(EXTRA_ORIGINAL_ARTIST, artist)
            putString(EXTRA_ORIGINAL_ALBUM, album)
            putString(EXTRA_FOLDER, folderName)
        }
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(displayTitle)
                    .setArtist(displayArtist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    private fun MediaItem.toSongSnapshot(duration: Long): Song? {
        val uri = localConfiguration?.uri ?: return null
        val metadata = mediaMetadata
        val extras = metadata.extras
        return Song(
            id = mediaId.toLongOrNull() ?: -kotlin.math.abs(uri.toString().hashCode().toLong()),
            title = extras?.getString(EXTRA_ORIGINAL_TITLE)?.takeIf { it.isNotBlank() }
                ?: metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "未知歌曲",
            artist = extras?.getString(EXTRA_ORIGINAL_ARTIST)?.takeIf { it.isNotBlank() }
                ?: metadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "未知艺术家",
            album = extras?.getString(EXTRA_ORIGINAL_ALBUM)?.takeIf { it.isNotBlank() }
                ?: metadata.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: "未知专辑",
            duration = duration.coerceAtLeast(0L),
            uri = uri,
            albumArtUri = metadata.artworkUri,
            folderName = extras?.getString(EXTRA_FOLDER) ?: "本地音乐"
        )
    }

    private companion object {
        const val EXTRA_ORIGINAL_TITLE = "original_title"
        const val EXTRA_ORIGINAL_ARTIST = "original_artist"
        const val EXTRA_ORIGINAL_ALBUM = "original_album"
        const val EXTRA_FOLDER = "folder"
    }
}
