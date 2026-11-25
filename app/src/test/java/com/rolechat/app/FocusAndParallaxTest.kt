package com.rolechat.app

import com.rolechat.app.appearance.ParallaxFilter
import com.rolechat.app.domain.CharacterCard
import com.rolechat.app.domain.Conversation
import com.rolechat.app.domain.Message
import com.rolechat.app.domain.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FocusAndParallaxTest {
    private val card = CharacterCard(name = "Lin", persona = "温柔", greeting = "欢迎光临。")

    @Test
    fun latestTurnWindowKeepsGreetingOnlyWhenNoRepliesYet() {
        val chat = Conversation(
            character = card,
            messages = listOf(Message(id = "g", role = Role.ASSISTANT, content = "欢迎光临。")),
        )
        val (visible, hidden) = chat.latestTurnWindow()
        assertEquals(listOf("g"), visible.map { it.id })
        assertEquals(0, hidden)
    }

    @Test
    fun latestTurnWindowKeepsLatestUserAndAssistantPair() {
        val chat = Conversation(
            character = card,
            messages = listOf(
                Message(id = "g", role = Role.ASSISTANT, content = "开场白"),
                Message(id = "u1", role = Role.USER, content = "第一句"),
                Message(id = "a1", role = Role.ASSISTANT, content = "第一回"),
                Message(id = "u2", role = Role.USER, content = "第二句"),
                Message(id = "a2", role = Role.ASSISTANT, content = "第二回"),
            ),
        )
        val (visible, hidden) = chat.latestTurnWindow()
        assertEquals(listOf("u2", "a2"), visible.map { it.id })
        assertEquals(3, hidden)
    }

    @Test
    fun latestTurnWindowKeepsPendingUserMessageWhileWaitingForReply() {
        val chat = Conversation(
            character = card,
            messages = listOf(
                Message(id = "g", role = Role.ASSISTANT, content = "开场白"),
                Message(id = "u1", role = Role.USER, content = "在吗"),
            ),
        )
        val (visible, hidden) = chat.latestTurnWindow()
        assertEquals(listOf("u1"), visible.map { it.id })
        assertEquals(1, hidden)
    }

    @Test
    fun parallaxFilterMovesWithTiltStaysBoundedAndDecaysBackToCenter() {
        val filter = ParallaxFilter()
        repeat(15) { filter.onGyroSample(omegaX = 1.5f, omegaY = -2.0f, dtSec = 0.02f) }
        assertTrue(filter.x < -0.1f)
        assertTrue(filter.y > 0.1f)
        assertTrue(abs(filter.x) <= 1f && abs(filter.y) <= 1f)

        val peakX = abs(filter.x)
        repeat(120) { filter.onGyroSample(omegaX = 0f, omegaY = 0f, dtSec = 0.05f) }
        assertTrue(abs(filter.x) < peakX * 0.25f)
    }
}
