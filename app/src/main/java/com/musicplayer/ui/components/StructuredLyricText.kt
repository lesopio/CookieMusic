package com.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicplayer.data.AudioVisualizationState
import com.musicplayer.data.LyricLine
import kotlin.math.roundToInt

@Composable
fun StructuredLyricText(
    line: LyricLine,
    active: Boolean,
    currentPosition: Long,
    fallbackProgress: Float,
    accent: Color,
    primaryColor: Color,
    secondaryColor: Color,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    visualizerState: AudioVisualizationState = AudioVisualizationState(),
    primaryStyle: TextStyle = MaterialTheme.typography.titleLarge,
    secondaryStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    activeBackground: Boolean = true,
    primaryMaxLines: Int = 3,
    translationMaxLines: Int = 2,
    activeTranslationSpacing: Dp = 6.dp,
    activeTranslationOffsetY: Dp = 0.dp
) {
    val displayText = line.toDisplayText()
    val readableAccent = remember(accent, darkTheme) { accent.readableOnDynamicBackground(darkTheme) }
    val highlightProgress = remember(line, currentPosition, fallbackProgress) {
        line.timedProgress(currentPosition, fallbackProgress)
    }
    val backgroundColor = if (active && activeBackground) {
        if (darkTheme) Color.Black.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.34f)
    } else {
        Color.Transparent
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .padding(horizontal = if (active && activeBackground) 10.dp else 0.dp, vertical = if (active && activeBackground) 8.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (active) {
            KaraokeClipText(
                text = displayText.primary,
                progress = highlightProgress,
                accent = readableAccent,
                inactiveColor = primaryColor,
                glowAlpha = (0.08f + visualizerState.beatPulse * 0.12f + visualizerState.treble * 0.06f).coerceIn(0.08f, 0.24f),
                modifier = Modifier.fillMaxWidth(),
                style = primaryStyle,
                fontWeight = FontWeight.Bold,
                maxLines = primaryMaxLines
            )
        } else {
            Text(
                text = displayText.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = primaryStyle,
                color = primaryColor,
                fontWeight = FontWeight.Medium,
                maxLines = primaryMaxLines,
                overflow = TextOverflow.Ellipsis,
                lineHeight = primaryStyle.fontSize * 1.18f
            )
        }
        displayText.secondary?.let { secondary ->
            Spacer(Modifier.height(if (active) activeTranslationSpacing else 4.dp))
            Text(
                text = secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = if (active) activeTranslationOffsetY else 0.dp),
                textAlign = TextAlign.Center,
                style = secondaryStyle,
                color = secondaryColor,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                maxLines = translationMaxLines,
                overflow = TextOverflow.Ellipsis,
                lineHeight = secondaryStyle.fontSize * 1.18f
            )
        }
        displayText.romanization?.let { romanization ->
            Spacer(Modifier.height(3.dp))
            Text(
                text = romanization,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryColor.copy(alpha = secondaryColor.alpha * 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KaraokeClipText(
    text: String,
    progress: Float,
    accent: Color,
    inactiveColor: Color,
    glowAlpha: Float,
    modifier: Modifier = Modifier,
    style: TextStyle,
    fontWeight: FontWeight,
    maxLines: Int
) {
    if (maxLines == 1) {
        OneWayMarqueeKaraokeClipText(text, progress, accent, inactiveColor, glowAlpha, modifier, style, fontWeight)
        return
    }
    var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = glowAlpha),
                textAlign = TextAlign.Center,
                style = style.copy(color = accent),
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = style.copy(color = inactiveColor),
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth().drawWithContent {
                    val layout = textLayout
                    if (layout == null || layout.lineCount <= 1) {
                        clipRect(right = size.width * progress.coerceIn(0f, 1f)) {
                            this@drawWithContent.drawContent()
                        }
                    } else {
                        val lineProgress = progress.coerceIn(0f, 1f) * layout.lineCount
                        for (lineIndex in 0 until layout.lineCount) {
                            val part = (lineProgress - lineIndex).coerceIn(0f, 1f)
                            if (part > 0f) {
                                val left = layout.getLineLeft(lineIndex)
                                val right = left + (layout.getLineRight(lineIndex) - left) * part
                                clipRect(
                                    left = left,
                                    top = layout.getLineTop(lineIndex),
                                    right = right,
                                    bottom = layout.getLineBottom(lineIndex)
                                ) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                        }
                    }
                },
                textAlign = TextAlign.Center,
                style = style.copy(color = accent),
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayout = it }
            )
        }
    }
}

@Composable
private fun OneWayMarqueeKaraokeClipText(
    text: String,
    progress: Float,
    accent: Color,
    inactiveColor: Color,
    glowAlpha: Float,
    modifier: Modifier,
    style: TextStyle,
    fontWeight: FontWeight
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val fullTextWidthPx = remember(text, style, fontWeight) {
        textMeasurer.measure(
            text = AnnotatedString(text),
            style = style.copy(fontWeight = fontWeight),
            softWrap = false,
            maxLines = 1
        ).size.width
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth().clipToBounds()) {
        val viewportWidthPx = with(density) { maxWidth.roundToPx() }
        val scrollDistancePx = (fullTextWidthPx - viewportWidthPx).coerceAtLeast(0)
        val highlightedRightPx = fullTextWidthPx * progress.coerceIn(0f, 1f)
        val offsetPx = -((highlightedRightPx - viewportWidthPx) * 1.15f)
            .coerceIn(0f, scrollDistancePx.toFloat())
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (scrollDistancePx > 0) Alignment.CenterStart else Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetPx.roundToInt(), 0) }
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
            ) {
            var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                text = text,
                modifier = Modifier.graphicsLayer(alpha = glowAlpha),
                textAlign = TextAlign.Start,
                style = style.copy(color = accent),
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
            Text(
                text = text,
                textAlign = TextAlign.Start,
                style = style.copy(color = inactiveColor),
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
            Text(
                text = text,
                modifier = Modifier.drawWithContent {
                    val layout = textLayout
                    if (layout != null) {
                        clipRect(right = size.width * progress.coerceIn(0f, 1f)) {
                            this@drawWithContent.drawContent()
                        }
                    }
                },
                textAlign = TextAlign.Start,
                style = style.copy(color = accent),
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                onTextLayout = { textLayout = it }
            )
            }
        }
    }
}

private fun LyricLine.timedProgress(currentPosition: Long, fallbackProgress: Float): Float {
    if (words.isEmpty()) return fallbackProgress.coerceIn(0f, 1f)
    val totalChars = words.sumOf { it.text.length.coerceAtLeast(1) }.toFloat().coerceAtLeast(1f)
    var completedChars = 0f
    for (word in words) {
        val length = word.text.length.coerceAtLeast(1).toFloat()
        val wordProgress = when {
            currentPosition >= word.endMs -> 1f
            currentPosition <= word.startMs -> 0f
            else -> ((currentPosition - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
        }
        completedChars += length * wordProgress
        if (wordProgress < 1f) break
    }
    return (completedChars / totalChars).coerceIn(0f, 1f)
}

private fun Color.readableOnDynamicBackground(darkTheme: Boolean): Color {
    return when {
        darkTheme && luminance() < 0.34f -> mix(Color.White, 0.42f)
        !darkTheme && luminance() > 0.48f -> mix(Color(0xFF302936), 0.58f)
        !darkTheme -> mix(Color(0xFF302936), 0.28f)
        else -> this
    }
}

private fun Color.mix(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - t) + other.red * t,
        green = green * (1f - t) + other.green * t,
        blue = blue * (1f - t) + other.blue * t,
        alpha = alpha * (1f - t) + other.alpha * t
    )
}
