package com.musicplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.data.Song

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    rotateCover: Boolean = false
) {
    if (song == null) return
    val glass = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val stroke = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
    val shape = RoundedCornerShape(32.dp)
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(glass)
            .border(BorderStroke(1.dp, stroke), shape)
            .then(clickModifier)
    ) {
        Column(Modifier.background(glass)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glass)
                    .padding(start = 12.dp, top = 9.dp, end = 10.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infinite = rememberInfiniteTransition(label = "capsule-cover-spin")
                val rotation by infinite.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 18_000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "capsule-cover-rotation"
                )
                AlbumArtImage(
                    song = song,
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer { rotationZ = if (rotateCover && isPlaying) rotation else 0f }
                        .clip(RoundedCornerShape(22.dp)),
                    fallbackModifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = primaryText
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = secondaryText
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", tint = primaryText)
                }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(50.dp)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(25.dp))
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White
                        )
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一首", tint = primaryText)
                }
            }
            CapsuleProgressBar(
                progress = progress,
                accent = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp, end = 30.dp, bottom = 9.dp)
            )
        }
    }
}

@Composable
private fun CapsuleProgressBar(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f)
    Canvas(modifier.height(3.dp)) {
        val radius = size.height / 2f
        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = CornerRadius(radius, radius)
        )
        val progressWidth = size.width * progress.coerceIn(0f, 1f)
        if (progressWidth > 0f) {
            drawRoundRect(
                color = accent,
                topLeft = Offset.Zero,
                size = Size(progressWidth, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}
