package com.recipebookmark.data

/** レシピの出どころ。共有テキストの URL ホスト名から自動判定する。 */
enum class SourceType(val label: String) {
    INSTAGRAM("Instagram"),
    X("X"),
    KURASHIRU("クラシル"),
    BOOK_PHOTO("料理本"),
    GEMINI("Gemini"),
    OTHER("その他");

    companion object {
        fun fromName(value: String?): SourceType =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
