# Issue renumbering

The original backlog had 26 stub issues. `docs/issues/README.md` stated that numbering was "a suggested dependency-friendly implementation sequence, not immutable numbering".

The refined backlog has **40** issues, numbered 001-037 and 039-041. The increase comes from three sources: splitting issues that bundled unrelated work, adding issues for the five surfaces the design shows but the backlog omitted (R-15), and separating cross-cutting hardening into its own issues.

## 1. Mapping

| Old | Old title | New | New title | Note |
| --- | --- | --- | --- | --- |
| 001 | Project bootstrap | 001 | Project bootstrap and dependency baseline | unchanged in intent |
| 002 | Architecture foundation | 002 | Architecture foundation | narrowed to DI, clock, dispatchers, logging, boundary test |
| 003 | Design system | 003, 004 | Design tokens and theme; Design system components | split: tokens are a dependency of everything, components are a large body of work |
| 004 | Domain model | 005 | Domain model | |
| 005 | Local persistence | 006, 007 | Room persistence foundation; DataStore preferences | split: two different stores, two different test approaches |
| 006 | Technique catalog | 008 | Technique catalog and content | now also owns the curriculum asset and the 112 content strings |
| 007 | Training engine | 009, 010, 011, 014 | State machine; Unlock rules; Plan generator; Day lifecycle | split: the original issue bundled four independently testable engines |
| 008 | Onboarding | 016 | Onboarding | |
| 009 | Today screen | 017 | Today screen | |
| 010 | Generic exercise runner | 018 | Exercise runner and template body | now explicitly includes the data-driven template (ADR-0007) |
| 011 | Focus timer | 021 | Focus timer | |
| 012 | Eisenhower matrix | 022 | Eisenhower matrix | |
| 013 | Feynman exercise | 023 | Feynman exercise | |
| 014 | Premortem exercise | 024 | Premortem exercise | |
| 015 | Habit stacking | 025 | Habit stacking | |
| 016 | Daily reflection | 019 | Evening reflection | moved earlier: it is part of the core loop, not a specialised extra |
| 017 | Spaced repetition | 012, 026 | Spaced repetition scheduler; Review screen | split: engine and UI |
| 018 | Progress | 013, 031 | Mastery and progress calculation; Progress screen | split: the calculation is a Phase 2 engine, the screen is Phase 5 |
| 019 | Technique library | 029, 030 | Technique library; Technique detail | split: two screens |
| 020 | History | 032 | History screen | |
| 021 | Notifications | 033 | Notifications and WorkManager | |
| 022 | Settings | 034 | Settings / You tab | |
| 023 | Analytics contract | 036 | Analytics contract | |
| 024 | Accessibility and localization | 037, 038 | Accessibility pass; Localization readiness | split: different skills, different verification |
| 025 | Test hardening | 039 | Test hardening and CI gates | |
| 026 | Release readiness | 040 | Release readiness | |

## 2. New issues with no predecessor

| New | Title | Why it exists |
| --- | --- | --- |
| 015 | Navigation graph and app shell | The original backlog had no issue for the four-tab shell, because the PRD did not describe one (R-01) |
| 041 | Localisation verification and audit | Four shipping languages are an explicit requirement. The feature itself is built across issues 001-034; `041` is the final audit (D-03, D-15) |
| 020 | Day complete | A drawn screen the backlog omitted (R-15) |
| 027 | Combination day runner | Drawn, and the only way to earn `Integrated` (R-15) |
| 028 | Train tab | Drawn; the backlog's "technique library" covered only part of it (R-15) |
| 035 | Journal export | Drawn in the You tab; moved into MVP scope (R-09) |

## 2a. Retired numbers

| Number | Was | Why it was retired |
| --- | --- | --- |
| 038 | "Localization readiness", then "Localisation verification pass" | Duplicated `041` once localisation became four shipping languages audited by a single issue (D-15). Its content was folded into `041`. The number is not reused. |

## 3. Renumbering rules for the future

- New issues take the next free number; they are never inserted between existing ones.
- A split keeps the lowest new number for the piece that retains the original intent.
- Closed issue numbers are never reused.
- This table is not updated after implementation starts - it is a record of one renumbering, not a living index. The living index is `docs/issues/README.md`.
