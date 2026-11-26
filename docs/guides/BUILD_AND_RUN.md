# Build, install, and verify Saigai

Status: current
Owner: Project Lead (Saigai)
Verified: 2026-11-26 · M1 commit · `assembleDebug` + `lintDebug` executed on JDK 17.0.21.1, SDK 35, Gradle 8.11.1 (wrapper); device checks below remain unexecuted
Scope: How to produce a debug APK from this tree and what to check on the device. Milestone gates live in the design doc.

## Prerequisites

| Tool | Version used by the build files |
| --- | --- |
| JDK | 17 (AGP 8.7.3 rejects JDK 8 and is untested on JDK 25) |
| Android SDK | Platform 35, Build-Tools 35.x |
| Gradle | 8.11.1 |
| Android Studio | Ladybug (2024.2.1) or newer, if you prefer the IDE |

Set `ANDROID_HOME` (or `sdk.dir` in `local.properties`) before building.

## Build

The Gradle 8.11.1 wrapper is committed. Fetch the hand model once, then build:

```bash
python scripts/fetch_hand_model.py    # 7.8 MB, skipped if already present
./build-debug.sh                      # Windows: build-debug.bat
```

The wrapper scripts exist because this machine's default `java` is JDK 25, which
Gradle 8.11.1 cannot run on; they pin JDK 17 for the build only. A plain
`./gradlew :app:assembleDebug` works too if `JAVA_HOME` already points at JDK 17.

MediaPipe bundles OpenCV, so the build produces **per-ABI APKs — there is no
universal `app-debug.apk`**:

| APK | Size | Use |
| --- | --- | --- |
| `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` | ~53 MB | physical phone (the demo build) |
| `app/build/outputs/apk/debug/app-x86_64-debug.apk` | ~86 MB | x86_64 emulator |

If `app/src/main/assets/hand_landmarker.task` is missing the app still builds and
runs; the camera works and the Hand tracking panel reports `MODEL_MISSING`. It does
not crash.

### `./gradlew clean` may fail on Windows

A lint worker can hold `app/build/intermediates/lint-cache/*.jar` open, so `clean`
fails with `Unable to delete directory`. Fix: `./gradlew --stop` first, then clean.

### Toolchain this build was verified with

| Component | Version |
| --- | --- |
| JDK | Temurin 17.0.21.1+2 (`JAVA_HOME`) |
| Android SDK | platform 35, build-tools 35.0.0, platform-tools 37.0.3 |
| Gradle | 8.11.1 (wrapper, `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties`) |
| AGP / Kotlin / Compose BOM | 8.7.3 / 2.0.21 / 2024.10.01 |

`./gradlew` uses whatever Java is on `PATH`; point `JAVA_HOME` at JDK 17 (AGP 8.7.3 does not support JDK 8, and the build is untested on JDK 21+).

### Lint

```bash
./gradlew :app:lintDebug
```

Expected: 0 errors. Two `GradleDependency` warnings are accepted on purpose — the pinned `androidx.core` / `androidx.activity` versions are the ones this build was verified against.

```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

Check what the APK actually asks for before trusting it:

```bash
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump badging app/build/outputs/apk/debug/app-arm64-v8a-debug.apk | grep uses-permission
# expected: CAMERA and the generated DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, nothing else
```

## Static checks (no Android SDK needed)

```bash
python scripts/check_m1.py
```

Covers: phrase catalog shape (20 phrases, kebab-case unique ids, all four categories, Tamil in `ta` and no Tamil in `en`), English/Tamil string-key parity, Tamil-character presence in every `values-ta` string, every `R.string.*` reference resolving, `CAMERA` declared, `INTERNET`/`ACCESS_NETWORK_STATE`/`AD_ID`/`RECORD_AUDIO` absent. Exit code 1 fails the check.

It also parses the **merged** manifests under `app/build/intermediates/merged_manifests/` after a build, because the source manifest cannot see permissions a dependency adds. MediaPipe's Firebase transport added `INTERNET` exactly that way; that regression is now guarded.

## Device checks

Run these on the baseline 4GB device and record the result in `CONTEXT.md`. These are the parts a static check cannot prove.

1. **Offline by construction.** Enable airplane mode, force-stop, relaunch. Quick Cards must speak. If anything shows a network error, the manifest has drifted.
2. **Tamil glyphs.** Open Quick Cards in தமிழ் mode and confirm all five geometries render (no tofu, no detached vowel signs): right-side (`கா கி கீ`), below (`கு டு று`), left-reordered (`கெ கே கை`), two-part (`கொ கோ கௌ`), pulli/conjunct (`க் க்ஷ ஸ்ரீ`). The catalog contains `ஸ்ரீ`-free text, so add a temporary card if you need the conjunct row.
3. **Tamil TTS voice.** Pick a device without the offline Tamil voice and confirm the red status line appears and audio falls back to English instead of silence. Then install the voice (Settings → Text-to-speech) and confirm இருமொழி/Both speaks Tamil then English in order.
4. **Language toggle.** Switch தமிழ் → ஆங்கிலம் → Both and confirm the caption surface and card labels change without a restart, and that a card speaks only the selected language(s).
5. **Camera permission flow.** Fresh install → Live Bridge shows the rationale and an "Allow camera" button. Deny, confirm Quick Cards still works and "Open settings" appears. Grant from settings, return, tap "Allow camera" again and confirm the panel switches to the M2 placeholder.
6. **Caption scale.** Captions are 34sp and card labels 20sp by construction; confirm nothing clips at the device's largest font scale (Settings → Display → Font size) and in landscape.
7. **Live camera.** Grant the camera and confirm the preview shows live frames and `Camera running` appears. Tap `Flip camera`; the preview must switch lens with no crash.
8. **Hand tracking.** Hold one hand in frame: the panel should change from `No hand in frame.` to `Hands tracked: 1` and draw a 21-point skeleton; the Confidence bar should move. Two hands → `Hands tracked: 2`. This is hand detection only — no phrase is recognised, by design (no trained classifier yet).
9. **Tamil voice, offline.** Install the Tamil TTS voice first (Settings → Text-to-speech → Install voice data), speak one card in Both mode, then enable airplane mode and repeat. The first Tamil sentence may need a download from the TTS app; after that it must work offline.

## What still owes

- Every device check above on a **physical** device. Emulator coverage is recorded in `CONTEXT.md`.
- Native Tamil and ISL review of the phrase catalog (M0).
- The M0 dataset, without which no phrase classifier can be trained; the 30-frame buffer,
  TFLite classifier, 0.80 threshold and 1.2 s debounce are all still missing.
- A decision on the dependency-pin lint warnings.
