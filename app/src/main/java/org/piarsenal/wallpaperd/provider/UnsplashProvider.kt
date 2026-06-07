package org.piarsenal.wallpaperd.provider

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.piarsenal.wallpaperd.data.SourceConfig
import org.piarsenal.wallpaperd.net.Http
import org.piarsenal.wallpaperd.net.UnsplashPhoto
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream

/**
 * Unsplash random-photo provider. Needs a free access key (client_id). Honours the API guideline
 * of pinging download_location so the photographer gets a download credit. Image deleted after use.
 */
class UnsplashProvider(
    private val env: ProviderEnv,
    private val cfg: SourceConfig
) : WallpaperProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun next(): ProvidedImage? {
        if (cfg.unsplashAccessKey.isBlank()) return null

        val url = "https://api.unsplash.com/photos/random".toHttpUrl().newBuilder().apply {
            addQueryParameter("client_id", cfg.unsplashAccessKey)
            if (cfg.unsplashQuery.isNotBlank()) addQueryParameter("query", cfg.unsplashQuery)
            if (cfg.unsplashOrientation.isNotBlank()) addQueryParameter("orientation", cfg.unsplashOrientation)
        }.build()

        val body = Http.client.newCall(Request.Builder().url(url).build()).execute().use {
            if (!it.isSuccessful) return null; it.body?.string() ?: return null
        }
        val photo = runCatching { json.decodeFromString<UnsplashPhoto>(body) }.getOrNull() ?: return null
        val imageUrl = when (cfg.quality) {
            org.piarsenal.wallpaperd.data.ImageQuality.LOW -> photo.urls.regular
            org.piarsenal.wallpaperd.data.ImageQuality.REGULAR -> photo.urls.regular.ifBlank { photo.urls.full }
            org.piarsenal.wallpaperd.data.ImageQuality.FULL -> photo.urls.full.ifBlank { photo.urls.raw }
        }.ifBlank { photo.urls.regular.ifBlank { photo.urls.raw } }
        if (imageUrl.isBlank()) return null

        triggerDownload(photo.links.download_location, cfg.unsplashAccessKey)

        val dir = File(cacheRoot(env), cfg.id).apply { mkdirs() }
        val dest = File(dir, "unsplash_${photo.id.ifBlank { System.currentTimeMillis().toString() }}.img")
        if (!downloadTo(imageUrl, dest, env, "Unsplash")) return null

        val credit = photo.user.name.ifBlank { "Unsplash" }
        return ProvidedImage(
            key = "unsplash:${photo.id}",
            label = "Unsplash by $credit",
            openStream = { FileInputStream(dest) },
            onConsumed = { dest.delete() }
        )
    }

    private fun triggerDownload(downloadLocation: String, key: String) {
        if (downloadLocation.isBlank()) return
        runCatching {
            val u = downloadLocation.toHttpUrl().newBuilder()
                .addQueryParameter("client_id", key).build()
            Http.client.newCall(Request.Builder().url(u).build()).execute().close()
        }
    }
}
