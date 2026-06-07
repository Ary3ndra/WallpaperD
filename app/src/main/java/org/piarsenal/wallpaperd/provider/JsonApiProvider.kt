package org.piarsenal.wallpaperd.provider

import okhttp3.Request
import org.piarsenal.wallpaperd.data.SourceConfig
import org.piarsenal.wallpaperd.net.Http
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileInputStream

/**
 * Generic provider for "any source": GET a JSON endpoint, extract an image URL via a dot path,
 * download it, delete after use. Makes the app fully customisable for arbitrary APIs.
 * Path examples:  "url"  |  "data.0.url"  |  "photos.0.src.large2x"
 */
class JsonApiProvider(
    private val env: ProviderEnv,
    private val cfg: SourceConfig
) : WallpaperProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun next(): ProvidedImage? {
        if (cfg.jsonEndpoint.isBlank() || cfg.jsonImagePath.isBlank()) return null

        val builder = Request.Builder().url(cfg.jsonEndpoint)
        cfg.jsonHeaders.forEach { (k, v) -> builder.addHeader(k, v) }
        val body = Http.client.newCall(builder.build()).execute().use {
            if (!it.isSuccessful) return null; it.body?.string() ?: return null
        }

        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
        val imageUrl = extract(root, cfg.jsonImagePath.split('.')) ?: return null

        val dir = File(cacheRoot(env), cfg.id).apply { mkdirs() }
        val dest = File(dir, "json_${System.currentTimeMillis()}.img")
        if (!downloadTo(imageUrl, dest, env, "JsonApi", cfg.jsonHeaders)) return null

        return ProvidedImage(
            key = imageUrl,
            label = imageUrl.takeLast(40),
            openStream = { FileInputStream(dest) },
            onConsumed = { dest.delete() }
        )
    }

    /** Walk object keys and array indices to resolve a string value. */
    private fun extract(element: JsonElement, path: List<String>): String? {
        var cur: JsonElement = element
        for (seg in path) {
            cur = when (cur) {
                is JsonObject -> cur[seg] ?: return null
                is JsonArray -> seg.toIntOrNull()?.let { cur.getOrNull(it) } ?: return null
                else -> return null
            }
        }
        return runCatching { cur.jsonPrimitive.content }.getOrNull()
    }
}
