# 009 - Exercise state machine

**Phase** 2 - Engine | **Depends on** 005 | **Blocks** 011, 014

## Goal

Implement `ActivityStateMachine` and the day-rollover rule exactly as specified in `docs/engine/00-exercise-state-machine.md`.

## User value

An activity is never in a state the user did not cause. Double taps, stale notifications and app restarts cannot corrupt the day.

## Scope

- `domain/training/ActivityStateMachine.kt`: `transition(current, event, now): Result<ActivityState>` covering the 12 valid transitions in section 3; every other pair returns `Result.failure(DomainException(IllegalTransition))`.
- The `ActivityEvent` sealed interface: `Reach`, `Start`, `Snooze(until)`, `Skip`, `Complete(result)`, `Abandon`, `DayRollover`.
- Guard evaluation as documented, including the `Skip` guard (optional activities and the reflection only).
- `becomesAvailableAt(dayPart, preferences)` per section 4.
- `domain/training/DayRollover.kt`: the pure rollover function in section 6, returning the updated day and the list of activities to expire.

## Non-goals

- Persisting transitions (issue 014 owns the transactional write).
- Post-completion effects (issue 014).
- Any UI.

## Implementation notes

- The machine is **pure**: it takes the current state plus the facts it needs (optional flag, source, `becomesAvailableAt`, `snoozedUntil`) and returns a state. It performs no I/O and holds no reference to a repository.
- Side effects listed in section 3's table are the **caller's** responsibility; document each in KDoc on the event so issue 014 cannot miss one.
- Never throw. An illegal transition is an expected outcome (`docs/architecture/05-error-handling-and-logging.md` section 1), logged at debug and ignored by callers.
- The rollover rule has a subtlety worth a named test: a day with some completions is `COMPLETE` **only if its PROGRAM activity completed**; otherwise it is `ABANDONED` regardless of how many optional items were done.
- `becomesAvailableAt` for `DAYTIME` is `max(morningTime + 90min, 11:00)` - both halves matter; test a 06:00 morning time and a 13:00 one.

## Affected layers

`domain/training`.

## Acceptance criteria

- [ ] All 12 documented transitions produce the documented target state.
- [ ] All remaining `(state, event)` pairs return `IllegalTransition` and do not throw.
- [ ] `Skip` from `AVAILABLE` or `IN_PROGRESS` fails for a required non-reflection activity.
- [ ] `Snooze` fails when `until <= now` or `until` is past the end of the local day.
- [ ] `Reach` from `SCHEDULED` fails before `becomesAvailableAt` and succeeds at or after it.
- [ ] `becomesAvailableAt` matches the table for all three day parts and for edge morning times.
- [ ] Rollover expires exactly the four non-terminal states and leaves terminal ones untouched.
- [ ] Rollover marks a day `COMPLETE` only when its program activity completed, else `ABANDONED`.
- [ ] Rollover preserves drafts on expired activities.
- [ ] The machine has no Android, Room or repository dependency.

## Unit test expectations

`ActivityStateMachineTest`:
- an exhaustive table over all 7 states x 6 events = 42 pairs, each asserted as the documented target or `IllegalTransition`. The test must be written so that adding a state or an event without extending it fails to compile or fails the assertion count check.
- each guard, from both sides of its boundary.
- `becomesAvailableAt` for morning times 06:00, 08:30 and 13:00.

`DayRolloverTest`:
- a day with nothing done -> `ABANDONED`, all expired;
- a day fully done -> `COMPLETE`, nothing expired;
- a day with the program done and the optional not -> `COMPLETE`;
- a day with the optional done and the program not -> `ABANDONED`;
- drafts survive expiry.

## UI test expectations

None.

## Manual verification

None - this issue has no user-visible surface. Verified through issue 014.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 10, 11.
