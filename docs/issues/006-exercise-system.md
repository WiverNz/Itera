# 006 - Exercise system

**Depends on** 003, 004, 005 | **Blocks** 007, 009

## Goal

Every technique has its real experience: the generic runner and template body, the focus timer, the five specialised exercises, the review screen and the combination-day runner. Days 1-14 are completable end to end.

## Included scope

- **Runner and template body** (old 018): intro / run / result host, six block types, completion rules, draft autosave and restore, snooze-from-intro.
- **Focus timer** (old 021): pre-timer sheet, timer screen, `FocusTimerState` on wall-clock `endsAt`, `specialUse` foreground service, restore table, result variant. **Creates the `focus_session` notification channel** it needs; 009 adds the other channels to the same registry.
- **Eisenhower** (old 022), **Feynman** (old 023, AI coach box in "Coming later" state only), **Premortem** (old 024), **Habit stacking** (old 025, nudge requested through `ReminderScheduler`), **Review screen** (old 026), **Combination day** (old 027).
- Strings for all of the above in four languages; analytics call sites for these flows.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/018-exercise-runner.md` and `021-focus-timer.md` through `027-combination-day.md`.
- `docs/ux/02-screen-specs-exercise.md`; `docs/engine/00-exercise-state-machine.md`, `02-spaced-repetition.md`, `05-timer-lifecycle.md`; ADR-0006, 0007, 0009, 0016.
- Prototype: `design/app/src/main/java/com/itera/app/ui/screens/Exercise.kt` (runner), `Focus.kt` (timer), `Practice.kt` (Eisenhower, Feynman, premortem, habit stack, review, combination).

## Key dependencies

- Needs 004 (lifecycle, review scheduling) and 005 (Today launches exercises).
- Combination day must follow the timer and Eisenhower within this milestone (Day-14 chain uses both).
- Reminder suppression while a session runs is implemented by the workers in 009; this milestone exposes the running-session state they read.
- Suggested order inside the milestone: runner -> timer -> Eisenhower -> Feynman/review -> premortem -> habit stack -> combination.

## Acceptance criteria

- [ ] The runner renders all six block types, gates completion per rule with a state description, and restores drafts after process death.
- [ ] Completing a template exercise closes Day 1 through reflection and Day complete, and Day 2 opens with the carry-over banner.
- [ ] The timer never stores a decrementing counter; pause/resume/`+5 min` are exact; it survives process death per the restore table; sessions under 60 s record nothing; the service uses `specialUse` with a chronometer notification.
- [ ] Each specialised exercise matches its prototype and persists its documented result; premortem "Add to today" creates one `MANUAL` activity; a Feynman completion schedules a review.
- [ ] Review hides the previous answer until reveal and grades move the ladder per spec; a double tap creates one attempt.
- [ ] The Day-14 chain runs Eisenhower -> 80/20 -> Deep Work -> reflection with values handed forward, and completing it grants `INTEGRATED`.
- [ ] No fabricated coaching feedback is shown anywhere.
- [ ] New strings exist in en/ru/de/es; each screen was compared against the prototype in light and dark, with only deviations recorded.

## Required tests

`TemplateBodyTest`, `CompletionRuleTest`, `BlockValueSerializationTest`, `ExerciseRunnerViewModelTest`, `ExerciseRunnerScreenTest`, `ExerciseCompletionIntegrationTest`, `FocusTimerStateTest`, `FocusViewModelTest`, `FocusScreenTest`, `FocusTimerServiceTest`, `EisenhowerViewModelTest`, `EisenhowerScreenTest`, `FeynmanViewModelTest`, `FeynmanScreenTest`, `PremortemViewModelTest`, `PremortemScreenTest`, `PremortemMitigationTest`, `HabitStackViewModelTest`, `HabitStackScreenTest`, `ReviewViewModelTest`, `ReviewScreenTest`, `ReviewIntegrationTest`, `CombinationViewModelTest`, `CombinationScreenTest`, `CombinationIntegrationTest`.

Process-death behaviour of the timer is checked manually on the oldest supported device during this milestone, not deferred.

## Deviations (2026-09-26)

Recorded per "Verification scope". Decisions that resolve gaps are in `docs/00-source-of-truth.md` ("Milestone 006 implementation decisions").

- **Review compare is ungated**, unlike the prototype, so an empty answer can be graded (issue 026 acceptance).
- **The 2-minute rule's Finish needs two ticked tasks** (the catalogue rule), where the prototype enables it at one.
- **Production additions not drawn in the prototype:** the Eisenhower entry step, the Feynman topic picker and inline creation, the review comparison with its three grades, premortem reason focus controls and "Finish without adding", habit-stack custom chips and the nudge time row, the focus pre-timer sheet and end confirmation, the focus/review result variants. They reuse existing components and tokens; `design/` was not changed.
- **The chain has three step rows, not four** (the 004 row shape); `CombinationIntegrationTest` asserts parent + three steps.

## Verification gap

- The side-by-side comparison with `design/` in light and dark was not done for these screens (no device session in this change). It stays open with 003's and 005's visual acceptance.
- The timer's process-death behaviour on the oldest supported device was not checked manually; `FocusTimerServiceTest` covers restore on the JVM.
- `HabitNudgeTest` and the `NotificationSuppressionTest` additions need the real `ReminderScheduler` and belong to 009.
