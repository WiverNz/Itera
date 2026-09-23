# ADR-0010: Inexact WorkManager scheduling; no exact alarms

Status: accepted (2026-09-22)

## Context

The product needs three daily reminders and an optional habit nudge. Android 13+ gates exact alarms behind a special permission.

## Decision

`WorkManager` only. Self-rescheduling one-shot workers for wall-clock reminders; a periodic worker for the daily plan and the log trim. No `SCHEDULE_EXACT_ALARM`, no `USE_EXACT_ALARM`, no `setExpedited`.

Reminder copy is written to tolerate a delay of up to roughly 45 minutes.

## Alternatives considered

- **Exact alarms** - a permission prompt and a battery-hostile reputation for a product whose whole pitch is calm. Rejected; the product explicitly does not need exact timing.
- **A periodic worker per reminder** - cannot target a wall-clock time; minimum period is 15 minutes with an arbitrary phase. Rejected.

## Consequences

- Reminders may be late. Designed for.
- All workers are idempotent and keyed, so replays are harmless.
- Re-scheduling triggers (boot, timezone, update, preference change) are enumerated in `docs/engine/06-workmanager-strategy.md`.

## Migration implications

None.
