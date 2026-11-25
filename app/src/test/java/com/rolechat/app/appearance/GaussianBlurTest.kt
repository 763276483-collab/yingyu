package com.rolechat.app.appearance

import org.junit.Assert.*
import org.junit.Test

class GaussianBlurTest {
    @Test fun `flat wallpaper stays exactly flat including edges`() {
        val color = 0xFF4B79AB.toInt()
        assertArrayEquals(IntArray(35) { color }, GaussianBlur.argb(IntArray(35) { color }, 7, 5, 2f))
    }

    @Test fun `a bright point diffuses symmetrically without modifying the source`() {
        val source = IntArray(49) { 0xFF000000.toInt() }.apply { this[24] = 0xFFFFFFFF.toInt() }
        val copy = source.copyOf()
        val output = GaussianBlur.argb(source, 7, 7, 1f)
        assertArrayEquals(copy, source)
        assertTrue((output[24] and 255) in 1..254)
        assertTrue((output[23] and 255) > 0)
        assertEquals(output[23], output[25])
        assertEquals(output[17], output[31])
    }

    @Test fun `zero sigma preserves the sharp image using a distinct buffer`() {
        val source = intArrayOf(0xFF112233.toInt(), 0xFFAABBCC.toInt())
        val output = GaussianBlur.argb(source, 2, 1, 0f)
        assertArrayEquals(source, output)
        assertNotSame(source, output)
    }

    @Test fun `invalid dimensions and non finite sigma are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { GaussianBlur.argb(IntArray(4), 3, 2, 1f) }
        assertThrows(IllegalArgumentException::class.java) { GaussianBlur.argb(IntArray(4), 2, 2, Float.NaN) }
    }
}
