package com.rolechat.app.domain

/** A turn begins at a user message; trimming never keeps a reply without its user. */
object PromptBuilder {
    fun build(conversation: Conversation, contextTurns: Int): List<Message> {
        val card = conversation.character
        val history = conversation.messages.filter { it.role != Role.SYSTEM }
        val starts = history.indices.filter { history[it].role == Role.USER }
        val start = starts.takeLast(contextTurns.coerceIn(1, 50)).firstOrNull()
        val opening = if (start == starts.firstOrNull()) history.takeWhile { it.role != Role.USER }
            .filter { it.role == Role.ASSISTANT }.joinToString("\n") { it.content } else ""
        val system = Message(role = Role.SYSTEM, content = """
            你正在扮演以下虚构角色，请使用该角色的口吻与用户自然交流。
            角色名字：${card.name}
            角色设定：
            ${card.persona}
        """.trimIndent() + if (opening.isBlank()) "" else "\n角色已经向用户说过的开场白（作为上下文，不必重复）：\n$opening")
        // Keep the visible greeting intact, but start API turns with a real user.
        return listOf(system) + if (start == null) emptyList() else history.drop(start)
    }
}
