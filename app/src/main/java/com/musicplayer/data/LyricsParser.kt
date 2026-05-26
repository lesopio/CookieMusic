package com.musicplayer.data

private val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

fun parseLrc(content: String): List<LyricLine> {
    return content.lineSequence()
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
}
