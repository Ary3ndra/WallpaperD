package org.piarsenal.wallpaperd.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.piarsenal.wallpaperd.data.db.AppDatabase
import org.piarsenal.wallpaperd.data.db.AppliedWallpaper
import java.io.File
import java.io.InputStream

/**
 * Persists applied wallpapers for the history screen. History lives in its own directory so the
 * "delete cache" action (which wipes downloaded source files) leaves history intact.
 */
object HistoryManager {

    private const val KEEP = 60   // non-favourites retained; favourites are never pruned

    fun historyDir(context: Context): File = File(context.filesDir, "history").apply { mkdirs() }

    /** Copy the just-applied image into history and record it. Runs before the source deletes it. */
    suspend fun record(
        context: Context,
        sourceName: String,
        label: String,
        openStream: () -> InputStream
    ) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(context).appliedWallpaperDao()
        val dest = File(historyDir(context), "h_${System.currentTimeMillis()}.img")
        runCatching {
            openStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
        }.onFailure { return@withContext }

        dao.insert(AppliedWallpaper(sourceName = sourceName, label = label, filePath = dest.absolutePath))

        // Prune oldest non-favourites and remove their orphaned files.
        val keepPaths = dao.nonFavouritePaths().toSet()
        dao.prune(KEEP)
        val stillKept = dao.nonFavouritePaths().toSet()
        (keepPaths - stillKept).forEach { runCatching { File(it).delete() } }
    }

    suspend fun clearNonFavourites(context: Context) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(context).appliedWallpaperDao()
        dao.nonFavouritePaths().forEach { runCatching { File(it).delete() } }
        dao.clearNonFavourites()
    }

    suspend fun deleteOne(context: Context, id: Long) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(context).appliedWallpaperDao()
        dao.byId(id)?.let { runCatching { File(it.filePath).delete() } }
        dao.delete(id)
    }
}
