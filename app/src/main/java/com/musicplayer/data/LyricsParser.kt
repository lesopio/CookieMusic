package com.musicplayer.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.Locale

private val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
private val lyricCreditLineRegex = Regex(
    """^\s*(?:by|作词|作曲|编曲|制作人|出品|联合出品|日语改编词|调教|吉他|贝斯|鼓|二胡|笛箫|古筝|琵琶|混音|母带|弦乐|民乐|录音|监制)\s*[:：]""",
    RegexOption.IGNORE_CASE
)
private val bilingualGapRegex = Regex("""^(.{2,}?)[\u2000-\u200A\u202F\u205F\u3000]+(.{2,})$""")

fun parseLrc(content: String): List<LyricLine> {
    val rawLines = content.lineSequence()
        .flatMap { line ->
            val matches = timestampRegex.findAll(line).toList()
            val text = line.replace(timestampRegex, "").trim()
            if (matches.isEmpty() || text.isBlank()) {
                emptySequence()
            } else {
                matches.asSequence().mapNotNull { match ->
                    val minutes = match.groupValues.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                    val seconds = match.groupValues.getOrNull(2)?.toLongOrNull() ?: return@mapNotNull null
                    val fraction = match.groupValues.getOrNull(3).orEmpty()
                    val millis = when (fraction.length) {
                        0 -> 0L
                        1 -> fraction.toLong() * 100
                        2 -> fraction.toLong() * 10
                        else -> fraction.take(3).toLong()
                    }
                    LyricLine((minutes * 60 + seconds) * 1000 + millis, text)
                }
            }
        }
        .sortedBy { it.timeMs }
        .toList()
    return mergeSameTimeTranslations(rawLines)
}

fun splitBilingualText(text: String): Pair<String, String>? {
    val normalized = text.trim()
    if (normalized.isBlank()) return null
    if (lyricCreditLineRegex.containsMatchIn(normalized)) return null
    val explicitLines = normalized
        .split('\n', '\r')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (explicitLines.size >= 2) {
        val first = explicitLines.first()
        val second = explicitLines.drop(1).joinToString(" ")
        if (looksLikeTranslationPair(first, second)) return first to second
    }

    val labeled = Regex(
        """^(?:原文|主歌词|歌词|orig(?:inal)?|source)\s*[:：]\s*(.+?)\s+(?:译文|翻译|中文|translation|trans)\s*[:：]\s*(.+)$""",
        RegexOption.IGNORE_CASE
    ).matchEntire(normalized)
    if (labeled != null) return labeled.groupValues[1].trim() to labeled.groupValues[2].trim()

    val parenthesized = Regex("""^(.+?)\s*[（(]([^（）()]{2,})[）)]\s*$""").matchEntire(normalized)
    if (parenthesized != null) {
        val first = parenthesized.groupValues[1].trim()
        val second = parenthesized.groupValues[2].trim()
        if (looksLikeTranslationPair(first, second)) return first to second
    }

    val separatorMatch = Regex("""^(.{2,}?)\s*(?:[|｜/／]|--| - | – | — )\s*(.{2,})$""").matchEntire(normalized)
    if (separatorMatch != null) {
        val first = separatorMatch.groupValues[1].trim()
        val second = separatorMatch.groupValues[2].trim()
        if (first.isNotBlank() && second.isNotBlank() && looksLikeTranslationPair(first, second)) return first to second
    }

    val bilingualGap = bilingualGapRegex.matchEntire(normalized)
    if (bilingualGap != null) {
        val first = bilingualGap.groupValues[1].trim()
        val second = bilingualGap.groupValues[2].trim()
        if (first.isNotBlank() && second.isNotBlank() && looksLikeTranslationPair(first, second)) return first to second
    }

    val wideSpace = Regex("""^(.{2,}?)\s{2,}(.{2,})$""").matchEntire(normalized)
    if (wideSpace != null) {
        val first = wideSpace.groupValues[1].trim()
        val second = wideSpace.groupValues[2].trim()
        if (looksLikeTranslationPair(first, second)) return first to second
    }

    return null
}

private fun looksLikeTranslationPair(first: String, second: String): Boolean {
    val firstScripts = first.scriptSet()
    val secondScripts = second.scriptSet()
    if (firstScripts.isEmpty() || secondScripts.isEmpty()) return false
    return firstScripts != secondScripts
}

private fun String.scriptSet(): Set<String> {
    val scripts = mutableSetOf<String>()
    forEach { ch ->
        when {
            ch in '\u4E00'..'\u9FFF' -> scripts += "han"
            ch in '\u3040'..'\u30FF' -> scripts += "kana"
            ch in '\uAC00'..'\uD7AF' -> scripts += "hangul"
            ch.isLetter() && ch.code < 128 -> scripts += "latin"
        }
    }
    return scripts
}

fun parseStructuredLyrics(content: String, fileName: String? = null): List<LyricLine> {
    val trimmed = content.trimStart()
    val extension = fileName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
    return when {
        extension in setOf("ttml", "xml") || trimmed.startsWith("<") -> parseTtml(content)
        extension == "srt" -> parseSrt(content)
        else -> parseLrcOrPlainText(content)
    }
}

fun parseTtml(content: String): List<LyricLine> {
    return runCatching {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser()
        parser.setInput(StringReader(content))
        val lines = mutableListOf<LyricLine>()
        val translationBuckets = linkedMapOf<Long, MutableList<String>>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("p", ignoreCase = true)) {
                val begin = parser.timeAttribute("begin") ?: parser.timeAttribute("start")
                val end = parser.timeAttribute("end")
                if (begin != null) {
                    val role = parser.roleAttribute()
                    val parsed = parser.readTimedParagraph(begin, end)
                    if (parsed.text.isNotBlank()) {
                        if (role.isTranslationRole()) {
                            translationBuckets.getOrPut(begin) { mutableListOf() }.add(parsed.text)
                        } else {
                            lines.add(parsed)
                        }
                    }
                }
            }
            event = parser.next()
        }

        val merged = lines.map { line ->
            val translation = translationBuckets[line.timeMs]?.distinct()?.joinToString(" ")
            if (translation.isNullOrBlank()) line else line.copy(translation = translation)
        }
        mergeSameTimeTranslations(merged.ifEmpty {
            translationBuckets.map { (time, values) -> LyricLine(time, values.distinct().joinToString(" ")) }
        })
    }.getOrDefault(emptyList())
}

fun parseSrt(content: String): List<LyricLine> {
    val blocks = content.replace("\r\n", "\n").split(Regex("""\n{2,}"""))
    val lines = blocks.mapNotNull { block ->
        val rows = block.lines().map { it.trim() }.filter { it.isNotBlank() }
        val timeRow = rows.firstOrNull { it.contains("-->") } ?: return@mapNotNull null
        val start = timeRow.substringBefore("-->").trim().toLyricTimeMs() ?: return@mapNotNull null
        val end = timeRow.substringAfter("-->").substringBefore(' ').trim().toLyricTimeMs()
        val text = rows.dropWhile { it != timeRow }.drop(1).joinToString("\n").trim()
        text.takeIf { it.isNotBlank() }?.let { LyricLine(start, it, endTimeMs = end) }
    }.sortedBy { it.timeMs }
    return mergeSameTimeTranslations(lines)
}

private fun XmlPullParser.readTimedParagraph(begin: Long, end: Long?): LyricLine {
    val words = mutableListOf<LyricWord>()
    val text = StringBuilder()
    var depth = 1
    var lastWordEnd = begin
    while (depth > 0) {
        when (next()) {
            XmlPullParser.START_TAG -> {
                if (name.equals("span", ignoreCase = true)) {
                    val spanBegin = timeAttribute("begin") ?: timeAttribute("start") ?: lastWordEnd
                    val spanEnd = timeAttribute("end")
                    val word = readSpanText()
                    if (word.isNotBlank()) {
                        appendWithSpacing(text, word)
                        val safeEnd = (spanEnd ?: spanBegin).coerceAtLeast(spanBegin)
                        words.add(LyricWord(word, spanBegin, safeEnd))
                        lastWordEnd = safeEnd
                    }
                } else {
                    depth += 1
                }
            }
            XmlPullParser.TEXT -> appendWithSpacing(text, this.text.orEmpty())
            XmlPullParser.END_TAG -> depth -= 1
            XmlPullParser.END_DOCUMENT -> depth = 0
        }
    }
    val finalText = text.toString().replace(Regex("""\s+"""), " ").trim()
    return LyricLine(
        timeMs = begin,
        text = finalText,
        endTimeMs = end,
        words = words.filter { it.text.isNotBlank() }
    )
}

private fun XmlPullParser.readSpanText(): String {
    val text = StringBuilder()
    var depth = 1
    while (depth > 0) {
        when (next()) {
            XmlPullParser.START_TAG -> depth += 1
            XmlPullParser.TEXT -> text.append(this.text.orEmpty())
            XmlPullParser.END_TAG -> depth -= 1
            XmlPullParser.END_DOCUMENT -> depth = 0
        }
    }
    return text.toString().replace(Regex("""\s+"""), " ").trim()
}

private fun XmlPullParser.timeAttribute(name: String): Long? {
    return getAttributeValue(null, name)?.toLyricTimeMs()
        ?: (0 until attributeCount).firstNotNullOfOrNull { index ->
            if (getAttributeName(index).equals(name, ignoreCase = true)) getAttributeValue(index).toLyricTimeMs() else null
        }
}

private fun XmlPullParser.roleAttribute(): String {
    return listOf("role", "type", "lang", "xml:lang")
        .firstNotNullOfOrNull { attr -> getAttributeValue(null, attr)?.takeIf { it.isNotBlank() } }
        ?: (0 until attributeCount).firstNotNullOfOrNull { index ->
            val name = getAttributeName(index).lowercase(Locale.ROOT)
            if (name.endsWith("role") || name.endsWith("type") || name.endsWith("lang")) getAttributeValue(index) else null
        }.orEmpty()
}

private fun String.isTranslationRole(): Boolean {
    val value = lowercase(Locale.ROOT)
    return value.contains("translation") ||
        value.contains("trans") ||
        value.contains("x-translation")
}

private fun String.toLyricTimeMs(): Long? {
    val value = trim().replace(',', '.')
    if (value.isBlank()) return null
    if (value.endsWith("ms")) return value.dropLast(2).toDoubleOrNull()?.toLong()
    if (value.endsWith("s") && ':' !in value) return value.dropLast(1).toDoubleOrNull()?.times(1000)?.toLong()
    val parts = value.split(':')
    return when (parts.size) {
        3 -> {
            val hours = parts[0].toLongOrNull() ?: return null
            val minutes = parts[1].toLongOrNull() ?: return null
            val seconds = parts[2].toDoubleOrNull() ?: return null
            ((hours * 3600 + minutes * 60) * 1000 + seconds * 1000).toLong()
        }
        2 -> {
            val minutes = parts[0].toLongOrNull() ?: return null
            val seconds = parts[1].toDoubleOrNull() ?: return null
            ((minutes * 60) * 1000 + seconds * 1000).toLong()
        }
        else -> value.toDoubleOrNull()?.times(1000)?.toLong()
    }
}

private fun mergeSameTimeTranslations(lines: List<LyricLine>): List<LyricLine> {
    if (lines.isEmpty()) return emptyList()
    val groups = mutableListOf<MutableList<LyricLine>>()
    lines.sortedBy { it.timeMs }.forEach { line ->
        val last = groups.lastOrNull()
        if (last != null && kotlin.math.abs(line.timeMs - last.first().timeMs) <= BILINGUAL_TIME_TOLERANCE_MS) {
            last.add(line)
        } else {
            groups.add(mutableListOf(line))
        }
    }
    return groups
        .mapNotNull { group ->
            val distinct = group.distinctBy { it.text.trim() }
            val primary = distinct.firstOrNull { it.words.isNotEmpty() } ?: distinct.firstOrNull() ?: return@mapNotNull null
            val explicit = splitBilingualText(primary.text)
            val primaryText = explicit?.first ?: primary.text
            val extras = distinct
                .filterNot { it === primary || it.text.trim() == primary.text.trim() }
                .mapNotNull { line ->
                    val lineExplicit = splitBilingualText(line.text)
                    val candidate = line.translation
                        ?: lineExplicit?.second
                        ?: lineExplicit?.first
                        ?: line.text
                    candidate.trim()
                        .takeIf { it.isNotBlank() && it != primaryText.trim() }
                }
                .filter { it.isNotBlank() }
                .distinct()
            primary.copy(
                text = primaryText,
                translation = primary.translation ?: explicit?.second ?: extras.firstOrNull(),
                romanization = primary.romanization
            )
        }
}

private fun appendWithSpacing(builder: StringBuilder, value: String) {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return
    if (builder.isNotEmpty() && !builder.last().isCjkOrSpace() && !trimmed.first().isCjkOrSpace()) {
        builder.append(' ')
    }
    builder.append(trimmed)
}

private fun Char.isCjkOrSpace(): Boolean {
    return isWhitespace() || code in 0x4E00..0x9FFF || code in 0x3040..0x30FF || code in 0xAC00..0xD7AF
}

private const val BILINGUAL_TIME_TOLERANCE_MS = 80L
