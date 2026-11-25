package com.rolechat.app.network

import com.rolechat.app.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

/** Responses are supplied by an application interceptor; no internet or real key. */
class ChatClientTest {
    private val config = ApiConfig("https://api.deepseek.com", "deepseek-flash")
    private val key = "unit-test-key"
    private val messages = listOf(Message(role = Role.USER, content = "你好"))
    private val reply = """{"choices":[{"message":{"content":"你好，很高兴见到你。"},"finish_reason":"stop"}]}"""

    private suspend fun <T> fake(status: Int = 200, body: String = reply, capture: (Request) -> Unit = {}, block: suspend (ChatClient) -> T): T {
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            capture(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("fixture")
                .header("x-request-id", "test-request")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        try { return block(ChatClient(http)) }
        finally { http.dispatcher.executorService.shutdown(); http.connectionPool.evictAll() }
    }

    @Test fun `DeepSeek request has the official endpoint json auth and explicit chat mode`() { runBlocking {
        fake(capture = { request ->
            assertEquals("https://api.deepseek.com/chat/completions", request.url.toString())
            assertEquals("POST", request.method)
            assertEquals("Bearer $key", request.header("Authorization"))
            assertEquals("application/json", request.header("Accept"))
            assertTrue(request.body!!.contentType().toString().startsWith("application/json"))
            val buffer = Buffer(); request.body!!.writeTo(buffer)
            val payload = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            assertEquals("disabled", payload["thinking"]!!.jsonObject["type"]!!.jsonPrimitive.content)
            assertEquals("deepseek-flash", payload["model"]!!.jsonPrimitive.content)
        }) { client -> assertEquals("你好，很高兴见到你。", client.complete(config, key, messages)) }
    } }

    @Test fun `provider specific parameters do not leak to other hosts`() {
        listOf("https://example.com/v1", "https://api.deepseek.com.example.org").forEach { base ->
            val payload = Json.parseToJsonElement(ChatClient.payload(config.copy(baseUrl = base), messages)).jsonObject
            assertFalse(payload.containsKey("thinking"))
        }
    }

    @Test fun `screenshot conversation keeps greeting without an assistant first API turn`() { runBlocking {
        val card = CharacterCard(name = "1", persona = "1", greeting = "1")
        val chat = Conversation(character = card, messages = listOf(Message(role = Role.ASSISTANT, content = "1"), Message(role = Role.USER, content = "1")))
        fake(capture = { request ->
            val buffer = Buffer(); request.body!!.writeTo(buffer)
            val sent = Json.parseToJsonElement(buffer.readUtf8()).jsonObject["messages"]!!.jsonArray
            assertEquals(listOf("system", "user"), sent.map { it.jsonObject["role"]!!.jsonPrimitive.content })
            assertTrue(sent.first().jsonObject["content"]!!.jsonPrimitive.content.contains("开场白"))
        }) { client -> client.complete(config, key, PromptBuilder.build(chat, 12)); Unit }
        assertEquals(2, chat.messages.size)
    } }

    @Test fun `HTTP 400 preserves provider details redacts secrets and is not retried`() { runBlocking {
        var calls = 0
        val body = """{"error":{"message":"Invalid messages; key=$key Bearer sk-another-secret-123456","code":"invalid_request_error","type":"invalid_request_error"}}"""
        fake(status = 400, body = body, capture = { calls++ }) { client ->
            try { client.complete(config, key, messages); fail("Expected API error") }
            catch (e: ApiException) {
                assertTrue(e.failure.title.contains("400"))
                assertTrue(e.failure.details.contains("Invalid messages"))
                assertTrue(e.failure.details.contains("invalid_request_error"))
                assertTrue(e.failure.details.contains("test-request"))
                assertFalse(e.failure.details.contains(key))
                assertFalse(e.failure.details.contains("sk-another-secret-123456"))
            }
        }
        assertEquals(1, calls)
    } }

    @Test fun `HTTP 402 identifies billing separately from authentication`() { runBlocking {
        fake(status = 402, body = """{"error":{"message":"Insufficient Balance"}}""") { client ->
            try { client.complete(config, key, messages); fail("Expected balance error") }
            catch (e: ApiException) { assertTrue(e.failure.title.contains("余额不足")); assertTrue(e.failure.details.contains("Insufficient Balance")) }
        }
    } }

    @Test fun `model discovery is an authenticated GET with no chat payload`() { runBlocking {
        fake(body = """{"data":[{"id":"deepseek-v4-pro"},{"id":"deepseek-flash"}]}""", capture = { request ->
            assertEquals("GET", request.method)
            assertEquals("https://api.deepseek.com/models", request.url.toString())
            assertEquals("Bearer $key", request.header("Authorization"))
            assertNull(request.body)
        }) { client -> assertEquals(listOf("deepseek-flash", "deepseek-v4-pro"), client.listModels(config, key)) }
    } }

    @Test fun `malformed successful response never dumps the raw body`() { runBlocking {
        fake(body = "<html>private-body-$key</html>") { client ->
            try { client.complete(config, key, messages); fail("Expected format error") }
            catch (e: ApiException) { assertTrue(e.failure.details.contains("JSON")); assertFalse(e.failure.details.contains("private-body")) }
        }
    } }

    @Test fun `HTTP 200 business error does not look like an empty assistant answer`() { runBlocking {
        fake(body = """{"error":{"message":"Model not available","code":"model_not_found"}}""") { client ->
            try { client.complete(config, key, messages); fail("Expected business error") }
            catch (e: ApiException) { assertTrue(e.failure.details.contains("model_not_found")) }
        }
    } }

    @Test fun `text content blocks are supported`() { runBlocking {
        fake(body = """{"choices":[{"message":{"content":[{"type":"text","text":"第一段"},{"type":"text","text":"第二段"}]}}]}""") { client ->
            assertEquals("第一段\n第二段", client.complete(config, key, messages))
        }
    } }

    @Test fun `an invalid key is rejected without echoing it`() { runBlocking {
        fake { client ->
            try { client.complete(config, "$key\nsecret-tail", messages); fail("Expected invalid key") }
            catch (e: IllegalArgumentException) { assertFalse(e.message.orEmpty().contains(key)); assertTrue(e.message.orEmpty().contains("换行")) }
        }
    } }
}
