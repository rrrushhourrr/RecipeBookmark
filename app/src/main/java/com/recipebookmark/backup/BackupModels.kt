package com.recipebookmark.backup

import kotlinx.serialization.Serializable

/** zip の中の data.json の形。将来フィールドを足せるよう version を持たせておく。 */
@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long = 0L,
    val folders: List<BackupFolder> = emptyList(),
    val recipes: List<BackupRecipe> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val JSON_ENTRY = "data.json"
        const val IMAGE_DIR = "images/"
    }
}

@Serializable
data class BackupFolder(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val isFixed: Boolean = false
)

@Serializable
data class BackupRecipe(
    val id: Long,
    val title: String,
    val folderId: Long,
    val sourceType: String,
    val url: String? = null,
    val bodyText: String? = null,
    val imagePaths: List<String> = emptyList(),
    val thumbnailPath: String? = null,
    val cookingTimeMin: Int? = null,
    val memo: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

enum class ImportMode {
    /** 今のデータを残したまま足す。 */
    MERGE,

    /** 今のデータを全部消してから入れ直す。 */
    REPLACE
}
