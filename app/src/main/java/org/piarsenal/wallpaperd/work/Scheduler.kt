package org.piarsenal.wallpaperd.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.piarsenal.wallpaperd.data.AppSettings
import org.piarsenal.wallpaperd.data.SourceType
import java.time.Duration

/**
 * Translates AppSettings into WorkManager jobs.
 *
 * Battery model:
 *  - A single PeriodicWorkRequest. WorkManager batches it with other system work and respects
 *    Doze, so the device is not held awake. The app sleeps between intervals.
 *  - WorkManager's minimum period is 15 minutes; shorter intervals are clamped to it.
 *  - Network is required only when an enabled source is online; a folder-only setup needs none.
 */
object Scheduler {

    private const val PERIODIC = "wallpaper_periodic"
    private const val ONE_SHOT = "wallpaper_now"
    const val MIN_INTERVAL_MINUTES = 15L

    fun apply(context: Context, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)
        val hasSources = settings.sources.any { it.enabled }

        if (!settings.enabled || !hasSources) {
            wm.cancelUniqueWork(PERIODIC)
            return
        }

        val interval = settings.intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES)
        val needsNetwork = settings.sources.any { it.enabled && it.type != SourceType.FOLDER }

        val constraints = Constraints.Builder().apply {
            if (needsNetwork) {
                setRequiredNetworkType(if (settings.onlyOnWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            }
            if (settings.onlyWhenCharging) setRequiresCharging(true)
        }.build()

        val request = PeriodicWorkRequestBuilder<ChangeWallpaperWorker>(Duration.ofMinutes(interval))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofMinutes(10))
            .build()

        // REPLACE so interval/constraint edits take effect immediately.
        wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }

    /** "Change now" — fires once, outside the periodic schedule. Runs promptly, no wakelock. */
    fun runOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<ChangeWallpaperWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONE_SHOT, ExistingWorkPolicy.REPLACE, request)
    }
}
