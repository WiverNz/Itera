# Issue backlog

Eleven milestone issues cover the Itera MVP. Each milestone is small in text and points to the specification rather than repeating it. The specification - PRD, UX, architecture, data, engines, testing, `design/` - is unchanged and authoritative.

The previous 40-issue backlog is preserved unchanged under [`docs/history/issues-detailed/`](../history/issues-detailed/). Each milestone lists the detailed issues it absorbs; **read those for scope detail, implementation notes and acceptance detail.** Where a detailed issue and its milestone disagree on dependencies, verification or recording, the milestone wins.

## Current status (2026-09-24)

This index is the implementation status entry point; each milestone owns its acceptance checklist and evidence.

- **001 and 002: complete.** Milestone 002's acceptance checklist is reconciled with its passing host and device results.
- **003: implemented; final visual acceptance pending.** Required tests and build checks pass. The remaining component parity and gallery/shell sign-off are tracked in [003](003-design-system-and-app-shell.md#verification-gap-2026-09-24).
- **004: complete (2026-09-25).** Catalogue, engine and lifecycle are implemented with all required tests passing; see its decisions in `docs/00-source-of-truth.md`.
- **005: implemented (2026-09-25).** Required tests pass; the prototype comparison in light and dark remains open with 003's visual acceptance. See [005](005-onboarding-and-daily-flow.md#verification-gap).
- **006-011: not started.**

Next implementation milestone: **006** when requested. Keep the remaining 003 visual verification open; readiness is not authorization to start another milestone.

## Index

| # | Milestone | Depends on | Absorbs (detailed) |
| --- | --- | --- | --- |
| [001](001-project-bootstrap.md) | Project bootstrap - **complete** | - | 001 |
| [002](002-core-architecture-and-storage.md) | Core architecture & storage - **complete** | 001 | 002, 005, 006, 007, analytics core of 036 |
| [003](003-design-system-and-app-shell.md) | Design system & app shell - **implemented; visual acceptance pending** | 002 | 003, 004, 015 |
| [004](004-content-and-training-engine.md) | Content & training engine - **complete** | 002 | 008-014 |
| [005](005-onboarding-and-daily-flow.md) | Onboarding & daily flow - **implemented; visual comparison pending** | 003, 004 | 016, 017, 019, 020 |
| [006](006-exercise-system.md) | Exercise system | 005 | 018, 021-027 |
| [007](007-train-and-technique-library.md) | Train & technique library | 006 | 028-030 |
| [008](008-progress-history-and-settings.md) | Progress, history & settings | 005 | 031, 032, 034, 035 |
| [009](009-notifications-and-quality-passes.md) | Notifications & quality passes | 006, 007, 008 | 033, rest of 036, 037, 041 |
| [010](010-test-hardening.md) | Test hardening | 009 | 039 |
| [011](011-release-readiness.md) | Release readiness | 010 | 040 |

Critical path: `001 -> 002 -> 004 -> 005 -> 006 -> 007 -> 009 -> 010 -> 011`. 003 sits alongside 004; 008 runs alongside 006-007.

## Old-to-new mapping

| Detailed | New | | Detailed | New |
| --- | --- | --- | --- | --- |
| 001 | 001 | | 021-027 | 006 |
| 002, 005, 006, 007 | 002 | | 028, 029, 030 | 007 |
| 003, 004, 015 | 003 | | 031, 032, 034, 035 | 008 |
| 008-014 | 004 | | 033, 037, 041 | 009 |
| 016, 017, 019, 020 | 005 | | 036 | 002 (interface, catalogue, sink) + 009 (privacy guard, trim, gap-fill) |
| 018 | 006 | | 039 / 040 | 010 / 011 |

038 stays retired. Specification documents (`docs/testing/01-test-matrix.md`, `docs/ux/00-screen-inventory.md`, `docs/ux/05-prototype-reference.md`, `docs/00-source-of-truth.md` and others) still cite detailed numbers; translate them with this table.

## Seams introduced by the consolidation

- **`Analytics`** (interface, catalogue, `event_log` sink) lands in 002, so each milestone instruments its own flows.
- **`ReminderScheduler`** exists from 004 with a no-op binding. 005, 006 and 008 call it; 009 binds the real implementation.
- **Notification channels**: 006 creates `focus_session` for the timer's foreground service; 009 adds the rest to the same registry.

## Working rules

1. Take the lowest-numbered milestone whose dependencies are done. Read it, the detailed issues it absorbs, and the documents it names.
2. For UI work, run `design/` and open the prototype files the milestone names (`docs/ux/05-prototype-reference.md`). Reproduce; do not redesign.
3. If a decision is not covered by the documentation, stop and record it in `docs/00-source-of-truth.md` or an ADR.
4. Strings are added in all four languages by the milestone that introduces them - never deferred to 009.
5. Work in small, working increments within a milestone; the app must build after each. Leave changes for the user to review and commit, following `AGENTS.md`.

## Verification scope

Keep verification proportional; the exhaustive passes happen once, at the end.

**Milestones 002-008** - per change:

- `./gradlew build` (formatting, lint, unit tests, coverage gate) must pass.
- Run and write only the tests for the area you changed, plus the milestone's required tests.
- UI: compare each new screen side by side with `design/` in light and dark. Record **deviations only**, in the milestone file. No comparison log when nothing deviates.
- Do not re-audit the whole repository, re-run earlier milestones' manual checks, or write verification reports. Write something down only when a failure or deviation needs documenting.
- The applicable sections of `docs/delivery/02-definition-of-done.md` are 1-6 and 9-11, read with this scope. Sections 7-8 apply as authoring rules (semantics, content descriptions, four-language strings), not as full manual sweeps.

**Milestones 009-011** own the exhaustive work: full-app accessibility (TalkBack, font scale, Switch Access), localisation audit and pseudo-locales, the golden-image set, the full regression and instrumented suites, release-build verification and manual QA.
