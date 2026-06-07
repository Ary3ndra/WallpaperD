package org.piarsenal.wallpaperd.provider

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.piarsenal.wallpaperd.data.SourceConfig

/**
 * Random image from a user-picked folder (Storage Access Framework tree).
 * Equivalent to the GNOME folder provider. With [SourceConfig.folderRecursive] it also walks
 * every child directory. No download, no deletion.
 */
class FolderProvider(
    private val env: ProviderEnv,
    private val cfg: SourceConfig
) : WallpaperProvider {

    override suspend fun next(): ProvidedImage? {
        val treeUri = cfg.folderTreeUri?.let(Uri::parse) ?: return null
        val root = DocumentFile.fromTreeUri(env.context, treeUri) ?: return null

        val images = ArrayList<DocumentFile>()
        if (cfg.folderRecursive) collectRecursive(root, images) else collectFlat(root, images)
        if (images.isEmpty()) return null

        // Avoid repeating the previous image when there is a choice.
        val pool = images.filter { it.uri.toString() != env.lastAppliedKey }.ifEmpty { images }
        val pick = pool.random()

        return ProvidedImage(
            key = pick.uri.toString(),
            label = pick.name ?: "folder image",
            openStream = {
                env.context.contentResolver.openInputStream(pick.uri)
                    ?: error("Cannot open ${pick.uri}")
            }
        )
    }

    private fun collectFlat(dir: DocumentFile, out: MutableList<DocumentFile>) {
        dir.listFiles().forEach { if (it.isFile && it.isImage()) out.add(it) }
    }

    // Bounded DFS so a deep/huge tree can't blow the stack or hang forever.
    private fun collectRecursive(root: DocumentFile, out: MutableList<DocumentFile>) {
        val excluded = cfg.folderExcludeNames.split(',')
            .map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        val stack = ArrayDeque<DocumentFile>()
        stack.addLast(root)
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val dir = stack.removeLast()
            for (child in dir.listFiles()) {
                visited++
                when {
                    child.isDirectory -> {
                        val name = child.name?.lowercase()
                        if (name == null || name !in excluded) stack.addLast(child)
                    }
                    child.isFile && child.isImage() -> out.add(child)
                }
            }
        }
    }

    private fun DocumentFile.isImage(): Boolean {
        if (type?.startsWith("image/") == true) return true
        val n = name?.lowercase() ?: return false
        return listOf(".jpg", ".jpeg", ".png", ".webp", ".bmp").any(n::endsWith)
    }

    private companion object { const val MAX_NODES = 50_000 }
}
