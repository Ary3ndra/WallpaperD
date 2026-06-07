package org.piarsenal.wallchanger.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.piarsenal.wallchanger.data.db.AppDatabase
import java.io.File

/** Wipes downloaded wallpapers (source cache + prefetch slot) and enforces the size cap. */
object CacheManager {

    private fun dirs(context: Context) = listOf(
        File(context.filesDir, "wallpapers"),
        File(context.filesDir, "prefetch")
    )

    suspend fun sizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        dirs(context).sumOf { dir ->
            dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        dirs(context).forEach { it.deleteRecursively(); it.mkdirs() }
        AppDatabase.get(context).usedImageDao().clear()
        SettingsRepository(context).update { it.copy(prefetched = null) }
    }

    /** Delete oldest cached files until under the user's limit. Never touches the prefetch slot file. */
    suspend fun enforceLimit(context: Context) = withContext(Dispatchers.IO) {
        val limitBytes = SettingsRepository(context).current().cacheLimitMb.coerceAtLeast(0) * 1024L * 1024L
        if (limitBytes <= 0) return@withContext
        val cacheDir = File(context.filesDir, "wallpapers")
        val files = cacheDir.walkBottomUp().filter { it.isFile }
            .sortedBy { it.lastModified() }   // oldest first
            .toMutableList()
        var total = files.sumOf { it.length() }
        var i = 0
        while (total > limitBytes && i < files.size) {
            val f = files[i]; val len = f.length()
            if (f.delete()) total -= len
            i++
        }
    }
}
