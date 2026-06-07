package org.piarsenal.wallchanger.net

import kotlinx.serialization.Serializable

@Serializable
data class ApodResponse(
    val title: String = "",
    val url: String = "",
    val hdurl: String = "",
    val media_type: String = ""   // "image" or "video"
)
