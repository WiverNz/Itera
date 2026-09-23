# ADR-0018: The `design/` prototype is the UI implementation reference

Status: accepted (2026-09-23)

## Context

UI was originally specified by transcribing a bundle of 51 static artboards (`Itera.html`) into Markdown. A running Kotlin + Jetpack Compose prototype, `design/`, now exists: 24 screens, a theme package, a component library, hand-authored icons, navigation, and four complete translations.

A Markdown transcription of a picture loses behaviour, cannot be run, and drifts silently. A running prototype in the same language and framework as the target does not.

## Decision

`design/` is the primary implementation reference for all UI and interaction work, second in authority only to explicit product requirements. `Itera.html` is obsolete and is retained only as historical context for older documentation decisions.

Implementers open the relevant prototype file while implementing each UI issue and reproduce its structure, values and behaviour. The UX documents in `docs/ux/` record what the prototype does so reviewers can check work without reading Kotlin; where the two disagree, the prototype is right and the document is stale.

The prototype is **not** authoritative for architecture, persistence, engines, scheduling or testing - it deliberately has none of those.

Production code never imports from `design/`; it is not part of the production Gradle build.

## Alternatives considered

- **Keep Markdown as the source and treat the prototype as a sketch** - reintroduces the drift and behaviour loss that motivated the change. Rejected.
- **Make `design/` a Gradle module the app depends on** - couples production to prototype code that has no tests, no architecture and in-memory state. Rejected.
- **Delete the UX documents and rely on the prototype alone** - a reviewer would have to read Kotlin to check a padding, and the product rules the prototype omits would have nowhere to live. Rejected.
- **Delete the prototype after transcribing it** - that is exactly what produced the previous situation. Rejected.

## Consequences

- Visual fidelity is checkable: run both, compare.
- Golden-image tests become worthwhile, because there is a reference implementation to generate them from (`docs/testing/04-visual-regression.md`).
- The prototype must be kept current: an agreed design change updates `design/` in the same change, or the reference rots.
- The theme and component packages are ported nearly verbatim, which removes a large class of transcription error.
- Where the prototype is thinner than the product - no persistence, no engines, no states - the gap is enumerated per screen rather than discovered during implementation.

## Migration implications

Documents that cite `Itera.html` artboard ids are recording history. No document may instruct an implementer to open it.
