# ADR-0009: Wall-clock end instant plus a foreground service

Status: accepted (2026-09-22)

## Context

A 25- or 50-minute timer must survive backgrounding, screen-off, Doze and process death, and must not drift.

## Decision

Persist `FocusTimerState` (including an absolute `endsAt: Instant`) to a dedicated DataStore file. Remaining time is always computed as `endsAt - now`; the 1 Hz tick only drives rendering. A running timer holds a `specialUse` foreground service with a chronometer notification.

Full behaviour in `docs/engine/05-timer-lifecycle.md`.

## Alternatives considered

- **A `CountDownTimer` in the ViewModel** - dies with the process, drifts under Doze. Rejected.
- **An `AlarmManager` exact alarm at the end time** - needs a special permission for no benefit over a foreground service that also shows live progress. Rejected.
- **A `WorkManager` one-shot** - not guaranteed to run at a precise time and cannot show a live countdown. Rejected.

## Consequences

- The app declares `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`.
- A denied notification permission degrades to a timer without a notification, with an honest inline warning.
- The timer is correct across clock and timezone changes because `Instant` is absolute.

## Migration implications

The DataStore file is transient; a schema change may discard an in-flight timer, which is acceptable and handled by the restore table in the lifecycle doc.
