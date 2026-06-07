package org.piarsenal.wallchanger.net

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Shared OkHttp client. Lazily created so it costs nothing until a fetch actually runs. */
object Http {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // Some image hosts reject requests without a UA.
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "WallpaperChanger/1.0 (Android)")
                    .build()
                chain.proceed(req)
            }
            .build()
    }
}
