package com.recipebookmark.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.recipebookmark.ui.edit.AddMode

/**
 * 「＋」を押したときの入り口。
 * 共有メニューが使いづらい媒体（料理本の写真、Gemini のテキストなど）の保険として、
 * アプリ内からでも同じことができるようにしている。
 */
@Composable
fun AddSheet(
    onSelect: (AddMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                text = "レシピを追加",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
            )
            AddRow(Icons.Outlined.Link, "URL を貼り付ける", AddMode.URL, onSelect)
            AddRow(Icons.Outlined.ContentPaste, "クリップボードから追加", AddMode.CLIPBOARD, onSelect)
            AddRow(Icons.Outlined.PhotoCamera, "写真を撮る", AddMode.CAMERA, onSelect)
            AddRow(Icons.Outlined.PhotoLibrary, "写真を選ぶ", AddMode.PHOTO, onSelect)
            AddRow(Icons.Outlined.TextFields, "テキストで書く", AddMode.TEXT, onSelect)
        }
    }
}

@Composable
private fun AddRow(
    icon: ImageVector,
    label: String,
    mode: AddMode,
    onSelect: (AddMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
