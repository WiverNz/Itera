# ADR-0005: Technique catalog as a versioned bundled JSON asset

Status: accepted (2026-09-22)

## Context

14 techniques with long explanatory prose, per-type defaults, template block definitions and a 14-day curriculum. This is content, not code, and it must be editable without touching Kotlin. It must also work fully offline.

## Decision

Two assets in `app/src/main/assets/catalog/`:

- `techniques.v1.json` - the 14 technique definitions
- `curriculum.v1.json` - the day-by-day program

Parsed with `kotlinx.serialization` into DTOs, mapped to domain types, cached in memory for the process lifetime. `contentVersion` is an integer at the root of each file.

**Prose is not in the JSON.** Each technique carries string-resource keys (`technique_pomodoro_name`, `technique_pomodoro_short`, `technique_pomodoro_explanation`); the JSON holds structure (ids, skill, introDay, exerciseType, durations, relations, template blocks). This keeps localisation in `res/values` where the tooling is.

## Alternatives considered

- **Hard-coded Kotlin objects** - no content/code separation, merge conflicts, recompiles for a typo. Rejected.
- **Room-seeded catalog table** - adds a migration for every content change and duplicates the source of truth. Rejected.
- **Remote catalog** - violates offline-first and the no-backend constraint. Rejected for the MVP; the loader interface leaves the seam open.

## Consequences

- Content edits are reviewable as a JSON diff.
- A malformed catalog fails fast in debug. In release the app falls back to the last successfully parsed copy; with none, it shows a blocking error screen. A shipped catalog is validated by a unit test, so release failure is effectively impossible.
- Adding a technique is a JSON entry plus its strings.

## Migration implications

See ADR-0015.
