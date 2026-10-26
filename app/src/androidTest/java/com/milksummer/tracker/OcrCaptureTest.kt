package com.milksummer.tracker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.milksummer.tracker.ui.ocr.OcrStatus
import com.milksummer.tracker.ui.ocr.OcrViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class OcrCaptureTest {
    @Test fun capturedImageIsDecodedAndRecognizedBeforeConfirmation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val store = ViewModelStore()
        lateinit var model: OcrViewModel
        instrumentation.runOnMainSync {
            model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OcrViewModel(context, SavedStateHandle()) as T
            })[OcrViewModel::class.java]
        }
        try {
            val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 80f }
            listOf("Qty: 4.8", "FAT: 6.5", "SNF: 8.8", "Rate: 38.00", "Total: 182.40").forEachIndexed { index, line ->
                canvas.drawText(line, 100f, 300f + index * 180f, paint)
            }
            lateinit var uri: android.net.Uri
            instrumentation.runOnMainSync { uri = model.createCapture() }
            context.contentResolver.openOutputStream(uri)!!.use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it))
            }
            bitmap.recycle()
            instrumentation.runOnMainSync { model.processCapture() }
            runBlocking {
                withTimeout(20000) {
                    while (model.state.value.status == OcrStatus.PROCESSING) delay(50)
                }
            }
            val state = model.state.value
            assertEquals(state.error, OcrStatus.DONE, state.status)
            assertNotNull(state.bitmap)
            assertTrue(maxOf(state.bitmap!!.width, state.bitmap.height) <= 1600)
            assertEquals(4.8, state.fields.quantity!!, 0.000001)
            assertEquals(6.5, state.fields.fat!!, 0.000001)
            assertEquals(8.8, state.fields.snf!!, 0.000001)
            assertEquals(38.0, state.fields.rate!!, 0.000001)
            assertEquals(182.4, state.fields.total!!, 0.000001)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
        }
    }
}
