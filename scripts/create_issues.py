#!/usr/bin/env python3
"""Post the milestone issue drafts in .github/ISSUE_DRAFTS/ to GitHub.

Drafts are Markdown files with YAML-ish frontmatter:

    ---
    title: "M0 — ..."
    labels: milestone-m0
    ---
    body...

Skips any issue whose title already exists, so re-running is safe.
Dry-run by default: prints what it would do and posts nothing.

Usage:
    python scripts/create_issues.py                  # dry run
    python scripts/create_issues.py --apply          # create the issues
    python scripts/create_issues.py --apply --repo owner/name --token $GITHUB_TOKEN

Token resolution order: --token, $GITHUB_TOKEN, $GH_TOKEN, then the credential
already stored by Git Credential Manager for github.com.

The token needs `issues: write` on the target repo. A read-only collaborator or a
token without repo scope gets 403/404 — the script prints the GitHub message.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

DRAFTS = Path(__file__).resolve().parent.parent / ".github" / "ISSUE_DRAFTS"


def parse_draft(path: Path) -> tuple[str, list[str], str]:
    text = path.read_text(encoding="utf-8")
    match = re.match(r"^---\n(.*?)\n---\n(.*)$", text, re.S)
    if not match:
        raise SystemExit(f"{path.name}: missing frontmatter block")
    header, body = match.group(1), match.group(2).strip()
    title = ""
    labels: list[str] = []
    for line in header.splitlines():
        key, _, value = line.partition(":")
        key, value = key.strip(), value.strip().strip('"')
        if key == "title":
            title = value
        elif key == "labels":
            labels = [label.strip() for label in value.split(",") if label.strip()]
    if not title:
        raise SystemExit(f"{path.name}: frontmatter has no title")
    return title, labels, body


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], capture_output=True, text=True, check=True
    ).stdout.strip()


def resolve_repo(explicit: str | None) -> str:
    if explicit:
        return explicit
    url = git("remote", "get-url", "origin")
    match = re.search(r"github\.com[:/](?P<owner>[^/]+)/(?P<repo>[^/]+?)(?:\.git)?$", url)
    if not match:
        raise SystemExit(f"cannot derive owner/repo from remote {url!r}; pass --repo")
    return f"{match.group('owner')}/{match.group('repo')}"


def resolve_token(explicit: str | None) -> str:
    for candidate in (explicit, os.environ.get("GITHUB_TOKEN"), os.environ.get("GH_TOKEN")):
        if candidate:
            return candidate
    try:
        out = subprocess.run(
            ["git", "credential", "fill"],
            input="protocol=https\nhost=github.com\n\n",
            capture_output=True,
            text=True,
            check=True,
            timeout=30,
        ).stdout
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired):
        raise SystemExit("no token: pass --token or set GITHUB_TOKEN")
    for line in out.splitlines():
        if line.startswith("password="):
            return line.split("=", 1)[1]
    raise SystemExit("no token: Git credential manager returned no password")


def api(method: str, path: str, token: str, payload: dict | None, repo: str) -> tuple[int, dict | list]:
    url = f"https://api.github.com/repos/{repo}{path}"
    data = json.dumps(payload).encode() if payload is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Authorization", f"Bearer {token}")
    request.add_header("Accept", "application/vnd.github+json")
    request.add_header("X-GitHub-Api-Version", "2022-11-28")
    if data:
        request.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status, json.loads(response.read() or b"{}")
    except urllib.error.HTTPError as error:
        try:
            return error.code, json.loads(error.read() or b"{}")
        except json.JSONDecodeError:
            return error.code, {"message": error.reason}
    except urllib.error.URLError as error:
        raise SystemExit(f"network error reaching api.github.com: {error.reason}")


def existing_titles(repo: str, token: str) -> set[str]:
    titles: set[str] = set()
    page = 1
    while True:
        status, body = api("GET", f"/issues?state=all&per_page=100&page={page}", token, None, repo)
        if status != 200:
            raise SystemExit(f"cannot list issues ({status}): {body}")
        batch = [item for item in body if "pull_request" not in item]
        titles.update(item["title"] for item in batch)
        if len(body) < 100:
            return titles
        page += 1


def main(argv: list[str]) -> int:
    apply = "--apply" in argv
    repo = resolve_repo(_arg(argv, "--repo"))
    token = resolve_token(_arg(argv, "--token"))

    drafts = sorted(DRAFTS.glob("*.md"))
    if not drafts:
        raise SystemExit(f"no drafts in {DRAFTS}")
    print(f"repo: {repo} ({len(drafts)} drafts, {'applying' if apply else 'dry run'})")

    titles = existing_titles(repo, token) if apply else set()
    created = 0
    for path in drafts:
        title, labels, body = parse_draft(path)
        if title in titles:
            print(f"  SKIP  {path.name}: already open/closed as {title!r}")
            continue
        if not apply:
            print(f"  WOULD CREATE  {title}   labels={labels}")
            continue
        status, response = api(
            "POST", "/issues", token, {"title": title, "body": body, "labels": labels}, repo
        )
        if status == 422 and labels:
            status, response = api("POST", "/issues", token, {"title": title, "body": body}, repo)
        if status not in (200, 201):
            print(f"  FAIL  {path.name}: HTTP {status} {response.get('message') if isinstance(response, dict) else response}")
            continue
        created += 1
        print(f"  CREATED  {response['html_url']}")

    if not apply:
        print("\ndry run: nothing posted. Re-run with --apply to create the issues.")
    else:
        print(f"\ncreated {created} issue(s)")
    return 0


def _arg(argv: list[str], flag: str) -> str | None:
    if flag in argv:
        index = argv.index(flag)
        if index + 1 < len(argv):
            return argv[index + 1]
    return None


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
