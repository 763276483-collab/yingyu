package com.rolechat.app.voice

import com.rolechat.app.domain.VoiceConfig
import kotlinx.coroutines.flow.StateFlow

data class VoiceOption(val id: String, val label: String, val offline: Boolean)
data class VoiceState(
    val ready: Boolean = false,
    val voices: List<VoiceOption> = emptyList(),
    val status: String = "正在初始化系统语音…",
    val speaking: Boolean = false,
    val activeTag: String? = null,
)

/** Cloud and embedded ONNX adapters can implement this without changing chat logic. */
interface VoiceEngine {
    val state: StateFlow<VoiceState>
    fun speak(text: String, config: VoiceConfig, tag: String? = null)
    fun stop()
    fun close()
}
