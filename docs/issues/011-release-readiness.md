# 011 - Release readiness

**Depends on** 010 | **Blocks** -

## Goal

Produce a signed, minified release build that passes the MVP acceptance criteria and the manual QA checklist.

## Included scope

Old 040 unchanged in intent: release build type (minify, shrink, not debuggable), R8 keep rules, `resConfigs` covering all four languages, signing from local/environment config (nothing secret committed), manifest and permission audit, performance and size checks, release-build verification in all four languages, versioning, archival of `mapping.txt` and the schema.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/040-release-readiness.md`.
- `docs/delivery/03-release-checklist.md`; `docs/prd/09-mvp-acceptance-criteria.md`; `docs/testing/02-manual-qa-checklist.md`; `docs/prd/04-nonfunctional-requirements.md`.

## Key dependencies

- Needs 010 (green CI and goldens).
- Tagging the release is left to the user (AGENTS.md: agents do not create tags).

## Acceptance criteria

- [ ] The minified release build installs, upgrades cleanly and runs the daily loop in all four languages.
- [ ] Permissions are exactly the documented five, including 012's `RECORD_AUDIO`; no `INTERNET` or microphone foreground service. Voice follows the amended ADR-0022 chain (on-device, consented system default, user-chosen installed provider); Privacy copy, consent, picker, Settings and unavailable/denied paths match it.
- [ ] APK/AAB is under 12 MB; start-up and scroll measurements are recorded.
- [ ] `docs/prd/09-mvp-acceptance-criteria.md` passes in full.
- [ ] `docs/testing/02-manual-qa-checklist.md` passes on two devices.
- [ ] `docs/delivery/03-release-checklist.md` is complete.

## Required tests

The full unit, Robolectric, golden and instrumented suites against the release configuration where applicable; the manual QA checklist on two devices.
