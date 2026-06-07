package org.piarsenal.wallpaperd.data

import kotlinx.serialization.Serializable

/** A wallpaper already downloaded and waiting, so the next change can apply instantly. */
@Serializable
data class PrefetchedImage(
    val sourceId: String,
    val key: String,
    val label: String,
    val sourceName: String,
    val filePath: String
)

/** Global behaviour. Stored as a single JSON blob in DataStore. */
@Serializable
data class AppSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Long = 60,      // clamped to >= 15 by the scheduler (WorkManager limit)
    val onlyOnWifi: Boolean = false,     // network constraint for online providers
    val onlyWhenCharging: Boolean = false,
    val applyToLockScreen: Boolean = true,
    val applyToHomeScreen: Boolean = true,
    val notifyOnChange: Boolean = true,
    val avoidRepeats: Boolean = true,    // skip the immediately previous image when alternatives exist
    val prefetchEnabled: Boolean = true, // download the next wallpaper ahead of time
    val cacheLimitMb: Long = 500,        // cap on downloaded cache size; oldest pruned past this
    val lastAppliedKey: String = "",     // identity of the last applied image (for avoidRepeats)
    val prefetched: PrefetchedImage? = null,
    val sources: List<SourceConfig> = emptyList()
)
