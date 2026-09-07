package com.nu.lis.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nu.lis.data.AppContainer
import com.nu.lis.util.GoogleDriveService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val driveService = GoogleDriveService(context)
    private val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _lastBackup = MutableStateFlow(prefs.getString("lastBackup", "-") ?: "-")
    val lastBackup = _lastBackup.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus = _backupStatus.asStateFlow()

    fun performBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dbFile = context.getDatabasePath("notes.db")
                if (dbFile.exists()) {
                    // Close DB before backup to ensure consistency
                    AppContainer.closeDatabase()
                    
                    val success = driveService.backup(dbFile)
                    if (success) {
                        val now = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                        prefs.edit().putString("lastBackup", now).apply()
                        _lastBackup.value = now
                        _backupStatus.value = "Backup Berhasil"
                    } else {
                        _backupStatus.value = "Backup Gagal: Hubungkan Google Drive"
                    }
                }
            } catch (e: Exception) {
                _backupStatus.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun performRestore(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dbFile = context.getDatabasePath("notes.db")
                val tempFile = File(context.cacheDir, "restore_temp.db")
                
                val success = driveService.restore(tempFile)
                if (success) {
                    AppContainer.closeDatabase()
                    
                    if (tempFile.exists()) {
                        tempFile.copyTo(dbFile, overwrite = true)
                        _backupStatus.value = "Restore Berhasil. Silakan restart aplikasi."
                        onSuccess()
                    }
                } else {
                    _backupStatus.value = "Restore Gagal: File backup tidak ditemukan"
                }
            } catch (e: Exception) {
                _backupStatus.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStatus() {
        _backupStatus.value = null
    }

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dbFile = context.getDatabasePath("notes.db")
                if (dbFile.exists()) {
                    AppContainer.closeDatabase()
                    
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    _backupStatus.value = "Berhasil mengekspor backup"
                }
            } catch (e: Exception) {
                _backupStatus.value = "Ekspor Gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importDatabase(uri: Uri, onRestart: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dbFile = context.getDatabasePath("notes.db")
                AppContainer.closeDatabase()
                
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                _backupStatus.value = "Impor Berhasil. Memuat ulang data..."
                onRestart()
            } catch (e: Exception) {
                _backupStatus.value = "Impor Gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
