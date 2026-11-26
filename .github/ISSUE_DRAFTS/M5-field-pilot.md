---
title: "M5 — Controlled field pilot in Virudhunagar (1 clinic + 1 bank branch)"
labels: milestone-m5
---

Tracks milestone **M5** from [`docs/decisions/DESIGN_DOC_B1.md`](../docs/decisions/DESIGN_DOC_B1.md) §11. Runs only after the M4 gate passes.

## Goal

10–15 Deaf and hard-of-hearing users complete real hospital/bank tasks with the app, and we come back with a prioritised improvement backlog and no privacy incidents.

## Work

- [ ] Secure written agreement from one clinic and one bank branch in Virudhunagar, including a staff briefing on what the app can and cannot do.
- [ ] Recruit 10–15 participants; obtain informed consent in Tamil, in a form participants can read without English literacy.
- [ ] Run the pilot for two weeks with Quick Cards available at all times as the documented fallback.
- [ ] Instrument the four pilot metrics: task completion time, first-attempt recognition rate, Quick Cards fallback rate, crash-free sessions.
- [ ] Collect qualitative feedback from both Deaf users and counter staff, in Tamil, and record it verbatim where consent allows.
- [ ] Run a daily triage during the pilot: any safety incident or recognition failure that blocks a task is logged the same day.
- [ ] Hand the pilot build + rollback instructions to the site, and confirm they can revert to Quick Cards unaided.

## Exit criteria (design doc §11)

- [ ] Pilot report in `docs/reports/` covering first-attempt recognition, task completion time, fallback rate, user feedback, safety incidents, and crash-free sessions.
- [ ] No unconsented video or audio collection — verified by reviewing every artifact that leaves the site.
- [ ] Prioritised improvement backlog filed as issues, with the ones that would have blocked a real task ranked first.

## Out of scope

Feature development during the pilot, any cloud sync of collected metrics (aggregate by hand if needed), marketing.

## Privacy line

Nothing recorded at the site is committed to this repository. Participant identifiers stay in the consent register held by the project lead, never in the issue tracker.
