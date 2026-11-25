package com.rolechat.app.appearance

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.roundToInt

/** Two separable Gaussian passes on an opaque wallpaper, not on UI text. */
object GaussianBlur {
    fun argb(pixels: IntArray, width: Int, height: Int, sigma: Float, checkCancellation: () -> Unit = {}): IntArray {
        require(width > 0 && height > 0 && width.toLong() * height == pixels.size.toLong())
        require(sigma.isFinite() && sigma >= 0)
        if (sigma < 0.1f) return pixels.copyOf()
        val spread = sigma.coerceAtMost(20f)
        val radius = ceil(spread * 3).toInt()
        val kernel = FloatArray(radius * 2 + 1) { index ->
            val distance = index - radius
            exp(-(distance * distance) / (2.0 * spread * spread)).toFloat()
        }
        val total = kernel.sum()
        kernel.indices.forEach { kernel[it] /= total }
        fun pass(source: IntArray, horizontal: Boolean): IntArray {
            val output = IntArray(source.size)
            for (y in 0 until height) {
                checkCancellation()
                for (x in 0 until width) {
                    var red = 0f; var green = 0f; var blue = 0f
                    for (k in -radius..radius) {
                        val xx = if (horizontal) (x + k).coerceIn(0, width - 1) else x
                        val yy = if (horizontal) y else (y + k).coerceIn(0, height - 1)
                        val pixel = source[yy * width + xx]
                        val weight = kernel[k + radius]
                        red += ((pixel ushr 16) and 255) * weight
                        green += ((pixel ushr 8) and 255) * weight
                        blue += (pixel and 255) * weight
                    }
                    output[y * width + x] = (255 shl 24) or (red.roundToInt().coerceIn(0, 255) shl 16) or
                        (green.roundToInt().coerceIn(0, 255) shl 8) or blue.roundToInt().coerceIn(0, 255)
                }
            }
            return output
        }
        return pass(pass(pixels, true), false)
    }
}
