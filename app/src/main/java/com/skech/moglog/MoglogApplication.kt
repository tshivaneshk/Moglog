package com.skech.moglog

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.skech.moglog.data.sync.EpisodeReminderWorker
import com.skech.moglog.data.sync.RecycleBinPurgeWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MoglogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebaseAppCheck()
        scheduleBackgroundWorkers()
    }

    private fun initFirebaseAppCheck() {
        try {
            FirebaseApp.initializeApp(this)
            val appCheck = FirebaseAppCheck.getInstance()
            val providerFactory: AppCheckProviderFactory = if (BuildConfig.DEBUG) {
                runCatching {
                    val debugClass = Class.forName(
                        "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
                    )
                    debugClass.getMethod("getInstance").invoke(null) as AppCheckProviderFactory
                }.getOrElse {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
            appCheck.installAppCheckProviderFactory(providerFactory)
        } catch (_: Exception) {
        }
    }

    private fun scheduleBackgroundWorkers() {
        try {
            val workManager = WorkManager.getInstance(this)

            // 1. Daily Recycle Bin 30-day auto-purge
            val purgeRequest = PeriodicWorkRequestBuilder<RecycleBinPurgeWorker>(1, TimeUnit.DAYS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                RecycleBinPurgeWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                purgeRequest
            )

            // 2. Daily Episode Air-Date Reminder check (requires network)
            val networkConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val reminderRequest = PeriodicWorkRequestBuilder<EpisodeReminderWorker>(12, TimeUnit.HOURS)
                .setConstraints(networkConstraints)
                .build()
            workManager.enqueueUniquePeriodicWork(
                EpisodeReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
            )
        } catch (_: Exception) {
        }
    }
}
