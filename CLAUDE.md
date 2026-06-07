# CLAUDE.md — project memory for Claude Code

This file is read automatically each session. It captures the state, decisions, and roadmap so
you can continue without re-explanation. Keep it updated as the project evolves.

## What this is
An Android wallpaper-changer app, ported in spirit from the GNOME `gnome-wallpaper-changer`
extension. Rotates the system wallpaper on a timer from pluggable sources, with a strong emphasis
on battery efficiency, user privacy, and being fully offline-capable (local folder source).
Intended to be open-sourced (MIT) and shipped on F-Droid eventually.

## Tech stack (pinned)
- Kotlin, Jetpack Compose, Material 3 (dynamic color)
- AGP 8.7.2, Kotlin 2.0.21, Gradle 8.11.1, KSP
- minSdk 26, compileSdk/targetSdk 35, JDK 17 (AGP 8.7 requires exactly 17)
- WorkManager (scheduling), DataStore (settings), Room (cache index + history)
- OkHttp + kotlinx.serialization (network/JSON), Coil (image previews), DocumentFile (SAF)
- Package root: `org.piarsenal.wallpaperd`

## Build / run / debug
```bash
# JDK must be 17. If `java -version` shows 21, pin it:
#   echo "org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64" >> gradle.properties
gradle wrapper            # one-time: repo ships WITHOUT the wrapper jar (binary)
./gradlew assembleDebug   # -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
CI: `.github/workflows/build.yml` builds the debug APK as an artifact on push.

## Architecture (data flow)
- **Settings**: `data/SettingsRepository` stores one JSON `AppSettings` blob in DataStore.
  `AppSettings.sources: List<SourceConfig>` — users add unlimited sources.
- **Providers** (`provider/`): one interface `WallpaperProvider.next(): ProvidedImage?`.
  Implementations: Folder (SAF, recursive optional, exclude-names), Wallhaven, Unsplash, NASA APOD,
  GitHub (by repo URL), URL list, JSON API. `ProviderFactory` maps `SourceType` -> provider.
  `ProvidedImage(key, label, openStream, onConsumed)` — `onConsumed` deletes online temp files.
- **Engine** (`wallpaper/WallpaperEngine.changeOnce()`): the single change pipeline. Prefers a
  prefetched image, else tries enabled sources in random order until one applies. Records history,
  updates `lastAppliedKey`, posts notifications, enforces cache limit. Logs every attempt.
- **Prefetcher** (`wallpaper/Prefetcher`): downloads the NEXT wallpaper into a slot so the next
  change is instant. Called after a successful change (worker awaits it; manual triggers on app scope).
- **Controller** (`wallpaper/WallpaperController`): process-scoped owner of manual change + reapply,
  exposes `isChanging` StateFlow. Survives screen changes/backgrounding (NOT process death — see Help).
- **Setter** (`wallpaper/WallpaperSetter`): applies via `WallpaperManager.setStream` (fast, no manual decode).
- **Scheduling** (`work/Scheduler` + `ChangeWallpaperWorker`): periodic WorkManager job. Min interval
  15 min (clamped). Network constraint only when an online source is enabled. No foreground service,
  no wakelock — this is the battery model. Survives reboot via WorkManager.
- **History** (`data/HistoryManager` + Room `AppliedWallpaper`): copies each applied image to
  `filesDir/history/`, keeps newest 60 non-favourites, favourites never pruned. Separate from cache.
- **Cache** (`data/CacheManager`): `filesDir/wallpapers/` (source temp) + `filesDir/prefetch/`.
  `enforceLimit()` prunes oldest past `cacheLimitMb`. `clear()` wipes both + index + prefetch slot.
- **Logging** (`log/Logger`): append-only file at `filesDir/logs/app.log`, capped 256KB, mirrored to
  logcat. Surfaced in Settings -> Diagnostics. Providers/engine log exact failure reasons.
- **UI** (`ui/`): single-activity Compose. Bottom nav: Home, Sources, History, Settings, Help.
  `WallpaperViewModel` delegates manual change/reapply to `WallpaperController`.

## Conventions
- Material 3 only. Avoid over-formatting. Keep providers free of singletons (pass `ProviderEnv`).
- New user-facing strings go in `res/values/strings.xml` and are referenced via `stringResource`.
  Home/Settings/Help/Nav are externalised; SourceEditorDialog still has inline English to migrate.
- New source type = add to `SourceType`, `SourceConfig` fields, a provider, `ProviderFactory`,
  the editor `when` (must stay exhaustive), and `presetName` in `SourcesScreen`.
- All network is HTTPS (`usesCleartextTraffic=false`). Log failures, never throw to a silent catch.

## Privacy / security (must uphold)
- No accounts, ads, analytics, or tracking. Data stays on device.
- API keys live in DataStore and are EXCLUDED from cloud backup (see `res/xml/*_rules.xml`).
- Minimal permissions: INTERNET, ACCESS_NETWORK_STATE, SET_WALLPAPER, POST_NOTIFICATIONS.

## Gotchas
- Gradle wrapper jar is intentionally absent (binary). Run `gradle wrapper` or open in Android Studio.
- `FlowRow` is experimental here — opt-in flag is set in `app/build.gradle.kts`
  (`-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi`). Keep it.
- Room uses `fallbackToDestructiveMigration()` (v1->v2 added history). Write a real Migration before
  any schema change that must preserve user data post-release.
- Release signing is conditional on `keystore.properties` (absent = unsigned, for F-Droid). Never commit it.

## Status — implemented
Sources (folder+recursive+exclude, wallhaven, unsplash w/ quality, nasa w/ quality, github-by-URL,
url-list, json-api); timer changes; battery-safe WorkManager; prefetch; history + favourites + reapply;
cache cap (MB/GB) + clear; avoid-repeats; per-source quality (unsplash/nasa); silent progress
notifications + failure reason; in-app diagnostics log; Help screen (plain English); i18n scaffolding
(+ Hindi stub + locale picker); privacy hardening; release-signing + reproducible-build config; CI.

## Roadmap — TODO (not yet done)
1. **#1 Wallhaven** — reported "not working". Repro: add source, Change now, read Settings->Diagnostics.
   Likely zero-results filter, 429 rate limit, or network. Fix root cause once the log line is known.
2. **#5/#6/#11** Fit-to-screen resize (checkbox) + reject wrong-orientation images
   (any/landscape/portrait). Decode bounds in `WallpaperSetter`; center-crop/scale to wallpaper dims.
3. **#18** Quick Settings tile (`TileService`) to trigger a change from the shade.
4. **#9** "Text file of image links" reader (GitHub raw .txt of URLs; skip dead/login-gated links).
5. **#13** GitLab provider (repository tree API). **#14** DeviantArt. **#10** Alphacoders (needs testing).
6. **#16/#17** Source-addition UI polish.
7. Migrate remaining inline strings (SourceEditorDialog, SourcesScreen, HistoryScreen) to strings.xml.

## Notes
- Wallhaven categories/purity are 3-bit strings ("100"/"100" = General/SFW). NSFW needs an API key.
- GitHub source: paste a repo URL or a `.../tree/branch/path` link; branch/path fields override.
