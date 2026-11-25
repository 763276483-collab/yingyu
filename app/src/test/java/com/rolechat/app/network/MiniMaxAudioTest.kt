package com.rolechat.app.network

import org.junit.Assert.*
import org.junit.Test

class MiniMaxAudioTest {
    @Test fun `decodes hexadecimal audio including signed bytes`() {
        assertArrayEquals(byteArrayOf(0x49, 0x44, 0x33, -1), MiniMaxClient.decodeAudio("""{"base_resp":{"status_code":0},"data":{"audio":"494433ff"}}"""))
    }
    @Test fun `rejects provider error even when HTTP succeeded`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            MiniMaxClient.decodeAudio("""{"base_resp":{"status_code":1004,"status_msg":"login fail: please check api key"},"data":null}""")
        }
        assertTrue(error.message.orEmpty().contains("1004"))
        assertTrue(error.message.orEmpty().contains("login fail"))
    }
    @Test fun `redacts secret and bearer token in provider status_msg`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            MiniMaxClient.decodeAudio(
                """{"base_resp":{"status_code":1004,"status_msg":"invalid Bearer tok-xyz and sk-secret999 and my-tts-key"},"data":null}""",
                secret = "my-tts-key",
            )
        }
        val msg = error.message.orEmpty()
        assertFalse(msg.contains("my-tts-key"))
        assertFalse(msg.contains("tok-xyz"))
        assertFalse(msg.contains("sk-secret999"))
        assertTrue(msg.contains("[密钥已隐藏]"))
    }
    @Test fun `rejects invalid hex instead of producing corrupt audio`() {
        listOf("f", "xz", "").forEach { hex ->
            assertThrows(IllegalArgumentException::class.java) {
                MiniMaxClient.decodeAudio("""{"base_resp":{"status_code":0},"data":{"audio":"$hex"}}""")
            }
        }
    }

    @Test fun `builds payload with pitch emotion and voice_modify only when set`() {
        val cfg = com.rolechat.app.domain.TtsApiConfig()
        val tuned = com.rolechat.app.domain.VoiceConfig(voiceId = "female-shaonv", speed = 1.2f, cloudPitch = 3, emotion = "happy", timbre = 25, intensity = -10)
        val json = MiniMaxClient.buildPayload("你好", tuned, cfg)
        assertTrue(json.contains("\"pitch\":3"))
        assertTrue(json.contains("\"emotion\":\"happy\""))
        assertTrue(json.contains("\"voice_modify\""))
        assertTrue(json.contains("\"timbre\":25"))
        assertTrue(json.contains("\"intensity\":-10"))
        val neutral = MiniMaxClient.buildPayload("你好", com.rolechat.app.domain.VoiceConfig(), cfg)
        assertFalse(neutral.contains("\"voice_modify\""))
        assertFalse(neutral.contains("\"emotion\""))
    }

    @Test fun `parses account voice list and derives get_voice endpoint`() {
        assertEquals("https://api.minimaxi.com/v1/get_voice", MiniMaxClient.getVoiceEndpoint("https://api.minimaxi.com/v1/t2a_v2").toString())
        val parsed = MiniMaxClient.parseVoiceList(
            """{"base_resp":{"status_code":0},"voice_cloning":[{"voice_id":"clone_1","voice_name":"我的克隆"}],"system_voice":[{"voice_id":"female-yujie","voice_name":"御姐"}]}"""
        )
        assertEquals(2, parsed.size)
        assertEquals("clone_1", parsed[0].id)
        assertEquals("female-yujie", parsed[1].id)
    }
}
