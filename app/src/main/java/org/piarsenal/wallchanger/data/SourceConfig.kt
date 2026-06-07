package org.piarsenal.wallchanger.data

import kotlinx.serialization.Serializable

/**
 * One configurable wallpaper source. The whole list is JSON-encoded into DataStore,
 * so users can add as many sources as they want ("fully customisable").
 */
@Serializable
data class SourceConfig(
    val id: String,
    val type: SourceType,
    val name: String,
    val enabled: Boolean = true,
    val quality: ImageQuality = ImageQuality.FULL, // used by Unsplash & NASA APOD

    // FOLDER
    val folderTreeUri: String? = null,
    val folderRecursive: Boolean = false, // walk child directories as well
    val folderExcludeNames: String = "", // comma-separated folder names to skip when recursing

    // GITHUB  (public repo tree -> random image, streamed via raw.githubusercontent.com)
    val ghUrl: String = "",               // e.g. https://github.com/owner/repo  or .../tree/branch/path
    val ghOwner: String = "",             // legacy/fallback if ghUrl is empty
    val ghRepo: String = "",
    val ghBranch: String = "main",
    val ghPath: String = "",              // optional subfolder; blank = repo root
    val ghRecursive: Boolean = true,      // descend into child folders under ghPath
    val ghToken: String = "",             // optional PAT for private repos / higher rate limit

    // WALLHAVEN  (see https://wallhaven.cc/help/api)
    val whApiKey: String = "",            // required only for NSFW / higher rate limits
    val whQuery: String = "",             // search terms / tags
    val whCategories: String = "100",     // bits: general / anime / people
    val whPurity: String = "100",         // bits: sfw / sketchy / nsfw (nsfw needs apikey)
    val whRatios: String = "16x9",
    val whSorting: String = "random",

    // UNSPLASH  (https://unsplash.com/documentation#get-a-random-photo)
    val unsplashAccessKey: String = "",
    val unsplashQuery: String = "",
    val unsplashOrientation: String = "landscape",  // landscape / portrait / squarish

    // NASA_APOD  (https://api.nasa.gov)
    val nasaApiKey: String = "DEMO_KEY",  // DEMO_KEY works but is rate-limited

    // URL_LIST
    val urls: List<String> = emptyList(),

    // JSON_API
    val jsonEndpoint: String = "",
    val jsonImagePath: String = "",       // dot path, e.g. "data.0.url" or "images.0.src.large"
    val jsonHeaders: Map<String, String> = emptyMap()
)
