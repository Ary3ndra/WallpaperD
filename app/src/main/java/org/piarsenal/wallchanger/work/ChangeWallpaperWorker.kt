package org.piarsenal.wallchanger.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.piarsenal.wallchanger.wallpaper.Prefetcher
import org.piarsenal.wallchanger.wallpaper.WallpaperEngine

/**
 * Periodic background change. Delegates to the shared engine, then — while still in the granted
 * wake window with network available — downloads the next wallpaper so the following change is
 * instant. No wakelock, no foreground service.
 */
class ChangeWallpaperWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val applied = WallpaperEngine(applicationContext).changeOnce()
        if (applied) {
            runCatching { Prefetcher.prefetchNext(applicationContext) }
            return Result.success()
        }
        return Result.retry()
    }
}
