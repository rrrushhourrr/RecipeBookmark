package com.recipebookmark.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipebookmark.ui.components.EmptyState
import com.recipebookmark.ui.components.FolderChipRow
import com.recipebookmark.ui.components.RecipeGridCard
import com.recipebookmark.ui.components.RecipeListCard

@Composable
fun HomeScreen(
    onOpenRecipe: (Long) -> Unit,
    onAdd: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("レシピ") },
                actions = {
                    IconButton(onClick = onOpenFolders) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = "フォルダを整理"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "設定")
                    }
                }
            )
        },
        bottomBar = {
            // 片手で届く位置に、並べ替え・お気に入り・表示切替をまとめる
            BottomAppBar(
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.Outlined.SwapVert, contentDescription = "並べ替え")
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.label,
                                            color = if (order == state.sortOrder) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = viewModel::toggleFavoriteOnly) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "お気に入りだけ表示",
                            tint = if (state.favoriteOnly) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = viewModel::toggleViewMode) {
                        Icon(
                            imageVector = if (state.isGrid) Icons.Outlined.ViewAgenda
                            else Icons.Outlined.GridView,
                            contentDescription = "リスト / グリッド切替"
                        )
                    }
                    if (state.isFiltering) {
                        IconButton(onClick = viewModel::clearFilters) {
                            Icon(Icons.Filled.Close, contentDescription = "絞り込みを解除")
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = onAdd) {
                        Icon(Icons.Filled.Add, contentDescription = "レシピを追加")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("題名・本文・メモ・URL を検索") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "検索語を消す")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            FolderChipRow(
                folders = state.folders,
                counts = state.counts,
                selectedId = state.selectedFolderId,
                onSelect = viewModel::selectFolder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // 「時短で作れるレシピ」に 1 タップで辿り着くための行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    TimeFilter.WITHIN_10,
                    TimeFilter.WITHIN_15,
                    TimeFilter.WITHIN_30,
                    TimeFilter.UNSET
                ).forEach { filter ->
                    FilterChip(
                        selected = state.timeFilter == filter,
                        onClick = { viewModel.setTimeFilter(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            when {
                !state.loaded -> Box(modifier = Modifier.fillMaxSize())

                state.recipes.isEmpty() -> EmptyState(
                    title = if (state.isFiltering) "条件に合うレシピがありません"
                    else "まだレシピがありません",
                    description = if (state.isFiltering) {
                        "絞り込みを外すか、別のフォルダを見てみてください。"
                    } else {
                        "Instagram や X の共有メニューから「レシピに保存」を選ぶか、" +
                            "右下の ＋ から追加できます。"
                    }
                )

                state.isGrid -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
                        RecipeGridCard(
                            recipe = recipe,
                            onClick = { onOpenRecipe(recipe.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(recipe) }
                        )
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
                        RecipeListCard(
                            recipe = recipe,
                            onClick = { onOpenRecipe(recipe.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(recipe) }
                        )
                    }
                }
            }
        }
    }
}
