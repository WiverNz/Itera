# 039 - Test hardening and CI gates

**Phase** 7 - Hardening | **Depends on** 035, 036, 041 | **Blocks** 040

## Goal

Close the gap between the test matrix and what actually exists, and turn the project's guarantees into build failures rather than intentions.

## User value

None directly. It is what stops the next change from quietly breaking the product's promises.

## Scope

- **Audit** `docs/testing/01-test-matrix.md` row by row. Every row must have a test that exists and passes. Write the missing ones.
- Verify `domain` line coverage is at or above 85 %; write tests for the gaps rather than lowering the gate.
- Complete the cross-cutting guarantee tests: `ArchitectureTest` (all seven rules), `NoStreakLanguageTest`, `NoUserTextLoggedTest`, `ResetCoverageTest`, `OfflineTest`, `CatalogValidationTest`, `CatalogCompatibilityTest`.
- Complete the integration suite in the matrix's Integration section, especially the ones no single issue owned end to end: the five-day gap, the full daily loop, review creation to grading, and the combination day granting `INTEGRATED`.
- **Set up golden images** per `docs/testing/04-visual-regression.md`: Roborazzi + Robolectric, a pinned device and font configuration, and the full set recorded from the signed-off production screens - 24 screens x 2 themes x 2 font scales in English, **plus a 24-image Russian subset** (light, default scale) to protect the Cyrillic font path (ADR-0019), plus one per design-system component. Wire `verifyRoborazziDebug` into CI.
- Add the instrumented CI job (`docs/testing/03-ci.md` section 1) on pull requests to `main` and nightly.
- Add the schema-change guard: a check that a changed `app/schemas` diff implies a new `Migration`.
- Fix or delete every flaky test; none may be `@Ignore`d without a linked issue.
- Disable animations in the instrumented runner.
- Produce a coverage and gap report appended to this file.

## Non-goals

- Rewriting existing passing tests.
- Adding a coverage gate outside `domain` (deliberately - `docs/testing/00-strategy.md` section 8).
- Re-establishing visual fidelity. Goldens **protect** the result that each screen issue already agreed against the prototype; they do not define it. A screen that never matched the prototype is not fixed by recording a golden of it.

## Implementation notes

- Start from the matrix, not from the code. The point of this issue is to find what nobody wrote, and the matrix is the list of what should exist.
- The likeliest gaps are the integration tests that span issues: no single issue owns "a five-day gap does not advance the program" end to end, though issues 014 and 017 each own half of it.
- `ArchitectureTest` must have both a positive and a negative fixture per rule. A boundary test that has never failed is a boundary test that might not work.
- Flakiness sources to expect: Compose idling with the focus timer (use `mainClock.autoAdvance = false`), and WorkManager timing (use `TestDriver`). Both have deterministic approaches; do not add retries.
- The schema-change guard can be a simple Gradle task comparing the newest schema JSON's version against the registered `Migration` array.
- If coverage is short of 85 %, the correct fix is tests for untested rules, not excluding files from the metric.

## Affected layers

`src/test`, `src/androidTest`, `build.gradle.kts`, CI workflow.

## Acceptance criteria

- [ ] Every row in `docs/testing/01-test-matrix.md` has a test that exists and passes.
- [ ] `domain` line coverage is at or above 85 %, enforced by `koverVerify` in CI.
- [ ] All seven `ArchitectureTest` rules have positive and negative fixtures.
- [ ] `NoStreakLanguageTest`, `NoUserTextLoggedTest`, `ResetCoverageTest`, `OfflineTest`, `CatalogValidationTest` and `CatalogCompatibilityTest` all exist and pass.
- [ ] The full integration suite passes on an emulator.
- [ ] The instrumented CI job runs on pull requests to `main` and nightly, and fails the build on failure.
- [ ] The schema-change guard fails when a schema changes without a migration.
- [ ] The golden set is recorded, committed, and `verifyRoborazziDebug` passes and fails correctly (verify by nudging a padding).
- [ ] Goldens are reproducible: two runs on different machines produce identical images.
- [ ] No test is `@Ignore`d without a linked issue.
- [ ] No test passes only on a retry.
- [ ] Animations are disabled in the instrumented runner.
- [ ] Lint, formatting, unit tests and coverage all gate the build.
- [ ] The coverage and gap report is appended to this file.

## Unit test expectations

Whatever the audit finds missing, plus the six guarantee tests above.

## UI test expectations

Whatever the audit finds missing, plus `FontScaleTest` and `AccessibilityAuditTest` (issue 037) and `LocalizedRenderTest` and `PseudoLocaleTest` (issue 041) wired into CI.

## Integration test expectations

`GapIntegrationTest` - five calendar days pass with no use; on return the program day is unchanged, the missed curriculum day is offered, overdue reviews are due once, and Progress shows hollow dots with no warning colour.

Plus every other row in the matrix's Integration section.

## Manual verification

1. Open a pull request with a deliberately broken unit test; CI fails.
2. Change a schema without a migration; the guard fails.
3. Run the full instrumented suite locally twice; identical results.
4. Introduce a boundary violation; `ArchitectureTest` fails with a useful message.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections, with section 5 fully evidenced by the appended report.

---

## Coverage and gap report

*(To be filled in during implementation: matrix rows audited, tests added, coverage before and after, flaky tests found and resolved.)*
