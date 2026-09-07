package com.personalnotes.app.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return when (val result = syncManager.syncAllPendingNotes()) {
            is SyncResult.Success -> Result.success()
            is SyncResult.NotConnected -> Result.retry()
            is SyncResult.Error -> Result.failure()
        }
    }
}
