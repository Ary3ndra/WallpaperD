package org.piarsenal.wallpaperd.wallpaper

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.piarsenal.wallpaperd.data.SettingsRepository
import org.piarsenal.wallpaperd.data.db.AppDatabase
import org.piarsenal.wallpaperd.data.db.AppliedWallpaper
import org.piarsenal.wallpaperd.log.Logger
import org.piarsenal.wallpaperd.provider.ProvidedImage
import org.piarsenal.wallpaperd.work.Notifications
import java.io.File
import java.io.FileInputStream

/**
 * Process-scoped owner of manual wallpaper changes. Running on an application-lifetime scope
 * (not a ViewModel/Activity scope) means a change keeps going if the user leaves the screen or
 * backgrounds the app. It still stops if the OS kills the process — see the in-app Help.
 */
object WallpaperController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isChanging = MutableStateFlow(false)
    val isChanging: StateFlow<Boolean> = _isChanging

    fun changeNow(context: Context) {
        val app = context.applicationContext
        scope.launch {
            if (!_isChanging.compareAndSet(expect = false, update = true)) return@launch
            try {
                val ok = WallpaperEngine(app).changeOnce()
                if (ok) Prefetcher.prefetchNext(app)   // get the next one ready
            } finally {
                _isChanging.value = false
            }
        }
    }

    /** Re-crop the most recently applied wallpaper to the screen resolution. */
    fun resizeCurrent(context: Context) {
        val app = context.applicationContext
        scope.launch {
            if (!_isChanging.compareAndSet(expect = false, update = true)) return@launch
            try {
                val s = SettingsRepository(app).current()
                val newest = AppDatabase.get(app).appliedWallpaperDao().observeAll().first().firstOrNull()
                val file = newest?.let { File(it.filePath) }
                if (file == null || !file.exists()) {
                    Logger.w(app, "Resize", "no current wallpaper to resize")
                    return@launch
                }
                if (s.notifyOnChange) Notifications.changing(app)
                val image = ProvidedImage("history:${newest.id}", newest.label, { FileInputStream(file) })
                runCatching { WallpaperSetter(app).apply(image, s, forceFit = true) }
                if (s.notifyOnChange) Notifications.changed(app, "Resized to fit screen")
            } finally {
                _isChanging.value = false
            }
        }
    }

    fun reapply(context: Context, item: AppliedWallpaper) {
        val app = context.applicationContext
        scope.launch {
            if (!_isChanging.compareAndSet(expect = false, update = true)) return@launch
            try {
                val repo = SettingsRepository(app)
                val s = repo.current()
                val file = File(item.filePath)
                if (!file.exists()) return@launch
                if (s.notifyOnChange) Notifications.changing(app)
                val image = ProvidedImage(
                    key = "history:${item.id}",
                    label = item.label,
                    openStream = { FileInputStream(file) }
                )
                runCatching { WallpaperSetter(app).apply(image, s) }
                repo.update { it.copy(lastAppliedKey = "history:${item.id}") }
                AppDatabase.get(app).appliedWallpaperDao().touch(item.id, System.currentTimeMillis())
                if (s.notifyOnChange) Notifications.changed(app, item.label)
            } finally {
                _isChanging.value = false
            }
        }
    }
}
