package com.recipebookmark.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.recipebookmark.app
import com.recipebookmark.data.Folder
import com.recipebookmark.data.Recipe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val recipe: Recipe? = null,
    val folders: List<Folder> = emptyList(),
    val loaded: Boolean = false
) {
    val folderName: String
        get() = folders.firstOrNull { it.id == recipe?.folderId }?.name ?: "未分類"
}

class DetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = application.app.repository
    private val recipeId: Long = savedStateHandle.get<Long>("recipeId") ?: 0L

    val state: StateFlow<DetailUiState> = combine(
        repository.observeRecipe(recipeId),
        repository.observeFolders()
    ) { recipe, folders ->
        DetailUiState(recipe = recipe, folders = folders, loaded = true)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState()
    )

    fun toggleFavorite() = viewModelScope.launch {
        state.value.recipe?.let { repository.setFavorite(it.id, !it.isFavorite) }
    }

    fun moveToFolder(folderId: Long) = viewModelScope.launch {
        state.value.recipe?.let { repository.moveRecipeToFolder(it, folderId) }
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        state.value.recipe?.let { repository.deleteRecipe(it) }
        onDeleted()
    }
}
