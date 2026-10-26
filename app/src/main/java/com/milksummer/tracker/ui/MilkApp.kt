package com.milksummer.tracker.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.milksummer.tracker.domain.model.*
import com.milksummer.tracker.ui.entry.EntryScreen
import com.milksummer.tracker.ui.ocr.OcrCaptureScreen
import com.milksummer.tracker.ui.settings.*
import com.milksummer.tracker.ui.summary.SummaryScreen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkApp(requestedShift: String?, onIntentHandled: () -> Unit) {
    val nav = rememberNavController()
    val ledger: LedgerViewModel = hiltViewModel()
    val settings: SettingsViewModel = hiltViewModel()
    val state by ledger.state.collectAsState()
    val settingsState by settings.state.collectAsState()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var delete by remember { mutableStateOf<MilkEntry?>(null) }
    var explainNotifications by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        settings.refreshPermissions()
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { ledger.refreshToday(); settings.refreshPermissions() }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(settingsState.loading, settingsState.settings.notificationAsked) {
        if (!settingsState.loading && !settingsState.settings.notificationAsked && Build.VERSION.SDK_INT >= 33 && !settingsState.canNotify) explainNotifications = true
    }
    LaunchedEffect(requestedShift) {
        if (requestedShift != null && Shift.entries.any { it.name == requestedShift }) {
            nav.navigate("entry?shift=$requestedShift")
            onIntentHandled()
        }
    }
    val add: (Shift?) -> Unit = { shift -> nav.navigate("entry?shift=${shift?.name.orEmpty()}") }
    val edit: (Long) -> Unit = { id -> nav.navigate("entry?entryId=$id") }
    val saved = { nav.popBackStack("home", inclusive = false); Unit }
    val requestPermission = {
        if (Build.VERSION.SDK_INT >= 33) explainNotifications = true
        else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    Scaffold(bottomBar = {
        if (route in listOf("home", "history", "summary", "settings")) NavigationBar {
            listOf("home" to Icons.Default.Home, "history" to Icons.Default.History, "summary" to Icons.Default.BarChart, "settings" to Icons.Default.Settings).forEach { (destination, icon) ->
                NavigationBarItem(selected = route == destination,
                    onClick = { nav.navigate(destination) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Icon(icon, destination) }, label = { Text(destination.replaceFirstChar { it.uppercase() }) })
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(state, add, { nav.navigate("scan") }, edit, { delete = it }) }
            composable("history") { HistoryScreen(state, edit, { delete = it }) }
            composable("summary") { SummaryScreen({ nav.navigate("day/$it") }) }
            composable("settings") { SettingsScreen(settings, requestPermission) }
            composable("entry?entryId={entryId}&shift={shift}", arguments = listOf(
                navArgument("entryId") { type = NavType.StringType; defaultValue = "0" },
                navArgument("shift") { type = NavType.StringType; defaultValue = "" }
            )) { EntryScreen({ nav.popBackStack() }, saved) }
            composable("scan") { OcrCaptureScreen({ nav.popBackStack() }, { nav.popBackStack(); add(null) }, saved) }
            composable("day/{date}") { entry ->
                val date = LocalDate.parse(entry.arguments?.getString("date"))
                Scaffold(topBar = { TopAppBar(title = { Text("Daily entries") }, navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }) }) { dayPadding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(dayPadding)) { HistoryScreen(state, edit, { delete = it }, date) }
                }
            }
        }
    }
    delete?.let { entry ->
        DeleteEntryDialog(entry, onConfirm = { ledger.delete(entry); delete = null }, onDismiss = { delete = null })
    }
    state.error?.let { message ->
        AlertDialog(onDismissRequest = ledger::dismissError, title = { Text("Entries unavailable") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = ledger::dismissError) { Text("OK") } })
    }
    if (explainNotifications) {
        AlertDialog(onDismissRequest = { settings.markAsked(); explainNotifications = false }, title = { Text("Milk entry reminders") },
            text = { Text("Allow notifications for morning and evening reminders. You can keep using MilkSummer if you decline.") },
            confirmButton = { TextButton(onClick = {
                settings.markAsked(); settings.enable(true); explainNotifications = false
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("Allow") } },
            dismissButton = { TextButton(onClick = { settings.markAsked(); explainNotifications = false }) { Text("Later") } })
    }
}
