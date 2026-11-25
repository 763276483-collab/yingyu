package com.rolechat.app.data

import com.rolechat.app.domain.AppData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

data class DecodedAppData(val data: AppData, val needsMigration: Boolean)

/** Missing v1 default fields are intentional; invalid explicit versions are errors. */
object AppDataCodec {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    fun decode(raw: String): DecodedAppData {
        val root = json.parseToJsonElement(raw).jsonObject
        val version = if ("schemaVersion" in root) root.getValue("schemaVersion").jsonPrimitive.intOrNull
            ?: error("数据版本字段无效。") else 1
        require(version in 1..2) { "数据版本暂不支持，请保留原文件后升级应用。" }
        return DecodedAppData(json.decodeFromString<AppData>(raw).copy(schemaVersion = 2), version == 1)
    }

    fun encode(data: AppData): String {
        require(data.schemaVersion == 2) { "保存版本不匹配。" }
        return json.encodeToString(data)
    }
}
