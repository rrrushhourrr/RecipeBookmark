package com.recipebookmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.recipebookmark.data.SourceType
import com.recipebookmark.util.ImageStore
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

fun iconFor(source: SourceType): ImageVector = when (source) {
    SourceType.INSTAGRAM -> Icons.Outlined.PhotoCamera
    SourceType.X -> Icons.Outlined.AlternateEmail
    SourceType.KURASHIRU -> Icons.Outlined.PlayCircleOutline
    SourceType.BOOK_PHOTO -> Icons.Outlined.MenuBook
    SourceType.GEMINI -> Icons.Outlined.AutoAwesome
    SourceType.OTHER -> Icons.Outlined.Link
}

@Composable
fun SourceIcon(source: SourceType, modifier: Modifier = Modifier) {
    Icon(
        imageVector = iconFor(source),
        contentDescription = source.label,
        modifier = modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * 内部ストレージのファイル名から画像を出す。
 * 画像が無いレシピ（URL だけ、本文だけ）は媒体アイコンのプレースホルダにする。
 */
@Composable
fun RecipeThumbnail(
    imageName: String?,
    source: SourceType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = imageName?.let { ImageStore.fileFor(context, it) }

    if (file != null && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconFor(source),
                contentDescription = source.label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}
