# B1 Design Doc — Saigai (சைகை): Offline Real-Time ISL-to-Tamil/English Speech Accessibility Bridge (v1.0 MVP)

Status: frozen
Version: 1.0
Owner: Project Lead (Saigai)
Verified: 2026-11-26 · commit that added this file · body received from the project lead and reproduced unchanged; only this header was added
Scope: The frozen v1.0 MVP contract. Code, issues, and milestone gates cite its section numbers. Changes ship as v1.1 in a new file, never by editing this one.

## 1. Title, Author, Reviewers, Status, Date

Title: Saigai (சைகை) — Offline Real-Time ISL-to-Tamil/English Speech Accessibility Bridge (v1.0 MVP)

Author: Project Lead / Solo Developer (Saigai Team)

Reviewers: Accessibility Community Mentors, Domain Evaluators (Hospital & Bank Staff)

Status: Draft / Ready for MVP Build

Date: November 26, 2026

## 2. Context

Deaf and hard-of-hearing individuals across Tamil Nadu face severe communication barriers at public service touchpoints such as government hospitals, banks, and municipal offices because front-desk staff do not understand Indian Sign Language (ISL). Existing solutions rely either on human interpreters—who are unavailable in Tier-2/Tier-3 towns like Virudhunagar and Madurai—or cloud-based translation apps that fail under poor indoor network connectivity, lack Tamil voice synthesis, and suffer from high latency during urgent interactions.

## 3. Goals and Non-Goals

Goals

- Translate 15–20 high-priority hospital, bank, and emergency ISL gestures into spoken Tamil and English audio offline within less than 300 ms per gesture.
- Provide two-way communication via offline/on-device Speech-to-Text (STT) that displays large, high-contrast Tamil and English captions for the hearing person's spoken response.
- Include a zero-latency Quick Cards grid for common phrases when camera framing or ambient lighting are poor.
- Run 100% offline on mid-range Android devices (4GB RAM+) without sending camera or audio frames to external servers.

Non-Goals

- Full continuous ISL grammar or open-vocabulary sentence translation in v1.0 (scoped strictly to isolated/short-sequence core phrases).
- iOS, desktop, or web browser deployment in v1.0.
- Medical diagnosis, legal advice, or official certified translation replacement.

## 4. Design

User & System Interfaces (Before Internals)

- Live Bridge Screen (Primary UI): Split-view mobile interface. Top half displays the front/rear camera feed with subtle hand-tracking skeleton feedback and a confidence meter; bottom half displays large Tamil/English output text, a replay speaker button, and a prominent "Hold to Speak" (Mic) button for the hearing counterpart.
- Quick Cards Screen: Categorized tab layout (Hospital, Bank, Emergency, General) with high-contrast icon cards. Tapping any card immediately triggers local Tamil/English TTS playback.
- Language & Mode Toggle: Single-tap header switch for output audio language (தமிழ் / English / Both) and camera flip.

Architecture & Data Flow

Input Pipeline (Sign → Speech): CameraX frame stream (30 FPS) → MediaPipe Hand Landmarker extracts 21 (x, y, z) coordinates per hand → sliding buffer of 30 frames feeds into a quantized TFLite LSTM/1D-CNN classifier → prediction passes a > 0.80 confidence threshold and 1.2s debounce lock → mapped to localized Tamil/English phrase → spoken via Android Native TTS.

Reverse Pipeline (Speech → Text): Counterpart presses Mic → Vosk / Whisper-tiny on-device STT transcribes Tamil/English speech → rendered in 28sp+ high-contrast typography on screen.

```text
[Camera Feed] → [MediaPipe Hands (21 pts)] → [30-Frame Buffer]
[Android TTS] ← [Debounce & Phrase Map] ← [TFLite Classifier]
[Counterpart Mic] → [On-Device STT (Vosk)] → [Large Tamil/EN Captions]
```

## 5. Alternatives Considered

| Option | Approach | Why It Lost |
| --- | --- | --- |
| Option A: Doing Nothing | Rely on pen-and-paper notes or family escorts at hospitals/banks. | Slows down urgent care, compromises patient privacy, and fails when users have low written literacy in English/Tamil. |
| Option B: Cloud Vision + Cloud TTS API | Stream video frames to a remote GPU server for inference and audio synthesis. | Fails completely inside concrete hospital/bank basements with weak mobile data; incurs recurring per-minute API costs and raises video privacy risks. |
| Option C: Raw RGB Video CNN (MobileNet/3D-CNN) | Feed raw camera pixels directly into an image classification model without landmark extraction. | Sensitive to background clutter, skin tone, and lighting variations; model size (>25MB) causes thermal throttling on budget Android phones. |

## 6. Sovereignty and Privacy

Data Residency: 100% on-device processing. Zero video frames, hand coordinates, or voice recordings are stored permanently or transmitted to any cloud server.

Core Dependencies & Replaceability:

- MediaPipe Hand Landmarker (Apache 2.0) — replaceable with MoveNet or custom ONNX hand-keypoint detectors.
- TensorFlow Lite Runtime (Apache 2.0) — replaceable with ONNX Runtime Mobile or ExecuTorch.
- Android Native TTS / Vosk STT — replaceable with local Piper TTS and Whisper.cpp quantized binaries.

## 7. Language

Affected Languages & Scripts:

- Input: Indian Sign Language (ISL) manual gestures; spoken Tamil (ta-IN) and Indian English (en-IN).
- Output: Tamil script (தமிழ்) and Latin script (English), paired with Tamil and English speech synthesis.

Evaluation:

- Verify Tamil Unicode rendering across standard Android system fonts (Noto Sans Tamil) without glyph clipping.
- Native Tamil speakers test TTS intelligibility and pronunciation clarity on hospital/bank phrases in noisy indoor environments (60–70 dB ambient noise).

## 8. Evaluation and Rollout

Eval Gate (Before Field Pilot)

- Top-1 gesture classification accuracy greater than 90% across 5 distinct signers under varied indoor lighting.
- End-to-end gesture-to-audio latency less than 300 ms on a baseline 4GB RAM Android device.
- False-trigger rate during idle hand movement less than 2%.

Canary Plan

- Phase 1 (Week 1–2): Lab validation with 5–10 student/community testers using the 20-phrase set.
- Phase 2 (Week 3–4): Controlled field pilot at 1 local clinic and 1 bank branch in Virudhunagar with 10–15 target users.
- Metrics: Task completion time (time to convey intent), first-attempt recognition rate, fallback rate to Quick Cards, and app crash-free sessions (>99.5%).
- Rollback: If live camera inference accuracy drops below 75% in field lighting, default the launch screen to Quick Cards Mode while keeping Live Camera accessible as an optional tab.

## 9. Cost Estimate

Hardware: ₹0 incremental (uses existing developer laptop, Google Colab free T4 GPU for training, and existing Android test devices).

People: 1 Developer × 3 weeks (approx. 60–75 engineering hours for dataset collection, model training, and Flutter/Android integration).

Running Cost: ₹0 / month (zero cloud inference, zero database hosting, zero API billing due to 100% offline edge execution).

Optional Distribution Cost: ₹2,100 ($25 one-time) only if publishing to Google Play Console; ₹0 for direct APK / hackathon demo distribution.

## 10. Open Questions

- One-Handed vs. Two-Handed ISL Signs: Which of the initial 20 phrases require two-handed tracking, and how does accuracy degrade if the user holds the phone in one hand? (Should we enforce a tabletop/counter stand mode or prioritize single-hand ISL variants?)
- Offline Tamil TTS Voice Availability: Many budget Android devices ship without the Tamil offline voice pack pre-installed; should the app bundle a lightweight local TTS fallback or guide first-time setup?
- Non-Manual Markers: How many critical medical/bank signs depend on facial expressions (e.g., pain severity or question vs. statement) beyond pure hand landmarks?

## 11. Execution Milestones

The MVP is planned as a three-week engineering build (approximately 60–75 engineering hours), followed by the lab and controlled field-pilot gates described in Section 8. Milestones are sequential; dates should be set after the project start date is confirmed.

| Milestone | Target window | Execution work | Deliverable | Exit / acceptance criteria |
| --- | --- | --- | --- | --- |
| M0 — Scope and dataset readiness | Week 1 (Days 1–3) | Confirm the 15–20 phrase shortlist with Deaf/ISL community input; identify one- and two-handed signs; define consent, recording, labelling and dataset handling; confirm target Android device and offline TTS/STT feasibility. | Approved phrase list, data-collection protocol, labelled starter dataset, device test checklist. | Phrase meanings and regional variants reviewed with qualified ISL contributors; consent and data handling documented; Tamil offline TTS and candidate STT tested on the target device. |
| M1 — Android app and Quick Cards foundation | Week 1 (Days 4–7) | Create Android app shell; implement language toggle, camera permission flow, Quick Cards categories, phrase mapping, local TTS playback and accessible high-contrast captions. | Runnable Android prototype with Quick Cards and Tamil/English audio. | All agreed MVP phrases are available as cards; app works without network; Tamil glyphs render correctly; user can switch Tamil, English and Both. |
| M2 — On-device sign recognition pipeline | Week 2 | Integrate CameraX and MediaPipe Hand Landmarker; implement 30-frame sequence buffer; train and quantize an initial TFLite LSTM/1D-CNN classifier; add confidence threshold and debounce; connect recognized labels to phrase mapping and TTS. | End-to-end sign-to-speech prototype using on-device inference. | No camera frames or hand landmarks are transmitted; inference works in airplane mode; latency, top-1 accuracy and false-trigger rate are measured and reported on the baseline 4GB device. |
| M3 — Reverse speech-to-text and hardening | Week 3 (Days 1–3) | Integrate and compare on-device Vosk / Whisper-tiny options for Tamil and Indian English; implement microphone interaction, transcription display, error states and permission handling. | Two-way prototype: sign-to-speech and speech-to-text. | Selected STT option runs offline on the target device; Tamil/English captions are readable at 28sp or larger; recording is not retained after processing. |
| M4 — MVP verification and pilot readiness | Week 3 (Days 4–5) | Run tests across at least five distinct signers and varied indoor lighting; measure latency, recognition accuracy, false triggers, crash-free sessions and Quick Cards fallback; fix critical defects; prepare pilot instructions. | MVP test report, known-limitations list, pilot build and rollback configuration. | Pilot proceeds only if the agreed evaluation gate is met: >90% top-1 accuracy, <300ms gesture-to-audio latency and <2% idle false-trigger rate. If field accuracy is <75%, default to Quick Cards Mode. |
| M5 — Controlled field pilot | After M4; Weeks 1–2 of pilot | Run the planned clinic and bank pilot in Virudhunagar with 10–15 target users; collect informed feedback and task-completion metrics; provide Quick Cards as a fallback. | Field-pilot findings and prioritized improvement backlog. | Pilot report includes first-attempt recognition, task completion time, fallback rate, user feedback, safety incidents and crash-free sessions; no unconsented video/audio collection. |

Milestone Governance

- Owner: Project Lead / Solo Developer. Accessibility community contributors and hospital/bank evaluators review phrase meaning, usability and pilot safety.
- Do not treat the proposed accuracy, latency, model-size or cost figures as achieved results until they are measured on the target devices and recorded in the test report.
- A milestone is complete only when its deliverable exists, acceptance criteria are measured, known limitations are documented and privacy checks pass.
- If the dataset or qualified ISL review is not ready, pause model training rather than treating unreviewed gestures as validated translations.
