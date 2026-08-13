package com.musicplayer.data

import java.util.Locale
import kotlin.math.abs

data class SongIdentitySignature(
    val fileSizeBytes: Long?,
    val durationMs: Long,
    val title: String,
    val artist: String
)

object SongIdentityMatcher {
    fun isStrongMatch(first: SongIdentitySignature, second: SongIdentitySignature): Boolean {
        val size = first.fileSizeBytes
        if (size == null || size <= 0L || size != second.fileSizeBytes) return false
        if (first.durationMs <= 0L || second.durationMs <= 0L || abs(first.durationMs - second.durationMs) > 2_000L) return false
        return normalize(first.title) == normalize(second.title) && normalizeArtist(first.artist) == normalizeArtist(second.artist)
    }

    fun normalize(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("""\s+"""), "")
        .replace(Regex("""[《》<>\[\]【】()（）_\-.,，。'\"]"""), "")

    private fun normalizeArtist(value: String): String {
        val normalized = normalize(value)
        return normalized.takeUnless { it in setOf("", "未知艺术家", "unknownartist", "unknown") }.orEmpty()
    }
}
