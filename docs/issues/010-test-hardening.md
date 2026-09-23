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

## Required tests

`GapIntegrationTest` and every missing matrix row; the full golden set; the instrumented suite in CI.
