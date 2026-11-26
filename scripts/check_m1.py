#!/usr/bin/env python3
"""M1 static checks for Saigai.

The Android SDK is not required. These checks cover the M1 acceptance criteria that
can be verified without a device:

  * the phrase catalog is well formed, unique, bilingual, and covers all four categories
  * every UI string exists in both values/ and values-ta/ with the same keys
  * every R.string.<name> used from Kotlin is defined
  * the manifest declares CAMERA and does NOT declare INTERNET (design doc §6)
  * resources and the manifest are well-formed XML

They do NOT prove the app builds, that Tamil glyphs shape correctly on a device, or
that TTS voices exist. Those are device checks and are listed in docs/guides/BUILD_AND_RUN.md.

Usage: python scripts/check_m1.py
Exit code 0 = all checks pass, 1 = at least one check failed.
"""

from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
CATALOG = RES / "raw" / "phrases.json"
STRINGS_EN = RES / "values" / "strings.xml"
STRINGS_TA = RES / "values-ta" / "strings.xml"
MANIFEST = ROOT / "app" / "src" / "main" / "AndroidManifest.xml"
KOTLIN_DIR = ROOT / "app" / "src" / "main" / "java"

CATEGORIES = {"hospital", "bank", "emergency", "general"}
TAMIL_RANGE = re.compile(r"[\u0B80-\u0BFF]")
FAILURES: list[str] = []


def fail(message: str) -> None:
    FAILURES.append(message)


def check(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def load_strings(path: Path) -> dict[str, str]:
    tree = ET.parse(path)
    return {
        element.attrib["name"]: (element.text or "")
        for element in tree.getroot().findall("string")
    }


def check_xml() -> None:
    for path in (STRINGS_EN, STRINGS_TA, MANIFEST):
        try:
            ET.parse(path)
        except ET.ParseError as error:
            fail(f"{path.relative_to(ROOT)}: not well-formed XML: {error}")


def check_string_parity() -> None:
    en = load_strings(STRINGS_EN)
    ta = load_strings(STRINGS_TA)

    only_en = sorted(set(en) - set(ta))
    only_ta = sorted(set(ta) - set(en))
    check(not only_en, f"strings missing from values-ta/strings.xml: {only_en}")
    check(not only_ta, f"strings missing from values/strings.xml: {only_ta}")
    check(en.get("app_name") != ta.get("app_name"), "Tamil and English app_name are identical")

    for key, value in ta.items():
        check(
            bool(TAMIL_RANGE.search(value)),
            f"values-ta string '{key}' has no Tamil characters: {value!r}",
        )
    # Tamil localisation rule: never build sentences by concatenating fragments, so
    # no Tamil string may be a bare placeholder interpolation.
    for key, value in ta.items():
        check(
            value.replace("%1$s", "").strip() != "",
            f"values-ta string '{key}' is only a placeholder",
        )


def check_catalog() -> list[dict]:
    raw = json.loads(CATALOG.read_text(encoding="utf-8"))
    phrases = raw.get("phrases", [])
    check(raw.get("version") == 1, f"phrases.json version is {raw.get('version')}, expected 1")
    check(
        15 <= len(phrases) <= 20,
        f"phrase count {len(phrases)} is outside the 15-20 scope of design doc §3",
    )
    check(
        raw.get("review_status", "").startswith("pending"),
        "review_status must stay 'pending...' until M0 sign-off lands",
    )

    seen: dict[str, int] = {}
    counts = {category: 0 for category in CATEGORIES}
    for entry in phrases:
        key = entry.get("id", "")
        seen[key] = seen.get(key, 0) + 1
        check(bool(re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", key)), f"phrase id not kebab-case: {key!r}")
        category = entry.get("category")
        check(category in CATEGORIES, f"{key}: unknown category {category!r}")
        if category in counts:
            counts[category] += 1
        for field in ("ta", "en"):
            check(bool(entry.get(field, "").strip()), f"{key}: empty '{field}'")
        check(
            bool(TAMIL_RANGE.search(entry.get("ta", ""))),
            f"{key}: 'ta' contains no Tamil characters: {entry.get('ta')!r}",
        )
        check(
            not TAMIL_RANGE.search(entry.get("en", "")),
            f"{key}: 'en' contains Tamil characters: {entry.get('en')!r}",
        )
    duplicates = sorted(key for key, count in seen.items() if count > 1)
    check(not duplicates, f"duplicate phrase ids: {duplicates}")

    empty = sorted(category for category, count in counts.items() if count == 0)
    check(not empty, f"categories with no phrases: {empty}")
    return phrases


def check_string_references() -> None:
    defined = set(load_strings(STRINGS_EN))
    referenced: set[str] = set()
    for source in KOTLIN_DIR.rglob("*.kt"):
        referenced.update(re.findall(r"R\.string\.([a-z0-9_]+)", source.read_text(encoding="utf-8")))
    missing = sorted(referenced - defined)
    check(not missing, f"R.string references with no definition: {missing}")

    unused = sorted(defined - referenced)
    if unused:
        print(f"note: defined but unreferenced strings: {unused}")


def check_manifest() -> None:
    text = MANIFEST.read_text(encoding="utf-8")
    tree = ET.parse(MANIFEST)
    android = "{http://schemas.android.com/apk/res/android}"
    permissions = {
        element.attrib.get(f"{android}name")
        for element in tree.getroot().findall("uses-permission")
    }
    check(
        "android.permission.CAMERA" in permissions,
        "manifest must declare android.permission.CAMERA",
    )
    check(
        "android.permission.INTERNET" not in permissions,
        "manifest must not declare INTERNET: design doc §6 requires on-device only",
    )
    check(
        "com.google.android.gms.permission.AD_ID" not in permissions,
        "manifest must not declare the advertising ID permission",
    )
    check(
        "android.permission.RECORD_AUDIO" not in permissions,
        "RECORD_AUDIO belongs to M3; adding it before the STT code exists misstates capability",
    )
    check(
        'android:required="false"' in text and "camera.any" in text,
        "camera hardware must stay optional so Quick Cards works on camera-less devices",
    )


def main() -> int:
    check_xml()
    check_string_parity()
    phrases = check_catalog()
    check_string_references()
    check_manifest()

    print(f"phrase catalog: {len(phrases)} phrases, version 1")
    for category in sorted(CATEGORIES):
        count = sum(1 for phrase in phrases if phrase.get("category") == category)
        print(f"  {category:10} {count}")

    if FAILURES:
        print(f"\nFAILED ({len(FAILURES)}):")
        for failure in FAILURES:
            print(f"  - {failure}")
        return 1
    print("\nOK: M1 static checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
