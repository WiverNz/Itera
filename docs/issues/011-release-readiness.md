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
- [ ] Merged permissions equal the reviewed set pinned in `OfflineTest` (012's `RECORD_AUDIO`; 013's Play Asset Delivery `FOREGROUND_SERVICE_DATA_SYNC`, declared in Play Console); no `INTERNET` or microphone foreground service. Voice follows the amended ADR-0022 chain (on-device, consented system default, user-chosen installed provider); Privacy copy, consent, picker, Settings and unavailable/denied paths match it.
- [ ] The Play base download is under 12 MB (013 measured 6.97 MB for arm64; the voice-model packs are on-demand and excluded); start-up and scroll measurements are recorded.
- [ ] From 013: decide universal sideload APK size / ABI packaging (45.3 MB with uncompressed `libvosk.so` for four ABIs; options include compressed native libs, per-ABI APKs, dropping x86 from release).
- [ ] From 013: verify real Play Console on-demand delivery of the `voice_model_<lang>` packs (Download, progress, install, Remove); 013 verified with bundletool local testing only.
- [ ] `docs/prd/09-mvp-acceptance-criteria.md` passes in full.
- [ ] `docs/testing/02-manual-qa-checklist.md` passes on two devices.
- [ ] `docs/delivery/03-release-checklist.md` is complete.

## Required tests

The full unit, Robolectric, golden and instrumented suites against the release configuration where applicable; the manual QA checklist on two devices.
