package com.rolechat.app.data

import com.rolechat.app.domain.AppData
import org.junit.Assert.*
import org.junit.Test

class AppDataCodecTest {
    @Test fun `v1 without its omitted default version migrates with existing records`() {
        val decoded = AppDataCodec.decode("""{"characters":[{"id":"old-id","name":"林澈","persona":"原设定"}],"conversations":[]}""")
        assertTrue(decoded.needsMigration)
        assertEquals(2, decoded.data.schemaVersion)
        assertEquals("old-id", decoded.data.characters.single().id)
        assertEquals("原设定", decoded.data.characters.single().persona)
        assertEquals(0.06f, decoded.data.appearance.glassOpacity, 0.001f)
        assertTrue(decoded.data.ttsApi.autoSpeak)
    }

    @Test fun `explicit v1 needs a preserved backup before storage writes v2`() {
        assertTrue(AppDataCodec.decode("""{"schemaVersion":1}""").needsMigration)
    }

    @Test fun `v2 round trip saves the explicit version and glass settings`() {
        val data = AppData(
            appearance = com.rolechat.app.domain.ChatAppearance(glassOpacity = 0.0f, blurRadiusDp = 0f, darkText = true, textColorHex = "#111111"),
            ttsApi = com.rolechat.app.domain.TtsApiConfig(autoSpeak = true),
        )
        val encoded = AppDataCodec.encode(data)
        assertTrue(encoded.contains("\"schemaVersion\": 2"))
        val decoded = AppDataCodec.decode(encoded)
        assertFalse(decoded.needsMigration)
        assertEquals(data, decoded.data)
        assertEquals(0x111111, decoded.data.appearance.resolvedTextRgb())
        assertTrue(decoded.data.appearance.isDarkTextColor())
    }

    @Test fun `newer malformed or null version does not become an empty database`() {
        listOf("""{"schemaVersion":99}""", """{"schemaVersion":null}""", "{" ).forEach { raw ->
            assertThrows(Exception::class.java) { AppDataCodec.decode(raw) }
        }
    }
}
