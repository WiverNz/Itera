# ADR-0002: MVVM with a single immutable UiState per screen

Status: accepted (2026-09-22)

## Context

The baseline calls for "MVVM or a similarly simple unidirectional-state approach". 24 screens will be implemented by different agents at different times; consistency matters more than sophistication.

## Decision

One `ViewModel` per screen exposing exactly one `StateFlow<XUiState>`, plus a `Channel` of one-shot effects. Screens are stateless composables taking `(state, onEvent)`. The full template is `docs/architecture/02-state-management.md`.

`loading` and `error` are **fields on** `UiState`, not a sealed `Loading | Content | Error` hierarchy.

## Alternatives considered

- **MVI with a reducer** - more machinery, and the app has no complex state transitions in the UI layer (they live in the domain). Rejected.
- **Sealed UiState hierarchy** - forces content to vanish during a refresh; the design never blanks a populated screen. Rejected.
- **Molecule / Compose-state presenters** - interesting, but non-standard for an agent-implemented codebase. Rejected.

## Consequences

- Every screen issue can be written against one template, so no issue needs a state-management decision.
- Refresh-while-showing-content is natural.
- Four files per screen of mild boilerplate, accepted in exchange for uniformity.

## Migration implications

None.
