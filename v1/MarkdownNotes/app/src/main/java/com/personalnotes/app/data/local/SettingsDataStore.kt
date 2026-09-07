package com.personalnotes.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.personalnotes.app.domain.model.AppSettings
import com.personalnotes.app.domain.model.SyncMode
import com.personalnotes.app.domain.model.SyncModeType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SYNC_MODE = stringPreferencesKey("sync_mode")
        val SYNC_HOUR = intPreferencesKey("sync_hour")
        val SYNC_MINUTE = intPreferencesKey("sync_minute")
        val DRIVE_CONNECTED = booleanPreferencesKey("drive_connected")
        val DRIVE_EMAIL = stringPreferencesKey("drive_email")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val SHOW_WORD_COUNT = booleanPreferencesKey("show_word_count")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            syncMode = SyncMode(
                mode = SyncModeType.valueOf(prefs[Keys.SYNC_MODE] ?: SyncModeType.MANUAL.name),
                scheduledHour = prefs[Keys.SYNC_HOUR] ?: 2,
                scheduledMinute = prefs[Keys.SYNC_MINUTE] ?: 0
            ),
            isDriveConnected = prefs[Keys.DRIVE_CONNECTED] ?: false,
            driveAccountEmail = prefs[Keys.DRIVE_EMAIL] ?: "",
            editorFontSize = prefs[Keys.EDITOR_FONT_SIZE] ?: 14,
            autoSave = prefs[Keys.AUTO_SAVE] ?: true,
            showWordCount = prefs[Keys.SHOW_WORD_COUNT] ?: true
        )
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun updateSyncMode(mode: SyncModeType, hour: Int = 2, minute: Int = 0) {
        context.dataStore.edit {
            it[Keys.SYNC_MODE] = mode.name
            it[Keys.SYNC_HOUR] = hour
            it[Keys.SYNC_MINUTE] = minute
        }
    }

    suspend fun updateDriveAccount(connected: Boolean, email: String = "") {
        context.dataStore.edit {
            it[Keys.DRIVE_CONNECTED] = connected
            it[Keys.DRIVE_EMAIL] = email
        }
    }

    suspend fun updateEditorFontSize(size: Int) {
        context.dataStore.edit { it[Keys.EDITOR_FONT_SIZE] = size }
    }

    suspend fun updateAutoSave(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun updateShowWordCount(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_WORD_COUNT] = enabled }
    }
}
