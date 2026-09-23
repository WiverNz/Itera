# ADR-0014: Two-tier reset: reset program vs erase everything

Status: accepted (2026-09-22)

## Context

"Reset training data" is ambiguous. A user who wants to restart the curriculum rarely wants to lose their learning topics and preferences; a user handing the phone on wants everything gone.

## Decision

Two distinct actions in the You tab, each with its own confirmation dialog naming exactly what is lost.

**Reset program** deletes `training_day`, `plan_activity`, `focus_session`, `reflection_entry`, `review_item`, `review_attempt`; clears `technique_state.unlockedAt` (except Daily reflection); sets `currentProgramDay = 1` and `programStartedOn = today`. Keeps preferences, learning topics, habit stacks.

**Erase everything** additionally deletes `learning_topic`, `habit_stack`, `event_log`, clears both DataStore files, and sets `onboardingCompleted = false` so the app returns to first run.

Both run in a single transaction, cancel all scheduled work, then re-schedule from the new state.

## Alternatives considered

- **One "reset" button** - forces the user to guess. Rejected.
- **Per-entity granular deletion** - a data-management UI inside a product that is trying not to be one. Rejected.

## Consequences

- Two use cases (`ResetProgramUseCase`, `EraseAllDataUseCase`), both instrumented-tested.
- Neither is undoable; both dialogs say so plainly.

## Migration implications

Every new table must be added to the correct tier. A test enumerates all tables and fails if one is in neither list.
