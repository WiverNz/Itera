# Release milestones

Four milestones. Each is independently demonstrable and has an exit condition that can be checked, not judged.

## M1 - Skeleton (issues 001-008)

**Demonstrates**: the app builds, is themed, persists data and knows its content.

What a reviewer sees: a debug build launching to a placeholder screen rendered with real design-system components, plus a green test run that includes catalog validation and architecture checks.

**Exit**:
- `./gradlew build` passes from a clean checkout.
- The database opens, seeds `technique_state`, and survives a restart.
- Preferences round-trip.
- All 14 techniques and the 14-day curriculum parse and pass validation.
- `ArchitectureTest` passes.

**Risk**: dependency-version friction with AGP 9 / Kotlin 2.2 / KSP. Resolve in issue 001 before anything else starts.

## M2 - The loop runs (issues 009-020)

**Demonstrates**: the product's core idea, end to end, with template techniques.

What a reviewer sees: a clean install, onboarding, a Day 1 exercise completed with a rating and a note, an evening reflection, the day-complete screen, and Day 2 opening with the carry-over banner.

**Exit**:
- Acceptance criteria groups A, B and D1-D5 pass (`docs/prd/09-mvp-acceptance-criteria.md`).
- The engine's four worked plan examples pass as unit tests.
- Process death mid-exercise restores the draft.
- `programDay` advances exactly once per completed day and not at all after a skipped day.

**Risk**: the plan generator is the most rule-dense piece in the app. It is on the critical path and should be reviewed against `docs/engine/01-training-plan-engine.md` line by line.

## M3 - Feature complete (issues 021-036)

**Demonstrates**: every technique, every screen, reminders, settings, export.

What a reviewer sees: Days 1-14 completable with their real experiences, all four tabs populated, a review falling due and being graded, a combination day granting `Integrated`, a morning reminder arriving and deep-linking, a journal exported to a readable file, and the whole app switched to Russian and back without a restart.

**Exit**:
- Every screen in `docs/ux/00-screen-inventory.md` exists and has passed a side-by-side comparison against `design/`.
- The app runs fully in English, Russian, German and Spanish.
- Acceptance criteria groups C, D, E, F, G and H pass.
- Every test in `docs/testing/01-test-matrix.md` for issues 001-036 exists and passes.
- No feature requires a network connection.

**Risk**: the focus timer's process-death and foreground-service behaviour is the most device-dependent part of the app. Test it on the oldest supported device early in the phase, not at the end.

## M4 - Ready to ship (issues 037, 041, 039, 040)

**Demonstrates**: the app is accessible, localisable, tested and releasable.

What a reviewer sees: the full TalkBack script completed, every screen at `fontScale 2.0` in English and German, all four languages rendering correctly, a recorded golden set with `verifyRoborazziDebug` green in CI, and a signed release build passing the manual QA checklist on two devices.

**Exit**:
- All of `docs/prd/09-mvp-acceptance-criteria.md` passes.
- `docs/testing/02-manual-qa-checklist.md` passes on two devices.
- `docs/delivery/03-release-checklist.md` is complete.
- A signed artifact installs, upgrades cleanly, and runs.

**Risk**: R8 breaking serialization, Hilt or the AppCompat locale service in the release build. Build and smoke-test a minified release build during M3, not for the first time in M4.

**Second risk**: recording goldens too early. They are recorded in issue 039, after accessibility and localisation have stopped moving layouts - otherwise everyone learns to re-record without looking.

## Sequencing notes

- M1 and M2 are strictly sequential.
- Within M3, issues 021-026 are six parallel tracks; 028-032 are largely parallel; 033-036 depend on the loop existing but not on the specialised exercises.
- M4 cannot start meaningfully until every UI screen exists, because the accessibility and localisation passes sweep all of them.

## What is deliberately not a milestone

There is no "beta with a subset of techniques" milestone. The curriculum is the product; shipping seven of fourteen techniques would not demonstrate the idea, because the combination days - which are the point of the second week - need the first week's techniques to exist.
