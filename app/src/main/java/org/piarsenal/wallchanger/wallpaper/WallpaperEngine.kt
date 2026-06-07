package org.piarsenal.wallchanger.wallpaper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.piarsenal.wallchanger.data.HistoryManager
import org.piarsenal.wallchanger.data.SettingsRepository
import org.piarsenal.wallchanger.data.db.AppDatabase
import org.piarsenal.wallchanger.log.Logger
import org.piarsenal.wallchanger.provider.ProvidedImage
import org.piarsenal.wallchanger.provider.ProviderEnv
import org.piarsenal.wallchanger.provider.ProviderFactory
import org.piarsenal.wallchanger.work.Notifications

/**
 * The one wallpaper-change pipeline, shared by the periodic worker and the manual "Change now".
 * Prefers a prefetched image (instant), else fetches fresh. Logs every attempt so failures are
 * explainable, and surfaces the reason in the failure notification.
 */
class WallpaperEngine(private val context: Context) {

    /** @return true if a wallpaper was applied. */
    suspend fun changeOnce(): Boolean = withContext(Dispatchers.IO) {
        val repo = SettingsRepository(context)
        val settings = repo.current()
        val sources = settings.sources.filter { it.enabled }
        if (sources.isEmpty()) {
            Logger.w(context, "Engine", "no enabled sources")
            return@withContext false
        }

        val notify = settings.notifyOnChange
        if (notify) Notifications.changing(context)

        Prefetcher.consume(context)?.let { ready ->
            if (applyAndFinish(ready.image, ready.sourceName, notify)) {
                Logger.i(context, "Engine", "applied prefetched from ${ready.sourceName}")
                return@withContext true
            }
        }

        val dao = AppDatabase.get(context).usedImageDao()
        val lastKey = if (settings.avoidRepeats) settings.lastAppliedKey else ""
        var lastReason = "No source returned an image."

        for (cfg in sources.shuffled()) {
            val name = cfg.name.ifBlank { cfg.type.name }
            val image = try {
                ProviderFactory.create(ProviderEnv(context, dao, lastKey), cfg).next()
            } catch (t: Throwable) {
                lastReason = "$name error: ${t.message ?: t.javaClass.simpleName}"
                Logger.e(context, "Engine", "source '$name' threw", t)
                null
            }
            if (image == null) {
                Logger.w(context, "Engine", "source '$name' returned no image")
                continue
            }
            if (applyAndFinish(image, name, notify)) {
                Logger.i(context, "Engine", "applied from '$name'")
                return@withContext true
            } else {
                lastReason = "Could not apply image from $name."
            }
        }

        Logger.e(context, "Engine", "change failed: $lastReason")
        if (notify) Notifications.changeFailed(context, lastReason)
        false
    }

    private suspend fun applyAndFinish(image: ProvidedImage, sourceName: String, notify: Boolean): Boolean {
        val repo = SettingsRepository(context)
        val settings = repo.current()
        return try {
            WallpaperSetter(context).apply(image, settings)
            HistoryManager.record(context, sourceName, image.label, image.openStream)
            image.onConsumed()
            repo.update { it.copy(lastAppliedKey = image.key) }
            org.piarsenal.wallchanger.data.CacheManager.enforceLimit(context)
            if (notify) Notifications.changed(context, image.label)
            true
        } catch (t: Throwable) {
            Logger.e(context, "Engine", "apply failed for $sourceName", t)
            false
        }
    }
}
