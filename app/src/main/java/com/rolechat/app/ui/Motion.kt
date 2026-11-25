package com.rolechat.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Tactile spring-bounce click feedback for glass pills and primary buttons. */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.91f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bounceScale",
    )
    this
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/** Staggered fade + gentle upward slide entrance for cards and suggestion chips. */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    stepDelayMs: Long = 55L,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        delay((index * stepDelayMs).coerceAtMost(280L))
        launch { alpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) }
        launch {
            offsetY.animateTo(
                0f,
                spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        },
    ) {
        content()
    }
}

/** Subtle flowing iridescent gradient brush for primary brand CTAs. */
@Composable
fun rememberShimmerAccentBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "ctaShimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerShift",
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF8B6BFF),
            Color(0xFF6AA4FF),
            Color(0xFFA584FF),
            Color(0xFF6AA4FF),
        ),
        start = Offset(shift - 450f, 0f),
        end = Offset(shift + 450f, 180f),
    )
}

/** Three staggered bouncing dots shown in a character glass bubble while waiting for an AI reply. */
@Composable
fun TypingDotsIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typingDots")
    val offsets = listOf(0, 140, 280).map { delayMs ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520, delayMillis = delayMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot_$delayMs",
        )
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        offsets.forEach { state ->
            val t = state.value
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        translationY = -t * 7f
                        alpha = 0.45f + t * 0.55f
                        val s = 0.85f + t * 0.25f
                        scaleX = s
                        scaleY = s
                    }
                    .background(color, CircleShape),
            )
        }
    }
}

/** Pulsing luminous halo around the character avatar while voice playback is active. */
@Composable
fun SpeakingAvatarAura(
    active: Boolean,
    size: Dp,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "avatarAura")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auraPulse",
    )
    Box(
        modifier = Modifier.size(size + 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val baseRadius = (size.toPx() / 2f)
                val expand = (4.dp.toPx()) * pulse
                drawCircle(
                    color = Color(0xFFA78BFF).copy(alpha = (1f - pulse) * 0.72f),
                    radius = baseRadius + expand,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCircle(
                    color = Color(0xFF72B4FF).copy(alpha = 0.42f),
                    radius = baseRadius + 1.dp.toPx(),
                    style = Stroke(width = 1.4.dp.toPx()),
                )
            }
        }
        content()
    }
}
