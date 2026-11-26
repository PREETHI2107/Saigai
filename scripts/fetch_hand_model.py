#!/usr/bin/env python3
"""Download the MediaPipe hand-landmark model into app/src/main/assets/.

The .task file is 7.8 MB, which is too large to commit, so it is fetched on
demand and ignored by git (see .gitignore). The app degrades honestly without it:
HandTracker reports MODEL_MISSING and the UI says so — it does not crash.

Usage:
    python scripts/fetch_hand_model.py

Verified file: hand_landmarker float16 v1, 7,819,105 bytes, SHA-256
fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1
"""

from __future__ import annotations

import hashlib
import sys
import urllib.request
from pathlib import Path

MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/hand_landmarker/"
    "hand_landmarker/float16/1/hand_landmarker.task"
)
EXPECTED_SHA256 = "fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1"
DESTINATION = Path(__file__).resolve().parent.parent / "app/src/main/assets/hand_landmarker.task"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    DESTINATION.parent.mkdir(parents=True, exist_ok=True)

    if DESTINATION.is_file():
        digest = sha256(DESTINATION)
        print(f"already present: {DESTINATION} ({DESTINATION.stat().st_size} bytes)")
        print(f"sha256: {digest}")
        if digest != EXPECTED_SHA256:
            print("ERROR: existing file does not match the expected digest; delete it and re-run")
            return 1
        return 0

    print(f"downloading {MODEL_URL}")
    with urllib.request.urlopen(MODEL_URL, timeout=120) as response:
        payload = response.read()

    digest = hashlib.sha256(payload).hexdigest()
    if digest != EXPECTED_SHA256:
        print(f"ERROR: digest mismatch\n  expected {EXPECTED_SHA256}\n  got      {digest}")
        return 1

    DESTINATION.write_bytes(payload)
    print(f"wrote {DESTINATION} ({len(payload)} bytes)")
    print(f"sha256: {digest} (verified)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
