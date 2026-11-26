---
title: "M4 — MVP verification and pilot readiness (accuracy, latency, false-trigger gate)"
labels: milestone-m4
---

Tracks milestone **M4** from [`docs/decisions/DESIGN_DOC_B1.md`](../docs/decisions/DESIGN_DOC_B1.md) §11. This is the gate between "it works in the lab" and "put it in front of a Deaf patient at a bank counter".

## Goal

A measured test report against the numbers in design doc §8, a known-limitations list, a pilot build, and a rollback switch — or an explicit decision to delay the pilot.

## Work

- [ ] Test with at least five distinct signers (from M0 recruitment) under at least three indoor lighting conditions: fluorescent corridor, window backlight, dim waiting area.
- [ ] Measure and report: top-1 accuracy per phrase, end-to-end gesture-to-audio latency (p50/p95), idle false-trigger rate, crash-free sessions.
- [ ] Measure Quick Cards fallback rate and cold-start-to-first-phrase time.
- [ ] Implement the design doc §8 rollback: an in-app switch and a launch-default setting so Quick Cards becomes the default screen when field accuracy drops below 75%, with Live Camera still reachable.
- [ ] Fix critical defects found; log the rest in a known-limitations section of the report.
- [ ] Write the pilot instructions for clinic/bank staff: how to hold the phone, where to stand, how to fall back to cards, what to do if recognition fails.
- [ ] Record battery/thermal behaviour over a 20-minute continuous session.

## Exit criteria (design doc §11)

- [ ] Top-1 accuracy > 90% across 5 signers and varied indoor lighting.
- [ ] Gesture-to-audio latency < 300 ms on the baseline 4GB device.
- [ ] Idle false-trigger rate < 2%.
- [ ] Report in `docs/reports/` with the device, build hash, and raw measurement tables; `CONTEXT.md` updated with the numbers.
- [ ] Rollback configuration exists and has been exercised at least once.

## Out of scope

New features, model architecture changes beyond what the gate requires, pilot logistics (M5).

## Honesty rule

Do not carry forward the design doc's targets as results. Every number in the report comes from a named command or a named test session with a device serial.
