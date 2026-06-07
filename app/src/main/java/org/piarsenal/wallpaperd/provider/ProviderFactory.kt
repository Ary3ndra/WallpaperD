package org.piarsenal.wallpaperd.provider

import org.piarsenal.wallpaperd.data.SourceConfig
import org.piarsenal.wallpaperd.data.SourceType

object ProviderFactory {
    fun create(env: ProviderEnv, cfg: SourceConfig): WallpaperProvider = when (cfg.type) {
        SourceType.FOLDER -> FolderProvider(env, cfg)
        SourceType.WALLHAVEN -> WallhavenProvider(env, cfg)
        SourceType.UNSPLASH -> UnsplashProvider(env, cfg)
        SourceType.NASA_APOD -> NasaApodProvider(env, cfg)
        SourceType.GITHUB -> GithubProvider(env, cfg)
        SourceType.URL_LIST -> UrlListProvider(env, cfg)
        SourceType.JSON_API -> JsonApiProvider(env, cfg)
    }
}
