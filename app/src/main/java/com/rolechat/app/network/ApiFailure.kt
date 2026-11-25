package com.rolechat.app.network

import kotlinx.serialization.json.*
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class ApiFailure(val title: String, val details: String)
class ApiException(val failure: ApiFailure) : IOException(failure.title)

/** Extract selected error fields; never expose headers or complete response bodies. */
object ApiFailures {
    fun clean(value: String, secret: String, limit: Int = 800): String {
        var result = value
        if (secret.isNotBlank()) result = result.replace(secret.trim(), "[密钥已隐藏]")
        return result.replace(Regex("(?i)Bearer\\s+[^\\s\"'<>]+"), "Bearer [已隐藏]")
            .replace(Regex("(?i)sk-[a-z0-9_-]{6,}"), "[密钥已隐藏]")
            .replace(Regex("[\\p{Cntrl}]"), " ")
            .replace(Regex("\\s+"), " ").trim().take(limit)
    }

    fun http(status: Int, body: String, host: String, model: String, secret: String, requestId: String = ""): ApiException {
        val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()
        val error = root?.get("error")
        val errorObject = error as? JsonObject
        fun field(name: String): String = ((errorObject?.get(name) ?: root?.get(name)) as? JsonPrimitive)?.contentOrNull.orEmpty()
        val message = field("message").ifBlank { (error as? JsonPrimitive)?.contentOrNull.orEmpty() }
        val label = when (status) {
            400, 422 -> "接口参数或消息格式不被接受"
            401 -> "接口认证失败"
            402 -> "接口账户余额不足"
            403 -> "接口访问或模型权限被拒绝"
            404 -> "接口路径或模型不存在"
            429 -> "接口请求频率受限"
            in 300..399 -> "接口返回重定向，请检查最终地址"
            500, 502, 503, 504 -> "服务端暂时不可用"
            else -> "模型接口返回错误"
        }
        val details = buildList {
            add("HTTP：$status")
            add("服务：${clean(host, secret, 150)}")
            if (model.isNotBlank()) add("模型：${clean(model, secret, 150)}")
            field("code").takeIf { it.isNotBlank() }?.let { add("服务端代码：${clean(it, secret, 120)}") }
            field("type").takeIf { it.isNotBlank() }?.let { add("类型：${clean(it, secret, 120)}") }
            add(if (message.isBlank()) "服务端未提供结构化错误说明。" else "服务端说明：${clean(message, secret)}")
            if (requestId.isNotBlank()) add("请求编号：${clean(requestId, secret, 150)}")
            if (status == 400 || status == 422) add("请核对上方具体说明；此状态不能直接判定密钥错误。")
            if (status == 402) add("有效密钥也可能因 API 账户余额不足而无法生成回复。")
        }.joinToString("\n")
        return ApiException(ApiFailure("$label（HTTP $status）", details))
    }

    fun protocol(message: String, host: String, model: String, secret: String): ApiException =
        ApiException(ApiFailure("模型响应无法使用", "服务：${clean(host, secret, 150)}\n模型：${clean(model, secret, 150)}\n${clean(message, secret)}"))

    fun network(error: IOException, host: String): ApiException {
        val description = when (error) {
            is UnknownHostException -> "无法解析服务域名，请检查地址和手机 DNS/网络。"
            is InterruptedIOException -> "请求超时；可稍后手动重试，或检查网络和模型响应速度。"
            is SSLException -> "HTTPS 安全连接失败，请检查手机时间、网络或证书环境。"
            is ConnectException -> "无法连接服务，请检查网络和地址。"
            else -> "网络连接中断，请检查手机网络并手动重试。"
        }
        return ApiException(ApiFailure("模型连接失败", "服务：$host\n$description"))
    }
}
