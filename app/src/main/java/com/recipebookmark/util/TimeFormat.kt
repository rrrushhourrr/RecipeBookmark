package com.recipebookmark.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormat {

    /** 料理時間チップの選択肢。 */
    val PRESET_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60)

    fun cookingTime(minutes: Int?): String = when {
        minutes == null -> "時間未設定"
        minutes < 60 -> "${minutes}分"
        minutes % 60 == 0 -> "${minutes / 60}時間"
        else -> "${minutes / 60}時間${minutes % 60}分"
    }

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)

    fun date(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
}
