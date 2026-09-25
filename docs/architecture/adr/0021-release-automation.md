# ADR-0021: Windows release scripts and tag builds

Status: accepted (2026-09-25; explicitly requested tooling scope).

## Decision

Production uses root `version.properties` as the single source of `versionName`
(stable MAJOR.MINOR.PATCH) and `versionCode` (1..2100000000). The initial values
normalize the scaffold's 1.0 / 1 to 1.0.0 / 1. Each release increments the code by one.

Windows PowerShell scripts support version inspection, preview, updates, and the
human-operated bump / release commit / annotated tag / optional push flow used in
ShipSim159 and FlightTrace. Agents do not execute Git mutations; AGENTS.md still applies.
Release preflight requires a clean branch and full history, checks fetched tags for
code reuse, and rejects existing target tags. The operator fetches tags first.

Branch/PR CI retains the existing Android checks and adds a debug APK artifact and
Windows script tests. A pushed v* tag must match the committed stable version and pass
those checks before a Windows runner builds signed APK/AAB files. Signing credentials
come only from environment variables backed by GitHub secrets. Missing or partial
credentials fail release CI; ordinary local builds without credentials stay unsigned.

CI verifies signatures, archives Room schemas and SHA-256 checksums (plus the R8
mapping when present), and creates a **draft** GitHub Release for human review.
Drafts are intentional while milestone 011 and manual distribution acceptance remain
open. Existing release optimization settings are unchanged by this tooling request.

## Consequences

Both Gradle and scripts work without deriving versions from local Git state.
A tag triggers the same source version on a fresh checkout. Keystores stay outside
the repository and are removed from the runner after signing; the signing build
disables configuration and Gradle action caches. The operator keeps a backed-up,
stable signing key and must not reuse codes across release branches.
See [scripts/README.md](../../../scripts/README.md) for operation and recovery.