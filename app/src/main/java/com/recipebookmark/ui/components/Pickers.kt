package com.recipebookmark.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.recipebookmark.data.Folder
import com.recipebookmark.util.TimeFormat

/**
 * 料理時間の入力。よく使う値はワンタップのチップで、それ以外は数字で直接。
 * 共有シートでもこのまま使う。
 */
@Composable
fun CookingTimePicker(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    // 「任意の数値入力」欄。チップにない値が入っているときは最初から開いておく。
    var customOpen by remember(selected) {
        mutableStateOf(selected != null && selected !in TimeFormat.PRESET_MINUTES)
    }
    var customText by remember(selected) {
        mutableStateOf(
            if (selected != null && selected !in TimeFormat.PRESET_MINUTES) selected.toString()
            else ""
        )
    }

    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimeFormat.PRESET_MINUTES.forEach { minutes ->
                FilterChip(
                    selected = selected == minutes,
                    onClick = {
                        customOpen = false
                        customText = ""
                        // もう一度押したら解除
                        onSelect(if (selected == minutes) null else minutes)
                    },
                    label = { Text("${minutes}分") }
                )
            }
            FilterChip(
                selected = customOpen,
                onClick = {
                    customOpen = !customOpen
                    if (!customOpen) onSelect(null)
                },
                label = { Text("その他") }
            )
        }

        if (customOpen) {
            OutlinedTextField(
                value = customText,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(4)
                    customText = digits
                    onSelect(digits.toIntOrNull()?.takeIf { it > 0 })
                },
                label = { Text("分") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(140.dp)
            )
        }
    }
}

/** フォルダ選択。項目数が少ないのでシンプルなドロップダウンで十分。 */
@Composable
fun FolderPicker(
    folders: List<Folder>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = folders.firstOrNull { it.id == selectedId }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Outlined.Folder, contentDescription = null)
            Text(
                text = selected?.name ?: "未分類",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.name) },
                    onClick = {
                        onSelect(folder.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** ホーム上部のフォルダチップ（すべて / 未分類 / 各フォルダ + 件数）。 */
@Composable
fun FolderChipRow(
    folders: List<Folder>,
    counts: Map<Long?, Int>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    // フォルダが増えても横に流せるようにしておく
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text("すべて ${counts[null] ?: 0}") }
        )
        folders.forEach { folder ->
            val count = counts[folder.id] ?: 0
            FilterChip(
                selected = selectedId == folder.id,
                onClick = { onSelect(folder.id) },
                label = {
                    Text(
                        text = "${folder.name} $count",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}
