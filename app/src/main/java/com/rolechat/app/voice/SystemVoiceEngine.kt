package com.rolechat.app.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.rolechat.app.domain.VoiceConfig
import com.rolechat.app.domain.VoiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

class SystemVoiceEngine(context: Context) : VoiceEngine {
    private val mutable = MutableStateFlow(VoiceState())
    override val state = mutable.asStateFlow()
    private val main = Handler(Looper.getMainLooper())
    private var closed = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status -> main.post {
            if (closed) return@post
            if (status != TextToSpeech.SUCCESS) {
                mutable.value = VoiceState(status = "系统语音不可用，请在手机设置中安装或启用文字转语音引擎。")
            } else {
                val available = tts?.voices.orEmpty().sortedBy { it.name }.map {
                    VoiceOption(it.name, "${it.locale.displayName} · ${it.name}", !it.isNetworkConnectionRequired)
                }
                mutable.value = VoiceState(true, available, "系统语音已就绪；离线使用需先安装对应音色。")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { main.post {
                        mutable.update { it.copy(speaking = true) }
                    } }
                    override fun onDone(utteranceId: String?) { main.post {
                        mutable.update { it.copy(speaking = false, activeTag = null) }
                    } }
                    @Deprecated("Android callback")
                    override fun onError(utteranceId: String?) { main.post {
                        mutable.update { it.copy(speaking = false, activeTag = null, status = "语音播放失败，请检查音色资源和系统语音设置。") }
                    } }
                })
            }
        } }
    }

    override fun speak(text: String, config: VoiceConfig, tag: String?) {
        fun report(value: String) { mutable.update { it.copy(status = value, speaking = false, activeTag = null) } }
        if (!state.value.ready) { report("语音尚未就绪，请检查系统文字转语音设置。"); return }
        if (config.provider != VoiceProvider.SYSTEM) { report("此语音提供商尚未接入。"); return }
        val engine = tts ?: return
        val language = Locale.forLanguageTag(config.language).language
        val voices = engine.voices.orEmpty()
        val selected = if (config.voiceId.isBlank()) voices.firstOrNull {
            it.locale.language == language && (!config.offlineOnly || !it.isNetworkConnectionRequired)
        } else voices.firstOrNull { it.name == config.voiceId }
        if (selected == null || (config.offlineOnly && selected.isNetworkConnectionRequired)) {
            report("没有可用的指定音色。请下载中文离线音色，或在人物卡中选择可用音色。")
            return
        }
        if (engine.setVoice(selected) == TextToSpeech.ERROR) { report("当前音色无法加载。"); return }
        engine.setSpeechRate(config.speed.coerceIn(0.5f, 2f))
        engine.setPitch(config.pitch.coerceIn(0.5f, 2f))
        engine.stop()
        val limit = TextToSpeech.getMaxSpeechInputLength() - 2
        var start = 0
        var index = 0
        while (start < text.length) {
            var end = (start + limit).coerceAtMost(text.length)
            if (end < text.length && text[end - 1].isHighSurrogate()) end--
            if (engine.speak(text.substring(start, end), TextToSpeech.QUEUE_ADD, null, "speech-${index++}") == TextToSpeech.ERROR) {
                report("语音请求失败，请检查系统语音引擎。")
                return
            }
            start = end
        }
        mutable.update {
            it.copy(
                speaking = true,
                activeTag = tag,
                status = if (selected.isNetworkConnectionRequired) "正在使用联网音色朗读。" else "正在使用离线音色朗读。",
            )
        }
    }

    override fun stop() { tts?.stop(); mutable.update { it.copy(speaking = false, activeTag = null) } }
    override fun close() { closed = true; tts?.stop(); tts?.shutdown(); tts = null }
}
