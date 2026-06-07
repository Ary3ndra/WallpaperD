# WallpaperD

[![Build status](https://github.com/Ary3ndra/WallpaperD/actions/workflows/build.yml/badge.svg)](https://github.com/Ary3ndra/WallpaperD/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/Ary3ndra/WallpaperD?label=release)](https://github.com/Ary3ndra/WallpaperD/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[![Download latest APK](https://img.shields.io/github/v/release/Ary3ndra/WallpaperD?style=for-the-badge&label=Download%20APK&color=4CAF50)](https://github.com/Ary3ndra/WallpaperD/releases/latest)

WallpaperD is an Android app that automatically changes your wallpaper on a schedule —
picking images from a local folder, or fetching them from sites like Wallhaven, Unsplash,
or NASA's Astronomy Picture of the Day. It runs quietly in the background, barely touches
your battery, and keeps everything on your device: no accounts, no ads, no tracking.

It's a spiritual port of the GNOME `gnome-wallpaper-changer` extension to Android.

## What it can do

- **Pick from many sources at once** — add a local folder, an online gallery, or a list of
  image URLs, and WallpaperD rotates between whichever ones you enable.
- **Change wallpapers on a timer** — set how often (minimum 15 minutes) and let it run.
- **Remember your favourites** — every wallpaper it applies is saved to a History tab where
  you can re-apply, star, or delete it later.
- **Stay light on battery** — no background service running constantly, no wake locks; it
  uses Android's built-in task scheduler so the OS decides the most efficient time to run.
- **Work fully offline** — the "local folder" source needs no internet connection at all.
- **Respect your privacy** — nothing leaves your device except requests to the image source
  *you* choose to enable.

## Where to get it

- **Download a release APK**: check the [Releases](../../releases) page for the latest signed
  build, or grab the latest debug build from the [Actions](../../actions) tab (build artifacts).
- **Build it yourself**: see [Building from source](#building-from-source) below.

WallpaperD requires **Android 8.0 (API 26)** or newer.

## Available wallpaper sources

| Source | What it does |
|---|---|
| **Local folder** | Picks a random image from a folder on your device. Nothing is downloaded or deleted — fully offline. |
| **Wallhaven** | Pulls from wallhaven.cc using your search filters (query, category, purity, sorting). |
| **Unsplash** | Random photos from unsplash.com. Needs a free personal access key from Unsplash. |
| **NASA APOD** | NASA's daily Astronomy Picture of the Day. Works out of the box (rate-limited demo key), or use your own NASA API key for smoother access. |
| **URL list** | You provide a list of direct image links; WallpaperD picks one at random each time. |
| **JSON API** | Point it at any JSON endpoint that returns image URLs — useful for custom or self-hosted galleries. |
| **GitHub repo** | Paste a link to a GitHub repo (or a folder within one) and it'll rotate through the images there. |

You can add as many sources as you like from the **+** button on the Sources screen — each
time the wallpaper changes, WallpaperD shuffles your enabled sources and uses the first one
that successfully returns an image.

## How it stays battery-friendly

This is a core design goal, so it's worth explaining plainly:

- WallpaperD doesn't run continuously in the background. It schedules a small periodic task
  with Android's WorkManager, which the OS bundles together with other apps' scheduled work
  to minimise how often your device has to wake up.
- The shortest interval is 15 minutes — Android itself enforces this floor for battery
  reasons, and exact timing can drift by design (that drift is what saves power).
- It only asks for network access when you've enabled an online source — a folder-only setup
  needs no internet permission at all.
- The schedule survives reboots automatically; there's no need for the app to start at boot.

## Privacy, in short

- No accounts, ads, analytics, or tracking of any kind.
- All your settings, history, and cached images stay on your device.
- The app only talks to the network when fetching from a source *you've* enabled.
- Any API keys you enter (e.g. for Unsplash) are stored locally and excluded from cloud backups.
- All network traffic is HTTPS — plain HTTP is disabled at the OS level.

## Troubleshooting

If a source isn't working, open **Settings → Diagnostics** — it logs the exact reason
(network error, HTTP status code, "no results for this filter", etc.) so you can adjust your
source's settings accordingly. The same reason also shows up in the "Couldn't change
wallpaper" notification if one appears.

## Translations

Want WallpaperD in your language? UI text lives in
`app/src/main/res/values/strings.xml`. Copy that file to `values-<language-code>/strings.xml`
(e.g. `values-fr` for French) and translate the entries — anything you don't translate falls
back to English automatically. A Hindi translation already exists as a starting example in
`values-hi/`. Don't forget to add your language code to `res/xml/locales_config.xml` so it
shows up in Android's per-app language picker.

---

# Building from source

This section is for developers who want to build, modify, or contribute to WallpaperD.

## Tech stack
- Kotlin, Jetpack Compose, Material 3 (dynamic color)
- WorkManager (scheduling), DataStore (settings), Room (local database)
- OkHttp + kotlinx.serialization (networking), Coil (image loading)
- minSdk 26, targetSdk/compileSdk 35, JDK 17

## Quick start
Easiest path: open the project folder in **Android Studio** (Ladybug or newer), let it sync,
and use **Build → Build APK(s)**. Android Studio fetches the SDK and generates the Gradle
wrapper for you automatically.

From the command line, with the Android SDK and JDK 17 already installed:
```bash
gradle wrapper           # one-time: this repo ships without the wrapper jar (it's a binary)
./gradlew assembleDebug  # -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Setting up a machine from scratch (Ubuntu/Debian)
If you're starting with neither Android Studio nor the command-line SDK tools:
```bash
sudo apt install -y openjdk-17-jdk unzip
export ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
url=$(curl -s https://developer.android.com/studio | grep -o 'https://dl.google.com/android/repository/commandlinetools-linux-[0-9]*_latest.zip' | head -1)
curl -L -o /tmp/cmdline.zip "$url"
unzip -q /tmp/cmdline.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# Gradle 8.11.1 (the version this project is pinned to — apt's is too old)
curl -s "https://get.sdkman.io" | bash && source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 8.11.1
echo "sdk.dir=$ANDROID_HOME" > local.properties
gradle wrapper
./gradlew assembleDebug
```

### Building via GitHub Actions (no local setup at all)
Every push triggers `.github/workflows/build.yml`, which builds a debug APK and uploads it as
a downloadable artifact — see the **Actions** tab. You can also trigger it manually from
**Actions → build → Run workflow**.

> The debug APK from any of the above methods is signed with Android's auto-generated debug
> key — it installs and runs fine for personal use, but can't be published to an app store or
> updated over a differently-signed release build.

## Signing a release build

If you want to distribute WallpaperD (outside an app store that signs for you), you'll need
your own signing key:

1. **Generate a keystore once, and back it up somewhere safe** — if you lose it, you can never
   publish an update under the same app identity; users would have to uninstall and reinstall.
   ```bash
   keytool -genkey -v -keystore wallpaperd.jks -alias wallpaperd \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Copy the template and fill in your details:
   ```bash
   cp keystore.properties.template keystore.properties
   ```
   Both `keystore.properties` and `*.jks` files are gitignored — they will never be committed.
3. Build the signed release APK:
   ```bash
   ./gradlew assembleRelease
   # -> app/build/outputs/apk/release/app-release.apk
   ```
   Without a `keystore.properties`, the release build comes out unsigned (this is the expected
   setup for F-Droid, which signs with its own key).

### Automated signed releases (GitHub Actions)
`.github/workflows/release.yml` builds and signs a release APK whenever you push a tag like
`v1.0.0`, then attaches it to a GitHub Release. It needs four repository secrets configured at
**Settings → Secrets and variables → Actions**:

| Secret | What goes in it |
|---|---|
| `KEYSTORE_B64` | Your `.jks` keystore file, base64-encoded (`base64 -w0 your.jks`) |
| `KEYSTORE_PASSWORD` | The keystore's password |
| `KEY_ALIAS` | The alias you chose when generating the key |
| `KEY_PASSWORD` | The password for that specific key |

### Submitting to F-Droid
1. Make sure the repo is public with the MIT `LICENSE` present.
2. Tag your release commit, e.g. `git tag v1.0.0 && git push --tags`.
3. Submit via F-Droid's [Submission Queue](https://f-droid.org/), or open a merge request to
   `fdroiddata` adding a metadata file (a starting template lives in `fdroid/`). Update the
   `Repo` URL to point at your repository.
4. Bump `versionCode` (and create a new tag) for every future update.

Expect F-Droid to label the app with a `NonFreeNet` anti-feature, since several sources
(Wallhaven, Unsplash, NASA, GitHub) talk to third-party services. The local-folder source needs
no network at all.

## Project structure, for contributors
- **Sources** live in `provider/` — each one implements a single `next(): ProvidedImage?`
  method. Adding a new source type means: add it to `SourceType`/`SourceConfig`, write a
  provider, register it in `ProviderFactory`, and add it to the source editor UI.
- **The wallpaper-change pipeline** lives in `wallpaper/WallpaperEngine` — it tries enabled
  sources in random order, applies the first successful image, records history, and logs
  every step.
- **Scheduling** is handled by WorkManager (`work/Scheduler` + `ChangeWallpaperWorker`).
- **Diagnostics**: every failure is logged to an in-app file viewable from
  Settings → Diagnostics, so issues can be debugged without a debugger attached.

See `CLAUDE.md` for a fuller architectural breakdown and the current roadmap.

## License
MIT — see [LICENSE](LICENSE). Same license as the upstream GNOME extension this was inspired by.
