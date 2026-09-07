package com.personalnotes.app.presentation

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
class MainViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val driveService: DriveService,
    private val syncManager: SyncManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun onDriveConnected(email: String, accountName: String) {
        viewModelScope.launch {
            driveService.initialize(accountName)
            settingsDataStore.updateDriveAccount(true, email)
        }
    }

    fun onDriveConnectionFailed() {
        viewModelScope.launch {
            settingsDataStore.updateDriveAccount(false, "")
        }
    }

    fun onAppOpen() {
        viewModelScope.launch {
            val s = settings.value
            if (s.isDriveConnected && s.syncMode.mode == SyncModeType.ON_OPEN_CLOSE) {
                syncManager.syncAllPendingNotes()
            }
        }
    }

    fun onAppClose() {
        viewModelScope.launch {
            val s = settings.value
            if (s.isDriveConnected && s.syncMode.mode == SyncModeType.ON_OPEN_CLOSE) {
                syncManager.syncAllPendingNotes()
            }
        }
    }
}
