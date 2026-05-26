package com.musicplayer.data

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

fun parseEmbeddedLyrics(input: InputStream): List<LyricLine> {
    val bytes = input.readBytes()
    return parseId3Lyrics(bytes).takeIf { it.isNotEmpty() } ?: parseFlacLyrics(bytes)
}

fun parseFlacAudioInfo(input: InputStream): AudioInfo? {
    val bytes = input.readBytes()
    if (!bytes.startsWithFlac()) return null
    var offset = 4
    while (offset + 4 <= bytes.size) {
        val header = bytes[offset].toInt() and 0xFF
        val type = header and 0x7F
        val length = ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
        val payloadStart = offset + 4
        if (length < 0 || payloadStart + length > bytes.size) return null
        if (type == 0 && length >= 34) {
            val streamInfo = bytes.copyOfRange(payloadStart, payloadStart + length)
            val sampleRate = ((streamInfo[10].toInt() and 0xFF) shl 12) or
                ((streamInfo[11].toInt() and 0xFF) shl 4) or
                ((streamInfo[12].toInt() and 0xF0) ushr 4)
            val channels = ((streamInfo[12].toInt() and 0x0E) ushr 1) + 1
            val bitDepth = (((streamInfo[12].toInt() and 0x01) shl 4) or
                ((streamInfo[13].toInt() and 0xF0) ushr 4)) + 1
            return AudioInfo(format = "FLAC", sampleRateHz = sampleRate, bitDepth = bitDepth, channels = channels)
        }
        offset = payloadStart + length
        if ((header and 0x80) != 0) break
    }
    return AudioInfo(format = "FLAC")
}

private fun parseId3Lyrics(bytes: ByteArray): List<LyricLine> {
    val input = bytes.inputStream()
    val header = ByteArray(10)
    if (input.read(header) != header.size) return emptyList()
    if (header[0].toInt().toChar() != 'I' || header[1].toInt().toChar() != 'D' || header[2].toInt().toChar() != '3') {
        return emptyList()
    }
    val version = header[3].toInt()
    val tagSize = syncSafeInt(header, 6)
    if (tagSize <= 0) return emptyList()

    val tag = input.readNBytesCompat(tagSize)
    var offset = 0
    while (offset + 10 <= tag.size) {
        val id = String(tag, offset, 4, StandardCharsets.ISO_8859_1)
        if (id.all { it.code == 0 }) break
        val frameSize = if (version >= 4) syncSafeInt(tag, offset + 4) else normalInt(tag, offset + 4)
        if (frameSize <= 0 || offset + 10 + frameSize > tag.size) break
        val payloadStart = offset + 10
        val payload = tag.copyOfRange(payloadStart, payloadStart + frameSize)
        if (id == "USLT" || id == "SYLT") {
            decodeLyricsFrame(payload, id == "SYLT")?.let { content ->
                val parsed = parseLrcOrPlainText(content)
                if (parsed.isNotEmpty()) return parsed
            }
        }
        offset += 10 + frameSize
    }
    return emptyList()
}

private fun parseFlacLyrics(bytes: ByteArray): List<LyricLine> {
    if (!bytes.startsWithFlac()) return emptyList()
    var offset = 4
    while (offset + 4 <= bytes.size) {
        val header = bytes[offset].toInt() and 0xFF
        val type = header and 0x7F
        val length = ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
        val payloadStart = offset + 4
        if (length < 0 || payloadStart + length > bytes.size) return emptyList()
        if (type == 4) {
            val parsed = parseVorbisCommentLyrics(bytes.copyOfRange(payloadStart, payloadStart + length))
            if (parsed.isNotEmpty()) return parsed
        }
        offset = payloadStart + length
        if ((header and 0x80) != 0) break
    }
    return emptyList()
}

private fun parseVorbisCommentLyrics(payload: ByteArray): List<LyricLine> {
    var offset = 0
    fun readLittleEndianInt(): Int? {
        if (offset + 4 > payload.size) return null
        val value = (payload[offset].toInt() and 0xFF) or
            ((payload[offset + 1].toInt() and 0xFF) shl 8) or
            ((payload[offset + 2].toInt() and 0xFF) shl 16) or
            ((payload[offset + 3].toInt() and 0xFF) shl 24)
        offset += 4
        return value
    }
    val vendorLength = readLittleEndianInt() ?: return emptyList()
    if (vendorLength < 0 || offset + vendorLength > payload.size) return emptyList()
    offset += vendorLength
    val count = readLittleEndianInt() ?: return emptyList()
    val lyricKeys = setOf("LYRICS", "UNSYNCEDLYRICS", "SYNCEDLYRICS", "DESCRIPTION")
    repeat(count.coerceAtLeast(0)) {
        val length = readLittleEndianInt() ?: return@repeat
        if (length < 0 || offset + length > payload.size) return@repeat
        val comment = runCatching { String(payload, offset, length, Charsets.UTF_8) }.getOrNull().orEmpty()
        offset += length
        val key = comment.substringBefore('=', "").uppercase(Locale.ROOT)
        val value = comment.substringAfter('=', "")
        if (key in lyricKeys && value.isNotBlank()) {
            val parsed = parseLrcOrPlainText(value)
            if (parsed.isNotEmpty()) return parsed
        }
    }
    return emptyList()
}

fun parseLrcOrPlainText(content: String): List<LyricLine> {
    val lrc = parseLrc(content)
    if (lrc.isNotEmpty()) return lrc
    return content.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapIndexed { index, line -> LyricLine(index * 4_000L, line) }
        .toList()
}

private fun decodeLyricsFrame(payload: ByteArray, synchronized: Boolean): String? {
    if (payload.size < 5) return null
    val encoding = payload[0].toInt() and 0xFF
    val charset = when (encoding) {
        1 -> Charsets.UTF_16
        2 -> Charset.forName("UTF-16BE")
        3 -> Charsets.UTF_8
        else -> StandardCharsets.ISO_8859_1
    }
    var offset = 4
    offset = skipEncodedTerminator(payload, offset, encoding)
    if (offset >= payload.size) return null
    if (synchronized) {
        if (offset + 6 >= payload.size) return null
        offset += 6
    }
    return runCatching { String(payload, offset, payload.size - offset, charset).trim('\u0000', ' ', '\n', '\r') }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
}

private fun skipEncodedTerminator(bytes: ByteArray, start: Int, encoding: Int): Int {
    var offset = start
    val doubleByte = encoding == 1 || encoding == 2
    if (doubleByte) {
        while (offset + 1 < bytes.size) {
            if (bytes[offset].toInt() == 0 && bytes[offset + 1].toInt() == 0) return offset + 2
            offset += 2
        }
    } else {
        while (offset < bytes.size) {
            if (bytes[offset].toInt() == 0) return offset + 1
            offset += 1
        }
    }
    return offset
}

private fun syncSafeInt(bytes: ByteArray, offset: Int): Int {
    return ((bytes[offset].toInt() and 0x7F) shl 21) or
        ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
        ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
        (bytes[offset + 3].toInt() and 0x7F)
}

private fun normalInt(bytes: ByteArray, offset: Int): Int {
    return ((bytes[offset].toInt() and 0xFF) shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)
}

private fun InputStream.readNBytesCompat(length: Int): ByteArray {
    val output = ByteArray(length)
    var total = 0
    while (total < length) {
        val read = read(output, total, length - total)
        if (read < 0) break
        total += read
    }
    return if (total == length) output else output.copyOf(total)
}

private fun ByteArray.startsWithFlac(): Boolean {
    return size >= 4 &&
        this[0].toInt().toChar() == 'f' &&
        this[1].toInt().toChar() == 'L' &&
        this[2].toInt().toChar() == 'a' &&
        this[3].toInt().toChar() == 'C'
}
