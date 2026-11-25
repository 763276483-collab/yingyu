package com.rolechat.app.appearance

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.exp

/**
 * Pure posture-adaptive parallax filter that maps gyroscope angular velocity
 * (or accelerometer tilt fallback) into a smooth, bounded [-1, 1] 2D offset.
 * Automatically re-centers around whatever angle the user holds the phone.
 */
class ParallaxFilter {
    var x: Float = 0f
        private set
    var y: Float = 0f
        private set

    private var targetX: Float = 0f
    private var targetY: Float = 0f
    private var baseAccelX: Float = 0f
    private var baseAccelY: Float = 0f
    private var accelInitialized: Boolean = false

    fun reset() {
        x = 0f
        y = 0f
        targetX = 0f
        targetY = 0f
        accelInitialized = false
    }

    fun onGyroSample(omegaX: Float, omegaY: Float, dtSec: Float) {
        if (!omegaX.isFinite() || !omegaY.isFinite() || dtSec <= 0f || dtSec > 0.25f) return
        val gain = 0.65f
        val decay = exp(-dtSec / 1.35f)
        targetX = ((targetX + omegaY * dtSec * gain) * decay).coerceIn(-1f, 1f)
        targetY = ((targetY + omegaX * dtSec * gain) * decay).coerceIn(-1f, 1f)
        val smooth = (dtSec * 14f).coerceIn(0.08f, 0.45f)
        x += (targetX - x) * smooth
        y += (targetY - y) * smooth
    }

    fun onAccelerometerSample(ax: Float, ay: Float) {
        if (!ax.isFinite() || !ay.isFinite()) return
        if (!accelInitialized) {
            baseAccelX = ax
            baseAccelY = ay
            accelInitialized = true
            return
        }
        baseAccelX += (ax - baseAccelX) * 0.03f
        baseAccelY += (ay - baseAccelY) * 0.03f
        targetX = ((ax - baseAccelX) / 2.8f).coerceIn(-1f, 1f)
        targetY = (-(ay - baseAccelY) / 2.8f).coerceIn(-1f, 1f)
        x += (targetX - x) * 0.16f
        y += (targetY - y) * 0.16f
    }
}

/**
 * Lifecycle-aware gyroscope/accelerometer parallax binder for [GlassBackdropState].
 * Writes directly to draw-phase state so sensor updates never recompose ChatScreen.
 */
@Composable
fun BindParallaxToBackdrop(
    backdrop: GlassBackdropState,
    enabled: Boolean,
    maxShiftDp: Dp = 15.dp,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val maxShiftPx = with(LocalDensity.current) { maxShiftDp.toPx() }
    val filter = remember { ParallaxFilter() }

    DisposableEffect(backdrop, context, lifecycleOwner, enabled, maxShiftPx) {
        if (!enabled) {
            filter.reset()
            backdrop.parallaxOffset = Offset.Zero
            backdrop.parallaxScale = 1f
            return@DisposableEffect onDispose {}
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val gyro = manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accel = if (gyro == null) manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) else null
        val activeSensor = gyro ?: accel
        if (manager == null || activeSensor == null) {
            backdrop.parallaxOffset = Offset.Zero
            backdrop.parallaxScale = 1f
            return@DisposableEffect onDispose {}
        }
        backdrop.parallaxScale = 1.09f
        var lastTimestampNs = 0L
        var registered = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    val ts = event.timestamp
                    if (lastTimestampNs != 0L) {
                        val dt = (ts - lastTimestampNs) / 1_000_000_000f
                        filter.onGyroSample(event.values[0], event.values[1], dt)
                        backdrop.parallaxOffset = Offset(filter.x * maxShiftPx, filter.y * maxShiftPx)
                    }
                    lastTimestampNs = ts
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    filter.onAccelerometerSample(event.values[0], event.values[1])
                    backdrop.parallaxOffset = Offset(filter.x * maxShiftPx, filter.y * maxShiftPx)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        fun start() {
            if (!registered) {
                lastTimestampNs = 0L
                registered = manager.registerListener(listener, activeSensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        fun stop() {
            if (registered) {
                manager.unregisterListener(listener)
                registered = false
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
            filter.reset()
            backdrop.parallaxOffset = Offset.Zero
            backdrop.parallaxScale = 1f
        }
    }
}
