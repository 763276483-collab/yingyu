package com.rolechat.app.appearance

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class WallpaperStore(private val context: Context) {
    private val directory = File(context.noBackupFilesDir, "wallpapers")
    private fun file(name: String): File {
        require(name.matches(Regex("wallpaper-[0-9a-f-]+\\.jpg"))) { "壁纸文件名无效。" }
        return File(directory, name)
    }

    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val staging = File.createTempFile("incoming-", ".image", directory)
        var target: File? = null
        var bitmap: Bitmap? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                staging.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 20L * 1024 * 1024) { "请选择不超过 20 MiB 的壁纸图片。" }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("无法读取所选图片，请重新选择。")
            val decoded = decode(staging)
            bitmap = decoded
            val output = file("wallpaper-${UUID.randomUUID()}.jpg")
            target = output
            output.outputStream().use { stream ->
                require(decoded.compress(Bitmap.CompressFormat.JPEG, 90, stream)) { "壁纸保存失败。" }
                stream.fd.sync()
            }
            output.name
        } catch (e: Exception) {
            target?.delete()
            throw e
        } finally { bitmap?.recycle(); staging.delete() }
    }

    suspend fun load(name: String): Bitmap = withContext(Dispatchers.IO) {
        if (name.isBlank()) defaultWallpaper()
        else BitmapFactory.decodeFile(file(name).absolutePath) ?: error("壁纸文件无法读取，请重新选择图片。")
    }

    suspend fun remove(name: String) = withContext(Dispatchers.IO) {
        if (name.matches(Regex("wallpaper-[0-9a-f-]+\\.jpg"))) file(name).delete()
        Unit
    }

    private fun decode(source: File): Bitmap {
        val decoded = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(source)) { decoder, info, _ ->
                val scale = (1600f / max(info.size.width, info.size.height)).coerceAtMost(1f)
                decoder.setTargetSize((info.size.width * scale).roundToInt().coerceAtLeast(1), (info.size.height * scale).roundToInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "图片格式暂不支持。" }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
            val raw = BitmapFactory.decodeFile(source.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 })
                ?: error("图片解码失败。")
            val matrix = Matrix()
            when (ExifInterface(source.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(270f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            }
            if (matrix.isIdentity) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also { if (it !== raw) raw.recycle() }
        }
        // Flatten transparent images once, so Gaussian filtering can use opaque RGB.
        val flat = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply { drawColor(Color.rgb(24, 32, 48)); drawBitmap(decoded, 0f, 0f, null) }
        decoded.recycle()
        return flat
    }

    companion object {
        fun defaultWallpaper(): Bitmap {
            val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = LinearGradient(0f, 0f, 720f, 1280f, intArrayOf(Color.rgb(21, 38, 60), Color.rgb(60, 54, 93), Color.rgb(24, 64, 76)), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, 720f, 1280f, paint)
            paint.shader = RadialGradient(620f, 400f, 440f, intArrayOf(0xB389AFC9.toInt(), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, 720f, 1280f, paint)
            paint.shader = RadialGradient(100f, 950f, 510f, intArrayOf(0xA58E88C0.toInt(), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, 720f, 1280f, paint)
            paint.shader = null
            paint.color = 0x14FFFFFF
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            for (index in 0..8) canvas.drawCircle(650f, 400f, 150f + index * 38f, paint)
            return bitmap
        }
    }
}
