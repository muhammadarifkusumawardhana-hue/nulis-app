package com.nu.lis.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nu.lis.data.AppContainer
import com.nu.lis.util.ZipUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val type = inputData.getString("type") ?: return Result.failure()
        val context = applicationContext
        
        return try {
            val dbFile = context.getDatabasePath("notes.db")
            if (!dbFile.exists()) return Result.failure()

            // Close database to ensure consistency
            AppContainer.closeDatabase()

            if (type == "CLOUD") {
                val driveService = GoogleDriveService(context)
                val tempZip = File(context.cacheDir, "auto_backup.zip")
                ZipUtils.zipAppInternalData(context, tempZip)
                
                val success = driveService.backup(tempZip)
                tempZip.delete()
                
                if (success) {
                    updateLastBackupTime(context)
                    Result.success()
                } else {
                    Result.retry()
                }
            } else {
                // Local Backup
                val backupDir = File(context.getExternalFilesDir(null), "backups")
                if (!backupDir.exists()) backupDir.mkdirs()
                
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "notes_backup_$timeStamp.zip")
                
                ZipUtils.zipAppInternalData(context, backupFile)
                
                // Keep only last 5 local backups
                val files = backupDir.listFiles()?.sortedByDescending { it.lastModified() }
                if (files != null && files.size > 5) {
                    files.drop(5).forEach { it.delete() }
                }

                Result.success()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun updateLastBackupTime(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val now = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        prefs.edit().putString("lastBackup", now).apply()
    }
}
