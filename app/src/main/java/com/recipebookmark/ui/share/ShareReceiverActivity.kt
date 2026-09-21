package com.recipebookmark.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.recipebookmark.ui.theme.RecipeBookmarkTheme

/**
 * Instagram / X / クラシル / Gemini などの「共有」から呼ばれる受け口。
 *
 * 透過 Activity なので、元のアプリの上に薄いシートが乗っているように見える。
 * 保存したらすぐ finish() して元のアプリへ戻る。
 * 画像の取り込みや OGP 取得は Application スコープで走り続けるので、
 * ここで待つ必要はない。
 */
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent?.getStringExtra(Intent.EXTRA_SUBJECT)
        val imageUris = readImageUris(intent)

        if (sharedText.isNullOrBlank() && imageUris.isEmpty()) {
            Toast.makeText(this, "共有された内容を読み取れませんでした", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            RecipeBookmarkTheme {
                ShareSheet(
                    sharedText = sharedText,
                    imageUris = imageUris,
                    onSaved = {
                        Toast.makeText(this, "レシピに保存しました", Toast.LENGTH_SHORT).show()
                        closeBackToSource()
                    },
                    onDismiss = { closeBackToSource() }
                )
            }
        }
    }

    /** 元のアプリの上に戻る。タスクに残したくないので finish するだけ。 */
    private fun closeBackToSource() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, android.R.anim.fade_out)
    }

    private fun readImageUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND ->
                listOfNotNull(intent.parcelableExtra(Intent.EXTRA_STREAM))

            Intent.ACTION_SEND_MULTIPLE ->
                intent.parcelableArrayListExtra(Intent.EXTRA_STREAM).orEmpty()

            else -> emptyList()
        }
    }
}

// Android 13 以降は型付きの API を使う必要があるため、ここで吸収する。
private inline fun <reified T : android.os.Parcelable> Intent.parcelableExtra(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as? T
    }

private inline fun <reified T : android.os.Parcelable> Intent.parcelableArrayListExtra(
    key: String
): List<T>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra<T>(key)
    }
