package com.recipebookmark.ui.share

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipebookmark.ui.components.CookingTimePicker
import com.recipebookmark.ui.components.FolderPicker
import com.recipebookmark.ui.components.SourceIcon

/**
 * 共有から出る軽いシート。
 * 「題名・フォルダ・料理時間」だけに絞って、迷わず 1〜2 タップで終われるようにしている。
 */
@Composable
fun ShareSheet(
    sharedText: String?,
    imageUris: List<Uri>,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: ShareViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.prepare(sharedText, imageUris)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Activity 側のテーマで背景を暗くしているので、シート自身の scrim は無しにする
        scrimColor = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceIcon(source = state.sourceType)
                Text(
                    text = "${state.sourceType.label} から保存",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
                if (state.isLoadingLink) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(14.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            state.url?.let { url ->
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (state.imageCount > 0) {
                Text(
                    text = "写真 ${state.imageCount} 枚を取り込みます",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text("題名") },
                placeholder = { Text("未入力でも保存できます") },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )

            Text(
                text = "フォルダ",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
            FolderPicker(
                folders = state.folders,
                selectedId = state.folderId,
                onSelect = viewModel::setFolder
            )

            Text(
                text = "料理時間",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
            CookingTimePicker(
                selected = state.cookingTimeMin,
                onSelect = viewModel::setCookingTime
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("キャンセル")
                }
                Button(
                    onClick = {
                        viewModel.save()
                        onSaved()
                    },
                    modifier = Modifier.weight(2f)
                ) {
                    Text("保存")
                }
            }
        }
    }
}
