# 005 - Onboarding & daily flow

**Depends on** 003, 004 | **Blocks** 006, 008

**Status (2026-09-25): implemented.** All required tests pass (`./gradlew build`). Open items: the side-by-side prototype comparison in light and dark (below), and the Day-1-to-Day-2 path through a real exercise, which waits on 006's runner as planned. Decisions are recorded in `docs/00-source-of-truth.md`, "Milestone 005 implementation decisions".

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

## Deviations (2026-09-25)

Recorded per "Verification scope"; each is required by a product or engine rule, and new visual elements were added to `design/` in the same change.

- **Engine availability wins over the prototype's always-tappable rows.** Rows map from `ActivityState` per `docs/engine/00` section 1 (SCHEDULED/SNOOZED -> `Next`, SKIPPED -> `Done` in the line colour with "Skipped"). A hero whose activity is still SCHEDULED (the focus block before its daytime slot, the reflection before evening - 60 min) keeps its card but its button is disabled.
- **A completed day stops producing work.** Once the day is COMPLETE the hero is the day-done card regardless of the hour; after Day complete has been seen (`last_seen_day_complete`) it is the quiet card with no button.
- **Day complete generalises to the real plan (P-01).** The ring draws one arc per row of the day with that row's skill colour and shows `{done}/{total}`; the prototype's full-day line is shown only when exercise, focus and reflection were all done, otherwise "Done today: ..." (with "Reflection skipped tonight" when it was).
- **Practice prompts complete in place with a 5 s undo window**; the completion is committed after the window, so undo writes nothing. Completed rows are not clickable (prototype and screen spec), superseding detailed 017's "opens its result read-only".
- **Added to both apps:** the notification rationale card on Rhythm and the weekly look-back card on the reflection (Days 7, 14, 21).
- **Onboarding view model** is scoped to the Welcome back-stack entry (the route set is flat, ADR-0008) rather than a nested `OnboardingGraph`.

## Verification gap

The production screens were exercised on an emulator (Welcome -> Goals -> Rhythm with permission request -> First week -> Today, Day 1), but the side-by-side comparison against `design/` in light and dark has not been done for these screens. It stays open alongside 003's pending visual acceptance.
