package com.rolechat.app.data

import android.content.Context
import android.util.AtomicFile
import com.rolechat.app.domain.AppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Caller serializes mutations. Never silently replace a corrupt or newer file. */
class LocalStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "rolechat-v1.json"))
    private val legacyBackup = AtomicFile(File(context.noBackupFilesDir, "rolechat-v1.pre-v2.json"))

    suspend fun load(): AppData = withContext(Dispatchers.IO) {
        val bytes = try { file.readFully() } catch (e: java.io.FileNotFoundException) {
            return@withContext AppData()
        }
        val decoded = AppDataCodec.decode(bytes.decodeToString())
        if (decoded.needsMigration) {
            if (!legacyBackup.baseFile.exists()) write(legacyBackup, bytes)
            save(decoded.data)
        }
        decoded.data
    }

    suspend fun save(data: AppData) = withContext(Dispatchers.IO) {
        val bytes = AppDataCodec.encode(data).encodeToByteArray()
        write(file, bytes)
    }

    private fun write(target: AtomicFile, bytes: ByteArray) {
        val stream = target.startWrite()
        try {
            stream.write(bytes)
            target.finishWrite(stream)
        } catch (e: Exception) {
            target.failWrite(stream)
            throw e
        }
    }
}
