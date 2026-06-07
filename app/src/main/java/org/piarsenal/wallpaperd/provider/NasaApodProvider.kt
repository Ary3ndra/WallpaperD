package org.piarsenal.wallpaperd.provider

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.piarsenal.wallpaperd.data.SourceConfig
import org.piarsenal.wallpaperd.net.ApodResponse
import org.piarsenal.wallpaperd.net.Http
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream

/**
 * NASA Astronomy Picture of the Day. Some days are videos; those are skipped (returns null, the
 * worker falls through to another source). Prefers the HD url. Image deleted after use.
 */
class NasaApodProvider(
    private val env: ProviderEnv,
    private val cfg: SourceConfig
) : WallpaperProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun next(): ProvidedImage? {
        val key = cfg.nasaApiKey.ifBlank { "DEMO_KEY" }
        val url = "https://api.nasa.gov/planetary/apod".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", key)
            .build()

        val body = Http.client.newCall(Request.Builder().url(url).build()).execute().use {
            if (!it.isSuccessful) return null; it.body?.string() ?: return null
        }
        val apod = runCatching { json.decodeFromString<ApodResponse>(body) }.getOrNull() ?: return null
        if (apod.media_type != "image") return null   // skip video days

        val imageUrl = when (cfg.quality) {
            org.piarsenal.wallpaperd.data.ImageQuality.FULL -> apod.hdurl.ifBlank { apod.url }
            else -> apod.url.ifBlank { apod.hdurl }   // standard-res is smaller
        }
        if (imageUrl.isBlank()) return null

        val dir = File(cacheRoot(env), cfg.id).apply { mkdirs() }
        val dest = File(dir, "apod_${System.currentTimeMillis()}.img")
        if (!downloadTo(imageUrl, dest, env, "Nasa")) return null

        return ProvidedImage(
            key = "apod:$imageUrl",
            label = "NASA APOD: ${apod.title.take(40)}",
            openStream = { FileInputStream(dest) },
            onConsumed = { dest.delete() }
        )
    }
}
