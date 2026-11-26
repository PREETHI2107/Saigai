# Saigai (சைகை)

An offline, on-device Android bridge that turns isolated Indian Sign Language (ISL) gestures into spoken Tamil and English, and turns the hearing person's speech back into large Tamil/English captions. Built for hospital and bank counters in Tamil Nadu towns where interpreters are absent and indoor mobile data is unreliable.

- **Scope:** v1.0 MVP, 20 high-priority hospital/bank/emergency/general phrases, Android only.
- **Non-goals:** continuous ISL grammar, iOS/desktop/web, medical or legal advice.
- **Offline by construction:** the app declares no `INTERNET` permission and no analytics dependency. Nothing leaves the device.

The contract everything else follows is the frozen design doc: [`docs/decisions/DESIGN_DOC_B1.md`](docs/decisions/DESIGN_DOC_B1.md). Live status is [`CONTEXT.md`](CONTEXT.md); the doc index is [`docs/README.md`](docs/README.md).

## What exists today

Milestone **M1 — Android app and Quick Cards foundation** is implemented:

| Piece | Where |
| --- | --- |
| 20-phrase bilingual catalog (hospital 8, bank 5, emergency 4, general 3) | `app/src/main/res/raw/phrases.json` |
| App shell, tab + language toggle (தமிழ் / ஆங்கிலம் / Both) | `app/src/main/java/org/yazhi/saigai/ui/SaigaiApp.kt` |
| Quick Cards with category chips and tap-to-speak | `ui/QuickCardsScreen.kt` |
| Live Bridge screen: camera permission flow, high-contrast captions, replay | `ui/LiveBridgeScreen.kt` |
| Live CameraX preview + front/rear flip | `ui/CameraPreview.kt` |
| On-device MediaPipe hand landmarks (21 points/hand) + skeleton + real confidence | `cam/HandTracker.kt`, `ui/HandSkeleton.kt` |
| Offline Tamil/English TTS with "Both" sequencing and Tamil-voice-missing notice | `tts/PhraseSpeaker.kt` |
| UI strings in English and Tamil | `res/values/strings.xml`, `res/values-ta/strings.xml` |
| Static checks (catalog, string parity, no-INTERNET, manifest) | `scripts/check_m1.py` |

Verified on an Android 15 x86_64 emulator: launch, Quick Cards, Tamil rendering, tap-to-speak with Tamil→English sequencing, camera permission grant/deny, live preview, camera flip, hand-tracker init. See [`CONTEXT.md`](CONTEXT.md) for exactly what was and was not proven.

Not built yet: **ISL phrase classification** (needs the uncollected M0 dataset — the app says so rather than guessing), speech-to-text (M3), pilot build (M4/M5).

## Verify

```bash
python scripts/fetch_hand_model.py   # one-off: 7.8 MB MediaPipe model into assets/
python scripts/check_m1.py           # static checks; no Android SDK needed
./build-debug.sh                     # or build-debug.bat on Windows; pins JDK 17
./gradlew :app:testDebugUnitTest     # 11 JVM unit tests
./gradlew :app:lintDebug             # Android lint: 0 errors expected
```

Wraps in per-ABI APKs (MediaPipe bundles OpenCV, so there is no universal APK):

| APK | Use for |
| --- | --- |
| `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` | **physical phone — this is the demo build** (~53 MB) |
| `app/build/outputs/apk/debug/app-x86_64-debug.apk` | x86_64 emulator (~86 MB) |

Last verified build: JDK 17.0.21.1, Android SDK 35 (build-tools 35.0.0), Gradle 8.11.1 via the committed wrapper. `lintDebug` 0 errors; remaining warnings are dependency-version pins, one plural-candidate and one autoboxing notice. APK permissions after merge: `CAMERA` only — `INTERNET` is stripped with `tools:node="remove"` because MediaPipe's Firebase transport adds it.

Device verification steps (Tamil glyphs, TTS voices, latency) are in [`docs/guides/BUILD_AND_RUN.md`](docs/guides/BUILD_AND_RUN.md#device-checks). They have **not** been run yet.

## Build

Requires JDK 17, Android SDK platform 35, and either the committed Gradle wrapper or Android Studio Ladybug+. Set `ANDROID_HOME` or `sdk.dir` in `local.properties`.

On this machine the default `java` is JDK 25, which Gradle 8.11.1 cannot run on (it fails with the bare message "What went wrong: 25.0.1"). Use `build-debug.bat` / `build-debug.sh`, which pin JDK 17 for the build only. See [`docs/guides/BUILD_AND_RUN.md`](docs/guides/BUILD_AND_RUN.md).

## Privacy and review status

- No camera frames, hand landmarks, or audio are written to disk or transmitted. `scripts/check_m1.py` fails if an `INTERNET` permission appears.
- Phrase wording and ISL gesture definitions are **pending** sign-off by qualified ISL contributors and a native Tamil reviewer (M0). Until then, treat the catalog as provisional translation, not validated output.

## Repo layout

```text
app/src/main/          Android app (Kotlin, Jetpack Compose, Material 3)
scripts/check_m1.py    Static M1 checks
docs/decisions/        Frozen contracts (design doc v1.0)
docs/guides/           How-to docs (build, device checks)
.github/ISSUE_DRAFTS/  Milestone issues M0–M5, ready to post
```
