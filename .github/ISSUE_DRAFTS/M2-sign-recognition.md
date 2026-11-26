---
title: "M2 — On-device sign recognition pipeline (CameraX + MediaPipe + TFLite)"
labels: milestone-m2
---

Tracks milestone **M2** from [`docs/decisions/DESIGN_DOC_B1.md`](../docs/decisions/DESIGN_DOC_B1.md) §11. Builds on the M1 shell that already exists (`app/src/main/java/org/yazhi/saigai`).

## Goal

Pointing the camera at one of the 20 catalog gestures speaks the phrase, entirely on device, with a confidence gate and debounce — no network path at any point.

## Work

- [ ] Add CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) with a `PreviewView` in the Live Bridge panel, replacing the M1 placeholder text. Keep the existing permission flow.
- [ ] Camera flip (front/rear) in the header, as specified in design doc §4.
- [ ] Integrate the MediaPipe Hand Landmarker task (bundled `.task` asset, 21 landmarks per hand).
- [ ] Implement the 30-frame sliding buffer over landmark coordinates (normalise per-hand to translation/scale invariance; document the normalisation in code).
- [ ] Train an LSTM / 1D-CNN sequence classifier on the M0 dataset, quantise it to TFLite, and record size + on-device inference time.
- [ ] Add the confidence threshold (> 0.80) and the 1.2s debounce lock; wire the winning label to `PhraseCatalog` by id and to `PhraseSpeaker`.
- [ ] Feed the confidence meter already on screen (`ConfidenceRow` in `ui/LiveBridgeScreen.kt`) from the real score instead of the 0f placeholder.
- [ ] Handle the negative case: no hand in frame, low confidence, and two hands detected — each with a distinct on-screen state, not a silent no-op.
- [ ] Record latency measurement instrumentation (frame in → audio out) so M4 can report the <300ms gate.

## Exit criteria (design doc §11)

- [ ] No camera frames or hand landmarks are transmitted — asserted by the manifest having no `INTERNET` permission and no socket call in the diff.
- [ ] Inference works in airplane mode.
- [ ] Latency, top-1 accuracy, and false-trigger rate measured and reported on the baseline 4GB device.

## Out of scope

Speech-to-text (M3), dataset collection and ISL review (M0), any cloud inference (rejected as Option B, design doc §5).

## Notes for the implementer

- Ship the model as an APK asset and keep the model file out of git if it exceeds a few MB; document the download/build step instead.
- The 20-phrase catalog and its ids are the contract between model labels and UI — a model class that has no catalog id must fail loudly, not map to the nearest phrase.
