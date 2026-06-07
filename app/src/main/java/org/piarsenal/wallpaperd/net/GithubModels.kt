package org.piarsenal.wallpaperd.net

import kotlinx.serialization.Serializable

@Serializable
data class GhTree(
    val tree: List<GhEntry> = emptyList(),
    val truncated: Boolean = false   // GitHub caps the tree response; partial result if true
)

@Serializable
data class GhEntry(
    val path: String = "",
    val type: String = "",           // "blob" (file) or "tree" (dir)
    val size: Long = 0
)
