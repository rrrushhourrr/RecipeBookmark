package com.recipebookmark.ui.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.recipebookmark.app
import com.recipebookmark.data.Folder
import com.recipebookmark.data.Recipe
import com.recipebookmark.data.SourceType
import com.recipebookmark.util.ImageStore
import com.recipebookmark.util.OgpFetcher
import com.recipebookmark.util.UrlUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 「＋」から新規作成するときの入り口の種類。 */
enum class AddMode {
    NONE, URL, CLIPBOARD, CAMERA, PHOTO, TEXT;

    companion object {
        fun fromName(value: String?): AddMode =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}

data class EditUiState(
    val id: Long = 0,
    val title: String = "",
    val folderId: Long = Folder.UNCATEGORIZED_ID,
    val sourceType: SourceType = SourceType.OTHER,
    val url: String = "",
    val bodyText: String = "",
    val imagePaths: List<String> = emptyList(),
    val thumbnailPath: String? = null,
    val cookingTimeMin: Int? = null,
    val memo: String = "",
    val isFavorite: Boolean = false,
    val folders: List<Folder> = emptyList(),
    val isNew: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isBusy: Boolean = false,
    val busyMessage: String = "",
    val message: String? = null,
    val savedId: Long? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank() || url.isNotBlank() ||
            bodyText.isNotBlank() || imagePaths.isNotEmpty()
}

class EditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = application.app.repository

    private val recipeId: Long = savedStateHandle.get<Long>("recipeId") ?: 0L
    val addMode: AddMode = AddMode.fromName(savedStateHandle.get<String>("mode"))

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    /** 元のレシピが持っていた画像。保存時に、外された分のファイルを消すのに使う。 */
    private var originalImages: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            repository.observeFolders().collectLatest { list ->
                _state.update { it.copy(folders = list) }
            }
        }
        viewModelScope.launch {
            if (recipeId > 0) {
                repository.getRecipe(recipeId)?.let { recipe ->
                    originalImages = (recipe.imagePaths + listOfNotNull(recipe.thumbnailPath)).toSet()
                    _state.update {
                        it.copy(
                            id = recipe.id,
                            title = recipe.title,
                            folderId = recipe.folderId,
                            sourceType = recipe.sourceType,
                            url = recipe.url.orEmpty(),
                            bodyText = recipe.bodyText.orEmpty(),
                            imagePaths = recipe.imagePaths,
                            thumbnailPath = recipe.thumbnailPath,
                            cookingTimeMin = recipe.cookingTimeMin,
                            memo = recipe.memo,
                            isFavorite = recipe.isFavorite,
                            isNew = false,
                            createdAt = recipe.createdAt
                        )
                    }
                }
            }
        }
    }

    // --- 入力 ---------------------------------------------------------------

    fun setTitle(value: String) = _state.update { it.copy(title = value) }
    fun setBody(value: String) = _state.update { it.copy(bodyText = value) }
    fun setMemo(value: String) = _state.update { it.copy(memo = value) }
    fun setFolder(id: Long) = _state.update { it.copy(folderId = id) }
    fun setCookingTime(minutes: Int?) = _state.update { it.copy(cookingTimeMin = minutes) }
    fun toggleFavorite() = _state.update { it.copy(isFavorite = !it.isFavorite) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun setUrl(value: String) = _state.update {
        it.copy(url = value, sourceType = detectSource(value, it))
    }

    /** クリップボードの中身を、URL なら URL 欄に、そうでなければ本文に入れる。 */
    fun applySharedText(text: String) {
        val url = UrlUtils.firstUrl(text)
        _state.update { current ->
            if (url != null) {
                current.copy(
                    url = url,
                    sourceType = UrlUtils.sourceTypeOf(url),
                    bodyText = if (text.trim() == url) current.bodyText else text.trim(),
                    title = current.title.ifBlank { "" }
                )
            } else {
                current.copy(
                    bodyText = text.trim(),
                    sourceType = if (current.imagePaths.isEmpty()) SourceType.GEMINI else current.sourceType,
                    title = current.title.ifBlank { UrlUtils.titleFromBody(text) }
                )
            }
        }
        if (url != null) fetchOgp()
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, busyMessage = "写真を取り込んでいます…") }
            val added = uris.mapNotNull { ImageStore.importImage(getApplication(), it) }
            _state.update { current ->
                current.copy(
                    imagePaths = current.imagePaths + added,
                    // 写真が主役のレシピは「料理本」扱いにしておく（あとで変更可）
                    sourceType = if (current.url.isBlank() && current.imagePaths.isEmpty() && added.isNotEmpty()) {
                        SourceType.BOOK_PHOTO
                    } else {
                        current.sourceType
                    },
                    isBusy = false,
                    busyMessage = "",
                    message = if (added.size < uris.size) "一部の写真を取り込めませんでした" else null
                )
            }
        }
    }

    fun removeImage(name: String) = _state.update { current ->
        current.copy(
            imagePaths = current.imagePaths - name,
            thumbnailPath = if (current.thumbnailPath == name) null else current.thumbnailPath
        )
    }

    /** og:title / og:image を取りに行く。失敗しても入力内容はそのまま。 */
    fun fetchOgp() {
        val url = _state.value.url.trim()
        if (url.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, busyMessage = "リンク先を読み込んでいます…") }
            val result = OgpFetcher.fetch(getApplication(), url)
            _state.update { current ->
                current.copy(
                    title = if (current.title.isBlank()) {
                        result.title ?: UrlUtils.fallbackTitle(url)
                    } else {
                        current.title
                    },
                    thumbnailPath = current.thumbnailPath ?: result.imageName,
                    isBusy = false,
                    busyMessage = "",
                    message = if (result.title == null && result.imageName == null) {
                        "リンク先の情報は取得できませんでした。題名は手入力してください。"
                    } else {
                        null
                    }
                )
            }
        }
    }

    // --- 保存 ---------------------------------------------------------------

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, busyMessage = "保存しています…") }

            val title = current.title.ifBlank {
                when {
                    current.bodyText.isNotBlank() -> UrlUtils.titleFromBody(current.bodyText)
                    current.url.isNotBlank() -> UrlUtils.fallbackTitle(current.url)
                    else -> "名称未設定のレシピ"
                }
            }

            val recipe = Recipe(
                id = current.id,
                title = title,
                folderId = current.folderId,
                sourceType = current.sourceType,
                url = current.url.trim().ifBlank { null },
                bodyText = current.bodyText.trim().ifBlank { null },
                imagePaths = current.imagePaths,
                thumbnailPath = current.thumbnailPath ?: current.imagePaths.firstOrNull(),
                cookingTimeMin = current.cookingTimeMin,
                memo = current.memo.trim(),
                isFavorite = current.isFavorite,
                // 既存レシピを編集したときに追加日が動かないようにする
                createdAt = current.createdAt,
                updatedAt = System.currentTimeMillis()
            )

            val id = if (current.isNew) {
                repository.addRecipe(recipe)
            } else {
                repository.updateRecipe(recipe)
                recipe.id
            }

            // 編集で外した写真のファイルを後始末する
            val kept = (recipe.imagePaths + listOfNotNull(recipe.thumbnailPath)).toSet()
            ImageStore.deleteAll(getApplication(), originalImages - kept)

            _state.update { it.copy(isBusy = false, busyMessage = "", savedId = id) }
        }
    }

    private fun detectSource(url: String, current: EditUiState): SourceType {
        val detected = UrlUtils.sourceTypeOf(url)
        // 判定できないときは、今の選択（料理本など）を壊さない
        return if (detected == SourceType.OTHER && current.sourceType != SourceType.OTHER) {
            current.sourceType
        } else {
            detected
        }
    }
}
