package org.piarsenal.wallpaperd.net

import kotlinx.serialization.Serializable

@Serializable
data class UnsplashPhoto(
    val urls: UnsplashUrls = UnsplashUrls(),
    val links: UnsplashLinks = UnsplashLinks(),
    val user: UnsplashUser = UnsplashUser(),
    val id: String = ""
)

@Serializable
data class UnsplashUrls(val raw: String = "", val full: String = "", val regular: String = "")

@Serializable
data class UnsplashLinks(val download_location: String = "")

@Serializable
data class UnsplashUser(val name: String = "")
