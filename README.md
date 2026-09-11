# Kishi Switch for Guild Wars

English | [Français](README.fr.md)

A personal Android 16 controller utility built with Kotlin and Android Views. It uses Shizuku, installed separately, without root or replacing your keyboard.

**Version 0.1.4 provides English and French documentation and bilingual attribution explanations inside the APK.** The app interface remains in French; the English user guide includes the corresponding French button labels.

**Controller passthrough and both inversions were confirmed by the user in Guild Wars on a OnePlus 11 with version 0.1.2.** Version 0.1.3 added licensing and attribution. Guided tests are still required on a new installation or after a configuration change. See the [validation report](docs/VALIDATION.md) and the [technical investigation (French)](docs/DIAGNOSTIC-TRANSMISSION.md).

## Install and use

Download the signed APK from [Releases](https://github.com/Maugrey/kishiswitch4gw/releases). This project is currently a prerelease.

Read the [English user guide](docs/NOTICE.md) or the [French user guide](docs/NOTICE.fr.md), then check the [validation report and remaining tests](docs/VALIDATION.md).

The app swaps A ↔ X and B ↔ Y while LT or RT is held, and independently inverts the **right stick's vertical axis**. Face buttons keep their usual behavior when no trigger is held. Both preferences start enabled, but each feature only becomes active after its guided test is confirmed. The game package `net.arena.guildwars.reforged` and axes `AXIS_LTRIGGER`, `AXIS_RTRIGGER` and `AXIS_RZ` were confirmed on the OnePlus. The relay currently supports the Kishi V2 Pro hardware profile 1532:0717. Use the floating controls; M2 is unavailable in this version.

## Build on Windows

Pinned versions: Gradle Wrapper 8.13 (download verified with SHA-256), AGP 8.13.2, Kotlin 2.2.21, Android Studio's JDK 21, SDK 36 and Build Tools 36.1.0. `minSdk`, `compileSdk` and `targetSdk` are all 36.

```powershell
# Debug APK, engine tests and lint
powershell -ExecutionPolicy Bypass -File ./scripts/build.ps1 -Check

# Personally signed release APK and checks
powershell -ExecutionPolicy Bypass -File ./scripts/build.ps1 -Release -Check

# From a Git clone with all tracked changes committed: collect distribution files
powershell -ExecutionPolicy Bypass -File ./scripts/package.ps1

# Android tests: use a disposable emulator; these tests clear app preferences
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
$env:ANDROID_SERIAL = 'emulator-5580'
./gradlew.bat :app:connectedDebugAndroidTest
```

The build script accepts `-Jdk` and `-Sdk` when paths differ. Android Studio can also open this directory directly. On other platforms, set `ANDROID_HOME` or provide `sdk.dir=...` in `local.properties`, use Java 21 and run `./gradlew :engine:test :app:assembleDebug :app:lintDebug`.

The first signed build creates a signing key and its properties **outside the repository**, in `%LOCALAPPDATA%/KishiSwitch/signing`, with access limited to the Windows user and SYSTEM. Back up that directory securely: you need the same key to update your build without uninstalling the app. Passwords are neither printed nor included in the sources. To use an existing key, set `KISHI_SIGNING_PROPERTIES` to the private file containing `storeFile`, `storePassword`, `keyAlias` and `keyPassword`.

Gradle outputs: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release.apk`. Distribution files are placed in `dist/`. Dependencies are pinned; APK bytes may still vary with the build environment.

The APK automatically includes the root `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md` and `licenses/`. Source archives come from the current Git commit and exclude untracked local files. `package.ps1` requires a Git clone; building is also possible from a source archive.

## License and attribution

Kishi Switch's own code and documentation are available under **[PolyForm Noncommercial 1.0.0](LICENSE)**. Modification and redistribution are allowed for the uses permitted by that license, without a requirement to publish modified source code. See the full license for its terms, including uses expressly permitted for certain organizations.

When redistributing the software, retain these lines from [NOTICE](NOTICE) and provide the license text or its URL:

```text
Required Notice: Kishi Switch - Copyright (c) 2026 Maugrey.
Required Notice: Original source: https://github.com/Maugrey/kishiswitch4gw
```

This project makes its source available for noncommercial use; it is not described as open source under the OSI definition. Third-party libraries retain their own licenses, documented in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). In the app, open **Licences et attribution** to read the texts and access the repository. Official license texts remain in their original language; explanatory notices are provided in English and French.

## Project layout

- `engine/`: Android-independent state machine and HID encoding, analog trigger hysteresis, mappings retained until button release, and setting changes deferred until controls return to rest.
- `app/.../input/`: accessibility for foreground, keyboard and lock detection, plus the overlay; no interception of controller events.
- `app/.../bridge/`: internal AIDL, Shizuku UserService, exclusive reading of the Kishi and forwarding to a UHID controller; caller checks and automatic shutdown on connection loss.
- `app/.../data/`: preferences, hardware profile and validations tied to the profile, threshold, game version and system build.
- `app/.../ui/`: main screen, guided identification, temporary tests and an overlay that does not take keyboard focus.

The accessibility service checks the application window's package and whether the keyboard is visible, without reading typed text. There is no Internet permission, server, account, telemetry, input method service or app notification. The bounded local log only records diagnostic steps; sharing it requires an explicit action in the interface.

## Technical limitations

Accessibility capture followed by InputManager reinjection failed in Guild Wars. The replacement relay reads the identified Linux input device and creates a UHID controller recognized by Android. It depends on the device's shell permissions and this Kishi's hardware format. System updates may require a new validation.

UHID uses Android's normal input dispatch and cannot target a UID per event. The accessibility service stops the relay when the foreground app changes, the keyboard appears or the phone locks. The Kishi is also released on error, client death or control connection timeout. Guided tests expire after three minutes. Global XYAB swapping is never used as a fallback.

References: [onMotionEvent and source interception](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onMotionEvent(android.view.MotionEvent)), [Android 16 InputDispatcher](https://android.googlesource.com/platform/frameworks/native/+/refs/heads/android16-release/services/inputflinger/dispatcher/InputDispatcher.cpp), [Shizuku API](https://github.com/RikkaApps/Shizuku-API), [AGP compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes).
