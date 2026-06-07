package org.piarsenal.wallpaperd.data

/** Provider kinds, mirroring the GNOME extension's provider model plus customizable sources. */
enum class SourceType {
    FOLDER,     // random image from a local folder (SAF tree)
    WALLHAVEN,  // wallhaven.cc REST API, downloads a page, consumes one at a time
    UNSPLASH,   // unsplash.com random photo API (needs access key)
    NASA_APOD,  // NASA Astronomy Picture of the Day
    GITHUB,     // a GitHub repo tree (optionally a subpath), streams one image at a time
    URL_LIST,   // user-supplied list of direct image URLs
    JSON_API    // any JSON endpoint; image URL extracted via a configurable field path
}
