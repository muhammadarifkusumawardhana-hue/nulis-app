package com.personalnotes.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalnotes.app.data.local.SettingsDataStore
import com.personalnotes.app.data.remote.DriveService
import com.personalnotes.app.data.remote.SyncManager
import com.personalnotes.app.domain.model.AppSettings
import com.personalnotes.app.domain.model.SyncModeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val driveService: DriveService,
    private val syncManager: SyncManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateDarkMode(enabled) }
    }

    fun updateSyncMode(mode: SyncModeType, hour: Int = 2, minute: Int = 0) {
        viewModelScope.launch {
            settingsDataStore.updateSyncMode(mode, hour, minute)
            when (mode) {
                SyncModeType.SCHEDULED -> syncManager.scheduleAutoSync(hour, minute)
                else -> syncManager.cancelScheduledSync()
            }
        }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch { settingsDataStore.updateEditorFontSize(size) }
    }

    fun updateAutoSave(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateAutoSave(enabled) }
    }

    fun updateShowWordCount(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateShowWordCount(enabled) }
    }

    fun disconnectDrive() {
        viewModelScope.launch {
            settingsDataStore.updateDriveAccount(false, "")
            syncManager.cancelScheduledSync()
        }
    }
}
