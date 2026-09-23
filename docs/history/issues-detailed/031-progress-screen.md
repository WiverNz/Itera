# 031 - Progress screen

**Phase** 5 - Learning-loop surfaces | **Depends on** 013, 015 | **Blocks** 032

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Progress.kt`** - `ProgressScreen`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the Progress screen, matching artboard `c01` and `docs/ux/02-screen-specs-train-progress-you.md` section 4.

## User value

An honest answer to "am I actually getting better?" - with no streak, no score, and no reason to feel bad about a missed day.

## Scope

- Header: "Progress".
- Summary: "{n} of the last 10 days" and "since {date}".
- The day strip: one **bar** per day over a window that grows to 14 - `weight(1)`, height 28, radius 8, `ink` when practised and `surface2` otherwise, 2 dp `accent` border on today (D-07).
- The standing reassurance line: "Missed days don't reset anything. The program simply continues where you left off."
- Five skill rows in fixed `Skill` enum order: name in `label`, level label in the skill content colour, a **4**-segment bar (one per band), and the plural practice count.
- A History entry row: icon, "History", "{n} activities - notes and reflections", chevron.
- The first-day state: all skills "Starting", empty bars, "No practice yet", one filled dot - a legitimate state, not an empty state.

## Non-goals

- Any configurability, date range picker, or chart.
- History itself (issue 032).
- Any metric not listed in `docs/engine/03-mastery-and-progress.md` section 4.

## Implementation notes

- **What must never appear here** (`docs/engine/03-mastery-and-progress.md` section 5): a streak or "days in a row" phrase, a points or XP total, a percentage-complete, a comparison with others, or any red/warning treatment for a missed day. `ProgressHonestyTest` asserts this and is part of this issue.
- The day strip and the skill window are both **14 days**; the prototype uses one span for both (`span = daysSinceStart + 1`, capped at 14). The two-window distinction in the older docs is gone.
- Skill level: `score = practiceCount + activeDays`, banded at 20 / 10 / 4 / 1 (D-09, `docs/engine/03-mastery-and-progress.md` section 3).
- Each of the four bars is filled when `index < level.ordinal + 1`, animating `tween(700, delayMillis = 80 * index)`.
- The detail line is `pluralStringResource(R.plurals.practices, count)` - one plural, no special cases.
- Progress links to **both** History and Library, as the prototype does (Q-06).
- The reassurance line is **static and always shown** - not conditional on having missed days. Making it conditional would turn it into a reproach.
- A dot's colour is that day's dominant skill (most completions; ties broken by fixed skill order).
- Loading shows skeleton bars but keeps the header and the reassurance line.

## Affected layers

`feature/progress`, `core/navigation`.

## Acceptance criteria

- [ ] The summary counts trained days in the window, which grows to 14 and stops.
- [ ] The day strip renders one bar per day, with today outlined in `accent`.
- [ ] The reassurance line is always present.
- [ ] Five skill rows render in `Skill` enum order.
- [ ] The 4-segment bars fill to the band and animate with the documented stagger.
- [ ] The detail line uses the `practices` plural and is correct in Russian at 1, 2 and 5.
- [ ] Levels computed from the demo fixture match what the prototype shows for the same data.
- [ ] The History row shows the correct all-time activity count.
- [ ] The first-day state renders as a legitimate state with no empty-state treatment.
- [ ] `ProgressHonestyTest` passes: no streak, points or percentage language anywhere on the screen.
- [ ] No warning colour is used for an untrained day.
- [ ] First frame within 300 ms with 1 500 seeded activities.
- [ ] Skill rows merge into single accessible nodes reading "Focus, Steady. 11 practices on 7 of 9 days."
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`ProgressViewModelTest` - the demo fixture's five rows; an empty database; a window with gaps; the 14-day cap; each band boundary.

## UI test expectations

`ProgressScreenTest` - all elements render; the first-day state; skeleton loading keeps the header.

`ProgressHonestyTest` - the rendered semantics tree contains none of: "streak", "in a row", "XP", "points", "% complete". Extend `NoStreakLanguageTest` to scan all string resources in this issue.

## Integration test expectations

- `ProgressPerformanceTest` - 1 500 activities, first frame within 300 ms.

## Manual verification

1. Load demo data in both the prototype and the app; compare every number, label and bar.
2. Skip three days; confirm hollow dots, no warning colour, and the reassurance line unchanged.
3. Fresh install; confirm the first-day state looks intentional, not broken.
4. Read a skill row with TalkBack.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
