package com.recipebookmark.ui.folder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipebookmark.data.Folder

@Composable
fun FolderScreen(
    onBack: () -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Folder?>(null) }
    var deleting by remember { mutableStateOf<Folder?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("フォルダを整理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("フォルダを追加") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(state.folders, key = { it.id }) { folder ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = folder.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${state.counts[folder.id] ?: 0} 件" +
                                    if (folder.isFixed) "・固定フォルダ" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!folder.isFixed) {
                            IconButton(onClick = { viewModel.move(folder, up = true) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上へ")
                            }
                            IconButton(onClick = { viewModel.move(folder, up = false) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下へ")
                            }
                            IconButton(onClick = { editing = folder }) {
                                Icon(Icons.Filled.Edit, contentDescription = "名前を変更")
                            }
                            IconButton(onClick = { deleting = folder }) {
                                Icon(Icons.Filled.Delete, contentDescription = "削除")
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        NameDialog(
            title = "フォルダを追加",
            initial = "",
            onConfirm = {
                viewModel.add(it)
                creating = false
            },
            onDismiss = { creating = false }
        )
    }

    editing?.let { folder ->
        NameDialog(
            title = "名前を変更",
            initial = folder.name,
            onConfirm = {
                viewModel.rename(folder, it)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }

    deleting?.let { folder ->
        val count = state.counts[folder.id] ?: 0
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("「${folder.name}」を削除しますか？") },
            text = {
                Text(
                    if (count > 0) "中の $count 件のレシピは「未分類」へ移動します。"
                    else "このフォルダは空です。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(folder)
                    deleting = null
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(30) },
                label = { Text("フォルダ名") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text("決定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}
