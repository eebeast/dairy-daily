package com.milksummer.tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milksummer.tracker.data.repository.ReminderPreferences
import com.milksummer.tracker.data.repository.ReminderSettings
import com.milksummer.tracker.domain.model.Shift
import com.milksummer.tracker.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: ReminderSettings = ReminderSettings(),
    val canNotify: Boolean = false,
    val exactAllowed: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val preferences: ReminderPreferences, private val scheduler: ReminderScheduler) : ViewModel() {
    private val permission = MutableStateFlow(scheduler.notificationsAllowed() to scheduler.exactAllowed())
    private val error = MutableStateFlow<String?>(null)
    val state = combine(preferences.settings, permission, error) { settings, allowed, message ->
        SettingsUiState(settings, allowed.first, allowed.second, loading = false, error = message)
    }.catch { emit(SettingsUiState(loading = false, error = "Could not read reminder settings.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private fun change(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                error.value = "Could not save reminder settings. Please retry."
            }
        }
    }

    fun enable(value: Boolean) = change { preferences.enable(value) }
    fun time(shift: Shift, value: LocalTime) = change { preferences.time(shift, value) }
    fun markAsked() = change { preferences.markNotificationAsked() }
    fun clearError() { error.value = null }
    fun refreshPermissions() {
        permission.value = scheduler.notificationsAllowed() to scheduler.exactAllowed()
        change { scheduler.schedule(preferences.settings.first()) }
    }
}
