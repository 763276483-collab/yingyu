package com.rolechat.app.network

import org.junit.Assert.*
import org.junit.Test
import java.net.UnknownHostException
import java.net.SocketTimeoutException

class ApiFailuresTest {
    @Test fun `status categories distinguish credentials balance limits and server faults`() {
        mapOf(400 to "格式", 401 to "认证", 402 to "余额", 403 to "权限", 404 to "不存在", 429 to "频率", 503 to "服务端").forEach { (status, label) ->
            assertTrue(ApiFailures.http(status, "{}", "api.deepseek.com", "deepseek-flash", "test-key").failure.title.contains(label))
        }
    }
    @Test fun `non JSON error pages are never dumped into the UI`() {
        val error = ApiFailures.http(502, "<html>private-content</html>", "example.com", "model", "test-key")
        assertTrue(error.failure.details.contains("未提供结构化"))
        assertFalse(error.failure.details.contains("private-content"))
    }
    @Test fun `provider messages have bounded length and no known secret`() {
        val secret = "realistic-test-secret"
        val result = ApiFailures.clean("$secret " + "x".repeat(5000), secret)
        assertTrue(result.length <= 800)
        assertFalse(result.contains(secret))
        assertFalse(ApiFailures.clean("Bearer abcdefghijklmn", secret).contains("abcdefghijklmn"))
    }
    @Test fun `network messages identify DNS and timeout without raw exception details`() {
        val dns = ApiFailures.network(UnknownHostException("private-raw-message"), "example.com")
        assertTrue(dns.failure.details.contains("DNS"))
        assertFalse(dns.failure.details.contains("private-raw-message"))
        assertTrue(ApiFailures.network(SocketTimeoutException(), "example.com").failure.details.contains("超时"))
    }
}
