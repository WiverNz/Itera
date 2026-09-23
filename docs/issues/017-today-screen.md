# 017 - Today screen

**Phase** 3 - Shell and core loop | **Depends on** 011, 014, 015 | **Blocks** 033

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Today.kt`** - `TodayScreen`, `HeroCard`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the Today screen and its four states exactly as specified in `docs/ux/02-screen-specs-today.md`.

## User value

The product principle, made real: open the app, know what to do, in a few seconds.

## Scope

- The screen anatomy in section 1: header, optional carry-over banner, hero card, checklist, bottom bar.
- Greeting and date-line rules (section 2), recomputed on `ON_RESUME`.
- Carry-over banner (section 3).
- Hero selection by the prototype's branch order (D-06): exercise not done -> exercise; focus not done and `hour < 18` -> focus; reflection not done -> reflection; otherwise the day-done card whose action opens Day complete. **Plus** a due review taking priority over the focus branch (P-01).
- `StepRow` per activity in the plan, with the real `done / total` counter - the prototype hard-codes three steps and a denominator of 3; production renders the plan (P-01).
- The five screen states in section 6, including the day-complete state.
- `EnsureTodayPlanUseCase` called in `init`; the header renders before the plan resolves.

## Non-goals

- The exercise runner (issue 018) - Today only navigates to it.
- Notifications (issue 033).
- Pull-to-refresh - explicitly not implemented.

## Implementation notes

- The header must render immediately from known state (greeting from the clock, day chip from preferences). Only the hero and checklist show skeletons, and only after 150 ms.
- Hero selection is a short ordered `when`; write it as a single chain so it is obviously total and testable in isolation. Follow the prototype's order exactly, inserting the review branch before focus.
- The counter denominator counts **all** activities in the day, optional ones included - which is 3 on a normal day, matching the prototype, and 4 when a review is due. `requiredCount`, used for day completion, is a different number. Name both clearly; this is the single most confusable thing on this screen.
- The undo snackbar must actually undo: reverting the activity to `AVAILABLE` and deleting nothing else. It is a state transition, not a delete.
- Day-complete state replaces the hero with a quiet card and offers **no new action**. Do not add a "practice something else" button - the product deliberately stops producing work.
- `mapToUiState` must be a pure top-level function so the six hero variants and seven row states can be table-tested without a ViewModel.
- The breathing dot honours `LocalReduceMotion`.

## Affected layers

`feature/today`, `core/navigation`.

## Acceptance criteria

- [ ] Greeting thresholds are correct at 11:59, 12:00, 17:59 and 18:00, and update on resume.
- [ ] The focus hero branch does not appear at or after 18:00.
- [ ] The carry-over banner appears when the previous day recorded an intent, and disappears once the hero activity completes.
- [ ] Each hero branch renders its documented eyebrow, badge, token, title and button.
- [ ] The hero eyebrow uses the hero technique's skill colour.
- [ ] `StepRow` renders `Done`, `Now` and `Next` with the documented indicator, subtitle and strikethrough.
- [ ] The counter counts every activity, optional included, and becomes `/ 4` when a review is due.
- [ ] A due review renders as a step row and as the hero when the exercise is done.
- [ ] Tapping a completed row opens its result read-only.
- [ ] The loading state shows the header immediately and skeletons only after 150 ms.
- [ ] A plan-generation failure shows an error card in place of the hero, keeps any stored checklist, and offers "Try again".
- [ ] The day-complete state offers no new primary action.
- [ ] Hero card, checklist rows and the counter expose the documented accessibility semantics.
- [ ] Works at `fontScale 2.0`.
- [ ] The header is visible within 300 ms of `onCreate` on a warm start.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`TodayViewModelTest` - initial loading state; content emission; each event; the error path; `ensurePlan` called once in `init`; practice-prompt completion and undo.

`TodayMapperTest` - a table over: each of the six hero priorities; each of the seven row states; the counter with and without optional activities; the day-complete state; a day with a due review.

## UI test expectations

`TodayScreenTest`:
- the Day 1, midday, Day 2 and Day 9 fixtures each render their documented elements;
- the practice-prompt tap completes in place and the snackbar's undo restores;
- the error state renders and "Try again" fires the event;
- the day-complete state has no primary button;
- the counter's accessible description reads "0 of 3 done";
- the hero card merges into one node with the button as a child.

`ProgressHonestyTest` additions - Today shows no streak or points language.

## Manual verification

1. Walk Day 1 -> midday -> Day 2 -> Day 9 with seeded data and compare each against the prototype loaded with demo data.
2. Change the system clock across 12:00 and 18:00; the greeting updates on resume.
3. Complete the day; confirm Today goes quiet and offers nothing new.
4. Run with TalkBack; the hero reads as one sentence.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
