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
| Offline Tamil/English TTS with "Both" sequencing and Tamil-voice-missing notice | `tts/PhraseSpeaker.kt` |
| UI strings in English and Tamil | `res/values/strings.xml`, `res/values-ta/strings.xml` |
| Static checks (catalog, string parity, no-INTERNET, manifest) | `scripts/check_m1.py` |

Not built yet: camera preview and on-device sign recognition (M2), speech-to-text (M3), pilot build (M4/M5).

## Verify

```bash
python scripts/check_m1.py     # M1 static checks; no Android SDK needed
```

Device verification steps (Tamil glyphs, TTS voices, latency) are in [`docs/guides/BUILD_AND_RUN.md`](docs/guides/BUILD_AND_RUN.md#device-checks).

## Build

Requires JDK 17, Android SDK 35, and Gradle 8.11 (or Android Studio Ladybug+). See the build guide for the exact steps — the Gradle wrapper is not committed yet.

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
