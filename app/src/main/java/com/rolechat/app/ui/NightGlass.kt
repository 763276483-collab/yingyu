package com.rolechat.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rolechat.app.appearance.GlassBackdropState
import com.rolechat.app.appearance.WallpaperBackground
import com.rolechat.app.appearance.WallpaperStore
import com.rolechat.app.appearance.rememberGlassBackdrop
import com.rolechat.app.domain.ChatAppearance

val YingYuNightColors = darkColorScheme(
    primary = Color(0xFFB9ADFF), onPrimary = Color(0xFF211747),
    primaryContainer = Color(0xFF33294F), onPrimaryContainer = Color(0xFFE8DFFF),
    secondary = Color(0xFFAACBFA), onSecondary = Color(0xFF132A43),
    secondaryContainer = Color(0xFF24354E), onSecondaryContainer = Color(0xFFD6E6FF),
    background = Color(0xFF0B1020), onBackground = Color(0xFFE9ECF5),
    surface = Color(0xFF151C2D), onSurface = Color(0xFFE9ECF5),
    surfaceVariant = Color(0xFF253047), onSurfaceVariant = Color(0xFFB6C0D4),
    outline = Color(0xFF64718B), outlineVariant = Color(0xFF354157),
    error = Color(0xFFFFADBB), onError = Color(0xFF4D1421),
)

data class NightGlassScene(val backdrop: GlassBackdropState, val appearance: ChatAppearance)
val LocalNightGlass = staticCompositionLocalOf<NightGlassScene?> { null }

/** One shared, static night backdrop. Cards sample the same blurred image as they scroll. */
@Composable
fun NightScene(store: WallpaperStore, enabled: Boolean, content: @Composable () -> Unit) {
    val appearance = remember { ChatAppearance(glassOpacity = 0.16f, blurRadiusDp = 24f, parallaxEnabled = false) }
    val backdrop = rememberGlassBackdrop(store, appearance, ::nightWallpaper)
    val scene = remember(backdrop, appearance) { NightGlassScene(backdrop, appearance) }
    CompositionLocalProvider(LocalNightGlass provides if (enabled) scene else null) {
        Box(Modifier.fillMaxSize()) {
            if (enabled) WallpaperBackground(backdrop)
            content()
        }
    }
}

private fun nightWallpaper(): Bitmap {
    val image = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(0f, 0f, 720f, 1280f,
        intArrayOf(0xFF0B1020.toInt(), 0xFF17172D.toInt(), 0xFF0B1B29.toInt()), null, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, 720f, 1280f, paint)
    paint.shader = RadialGradient(650f, 300f, 600f,
        intArrayOf(0x70564B93, android.graphics.Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, 720f, 1280f, paint)
    paint.shader = RadialGradient(70f, 970f, 580f,
        intArrayOf(0x50326A85, android.graphics.Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, 720f, 1280f, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = 0x0DCCCAFF
    repeat(3) { i ->
        val radius = 260f + i * 55f
        canvas.drawCircle(650f, 300f, radius, paint)
    }
    return image
}
