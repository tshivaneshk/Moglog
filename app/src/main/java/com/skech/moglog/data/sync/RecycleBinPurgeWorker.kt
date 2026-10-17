package com.skech.moglog.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skech.moglog.data.local.DramaDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that permanently deletes Recycle Bin items older than 30 days.
 */
class RecycleBinPurgeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PurgeWorkerEntryPoint {
        fun dramaDatabase(): DramaDatabase
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                PurgeWorkerEntryPoint::class.java
            )
            val cutoffTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            entryPoint.dramaDatabase().dao.purgeDeletedBefore(cutoffTimestamp)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "moglog_recycle_bin_purge_worker"
    }
}
