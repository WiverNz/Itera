# Functional requirements

Each requirement links to the document that specifies it fully. `FR-` ids are referenced by issue acceptance criteria and by the test matrix.

## Daily training plan

- **FR-01** The app generates a local daily plan for each calendar day the user opens it. `docs/engine/01-training-plan-engine.md`
- **FR-02** Generation is idempotent and deterministic: the same inputs always produce the same plan, and the plan for a given day is stable once generated.
- **FR-03** A plan contains exactly one program (or combination) activity, exactly one reflection, at most one optional secondary activity, and zero or more due reviews (capped by pace).
- **FR-04** Each activity carries: technique, exercise type, source, day part, resolved copy, estimated duration, state, optional flag, and - once done - a result.
- **FR-05** `programDay` advances by exactly one when a day completes, and never on an abandoned day.

## Exercise lifecycle

- **FR-06** States: `SCHEDULED`, `AVAILABLE`, `IN_PROGRESS`, `SNOOZED`, `COMPLETED`, `SKIPPED`, `EXPIRED`. Transitions are exhaustively specified in `docs/engine/00-exercise-state-machine.md`.
- **FR-07** An invalid transition is a no-op, never a crash.
- **FR-08** In-progress work is autosaved as a draft and restored after process death.
- **FR-09** An activity may be snoozed once to a later time the same day.

## Technique unlocks

- **FR-10** A technique unlocks when `programDay` reaches its `introDay`. There is no performance gate. `docs/engine/04-unlock-rules.md`
- **FR-11** Locked techniques are visible and openable in the library, showing the day they unlock.
- **FR-12** The plan engine never emits a locked technique.

## Exercise completion

- **FR-13** Completion captures: timestamp, duration, an optional difficulty rating (Easy / Okay / Hard), an optional note, and a type-specific structured result.
- **FR-14** Completing the first instance of a technique sets mastery to `MET`.
- **FR-15** Completing a review-eligible exercise schedules a review.
- **FR-16** Completing every non-optional activity completes the day and advances the program.

## Spaced repetition

- **FR-17** Review intervals are a fixed five-stage ladder: 1, 4, 9, 21, 60 days. `docs/engine/02-spaced-repetition.md`
- **FR-18** Recall is self-graded three ways, which reset, repeat, or advance the stage.
- **FR-19** The previous answer is not revealed before the user submits their attempt.
- **FR-20** Overdue reviews are due exactly once, with no penalty and no pile-up multiplier.

## Progress

- **FR-21** Progress is derived on read from the activity log; no progress value is stored. `docs/engine/03-mastery-and-progress.md`
- **FR-22** Technique mastery is `Met / Practiced / Applied / Integrated` with the thresholds in that document.
- **FR-23** Skill level is `Starting / Building / Steady / Strong` over a trailing 14-day window.
- **FR-24** Only completed activities, focus minutes and completed reviews count. Opening the app counts for nothing.
- **FR-25** No streak, points total, or program-completion percentage appears anywhere.

## Reminders

- **FR-26** Local reminders for morning training, the optional focus block, and the evening reflection, plus optional habit-stack nudges. `docs/engine/06-workmanager-strategy.md`
- **FR-27** At most one nudge per day part.
- **FR-28** A reminder is never posted for something already done, or while the app is in the foreground.
- **FR-29** Scheduling uses WorkManager with inexact timing. No exact alarms.
- **FR-30** The app is fully functional with notifications denied.

## Focus sessions

- **FR-31** A focus session survives backgrounding, screen-off and process death, and does not drift. `docs/engine/05-timer-lifecycle.md`
- **FR-32** Pause, resume, +5 minutes and early end are supported; an early end under 60 seconds records nothing.
- **FR-33** A running session holds a foreground service with a countdown notification.

## History

- **FR-34** Users can review previous exercises, focus sessions, reflections and technique practice, by month and by day.
- **FR-35** Days with no training are shown as rest days, not hidden.

## Settings

- **FR-36** Theme (System / Light / Dark), reminder toggles, morning and evening times, daily time budget, program pace, focus areas, learning topics, and an optional local display name.
- **FR-37** Journal export to Markdown, shared or saved locally. `docs/data/03-journal-export-format.md`
- **FR-38** Two-tier reset: "Reset program" and "Erase everything", each confirmed and each naming exactly what it removes. ADR-0014.

## Content

- **FR-39** The technique catalog and curriculum are bundled, versioned assets. ADR-0005.
- **FR-40** A content update reconciles unlock state without regenerating past days or re-locking met techniques. ADR-0015.

## Constraints that are requirements

- **FR-41** Every feature works with no network. The app declares no `INTERNET` permission.
- **FR-42** No account, no backend, no subscription, no AI dependency.
- **FR-43** No user-authored text is ever logged or transmitted.
