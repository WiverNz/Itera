# ADR-0001: Single Gradle module with package boundaries

Status: accepted (2026-09-22)

## Context

The MVP is one binary, built by one team, with no dynamic delivery and no second app target. The bootstrap docs already lean this way ("A single app module with package boundaries is acceptable for the MVP").

## Decision

Ship `:app` as the only Gradle module. Enforce layer boundaries with package rules and a JVM `ArchitectureTest` rather than with module visibility.

## Alternatives considered

- **Full multi-module (`:core:*`, `:feature:*`)** - buys compile-time boundary enforcement and parallel builds, costs ~15 build files, a convention plugin, and slower incremental builds at this size. Rejected for now.
- **Two modules (`:app`, `:core`)** - half the ceremony for a quarter of the benefit. Rejected as the worst of both.

## Consequences

- Boundary violations are caught by a test, not the compiler. The test must be maintained.
- Build times stay low while the codebase is small.
- The package tree already mirrors the eventual module tree, so splitting is file movement.
- Split triggers are documented in `docs/architecture/01-package-structure.md` section 5.

## Migration implications

None today. A future split requires no source changes beyond widening some visibilities.
