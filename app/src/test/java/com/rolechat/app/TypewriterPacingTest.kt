package com.rolechat.app

import com.rolechat.app.ui.TypewriterPacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypewriterPacingTest {
    @Test
    fun longerRepliesAccelerateAutomatically() {
        val shortText = "你好呀，今天过得怎么样？"
        val mediumText = "这是一个中等长度的回复内容，用来测试打字机效果在超过八十个字符时会自动加快速度，让用户阅读体验更加自然顺畅而不会觉得拖沓。" +
            "这里再补充一些对话细节让长度超过八十个字。"
        val longText = "这是一段很长的角色扮演回复。".repeat(25)

        val shortDelay = TypewriterPacing.delayAfterCharMs(shortText, 0)
        val mediumDelay = TypewriterPacing.delayAfterCharMs(mediumText, 0)
        val longDelay = TypewriterPacing.delayAfterCharMs(longText, 0)

        assertTrue(shortDelay > mediumDelay)
        assertTrue(mediumDelay > longDelay)
        assertEquals(1, TypewriterPacing.stepSize(shortText))
        assertEquals(2, TypewriterPacing.stepSize(longText))
    }

    @Test
    fun punctuationPausesLongerThanNormalCharacters() {
        val text = "好的，我明白了。明天见！"
        val normal = TypewriterPacing.delayAfterCharMs(text, 0)
        val comma = TypewriterPacing.delayAfterCharMs(text, 2)
        val period = TypewriterPacing.delayAfterCharMs(text, 7)

        assertTrue(comma > normal)
        assertTrue(period > comma)
    }

    @Test
    fun narrationInsideBracketsRendersFasterThanDialogue() {
        val text = "（微微一笑）你好呀"
        assertTrue(TypewriterPacing.isInsideNarration(text, 2))
        assertFalse(TypewriterPacing.isInsideNarration(text, 7))

        val narrationDelay = TypewriterPacing.delayAfterCharMs(text, 2)
        val dialogueDelay = TypewriterPacing.delayAfterCharMs(text, 7)
        assertTrue(narrationDelay < dialogueDelay)
    }
}
