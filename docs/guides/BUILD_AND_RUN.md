# Build, install, and verify Saigai

Status: current
Owner: Project Lead (Saigai)
Verified: 2026-11-26 · M1 commit · steps written from the build files in this repo, not yet executed on a machine with the Android SDK
Scope: How to produce a debug APK from this tree and what to check on the device. Milestone gates live in the design doc.

## Prerequisites

| Tool | Version used by the build files |
| --- | --- |
| JDK | 17 (AGP 8.7.3 rejects JDK 8 and is untested on JDK 25) |
| Android SDK | Platform 35, Build-Tools 35.x |
| Gradle | 8.11.1 |
| Android Studio | Ladybug (2024.2.1) or newer, if you prefer the IDE |

Set `ANDROID_HOME` (or `sdk.dir` in `local.properties`) before building.

## First build

The Gradle wrapper is **not committed**, so generate it once before `./gradlew` works:

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew :app:assembleDebug
```

Android Studio users can skip the wrapper step: open the repo and let the IDE sync.

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Static checks (no Android SDK needed)

```bash
python scripts/check_m1.py
```

Covers: phrase catalog shape (20 phrases, kebab-case unique ids, all four categories, Tamil in `ta` and no Tamil in `en`), English/Tamil string-key parity, Tamil-character presence in every `values-ta` string, every `R.string.*` reference resolving, `CAMERA` declared, `INTERNET`/`AD_ID`/`RECORD_AUDIO` absent. Exit code 1 fails the check.

## Device checks

Run these on the baseline 4GB device and record the result in `CONTEXT.md`. These are the parts a static check cannot prove.

1. **Offline by construction.** Enable airplane mode, force-stop, relaunch. Quick Cards must speak. If anything shows a network error, the manifest has drifted.
2. **Tamil glyphs.** Open Quick Cards in தமிழ் mode and confirm all five geometries render (no tofu, no detached vowel signs): right-side (`கா கி கீ`), below (`கு டு று`), left-reordered (`கெ கே கை`), two-part (`கொ கோ கௌ`), pulli/conjunct (`க் க்ஷ ஸ்ரீ`). The catalog contains `ஸ்ரீ`-free text, so add a temporary card if you need the conjunct row.
3. **Tamil TTS voice.** Pick a device without the offline Tamil voice and confirm the red status line appears and audio falls back to English instead of silence. Then install the voice (Settings → Text-to-speech) and confirm இருமொழி/Both speaks Tamil then English in order.
4. **Language toggle.** Switch தமிழ் → ஆங்கிலம் → Both and confirm the caption surface and card labels change without a restart, and that a card speaks only the selected language(s).
5. **Camera permission flow.** Fresh install → Live Bridge shows the rationale and an "Allow camera" button. Deny, confirm Quick Cards still works and "Open settings" appears. Grant from settings, return, tap "Allow camera" again and confirm the panel switches to the M2 placeholder.
6. **Caption scale.** Captions are 34sp and card labels 20sp by construction; confirm nothing clips at the device's largest font scale (Settings → Display → Font size) and in landscape.

## What M1 still owes

- A compiled, installed APK from this tree (no build has been run yet — see `CONTEXT.md`).
- A committed Gradle wrapper, or a decision to keep it out of git.
- Native Tamil and ISL review of the phrase catalog (M0).
