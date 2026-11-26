# Saigai — current state

Status: current
Owner: Project Lead (Saigai)
Verified: 2026-11-26 · M1 commit (Kotlin/Compose app, `scripts/check_m1.py` passing) · not yet verified on a device
Scope: What is implemented, what is not, and what blocks each milestone. The contract is [`docs/decisions/DESIGN_DOC_B1.md`](docs/decisions/DESIGN_DOC_B1.md).

## Milestones

| Milestone | State | Evidence or gap |
| --- | --- | --- |
| M0 — Scope and dataset readiness | **open** | 20-phrase shortlist drafted in `app/src/main/res/raw/phrases.json` with `review_status: pending-isl-and-tamil-native-review`. No ISL or native-Tamil sign-off, no data-collection protocol, no starter dataset. |
| M1 — Android app and Quick Cards foundation | **implemented, device-verification pending** | `scripts/check_m1.py` passes: catalog valid (20 phrases, 4 categories), English/Tamil string keys match, manifest has CAMERA and no INTERNET. App builds were **not** run here: no Android SDK/JDK 17 on the authoring machine. |
| M2 — On-device sign recognition | **open** | Camera permission flow exists; no CameraX preview, no MediaPipe, no TFLite model. |
| M3 — Speech-to-text | **open** | Mic button is a disabled placeholder; no STT engine, no `RECORD_AUDIO` permission. |
| M4 — MVP verification | **open** | No benchmark device, no test report, no rollback switch beyond the manual Quick Cards tab. |
| M5 — Controlled field pilot | **open** | Not scheduled; depends on M4 gate. |

## Known limitations of the M1 tree

1. **Build unverified.** AGP 8.7.3 / Kotlin 2.0.21 / Compose BOM 2024.10.01 are pinned but nothing was compiled locally. Treat the first successful `assembleDebug` as part of M1 acceptance.
2. **No Gradle wrapper committed.** A clean checkout needs `gradle wrapper --gradle-version 8.11.1` or Android Studio before `./gradlew` exists.
3. **Tamil rendering unverified on device.** System Noto Sans Tamil is assumed; the five glyph geometries in the `tamil-localization` skill have not been eyeballed on hardware.
4. **Tamil TTS availability varies by device.** The screen reports a missing Tamil voice and falls back to English audio, but no bundled fallback voice exists (design doc §10).
5. **Phrase wording is provisional.** Not reviewed by an ISL contributor or native Tamil reviewer (M0).

## Next move

Run M0 phrase review in parallel with a device build of M1: install the debug APK on the baseline 4GB device, then walk the checks in [`docs/guides/BUILD_AND_RUN.md`](docs/guides/BUILD_AND_RUN.md#device-checks) and record results here.
