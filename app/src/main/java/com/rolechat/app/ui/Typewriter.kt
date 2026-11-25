package com.rolechat.app.ui

/** Natural pacing rules for simulated typewriter reveal of newly received AI replies. */
object TypewriterPacing {
    fun isInsideNarration(text: String, index: Int): Boolean {
        if (index < 0 || index >= text.length) return false
        var roundFull = 0
        var roundAscii = 0
        var squareFull = 0
        for (i in 0..index) {
            when (text[i]) {
                '（' -> roundFull++
                '）' -> if (i < index && roundFull > 0) roundFull--
                '(' -> roundAscii++
                ')' -> if (i < index && roundAscii > 0) roundAscii--
                '【' -> squareFull++
                '】' -> if (i < index && squareFull > 0) squareFull--
            }
        }
        return roundFull > 0 || roundAscii > 0 || squareFull > 0
    }

    fun stepSize(text: String): Int = if (text.length > 320) 2 else 1

    fun delayAfterCharMs(text: String, charIndex: Int): Long {
        if (charIndex < 0 || charIndex >= text.length) return 0L
        val length = text.length
        val longText = length > 200
        val ch = text[charIndex]
        val punctuationDelay = when (ch) {
            '。', '！', '？', '!', '?', '…' -> if (longText) 110L else 170L
            '，', '、', '；', ',', ';', '：', ':' -> if (longText) 65L else 100L
            '\n' -> if (longText) 85L else 130L
            else -> null
        }
        if (punctuationDelay != null) {
            return if (isInsideNarration(text, charIndex)) (punctuationDelay * 0.65f).toLong() else punctuationDelay
        }
        val base = when {
            length <= 80 -> 34L
            length <= 200 -> 22L
            else -> 14L
        }
        return if (isInsideNarration(text, charIndex)) (base * 0.6f).toLong().coerceAtLeast(8L) else base
    }
}
