# ADR-0011: Deterministic engines with an injected Clock

Status: accepted (2026-09-22)

## Context

Plan generation, unlocks, review scheduling and progress must be reproducible, testable on the JVM, and identical across two runs on the same day.

## Decision

- All engine logic is pure functions, or use cases over repository interfaces.
- No `Random` anywhere in plan generation; every choice is a documented deterministic rule with an explicit tiebreak.
- Time enters only through an injected `java.time.Clock`. `LocalDate.now()`, `Instant.now()` and `System.currentTimeMillis()` are banned outside `core/common/Clock.kt`, enforced by `ArchitectureTest`.

## Alternatives considered

- **Seeded `Random` for variety** - reintroduces a hidden input and makes "why did I get this today?" unanswerable. Rejected.
- **Ambient time** - untestable day-boundary, DST and rollover behaviour. Rejected.

## Consequences

- Every engine test is a table of inputs and exact expected outputs.
- Day-boundary, DST and timezone-change behaviour is testable without touching device settings.
- Variety must come from the curriculum and the practice-prompt rule, not from chance. That is a product-positive constraint.

## Migration implications

None.
