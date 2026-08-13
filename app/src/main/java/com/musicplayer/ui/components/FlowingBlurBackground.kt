package com.musicplayer.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.musicplayer.data.AudioVisualizationState
import com.musicplayer.data.Song

@Composable
fun FlowingBlurBackground(
    song: Song?,
    accent: Color,
    isPlaying: Boolean,
    enabled: Boolean,
    intensity: Float,
    stageParticlesEnabled: Boolean = true,
    beatReactiveEnabled: Boolean = true,
    visualizerState: AudioVisualizationState = AudioVisualizationState(),
    subduedArtwork: Boolean = false,
    darkTheme: Boolean = false
) {
    val clampedIntensity = intensity.coerceIn(0.2f, 1f)
    val active = enabled && isPlaying
    val audioPulse = if (active && beatReactiveEnabled) visualizerState.beatPulse.coerceIn(0f, 1f) else 0f
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
    val x = if (active) driftX else 0f
    val y = if (active) driftY else 0f
    val scale = if (active) scalePulse + audioPulse * 0.035f else 0f
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
    val blobPulse = if (active) 0.92f + audioPulse * 0.18f else 0.86f
    val baseAlpha = when {
        subduedArtwork -> 0.20f
        darkTheme -> 0.58f
        else -> 0.40f
    }
    val blobAlphaBase = if (subduedArtwork) 0.13f else if (darkTheme) 0.22f else 0.18f

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
        val complement = Color(1f - accent.red, 1f - accent.green, 1f - accent.blue, accent.alpha)
        val neighbor = Color(accent.green, accent.blue, accent.red, accent.alpha)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = (0.06f + clampedIntensity * 0.05f).coerceIn(0f, 0.14f)),
                        complement.copy(alpha = (0.04f + clampedIntensity * 0.04f).coerceIn(0f, 0.11f)),
                        primary.copy(alpha = (0.05f + clampedIntensity * 0.04f).coerceIn(0f, 0.12f)),
                        neighbor.copy(alpha = (0.04f + clampedIntensity * 0.04f).coerceIn(0f, 0.10f))
                    ),
                    start = Offset(size.width * 0.08f + ax.dp.toPx() * 0.12f, 0f),
                    end = Offset(size.width * 0.92f + bx.dp.toPx() * 0.10f, size.height)
                )
            )

            fun drawSoftWash(
                color: Color,
                center: Offset,
                radius: Float,
                alpha: Float
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = (0.34f * alpha).coerceIn(0f, 1f)),
                            color.copy(alpha = (0.14f * alpha).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
            drawSoftWash(
                color = accent,
                center = Offset(size.width * 0.18f + ax.dp.toPx(), size.height * 0.23f + ay.dp.toPx()),
                radius = size.maxDimension * 0.46f * blobPrimaryScale,
                alpha = (blobAlphaBase + clampedIntensity * 0.12f) * blobPulse
            )
            drawSoftWash(
                color = complement,
                center = Offset(size.width * 0.84f + bx.dp.toPx(), size.height * 0.32f + by.dp.toPx()),
                radius = size.maxDimension * 0.40f * blobSecondaryScale,
                alpha = (blobAlphaBase + clampedIntensity * 0.08f) * (1.12f - (blobPulse - 0.72f) * 0.42f)
            )
            drawSoftWash(
                color = tertiary,
                center = Offset(size.width * 0.22f + cx.dp.toPx(), size.height * 0.76f + cy.dp.toPx()),
                radius = size.maxDimension * 0.48f * blobSecondaryScale,
                alpha = (blobAlphaBase + clampedIntensity * 0.10f) * (0.92f + (blobPulse - 0.72f) * 0.28f)
            )
            drawSoftWash(
                color = neighbor,
                center = Offset(size.width * 0.78f + dx.dp.toPx(), size.height * 0.84f + dy.dp.toPx()),
                radius = size.maxDimension * 0.36f * blobPrimaryScale,
                alpha = (blobAlphaBase * 0.82f + clampedIntensity * 0.08f) * (1.02f - (blobPulse - 0.72f) * 0.25f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (darkTheme) {
                            listOf(
                                Color.Black.copy(alpha = if (subduedArtwork) 0.72f else 0.38f),
                                accent.copy(alpha = if (subduedArtwork) 0.08f else 0.10f),
                                Color.Black.copy(alpha = if (subduedArtwork) 0.82f else 0.54f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = if (subduedArtwork) 0.88f else 0.44f),
                                accent.copy(alpha = if (subduedArtwork) 0.05f else 0.08f),
                                Color.White.copy(alpha = if (subduedArtwork) 0.92f else 0.58f)
                            )
                        }
                    )
                )
        )
    }
}
