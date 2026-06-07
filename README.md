# WallpaperD (Android)

Android port of the GNOME `gnome-wallpaper-changer` extension. Pluggable wallpaper sources,
timer-based rotation, cache that clears as images are consumed. Built to sit near-zero on the
battery between changes.

## Stack
- Kotlin, Jetpack Compose, Material 3 (Material You dynamic color)
- WorkManager for scheduling
- DataStore (settings), Room (cache index)
- OkHttp + kotlinx.serialization (network/JSON)
- minSdk 26, targetSdk 35

## Build
Open the folder in Android Studio (Ladybug+) and let it sync, or from a machine with the
Android SDK:

```bash
gradle wrapper          # generate the wrapper if ./gradlew is missing
./gradlew assembleDebug  # APK at app/build/outputs/apk/debug/
```

This package ships without the Gradle wrapper jar (binary). Android Studio adds it on import;
the `gradle wrapper` command does the same from CLI.

## Sources (= GNOME providers + more)
| Type | Behaviour |
|------|-----------|
| FOLDER | Random image from a SAF-picked folder. No download, no deletion. (GNOME folder provider) |
| WALLHAVEN | wallhaven.cc REST API. Fetches a page, caches it, applies one at a time, deletes each after use, refetches when empty. (GNOME wallhaven provider) |
| UNSPLASH | unsplash.com random photo. Needs a free access key. Pings the download endpoint for photographer credit. Deleted after use. |
| NASA_APOD | NASA Astronomy Picture of the Day. Prefers the HD url; skips video days. `DEMO_KEY` works but is rate-limited. |
| URL_LIST | Random pick from your list of direct image URLs. Downloaded, deleted after use. |
| JSON_API | Any JSON endpoint. Image URL pulled via a dot path (e.g. `data.0.url`). Custom headers. Deleted after use. |


Add as many as you want from the + menu (each type is a one-tap preset). Each tick the worker
shuffles enabled sources and uses the first that yields an image.

## Wallhaven config
`categories`/`purity` are 3-bit strings. Defaults `100`/`100` = General / SFW (matches GNOME
default of SFW General 16x9). NSFW (`purity` last bit) requires an API key.

## History
Every applied wallpaper is copied into `filesDir/history/` and shown in the History tab. Tap a
tile to re-apply it instantly (no fetch), star it to favourite, or delete it. The list keeps the
newest 60 non-favourites; favourites are never pruned. History lives separately from the download
cache, so "Delete cache" never touches it.

## Cache
Downloaded source files live in `filesDir/wallpapers/`. Settings -> Delete cache wipes that
directory and the Room index. History and favourites are untouched.

## Battery model (the important part)
- No foreground service. No wakelock. No alarm holding the CPU.
- One `PeriodicWorkRequest`. The OS batches it into Doze maintenance windows. The process is
  dead between runs; it wakes, applies one wallpaper, exits.
- WorkManager's floor is 15 min, so shorter intervals are clamped. Exact firing drifts by
  design. That drift is what buys the battery savings.
- Constraints: network is required only when an enabled source is online (a folder-only setup
  asks for no network); optional Wi-Fi-only and charging-only gates.
- Periodic work survives reboot (WorkManager restores it). No boot receiver needed.

If you ever need sub-15-min or to-the-second firing, the only Android-sanctioned path is
`AlarmManager.setExactAndAllowWhileIdle`, which costs battery and is rate-limited under Doze.
Not wired up here on purpose.

## License
MIT, same as the upstream extension.

## Building the APK

### Local (Ubuntu, CLI)
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

# Gradle 8.11.1 (apt's is too old) to bootstrap the wrapper, then build:
curl -s "https://get.sdkman.io" | bash && source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 8.11.1
echo "sdk.dir=$ANDROID_HOME" > local.properties
gradle wrapper
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Online (GitHub Actions)
Push the repo. `.github/workflows/build.yml` builds the debug APK on every push and uploads it
as the `app-debug-apk` artifact. No local SDK needed. Also runnable from Actions -> Run workflow.

### Android Studio
Open the folder, let it sync (it fetches the SDK and generates the Gradle wrapper), then
Build -> Build APK(s).

Notes:
- The debug APK is signed with the auto-generated debug key and installs directly. For a release
  build you'd add a keystore and a `signingConfigs` block in `app/build.gradle.kts`.
- This project deliberately omits the Gradle wrapper jar (binary). `gradle wrapper` or Android
  Studio regenerates it.

## Signing & distribution

### Running for yourself
The debug APK is auto-signed with the debug keystore and installs on any device (minSdk 26).
Nothing to set up.

### Self-distribution (GitHub Releases / your site)
1. Create a keystore once and back it up. Android will not update an app signed with a different
   key, so losing it means users must reinstall.
   ```bash
   keytool -genkey -v -keystore wallpaperd.jks -alias wallpaperd \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. `cp keystore.properties.template keystore.properties` and fill in the four values.
   `keystore.properties` and `*.jks` are gitignored.
3. Build a signed release:
   ```bash
   ./gradlew assembleRelease
   # -> app/build/outputs/apk/release/app-release.apk  (signed)
   ```
   With no `keystore.properties`, the release build is left unsigned (the F-Droid case).

### F-Droid
You submit source + metadata; F-Droid builds and signs. Steps:
1. Push to a public Git repo with the MIT LICENSE present.
2. Tag the release commit: `git tag v1.0.0 && git push --tags`.
3. Submit via the fdroiddata Submission Queue (reviewer does the rest) or open a merge request
   adding `metadata/org.piarsenal.wallpaperd.yml` (sample in `fdroid/`). Edit the Repo URL.
4. Bump `versionCode` (and tag) for each update.

The `dependenciesInfo { includeInApk/Bundle = false }` block is already set for reproducible
builds. To keep one signature across F-Droid + GitHub: publish your signed APK at a stable URL,
run `fdroid signatures <apk>`, and add `AllowedAPKSigningKeys` to the metadata so F-Droid verifies
its build matches yours and publishes your signature instead of its own.

Expect a `NonFreeNet` anti-feature label because Wallhaven/Unsplash/NASA/GitHub are third-party
network services. The folder source works fully offline.

## Diagnostics
If a source misbehaves, Settings -> Diagnostics log shows the exact reason (HTTP code, parse
error, download failure, or "0 results for this filter"). Failures also appear in the
"Couldn't change wallpaper" notification. Use this to debug source configs.

## Roadmap (planned, not yet implemented)
Tracked for future iterations:
- Fit-to-screen resize + reject wrong-orientation images (landscape/portrait toggle).
- Quick Settings tile to change wallpaper from the notification shade.
- More sources: GitLab repos, DeviantArt, Alphacoders, and a "text file of image links" reader.
- Source-addition UI polish.

## Privacy & security summary
- No accounts, no ads, no analytics, no tracking. All data stays on device.
- The app only contacts the source you configure (e.g. Wallhaven) to fetch images.
- API keys you enter are stored locally and excluded from cloud backups.
- HTTPS-only (cleartext traffic disabled). Minimal permissions: internet, set wallpaper, notifications.
- The Local folder source needs no internet.

## Translations
UI strings live in `app/src/main/res/values/strings.xml`. To add a language, copy it to
`values-<code>/strings.xml` (e.g. `values-fr`) and translate; missing keys fall back to English.
A Hindi stub is in `values-hi/`. Add the locale to `res/xml/locales_config.xml` so it appears in
Android's per-app language picker. (Home, Settings, Help, and navigation are already externalised;
the source editor still has inline English to migrate.)
