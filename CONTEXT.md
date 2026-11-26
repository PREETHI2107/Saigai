# Saigai — current state

Status: current
Owner: Project Lead (Saigai)
Verified: 2026-11-26 · M1 commit · `assembleDebug` and `lintDebug` succeed with JDK 17 / SDK 35 / Gradle 8.11.1 · not yet verified on a device
Scope: What is implemented, what is not, and what blocks each milestone. The contract is [`docs/decisions/DESIGN_DOC_B1.md`](docs/decisions/DESIGN_DOC_B1.md).

## Milestones

| Milestone | State | Evidence or gap |
| --- | --- | --- |
| M0 — Scope and dataset readiness | **open** | 20-phrase shortlist drafted in `app/src/main/res/raw/phrases.json` with `review_status: pending-isl-and-tamil-native-review`. No ISL or native-Tamil sign-off, no data-collection protocol, no starter dataset. |
| M1 — Android app and Quick Cards foundation | **built, device-verification pending** | `python scripts/check_m1.py` passes; `./gradlew :app:assembleDebug` produced `app-debug.apk` (9.1 MiB, `org.yazhi.saigai` 0.1.0-m1) and `./gradlew :app:lintDebug` reports 0 errors, 2 dependency-pin warnings. APK badging shows CAMERA as the only permission, no INTERNET, camera feature not required. |
| M2 — On-device sign recognition | **input stage built, classifier blocked** | Live CameraX preview + camera flip run on an Android 15 x86_64 emulator. MediaPipe Hand Landmarker (bundled `hand_landmarker.task`, CPU delegate) initialises and analyses frames. **No phrase classifier exists**: it requires the M0 dataset, which has not been collected or ISL-reviewed, so no gesture is mapped to a phrase. See "M2 partial" below. |
| M3 — Speech-to-text | **open** | Mic button is a disabled placeholder; no STT engine, no `RECORD_AUDIO` permission. |
| M4 — MVP verification | **open** | No benchmark device, no test report, no rollback switch beyond the manual Quick Cards tab. |
| M5 — Controlled field pilot | **open** | Not scheduled; depends on M4 gate. |

## What the emulator run proved (Android 15, x86_64)

Installed and exercised on an emulator, screenshots captured at `/tmp`-equivalent local paths:

- App launches, no crash, no ANR.
- Quick Cards render all 20 phrases; Tamil glyphs correct including two-part `கொ`.
- Tap card → caption surface → TTS. logcat shows `ta-IN` synthesis dispatched, then `en-IN`, proving the "Both" sequencing chains through `UtteranceProgressListener`.
- Camera permission: rationale → system dialog → grant → live preview ("Camera running"); revoke → rationale + "Camera off", Quick Cards still works.
- Camera flip rebinds the lens without crashing.
- Hand tracking initialises (`No hand in frame.`) with no JNI/link error.

Not proven: hand **detection** accuracy or the skeleton drawing (no hand can be placed in front of an emulated camera here), real-device performance, audio intelligibility.

## M2 partial — what exists and what does not

Built: `ui/CameraPreview.kt` (CameraX preview + `ImageAnalysis`), `cam/HandTracker.kt` (MediaPipe Hand Landmarker, 21 landmarks per hand, LIVE_STREAM, CPU), `ui/HandSkeleton.kt` (skeleton renderer), real confidence readout from the presence score.

Missing, and deliberately not faked: the 30-frame sliding buffer, the quantised TFLite LSTM/1D-CNN classifier, the >0.80 phrase threshold and the 1.2 s debounce. Those need the M0 dataset. The screen says so instead of guessing a phrase.

Two build facts found the hard way and now guarded:

- MediaPipe's transitive Firebase `datatransport` **silently merged `INTERNET` and `ACCESS_NETWORK_STATE`** into the APK. Both are now stripped with `tools:node="remove"`, and `scripts/check_m1.py` fails if a merged manifest ever regains them.
- MediaPipe ships OpenCV + JNI libraries for four ABIs (~160 MB). `splits.abi` limits builds to `arm64-v8a` (52.7 MB) and `x86_64` (85.7 MB). There is no longer a single universal `app-debug.apk`.

## Known limitations of the M1 tree

1. **Not run on a physical device.** Emulator only: no latency measurement, no audio intelligibility check, no thermal behaviour. The device checklist in `docs/guides/BUILD_AND_RUN.md#device-checks` is unexecuted.
2. **Two dependency-pin warnings stand.** lint prefers newer `androidx.core` / `androidx.activity` releases than the versions this build was verified against; bumping them is a deliberate, separate change.
3. **Tamil rendering unverified on device.** System Noto Sans Tamil is assumed; the five glyph geometries in the `tamil-localization` skill have not been eyeballed on hardware.
4. **Tamil TTS availability varies by device, and the first Tamil utterance may need a download.** The screen reports a missing Tamil voice and falls back to English audio. On the test emulator the Google TTS engine fetched `ta-in-x-multi-r45.zvoice` over the network on first use (the TTS app has INTERNET; Saigai does not). Install the Tamil voice pack before the demo, then test in airplane mode.
5. **Two-way speech is unimplemented.** The mic button is disabled. Using Android's `SpeechRecognizer` was rejected because it delegates to a separate service that may transmit audio, which would falsify the offline claim; the frozen doc requires bundled Vosk/Whisper instead.
5. **Phrase wording is provisional.** Not reviewed by an ISL contributor or native Tamil reviewer (M0).

## Next move

Run M0 phrase review in parallel with a device build of M1: install the debug APK on the baseline 4GB device, then walk the checks in [`docs/guides/BUILD_AND_RUN.md`](docs/guides/BUILD_AND_RUN.md#device-checks) and record results here.
