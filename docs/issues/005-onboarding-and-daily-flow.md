# 005 - Onboarding & daily flow

**Depends on** 003, 004 | **Blocks** 006, 008

## Goal

A clean install can be onboarded and driven through a complete Day 1 - Today, a template exercise, the evening reflection, Day complete - and into Day 2 with the carry-over banner.

## Included scope

- **Onboarding** (old 016): Welcome (with language pill), Goals, Rhythm (M3 time pickers, D-17), First week; notification permission request; writes preferences and generates Day 1; debug-only "Explore with demo data" (D-11).
- **Today** (old 017): greeting, carry-over banner, hero branches, step list, counter, rest-day and quiet completion states.
- **Evening reflection** (old 019): three questions on the night surface, pre-fill, chips, skip, weekly look-back header.
- **Day complete** (old 020): the summary screen and Today's quiet completion card.
- Strings for these screens in all four languages; analytics call sites for these flows.

The exercise **runner** is in 006. Until then, Today's hero and step rows navigate to the runner route placeholder; Day 1 end-to-end is verified once 006's runner lands (see acceptance criteria).

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/016-onboarding.md`, `017-today-screen.md`, `019-evening-reflection.md`, `020-day-complete.md`.
- `docs/ux/02-screen-specs-onboarding.md`, `02-screen-specs-today.md`, `03-ux-states.md`, `09-copy-deck.md`; `docs/prd/02-user-flows.md`; `docs/00-source-of-truth.md` sections 6 and D-11, D-16, D-17.
- Prototype: `design/app/src/main/java/com/itera/app/ui/screens/Onboarding.kt`, `Today.kt`, `Reflection.kt` (reflection and day complete).

## Key dependencies

- Needs 003 (components, shell, routes) and 004 (plan generation, lifecycle use cases, `ReminderScheduler` no-op).
- Reminders requested here are wired to real workers in 009.
- Day-1-to-Day-2 end-to-end needs the template runner from 006; the core-loop integration test is completed there.

## Acceptance criteria

- [ ] Onboarding matches the prototype; Goals keeps 1-2 selections (a third drops the oldest); time rows use real M3 pickers in the device 12/24h format; finishing pops the onboarding graph.
- [ ] Switching language during onboarding re-renders immediately and keeps entered values.
- [ ] Today answers "what now?" with one hero and a short list; greeting thresholds, hero branches, counter and carry-over banner behave per spec; no streak/percentage language.
- [ ] Reflection pre-fills from real activity, stores chips + text, supports "Skip tonight", and Q3 becomes tomorrow's carry-over verbatim.
- [ ] Day complete shows an honest summary and returns to a quiet Today with no new primary action.
- [ ] New strings exist in en/ru/de/es.
- [ ] Each screen was compared against the prototype in light and dark; only deviations are recorded.

## Required tests

`OnboardingViewModelTest`, `OnboardingScreenTest`, `FirstRunIntegrationTest`, `TodayMapperTest`, `TodayViewModelTest`, `TodayScreenTest`, `ProgressHonestyTest` (Today cases), `ReflectionPrefillTest`, `ReflectionViewModelTest`, `ReflectionScreenTest`, `ReflectionIntegrationTest`, `DayCompleteViewModelTest`, `DayCompleteScreenTest`, `DayCompleteIntegrationTest`.
