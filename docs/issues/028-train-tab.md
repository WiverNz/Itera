# 028 - Train tab

**Phase** 5 - Learning-loop surfaces | **Depends on** 010, 012, 015 | **Blocks** none

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Train.kt`** - `TrainScreen`, `LinkRow`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the Train tab, matching artboard `c02` and `docs/ux/02-screen-specs-train-progress-you.md` section 1.

## User value

Answers "what am I learning?" - where you are in the program, what is due, and where everything else lives.

## Scope

- Header: "Train" with the subtitle "Your program, one technique at a time."
- Week header: "Week {n} - {name}" and "Day {d} of {total}", with week names from the curriculum (Foundations / Depth / Combine).
- The program path: a vertical rail of day nodes. Past days are muted with a small skill dot; **today** is an expanded card with a skill tile, the technique name, "Today - {Skill}" and a chevron; future days show the name and day label in `outlineDisabled`.
- A review card, shown only when a review is due: "Review due today" plus the prompt and a chevron.
- A library entry row: icon, "Library", "All 14 techniques - {n} unlocked", chevron.
- Auto-scroll so today's node sits one row below the week header on first composition, without animation.
- Past nodes open that day's History detail; future nodes are not tappable.

## Non-goals

- The library itself (issue 029) and technique detail (issue 030).
- Any way to jump ahead in the program.
- Editing the program.

## Implementation notes

- Future nodes are **not tappable**. Peeking at locked content happens through the Library, which is designed for it. Making path nodes tappable would create two answers to the same question.
- The week number is `ceil(programDay / 7)`; week names come from the curriculum's authored names for weeks 1-2 and "Combine" thereafter.
- "Day {d} of {total}" uses 14 as the total while `programDay <= 14`, and stops showing a total afterwards (there is no end to the generated program). Decide once and be consistent: past 14, show "Day {d}" alone.
- The review card shows the highest-priority due review (same ordering as the plan engine). If more than one is due, add a "+{n} more" line rather than a list - Train is a map, not a queue screen.
- Auto-scroll must be a `scrollToItem`, not `animateScrollToItem`, on first composition only. Re-scrolling on every recomposition is a common bug here.
- The path renders from the curriculum plus unlock state, both of which are already cached; this screen needs no new query beyond the review lookup.

## Affected layers

`feature/train`, `core/navigation`.

## Acceptance criteria

- [ ] Matches artboard `c02`.
- [ ] The week header shows the correct week number and name.
- [ ] Past, today and future nodes render with the documented styling.
- [ ] Today's node is expanded and tappable, opening the day's exercise.
- [ ] Past nodes open that day in History.
- [ ] Future nodes are not tappable and are announced as locked.
- [ ] The review card appears only when a review is due and opens it.
- [ ] Multiple due reviews show a "+{n} more" line rather than a list.
- [ ] The library row shows the correct unlocked count.
- [ ] The path auto-scrolls to today once, without animation, and does not re-scroll on recomposition.
- [ ] Past day 14, the header shows "Day {d}" with no total.
- [ ] The loading state shows a skeleton rail and keeps the library row usable.
- [ ] An error replaces the path only, leaving the library row working.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`TrainViewModelTest` - week number and name at days 1, 7, 8, 14, 15 and 30; node states across the path; the review card with 0, 1 and 3 due reviews; the unlocked count; the error path.

## UI test expectations

`TrainScreenTest` - the three node states render; future nodes are not clickable; the review card appears and navigates; the library row navigates; auto-scroll positions today correctly.

## Manual verification

1. At program days 1, 9 and 20, compare the path against the artboard.
2. With a due review, confirm the card appears and opens the review.
3. Tap a past day; it opens History at that day.
4. Confirm the path does not jump when you scroll and come back.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
