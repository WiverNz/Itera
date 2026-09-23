# 022 - Eisenhower matrix

**Phase** 4 - Specialised exercises | **Depends on** 018 | **Blocks** 027

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Practice.kt`** - `EisenhowerScreen`, `QuadrantBox`, `AxisLabel`, `SideLabel`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the Eisenhower sorter, matching artboard `b02` and `docs/ux/02-screen-specs-exercise.md` section 2.3.

## User value

A concrete sorting move the user makes with their own real tasks, ending in one thing chosen to do now.

## Scope

- An entry step collecting 5-7 tasks, one per line.
- The inbox strip: unsorted tasks as chips; the selected one inverted; a hint that changes from "Tap a task" to "Now tap a square".
- The 2x2 matrix with URGENT / NOT URGENT column headers and IMPORTANT / NOT IMPORTANT row labels, and the four quadrants: Do now, Schedule, Delegate, Drop - each with its subtitle and its placed tasks.
- Tap-a-task-then-tap-a-quadrant placement, auto-advancing to the next unsorted task.
- The all-sorted state: "All sorted. Pick one from Do now."
- Choosing one task from Do now as `chosenItemId`.
- `ActivityResult.Eisenhower` persistence and draft support.
- Primary button label: "Continue with 80/20" on a combination day, "Finish exercise" otherwise.

## Non-goals

- Drag and drop. The artboard's interaction is tap-then-tap, which is also far more accessible.
- Editing a task after entry (a placed task can be re-placed, but not renamed).
- Carrying tasks between days - each exercise starts fresh.

## Implementation notes

- Tap-then-tap, not drag. It is what the artboard's logic does, it works with TalkBack and Switch Access, and it needs no gesture handling.
- Auto-advance after placement: select the next unsorted task automatically, so the common case is a single tap per task.
- Quadrant buttons are disabled when no task is selected, with `stateDescription = "Pick a task first"` - not silently inert.
- Quadrant accessibility: "Do now, urgent and important, 2 tasks. Double tap to place {selected task}." The count and the pending action both matter.
- At `fontScale 2.0` the 2x2 keeps its shape and scrolls internally rather than collapsing to a list - the spatial layout is the technique.
- Re-placing an already-placed task is allowed: tapping it selects it again.
- The draft must capture the full item list with quadrants, so a mid-sort process death loses nothing.

## Affected layers

`feature/exercise/eisenhower`.

## Acceptance criteria

- [ ] Matches artboard `b02`.
- [ ] The entry step accepts 5-7 tasks and requires at least 4 to continue.
- [ ] Tapping a task selects it; tapping a quadrant places it and auto-selects the next unsorted task.
- [ ] The hint changes between "Tap a task" and "Now tap a square".
- [ ] Quadrants are disabled with an explanatory state description when nothing is selected.
- [ ] Each quadrant shows its placed tasks.
- [ ] The all-sorted state appears when the inbox empties.
- [ ] One task can be chosen from Do now, and is stored as `chosenItemId`.
- [ ] A placed task can be re-selected and moved.
- [ ] The result persists as `ActivityResult.Eisenhower` with every item and its quadrant.
- [ ] Mid-sort process death restores the exact state.
- [ ] The primary label changes on a combination day.
- [ ] The 2x2 keeps its shape and scrolls internally at `fontScale 2.0`.
- [ ] Fully operable with TalkBack and Switch Access.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`EisenhowerViewModelTest` - task entry validation; selection; placement; auto-advance; auto-advance when the placed task was the last; re-placement; the all-sorted transition; choosing a Do-now task; draft round-trip.

## UI test expectations

`EisenhowerScreenTest` - the entry step; select-then-place; the hint text changes; disabled quadrants expose their state description; the all-sorted message; the 2x2 renders at `fontScale 2.0`.

## Manual verification

1. Sort seven real tasks; confirm auto-advance makes it one tap each.
2. Move a placed task to a different quadrant.
3. Kill the app mid-sort; reopen; the board is as you left it.
4. Complete it with TalkBack only.
5. Check at maximum font size.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
