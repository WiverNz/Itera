# Issue backlog

40 refined issues covering the Itera MVP, numbered 001-037 and 039-041. Each is written so it can be implemented **without making a product or architecture decision** - everything it needs is either in the issue, in a document it names, or in the `design/` prototype.

Before starting any issue, read:

1. `docs/00-source-of-truth.md` - the source priority and every resolved conflict;
2. the issue's own `Depends on` list - none of those may be unmerged;
3. the spec documents the issue references.

**For any UI issue, run the `design/` prototype and open the file its "Prototype reference" section names.** It is a working Kotlin + Compose app and the source of truth for everything visual and interactive. See `docs/ux/05-prototype-reference.md`.

If implementing an issue forces a decision the documentation does not cover, **stop**, resolve it in `docs/00-source-of-truth.md` or a new ADR, then continue. Do not decide it inside the implementation.

## Index

### Phase 1 - Foundation

| # | Issue | Depends on |
| --- | --- | --- |
| [001](001-project-bootstrap.md) | Project bootstrap and dependency baseline | - |
| [002](002-architecture-foundation.md) | Architecture foundation | 001 |
| [003](003-design-tokens-and-theme.md) | Design system: tokens and theme | 001 |
| [004](004-design-system-components.md) | Design system: components | 003 |
| [005](005-domain-model.md) | Domain model | 002 |
| [006](006-room-persistence.md) | Room persistence foundation | 005 |
| [007](007-datastore-preferences.md) | DataStore preferences | 005 |
| [008](008-technique-catalog.md) | Technique catalog and content | 005, 006 |

### Phase 2 - Engine

| # | Issue | Depends on |
| --- | --- | --- |
| [009](009-exercise-state-machine.md) | Exercise state machine | 005 |
| [010](010-unlock-rules.md) | Unlock rules | 005, 008 |
| [011](011-daily-plan-generator.md) | Daily plan generator | 006, 007, 008, 009, 010 |
| [012](012-spaced-repetition-scheduler.md) | Spaced repetition scheduler | 005, 006 |
| [013](013-mastery-and-progress.md) | Mastery and progress calculation | 006, 008 |
| [014](014-day-lifecycle.md) | Day lifecycle and completion effects | 009, 011, 012, 013 |

### Phase 3 - Shell and core loop

| # | Issue | Depends on |
| --- | --- | --- |
| [015](015-navigation-and-shell.md) | Navigation graph and app shell | 004, 007 |
| [016](016-onboarding.md) | Onboarding | 007, 008, 011, 015 |
| [017](017-today-screen.md) | Today screen | 011, 014, 015 |
| [018](018-exercise-runner.md) | Exercise runner and template body | 008, 014, 015 |
| [019](019-evening-reflection.md) | Evening reflection | 014, 018 |
| [020](020-day-complete.md) | Day complete | 014, 019 |

### Phase 4 - Specialised exercises

| # | Issue | Depends on |
| --- | --- | --- |
| [021](021-focus-timer.md) | Focus timer | 018 |
| [022](022-eisenhower-matrix.md) | Eisenhower matrix | 018 |
| [023](023-feynman-exercise.md) | Feynman exercise | 012, 018 |
| [024](024-premortem-exercise.md) | Premortem exercise | 018 |
| [025](025-habit-stacking-exercise.md) | Habit stacking | 018 |
| [026](026-review-screen.md) | Spaced repetition review screen | 012, 018 |
| [027](027-combination-day.md) | Combination day runner | 018, 021, 022 |

### Phase 5 - Learning-loop surfaces

| # | Issue | Depends on |
| --- | --- | --- |
| [028](028-train-tab.md) | Train tab | 010, 012, 015 |
| [029](029-technique-library.md) | Technique library | 008, 013, 015 |
| [030](030-technique-detail.md) | Technique detail | 013, 018, 029 |
| [031](031-progress-screen.md) | Progress screen | 013, 015 |
| [032](032-history-screen.md) | History screen | 013, 015, 031 |

### Phase 6 - Infrastructure

| # | Issue | Depends on |
| --- | --- | --- |
| [033](033-notifications.md) | Notifications and WorkManager | 011, 014, 017 |
| [034](034-settings-you-tab.md) | Settings / You tab | 007, 033 |
| [035](035-journal-export.md) | Journal export | 013, 034 |
| [036](036-analytics-contract.md) | Analytics contract | 002, 006 |

### Phase 7 - Hardening

| # | Issue | Depends on |
| --- | --- | --- |
| [037](037-accessibility-pass.md) | Accessibility pass | 020, 027, 032, 034 |
| [041](041-localization-verification.md) | Localisation verification and audit | 037 |
| [039](039-test-hardening.md) | Test hardening, goldens and CI gates | 035, 036, 041 |
| [040](040-release-readiness.md) | Release readiness | 039 |

## Localisation is not one issue

The app ships in English, Russian, German and Spanish. That is built **across** issues 001-034, not in a single late issue: platform setup in 001, primitives in 002, fonts in 003, the picker in 004, content in 008, entry points in 016 and 034, and each screen's own strings in its own issue. Issue **041 only audits** the result (D-15).

Every screen issue's definition of done requires its new strings to exist in all four languages. If that is skipped, 041 will find it - but the fix belongs to the issue that skipped it.

## Structure

Every issue has: goal, user value, scope, non-goals, implementation notes, affected layers, dependencies, acceptance criteria, unit test expectations, UI test expectations where relevant, manual verification steps, and a definition of done.

## Related documents

| Topic | Document |
| --- | --- |
| Implementation order and parallelisation | `docs/delivery/00-implementation-order.md` |
| Dependency graph and critical path | `docs/delivery/01-issue-dependency-graph.md` |
| Definition of done (the full checklist) | `docs/delivery/02-definition-of-done.md` |
| Milestones | `docs/delivery/04-milestones.md` |
| Mapping from the original 26 stubs | `docs/delivery/05-issue-renumbering.md` |
| Test matrix | `docs/testing/01-test-matrix.md` |
| Using the prototype | `docs/ux/05-prototype-reference.md` |
| Visual fidelity and goldens | `docs/testing/04-visual-regression.md` |
| Localisation | `docs/i18n/00-localization.md` |
