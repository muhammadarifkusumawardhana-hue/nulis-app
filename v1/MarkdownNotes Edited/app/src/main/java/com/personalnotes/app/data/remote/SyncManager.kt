package com.personalnotes.app.data.remote

import android.content.Context
import androidx.work.*
import com.personalnotes.app.domain.model.SyncModeType
import com.personalnotes.app.domain.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val driveService: DriveService,
    private val noteRepository: NoteRepository
) {
    suspend fun syncAllPendingNotes(): SyncResult = withContext(Dispatchers.IO) {
        if (!driveService.isConnected()) return@withContext SyncResult.NotConnected

        val unsynced = noteRepository.getUnsyncedNotes()
        var successCount = 0
        var failCount = 0

        unsynced.forEach { note ->
            val driveFileId = driveService.syncNote(note)
            if (driveFileId != null) {
                noteRepository.markAsSynced(note.id, driveFileId)
                successCount++
            } else {
                failCount++
            }
        }
        SyncResult.Success(successCount, failCount)
    }

    fun scheduleAutoSync(hourOfDay: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }

        val delay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "scheduled_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelScheduledSync() {
        WorkManager.getInstance(context).cancelUniqueWork("scheduled_sync")
    }

    fun scheduleOnChangeSync() {
        // Debounced via ViewModel - just trigger immediate one-time work
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(30, TimeUnit.SECONDS) // 30s debounce
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "onChange_sync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

sealed class SyncResult {
    data class Success(val synced: Int, val failed: Int) : SyncResult()
    object NotConnected : SyncResult()
    data class Error(val message: String) : SyncResult()
}

enum class SyncTrigger {
    ON_CHANGE, ON_SAVE, ON_OPEN, ON_CLOSE, SCHEDULED, MANUAL
}
