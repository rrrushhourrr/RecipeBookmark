package com.recipebookmark.ui.share

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recipebookmark.app
import com.recipebookmark.data.Folder
import com.recipebookmark.data.Recipe
import com.recipebookmark.data.SourceType
import com.recipebookmark.util.ImageStore
import com.recipebookmark.util.OgpFetcher
import com.recipebookmark.util.UrlUtils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareUiState(
    val title: String = "",
    val folderId: Long = Folder.UNCATEGORIZED_ID,
    val cookingTimeMin: Int? = null,
    val folders: List<Folder> = emptyList(),
    val url: String? = null,
    val bodyText: String? = null,
    val sourceType: SourceType = SourceType.OTHER,
    val imageCount: Int = 0,
    val isLoadingLink: Boolean = false
)

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.app.repository
    private val appScope = application.app.appScope

    private val _state = MutableStateFlow(ShareUiState())
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    private var prepared = false

    /** 画像の取り込みと OGP 取得。シートを閉じたあとも走り切らせたいので appScope に置く。 */
    private var imageJob: Deferred<List<String>>? = null
    private var ogpJob: Deferred<OgpFetcher.Result>? = null

    /** 題名をユーザーが自分で触ったら、OGP の結果で上書きしない。 */
    private var titleTouched = false

    init {
        viewModelScope.launch {
            repository.observeFolders().collectLatest { folders ->
                _state.update { it.copy(folders = folders) }
            }
        }
    }

    fun prepare(sharedText: String?, imageUris: List<Uri>) {
        if (prepared) return
        prepared = true

        val context = getApplication<Application>()
        val url = UrlUtils.firstUrl(sharedText)

        val body = when {
            sharedText.isNullOrBlank() -> null
            // URL だけが共有されたときは本文として持たない
            sharedText.trim() == url -> null
            else -> sharedText.trim()
        }

        val source = when {
            url != null -> UrlUtils.sourceTypeOf(url)
            imageUris.isNotEmpty() -> SourceType.BOOK_PHOTO
            // URL の無いテキスト（Gemini の提案レシピなど）
            !sharedText.isNullOrBlank() -> SourceType.GEMINI
            else -> SourceType.OTHER
        }

        _state.update {
            it.copy(
                title = if (url == null) UrlUtils.titleFromBody(sharedText) else "",
                url = url,
                bodyText = body,
                sourceType = source,
                imageCount = imageUris.size,
                isLoadingLink = url != null
            )
        }

        if (imageUris.isNotEmpty()) {
            imageJob = appScope.async {
                // 共有元の許可が切れる前に、まず素のままキャッシュへ退避してから加工する
                val staged = imageUris.mapNotNull { ImageStore.stageToCache(context, it) }
                staged.mapNotNull { ImageStore.importFile(context, it) }
            }
        }

        if (url != null) {
            val job = appScope.async { OgpFetcher.fetch(context, url) }
            ogpJob = job
            viewModelScope.launch {
                val result = runCatching { job.await() }.getOrNull()
                _state.update { current ->
                    current.copy(
                        title = if (!titleTouched && current.title.isBlank()) {
                            result?.title ?: UrlUtils.fallbackTitle(url)
                        } else {
                            current.title
                        },
                        isLoadingLink = false
                    )
                }
            }
        }
    }

    fun setTitle(value: String) {
        titleTouched = true
        _state.update { it.copy(title = value) }
    }

    fun setFolder(id: Long) = _state.update { it.copy(folderId = id) }

    fun setCookingTime(minutes: Int?) = _state.update { it.copy(cookingTimeMin = minutes) }

    /**
     * 保存。何も選ばずに押しても「未分類」に入る。
     * 画像取り込みと OGP の完了待ちは appScope 側でやるので、画面はすぐ閉じてよい。
     */
    fun save() {
        val snapshot = _state.value
        val typedTitle = snapshot.title.trim()

        appScope.launch {
            val images = runCatching { imageJob?.await() }.getOrNull().orEmpty()
            val ogp = runCatching { ogpJob?.await() }.getOrNull()

            val ogpTitle = ogp?.title
            val title = when {
                typedTitle.isNotBlank() -> typedTitle
                !ogpTitle.isNullOrBlank() -> ogpTitle
                !snapshot.bodyText.isNullOrBlank() -> UrlUtils.titleFromBody(snapshot.bodyText)
                snapshot.url != null -> UrlUtils.fallbackTitle(snapshot.url)
                else -> "名称未設定のレシピ"
            }

            repository.addRecipe(
                Recipe(
                    title = title,
                    folderId = snapshot.folderId,
                    sourceType = snapshot.sourceType,
                    url = snapshot.url,
                    bodyText = snapshot.bodyText,
                    imagePaths = images,
                    thumbnailPath = images.firstOrNull() ?: ogp?.imageName,
                    cookingTimeMin = snapshot.cookingTimeMin,
                    memo = "",
                    isFavorite = false
                )
            )
        }
    }
}
