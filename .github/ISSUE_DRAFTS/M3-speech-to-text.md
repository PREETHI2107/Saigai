---
title: "M3 — Offline speech-to-text for the hearing person's reply"
labels: milestone-m3
---

Tracks milestone **M3** from [`docs/decisions/DESIGN_DOC_B1.md`](../docs/decisions/DESIGN_DOC_B1.md) §11. Completes the two-way bridge: the front-desk staff member speaks, the Deaf user reads.

## Goal

Press and hold the mic, speak Tamil or Indian English at a normal pace, and read a ≥28sp transcription on screen — offline, with no recording retained after transcription.

## Work

- [ ] Evaluate on-device STT candidates on the baseline device: Vosk Tamil + Indian-English models vs Whisper-tiny (quantised). Record model size, load time, word accuracy on 20 hospital/bank sentences, and thermal behaviour.
- [ ] Choose one, add it as a local dependency or asset, and document the choice + size in `CONTEXT.md`.
- [ ] Add `android.permission.RECORD_AUDIO`, runtime permission handling, and a denial/error path (mirror the camera flow in `ui/LiveBridgeScreen.kt`).
- [ ] Wire the existing disabled "Hold to Speak" button: press-to-start, release-to-transcribe, with a visible recording state that cannot be mistaken for idle.
- [ ] Render the transcript in the caption surface at ≥28sp (reuse `MaterialTheme.typography.headlineMedium`), in the language actually spoken, with a script label so the user knows which language was recognised.
- [ ] Ensure audio is processed in memory only: no file written, no buffer logged. Add a code comment stating the invariant.
- [ ] Handle the failure states honestly: model missing, mic busy, silence, unsupported language, low-confidence transcript.

## Exit criteria (design doc §11)

- [ ] Selected STT option runs offline on the target device (airplane mode, cold start).
- [ ] Tamil/English captions are readable at 28sp or larger.
- [ ] Recording is not retained after processing — verified by grepping the diff for `File`/`FileOutputStream`/`MediaRecorder` and by a device storage check.

## Out of scope

Sign recognition (M2), cloud STT, transcript history or logging, translation between the two languages.
