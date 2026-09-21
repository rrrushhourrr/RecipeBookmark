package com.recipebookmark.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup

/**
 * 公開ページの OGP(og:title / og:image)だけを読む。
 *
 * ここでやらないこと:
 *   - Instagram / X の本文の自動取得
 *   - ログイン回避、非公式 API、スクレイピング
 * ログインが要るページでは単に取得に失敗するので、そのときは手入力の題名と
 * 媒体アイコンにフォールバックする。取得の成否に関わらず保存自体は必ず成功させる。
 */
object OgpFetcher {

    private const val TIMEOUT_MS = 5_000
    private const val MAX_IMAGE_BYTES = 4 * 1024 * 1024
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    data class Result(val title: String?, val imageName: String?)

    suspend fun fetch(context: Context, url: String): Result = withContext(Dispatchers.IO) {
        // Jsoup 側のタイムアウトに加えて、全体も 5 秒で必ず打ち切る
        withTimeoutOrNull(TIMEOUT_MS.toLong() + 1_000L) {
            runCatching {
                val doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get()

                val title = doc.metaContent("og:title")
                    ?: doc.metaContent("twitter:title")
                    ?: doc.title().ifBlank { null }

                val imageUrl = doc.absMeta("og:image") ?: doc.absMeta("twitter:image")
                val imageName = imageUrl?.let { downloadImage(context, it) }

                Result(title?.trim()?.take(120), imageName)
            }.getOrElse { Result(null, null) }
        } ?: Result(null, null)
    }

    private suspend fun downloadImage(context: Context, imageUrl: String): String? =
        runCatching {
            val bytes = Jsoup.connect(imageUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .ignoreContentType(true)
                .maxBodySize(MAX_IMAGE_BYTES)
                .execute()
                .bodyAsBytes()
            ImageStore.importBytes(context, bytes)
        }.getOrNull()

    private fun org.jsoup.nodes.Document.metaContent(property: String): String? =
        selectFirst("meta[property=$property]")?.attr("content")?.ifBlank { null }
            ?: selectFirst("meta[name=$property]")?.attr("content")?.ifBlank { null }

    private fun org.jsoup.nodes.Document.absMeta(property: String): String? =
        selectFirst("meta[property=$property]")?.absUrl("content")?.ifBlank { null }
            ?: selectFirst("meta[name=$property]")?.absUrl("content")?.ifBlank { null }
}
