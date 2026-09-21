package com.recipebookmark.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipebookmark.ui.components.EmptyState
import com.recipebookmark.ui.components.RecipeThumbnail
import com.recipebookmark.ui.components.SourceIcon
import com.recipebookmark.util.TimeFormat

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val recipe = state.recipe

    var confirmDelete by remember { mutableStateOf(false) }
    var folderMenuOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    if (state.loaded && recipe == null) {
        // 直前に削除された場合など
        EmptyState(title = "レシピが見つかりません", description = "削除された可能性があります。")
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = recipe?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (recipe?.isFavorite == true) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = "お気に入り"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // 片手で届く位置に主要操作をまとめる
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { folderMenuOpen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            Text(
                                text = state.folderName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = folderMenuOpen,
                            onDismissRequest = { folderMenuOpen = false }
                        ) {
                            state.folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        viewModel.moveToFolder(folder.id)
                                        folderMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { recipe?.let { onEdit(it.id) } }) {
                        Icon(Icons.Filled.Edit, contentDescription = "編集")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "削除")
                    }
                }
            }
        }
    ) { padding ->
        if (recipe == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- 写真（横スワイプ、タップで拡大） -----------------------------
            if (recipe.imagePaths.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { recipe.imagePaths.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) { page ->
                    val name = recipe.imagePaths[page]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { viewerIndex = page }
                    ) {
                        RecipeThumbnail(
                            imageName = name,
                            source = recipe.sourceType,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (recipe.imagePaths.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${recipe.imagePaths.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 6.dp)
                    )
                }
            } else if (recipe.thumbnailPath != null) {
                RecipeThumbnail(
                    imageName = recipe.thumbnailPath,
                    source = recipe.sourceType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = recipe.title, style = MaterialTheme.typography.headlineSmall)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(TimeFormat.cookingTime(recipe.cookingTimeMin)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(recipe.sourceType.label) },
                        leadingIcon = { SourceIcon(source = recipe.sourceType) }
                    )
                }

                Text(
                    text = "追加日 ${TimeFormat.date(recipe.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (!recipe.url.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { openUrl(context, recipe.url) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                        Text("元の投稿を開く", modifier = Modifier.padding(start = 8.dp))
                    }
                }

                if (!recipe.bodyText.isNullOrBlank()) {
                    SectionCard(title = "本文", body = recipe.bodyText)
                }
                if (recipe.memo.isNotBlank()) {
                    SectionCard(title = "メモ", body = recipe.memo)
                }
            }
        }
    }

    // --- 写真ビューア -------------------------------------------------------
    viewerIndex?.let { startIndex ->
        val images = recipe?.imagePaths.orEmpty()
        if (images.isNotEmpty()) {
            ImageViewerDialog(
                imageNames = images,
                startIndex = startIndex.coerceIn(0, images.lastIndex),
                onDismiss = { viewerIndex = null }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("このレシピを削除しますか？") },
            text = { Text("削除すると元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, body: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            SelectionContainer {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
