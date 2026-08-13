package com.musicplayer.data

import android.net.Uri

data class Song(
    val id: Long,
    val canonicalId: String = "legacy:$id",
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri? = null,
    val trackNumber: Int = 0,
    val sourcePath: String? = null,
    val folderName: String = "本地音乐",
    val imported: Boolean = false,
    val audioInfo: AudioInfo? = null
)

data class AudioInfo(
    val format: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val channels: Int? = null,
    val fileSizeBytes: Long? = null
) {
    fun displayText(): String {
        return listOfNotNull(
            format?.uppercase(),
            bitrateKbps?.takeIf { it > 0 }?.let { "${it}kbps" },
            sampleRateHz?.takeIf { it > 0 }?.let { hz ->
                if (hz % 1000 == 0) "${hz / 1000}kHz" else "${hz / 1000f}kHz"
            },
            bitDepth?.takeIf { it > 0 }?.let { "${it}bit" },
            channels?.takeIf { it > 0 }?.let { if (it == 1) "Mono" else if (it == 2) "Stereo" else "${it}ch" }
        ).joinToString(" · ")
    }
}

data class SongPlayHistory(
    val songId: Long,
    val title: String,
    val artist: String,
    val playCount: Int,
    val lastPlayedAt: Long
)
