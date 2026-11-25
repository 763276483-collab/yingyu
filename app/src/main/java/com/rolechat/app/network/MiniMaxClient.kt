package com.rolechat.app.network

import com.rolechat.app.domain.TtsApiConfig
import com.rolechat.app.domain.VoiceConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class CloudVoicePreset(val id: String, val label: String, val category: String = "")

class MiniMaxClient {
    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS).callTimeout(150, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()

    companion object {
        val PRESET_VOICES = listOf(
            CloudVoicePreset("male-qn-qingse", "青涩青年（男）", "预置"),
            CloudVoicePreset("male-qn-jingying", "精英青年（男）", "预置"),
            CloudVoicePreset("male-qn-badao", "霸道青年（男）", "预置"),
            CloudVoicePreset("male-qn-daxuesheng", "青年大学生（男）", "预置"),
            CloudVoicePreset("female-shaonv", "少女（女）", "预置"),
            CloudVoicePreset("female-yujie", "御姐（女）", "预置"),
            CloudVoicePreset("female-chengshu", "成熟女性（女）", "预置"),
            CloudVoicePreset("female-tianmei", "甜美女性（女）", "预置"),
        )

        val EMOTION_OPTIONS = listOf(
            "" to "自动",
            "calm" to "平静",
            "happy" to "开心",
            "sad" to "悲伤",
            "angry" to "生气",
            "surprised" to "惊讶",
            "fearful" to "紧张",
            "whisper" to "轻语",
        )

        fun validateEndpoint(value: String): HttpUrl {
            val url = value.trim().toHttpUrlOrNull()
            require(url != null && url.isHttps && url.username.isEmpty() && url.password.isEmpty()
                && url.query == null && url.fragment == null) { "语音接口必须使用不带密码和查询参数的 HTTPS 地址。" }
            return url
        }

        fun getVoiceEndpoint(t2aEndpoint: String): HttpUrl =
            validateEndpoint(t2aEndpoint).newBuilder().encodedPath("/v1/get_voice").build()

        internal fun buildPayload(text: String, voice: VoiceConfig, config: TtsApiConfig): String = buildJsonObject {
            put("model", config.model.trim()); put("text", text); put("stream", false); put("output_format", "hex")
            putJsonObject("voice_setting") {
                put("voice_id", voice.voiceId.trim())
                put("speed", voice.speed.coerceIn(0.5f, 2f))
                put("vol", 1)
                put("pitch", voice.cloudPitch.coerceIn(-12, 12))
                voice.emotion.trim().takeIf { it.isNotEmpty() }?.let { put("emotion", it) }
            }
            val timbre = voice.timbre.coerceIn(-100, 100)
            val intensity = voice.intensity.coerceIn(-100, 100)
            if (timbre != 0 || intensity != 0) {
                putJsonObject("voice_modify") {
                    if (timbre != 0) put("timbre", timbre)
                    if (intensity != 0) put("intensity", intensity)
                }
            }
            putJsonObject("audio_setting") { put("sample_rate", 32000); put("bitrate", 128000); put("format", "mp3"); put("channel", 1) }
        }.toString()

        fun parseVoiceList(json: String, secret: String = ""): List<CloudVoicePreset> {
            val root = Json.parseToJsonElement(json).jsonObject
            val baseResp = root["base_resp"]?.jsonObject
            val code = baseResp?.get("status_code")?.jsonPrimitive?.intOrNull
            if (code != null && code != 0) {
                val cleanMsg = ApiFailures.clean(baseResp["status_msg"]?.jsonPrimitive?.contentOrNull.orEmpty(), secret, 200)
                val suffix = if (cleanMsg.isNotBlank()) "：$cleanMsg。" else "，"
                throw IllegalArgumentException("拉取音色失败（$code）${suffix}请检查语音 Key 和接口地域。")
            }
            val result = mutableListOf<CloudVoicePreset>()
            val seen = mutableSetOf<String>()
            fun collect(arrayKey: String, category: String) {
                val arr = root[arrayKey] as? JsonArray ?: return
                for (item in arr) {
                    val obj = item as? JsonObject ?: continue
                    val id = obj["voice_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                    if (id.isEmpty() || !seen.add(id)) continue
                    val name = obj["voice_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().ifBlank { id }
                    result.add(CloudVoicePreset(id, "$name（$category）", category))
                }
            }
            collect("voice_cloning", "克隆")
            collect("voice_generation", "生成")
            collect("system_voice", "系统")
            return result
        }

        fun decodeAudio(json: String, secret: String = ""): ByteArray {
            val root = Json.parseToJsonElement(json).jsonObject
            val baseResp = root["base_resp"]?.jsonObject
            val code = baseResp?.get("status_code")?.jsonPrimitive?.intOrNull
            if (code != 0) {
                val rawMsg = baseResp?.get("status_msg")?.jsonPrimitive?.contentOrNull.orEmpty()
                val cleanMsg = ApiFailures.clean(rawMsg, secret, 200)
                val suffix = if (cleanMsg.isNotBlank()) "：$cleanMsg。" else "，"
                throw IllegalArgumentException("语音服务返回错误（${code ?: "未知"}）${suffix}请检查语音 Key、接口地域（国内/国际）、余额、模型和音色 ID。")
            }
            val hex = root["data"]?.jsonObject?.get("audio")?.jsonPrimitive?.contentOrNull
            require(!hex.isNullOrEmpty() && hex.length % 2 == 0) { "语音服务没有返回有效音频。" }
            return ByteArray(hex.length / 2) { index ->
                val high = hex[index * 2].digitToIntOrNull(16)
                val low = hex[index * 2 + 1].digitToIntOrNull(16)
                require(high != null && low != null) { "语音音频编码无效。" }
                ((high shl 4) or low).toByte()
            }
        }
    }

    suspend fun listVoices(config: TtsApiConfig, key: String): List<CloudVoicePreset> {
        require(key.isNotBlank()) { "请先在接口设置中保存云端语音 API Key。" }
        val request = Request.Builder().url(getVoiceEndpoint(config.endpoint))
            .header("Authorization", "Bearer ${key.trim()}")
            .post("""{"voice_type":"all"}""".toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        return suspendCancellableCoroutine { continuation ->
            val call = http.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(IOException("拉取云端音色失败，请检查网络和语音接口地址。"))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val voices = response.use {
                            if (!it.isSuccessful) throw IOException("拉取音色返回 HTTP ${it.code}，请检查语音 Key 和接口地域。")
                            val source = it.body?.source() ?: throw IOException("拉取音色返回空响应。")
                            val limit = 2L * 1024 * 1024
                            source.request(limit + 1)
                            require(source.buffer.size <= limit) { "音色列表响应过大。" }
                            parseVoiceList(source.readUtf8(), key)
                        }
                        continuation.resume(voices)
                    } catch (e: Exception) {
                        continuation.resumeWithException(if (e is IllegalArgumentException || e is IOException) e
                            else IOException("音色列表响应格式无法识别。"))
                    }
                }
            })
        }
    }

    suspend fun synthesize(text: String, voice: VoiceConfig, config: TtsApiConfig, key: String): ByteArray {
        require(key.isNotBlank()) { "请在接口设置中单独保存云端语音 API Key。" }
        require(config.model.isNotBlank() && voice.voiceId.isNotBlank()) { "请填写语音模型和人物卡中的音色 ID。" }
        require(text.isNotBlank() && text.length <= 1500) { "本版云端朗读每次支持 1–1500 个字符，请将长回复分段。" }
        val payload = buildPayload(text, voice, config)
        val request = Request.Builder().url(validateEndpoint(config.endpoint))
            .header("Authorization", "Bearer ${key.trim()}")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        return suspendCancellableCoroutine { continuation ->
            val call = http.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(IOException("云端语音连接失败，请检查网络和接口地址。"))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val audio = response.use {
                            if (!it.isSuccessful) throw IOException("语音接口返回 HTTP ${it.code}，请检查语音 Key、余额和接口地址。")
                            val source = it.body?.source() ?: throw IOException("语音接口返回空响应。")
                            val limit = 20L * 1024 * 1024
                            source.request(limit + 1)
                            require(source.buffer.size <= limit) { "音频响应过大，请缩短朗读文本。" }
                            decodeAudio(source.readUtf8(), key)
                        }
                        continuation.resume(audio)
                    } catch (e: Exception) {
                        continuation.resumeWithException(if (e is IllegalArgumentException || e is IOException) e
                            else IOException("语音响应格式无法识别，请确认接口为 MiniMax 同步语音合成。"))
                    }
                }
            })
        }
    }
}
