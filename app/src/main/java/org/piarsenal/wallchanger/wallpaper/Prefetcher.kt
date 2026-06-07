package org.piarsenal.wallchanger.wallpaper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.piarsenal.wallchanger.data.PrefetchedImage
import org.piarsenal.wallchanger.data.SettingsRepository
import org.piarsenal.wallchanger.data.db.AppDatabase
import org.piarsenal.wallchanger.provider.ProvidedImage
import org.piarsenal.wallchanger.provider.ProviderEnv
import org.piarsenal.wallchanger.provider.ProviderFactory
import java.io.File
import java.io.FileInputStream

/**
 * Keeps one wallpaper downloaded and ready so the next change applies with no network wait.
 * This is the main fix for online-source lag: the slow download happens in the background after
 * the previous change, off the critical path. In the periodic worker it also shortens the wake
 * window (apply a ready file, then fetch the next while already awake).
 */
object Prefetcher {

    private val mutex = Mutex()

    class Ready(val image: ProvidedImage, val sourceName: String)

    private fun dir(context: Context) = File(context.filesDir, "prefetch").apply { mkdirs() }

    /** Hand over the ready image if the slot is valid, wiring cleanup to clear the slot on use. */
    suspend fun consume(context: Context): Ready? = withContext(Dispatchers.IO) {
        val repo = SettingsRepository(context)
        val s = repo.current()
        val p = s.prefetched ?: return@withContext null

        val stillValid = s.sources.any { it.id == p.sourceId && it.enabled }
        val file = File(p.filePath)
        if (!stillValid || !file.exists()) {
            file.delete(); repo.update { it.copy(prefetched = null) }
            return@withContext null
        }
        Ready(
            ProvidedImage(
                key = p.key,
                label = p.label,
                openStream = { FileInputStream(file) },
                onConsumed = { file.delete(); repo.update { it.copy(prefetched = null) } }
            ),
            p.sourceName
        )
    }

    /** Download the next wallpaper into the slot if empty and enabled. Safe to call concurrently. */
    suspend fun prefetchNext(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val repo = SettingsRepository(context)
            val s = repo.current()
            if (!s.prefetchEnabled || s.prefetched != null) return@withLock
            val sources = s.sources.filter { it.enabled }
            if (sources.isEmpty()) return@withLock

            val dao = AppDatabase.get(context).usedImageDao()
            val lastKey = if (s.avoidRepeats) s.lastAppliedKey else ""

            for (cfg in sources.shuffled()) {
                val env = ProviderEnv(context, dao, lastKey)
                val img = runCatching { ProviderFactory.create(env, cfg).next() }.getOrNull() ?: continue
                val dest = File(dir(context), "pf_${System.currentTimeMillis()}")
                val copied = runCatching {
                    img.openStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
                    dest.length() > 0
                }.getOrDefault(false)
                runCatching { img.onConsumed() }   // free the provider's own temp copy
                if (!copied) { dest.delete(); continue }

                repo.update {
                    it.copy(prefetched = PrefetchedImage(
                        sourceId = cfg.id, key = img.key, label = img.label,
                        sourceName = cfg.name.ifBlank { cfg.type.name }, filePath = dest.absolutePath
                    ))
                }
                org.piarsenal.wallchanger.data.CacheManager.enforceLimit(context)
                return@withLock
            }
        }
    }
}
