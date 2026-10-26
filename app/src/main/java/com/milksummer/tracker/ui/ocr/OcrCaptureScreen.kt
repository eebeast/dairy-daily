package com.milksummer.tracker.ui.ocr

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OcrCaptureScreen(onBack: () -> Unit, onManual: () -> Unit, onSaved: () -> Unit, viewModel: OcrViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var launched by rememberSaveable { mutableStateOf(false) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        if (captured) viewModel.processCapture() else onBack()
    }
    val capture = {
        try {
            launched = true
            camera.launch(viewModel.createCapture())
        } catch (_: ActivityNotFoundException) {
            viewModel.error("No camera app is available. Use manual entry instead.")
        } catch (_: Exception) {
            viewModel.error("Could not open the camera. Try again or use manual entry.")
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.permission(granted)
        if (granted) capture()
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        viewModel.permission(granted)
        if (granted && !launched && state.status == OcrStatus.IDLE) capture()
    }
    when (state.status) {
        OcrStatus.DONE -> OcrConfirmScreen(
            fields = state.fields, scanId = state.scanId, onBack = onBack, onSaved = onSaved,
            onRetake = { viewModel.retake(); capture() }, onManual = onManual
        )
        else -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Scan a milk slip", style = MaterialTheme.typography.headlineMedium)
            if (state.status == OcrStatus.PROCESSING) {
                CircularProgressIndicator()
                Text("Reading the slip on your device…")
            } else {
                Text(state.error ?: "Camera access is used to photograph the slip and read its values on your device. Review every value before saving.")
                Button(onClick = { if (state.permissionGranted) capture() else permission.launch(Manifest.permission.CAMERA) }) {
                    Text(if (state.permissionGranted) "Take photo" else "Allow camera and scan")
                }
                if (!state.permissionGranted) {
                    Text("If camera access was denied, enable it in app settings or use manual entry.")
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Open app settings") }
                }
                OutlinedButton(onClick = onManual) { Text("Enter manually") }
            }
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}
