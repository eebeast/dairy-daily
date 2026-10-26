package com.milksummer.tracker.ui.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class OcrStatus { IDLE, PROCESSING, DONE, ERROR }

data class OcrUiState(
    val status: OcrStatus = OcrStatus.IDLE,
    val permissionGranted: Boolean = false,
    val bitmap: Bitmap? = null,
    val fields: ParsedSlip = ParsedSlip(),
    val error: String? = null,
    val scanId: String = ""
)

@HiltViewModel
class OcrViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow(OcrUiState())
    val state = mutableState.asStateFlow()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun permission(granted: Boolean) { mutableState.update { it.copy(permissionGranted = granted) } }

    fun createCapture(): Uri {
        clearCapture()
        val directory = File(context.cacheDir, "slips").apply { mkdirs() }
        val file = File.createTempFile("slip-", ".jpg", directory)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        savedState["photoPath"] = file.absolutePath
        savedState["photoUri"] = uri.toString()
        return uri
    }

    fun error(message: String) { mutableState.update { it.copy(status = OcrStatus.ERROR, error = message) } }
    fun retake() { mutableState.update { it.copy(status = OcrStatus.IDLE, fields = ParsedSlip(), bitmap = null, error = null) } }

    fun processCapture() {
        val value = savedState.get<String>("photoUri") ?: return error("The photo was lost. Please retake the slip.")
        mutableState.update { it.copy(status = OcrStatus.PROCESSING, error = null) }
        viewModelScope.launch {
            try {
                val uri = Uri.parse(value)
                val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
                val bitmap = withContext(Dispatchers.IO) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1600) inSampleSize *= 2
                    }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                }
                val text = suspendCancellableCoroutine<Text> { continuation ->
                    recognizer.process(image)
                        .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                        .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                }
                val fields = SlipParser.parse(text)
                if (fields.isEmpty) {
                    mutableState.update { it.copy(bitmap = bitmap) }
                    error("No slip data could be extracted. Try a clearer photo or enter the values manually.")
                } else mutableState.update { it.copy(status = OcrStatus.DONE, bitmap = bitmap, fields = fields, scanId = value) }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                error("Could not read the photo. Retake the slip or use manual entry.")
            }
        }
    }

    private fun clearCapture() {
        savedState.get<String>("photoPath")?.let { File(it).delete() }
        savedState.remove<String>("photoPath")
        savedState.remove<String>("photoUri")
    }

    override fun onCleared() {
        recognizer.close()
        clearCapture()
    }
}
