package com.recipebookmark.ui.folder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recipebookmark.app
import com.recipebookmark.data.Folder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FolderUiState(
    val folders: List<Folder> = emptyList(),
    /** フォルダ id -> 入っているレシピ数。削除前の確認に出す。 */
    val counts: Map<Long, Int> = emptyMap()
)

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.app.repository

    val state: StateFlow<FolderUiState> = combine(
        repository.observeFolders(),
        repository.observeRecipes()
    ) { folders, recipes ->
        FolderUiState(
            folders = folders,
            counts = folders.associate { folder ->
                folder.id to recipes.count { it.folderId == folder.id }
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FolderUiState()
    )

    fun add(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.addFolder(name)
    }

    fun rename(folder: Folder, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.renameFolder(folder, name)
    }

    fun delete(folder: Folder) = viewModelScope.launch {
        repository.deleteFolder(folder)
    }

    /** 1 つ上 / 下へ入れ替える。ドラッグ操作より誤操作が少ないのでこちらにした。 */
    fun move(folder: Folder, up: Boolean) = viewModelScope.launch {
        val current = state.value.folders.toMutableList()
        val index = current.indexOfFirst { it.id == folder.id }
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target !in current.indices) return@launch
        // 「未分類」は常に先頭固定にしておきたいので入れ替え対象から外す
        if (current[target].isFixed || current[index].isFixed) return@launch
        current[index] = current[target].also { current[target] = current[index] }
        repository.reorderFolders(current)
    }
}
