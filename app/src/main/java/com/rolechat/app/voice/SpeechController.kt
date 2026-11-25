package com.rolechat.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.rolechat.app.domain.TtsApiConfig
import com.rolechat.app.domain.VoiceConfig
import com.rolechat.app.domain.VoiceProvider
import com.rolechat.app.network.MiniMaxClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Explicit provider selection: never silently send offline speech to the cloud. */
class SpeechController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val credentials: suspend () -> Pair<TtsApiConfig, String>,
) : VoiceEngine {
    private val system = SystemVoiceEngine(context)
    private val cloud = MiniMaxClient()
    private val status = MutableStateFlow<String?>("云端语音：请先保存语音 Key，并在人物卡中设置音色 ID。")
    private val cloudSpeaking = MutableStateFlow(false)
    private val cloudTag = MutableStateFlow<String?>(null)
    private var job: Job? = null
    override val state = combine(system.state, status, cloudSpeaking, cloudTag) { local, cloudStatus, isCloudSpeaking, tag ->
        local.copy(
            ready = cloudStatus != null || local.ready,
            status = cloudStatus ?: local.status,
            speaking = isCloudSpeaking || local.speaking,
            activeTag = if (isCloudSpeaking) tag else local.activeTag,
        )
    }.stateIn(scope, SharingStarted.Eagerly, VoiceState())

    override fun speak(text: String, config: VoiceConfig, tag: String?) {
        stop()
        val spoken = SpeechTextFilter.spokenOnly(text)
        if (spoken.isBlank()) { status.value = "本句仅含旁白，已跳过朗读。"; return }
        if (config.provider == VoiceProvider.SYSTEM) { status.value = null; system.speak(spoken, config, tag); return }
        if (config.provider != VoiceProvider.CLOUD) { status.value = "内置本地模型尚未接入，可选择系统语音引擎。"; return }
        status.value = "正在生成云端语音…"
        cloudSpeaking.value = true
        cloudTag.value = tag
        job = scope.launch {
            var file: File? = null
            var player: MediaPlayer? = null
            try {
                val (api, key) = credentials()
                val bytes = cloud.synthesize(spoken, config, api, key)
                // Keep the tiny file ownership step synchronous: cancellation cannot orphan it.
                val target = File.createTempFile("rolechat-speech-", ".mp3", context.cacheDir)
                file = target
                withContext(Dispatchers.IO) { target.writeBytes(bytes) }
                val active = MediaPlayer()
                player = active
                active.setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                active.setDataSource(target.absolutePath)
                suspendCancellableCoroutine<Unit> { continuation ->
                    active.setOnPreparedListener {
                        try {
                            it.start()
                            status.value = "正在播放云端语音。"
                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(IllegalStateException("音频无法开始播放。"))
                        }
                    }
                    active.setOnCompletionListener { if (continuation.isActive) continuation.resume(Unit) }
                    active.setOnErrorListener { _, _, _ ->
                        if (continuation.isActive) continuation.resumeWithException(IllegalStateException("音频播放失败，请稍后重试。"))
                        true
                    }
                    active.prepareAsync()
                }
                status.value = "朗读完成。"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status.value = e.message ?: "云端语音生成失败。"
            } finally {
                cloudSpeaking.value = false
                cloudTag.value = null
                player?.release()
                file?.delete()
            }
        }
    }

    override fun stop() {
        job?.cancel(); job = null
        cloudSpeaking.value = false; cloudTag.value = null
        system.stop()
        status.value = "朗读已停止。"
    }
    override fun close() { stop(); system.close() }
}
