# 011 - Daily plan generator

**Phase** 2 - Engine | **Depends on** 006, 007, 008, 009, 010 | **Blocks** 014, 016, 017, 033

## Goal

Implement `GenerateDailyPlanUseCase` exactly as specified in `docs/engine/01-training-plan-engine.md`, including the four worked examples.

## User value

The single most important behaviour in the product: the user opens the app and one right thing is waiting.

## Scope

- `domain/training/GenerateDailyPlanUseCase.kt` implementing the 11-step algorithm in section 3.
- `EnsureTodayPlanUseCase`: the idempotent entry point Today calls in `init`.
- `reviewCap(pace)`, `focusSuggestionAllowed()`, `focusTechnique()`, `practiceTechnique()` as specified in sections 3.1-3.3.
- Copy-key assignment per section 3.4, including the two special cases: `activity_focus_generic` for the Day-1 focus block, and the rule that a PROGRAM focus-timer technique suppresses the focus suggestion in favour of a practice prompt.
- `orderIndex` assignment by `(dayPart, source priority, id)`.
- Transactional persistence of the day plus its activities.
- Day 15+ generation per section 7, including generated combinations (7.1).
- Regeneration guard per section 5 (`generatorVersion`).
- Weekly look-back flag per section 8.
- Rollover of the previous day before generating (delegating to issue 009's pure function).

## Non-goals

- The Today UI (issue 017).
- Completing activities or advancing the program (issue 014).
- Scheduling notifications from the generated plan (issue 033).

## Implementation notes

- **No randomness anywhere.** Every choice has a documented deterministic rule with an explicit tiebreak (ADR-0011). A `Random`, a `hashCode()` ordering, or an unstable `sortedBy` are all bugs.
- Injected `Clock` only - no ambient time.
- Idempotence is enforced by `training_day.date` being `UNIQUE`; the use case checks for an existing row first, and the unique index is the backstop against a race between the worker and the UI.
- The four worked examples in section 4 are the acceptance test. Write them as fixtures first and make them pass byte-for-byte, including `orderIndex`, `copyKey` and `copyArgs`.
- The Day-1 subtlety is documented and deliberate: the focus suggestion references `pomodoro` but uses `activity_focus_generic` copy and does **not** grant Pomodoro mastery. Completing it must produce an activity whose `techniqueId` is `pomodoro` but whose `source` is `FOCUS_SUGGESTION` - and issue 013's mastery query must therefore not count `FOCUS_SUGGESTION` completions before the technique's own intro. Add an explicit test for that interaction and note it in issue 013.
- `practiceTechnique()` ordering is a four-level comparator; write it as an explicit `compareBy` chain, not as a hand-rolled loop, so it is readable and obviously total.
- A combination day's steps each become their own `plan_activity` row with `source = COMBINATION`, plus a parent row of `exerciseType = COMBINATION`. Get this right here; issues 013 and 027 both depend on it.

## Affected layers

`domain/training`, `data/repository`.

## Acceptance criteria

- [ ] The Day 1, Day 2, Day 9 and Day 14 examples in section 4 are reproduced exactly, including order, copy keys and arguments.
- [ ] Calling the use case twice for the same date produces one `training_day` row and returns the same plan.
- [ ] Given identical inputs, 100 runs produce byte-identical output.
- [ ] `reviewCap` per pace: Gentle 1, Standard 2, Intense 3; overflow reviews remain due.
- [ ] A locked technique never appears in a plan, across 1 000 generated days with randomised preferences.
- [ ] Every generated day has exactly one program-or-combination activity and exactly one reflection.
- [ ] Every generated day has at most one optional activity; a due review displaces it.
- [ ] A PROGRAM focus-timer technique suppresses the focus suggestion.
- [ ] `timeBudget = SHORT` produces no focus suggestion.
- [ ] Day 15+ produces a combination every third day and a deepening practice otherwise, for all three paces.
- [ ] A generated combination picks three techniques at `PRACTICED`+ from three different skills, or falls back to a single practice.
- [ ] A plan is not regenerated when preferences change mid-day.
- [ ] Day 7 and Day 14 set the weekly-look-back flag on the reflection.
- [ ] The previous day is rolled over before a new plan is generated.

## Unit test expectations

`GenerateDailyPlanUseCaseTest` - every acceptance criterion above, with fixtures. Specifically:
- the four worked examples, asserted field by field;
- idempotence and determinism;
- review cap per pace, including overflow remaining due;
- `focusSuggestionAllowed` truth table;
- `practiceTechnique` selection across all four tiebreak levels, including the case where every candidate is excluded;
- Day 15+ for days 15-30 at each pace;
- generated combination with 2, 3 and 5 eligible techniques;
- no locked technique across 1 000 randomised days;
- the Day-1 generic-focus interaction.

## UI test expectations

None.

## Integration test expectations

- `PlanPersistenceTest` - generate, reopen the database, observe the same plan; a concurrent second generation does not create a duplicate row.

## Manual verification

1. With a seeded database at each of program days 1, 2, 9 and 14, dump the generated plan and compare against the artboards.
2. Change pace and confirm only the optional activity and review count change.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 10, 11.
