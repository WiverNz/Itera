# Implementation order

40 issues in 7 phases (001-037, 039-041; 038 was retired - see `05-issue-renumbering.md`). The order is a valid topological sort of `01-issue-dependency-graph.md`; any other valid sort works, but this one minimises rework and keeps the app runnable at the end of every phase.

**Rule: do not start an issue until every issue in its `Dependencies` list is merged.** Each issue is written to be implementable without making a product or architecture decision; if one is needed, stop and update `docs/00-source-of-truth.md` first.

**Rule: for any UI issue, run `design/` first.** The prototype is the implementation reference (`docs/ux/05-prototype-reference.md`). Issues 003, 004 and 015-034 each name the file to open.

**Rule: for any UI issue, run `design/` first.** The prototype is the implementation reference (`docs/ux/05-prototype-reference.md`). Issues 003, 004 and 015-034 each name the file to open.

## Phase 1 - Foundation (issues 001-008)

The app builds, stores data, and knows its content.

| # | Issue | Depends on |
| --- | --- | --- |
| 001 | Project bootstrap and dependency baseline | - |
| 002 | Architecture foundation (Hilt, Clock, dispatchers, Logger, ArchitectureTest) | 001 |
| 003 | Design system: tokens and theme | 001 |
| 004 | Design system: components | 003 |
| 005 | Domain model | 002 |
| 006 | Room persistence foundation | 005 |
| 007 | DataStore preferences | 005 |
| 008 | Technique catalog and content | 005, 006 |

**Exit**: `./gradlew build` passes, a debug app launches to a placeholder, the database opens and seeds, the catalog parses and validates.

## Phase 2 - Engine (issues 009-014)

Every product rule exists and is tested, with no UI.

| # | Issue | Depends on |
| --- | --- | --- |
| 009 | Exercise state machine | 005 |
| 010 | Unlock rules | 005, 008 |
| 011 | Daily plan generator | 006, 007, 008, 009, 010 |
| 012 | Spaced repetition scheduler | 005, 006 |
| 013 | Mastery and progress calculation | 006, 008 |
| 014 | Day lifecycle and completion effects | 009, 011, 012, 013 |

**Exit**: the full daily-loop logic passes unit tests from a seeded database, driven by a fake clock, with no Activity involved.

## Phase 3 - Shell and core loop (issues 015-020)

A new user can onboard, train, reflect and close a day.

| # | Issue | Depends on |
| --- | --- | --- |
| 015 | Navigation graph and app shell | 004, 007 |
| 016 | Onboarding | 007, 008, 011, 015 |
| 017 | Today screen | 011, 014, 015 |
| 018 | Exercise runner and template body | 008, 014, 015 |
| 019 | Evening reflection | 014, 018 |
| 020 | Day complete | 014, 019 |

**Exit**: a clean install can be onboarded and driven through a complete Day 1 and into Day 2, using only template techniques.

## Phase 4 - Specialised exercises (issues 021-027)

Every technique has its real experience.

| # | Issue | Depends on |
| --- | --- | --- |
| 021 | Focus timer | 018 |
| 022 | Eisenhower matrix | 018 |
| 023 | Feynman exercise | 012, 018 |
| 024 | Premortem | 018 |
| 025 | Habit stacking | 018 |
| 026 | Spaced repetition review screen | 012, 018 |
| 027 | Combination day runner | 018, 021, 022 |

021-026 are mutually independent and can be done in any order or in parallel. 027 needs the timer and Eisenhower because the Day-14 chain uses both.

**Exit**: Days 1-14 are all completable with their intended experiences.

## Phase 5 - Learning-loop surfaces (issues 028-032)

The user can see what they are learning and how far they have come.

| # | Issue | Depends on |
| --- | --- | --- |
| 028 | Train tab | 010, 012, 015 |
| 029 | Technique library | 008, 013, 015 |
| 030 | Technique detail | 013, 018, 029 |
| 031 | Progress screen | 013, 015 |
| 032 | History screen | 013, 015, 031 |

**Exit**: all four tabs are complete and every screen in the inventory exists.

## Phase 6 - Infrastructure (issues 033-036)

The app reminds, configures, exports and records.

| # | Issue | Depends on |
| --- | --- | --- |
| 033 | Notifications and WorkManager | 011, 014, 017 |
| 034 | Settings / You tab | 007, 033 |
| 035 | Journal export | 013, 034 |
| 036 | Analytics contract | 002, 006 |

**Exit**: reminders fire, settings work, the journal exports, and the event log records. The app already runs in all four languages - localisation landed across issues 001-034, not here (D-15).

## Phase 7 - Hardening (issues 037, 041, 039, 040)

| # | Issue | Depends on |
| --- | --- | --- |
| 037 | Accessibility pass | all UI issues |
| 041 | Localisation verification and audit | 037 |
| 039 | Test hardening, golden images and CI gates | all |
| 040 | Release readiness | all |

Phase 7 runs in that order: accessibility can move layouts, so the localisation audit follows it, and the golden set is recorded only once both have settled.

**Exit**: `docs/prd/09-mvp-acceptance-criteria.md` passes in full.

## Parallelisation

If more than one agent works at once:

| Phase | Parallel tracks |
| --- | --- |
| 1 | {001 -> 002 -> 005} and {001 -> 003 -> 004} are independent after 001 |
| 2 | 009, 010, 012, 013 are independent of each other; 011 and 014 join them |
| 4 | 021-026 are six independent tracks |
| 5 | 028, 029, 031 are independent; 030 follows 029; 032 follows 031 |
| 6 | 036 is independent of 033-035 |

Everything else is sequential.

## Runnable checkpoints

The app should be launchable and non-broken at the end of every phase. Specifically:

| After issue | The app can |
| --- | --- |
| 004 | Show a themed placeholder screen with the real components |
| 015 | Navigate all four empty tabs |
| 017 | Show a real Today from a seeded plan |
| 020 | Run a complete day end to end |
| 027 | Run every technique |
| 032 | Be used as the finished product, minus reminders |
| 036 | Be feature complete, and usable end to end in English, Russian, German or Spanish |
| 041 | Be used end to end in English, Russian, German or Spanish |
| 040 | Be released internally |
