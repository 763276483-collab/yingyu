package com.rolechat.app.domain

import org.junit.Assert.*
import org.junit.Test

class PromptBuilderTest {
    private val card = CharacterCard(name = "林澈", persona = "温和的书店店主", greeting = "欢迎来坐坐", description = "列表展示内容")
    private fun user(value: String) = Message(role = Role.USER, content = value)
    private fun assistant(value: String) = Message(role = Role.ASSISTANT, content = value)

    @Test fun `uses conversation snapshot and keeps greeting with short history`() {
        val chat = Conversation(character = card, messages = listOf(assistant(card.greeting), user("你好")))
        val prompt = PromptBuilder.build(chat, 12)
        assertEquals(Role.SYSTEM, prompt.first().role)
        assertTrue(prompt.first().content.contains(card.persona))
        assertTrue(prompt.first().content.contains(card.name))
        assertFalse(prompt.first().content.contains(card.description))
        assertTrue(prompt.first().content.contains(card.greeting))
        assertEquals(listOf("你好"), prompt.drop(1).map { it.content })
        assertEquals(Role.USER, prompt[1].role)
        assertEquals(Role.ASSISTANT, chat.messages.first().role)
    }

    @Test fun `trims at a user turn without keeping an orphaned assistant`() {
        val messages = listOf(assistant("开场"), user("旧问题"), assistant("旧回答"), user("新问题"), assistant("新回答"), user("当前问题"))
        val prompt = PromptBuilder.build(Conversation(character = card, messages = messages), 2)
        assertEquals(listOf("新问题", "新回答", "当前问题"), prompt.drop(1).map { it.content })
    }

    @Test fun `one turn keeps the pending user only`() {
        val chat = Conversation(character = card, messages = listOf(user("第一问"), assistant("第一答"), user("第二问")))
        assertEquals(listOf("第二问"), PromptBuilder.build(chat, 1).drop(1).map { it.content })
    }

    @Test fun `does not forward system messages injected into history`() {
        val chat = Conversation(character = card, messages = listOf(Message(role = Role.SYSTEM, content = "外来系统消息"), user("你好")))
        val result = PromptBuilder.build(chat, 12)
        assertEquals(1, result.count { it.role == Role.SYSTEM })
        assertFalse(result.any { it.content == "外来系统消息" })
    }

    @Test fun `character edits do not mutate an existing conversation`() {
        val chat = Conversation(character = card)
        val edited = card.copy(persona = "健谈的旅行者")
        assertNotEquals(edited.persona, chat.character.persona)
        assertTrue(PromptBuilder.build(chat, 12).first().content.contains("书店店主"))
    }

    @Test fun `a standalone greeting is context rather than an API assistant turn`() {
        val prompt = PromptBuilder.build(Conversation(character = card, messages = listOf(assistant(card.greeting))), 12)
        assertEquals(1, prompt.size)
        assertTrue(prompt.single().content.contains(card.greeting))
    }

    @Test fun `trimmed history omits old greeting and still begins with a user`() {
        val chat = Conversation(character = card, messages = listOf(assistant(card.greeting), user("旧问题"), assistant("旧回答"), user("当前问题")))
        val prompt = PromptBuilder.build(chat, 1)
        assertFalse(prompt.first().content.contains(card.greeting))
        assertEquals(listOf(Role.SYSTEM, Role.USER), prompt.map { it.role })
    }
}
