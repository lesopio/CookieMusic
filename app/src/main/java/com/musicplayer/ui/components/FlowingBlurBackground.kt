package com.musicplayer.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.musicplayer.data.Song

@Composable
fun FlowingBlurBackground(
    song: Song?,
    accent: Color,
    isPlaying: Boolean,
    enabled: Boolean,
    intensity: Float,
    subduedArtwork: Boolean = false
) {
    val clampedIntensity = intensity.coerceIn(0.2f, 1f)
    val active = enabled && isPlaying
    val transition = rememberInfiniteTransition(label = "flowing_blur_background")
    val driftX by transition.animateFloat(
        initialValue = -22f * clampedIntensity,
        targetValue = 22f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(11_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_drift_x"
    )
    val driftY by transition.animateFloat(
        initialValue = 18f * clampedIntensity,
        targetValue = -18f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(13_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_drift_y"
    )
    val scalePulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0.055f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(12_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_scale"
    )
    val blobAX by transition.animateFloat(
        initialValue = -72f * clampedIntensity,
        targetValue = 42f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(15_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_a_x"
    )
    val blobAY by transition.animateFloat(
        initialValue = -18f * clampedIntensity,
        targetValue = 56f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(18_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_a_y"
    )
    val blobBX by transition.animateFloat(
        initialValue = 70f * clampedIntensity,
        targetValue = -38f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(19_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_b_x"
    )
    val blobBY by transition.animateFloat(
        initialValue = 36f * clampedIntensity,
        targetValue = -42f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(13_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_b_y"
    )
    val blobCX by transition.animateFloat(
        initialValue = -32f * clampedIntensity,
        targetValue = 78f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(22_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_c_x"
    )
    val blobCY by transition.animateFloat(
        initialValue = 64f * clampedIntensity,
        targetValue = -24f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(17_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_c_y"
    )
    val blobDX by transition.animateFloat(
        initialValue = 44f * clampedIntensity,
        targetValue = -66f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(24_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_d_x"
    )
    val blobDY by transition.animateFloat(
        initialValue = -52f * clampedIntensity,
        targetValue = 28f * clampedIntensity,
        animationSpec = infiniteRepeatable(tween(16_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_d_y"
    )
    val blobScaleA by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_scale_a"
    )
    val blobScaleB by transition.animateFloat(
        initialValue = 1.18f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(14_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flow_blob_scale_b"
    )
    val blobAlpha by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(11_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "flow_blob_alpha"
    )
    val x = if (active) driftX else 0f
    val y = if (active) driftY else 0f
    val scale = if (active) scalePulse else 0f
    val ax = if (active) blobAX else -18f
    val ay = if (active) blobAY else 10f
    val bx = if (active) blobBX else 16f
    val by = if (active) blobBY else -10f
    val cx = if (active) blobCX else -12f
    val cy = if (active) blobCY else 12f
    val dx = if (active) blobDX else 10f
    val dy = if (active) blobDY else -8f
    val blobPrimaryScale = if (active) blobScaleA else 1f
    val blobSecondaryScale = if (active) blobScaleB else 1f
    val blobPulse = if (active) blobAlpha else 0.86f
    val baseAlpha = if (subduedArtwork) 0.20f else 0.94f
    val blobAlphaBase = if (subduedArtwork) 0.13f else 0.24f

    Box(Modifier.fillMaxSize()) {
        if (song != null) {
            AlbumArtImage(
                song = song,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        translationX = x,
                        translationY = y,
                        scaleX = 1.08f + scale,
                        scaleY = 1.08f + scale,
                        alpha = baseAlpha
                    )
                    .blur(if (subduedArtwork) 30.dp else 42.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        Canvas(Modifier.fillMaxSize()) {
            fun drawSoftBlob(
                color: Color,
                baseSizeDp: Float,
                offsetXDp: Float,
                offsetYDp: Float,
                scaleX: Float,
                scaleY: Float,
                alpha: Float
            ) {
                val width = baseSizeDp.dp.toPx() * scaleX
                val height = baseSizeDp.dp.toPx() * scaleY
                val topLeft = Offset(offsetXDp.dp.toPx(), offsetYDp.dp.toPx())
                val center = Offset(topLeft.x + width / 2f, topLeft.y + height / 2f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = (0.78f * alpha).coerceIn(0f, 1f)),
                            color.copy(alpha = (0.28f * alpha).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxOf(width, height) * 0.58f
                    ),
                    topLeft = topLeft,
                    size = Size(width, height)
                )
            }
            drawSoftBlob(
                color = accent,
                baseSizeDp = 430f,
                offsetXDp = -118f + ax,
                offsetYDp = 48f + ay,
                scaleX = 1.28f * blobPrimaryScale,
                scaleY = 0.78f * blobSecondaryScale,
                alpha = (blobAlphaBase + clampedIntensity * 0.12f) * blobPulse
            )
            drawSoftBlob(
                color = primary,
                baseSizeDp = 390f,
                offsetXDp = 250f + bx,
                offsetYDp = 132f + by,
                scaleX = 0.86f * blobSecondaryScale,
                scaleY = 1.34f * blobPrimaryScale,
                alpha = (blobAlphaBase + clampedIntensity * 0.08f) * (1.12f - (blobPulse - 0.72f) * 0.42f)
            )
            drawSoftBlob(
                color = tertiary,
                baseSizeDp = 460f,
                offsetXDp = -86f + cx,
                offsetYDp = 430f + cy,
                scaleX = 1.18f * blobSecondaryScale,
                scaleY = 0.92f * blobPrimaryScale,
                alpha = (blobAlphaBase + clampedIntensity * 0.10f) * (0.92f + (blobPulse - 0.72f) * 0.28f)
            )
            drawSoftBlob(
                color = accent,
                baseSizeDp = 360f,
                offsetXDp = 218f + dx,
                offsetYDp = 596f + dy,
                scaleX = 1.04f * blobPrimaryScale,
                scaleY = 1.18f * blobSecondaryScale,
                alpha = (blobAlphaBase * 0.82f + clampedIntensity * 0.08f) * (1.02f - (blobPulse - 0.72f) * 0.25f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = if (subduedArtwork) 0.06f else 0.10f),
                            MaterialTheme.colorScheme.background.copy(alpha = if (subduedArtwork) 0.90f else 0.72f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
    }
}
