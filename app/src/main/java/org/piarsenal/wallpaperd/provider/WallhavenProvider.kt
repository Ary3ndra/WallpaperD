package org.piarsenal.wallpaperd.provider

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.piarsenal.wallpaperd.data.SourceConfig
import org.piarsenal.wallpaperd.data.db.UsedImage
import org.piarsenal.wallpaperd.log.Logger
import org.piarsenal.wallpaperd.net.Http
import org.piarsenal.wallpaperd.net.WallhavenSearch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream

/**
 * Wallhaven REST provider. Replicates the GNOME extension: fetch a page of results, cache them,
 * apply one at a time, delete each file once used. Refetches a new page when the cache runs dry.
 * Every failure path is logged so a non-working source can be diagnosed from the in-app log.
 */
class WallhavenProvider(
    private val env: ProviderEnv,
    private val cfg: SourceConfig
) : WallpaperProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun next(): ProvidedImage? {
        if (env.dao.unconsumedCount(cfg.id) == 0) fetchPage()
        val row = env.dao.nextUnconsumed(cfg.id) ?: run {
            Logger.w(env.context, "Wallhaven", "no images available after fetch (check query/categories/purity)")
            return null
        }
        val file = File(row.filePath)
        if (!file.exists()) {
            env.dao.markConsumed(row.key)
            return null
        }
        return ProvidedImage(
            key = row.key,
            label = "wallhaven ${row.key}",
            openStream = { FileInputStream(file) },
            onConsumed = {
                env.dao.markConsumed(row.key)
                file.delete()
            }
        )
    }

    private suspend fun fetchPage() {
        val url = "https://wallhaven.cc/api/v1/search".toHttpUrl().newBuilder().apply {
            if (cfg.whQuery.isNotBlank()) addQueryParameter("q", cfg.whQuery)
            addQueryParameter("categories", cfg.whCategories)
            addQueryParameter("purity", cfg.whPurity)
            if (cfg.whRatios.isNotBlank()) addQueryParameter("ratios", cfg.whRatios)
            addQueryParameter("sorting", cfg.whSorting)
            if (cfg.whApiKey.isNotBlank()) addQueryParameter("apikey", cfg.whApiKey)
        }.build()

        // Logged without the API key.
        Logger.i(env.context, "Wallhaven", "GET ${url.toString().substringBefore("&apikey")}")

        val body = try {
            Http.client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val snippet = resp.body?.string()?.take(160).orEmpty()
                    Logger.e(env.context, "Wallhaven", "search HTTP ${resp.code} $snippet")
                    return
                }
                resp.body?.string() ?: run {
                    Logger.e(env.context, "Wallhaven", "search: empty body")
                    return
                }
            }
        } catch (t: Throwable) {
            Logger.e(env.context, "Wallhaven", "search request failed", t)
            return
        }

        val items = try {
            json.decodeFromString<WallhavenSearch>(body).data
        } catch (t: Throwable) {
            Logger.e(env.context, "Wallhaven", "could not parse search response", t)
            return
        }

        if (items.isEmpty()) {
            Logger.w(env.context, "Wallhaven", "search returned 0 results for this query/filter")
            return
        }

        val dir = File(cacheRoot(env), cfg.id).apply { mkdirs() }
        val rows = items.mapNotNull { item ->
            val ext = item.path.substringAfterLast('.', "jpg")
            val dest = File(dir, "${item.id}.$ext")
            if (downloadTo(item.path, dest, env, "Wallhaven")) {
                UsedImage(key = "${cfg.id}:${item.id}", sourceId = cfg.id, filePath = dest.absolutePath)
            } else null
        }
        Logger.i(env.context, "Wallhaven", "fetched ${rows.size}/${items.size} images")
        env.dao.insertAll(rows)
    }
}
