# 010 - Unlock rules

**Phase** 2 - Engine | **Depends on** 005, 008 | **Blocks** 011, 028

## Goal

Implement the unlock rule and the unlock table from `docs/engine/04-unlock-rules.md`.

## User value

Techniques arrive one at a time, in a sensible order, and never disappear once met.

## Scope

- `domain/unlock/UnlockRules.kt`: `shouldUnlock(technique, programDay)`, and `unlockedSet(catalog, programDay)` returning the complete set for a given day.
- `unlocksOnDay(techniqueId)` for the library's "Day {n}" chip.
- `UnlockTechniquesUseCase`: given the current program day, unlock any catalog technique whose `introDay` has been reached and which is still locked, writing `unlockedAt` and `unlockedOnProgramDay`. Idempotent.
- Seed behaviour: `daily_reflection` is unlocked at seed time with `introDay = null`.

## Non-goals

- Calling this from the plan engine (issue 011 does).
- The library's locked-row rendering (issue 029).
- Any performance or mastery gate - there is none, deliberately.

## Implementation notes

- There is **no** performance gate. Difficulty ratings, recall grades and missed days do not affect unlocking. Resist any temptation to add one; it would contradict the product's stated stance and `docs/prd/00-product-brief.md`.
- Unlocking is monotone: once `unlockedAt` is set it is only cleared by a program reset.
- `unlockedSet` must be cheap - it is called on every plan generation and on every library render. It is a filter over an already-cached catalog plus one DAO read.
- Guard against a catalog whose `introDay` values have shifted for an in-flight user: unlocking is driven by `introDay <= currentProgramDay`, so a technique whose day moved *earlier* unlocks immediately, and one that moved *later* stays unlocked if already met (ADR-0015).

## Affected layers

`domain/unlock`, `data/repository` (the `TechniqueStateDao` write path).

## Acceptance criteria

- [ ] `unlockedSet` for program days 1 through 20 matches the table in `docs/engine/04-unlock-rules.md` section 2 exactly.
- [ ] `daily_reflection` is unlocked at every program day including 1.
- [ ] `UnlockTechniquesUseCase` is idempotent: running it twice for the same day performs one write.
- [ ] An already-unlocked technique is never re-locked by the use case.
- [ ] `unlockedOnProgramDay` records the day the unlock actually happened, not the technique's `introDay` (these differ when a user skips days or content shifts).
- [ ] The function is pure and Android-free.

## Unit test expectations

`UnlockRulesTest`:
- exact expected unlocked set for each program day 1..20;
- `daily_reflection` always unlocked;
- an unlocked technique survives an abandoned day;
- a technique whose `introDay` moved later stays unlocked;
- a technique whose `introDay` moved earlier unlocks on next reconciliation;
- `unlocksOnDay` returns the catalog value for a locked technique and `null` for `daily_reflection`.

`UnlockTechniquesUseCaseTest`:
- first run at day 5 unlocks days 1-5 and writes five rows;
- second run writes nothing;
- `unlockedOnProgramDay` is the current day, not `introDay`, when a user jumps from day 1 to day 5.

## UI test expectations

None.

## Manual verification

None directly - verified through issue 011 and issue 029.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 10, 11.
