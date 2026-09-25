# 004 - Content & training engine

**Depends on** 002 | **Blocks** 005, 006, 007, 008

**Status (2026-09-25): complete.** All acceptance criteria are met and every required test passes (`./gradlew build`). Decisions and deviations are recorded in `docs/00-source-of-truth.md`, "Milestone 004 implementation decisions". Gentle pace keeping the curriculum rate is now the final product decision (pace affects review workload and focus suggestions only).

## Goal

Load the technique catalogue and curriculum, and implement every product rule of the daily loop - state machine, unlocks, plan generation, spaced repetition, mastery/progress and the transactional day lifecycle - as pure, tested domain code with no UI.

## Included scope

- **Catalog** (old 008): `techniques.json` + curriculum assets, parsing, validation, caching, `ContentReconciler`, seeding, the 14 techniques' content in all four languages.
- **Exercise state machine** (old 009): transitions, availability by day part, rollover.
- **Unlock rules** (old 010): `unlockedSet`, `UnlockTechniquesUseCase`.
- **Daily plan generator** (old 011): `GenerateDailyPlanUseCase` and persistence, including Day 15+.
- **Spaced repetition** (old 012): `ReviewScheduler`, `ScheduleReviewUseCase`, `SubmitReviewUseCase`.
- **Mastery and progress** (old 013): `masteryOf`, `skillLevel`, `ProgressSummary`, derived on read (ADR-0013).
- **Day lifecycle** (old 014): `CompleteActivityUseCase` with all seven effects in one transaction, `AdvanceProgramDayUseCase`, snooze/skip/abandon/draft/manual-practice/rollover use cases, `ResetProgramUseCase`, `EraseAllDataUseCase`.
- **`ReminderScheduler` interface with a no-op binding** - the seam that 005, 006 and 008 call and 009 replaces with the real implementation.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/008-technique-catalog.md` through `014-day-lifecycle.md`.
- `docs/engine/00-exercise-state-machine.md`, `01-training-plan-engine.md`, `02-spaced-repetition.md`, `03-mastery-and-progress.md`, `04-unlock-rules.md`.
- `docs/data/04-technique-catalog-format.md`, `05-migrations-and-content-versioning.md`; `docs/prd/05-content-and-technique-model.md`, `07-technique-curriculum.md`, `08-content-style-guide.md`.
- ADR-0005, 0011, 0012, 0013, 0014, 0015; `docs/00-source-of-truth.md` D-13 (technique content).
- Prototype (content source only): `design/app/src/main/java/com/itera/app/model/Model.kt`, `design/app/src/main/res/values*/strings.xml`.

## Key dependencies

- Needs 002 (domain model, Room, DataStore, `Analytics` for the `exercise_completed` effect).
- Independent of 003; 003 and 004 can be done in either order.

## Acceptance criteria

- [x] Both assets parse and validate; 14 techniques with complete content in en/ru/de/es; curriculum days 1-14 match the spec.
- [x] The state machine produces every documented transition and rejects every other pair without throwing; rollover is correct.
- [x] `unlockedSet` for days 1-20 matches the table; unlocks are idempotent and never reverse.
- [x] The four worked plan examples (Days 1, 2, 9, 14) are reproduced exactly; generation is idempotent and deterministic; no locked technique ever appears.
- [x] The review ladder, retirement, overdue and ordering rules hold; replayed submits write once.
- [x] Mastery and skill-level truth tables hold, including boundaries; nothing derived is persisted.
- [x] Completion is atomic and idempotent; `programDay` advances exactly once per completed day and never after an abandoned day.
- [x] Both reset paths cover every table (`ResetCoverageTest`).
- [x] The full daily loop runs from a seeded database under `FakeClock`, with no Activity.

## Required tests

`CatalogParsingTest`, `CatalogValidationTest`, `CatalogCompatibilityTest`, `CatalogCachingTest`, `ContentReconcilerTest`, `ActivityStateMachineTest`, `DayRolloverTest`, `UnlockRulesTest`, `UnlockTechniquesUseCaseTest`, `GenerateDailyPlanUseCaseTest`, `PlanPersistenceTest`, `ReviewSchedulerTest`, `ScheduleReviewUseCaseTest`, `SubmitReviewUseCaseTest`, `MasteryTest`, `MasterySourceExclusionTest`, `SkillLevelTest`, `ProgressSummaryTest`, `ProgressDerivationTest`, `ProgressPerformanceTest`, `CompleteActivityUseCaseTest`, `AdvanceProgramDayUseCaseTest`, `TransactionAtomicityTest`, `ResetProgramUseCaseTest`, `EraseAllDataUseCaseTest`, `ResetCoverageTest`, `ResetIntegrationTest`, `DailyLoopIntegrationTest`.
