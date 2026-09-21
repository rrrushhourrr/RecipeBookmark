package com.recipebookmark.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.recipebookmark.data.AppDatabase
import com.recipebookmark.data.Folder
import com.recipebookmark.data.Recipe
import com.recipebookmark.data.SourceType
import com.recipebookmark.util.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 全データ(JSON + 画像)を 1 つの zip にまとめて書き出す / 読み戻す。
 * 保存先・読み込み元は SAF(Storage Access Framework)の Uri なので、
 * 端末のどこにでも置けるし、ストレージ権限も要らない。
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    data class Summary(val folders: Int, val recipes: Int, val images: Int)

    fun suggestedFileName(): String {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.JAPAN)
            .format(java.util.Date())
        return "recipebookmark-backup-$stamp.zip"
    }

    // --- 書き出し -----------------------------------------------------------

    suspend fun export(target: Uri): Result<Summary> = withContext(Dispatchers.IO) {
        runCatching {
            val folders = db.folderDao().getAll()
            val recipes = db.recipeDao().getAll()

            val data = BackupData(
                version = BackupData.CURRENT_VERSION,
                exportedAt = System.currentTimeMillis(),
                folders = folders.map {
                    BackupFolder(it.id, it.name, it.sortOrder, it.isFixed)
                },
                recipes = recipes.map { it.toBackup() }
            )

            // 実際に参照されている画像だけを入れる（孤立ファイルは持っていかない）
            val imageNames = recipes
                .flatMap { it.imagePaths + listOfNotNull(it.thumbnailPath) }
                .distinct()
                .filter { ImageStore.exists(context, it) }

            val stream = context.contentResolver.openOutputStream(target, "wt")
                ?: error("書き出し先を開けませんでした")

            ZipOutputStream(stream.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(BackupData.JSON_ENTRY))
                zip.write(json.encodeToString(data).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                imageNames.forEach { name ->
                    zip.putNextEntry(ZipEntry(BackupData.IMAGE_DIR + name))
                    ImageStore.fileFor(context, name).inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }

            Summary(folders.size, recipes.size, imageNames.size)
        }
    }

    // --- 読み込み -----------------------------------------------------------

    suspend fun import(source: Uri, mode: ImportMode): Result<Summary> = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            runCatching {
                val (data, stagedImages) = unpack(source, staging)
                restore(data, stagedImages, mode)
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    /** zip を 1 回走査して、JSON と画像を取り出す。画像はいったん cache に置く。 */
    private fun unpack(source: Uri, staging: File): Pair<BackupData, Map<String, File>> {
        val images = mutableMapOf<String, File>()
        var data: BackupData? = null

        val stream = context.contentResolver.openInputStream(source)
            ?: error("バックアップファイルを開けませんでした")

        ZipInputStream(stream.buffered()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    entry.isDirectory -> Unit

                    name == BackupData.JSON_ENTRY ->
                        data = json.decodeFromString<BackupData>(
                            zip.readBytes().toString(Charsets.UTF_8)
                        )

                    name.startsWith(BackupData.IMAGE_DIR) -> {
                        // zip slip 対策: ファイル名だけを使う
                        val leaf = name.substringAfterLast('/')
                        if (leaf.isNotBlank()) {
                            val out = File(staging, leaf)
                            out.outputStream().use { zip.copyTo(it) }
                            images[leaf] = out
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val parsed = data
            ?: error("バックアップの形式が違うようです（data.json が見つかりません）")
        return parsed to images
    }

    private suspend fun restore(
        data: BackupData,
        stagedImages: Map<String, File>,
        mode: ImportMode
    ): Summary {
        if (mode == ImportMode.REPLACE) {
            db.withTransaction {
                db.recipeDao().deleteAll()
                db.folderDao().deleteAllExcept(Folder.UNCATEGORIZED_ID)
            }
            ImageStore.dir(context).listFiles()?.forEach { it.delete() }
        }

        // 画像を内部ストレージへ。名前がぶつかったら付け替えるので、対応表を作る。
        val imageNameMap = mutableMapOf<String, String>()
        stagedImages.forEach { (originalName, file) ->
            val stored = file.inputStream().use {
                ImageStore.importRaw(context, it, originalName)
            }
            imageNameMap[originalName] = stored
        }

        // フォルダ id も同様に付け替える。同名フォルダがあれば作らずそこへ寄せる。
        val existing = db.folderDao().getAll()
        val folderIdMap = mutableMapOf<Long, Long>()
        val byName = existing.associateBy { it.name }
        var nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1

        data.folders.sortedBy { it.sortOrder }.forEach { backupFolder ->
            val localId = when {
                // バックアップ側の「未分類」は、こちら側の固定「未分類」に必ず寄せる
                backupFolder.isFixed || backupFolder.id == Folder.UNCATEGORIZED_ID ->
                    Folder.UNCATEGORIZED_ID

                byName.containsKey(backupFolder.name) ->
                    byName.getValue(backupFolder.name).id

                else -> db.folderDao().insert(
                    Folder(
                        id = 0,
                        name = backupFolder.name,
                        sortOrder = nextOrder++,
                        isFixed = false
                    )
                )
            }
            folderIdMap[backupFolder.id] = localId
        }

        val recipes = data.recipes.map { backup ->
            Recipe(
                id = 0,
                title = backup.title,
                folderId = folderIdMap[backup.folderId] ?: Folder.UNCATEGORIZED_ID,
                sourceType = SourceType.fromName(backup.sourceType),
                url = backup.url,
                bodyText = backup.bodyText,
                imagePaths = backup.imagePaths.mapNotNull { imageNameMap[it] },
                thumbnailPath = backup.thumbnailPath?.let { imageNameMap[it] },
                cookingTimeMin = backup.cookingTimeMin,
                memo = backup.memo,
                isFavorite = backup.isFavorite,
                createdAt = backup.createdAt,
                updatedAt = backup.updatedAt
            )
        }
        db.recipeDao().insertAll(recipes)

        return Summary(data.folders.size, recipes.size, imageNameMap.size)
    }

    private fun Recipe.toBackup() = BackupRecipe(
        id = id,
        title = title,
        folderId = folderId,
        sourceType = sourceType.name,
        url = url,
        bodyText = bodyText,
        imagePaths = imagePaths,
        thumbnailPath = thumbnailPath,
        cookingTimeMin = cookingTimeMin,
        memo = memo,
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
