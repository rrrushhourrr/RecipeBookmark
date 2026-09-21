package com.recipebookmark.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    /** 「未分類」は削除・リネームできない固定フォルダ。 */
    val isFixed: Boolean = false
) {
    companion object {
        /** 共有からノンストップ保存したときの行き先。DB 作成時に必ずこの id で作る。 */
        const val UNCATEGORIZED_ID = 1L

        val DEFAULT_NAMES = listOf("メイン", "副菜", "デザート", "おやつ", "丼もの", "汁物")
    }
}
