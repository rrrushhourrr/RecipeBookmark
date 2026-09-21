package com.recipebookmark.ui.edit

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.recipebookmark.ui.components.CookingTimePicker
import com.recipebookmark.ui.components.FolderPicker
import com.recipebookmark.util.ImageStore

@Composable
fun EditScreen(
    onDone: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: EditViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris -> viewModel.addImages(uris) }

    var cameraTarget by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraTarget
        if (success && uri != null) viewModel.addImages(listOf(uri))
        cameraTarget = null
    }

    fun launchCamera() {
        val uri = createCameraUri(context)
        cameraTarget = uri
        camera.launch(uri)
    }

    // 「＋」でどの入り口から来たかによって、開いた瞬間の挙動を変える
    LaunchedEffect(Unit) {
        when (viewModel.addMode) {
            AddMode.PHOTO -> photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            AddMode.CAMERA -> launchCamera()
            AddMode.CLIPBOARD -> readClipboard(context)?.let { viewModel.applySharedText(it) }
            else -> Unit
        }
    }

    LaunchedEffect(state.savedId) {
        state.savedId?.let(onDone)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "レシピを追加" else "レシピを編集") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = "お気に入り"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // 保存は片手で押せるように一番下
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = viewModel::save,
                    enabled = state.canSave && !state.isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("保存")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (state.isBusy) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = state.busyMessage,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text("題名") },
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            Text(
                text = "フォルダ",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            FolderPicker(
                folders = state.folders,
                selectedId = state.folderId,
                onSelect = viewModel::setFolder
            )

            Text(
                text = "料理時間",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            CookingTimePicker(
                selected = state.cookingTimeMin,
                onSelect = viewModel::setCookingTime
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = viewModel::fetchOgp,
                    enabled = state.url.isNotBlank() && !state.isBusy
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "リンク先から題名と画像を取得")
                }
            }

            // --- 写真 ---------------------------------------------------------
            Text(
                text = "写真",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                    Text("写真を選ぶ", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = { launchCamera() }) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Text("撮る", modifier = Modifier.padding(start = 6.dp))
                }
            }

            if (state.imagePaths.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    items(state.imagePaths.size) { index ->
                        val name = state.imagePaths[index]
                        Box {
                            AsyncImage(
                                model = ImageStore.fileFor(context, name),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            IconButton(
                                onClick = { viewModel.removeImage(name) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "この写真を外す")
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.bodyText,
                onValueChange = viewModel::setBody,
                label = { Text("本文（作り方・材料など）") },
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            OutlinedTextField(
                value = state.memo,
                onValueChange = viewModel::setMemo,
                label = { Text("メモ") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp)
            )
        }
    }
}

/** カメラアプリに渡す保存先。FileProvider 経由でないと共有できない。 */
private fun createCameraUri(context: Context): Uri {
    val dir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
    val file = java.io.File(dir, "shot-${System.currentTimeMillis()}.jpg")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun readClipboard(context: Context): String? {
    val manager = context.getSystemService(android.content.ClipboardManager::class.java)
        ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
}
