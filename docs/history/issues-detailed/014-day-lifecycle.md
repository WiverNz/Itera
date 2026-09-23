# 014 - Day lifecycle and completion effects

**Phase** 2 - Engine | **Depends on** 009, 011, 012, 013 | **Blocks** 017, 018, 019, 020, 033

## Goal

Implement `CompleteActivityUseCase` with all seven post-completion effects, `AdvanceProgramDayUseCase`, and the remaining transactional write paths, per `docs/engine/00-exercise-state-machine.md` section 5.

## User value

Finishing an exercise does everything it should - records it, schedules the review, advances the program - atomically, once.

## Scope

- `domain/training/CompleteActivityUseCase.kt`: validates the transition through `ActivityStateMachine`, persists the result, and fires all seven effects in one database transaction:
  1. set `introCompletedAt` on first completion;
  2. schedule a review when eligible;
  3. insert the `focus_session` row;
  4. insert/replace the `habit_stack` row;
  5. insert a `MANUAL` activity for a premortem mitigation;
  6. complete the day and advance `programDay` when every required activity is terminal;
  7. append the `exercise_completed` analytics event.
- `AdvanceProgramDayUseCase`: the **only** writer of `current_program_day`, idempotent per day.
- `SnoozeActivityUseCase`, `SkipActivityUseCase`, `AbandonActivityUseCase`, `SaveDraftUseCase`, `AddManualPracticeUseCase`.
- `RollOverDayUseCase`: applies issue 009's pure rollover and persists it.
- `ResetProgramUseCase` and `EraseAllDataUseCase` per ADR-0014, each in one transaction, cancelling scheduled work and rescheduling afterwards.

## Non-goals

- Any UI.
- Notification scheduling itself (issue 033 provides `ReminderScheduler`; this issue calls a no-op binding until then, then wires it).

## Implementation notes

- **Atomicity is the point.** All seven effects and the state write happen inside one `withTransaction`. A partial completion - result saved but review not scheduled - is the worst failure mode this issue can produce.
- **Idempotence is the second point.** Effect 6 must be safe to run on an already-`COMPLETE` day. A replayed tap, a re-delivered notification and a retried worker must all be harmless. Test each.
- `AdvanceProgramDayUseCase` guards on `training_day.completedAt` already being set. It is the only place `current_program_day` is written outside reset; enforce with the visibility rule from issue 007 and a test.
- Effect 2 delegates to issue 012's `ScheduleReviewUseCase`; do not reimplement the ladder here.
- Effect 5 creates a new activity in **today's** plan, which may be a different day than the premortem if the user completes it near midnight. Use the activity's own `trainingDayId`, and if that day is no longer today, attach to today's plan instead. Document the choice in a comment.
- `ResetProgramUseCase` and `EraseAllDataUseCase` must cover every table. `ResetCoverageTest` enumerates the database's tables and fails if one is in neither tier - write that test in this issue.
- Reset must cancel all `WorkManager` unique work before deleting, then reschedule from the new state, or a stale worker will resurrect a reminder for a deleted activity.

## Affected layers

`domain/training`, `data/repository`, `analytics`.

## Acceptance criteria

- [ ] Completing an activity persists the result, difficulty, note, duration and `completedAt` atomically.
- [ ] A result whose type does not match the activity's `exerciseType` is rejected (fail-fast in debug, `DomainError` in release).
- [ ] First completion of a technique sets `introCompletedAt` and produces `MasteryLevel.MET`.
- [ ] Completing a review-eligible exercise creates a review item due the next day.
- [ ] Completing a focus activity writes exactly one `focus_session` row.
- [ ] Completing a habit-stack activity writes the stack and requests its nudge.
- [ ] A premortem with "Add to today" creates one `MANUAL` activity.
- [ ] Completing the last required activity marks the day `COMPLETE` and advances `programDay` by exactly one.
- [ ] Running day completion twice advances the program once.
- [ ] Completing an already-completed activity is a no-op that returns success.
- [ ] An abandoned day does not advance `programDay`.
- [ ] `ResetProgramUseCase` deletes exactly the tier-1 tables and keeps preferences, topics and habit stacks; the program returns to Day 1 with only `daily_reflection` unlocked.
- [ ] `EraseAllDataUseCase` returns the app to first run.
- [ ] Both reset paths cancel and reschedule work.
- [ ] `ResetCoverageTest` passes.

## Unit test expectations

`CompleteActivityUseCaseTest` - one test per effect; a test that all seven fire from a single call; a test that a failure in any effect rolls back the whole transaction (inject a failing fake).

`AdvanceProgramDayUseCaseTest` - advances once; second call is a no-op; abandoned day does not advance.

`ResetProgramUseCaseTest` / `EraseAllDataUseCaseTest` - exactly what is deleted and kept, table by table.

`ResetCoverageTest` - every table is assigned a tier.

## UI test expectations

None.

## Integration test expectations

- `DailyLoopIntegrationTest` - generate Day 1, complete all activities, assert the day is `COMPLETE`, `programDay` is 2, mastery is `MET`, and Day 2 generates the expected plan.
- `TransactionAtomicityTest` - force a failure in effect 3 and assert nothing was written.
- `ResetIntegrationTest` - both tiers, end to end, against a real database.

## Manual verification

1. Complete a full day; confirm the program advances and tomorrow's plan is correct.
2. Complete an exercise, force-stop mid-transaction (hard to time; instead inject a failure in a debug build) and confirm no partial state.
3. Run both resets and inspect the database.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 6, 9, 10, 11.
