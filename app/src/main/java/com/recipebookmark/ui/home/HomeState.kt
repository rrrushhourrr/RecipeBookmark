package com.recipebookmark.ui.home

import com.recipebookmark.data.Folder
import com.recipebookmark.data.Recipe

enum class SortOrder(val label: String) {
    COOKING_TIME_ASC("料理時間が短い順"),
    CREATED_DESC("追加日（新しい順）"),
    CREATED_ASC("追加日（古い順）"),
    TITLE("題名順");

    companion object {
        fun fromName(value: String?): SortOrder =
            entries.firstOrNull { it.name == value } ?: COOKING_TIME_ASC
    }
}

enum class TimeFilter(val label: String, val maxMinutes: Int?) {
    ALL("すべて", null),
    WITHIN_10("10分以内", 10),
    WITHIN_15("15分以内", 15),
    WITHIN_30("30分以内", 30),
    UNSET("未設定", null)
}

data class HomeUiState(
    val recipes: List<Recipe> = emptyList(),
    val folders: List<Folder> = emptyList(),
    /** フォルダ id -> 件数。null キーは「すべて」。 */
    val counts: Map<Long?, Int> = emptyMap(),
    val query: String = "",
    val selectedFolderId: Long? = null,
    val sortOrder: SortOrder = SortOrder.COOKING_TIME_ASC,
    val timeFilter: TimeFilter = TimeFilter.ALL,
    val favoriteOnly: Boolean = false,
    val isGrid: Boolean = true,
    val loaded: Boolean = false
) {
    val isFiltering: Boolean
        get() = timeFilter != TimeFilter.ALL || favoriteOnly || query.isNotBlank()
}
