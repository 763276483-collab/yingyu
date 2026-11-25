package com.rolechat.app.appearance

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rolechat.app.domain.ChatAppearance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

@Stable
class GlassBackdropState {
    var original by mutableStateOf<ImageBitmap?>(null)
    var blurred by mutableStateOf<ImageBitmap?>(null)
    var viewport by mutableStateOf(IntSize.Zero)
    var origin by mutableStateOf(Offset.Zero)
    var parallaxOffset by mutableStateOf(Offset.Zero)
    var parallaxScale by mutableStateOf(1f)
    var warning by mutableStateOf<String?>(null)
}

private data class LoadedWallpaper(val bitmap: Bitmap? = null, val warning: String? = null)
private data class BlurredWallpaper(val source: Bitmap, val radius: Float, val viewport: IntSize, val density: Float, val image: ImageBitmap)

@Composable
fun rememberGlassBackdrop(store: WallpaperStore, appearance: ChatAppearance, defaultWallpaper: (() -> Bitmap)? = null): GlassBackdropState {
    val state = remember { GlassBackdropState() }
    val density = LocalDensity.current.density
    val loaded by produceState(LoadedWallpaper(), store, appearance.wallpaperFile) {
        value = LoadedWallpaper()
        value = try {
            LoadedWallpaper(if (appearance.wallpaperFile.isBlank() && defaultWallpaper != null) {
                withContext(Dispatchers.Default) { defaultWallpaper() }
            } else store.load(appearance.wallpaperFile))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadedWallpaper(withContext(Dispatchers.Default) { WallpaperStore.defaultWallpaper() }, "壁纸暂时无法读取，已显示默认背景。")
        }
    }
    val original = remember(loaded.bitmap) { loaded.bitmap?.asImageBitmap() }
    val blurred by produceState<BlurredWallpaper?>(null, loaded.bitmap, appearance.blurRadiusDp, state.viewport, density) {
        value = null
        val source = loaded.bitmap ?: return@produceState
        val viewport = state.viewport
        if (viewport.width <= 0 || viewport.height <= 0 || appearance.blurRadiusDp <= 0) return@produceState
        value = withContext(Dispatchers.Default) {
            val ratio = (384f / max(source.width, source.height)).coerceAtMost(1f)
            val small = Bitmap.createScaledBitmap(source, (source.width * ratio).roundToInt().coerceAtLeast(1),
                (source.height * ratio).roundToInt().coerceAtLeast(1), true)
            try {
                val pixels = IntArray(small.width * small.height)
                small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
                val cover = max(viewport.width.toFloat() / source.width, viewport.height.toFloat() / source.height)
                val sigma = appearance.blurRadiusDp.coerceIn(0f, 32f) * density * ratio / cover
                val filtered = GaussianBlur.argb(pixels, small.width, small.height, sigma) { ensureActive() }
                val image = Bitmap.createBitmap(filtered, small.width, small.height, Bitmap.Config.ARGB_8888).asImageBitmap()
                BlurredWallpaper(source, appearance.blurRadiusDp, viewport, density, image)
            } finally { if (small !== source) small.recycle() }
        }
    }
    BindParallaxToBackdrop(state, enabled = appearance.parallaxEnabled, maxShiftDp = 15.dp)
    SideEffect {
        state.original = original
        state.blurred = blurred?.takeIf {
            it.source === loaded.bitmap && it.radius == appearance.blurRadiusDp && it.viewport == state.viewport && it.density == density
        }?.image
        state.warning = loaded.warning
    }
    return state
}

/** Both passes use the same center crop and parallax transform; a bubble samples at its global offset. */
private fun DrawScope.drawWallpaper(image: ImageBitmap, backdrop: GlassBackdropState, offset: Offset) {
    val original = backdrop.original ?: return
    if (backdrop.viewport.width <= 0 || backdrop.viewport.height <= 0) return
    val baseScale = max(backdrop.viewport.width.toFloat() / original.width, backdrop.viewport.height.toFloat() / original.height)
    val scale = baseScale * backdrop.parallaxScale
    val width = (original.width * scale).roundToInt()
    val height = (original.height * scale).roundToInt()
    withTransform({ translate(-offset.x + backdrop.parallaxOffset.x, -offset.y + backdrop.parallaxOffset.y) }) {
        drawImage(image,
            dstOffset = IntOffset(((backdrop.viewport.width - width) / 2f).roundToInt(),
                ((backdrop.viewport.height - height) / 2f).roundToInt()),
            dstSize = IntSize(width, height), filterQuality = FilterQuality.Medium)
    }
}

@Composable
fun WallpaperBackground(backdrop: GlassBackdropState, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().onSizeChanged { backdrop.viewport = it }
        .onGloballyPositioned { backdrop.origin = it.positionInRoot() }) {
        drawRect(Color(0xFF182234))
        backdrop.original?.let { drawWallpaper(it, backdrop, Offset.Zero) }
    }
}

@Composable
fun GlassSurface(
    backdrop: GlassBackdropState,
    appearance: ChatAppearance,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    borderAlpha: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    var position by remember { mutableStateOf(Offset.Zero) }
    Box(modifier
        .onGloballyPositioned { position = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            val localOffset = position - backdrop.origin
            (backdrop.blurred ?: backdrop.original)?.let { drawWallpaper(it, backdrop, localOffset) }
            val opacity = appearance.glassOpacity.coerceIn(0f, 0.48f)
            if (opacity > 0f) drawRect(tint.copy(alpha = opacity))
            // Subtle specular sheen scales with opacity so 100% transparency stays crystal-clear.
            val sheen = 0.035f + opacity * 0.12f
            drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = sheen), Color.Transparent, Color.White.copy(alpha = sheen * 0.3f))))
        }
        .border(BorderStroke(1.dp, Brush.linearGradient(listOf(
            Color(0xFFF4EAFF).copy(alpha = 0.72f * borderAlpha.coerceIn(0f, 1f)),
            Color.White.copy(alpha = 0.14f * borderAlpha.coerceIn(0f, 1f)),
            Color(0xFFD8ECFF).copy(alpha = 0.42f * borderAlpha.coerceIn(0f, 1f)),
        ))), shape),
        content = content)
}
