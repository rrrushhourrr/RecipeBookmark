package com.recipebookmark.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipes",
    indices = [Index("folderId"), Index("cookingTimeMin"), Index("createdAt")]
)
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val folderId: Long = Folder.UNCATEGORIZED_ID,
    val sourceType: SourceType = SourceType.OTHER,
    val url: String? = null,
    val bodyText: String? = null,
    /** アプリ内部ストレージ files/images/ 配下のファイル名のみを保持する。 */
    @ColumnInfo(name = "imagePaths") val imagePaths: List<String> = emptyList(),
    /** 同じく files/images/ 配下のファイル名。OGP 画像または 1 枚目の写真。 */
    val thumbnailPath: String? = null,
    val cookingTimeMin: Int? = null,
    val memo: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 一覧カードに出す画像。サムネが無ければ 1 枚目の写真。 */
    val displayImage: String?
        get() = thumbnailPath ?: imagePaths.firstOrNull()
}
