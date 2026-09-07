package com.nu.lis.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jakewharton.processphoenix.ProcessPhoenix
import com.nu.lis.data.AppContainer
import com.nu.lis.util.BackupWorker
import com.nu.lis.util.GoogleDriveService
import com.nu.lis.util.ZipUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
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
                val tempZip = File(context.cacheDir, "nulis_backup.zip")
                AppContainer.closeDatabase()
                
                ZipUtils.zipAppInternalData(context, tempZip)
                
                val success = driveService.backup(tempZip)
                if (success) {
                    val now = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                    prefs.edit().putString("lastBackup", now).apply()
                    _lastBackup.value = now
                    _backupStatus.value = "Backup Berhasil"
                } else {
                    _backupStatus.value = "Backup Gagal: Hubungkan Google Drive"
                }
                tempZip.delete()
            } catch (e: Exception) {
                _backupStatus.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun performRestore() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tempFile = File(context.cacheDir, "restore_temp")
                val success = driveService.restore(tempFile)
                
                if (success) {
                    AppContainer.closeDatabase()
                    
                    // Smart Restore: Check if it's a zip or legacy db
                    val isZip = isZipFile(tempFile)
                    
                    if (isZip) {
                        ZipUtils.unzipAppInternalData(context, tempFile)
                    } else {
                        // Legacy .db restore
                        val dbFile = context.getDatabasePath("notes.db")
                        tempFile.copyTo(dbFile, overwrite = true)
                    }
                    
                    _backupStatus.value = "Restore Berhasil. Restarting..."
                    ProcessPhoenix.triggerRebirth(context)
                } else {
                    _backupStatus.value = "Restore Gagal: File tidak ditemukan"
                }
                tempFile.delete()
            } catch (e: Exception) {
                _backupStatus.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun isZipFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        val bytes = ByteArray(4)
        try {
            BufferedInputStream(FileInputStream(file)).use { it.read(bytes) }
        } catch (e: Exception) {
            return false
        }
        return bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && 
               bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
    }

    fun clearStatus() {
        _backupStatus.value = null
    }

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tempZip = File(context.cacheDir, "export.zip")
                AppContainer.closeDatabase()
                ZipUtils.zipAppInternalData(context, tempZip)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    tempZip.inputStream().use { it.copyTo(outputStream) }
                }
                tempZip.delete()
                _backupStatus.value = "Berhasil mengekspor backup (.zip)"
            } catch (e: Exception) {
                _backupStatus.value = "Ekspor Gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tempFile = File(context.cacheDir, "import_temp")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { input.copyTo(it) }
                }

                AppContainer.closeDatabase()
                if (isZipFile(tempFile)) {
                    ZipUtils.unzipAppInternalData(context, tempFile)
                } else {
                    val dbFile = context.getDatabasePath("notes.db")
                    tempFile.copyTo(dbFile, overwrite = true)
                }
                
                _backupStatus.value = "Impor Berhasil. Restarting..."
                ProcessPhoenix.triggerRebirth(context)
            } catch (e: Exception) {
                _backupStatus.value = "Impor Gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleAutoBackup(type: String, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val tag = "auto_backup_$type"

        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (type == "CLOUD") NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(true)
                .build()

            val inputData = Data.Builder()
                .putString("type", type)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, java.util.concurrent.TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(tag)
                .build()

            workManager.enqueueUniquePeriodicWork(
                tag,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } else {
            workManager.cancelUniqueWork(tag)
        }
    }
}
