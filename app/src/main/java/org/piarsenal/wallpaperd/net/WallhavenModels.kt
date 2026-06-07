package org.piarsenal.wallpaperd.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WallhavenSearch(
    val data: List<WallhavenItem> = emptyList()
)

@Serializable
data class WallhavenItem(
    val id: String,
    val path: String,                       // direct full-resolution image URL
    @SerialName("file_type") val fileType: String = "",
    val resolution: String = ""
)
