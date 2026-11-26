# Saigai docs

Status: current
Owner: Project Lead (Saigai)
Verified: 2026-11-26 · M1 commit
Scope: Index of every authored doc in this repo, with its colour (authority level).

Colours follow the Yazhi doc convention: **green** = living state, **blue** = frozen contract, **orange** = provisional with an expiry.

| Doc | Colour | Kind | Read it when |
| --- | --- | --- | --- |
| [`README.md`](../README.md) | green | Product overview | You are new to the repo (≤100 lines, links out). |
| [`CONTEXT.md`](../CONTEXT.md) | green | Current state | You need to know what is actually implemented. |
| [`decisions/DESIGN_DOC_B1.md`](decisions/DESIGN_DOC_B1.md) | blue | Frozen contract v1.0 | You are implementing or reviewing against the MVP contract. Never edit in place; supersede with v1.1. |
| [`guides/BUILD_AND_RUN.md`](guides/BUILD_AND_RUN.md) | green | How-to / runbook | You are building the APK or running device checks. |

## Not docs

- `app/src/main/res/raw/phrases.json` — the phrase catalog is **data**, owned by the M1 code and validated by `scripts/check_m1.py`. Do not copy the table into a Markdown file; link here instead.
- `.github/ISSUE_DRAFTS/` — milestone issues waiting to be posted, with the frontmatter that `scripts/create_issues.py` reads.
