package com.jiucaihua.app.presentation.settings

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.R
import com.jiucaihua.app.data.backup.BackupData
import com.jiucaihua.app.data.backup.BackupRepository
import com.jiucaihua.app.data.backup.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val isSuccess: Boolean = false,
    val lastRestoreResult: RestoreResult? = null,
    val pendingBackup: BackupData? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportData(
        createDocumentLauncher: ActivityResultLauncher<String>,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null, isSuccess = false)
            try {
                val backup = backupRepository.exportData()
                val fileName = backupRepository.getDefaultFileName()
                createDocumentLauncher.launch(fileName)
                _uiState.value = _uiState.value.copy(pendingBackup = backup)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = context.getString(R.string.backup_export_failed, e.message),
                    isSuccess = false,
                )
            }
        }
    }

    fun completeExport(uri: android.net.Uri) {
        viewModelScope.launch {
            val backup = _uiState.value.pendingBackup
            if (backup == null) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = context.getString(R.string.backup_export_no_data),
                    isSuccess = false,
                )
                return@launch
            }
            try {
                val content = backupRepository.serializeBackup(backup)
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        message = context.getString(R.string.backup_export_open_failed),
                        isSuccess = false,
                    )
                    return@launch
                }
                backupRepository.writeToUri(outputStream, content)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    pendingBackup = null,
                    message = context.getString(
                        R.string.backup_export_success,
                        backup.holdings.size,
                        backup.alerts.size,
                    ),
                    isSuccess = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    pendingBackup = null,
                    message = context.getString(R.string.backup_export_failed, e.message),
                    isSuccess = false,
                )
            }
        }
    }

    fun cancelExport() {
        _uiState.value = _uiState.value.copy(
            isExporting = false,
            pendingBackup = null,
            message = null,
            isSuccess = false,
        )
    }

    fun startImport(
        openDocumentLauncher: ActivityResultLauncher<Array<String>>,
    ) {
        _uiState.value = _uiState.value.copy(isImporting = true, message = null, isSuccess = false)
        openDocumentLauncher.launch(arrayOf("application/json"))
    }

    fun completeImport(uri: android.net.Uri, mode: BackupRepository.RestoreMode) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        message = context.getString(R.string.backup_import_open_failed),
                        isSuccess = false,
                    )
                    return@launch
                }
                val content = inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                val backup = backupRepository.deserializeBackup(content)
                val result = backupRepository.restoreData(backup, mode)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    lastRestoreResult = result,
                    message = context.getString(
                        R.string.backup_import_success,
                        result.holdingsCount,
                        result.alertsCount,
                    ),
                    isSuccess = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    message = context.getString(R.string.backup_import_failed, e.message),
                    isSuccess = false,
                )
            }
        }
    }

    fun cancelImport() {
        _uiState.value = _uiState.value.copy(isImporting = false, message = null, isSuccess = false)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, isSuccess = false)
    }
}
