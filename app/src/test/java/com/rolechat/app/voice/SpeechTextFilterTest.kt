package com.rolechat.app.voice

import org.junit.Assert.*
import org.junit.Test

class SpeechTextFilterTest {
    @Test fun `strips Chinese and ASCII parenthetical narration while keeping spoken dialogue`() {
        val raw = "（铁笼栏杆被攥得咯咯响，紫色长发垂下来）先生…买我吧… (尾音发颤，轻轻蹭着栏杆) 我吃得很少的…"
        assertEquals("先生…买我吧… 我吃得很少的…", SpeechTextFilter.spokenOnly(raw))
    }

    @Test fun `strips square brackets and markdown asterisk actions`() {
        val raw = "【轻叹一口气】*起身倒了杯茶* 今天过得怎么样？"
        assertEquals("今天过得怎么样？", SpeechTextFilter.spokenOnly(raw))
    }

    @Test fun `returns empty string when message contains only narration`() {
        val raw = "（远处执事脚步声愈发靠近）"
        assertEquals("", SpeechTextFilter.spokenOnly(raw))
    }

    @Test fun `keeps plain dialogue untouched`() {
        val raw = "你好，欢迎光临。"
        assertEquals("你好，欢迎光临。", SpeechTextFilter.spokenOnly(raw))
    }
}
