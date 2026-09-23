# ADR-0013: Progress and mastery are derived, never stored

Status: accepted (2026-09-22)

## Context

Mastery levels and skill levels are functions of the activity log. Storing them creates a second source of truth that can drift from the log.

## Decision

Nothing about progress is persisted. `MasteryLevel`, `SkillLevel`, practice counts, day counts and focus minutes are computed on read from `plan_activity` and `focus_session` with indexed aggregate queries. Only *facts* are stored: completions, durations, sessions, unlock timestamps.

## Alternatives considered

- **Incremental counters updated on completion** - fast reads, but every rule change needs a backfill migration, and a missed increment is permanently wrong. Rejected.
- **A materialised progress table refreshed by a worker** - a drift window plus a worker to debug. Rejected.

## Consequences

- Changing a mastery threshold is a code change with no migration and no backfill.
- Reset is a single `DELETE`.
- Reads must stay fast; the target is 300 ms for the Progress screen with about 1 500 activities, with `@DatabaseView` (not a cache) as the escalation.

## Migration implications

None, by design. That is the point.
