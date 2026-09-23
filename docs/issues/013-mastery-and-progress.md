# 013 - Mastery and progress calculation

**Phase** 2 - Engine | **Depends on** 006, 008 | **Blocks** 014, 029, 030, 031, 032, 035

## Goal

Implement mastery levels, skill levels and the progress summary exactly as specified in `docs/engine/03-mastery-and-progress.md`, derived on read with nothing stored.

## User value

Progress that reflects real practice and can be explained in one sentence, with no points and no streaks.

## Scope

- `domain/progress/Mastery.kt`: `masteryOf(...)` per section 2, and `nextLevelHint(level, facts)` per 2.1.
- `domain/progress/SkillLevels.kt`: `skillLevel(facts)` using the **second** threshold table in section 3 (the one validated against the artboard), and the detail-line rules in 3.1.
- `domain/progress/ObserveProgressUseCase.kt`: combines the DAO aggregates into `ProgressSummary` per section 4.
- `ObserveTechniqueProgressUseCase` for the library and technique detail.
- `ObserveHistoryUseCase(month)` for the history screen.
- The day-dot window (trailing 10 days) and the skill window (trailing 14 days) as separate, correctly-named concepts.

## Non-goals

- Any progress UI (issues 029-032).
- Storing or caching any derived value (forbidden by ADR-0013).

## Implementation notes

- **Nothing is stored.** No counter, no materialised table, no cache. If a query is slow, add a `@DatabaseView`, not a cached value.
- Use the **second** `skillLevel` table in section 3 (thresholds 16/11, 9/6, 4/3). The first is shown in the document only to record why it was rejected; implementing it would contradict the artboard.
- `INTEGRATED` dominates: check it before the `APPLIED` span test.
- The `APPLIED` span is `DAYS.between(firstUse, lastUse) >= 14`. 13 days is `PRACTICED`; 14 is `APPLIED`. Test both sides.
- **Interaction with issue 011**: a `FOCUS_SUGGESTION` completion before a technique's own intro must not grant mastery for that technique (the Day-1 generic focus block references `pomodoro`). Exclude `source = FOCUS_SUGGESTION` completions that predate `technique_state.introCompletedAt`. Add a named test.
- The two windows are different and both correct: day dots use 10 days (the Progress strip), skill levels use 14 (the IA board's "days practiced of the last 14"). Name the constants so they cannot be confused.
- Performance budget: Progress must render within 300 ms with ~1 500 activities. Measure it with a seeded fixture in an instrumented test, not by eye.

## Affected layers

`domain/progress`, `data/repository`.

## Acceptance criteria

- [ ] `masteryOf` matches the full truth table, including the 13/14-day boundary and the `INTEGRATED` override.
- [ ] `skillLevel` reproduces all five artboard rows: Focus (11,7) Steady, Reflection (9,9) Steady, Habits (7,5) Building, Planning (4,3) Building, Learning (3,2) Starting.
- [ ] Every `skillLevel` threshold is asserted from both sides.
- [ ] `nextLevelHint` returns the documented string for each level with the correct remaining count.
- [ ] `ProgressSummary` computes trained days over the trailing 10 days, the window start, the day dots with their skill sets, and the all-time activity count.
- [ ] Focus minutes come from `focus_session.actualSeconds`.
- [ ] A `FOCUS_SUGGESTION` completion before a technique's intro does not grant it mastery.
- [ ] An empty database yields all skills `STARTING`, 0 of 10 days, and no crash.
- [ ] Window arithmetic is correct across a month boundary and a DST change.
- [ ] Progress renders within 300 ms with 1 500 seeded activities.
- [ ] No derived value is persisted anywhere.

## Unit test expectations

`MasteryTest` - the full truth table; the `INTEGRATED` override; the 13-vs-14-day span; a locked technique; an unlocked-but-never-completed technique.

`SkillLevelTest` - the five artboard rows; every threshold from both sides; zero practices.

`ProgressSummaryTest` - month boundary; DST change; empty database; a 10-day window with gaps; the day-dot skill sets.

`MasterySourceExclusionTest` - the Day-1 generic focus interaction.

## UI test expectations

None (the screens are issues 029-032).

## Integration test expectations

- `ProgressPerformanceTest` - 1 500 seeded activities, first emission within 300 ms.
- `ProgressDerivationTest` - the Day-9 history fixture produces exactly the artboard's five skill rows.

## Manual verification

1. Seed the Day-9 fixture; dump the computed progress and compare against the Progress artboard line by line.
2. Complete an exercise and confirm the relevant skill's numbers change by exactly one.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 10, 11.
