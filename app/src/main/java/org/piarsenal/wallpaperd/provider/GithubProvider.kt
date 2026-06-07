package org.piarsenal.wallpaperd.provider

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.piarsenal.wallpaperd.data.SourceConfig
import org.piarsenal.wallpaperd.log.Logger
import org.piarsenal.wallpaperd.net.GhTree
import org.piarsenal.wallpaperd.net.Http
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream

/**
 * Streams a random image out of a GitHub repo's file tree without cloning. Accepts a repo URL
 * (optionally a .../tree/branch/path link); branch and subfolder fields override what's in the URL.
 * Lists the tree once, picks a random image, fetches it from raw.githubusercontent.com, deletes
 * after use. Built for big repos: never pulls more than the single image it applies.
 */
class GithubProvider(
    private val env: ProviderEnv,
    private val cfg: SourceConfig
) : WallpaperProvider {

    private val json = Json { ignoreUnknownKeys = true }

    private data class Repo(val owner: String, val repo: String, val branch: String?, val path: String?)

    override suspend fun next(): ProvidedImage? {
        val r = resolveRepo() ?: run {
            Logger.e(env.context, "GitHub", "could not parse repo from URL/owner/repo")
            return null
        }
        val branch = cfg.ghBranch.takeIf { it.isNotBlank() && it != "main" } ?: r.branch ?: cfg.ghBranch.ifBlank { "main" }
        val basePath = (cfg.ghPath.ifBlank { r.path ?: "" }).trim('/')

        val treeUrl = "https://api.github.com".toHttpUrl().newBuilder()
            .addPathSegments("repos/${r.owner}/${r.repo}/git/trees/$branch")
            .addQueryParameter("recursive", "1")
            .build()

        val req = Request.Builder().url(treeUrl)
            .header("Accept", "application/vnd.github+json")
            .apply { if (cfg.ghToken.isNotBlank()) header("Authorization", "Bearer ${cfg.ghToken}") }
            .build()

        val body = try {
            Http.client.newCall(req).execute().use {
                if (!it.isSuccessful) {
                    Logger.e(env.context, "GitHub", "tree HTTP ${it.code} for ${r.owner}/${r.repo}@$branch")
                    return null
                }
                it.body?.string() ?: return null
            }
        } catch (t: Throwable) {
            Logger.e(env.context, "GitHub", "tree request failed", t); return null
        }

        val tree = runCatching { json.decodeFromString<GhTree>(body) }.getOrNull() ?: run {
            Logger.e(env.context, "GitHub", "could not parse tree response"); return null
        }
        if (tree.truncated) Logger.w(env.context, "GitHub", "repo tree was truncated; picking from partial list")

        val candidates = tree.tree.filter { it.type == "blob" && it.path.isImage() && it.path.underBase(basePath, cfg.ghRecursive) }
        if (candidates.isEmpty()) {
            Logger.w(env.context, "GitHub", "no images under '${basePath.ifBlank { "/" }}' (recursive=${cfg.ghRecursive})")
            return null
        }

        val pool = candidates.filter { "gh:${it.path}" != env.lastAppliedKey }.ifEmpty { candidates }
        val entry = pool.random()

        val rawUrl = "https://raw.githubusercontent.com".toHttpUrl().newBuilder()
            .addPathSegment(r.owner).addPathSegment(r.repo).addPathSegment(branch)
            .addPathSegments(entry.path)
            .build()

        val headers = if (cfg.ghToken.isNotBlank()) mapOf("Authorization" to "Bearer ${cfg.ghToken}") else emptyMap()
        val dir = File(cacheRoot(env), cfg.id).apply { mkdirs() }
        val dest = File(dir, "gh_${System.currentTimeMillis()}_${entry.path.substringAfterLast('/')}")
        if (!downloadTo(rawUrl.toString(), dest, env, "GitHub", headers)) return null

        return ProvidedImage(
            key = "gh:${entry.path}",
            label = "${r.repo}/${entry.path}".takeLast(48),
            openStream = { FileInputStream(dest) },
            onConsumed = { dest.delete() }
        )
    }

    /** Parse owner/repo (+ optional branch/path) from ghUrl, falling back to legacy owner/repo fields. */
    private fun resolveRepo(): Repo? {
        val url = cfg.ghUrl.trim()
        if (url.isNotBlank()) {
            val cleaned = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                .removePrefix("github.com/").trim('/')
            val parts = cleaned.split('/').filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val owner = parts[0]
                val repo = parts[1].removeSuffix(".git")
                var branch: String? = null
                var path: String? = null
                if (parts.size >= 4 && parts[2] == "tree") {
                    branch = parts[3]
                    if (parts.size >= 5) path = parts.drop(4).joinToString("/")
                }
                return Repo(owner, repo, branch, path)
            }
            return null
        }
        if (cfg.ghOwner.isNotBlank() && cfg.ghRepo.isNotBlank())
            return Repo(cfg.ghOwner, cfg.ghRepo, null, null)
        return null
    }

    private fun String.isImage(): Boolean {
        val n = lowercase()
        return listOf(".jpg", ".jpeg", ".png", ".webp", ".bmp").any(n::endsWith)
    }

    private fun String.underBase(base: String, recursive: Boolean): Boolean {
        val rel = when {
            base.isEmpty() -> this
            this == base -> return false
            startsWith("$base/") -> removePrefix("$base/")
            else -> return false
        }
        return if (recursive) true else !rel.contains('/')
    }
}
