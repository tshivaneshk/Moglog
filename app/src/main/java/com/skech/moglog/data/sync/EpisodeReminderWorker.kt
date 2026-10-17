package com.skech.moglog.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skech.moglog.MainActivity
import com.skech.moglog.R
import com.skech.moglog.data.local.SettingsStore
import com.skech.moglog.domain.repository.DramaRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Periodic worker that checks "Watching" shows for upcoming/today episode air dates
 * and posts a polite notification if enabled in Settings.
 */
class EpisodeReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderWorkerEntryPoint {
        fun dramaRepository(): DramaRepository
        fun settingsStore(): SettingsStore
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ReminderWorkerEntryPoint::class.java
            )
            val settings = entryPoint.settingsStore().settingsFlow.first()
            if (!settings.episodeRemindersEnabled) {
                return Result.success()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return Result.success()
            }

            ensureNotificationChannel()

            val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val watching = entryPoint.dramaRepository().getAllDramas().first()
                .filter { !it.isDeleted && it.status == "Watching" }

            for (drama in watching) {
                val details = entryPoint.dramaRepository().getDramaExtraDetails(drama.id)
                val airDate = details?.nextEpisodeAirDate ?: drama.nextEpisodeAirDate
                if (airDate == todayIso) {
                    val epNum = details?.nextEpisodeNumber?.takeIf { it > 0 } ?: drama.nextEpisodeNumber
                    val sNum = details?.nextSeasonNumber?.takeIf { it > 0 } ?: drama.nextSeasonNumber.coerceAtLeast(1)
                    postEpisodeNotification(
                        dramaId = drama.id,
                        title = drama.title,
                        season = sNum,
                        episode = epNum
                    )
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Episode Air-Date Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when a show in your Watching list airs a new episode"
            }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun postEpisodeNotification(
        dramaId: Int,
        title: String,
        season: Int,
        episode: Int
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            dramaId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val epLabel = if (episode > 0) "S$season · E$episode airs today" else "A new episode airs today"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(epLabel)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(dramaId, notification)
        }
    }

    companion object {
        const val WORK_NAME = "moglog_episode_reminder_worker"
        private const val CHANNEL_ID = "moglog_episode_reminders"
    }
}
