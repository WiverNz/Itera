# Itera documentation

Everything needed to implement the Itera MVP, one issue at a time, without making product or architecture decisions along the way.

**Read `00-source-of-truth.md` first.** It sets the source priority, explains how to use the `design/` prototype, and records every decision taken where sources disagreed.

**The `design/` folder is a running Kotlin + Compose prototype and the source of truth for all UI and interaction.** See [`ux/05-prototype-reference.md`](ux/05-prototype-reference.md). The earlier `Itera.html` artboard bundle has been removed from the repository.

## Implementation status

See the [milestone index](issues/README.md#current-status-2026-09-24) for current status and acceptance evidence. Milestones 001-002 are complete; 003 is implemented with final visual acceptance pending. Milestone 004 is ready to start and has not been started.

## Map

### Start here

| Document | What it is |
| --- | --- |
| [`00-source-of-truth.md`](00-source-of-truth.md) | Authority model, 16 resolved inconsistencies, and the index of architecture decisions. Authoritative over everything else. |
| [`ux/05-prototype-reference.md`](ux/05-prototype-reference.md) | How to run `design/`, the screen-to-file map, and the porting checklist. Read before any UI issue. |
| [`delivery/00-implementation-order.md`](delivery/00-implementation-order.md) | The 12 milestone issues, their order and exit conditions; 012 voice runs before 009. |
| [`issues/README.md`](issues/README.md) | The issue index. |

### Product (`prd/`)

| Document | What it is |
| --- | --- |
| [`00-product-brief.md`](prd/00-product-brief.md) | What Itera is, the daily loop, the four tabs, the anti-streak stance. |
| [`01-mvp-scope.md`](prd/01-mvp-scope.md) | What ships, what does not, and what is partially built. |
| [`02-user-flows.md`](prd/02-user-flows.md) | Ten flows, including missing days and starting over. |
| [`03-functional-requirements.md`](prd/03-functional-requirements.md) | FR-01 to FR-49, including planned voice input, each linked to its spec. |
| [`04-nonfunctional-requirements.md`](prd/04-nonfunctional-requirements.md) | Offline, performance budgets, reliability, privacy, compatibility. |
| [`05-content-and-technique-model.md`](prd/05-content-and-technique-model.md) | The 14 techniques, the one-to-one skill mapping, the exercise types. |
| [`06-personas-and-assumptions.md`](prd/06-personas-and-assumptions.md) | Who it is for, eight stated assumptions, anti-goals. |
| [`07-technique-curriculum.md`](prd/07-technique-curriculum.md) | The 14-day program, why that order, and what happens after Day 14. |
| [`08-content-style-guide.md`](prd/08-content-style-guide.md) | How to write the eight strings each technique needs. |
| [`09-mvp-acceptance-criteria.md`](prd/09-mvp-acceptance-criteria.md) | 60 criteria across 10 groups. The MVP is done when these pass. |
| [`10-post-mvp-backlog.md`](prd/10-post-mvp-backlog.md) | What is seamed for, what is candidate, what is rejected, and the debt accepted. |

### UX (`ux/`)

| Document | What it is |
| --- | --- |
| [`00-screen-inventory.md`](ux/00-screen-inventory.md) | All 24 screens mapped to artboards and issues, plus the 9 surfaces the design does not draw. |
| [`01-navigation-graph.md`](ux/01-navigation-graph.md) | Routes, tabs, full-screen routes, deep links, draft preservation. |
| [`02-screen-specs-onboarding.md`](ux/02-screen-specs-onboarding.md) | Welcome and the three steps. |
| [`02-screen-specs-today.md`](ux/02-screen-specs-today.md) | Today: anatomy, hero priority, seven row states, five screen states. |
| [`02-screen-specs-exercise.md`](ux/02-screen-specs-exercise.md) | The runner host, the template body, and the seven specialised bodies. |
| [`02-screen-specs-train-progress-you.md`](ux/02-screen-specs-train-progress-you.md) | Train, Library, Technique detail, Progress, History, You. |
| [`03-ux-states.md`](ux/03-ux-states.md) | Loading, empty, error, first-run, day-complete, permission-denied, reduced motion. |
| [`04-design-system.md`](ux/04-design-system.md) | Every colour, type style, shape, spacing rule, component and animation, transcribed from the prototype's theme and component packages. |
| [`05-prototype-reference.md`](ux/05-prototype-reference.md) | Running `design/`, the screen-to-file map, what to change when porting, and the prototype's known limitations. |
| [`07-accessibility.md`](ux/07-accessibility.md) | Semantics per screen, font scaling, the TalkBack script, localisation readiness. |
| [`08-notification-ux.md`](ux/08-notification-ux.md) | The six notifications, suppression rules, copy rules, permission handling. |
| [`09-copy-deck.md`](ux/09-copy-deck.md) | Voice, terminology, the key scheme, and the rules enforced by tests. The copy itself lives in `design/`. |
| [`10-voice-input.md`](ux/10-voice-input.md) | Planned dictation, contextual commands, confirmation, states and minimal prototype additions (milestone 012). |

### Architecture (`architecture/`)

| Document | What it is |
| --- | --- |
| [`00-overview.md`](architecture/00-overview.md) | Layers, DI, threading, startup, build configuration. |
| [`01-package-structure.md`](architecture/01-package-structure.md) | The full package tree, naming, boundary rules, and when to modularise. |
| [`02-state-management.md`](architecture/02-state-management.md) | The one screen template every feature copies. |
| [`03-compose-conventions.md`](architecture/03-compose-conventions.md) | Composable shape, previews, theming, lists, semantics, performance. |
| [`04-navigation-architecture.md`](architecture/04-navigation-architecture.md) | How the graph is wired in code. |
| [`05-error-handling-and-logging.md`](architecture/05-error-handling-and-logging.md) | Error classes, presentation, logging policy, what must never be logged. |
| [`06-dependency-catalog.md`](architecture/06-dependency-catalog.md) | Every dependency, why, and what is deliberately excluded. |
| [`07-voice-input.md`](architecture/07-voice-input.md) | Planned recognition adapter, typed commands, deterministic parsing and existing-action dispatch; [ADR-0022](architecture/adr/0022-voice-recognition-and-privacy.md) defines the recognizer order (on-device, then consented system default or user-chosen app) and privacy/permission policy. |
| [`adr/`](architecture/adr/) | 19 architecture decision records. |

### Data (`data/`)

| Document | What it is |
| --- | --- |
| [`00-domain-model.md`](data/00-domain-model.md) | Every domain type, repository interface, and the nine invariants. |
| [`01-room-schema.md`](data/01-room-schema.md) | 11 tables, the ERD, DAOs, payload encoding, copy keys, backup policy. |
| [`02-datastore-preferences.md`](data/02-datastore-preferences.md) | Both DataStore instances and the privileged `current_program_day`. |
| [`03-journal-export-format.md`](data/03-journal-export-format.md) | The Markdown format, with a worked example. |
| [`04-technique-catalog-format.md`](data/04-technique-catalog-format.md) | The two JSON assets, the frozen ids, and the validation test. |
| [`05-migrations-and-content-versioning.md`](data/05-migrations-and-content-versioning.md) | Two version axes, allowed and forbidden content changes. |

### Engine (`engine/`)

| Document | What it is |
| --- | --- |
| [`00-exercise-state-machine.md`](engine/00-exercise-state-machine.md) | Seven states, 12 transitions, availability, rollover, completion effects. |
| [`01-training-plan-engine.md`](engine/01-training-plan-engine.md) | The generation algorithm, with four worked examples that are also its tests. |
| [`02-spaced-repetition.md`](engine/02-spaced-repetition.md) | The 1/4/9/21/60 ladder and the three-way recall grade. |
| [`03-mastery-and-progress.md`](engine/03-mastery-and-progress.md) | Mastery levels, skill bands, the progress summary, and what must never be shown. |
| [`04-unlock-rules.md`](engine/04-unlock-rules.md) | The unlock table and what a locked technique can still do. |
| [`05-timer-lifecycle.md`](engine/05-timer-lifecycle.md) | Wall-clock persistence, the foreground service, restore, clock changes. |
| [`06-workmanager-strategy.md`](engine/06-workmanager-strategy.md) | Eight workers, scheduling policy, channels, rescheduling triggers. |

### Localisation (`i18n/`)

| Document | What it is |
| --- | --- |
| [`00-localization.md`](i18n/00-localization.md) | Four shipping languages, the per-app language mechanism, the picker, resources, formatting, and what is never translated. |

### Testing (`testing/`)

| Document | What it is |
| --- | --- |
| [`00-strategy.md`](testing/00-strategy.md) | The pyramid, no mocking framework, fake clocks, product-guarantee tests. |
| [`01-test-matrix.md`](testing/01-test-matrix.md) | Every test, its type, its file, and the issue that must deliver it. |
| [`02-manual-qa-checklist.md`](testing/02-manual-qa-checklist.md) | The twelve-section pre-release pass. |
| [`03-ci.md`](testing/03-ci.md) | Pipeline, gates, static checks, coverage. |
| [`04-visual-regression.md`](testing/04-visual-regression.md) | How visual fidelity to `design/` is established and then protected: side-by-side comparison, semantic assertions, golden images. |

### Analytics (`analytics/`)

| Document | What it is |
| --- | --- |
| [`00-event-model.md`](analytics/00-event-model.md) | The local-only event catalogue and its privacy rules. |

### Delivery (`delivery/`)

| Document | What it is |
| --- | --- |
| [`00-implementation-order.md`](delivery/00-implementation-order.md) | 12 milestones, order and exit conditions. |
| [`01-issue-dependency-graph.md`](delivery/01-issue-dependency-graph.md) | The milestone graph, critical path and cross-milestone seams. |
| [`02-definition-of-done.md`](delivery/02-definition-of-done.md) | Twelve sections every issue must satisfy. |
| [`03-release-checklist.md`](delivery/03-release-checklist.md) | Pre-flight through post-release. |
| [`04-milestones.md`](delivery/04-milestones.md) | Four demonstrable milestones with their risks. |
| [`05-issue-renumbering.md`](delivery/05-issue-renumbering.md) | Historical: how the original 26 stubs became the 40 detailed issues (since consolidated). |

### Legacy (`implementation-plan/`)

The bootstrap planning documents. Superseded, kept for provenance - see [`implementation-plan/README.md`](implementation-plan/README.md).

## Conventions

- Anything stated as a **decision** is binding. Changing it requires updating `00-source-of-truth.md` or adding an ADR, in the same change as the code.
- Anything stated as a **budget** (start time, APK size, coverage) is a gate, not an aspiration.
- Where a document shows a worked example, that example is also a test fixture. They must not drift.
- Where a document describes UI, `design/` is what the code must match; the document exists so a reviewer can check the work without reading Kotlin. If the two disagree, the document is stale - fix it.
