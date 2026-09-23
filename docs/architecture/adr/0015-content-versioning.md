# ADR-0015: Catalog contentVersion with additive-only changes inside a major

Status: accepted (2026-09-22)

## Context

Techniques, curriculum and template definitions will change after release. Existing `plan_activity` rows reference technique ids and template block keys that must keep resolving.

## Decision

- Each catalog file carries an integer `contentVersion`; `UserPreferences.contentVersion` records the last version the user's data was reconciled against.
- Within a major app version, catalog changes are **additive only**: add techniques, add template blocks, edit prose. Never remove a technique id, never rename a block key, never change a technique's `skill`.
- Removing a technique is done by marking it `retired: true`: it stops appearing in the curriculum and the library, but historical activities still resolve.
- Changing `introDay` for an already-started user does **not** re-order their program; the curriculum is read at generation time and past days are never regenerated (`training_day.generatorVersion` guards this).
- On app start, if `catalog.contentVersion > prefs.contentVersion`, a `ContentReconciler` runs: it inserts `technique_state` rows for new techniques (locked), unlocks any whose `introDay <= currentProgramDay`, and records the new version. Idempotent.

## Alternatives considered

- **No versioning** - a removed technique id orphans history rows. Rejected.
- **A full content migration per change** - heavy for prose edits. Rejected.

## Consequences

- Content edits ship without a database migration in the common case.
- A breaking content change requires a major app version and an explicit migration path, reviewed as such.

## Migration implications

`ContentReconciler` is the single entry point and must be extended, not bypassed, when the catalog schema grows.
