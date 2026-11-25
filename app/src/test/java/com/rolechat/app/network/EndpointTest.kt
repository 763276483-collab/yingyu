package com.rolechat.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EndpointTest {
    @Test fun `adds endpoint with or without trailing slash`() {
        assertEquals("https://example.com/v1/chat/completions", ChatClient.endpoint("https://example.com/v1").toString())
        assertEquals("https://example.com/v1/chat/completions", ChatClient.endpoint("https://example.com/v1/").toString())
        assertEquals("https://example.com/api/compatible/v1/chat/completions", ChatClient.endpoint("https://example.com/api/compatible/v1").toString())
    }

    @Test fun `rejects insecure or credential bearing URLs`() {
        listOf("http://example.com/v1", "https://user:secret@example.com/v1", "https://example.com/v1?key=secret", "https://example.com/v1#part").forEach {
            assertThrows(IllegalArgumentException::class.java) { ChatClient.endpoint(it) }
        }
    }

    @Test fun `rejects accidentally duplicated endpoint`() {
        assertThrows(IllegalArgumentException::class.java) { ChatClient.endpoint("https://example.com/v1/chat/completions/") }
    }

    @Test fun `DeepSeek official root and v1 routes are not doubled`() {
        assertEquals("https://api.deepseek.com/chat/completions", ChatClient.endpoint("https://api.deepseek.com").toString())
        assertEquals("https://api.deepseek.com/v1/chat/completions", ChatClient.endpoint("https://api.deepseek.com/v1").toString())
        assertEquals("https://api.deepseek.com/models", ChatClient.modelsEndpoint("https://api.deepseek.com").toString())
        assertEquals("https://api.deepseek.com/v1/models", ChatClient.modelsEndpoint("https://api.deepseek.com/v1/").toString())
    }
}
