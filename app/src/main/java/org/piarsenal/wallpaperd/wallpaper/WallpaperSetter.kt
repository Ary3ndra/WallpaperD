package org.piarsenal.wallpaperd.wallpaper

import android.app.WallpaperManager
import android.content.Context
import org.piarsenal.wallpaperd.data.AppSettings
import org.piarsenal.wallpaperd.provider.ProvidedImage

/**
 * Applies a ProvidedImage to the system wallpaper. Uses WallpaperManager.setStream so the
 * framework handles decoding/downsampling natively — faster and lighter than decoding to a
 * Bitmap ourselves, which was the slow step on large downloaded images.
 */
class WallpaperSetter(context: Context) {

    private val wm = WallpaperManager.getInstance(context)

    fun apply(image: ProvidedImage, settings: AppSettings) {
        val home = if (settings.applyToHomeScreen) WallpaperManager.FLAG_SYSTEM else 0
        val lock = if (settings.applyToLockScreen) WallpaperManager.FLAG_LOCK else 0
        var which = home or lock
        if (which == 0) which = WallpaperManager.FLAG_SYSTEM   // never set nothing

        // setStream consumes the stream once and lets the system scale to wallpaper size.
        image.openStream().use { wm.setStream(it, null, true, which) }
    }
}
