# ADR-0006: Sealed ActivityResult persisted as discriminated JSON

Status: accepted (2026-09-22)

## Context

Nine exercise types produce structurally different results: a checklist with stopwatches, four quadrants of tasks, an explanation plus chips, a ranked list of failure reasons, an anchor/habit pair, a timer summary, a three-question reflection, a graded recall, a chain of steps. They are written once and read back for History, Technique detail and reviews.

## Decision

A sealed `ActivityResult` hierarchy in `domain/model`. Persisted in a single `resultPayload TEXT` column as JSON with a `type` discriminator, via `kotlinx.serialization` polymorphic serialisation over a `SerializersModule`. `@SerialName` values are frozen once released.

Drafts use the identical shape in `draftPayload`.

## Alternatives considered

- **A table per result type** - nine tables, nine DAOs, nine migrations, and History needs a nine-way union query. Rejected.
- **A wide nullable table** - roughly 30 mostly-null columns; unmaintainable. Rejected.
- **Protobuf or an opaque blob** - not human-inspectable, and the journal export reads these. Rejected.

## Consequences

- The column is not queryable by SQL. Accepted: nothing queries *inside* a result. Aggregates use real columns on `plan_activity` (`durationSeconds`, `difficulty`, `practiceDate`) and `focus_session`, which exist precisely for this reason.
- `ignoreUnknownKeys = true` on read makes forward-rolled payloads safe.
- An undecodable payload yields `result = null`, is logged, and degrades the UI to a title-only row. It never throws.

## Migration implications

Renaming a `@SerialName` requires a data migration rewriting every affected payload. Adding a nullable field is free.
