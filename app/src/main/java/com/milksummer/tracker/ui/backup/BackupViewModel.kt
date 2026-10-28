package com.milksummer.tracker.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milksummer.tracker.data.backup.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val busy: Boolean = false,
    val preview: BackupData? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class BackupViewModel internal constructor(
    private val service: BackupService,
    private val openInput: (Uri) -> InputStream?,
    private val openOutput: (Uri) -> OutputStream?
) : ViewModel() {
    @Inject constructor(@ApplicationContext context: Context, service: BackupService) : this(service,
        { context.contentResolver.openInputStream(it) }, { context.contentResolver.openOutputStream(it, "wt") })

    private val mutableState = MutableStateFlow(BackupUiState())
    val state = mutableState.asStateFlow()

    private fun work(failure: String, action: suspend () -> Unit) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try { action() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.update { it.copy(error = (error as? BackupException)?.message ?: failure) }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun export(uri: Uri?) {
        if (uri == null) return
        work("Backup could not be saved. Delete any incomplete file and try another destination.") {
            val count = withContext(Dispatchers.IO) {
                val stream = openOutput(uri) ?: throw BackupException("Could not open this destination. Choose another location.")
                stream.use { service.export(it) }
            }
            mutableState.update { it.copy(message = "Backup saved: $count entries.") }
        }
    }

    fun preview(uri: Uri?) {
        if (uri == null) return
        if (mutableState.value.busy) return
        mutableState.update { it.copy(preview = null) }
        work("Could not read this backup. Check the file and try again.") {
            val data = withContext(Dispatchers.IO) {
                val stream = openInput(uri) ?: throw BackupException("Could not open this backup. Select another file.")
                stream.use { service.preview(it) }
            }
            mutableState.update { it.copy(preview = data) }
        }
    }

    fun confirmRestore() {
        val data = mutableState.value.preview ?: return
        work("Restore failed. No entries were imported. You can retry this backup.") {
            val result = service.restore(data)
            val settingsMessage = if (result.settingsRestored) "Reminder settings restored."
                else "Reminder settings could not be restored. Retry this backup to apply them."
            mutableState.update { it.copy(preview = null,
                message = "Restored ${result.imported} entries; skipped ${result.skipped} existing entries. $settingsMessage") }
        }
    }

    fun cancelPreview() { if (!mutableState.value.busy) mutableState.update { it.copy(preview = null) } }
    fun dismissMessage() { mutableState.update { it.copy(error = null, message = null) } }
}
