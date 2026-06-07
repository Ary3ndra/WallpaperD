package org.piarsenal.wallchanger.provider

import okhttp3.Request
import org.piarsenal.wallchanger.log.Logger
import org.piarsenal.wallchanger.net.Http
import java.io.File

/**
 * Download a URL to [dest]. Never throws — logs the exact reason (HTTP code or exception) and
 * returns false, so callers can move on and the user can read why in the in-app log.
 */
internal fun downloadTo(
    url: String,
    dest: File,
    env: ProviderEnv,
    tag: String,
    headers: Map<String, String> = emptyMap()
): Boolean {
    return try {
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        Http.client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) {
                Logger.e(env.context, tag, "download HTTP ${resp.code} for ${url.takeLast(80)}")
                return false
            }
            val body = resp.body ?: run {
                Logger.e(env.context, tag, "download: empty body for ${url.takeLast(80)}")
                return false
            }
            dest.outputStream().use { out -> body.byteStream().copyTo(out) }
            if (dest.length() <= 0) {
                Logger.e(env.context, tag, "download: 0 bytes written for ${url.takeLast(80)}")
                return false
            }
            true
        }
    } catch (t: Throwable) {
        Logger.e(env.context, tag, "download failed for ${url.takeLast(80)}", t)
        false
    }
}

/** Cache dir for downloaded wallpapers. Cleared by the "delete cache" action. */
internal fun cacheRoot(env: ProviderEnv): File =
    File(env.context.filesDir, "wallpapers").apply { mkdirs() }
