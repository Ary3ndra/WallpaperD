package org.piarsenal.wallpaperd.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.piarsenal.wallpaperd.data.AppSettings
import org.piarsenal.wallpaperd.provider.ProvidedImage
import kotlin.math.max

/**
 * Applies a ProvidedImage. Fast path uses setStream. When fitting is requested (auto-fit setting
 * or a manual resize) the image is decoded, scaled to cover the screen, and center-cropped —
 * supersizing small images and shrinking large ones.
 */
class WallpaperSetter(private val context: Context) {

    private val wm = WallpaperManager.getInstance(context)

    fun apply(image: ProvidedImage, settings: AppSettings, forceFit: Boolean = false) {
        val which = whichFlags(settings)
        if (forceFit || settings.fitToScreen) {
            val bmp = decodeFitted(image)
            try { wm.setBitmap(bmp, null, true, which) } finally { bmp.recycle() }
        } else {
            image.openStream().use { wm.setStream(it, null, true, which) }
        }
    }

    private fun whichFlags(settings: AppSettings): Int {
        val home = if (settings.applyToHomeScreen) WallpaperManager.FLAG_SYSTEM else 0
        val lock = if (settings.applyToLockScreen) WallpaperManager.FLAG_LOCK else 0
        val w = home or lock
        return if (w == 0) WallpaperManager.FLAG_SYSTEM else w
    }

    private fun decodeFitted(image: ProvidedImage): Bitmap {
        val (tw, th) = screenSize()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        image.openStream().use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, tw, th)
        }
        val src = image.openStream().use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Could not decode image")
        return cropToCover(src, tw, th)
    }

    private fun screenSize(): Pair<Int, Int> {
        val dm = context.resources.displayMetrics
        return max(dm.widthPixels, 1) to max(dm.heightPixels, 1)
    }

    private fun sampleSize(srcW: Int, srcH: Int, tw: Int, th: Int): Int {
        if (srcW <= 0 || srcH <= 0) return 1
        var s = 1
        while (srcW / (s * 2) >= tw && srcH / (s * 2) >= th) s *= 2
        return s
    }

    private fun cropToCover(src: Bitmap, tw: Int, th: Int): Bitmap {
        val scale = max(tw.toFloat() / src.width, th.toFloat() / src.height)
        val sw = (src.width * scale).toInt().coerceAtLeast(tw)
        val sh = (src.height * scale).toInt().coerceAtLeast(th)
        val scaled = Bitmap.createScaledBitmap(src, sw, sh, true)
        if (scaled != src) src.recycle()
        val x = ((sw - tw) / 2).coerceAtLeast(0)
        val y = ((sh - th) / 2).coerceAtLeast(0)
        val out = Bitmap.createBitmap(scaled, x, y, tw, th)
        if (out != scaled) scaled.recycle()
        return out
    }
}
