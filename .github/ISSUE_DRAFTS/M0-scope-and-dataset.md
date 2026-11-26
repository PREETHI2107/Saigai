---
title: "M0 — Scope and dataset readiness (phrase list, consent, device feasibility)"
labels: milestone-m0
---

Tracks milestone **M0** from [`docs/decisions/DESIGN_DOC_B1.md`](../docs/decisions/DESIGN_DOC_B1.md) §11. Everything downstream (M2 training, M4 evaluation) is unblocked or blocked by this issue.

## Goal

Here is done means: an approved 20-phrase shortlist, a written data-collection/consent protocol, a labelled starter dataset, and a device feasibility checklist — all reviewed by qualified ISL contributors and a native Tamil reviewer.

## Work

- [ ] Confirm the 15–20 phrase shortlist with Deaf/ISL community input. Draft catalog now in `app/src/main/res/raw/phrases.json` (hospital 8, bank 5, emergency 4, general 3) — review each Tamil wording and English meaning.
- [ ] For each phrase, record the ISL gesture definition, one-handed vs two-handed, and known regional variants. Nothing is "validated translation" before this.
- [ ] Write the consent + recording + labelling + dataset-handling protocol. Recordings stay local, are never committed (`datasets/` and `recordings/` are gitignored), and are deleted on request.
- [ ] Recruit 5 distinct signers with varied hand size, skin tone, and signing speed for M4's accuracy gate.
- [ ] Confirm the baseline target device (4GB RAM Android) and record its model, SoC, and Android version.
- [ ] Test offline Tamil TTS voice availability on that device, and test Vosk / Whisper-tiny Tamil + Indian-English candidates for M3.
- [ ] Decide the two open design questions that M0 can answer: tabletop/counter stand mode vs single-hand ISL variants; bundled TTS fallback vs first-run setup guidance.

## Exit criteria (design doc §11)

- [ ] Phrase meanings and regional variants reviewed with qualified ISL contributors.
- [ ] Consent and data handling documented.
- [ ] Tamil offline TTS and candidate STT tested on the target device.
- [ ] `review_status` in `phrases.json` changes from `pending-isl-and-tamil-native-review` once reviewed; `scripts/check_m1.py` enforces that it stays `pending…` until then.

## Out of scope

Model training, app UI changes, anything that depends on camera inference.

## Do not

Treat unreviewed gestures as validated translations, or start M2 training on a dataset that has not passed this gate.
