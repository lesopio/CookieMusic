package com.musicplayer.ui.components

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musicplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumArtImage(
    song: Song,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackModifier: Modifier = Modifier.size(40.dp)
) {
    val context = LocalContext.current
    val bitmap = produceState<android.graphics.Bitmap?>(initialValue = null, song.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                loadEmbeddedBitmap(context, song, 768)
            }.getOrNull()
        }
    }

    if (bitmap.value != null) {
        Image(
            bitmap = bitmap.value!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (song.albumArtUri != null) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = fallbackModifier,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
fun rememberAlbumAccentColor(song: Song?, fallback: Color = MaterialTheme.colorScheme.primary): State<Color> {
    val context = LocalContext.current
    return produceState(initialValue = fallback, song?.uri, song?.albumArtUri, fallback) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = loadAlbumBitmap(context, song) ?: return@withContext fallback
                dominantColor(bitmap) ?: fallback
            }.getOrDefault(fallback)
        }
    }
}

private fun loadAlbumBitmap(context: android.content.Context, song: Song?): android.graphics.Bitmap? {
    if (song == null) return null
    AlbumArtMemoryCache.get(song.uri.toString())?.let { return it }
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, song.uri)
        retriever.embeddedPicture?.let { bytes -> decodeSampled(bytes, 768) }?.also {
            AlbumArtMemoryCache.put(song.uri.toString(), it)
        } ?: song.albumArtUri?.let { uri ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }?.also { AlbumArtMemoryCache.put(song.uri.toString(), it) }
        }
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun loadEmbeddedBitmap(context: android.content.Context, song: Song, targetPx: Int): android.graphics.Bitmap? {
    AlbumArtMemoryCache.get(song.uri.toString())?.let { return it }
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, song.uri)
        retriever.embeddedPicture?.let { decodeSampled(it, targetPx) }
            ?.also { AlbumArtMemoryCache.put(song.uri.toString(), it) }
    } finally {
        runCatching { retriever.release() }
    }
}

private fun decodeSampled(bytes: ByteArray, targetPx: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / sample > targetPx * 2 || bounds.outHeight / sample > targetPx * 2) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private object AlbumArtMemoryCache {
    private val cache = object : LruCache<String, android.graphics.Bitmap>(16 * 1024) {
        override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.byteCount / 1024
    }
    fun get(key: String): android.graphics.Bitmap? = cache.get(key)
    fun put(key: String, value: android.graphics.Bitmap) { cache.put(key, value) }
}

private fun dominantColor(bitmap: android.graphics.Bitmap): Color? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return null

    val buckets = LinkedHashMap<Int, LongArray>()
    val stepX = (width / 48).coerceAtLeast(1)
    val stepY = (height / 48).coerceAtLeast(1)
    for (y in 0 until height step stepY) {
        for (x in 0 until width step stepX) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(pixel)
            if (alpha < 180) continue
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            if (max < 42 || min > 232 || max - min < 18) continue
            val key = ((r / 32) shl 16) or ((g / 32) shl 8) or (b / 32)
            val bucket = buckets.getOrPut(key) { longArrayOf(0L, 0L, 0L, 0L) }
            bucket[0] += r.toLong()
            bucket[1] += g.toLong()
            bucket[2] += b.toLong()
            bucket[3] += 1L
        }
    }
    val best = buckets.values.maxByOrNull { bucket ->
        val count = bucket[3]
        val r = bucket[0] / count
        val g = bucket[1] / count
        val b = bucket[2] / count
        val saturation = maxOf(r, g, b) - minOf(r, g, b)
        count * 2 + saturation
    } ?: return null
    val count = best[3].coerceAtLeast(1L)
    return Color(
        red = (best[0] / count).toInt(),
        green = (best[1] / count).toInt(),
        blue = (best[2] / count).toInt()
    )
}
