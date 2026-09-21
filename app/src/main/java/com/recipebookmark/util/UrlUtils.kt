package com.recipebookmark.util

import com.recipebookmark.data.SourceType
import java.util.regex.Pattern

object UrlUtils {

    private val URL_PATTERN: Pattern =
        Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE)

    /** 共有テキストから最初の URL を取り出す。無ければ null。 */
    fun firstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = URL_PATTERN.matcher(text)
        if (!matcher.find()) return null
        // 文末の句読点や閉じ括弧を巻き込みやすいので落とす
        return matcher.group().trimEnd('.', ',', ')', ']', '。', '、', '）')
    }

    /** URL のホスト名から媒体を判定する。 */
    fun sourceTypeOf(url: String?): SourceType {
        val host = hostOf(url)?.lowercase() ?: return SourceType.OTHER
        return when {
            host.contains("instagram.com") -> SourceType.INSTAGRAM
            host == "x.com" || host.endsWith(".x.com") -> SourceType.X
            host.contains("twitter.com") -> SourceType.X
            host.contains("kurashiru.com") -> SourceType.KURASHIRU
            host.contains("gemini.google.com") -> SourceType.GEMINI
            host.contains("g.co") -> SourceType.GEMINI
            else -> SourceType.OTHER
        }
    }

    fun hostOf(url: String?): String? =
        runCatching { java.net.URI(url ?: return null).host }.getOrNull()

    /**
     * URL を含まない共有テキスト（Gemini の提案レシピなど）は本文として扱う。
     * 題名の初期値は最初の意味のある 1 行。
     */
    fun titleFromBody(text: String?): String {
        val line = text?.lineSequence()
            ?.map { it.trim().trimStart('#', '*', '-', '・', ' ') }
            ?.firstOrNull { it.isNotBlank() }
            ?: return ""
        return line.take(80)
    }

    /** URL しか無いときの、せめてもの仮タイトル。 */
    fun fallbackTitle(url: String?): String {
        val source = sourceTypeOf(url)
        if (source != SourceType.OTHER) return "${source.label}のレシピ"
        return hostOf(url) ?: "レシピ"
    }
}
