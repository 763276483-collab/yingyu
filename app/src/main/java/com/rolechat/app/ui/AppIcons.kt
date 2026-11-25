package com.rolechat.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SpeakerIcon(muted: Boolean, color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val body = Path().apply {
            moveTo(w * 0.14f, h * 0.37f)
            lineTo(w * 0.34f, h * 0.37f)
            lineTo(w * 0.56f, h * 0.19f)
            lineTo(w * 0.56f, h * 0.81f)
            lineTo(w * 0.34f, h * 0.63f)
            lineTo(w * 0.14f, h * 0.63f)
            close()
        }
        drawPath(body, color = color, style = stroke)
        if (!muted) {
            drawArc(color, -45f, 90f, false, Offset(w * 0.48f, h * 0.30f), Size(w * 0.24f, h * 0.40f), style = stroke)
            drawArc(color, -45f, 90f, false, Offset(w * 0.56f, h * 0.18f), Size(w * 0.34f, h * 0.64f), style = stroke)
        } else {
            drawLine(color, Offset(w * 0.68f, h * 0.34f), Offset(w * 0.90f, h * 0.66f), strokeWidth = w * 0.095f, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.90f, h * 0.34f), Offset(w * 0.68f, h * 0.66f), strokeWidth = w * 0.095f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun BackArrowIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f
        drawLine(color, Offset(w * 0.66f, h * 0.20f), Offset(w * 0.32f, h * 0.50f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.32f, h * 0.50f), Offset(w * 0.66f, h * 0.80f), sw, StrokeCap.Round)
    }
}

@Composable
fun MenuIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.095f
        drawLine(color, Offset(w * 0.18f, h * 0.28f), Offset(w * 0.82f, h * 0.28f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.18f, h * 0.50f), Offset(w * 0.82f, h * 0.50f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.18f, h * 0.72f), Offset(w * 0.82f, h * 0.72f), sw, StrokeCap.Round)
    }
}

@Composable
fun SoundWaveIcon(color: Color, size: Dp = 14.dp, animated: Boolean = false) {
    val bar1: Float
    val bar2: Float
    val bar3: Float
    if (animated) {
        val transition = rememberInfiniteTransition(label = "soundWave")
        val a1 by transition.animateFloat(
            initialValue = 0.22f, targetValue = 0.86f,
            animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "b1",
        )
        val a2 by transition.animateFloat(
            initialValue = 0.88f, targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "b2",
        )
        val a3 by transition.animateFloat(
            initialValue = 0.32f, targetValue = 0.82f,
            animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "b3",
        )
        bar1 = a1; bar2 = a2; bar3 = a3
    } else {
        bar1 = 0.34f; bar2 = 0.70f; bar3 = 0.42f
    }
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.14f
        fun drawBar(xFrac: Float, heightFrac: Float) {
            val half = (h * heightFrac) / 2f
            val cy = h / 2f
            drawLine(color, Offset(w * xFrac, cy - half), Offset(w * xFrac, cy + half), sw, StrokeCap.Round)
        }
        drawBar(0.20f, bar1)
        drawBar(0.50f, bar2)
        drawBar(0.80f, bar3)
    }
}

@Composable
fun SendArrowIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f
        drawLine(color, Offset(w * 0.50f, h * 0.80f), Offset(w * 0.50f, h * 0.22f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.24f, h * 0.46f), Offset(w * 0.50f, h * 0.20f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.76f, h * 0.46f), Offset(w * 0.50f, h * 0.20f), sw, StrokeCap.Round)
    }
}

@Composable
fun StopIcon(color: Color, size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val inset = this.size.width * 0.22f
        val side = this.size.width - inset * 2
        drawRoundRect(color, Offset(inset, inset), Size(side, side), CornerRadius(side * 0.22f, side * 0.22f))
    }
}

@Composable
fun LightbulbIcon(color: Color, size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawArc(
            color = color,
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(w * 0.20f, h * 0.12f),
            size = Size(w * 0.60f, h * 0.56f),
            style = stroke,
        )
        drawLine(color, Offset(w * 0.34f, h * 0.64f), Offset(w * 0.38f, h * 0.74f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.66f, h * 0.64f), Offset(w * 0.62f, h * 0.74f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.38f, h * 0.78f), Offset(w * 0.62f, h * 0.78f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.42f, h * 0.88f), Offset(w * 0.58f, h * 0.88f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.50f, h * 0.30f), Offset(w * 0.50f, h * 0.50f), sw * 0.9f, StrokeCap.Round)
    }
}
