package org.piarsenal.wallpaperd.provider

import org.piarsenal.wallpaperd.data.SourceConfig
import java.io.File
import java.io.FileInputStream

/** Picks a random direct image URL from a user-supplied list, downloads it, deletes after use. */
class UrlListProvider(
    private val env: ProviderEnv,
    private val cfg: SourceConfig
) : WallpaperProvider {

    override suspend fun next(): ProvidedImage? {
        val all = cfg.urls.filter { it.isNotBlank() }
        if (all.isEmpty()) return null
        val candidates = all.filterNot { it == env.lastAppliedKey }.ifEmpty { all }
        val url = candidates.random()

        val dir = File(cacheRoot(env), cfg.id).apply { mkdirs() }
        val dest = File(dir, "url_${System.currentTimeMillis()}.img")
        if (!downloadTo(url, dest, env, "UrlList")) return null
        return ProvidedImage(
            key = url,
            label = url.takeLast(40),
            openStream = { FileInputStream(dest) },
            onConsumed = { dest.delete() }
        )
    }
}
