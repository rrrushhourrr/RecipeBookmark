package com.recipebookmark.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recipebookmark.app
import com.recipebookmark.data.Recipe
import com.recipebookmark.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.app.repository
    private val prefs = Prefs(application)

    /** 画面側の操作（検索語・絞り込み・並べ替え）だけを持つ内部の状態。 */
    private data class Controls(
        val query: String = "",
        val folderId: Long? = null,
        val sortOrder: SortOrder = SortOrder.COOKING_TIME_ASC,
        val timeFilter: TimeFilter = TimeFilter.ALL,
        val favoriteOnly: Boolean = false,
        val isGrid: Boolean = true
    )

    private val controls = MutableStateFlow(
        Controls(
            sortOrder = SortOrder.fromName(prefs.sortName),
            isGrid = prefs.isGrid
        )
    )

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeRecipes(),
        repository.observeFolders(),
        controls
    ) { recipes, folders, c ->
        val counts = buildMap<Long?, Int> {
            put(null, recipes.size)
            folders.forEach { folder ->
                put(folder.id, recipes.count { it.folderId == folder.id })
            }
        }

        val visible = recipes
            .asSequence()
            .filter { c.folderId == null || it.folderId == c.folderId }
            .filter { !c.favoriteOnly || it.isFavorite }
            .filter { matchesTime(it, c.timeFilter) }
            .filter { matchesQuery(it, c.query) }
            .toList()
            .sortedWith(comparatorFor(c.sortOrder))

        HomeUiState(
            recipes = visible,
            folders = folders,
            counts = counts,
            query = c.query,
            selectedFolderId = c.folderId,
            sortOrder = c.sortOrder,
            timeFilter = c.timeFilter,
            favoriteOnly = c.favoriteOnly,
            isGrid = c.isGrid,
            loaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    // --- 操作 ---------------------------------------------------------------

    fun setQuery(value: String) = controls.update { it.copy(query = value) }

    fun selectFolder(id: Long?) = controls.update { it.copy(folderId = id) }

    fun setSortOrder(order: SortOrder) {
        prefs.sortName = order.name
        controls.update { it.copy(sortOrder = order) }
    }

    fun setTimeFilter(filter: TimeFilter) = controls.update {
        // 同じチップをもう一度押したら解除
        it.copy(timeFilter = if (it.timeFilter == filter) TimeFilter.ALL else filter)
    }

    fun toggleFavoriteOnly() = controls.update { it.copy(favoriteOnly = !it.favoriteOnly) }

    fun toggleViewMode() = controls.update {
        val grid = !it.isGrid
        prefs.isGrid = grid
        it.copy(isGrid = grid)
    }

    fun clearFilters() = controls.update {
        it.copy(query = "", timeFilter = TimeFilter.ALL, favoriteOnly = false)
    }

    fun toggleFavorite(recipe: Recipe) = viewModelScope.launch {
        repository.setFavorite(recipe.id, !recipe.isFavorite)
    }

    // --- 絞り込み / 並べ替えの中身 -------------------------------------------

    private fun matchesTime(recipe: Recipe, filter: TimeFilter): Boolean = when (filter) {
        TimeFilter.ALL -> true
        TimeFilter.UNSET -> recipe.cookingTimeMin == null
        else -> recipe.cookingTimeMin != null &&
            recipe.cookingTimeMin <= (filter.maxMinutes ?: Int.MAX_VALUE)
    }

    private fun matchesQuery(recipe: Recipe, query: String): Boolean {
        val q = query.trim()
        if (q.isBlank()) return true
        return listOfNotNull(recipe.title, recipe.bodyText, recipe.memo, recipe.url)
            .any { it.contains(q, ignoreCase = true) }
    }

    private fun comparatorFor(order: SortOrder): Comparator<Recipe> = when (order) {
        // 未設定は必ず最後に回す
        SortOrder.COOKING_TIME_ASC ->
            compareBy<Recipe> { it.cookingTimeMin ?: Int.MAX_VALUE }
                .thenByDescending { it.createdAt }

        SortOrder.CREATED_DESC -> compareByDescending { it.createdAt }
        SortOrder.CREATED_ASC -> compareBy { it.createdAt }
        SortOrder.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    }
}
