package org.piarsenal.wallchanger.provider

import android.content.Context
import org.piarsenal.wallchanger.data.db.UsedImageDao
import java.io.InputStream

/** Everything a provider needs, handed in so providers stay free of singletons. */
class ProviderEnv(
    val context: Context,
    val dao: UsedImageDao,
    val lastAppliedKey: String = ""   // lets providers avoid repeating the previous image
)

/**
 * A wallpaper ready to apply. [openStream] is re-callable and yields image bytes; [onConsumed]
 * runs after the image has been set, letting online providers delete the cached file
 * (GNOME "delete as used"). [key] is a stable identity used for repeat-avoidance and history.
 */
class ProvidedImage(
    val key: String,
    val label: String,
    val openStream: () -> InputStream,
    val onConsumed: suspend () -> Unit = {}
)

/** A single wallpaper source. One call = one wallpaper, downloading only if needed. */
interface WallpaperProvider {
    suspend fun next(): ProvidedImage?
}
