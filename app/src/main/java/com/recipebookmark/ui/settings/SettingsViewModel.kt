package com.recipebookmark.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recipebookmark.app
import com.recipebookmark.backup.ImportMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isBusy: Boolean = false,
    val busyMessage: String = "",
    val message: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val backup = application.app.backupManager

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun suggestedFileName(): String = backup.suggestedFileName()

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun export(target: Uri) = viewModelScope.launch {
        _state.update { it.copy(isBusy = true, busyMessage = "書き出しています…") }
        val result = backup.export(target)
        _state.update {
            it.copy(
                isBusy = false,
                busyMessage = "",
                message = result.fold(
                    onSuccess = { s -> "書き出しました（レシピ ${s.recipes} 件・写真 ${s.images} 枚）" },
                    onFailure = { e -> "書き出しに失敗しました: ${e.message}" }
                )
            )
        }
    }

    fun import(source: Uri, mode: ImportMode) = viewModelScope.launch {
        _state.update { it.copy(isBusy = true, busyMessage = "読み込んでいます…") }
        val result = backup.import(source, mode)
        _state.update {
            it.copy(
                isBusy = false,
                busyMessage = "",
                message = result.fold(
                    onSuccess = { s -> "読み込みました（レシピ ${s.recipes} 件・写真 ${s.images} 枚）" },
                    onFailure = { e -> "読み込みに失敗しました: ${e.message}" }
                )
            )
        }
    }
}
