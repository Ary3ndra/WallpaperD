package org.piarsenal.wallpaperd.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.piarsenal.wallpaperd.data.AppSettings
import org.piarsenal.wallpaperd.data.CacheManager
import org.piarsenal.wallpaperd.data.HistoryManager
import org.piarsenal.wallpaperd.data.SettingsRepository
import org.piarsenal.wallpaperd.data.SourceConfig
import org.piarsenal.wallpaperd.data.WallOrientation
import org.piarsenal.wallpaperd.data.db.AppDatabase
import org.piarsenal.wallpaperd.data.db.AppliedWallpaper
import org.piarsenal.wallpaperd.log.Logger
import org.piarsenal.wallpaperd.wallpaper.WallpaperController
import org.piarsenal.wallpaperd.work.Scheduler

class WallpaperViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)
    private val historyDao = AppDatabase.get(app).appliedWallpaperDao()

    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val history: StateFlow<List<AppliedWallpaper>> =
        historyDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cacheBytes = MutableStateFlow(0L)
    val logText = MutableStateFlow("")

    // Change state lives in a process-scoped controller so it survives screen changes/backgrounding.
    val isChanging: StateFlow<Boolean> = WallpaperController.isChanging

    init { refreshCache() }

    fun refreshLogs() = viewModelScope.launch {
        logText.value = withContext(Dispatchers.IO) { Logger.recent(getApplication()) }
    }

    fun clearLogs() = viewModelScope.launch {
        Logger.clear(getApplication())
        logText.value = ""
    }

    private fun edit(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        repo.update(transform)
        Scheduler.apply(getApplication(), repo.current())  // reschedule on every change
    }

    fun setEnabled(on: Boolean) = edit { it.copy(enabled = on) }
    fun setInterval(minutes: Long) = edit { it.copy(intervalMinutes = minutes) }
    fun setWifiOnly(on: Boolean) = edit { it.copy(onlyOnWifi = on) }
    fun setChargingOnly(on: Boolean) = edit { it.copy(onlyWhenCharging = on) }
    fun setApplyHome(on: Boolean) = edit { it.copy(applyToHomeScreen = on) }
    fun setApplyLock(on: Boolean) = edit { it.copy(applyToLockScreen = on) }
    fun setNotify(on: Boolean) = edit { it.copy(notifyOnChange = on) }
    fun setAvoidRepeats(on: Boolean) = edit { it.copy(avoidRepeats = on) }
    fun setPrefetch(on: Boolean) = edit { it.copy(prefetchEnabled = on) }
    fun setCacheLimitMb(mb: Long) = edit { it.copy(cacheLimitMb = mb.coerceAtLeast(0)) }
    fun setFitToScreen(on: Boolean) = edit { it.copy(fitToScreen = on) }
    fun setRejectIncompatible(on: Boolean) = edit { it.copy(rejectIncompatible = on) }
    fun setOrientation(o: WallOrientation) = edit { it.copy(orientation = o) }
    fun resizeCurrent() = WallpaperController.resizeCurrent(getApplication())

    fun upsertSource(source: SourceConfig) = edit { s ->
        val list = s.sources.toMutableList()
        val idx = list.indexOfFirst { it.id == source.id }
        if (idx >= 0) list[idx] = source else list.add(source)
        s.copy(sources = list)
    }

    fun removeSource(id: String) = edit { s -> s.copy(sources = s.sources.filterNot { it.id == id }) }

    // Manual change + re-apply run in the controller's app scope (see WallpaperController).
    fun changeNow() {
        WallpaperController.changeNow(getApplication())
        refreshCache()
    }

    fun reapply(item: AppliedWallpaper) = WallpaperController.reapply(getApplication(), item)

    // ---- History ----

    fun toggleFavourite(item: AppliedWallpaper) = viewModelScope.launch {
        historyDao.setFavourite(item.id, !item.favourite)
    }

    fun deleteHistory(item: AppliedWallpaper) = viewModelScope.launch {
        HistoryManager.deleteOne(getApplication(), item.id)
    }

    fun clearHistory() = viewModelScope.launch {
        HistoryManager.clearNonFavourites(getApplication())
    }

    // ---- Cache ----

    fun clearCache() = viewModelScope.launch {
        CacheManager.clear(getApplication())
        refreshCache()
    }

    private fun refreshCache() = viewModelScope.launch {
        cacheBytes.value = CacheManager.sizeBytes(getApplication())
    }
}
