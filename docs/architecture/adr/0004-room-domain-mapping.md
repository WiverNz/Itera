# ADR-0004: Explicit mappers between Room entities and domain types

Status: accepted (2026-09-22)

## Context

Room entities need annotations, nullable columns and primitive types. Domain types need value classes, sealed hierarchies and non-null invariants. Sharing one class forces the worse of both.

## Decision

Separate `XEntity` and `X`, with extension-function mappers in `data/mapper`. DAOs return entities only. No Room annotation ever appears on a domain type; no domain type appears in a DAO signature.

Copy resolution (`copyKey` + `copyArgs` to localised strings) happens in the mapper, via an injected `CopyResolver`.

## Alternatives considered

- **Annotate domain classes with Room** - couples `domain` to `androidx.room`, breaking the pure-Kotlin rule and the JVM test story. Rejected.
- **Auto-mapping (MapStruct-style)** - no mature KSP option that handles sealed payload decoding. Rejected.

## Consequences

- Mapper code to maintain, one file per aggregate.
- Schema changes cannot silently change domain semantics.
- Mappers are pure and directly unit-tested, including the corrupt-payload path.

## Migration implications

A column rename touches the entity and one mapper, not the whole app.
