# 010 - Test hardening

**Depends on** 009 | **Blocks** 011

## Goal

Close any gap between the test matrix and the tests that exist, record the golden-image set once layouts have settled, and turn the project's guarantees into CI failures.

## Included scope

Old 039 unchanged in intent:

- Audit `docs/testing/01-test-matrix.md` row by row (its Issue column uses the detailed numbering - see `docs/issues/README.md` for the mapping); write any missing test.
- Hold `domain` coverage at or above 85 %.
- Complete the cross-cutting guarantee tests and the integration suite, especially flows no single milestone owned end to end (five-day gap, full daily loop, review creation to grading, combination day).
- Record the full Roborazzi golden set per `docs/testing/04-visual-regression.md`.
- Add the instrumented CI job, the schema-change guard, and disable animations in the instrumented runner.
- Remove flakiness; nothing `@Ignore`d.
- Include milestone 012's voice matrix and shared-state goldens (including the ADR-0022 system-consent, recognition-app picker and named-consent states, and the Settings Voice section), with fake recognition in CI and device checks for real service/model availability. “Milestone 012” rows are not historical detailed issue 012.

## Carried over from 009 (2026-09-28)

Manual verification 009 did not finish. Record findings and fixes only; do not repeat 013's device testing.

- [ ] Full TalkBack and Switch Access daily loops, including voice confirmations and the 012/013 voice panels and Settings → Voice sheets.
- [ ] Remaining maximum font and display-size (and grayscale) checks on supporting surfaces beyond the automated `FontScaleTest`/`LocalizedRenderTest` coverage.
- [ ] Remaining prototype side-by-side comparisons from 005, 006 and 012 (light and dark).
- [ ] Exhaustive EN/RU/DE/ES runtime walks and language persistence across force-stop.
- [ ] Reboot and process-death verification (reminders rescheduled, focus timer and drafts restored).
- [ ] API 26-30 language/timer checks, **if a compatible environment becomes available** (none installed: images 36.1 and 37.2 only; not blocking otherwise).
- [ ] Voice checks still relevant after 013: the first-syllable loss when speaking at the instant of the tap (013 closing decision); the external-provider chain (system consent, app picker, named consent) on a device that has such providers working.

Already settled by 013 and not to be repeated: real offline-model behaviour in RU/EN/DE (dictation, grammar commands, import, removal, damaged-model fall-through, language switching, Play Asset Delivery local testing). Spanish device testing is waived; small Vosk models are accepted and EN/DE free-form dictation is best-effort. Play Console delivery and universal APK packaging belong to [011](011-release-readiness.md).

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/039-test-hardening.md`.
- `docs/testing/00-strategy.md`, `01-test-matrix.md`, `03-ci.md`, `04-visual-regression.md`.

## Key dependencies

- Needs 009: goldens are recorded only after accessibility and localisation have stopped moving layouts.

## Acceptance criteria

- [ ] Every row of the test matrix maps to an existing, passing test.
- [ ] `domain` coverage is at least 85 % and enforced by the build.
- [ ] The golden set is recorded and `verifyRoborazziDebug` is green in CI.
- [ ] The instrumented CI job runs on pull requests to `main` and nightly.
- [ ] A schema change without a migration fails the build.
- [ ] No test is flaky or ignored.
- [ ] A short gap summary (what was missing, what was added) is recorded in this file.
- [ ] From 013: speaking at the instant of the mic tap can lose the first syllable with the offline recogniser (seen on a vivo, RU); reproduce, find the cause and fix or document it. Until then, recognition should begin after the Listening state appears.

## Required tests

`GapIntegrationTest` and every missing matrix row; the full golden set; the instrumented suite in CI.
