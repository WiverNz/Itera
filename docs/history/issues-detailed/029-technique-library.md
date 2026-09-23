# 029 - Technique library

**Phase** 5 - Learning-loop surfaces | **Depends on** 008, 013, 015 | **Blocks** 030

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Train.kt`** - `LibraryScreen`, `MasteryDots`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the library, matching artboard `c03` and `docs/ux/02-screen-specs-train-progress-you.md` section 2.

## User value

All 14 techniques, visible from day one, including the ones not yet unlocked - because knowing what is coming is motivating, and peeking costs nothing.

## Scope

- Header: back, "Library", and the intro line "All 14 techniques. Your program unlocks them in order - you can open any of them to peek."
- A sticky single-select filter row: All / Focus / Planning / Learning / Habits / Reflection.
- 14 rows in a `LazyColumn`.
- **Unlocked row**: 48 dp skill tile, name, short description, a 6-segment compact mastery strip, and the level label.
- **Locked row**: a `surfaceVariant` tile with a lock glyph, name and description in `outlineDisabled`, and a "Day {introDay}" chip with a lock icon in place of the mastery strip.
- Sort: unlocked first by descending mastery then ascending `introDay`, then locked by ascending `introDay`.
- Every row tappable, locked included.

## Non-goals

- Technique detail (issue 030).
- Search - 14 items filtered by skill is enough.
- Any way to unlock early.

## Implementation notes

- The compact mastery strip has 6 segments filled 0 / 2 / 3 / 5 / 6 for NONE / MET / PRACTICED / APPLIED / INTEGRATED. This is the artboard's rendering; it is deliberately coarser than the four-step `MasteryLadder` used on the detail and result screens.
- Sorting must reproduce the artboard's order for the Day-9 fixture. Assert that fixture in a test rather than trusting the comparator by eye.
- Filtering to a skill with no unlocked techniques still shows its locked ones - so there is no empty state on this screen.
- The catalog is an in-memory asset, so there is no loading state; mastery comes from a `Flow`, so the strips fill in a frame after the rows. Render the rows immediately with empty strips rather than skeletoning the whole list.
- Locked rows must be announced as locked, with the unlock day: "Premortem, locked, unlocks on day 12."
- The filter row is sticky under the header; use `stickyHeader` or a pinned `Column`, not a nested scroll.

## Affected layers

`feature/train` (library package), `core/navigation`.

## Acceptance criteria

- [ ] Matches artboard `c03`.
- [ ] All 14 techniques are listed at every program day, including Day 1.
- [ ] Unlocked and locked rows render with the documented styling.
- [ ] The mastery strip fills 0/2/3/5/6 for the five levels.
- [ ] Locked rows show "Day {introDay}" with a lock icon.
- [ ] The sort order reproduces the artboard for the Day-9 fixture.
- [ ] The filter row is sticky and single-select, defaulting to All.
- [ ] Filtering to a skill shows both its unlocked and locked techniques.
- [ ] Every row is tappable and opens technique detail.
- [ ] Locked rows announce their locked state and unlock day.
- [ ] Rows render immediately; mastery strips fill in without a full-list skeleton.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`LibraryViewModelTest` - all 14 present at day 1 and day 20; sort order for the Day-9 fixture and for an all-locked state; each filter's result set; the unlocked count.

## UI test expectations

`LibraryScreenTest` - 14 rows render; a locked row shows its day chip and the correct announcement; the filter narrows the list; the sticky filter stays visible while scrolling; tapping a locked row navigates.

## Manual verification

1. At Day 1, confirm 13 locked rows with correct day chips.
2. At Day 9, compare the order against the artboard.
3. Filter each skill; confirm counts of 4 / 3 / 2 / 3 / 2.
4. Open a locked technique; it opens rather than doing nothing.
5. Read a locked row with TalkBack.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
