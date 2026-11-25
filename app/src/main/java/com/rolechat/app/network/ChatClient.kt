package com.rolechat.app.network

import com.rolechat.app.domain.ApiConfig
import com.rolechat.app.domain.Message
import com.rolechat.app.domain.Role
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

/** HTTP transport can be injected for tests without a real API key. */
class ChatClient(private val client: OkHttpClient = defaultClient()) {
    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).build()

        private fun route(baseUrl: String, suffix: String): HttpUrl {
            val url = baseUrl.trim().toHttpUrlOrNull()
                ?: throw IllegalArgumentException("请填写有效的 HTTPS API 基础地址。")
            require(url.isHttps && url.username.isEmpty() && url.password.isEmpty() &&
                url.query == null && url.fragment == null) { "API 地址必须为 HTTPS，且不能包含密码、查询参数或片段。" }
            require(!url.encodedPath.trimEnd('/').endsWith("/chat/completions")) {
                "请填基础地址（例如 https://服务商/v1），不包含 /chat/completions。"
            }
            return url.newBuilder().apply {
                if (!url.encodedPath.endsWith('/')) addPathSegment("")
            }.addPathSegments(suffix).build()
        }

        fun endpoint(baseUrl: String): HttpUrl = route(baseUrl, "chat/completions")
        fun modelsEndpoint(baseUrl: String): HttpUrl = route(baseUrl, "models")

        private fun normalizedKey(value: String): String {
            val key = value.trim()
            require(key.isNotBlank()) { "请先填写或保存 API Key。" }
            require(key.all { it.code in 33..126 }) { "API Key 中含有空格、换行或非英文符号，请只粘贴密钥本身。" }
            return key
        }

        fun payload(config: ApiConfig, messages: List<Message>): String {
            require(config.model.isNotBlank()) { "请先在设置中填写模型 ID。" }
            val chat = messages.filter { it.role != Role.SYSTEM }
            require(chat.firstOrNull()?.role == Role.USER && chat.lastOrNull()?.role == Role.USER) {
                "发送的聊天历史必须从用户消息开始，并以本次用户消息结束。"
            }
            return buildJsonObject {
                put("model", config.model.trim())
                put("stream", false)
                if (endpoint(config.baseUrl).host == "api.deepseek.com") {
                    putJsonObject("thinking") { put("type", "disabled") }
                }
                putJsonArray("messages") {
                    messages.forEach { message -> addJsonObject {
                        put("role", message.role.name.lowercase())
                        put("content", message.content)
                    } }
                }
            }.toString()
        }
    }

    private suspend fun execute(request: Request, key: String, model: String): JsonObject = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(ApiFailures.network(e, request.url.host))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val root = response.use { res ->
                        if (!res.isSuccessful) {
                            val body = runCatching {
                                val source = res.body?.source() ?: return@runCatching ""
                                source.request(16L * 1024 + 1)
                                source.readUtf8(minOf(source.buffer.size, 16L * 1024))
                            }.getOrDefault("")
                            throw ApiFailures.http(res.code, body, request.url.host, model, key,
                                res.header("x-request-id") ?: res.header("x-ds-request-id").orEmpty())
                        }
                        val source = res.body?.source() ?: throw ApiFailures.protocol("模型返回空响应。", request.url.host, model, key)
                        val limit = 2L * 1024 * 1024
                        source.request(limit + 1)
                        if (source.buffer.size > limit) throw ApiFailures.protocol("响应超过 2 MiB，请减少回复或历史长度。", request.url.host, model, key)
                        val data = runCatching { Json.parseToJsonElement(source.readUtf8()).jsonObject }.getOrElse {
                            throw ApiFailures.protocol("返回内容不是可识别的 JSON；请确认地址属于兼容的模型接口。", request.url.host, model, key)
                        }
                        if (data["error"] != null && data["error"] != JsonNull) {
                            throw ApiFailures.http(res.code, data.toString(), request.url.host, model, key)
                        }
                        data
                    }
                    continuation.resume(root)
                } catch (e: Exception) {
                    continuation.resumeWithException(if (e is ApiException) e else
                        ApiFailures.protocol("读取模型响应失败，请重试或检查接口兼容性。", request.url.host, model, key))
                }
            }
        })
    }

    suspend fun complete(config: ApiConfig, apiKey: String, messages: List<Message>): String {
        val key = normalizedKey(apiKey)
        val url = endpoint(config.baseUrl)
        val request = Request.Builder().url(url).header("Authorization", "Bearer $key")
            .header("Accept", "application/json")
            .post(payload(config, messages).toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val root = execute(request, key, config.model)
        val choice = (root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject
        val message = choice?.get("message") as? JsonObject
        val content = message?.get("content")
        val text = when (content) {
            is JsonPrimitive -> content.contentOrNull
            is JsonArray -> content.mapNotNull { part ->
                val item = part as? JsonObject
                if ((item?.get("type") as? JsonPrimitive)?.contentOrNull == "text")
                    (item?.get("text") as? JsonPrimitive)?.contentOrNull else null
            }.joinToString("\n")
            else -> null
        }
        return text?.takeIf { it.isNotBlank() } ?: throw ApiFailures.protocol(
            if ((choice?.get("finish_reason") as? JsonPrimitive)?.contentOrNull == "length")
                "模型达到输出上限但没有返回最终正文。请检查模型参数或切换普通聊天模式。"
            else "接口未返回最终文本，请确认模型支持 Chat Completions 文本聊天。", url.host, config.model, key)
    }

    /** GET only; does not send a character prompt or generate a chat reply. */
    suspend fun listModels(config: ApiConfig, apiKey: String): List<String> {
        val key = normalizedKey(apiKey)
        val url = modelsEndpoint(config.baseUrl)
        val request = Request.Builder().url(url).header("Authorization", "Bearer $key").header("Accept", "application/json").get().build()
        val root = execute(request, key, config.model)
        val models = (root["data"] as? JsonArray)?.mapNotNull { item ->
            ((item as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull
        }?.filter { it.isNotBlank() }?.distinct()
        if (models == null) throw ApiFailures.protocol("服务没有返回标准模型列表；它可能不支持 /models，这不等于聊天不可用。", url.host, config.model, key)
        return models.sorted()
    }
}
