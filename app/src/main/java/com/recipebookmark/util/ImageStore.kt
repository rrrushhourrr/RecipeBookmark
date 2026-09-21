package com.recipebookmark.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * 写真はアプリ内部ストレージ(files/images/)にコピーして持つ。
 * こうしておくと、ギャラリー側で元の写真を消してもレシピからは消えない。
 * DB には「ファイル名だけ」を保存し、実体のパスはここで解決する。
 */
object ImageStore {

    private const val DIR = "images"
    private const val STAGING_DIR = "shared"
    private const val MAX_EDGE = 2048
    private const val JPEG_QUALITY = 85

    fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun fileFor(context: Context, name: String): File = File(dir(context), name)

    fun exists(context: Context, name: String?): Boolean =
        name != null && fileFor(context, name).exists()

    /**
     * 端末上の画像 Uri を読み込み、長辺 2048px 程度に縮小した JPEG として取り込む。
     * @return 保存したファイル名。失敗したら null。
     */
    suspend fun importImage(context: Context, uri: Uri): String? =
        importFrom(context) { context.contentResolver.openInputStream(uri) }

    /** 先に退避しておいたファイルから取り込む（共有経由で使う）。 */
    suspend fun importFile(context: Context, file: File): String? =
        importFrom(context) { if (file.exists()) file.inputStream() else null }
            .also { runCatching { file.delete() } }

    /**
     * 共有で受け取った画像 Uri の読み取り許可は、受け取った Activity が生きている間しか無い。
     * 縮小処理を待っていると間に合わないことがあるので、まず素のままキャッシュへ退避する。
     */
    suspend fun stageToCache(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val staging = File(context.cacheDir, STAGING_DIR).apply { mkdirs() }
            val out = File(staging, "shared-${UUID.randomUUID()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            } ?: return@runCatching null
            out
        }.getOrNull()
    }

    /** OGP 画像などのバイト列をそのまま取り込む。 */
    suspend fun importBytes(context: Context, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
            writeJpeg(context, bitmap.scaledToMaxEdge())
        }.getOrNull()
    }

    /** バックアップ zip からの復元用。すでに加工済みなのでそのまま書き出す。 */
    suspend fun importRaw(context: Context, input: InputStream, preferredName: String): String =
        withContext(Dispatchers.IO) {
            val name = uniqueName(context, preferredName)
            fileFor(context, name).outputStream().use { input.copyTo(it) }
            name
        }

    suspend fun deleteAll(context: Context, names: Collection<String>) = withContext(Dispatchers.IO) {
        names.forEach { runCatching { fileFor(context, it).delete() } }
    }

    // --- 実処理 -------------------------------------------------------------

    private suspend fun importFrom(
        context: Context,
        openStream: () -> InputStream?
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: return@runCatching null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            val decoded = openStream()?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null

            val rotation = openStream()?.use { readRotation(it) } ?: 0
            writeJpeg(context, decoded.scaledToMaxEdge().rotated(rotation))
        }.getOrNull()
    }

    private fun writeJpeg(context: Context, bitmap: Bitmap): String {
        val name = "${UUID.randomUUID()}.jpg"
        fileFor(context, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
        }
        return name
    }

    /** 復元時に既存ファイルと名前がぶつかったら別名にする。 */
    private fun uniqueName(context: Context, preferred: String): String {
        val safe = preferred.substringAfterLast('/').ifBlank { "${UUID.randomUUID()}.jpg" }
        if (!fileFor(context, safe).exists()) return safe
        val base = safe.substringBeforeLast('.', safe)
        val ext = safe.substringAfterLast('.', "jpg")
        return "$base-${UUID.randomUUID().toString().take(8)}.$ext"
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= MAX_EDGE && h / 2 >= MAX_EDGE) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun Bitmap.scaledToMaxEdge(): Bitmap {
        val longEdge = maxOf(width, height)
        if (longEdge <= MAX_EDGE) return this
        val scale = MAX_EDGE.toFloat() / longEdge
        val scaled = Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled != this) recycle()
        return scaled
    }

    private fun Bitmap.rotated(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        if (rotated != this) recycle()
        return rotated
    }

    private fun readRotation(input: InputStream): Int =
        runCatching {
            when (ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }.getOrDefault(0)
}
