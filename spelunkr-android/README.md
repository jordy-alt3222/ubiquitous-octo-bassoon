# SPELUNKR for Android (phone + Android TV)

A native Android rebuild of [spelunkr.tv](https://spelunkr.tv/): "24/7 synchronized channels of forgotten
Internet Archive video. Like cable TV, but weirder." One APK installs on phones and tablets (touch) and on
Android TV / Google TV (remote and D-pad), and shows up in both launchers.

> **Unofficial.** This is not made by or affiliated with Spelunkr. On 29 October 2026 Spelunkr announced its
> own Android TV / Google TV / Fire TV app at spelunkr.tv/app. Read [Before you ship this](#before-you-ship-this).

## What it does (same as the site)

| Site | App |
|---|---|
| Landing page: little TV playing a random channel with the sound off, stats (channels, 24/7 live sync, hours), **TUNE IN** / "or press any key" | `HomeScreen` |
| **TV Guide**: preview + CH ▲▼, a Now Playing panel (title, year, description, elapsed/remaining bar, Up Next), **WATCH** / **VIEW CHANNEL**, and a listings grid with channel number, pixel logo and name | `GuideScreen` |
| **Watch**: full-screen picture, channel banner when you tune, GUIDE, PAUSE/PLAY, 15s back and forward (never past live), **SPELUNK** (random channel), LIVE badge | `WatchScreen` |
| Typed channel numbers, channel up/down, last channel | `MainActivity.dispatchKeyEvent`, `SpelunkrViewModel` |
| "ONE MOMENT PLEASE / TUNING…", "NO SIGNAL", "ADJUSTING THE ANTENNA…" | `TuningSlate`, `NoSignal`, guide rows |
| **STILL WATCHING?** after four hours with no button pressed | `StillWatching` |
| **DIRECTORY** and channel pages (description, tapes, hours, schedule) | `DirectoryScreen`, `ChannelPage` |

### How "live" works

The site has no streaming server. It publishes schedule files, and every viewer works out from the wall
clock which tape is on and how far into it they are. The app reads the same files:

```
lineup.json                    channels, dial numbers, colours, logos, schedule versions
manifests/<channel>/v<n>.json  the tapes in order: archive.org id, file, runtime, trim points
details/<channel>/v<n>.json    one description per tape
```

These come from the site's public storage bucket, or from `https://spelunkr.tv/broadcast/` if that fails.
Neither needs a key. A version's first cycle starts at `epochMs` with tape `startIndex`, then the whole list
repeats back to back. A newer version takes over at its `effectiveAtMs`. That logic is in
`schedule/Schedule.kt`, with unit tests.

Video plays straight from `https://archive.org/download/<id>/<file>`, clipped to the aired part (`inS`/`outS`),
starting at the right second. The next tape is always queued. If buffering leaves playback more than 8 s
behind where you should be, it catches up. If the device clock is more than 3 s off, the app corrects it
using the server's `Date` header.

## Phone vs TV: one codebase

Everything is shared: data, schedule, player, view model, and every screen. The differences are small and
explicit:

- **Detection:** `LocalIsTv` is true when the device has `FEATURE_LEANBACK` or is in TV UI mode.
- **Layout:** chosen by available width, not device type. At 720dp or wider (TV, landscape tablet) the Guide
  uses the site's desktop layout: preview and info side by side above a 3-hour grid. On a phone in portrait
  it stacks them, with a 90-minute grid.
- **Guide input:**
  - TV: each channel row is one focus stop. Moving focus tunes the preview after 0.6 s, and OK goes full screen.
  - Phone: tap a row to tune it, tap it again (or WATCH) to go full screen.
- **Watch input:**

  | Remote (controls hidden) | Action |
  |---|---|
  | ▲ / ▼, CH+ / CH−, Page Up/Down | Channel up / down |
  | ◀ / ▶, ⏪ / ⏩ | 15 s back / forward |
  | OK | Show the control bar (focus on PAUSE) |
  | 0–9 | Dial a channel (tunes after 1.5 s, or at 3 digits) |
  | Last channel key | Previous channel |
  | Play/Pause key | Pause / resume |
  | Guide key, Back | Guide |

  On phones, tap the picture to show or hide the same control bar. Watch switches to landscape full screen,
  and the other screens go back to normal.
- **Focus styling:** the site's yellow rim marks focus. All buttons are Compose `clickable`s, so a tap and the
  D-pad's centre button do the same thing.

## Build

Requirements: JDK 17 or newer, and the Android SDK with platform 36 (Android Studio installs both). The Gradle
wrapper downloads Gradle 9.8.2 itself.

```bash
cd spelunkr-android
# Point Gradle at the SDK (Android Studio writes this file for you):
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # schedule tests
```

Or open the `spelunkr-android` folder in Android Studio and press Run.

### Release APK

`./gradlew assembleRelease` builds a shrunk (R8) release APK of about 2 MB. To sign it, put these properties
in `~/.gradle/gradle.properties`, never in the repo:

```properties
spelunkr.keystore=/path/to/spelunkr-release.jks
spelunkr.keystore.password=…
spelunkr.key.alias=spelunkr
spelunkr.key.password=…
```

Without them the release APK comes out unsigned, and Android won't install it. To make a key:
`keytool -genkeypair -keystore spelunkr-release.jks -alias spelunkr -keyalg RSA -keysize 4096 -validity 10000`.
Keep the keystore. Every later version must be signed with the same key, or it can't be installed over the
earlier one.

Stack: Kotlin 2.4, AGP 9.5 (built-in Kotlin), Jetpack Compose (BOM 2026.09), Media3 ExoPlayer 1.11,
kotlinx.serialization, lifecycle ViewModel / StateFlow. `minSdk` 23, `targetSdk` 36. The UI uses plain
Compose foundation and Material 3 text, not `androidx.tv` components, so the same composables run on both form
factors.

## Run on a phone

1. Turn on Developer options and USB debugging on the phone, then plug it in.
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or press Run in Android Studio.
3. Open **SPELUNKR** from the app drawer.

Phone emulator: Device Manager → any Pixel with an API 34+ image.

## Run on Android TV

**Real TV / Google TV / Chromecast with Google TV:**

1. Settings → System → About → click **Android TV OS build** 7 times to enable Developer options.
2. Developer options → turn on **USB debugging** (or **Network debugging** / **Wireless debugging**).
3. `adb connect <tv-ip>:5555` and accept the prompt on the TV. (Wireless debugging on Google TV: use the
   pairing port and code it shows, then `adb pair <ip>:<port>`.)
4. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
5. The app appears in the TV's Apps row with its banner. On Google TV, look under Apps → "Your apps".

**Emulator:**

```bash
sdkmanager "system-images;android-34;android-tv;x86" "emulator"
avdmanager create avd -n tv -k "system-images;android-34;android-tv;x86" -d tv_1080p
emulator -avd tv
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

(Or in Android Studio: Device Manager → Create device → TV → 1080p.) The emulator's extended controls include
a D-pad. Arrow keys, Enter and Esc on your keyboard also work as D-pad, OK and Back.

Fire TV boxes run the same APK (Fire OS is Android). Install it with `adb` the same way.

## Before you ship this

- **Branding and permission.** The name SPELUNKR, its look, its copy and its channel identities belong to the
  Spelunkr project, and Spelunkr has its own official TV app. The `applicationId` is
  `com.example.spelunkrtv` on purpose. Don't publish this to Google Play or elsewhere under that name or brand
  without the operator's permission. They are reachable through the Discord linked on the site.
- **The data feed is not a public API.** The app reads the site's own schedule files (a Supabase storage
  bucket and the `/broadcast/` snapshot). Nothing documents them, and the format (`engine: 5`) can change
  without notice and break the app. Every install also adds traffic to their hosting. Get the operator's OK
  before giving this to anyone. The site also embeds a Supabase "anon" key for accounts and comments; this
  app does not use it.
- **Internet Archive content and terms.** Every tape streams from archive.org, which the app does not host,
  under the [Internet Archive's Terms of Use](https://archive.org/about/terms.php). Many of these recordings are
  off-air TV captures uploaded by users, and their copyright status varies from item to item. Like the site,
  the app credits the Archive ("ALL PROGRAMMING COURTESY OF THE INTERNET ARCHIVE"). The Archive is sometimes
  slow, and tapes then take a while to start.
- **Fonts.** The site uses "Star4000" faces, fan recreations of the 1990s WeatherStar typefaces, whose license
  is unclear. The app does not bundle them and uses the platform monospace in bold instead.

## Not replicated (and why)

- **Breaks between shows** (text bumpers over music, "Up next" cards, vintage commercials). The site
  interleaves these with a seeded engine that is internal to its JavaScript and undocumented. The app plays
  the tapes back to back. **On channels with breaks, the app runs ahead of the website.** App viewers are in
  sync with each other, but not with spelunkr.tv. When checked on 29 Oct 2026, RADIO-ACTIVE TV matched the site
  to within one second, while LATCHKEY-TV (which has commercial breaks) was about 24 minutes ahead, on the same
  tape. Matching the site exactly would mean reimplementing that engine. Ask the operator for its spec, or for
  a precomputed "what's on" endpoint.
- **Bumper music** (CC BY tracks from `audio.spelunkr.tv`): goes with the breaks above. If added, the
  per-track attribution in the manifests must be shown.
- **Accounts, comments, reactions, watcher counts, clips, reporting a broken tape.** These need the site's
  Discord sign-in and its Supabase backend.
- **Chromecast** from the site: on Android TV the app *is* the TV. Phone-to-TV casting would need a registered
  Cast receiver.
- **Machine-generated subtitles**, the **music overlay** on VACATION SLIDES, the **About / Updates** pages,
  the Discord / Ko-fi / donate links (TVs have no browser), the hidden MODERN skin, and the held dial numbers
  1 (LEAN BACK) and 601 (WINDOW SEAT).
- **Pixel logos** are drawn from the lineup's bitmaps, but the letter-to-colour mapping is the app's own
  approximation of the site's.
