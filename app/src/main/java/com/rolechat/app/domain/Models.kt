package com.rolechat.app.domain

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
enum class VoiceProvider { SYSTEM, LOCAL_ONNX, CLOUD }

@Serializable
data class VoiceConfig(
    val provider: VoiceProvider = VoiceProvider.CLOUD,
    val voiceId: String = "male-qn-qingse",
    val language: String = "zh-CN",
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val offlineOnly: Boolean = true,
    val enginePackage: String = "",
    val modelId: String = "",
    val cloudPitch: Int = 0,
    val emotion: String = "",
    val timbre: Int = 0,
    val intensity: Int = 0,
)

@Serializable
data class CharacterCard(
    val id: String = newId(),
    val name: String = "",
    val persona: String = "",
    val greeting: String = "",
    val description: String = "",
    val voice: VoiceConfig = VoiceConfig(),
    val updatedAt: Long = System.currentTimeMillis(),
    val wallpaperFile: String = "",
    val avatarFile: String = "",
) {
    fun effectiveWallpaper(globalWallpaper: String): String = wallpaperFile.ifBlank { globalWallpaper }
    fun effectiveAvatar(globalWallpaper: String): String = avatarFile.ifBlank { effectiveWallpaper(globalWallpaper) }
}

@Serializable
enum class Role { SYSTEM, USER, ASSISTANT }

@Serializable
data class Message(
    val id: String = newId(),
    val role: Role,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class Conversation(
    val id: String = newId(),
    val character: CharacterCard,
    val createdAt: Long = System.currentTimeMillis(),
    val messages: List<Message> = emptyList(),
) {
    fun deleteMessage(messageId: String): Conversation =
        copy(messages = messages.filterNot { it.id == messageId })

    fun rewindTo(messageId: String): Conversation {
        val index = messages.indexOfFirst { it.id == messageId }
        return if (index < 0) this else copy(messages = messages.take(index + 1))
    }

    fun resetToGreeting(): Conversation = copy(
        messages = if (character.greeting.isBlank()) emptyList()
        else listOf(Message(role = Role.ASSISTANT, content = character.greeting)),
    )

    /** Returns the latest turn (user + assistant pair when present, else latest single message) and hidden count. */
    fun latestTurnWindow(): Pair<List<Message>, Int> {
        if (messages.size <= 1) return messages to 0
        val last = messages.last()
        val keepCount = if (last.role == Role.ASSISTANT && messages[messages.lastIndex - 1].role == Role.USER) 2 else 1
        val hidden = (messages.size - keepCount).coerceAtLeast(0)
        return messages.takeLast(keepCount) to hidden
    }
}

@Serializable
data class ApiConfig(
    val baseUrl: String = "",
    val model: String = "",
    val contextTurns: Int = 12,
)

@Serializable
data class TtsApiConfig(
    val endpoint: String = "https://api.minimax.cn/v1/t2a_v2",
    val model: String = "speech-2.8-hd",
    val autoSpeak: Boolean = true,
)

@Serializable
data class ChatAppearance(
    val wallpaperFile: String = "",
    val glassOpacity: Float = 0.06f,
    val blurRadiusDp: Float = 12f,
    val darkText: Boolean = false,
    val textColorHex: String = "",
    val parallaxEnabled: Boolean = true,
) {
    fun resolvedTextRgb(): Int = parseHexRgb(textColorHex) ?: if (darkText) 0x142032 else 0xFFFFFF
    fun isDarkTextColor(): Boolean {
        val rgb = resolvedTextRgb()
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b) < 145.0
    }

    companion object {
        fun normalizeHex(value: String): String {
            val cleaned = value.trim().removePrefix("#")
            if (cleaned.length != 6 || cleaned.any { it.digitToIntOrNull(16) == null }) return ""
            return "#${cleaned.uppercase()}"
        }

        fun parseHexRgb(value: String): Int? {
            val normalized = normalizeHex(value)
            return if (normalized.isEmpty()) null else normalized.substring(1).toIntOrNull(16)
        }
    }
}

@Serializable
data class AppData(
    val schemaVersion: Int = 2,
    val characters: List<CharacterCard> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val api: ApiConfig = ApiConfig(),
    val ttsApi: TtsApiConfig = TtsApiConfig(),
    val appearance: ChatAppearance = ChatAppearance(),
)
