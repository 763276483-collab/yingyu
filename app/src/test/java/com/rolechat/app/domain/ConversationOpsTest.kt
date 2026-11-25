package com.rolechat.app.domain

import org.junit.Assert.*
import org.junit.Test

class ConversationOpsTest {
    private val card = CharacterCard(id = "c1", name = "月魅", persona = "设定", greeting = "欢迎光临。")
    private val m1 = Message(id = "m1", role = Role.ASSISTANT, content = "欢迎光临。")
    private val m2 = Message(id = "m2", role = Role.USER, content = "你好")
    private val m3 = Message(id = "m3", role = Role.ASSISTANT, content = "今天想聊什么？")
    private val m4 = Message(id = "m4", role = Role.USER, content = "聊聊故事")
    private val chat = Conversation(id = "conv1", character = card, messages = listOf(m1, m2, m3, m4))

    @Test fun `deleteMessage removes only the selected message`() {
        val updated = chat.deleteMessage("m2")
        assertEquals(listOf("m1", "m3", "m4"), updated.messages.map { it.id })
    }

    @Test fun `rewindTo keeps messages up to and including target`() {
        val updated = chat.rewindTo("m3")
        assertEquals(listOf("m1", "m2", "m3"), updated.messages.map { it.id })
    }

    @Test fun `rewindTo ignores unknown id without mutating history`() {
        assertEquals(chat.messages, chat.rewindTo("missing").messages)
    }

    @Test fun `resetToGreeting restores initial greeting only`() {
        val reset = chat.resetToGreeting()
        assertEquals(1, reset.messages.size)
        assertEquals(Role.ASSISTANT, reset.messages.single().role)
        assertEquals("欢迎光临。", reset.messages.single().content)
    }

    @Test fun `per-character wallpaper avatar and voice remain isolated`() {
        val a = CharacterCard(id = "a", name = "A", persona = "p", voice = VoiceConfig(voiceId = "female-shaonv", cloudPitch = 2), wallpaperFile = "wp-a.jpg")
        val b = CharacterCard(id = "b", name = "B", persona = "p", voice = VoiceConfig(voiceId = "male-qn-badao", cloudPitch = -3), wallpaperFile = "wp-b.jpg", avatarFile = "av-b.jpg")
        assertEquals("wp-a.jpg", a.effectiveWallpaper("global.jpg"))
        assertEquals("wp-a.jpg", a.effectiveAvatar("global.jpg"))
        assertEquals("wp-b.jpg", b.effectiveWallpaper("global.jpg"))
        assertEquals("av-b.jpg", b.effectiveAvatar("global.jpg"))
        assertNotEquals(a.voice.voiceId, b.voice.voiceId)
        assertNotEquals(a.voice.cloudPitch, b.voice.cloudPitch)
    }
}
