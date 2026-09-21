package com.recipebookmark.util

import android.content.Context

/** 表示モードや並べ替えの選択を、次に開いたときも覚えておくための小さな置き場。 */
class Prefs(context: Context) {

    private val prefs = context.getSharedPreferences("recipebookmark", Context.MODE_PRIVATE)

    var isGrid: Boolean
        get() = prefs.getBoolean(KEY_GRID, true)
        set(value) = prefs.edit().putBoolean(KEY_GRID, value).apply()

    var sortName: String
        get() = prefs.getString(KEY_SORT, null) ?: ""
        set(value) = prefs.edit().putString(KEY_SORT, value).apply()

    private companion object {
        const val KEY_GRID = "is_grid"
        const val KEY_SORT = "sort_order"
    }
}
