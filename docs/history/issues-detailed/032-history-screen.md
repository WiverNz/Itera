# 032 - History screen

**Phase** 5 - Learning-loop surfaces | **Depends on** 013, 015, 031 | **Blocks** 037

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Progress.kt`** - `HistoryScreen`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the History screen, matching artboard `c05` and `docs/ux/02-screen-specs-train-progress-you.md` section 5.

## User value

Everything you actually did, with your own notes, month by month - including the days you rested.

## Scope

- Header: back, "History", "{Month} {year}", and previous/next month chevrons.
- A 7-column calendar whose **first day of week comes from the locale** (`WeekFields.of(locale).firstDayOfWeek`), with `NARROW` weekday names. Each cell shows its date and up to **3** skill dots.
- A legend: five skill dots with names.
- Entries grouped by day, newest first, with day headers ("Today", "Mon, Sep 21").
- Each entry: a skill-tinted icon tile, a title, and the note or result summary.
- Rest days render their header plus "Rest day. No training logged."
- Tapping a calendar cell scrolls the list to that day; tapping an entry opens its read-only result.
- Month navigation bounded by `program_started_on` and the current month.

## Non-goals

- Editing or deleting history.
- Search or filtering.
- Paging (`docs/architecture/06-dependency-catalog.md` section 4) - revisit above ~2 000 rows.

## Implementation notes

- **Rest days are shown, not hidden.** This is a product decision, not an oversight. The calendar tells the truth and the copy never scolds.
- Month navigation is bounded in both directions. Disable the chevron rather than hiding it, and give it a state description.
- A cell's dots are the distinct skills practised that day, capped at **3** (the prototype's `skills.take(3)`) and ordered by the fixed skill order - not by count, which would make the calendar shimmer as data changes.
- Cells are `weight(1).aspectRatio(0.95f)`, radius 12, `surface` background only when the day has activity, 2 dp `ink` border on today, future dates in `ink3`.
- Month navigation is a **production addition** (P-06); the prototype shows only the current month.
- Entry summaries reuse the type-specific logic from issue 030; extract it to a shared function rather than writing it twice. If issue 030 landed first, reuse; if not, write it here and have 030 reuse it.
- Calendar cell accessibility: "{weekday} {date}, {n} activities: Focus, Habits" or "{weekday} {date}, no training".
- Tapping a cell uses `scrollToItem` on the list's day header, not a navigation.
- One `Flow` per visible month; do not hold all months in memory.
- A skipped reflection appears as an entry reading "Reflection - skipped", not as an absence.

## Affected layers

`feature/progress` (history package), `core/navigation`.

## Acceptance criteria

- [ ] Matches artboard `c05`.
- [ ] The calendar's first day of week follows the locale, with correct weekday names and date alignment for any month.
- [ ] Cells show up to 3 skill dots in fixed skill order.
- [ ] Month headers use the standalone month form, title-cased per locale.
- [ ] The legend renders all five skills.
- [ ] Entries are grouped by day, newest first, with "Today" for the current day.
- [ ] Each entry shows a type-appropriate summary and the user's note when present.
- [ ] Rest days render with the documented copy.
- [ ] A skipped reflection appears as a skipped entry.
- [ ] Tapping a cell scrolls the list to that day.
- [ ] Tapping an entry opens its read-only result.
- [ ] Month navigation is bounded; out-of-range chevrons are disabled with a state description.
- [ ] A month with no data shows an empty calendar and "No training logged in {Month}."
- [ ] Calendar cells expose the documented accessible descriptions.
- [ ] An unreadable payload degrades one entry only.
- [ ] Scrolling is smooth with a year of seeded data.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`HistoryViewModelTest` - calendar layout for a month starting on each weekday and for February in a leap year; dot sets capped at 4; grouping and ordering; rest days; a skipped reflection; month bounds at the program start and the current month; an unreadable payload.

## UI test expectations

`HistoryScreenTest` - the calendar renders; a rest-day row appears; tapping a cell scrolls; out-of-range chevrons are disabled; the empty-month state renders.

## Manual verification

1. Seed several months; navigate back to the program start and confirm the chevron disables.
2. Confirm a rest day is visible and reads honestly.
3. Tap a cell with several activities; the list scrolls to it.
4. Open an entry; it opens read-only and back returns to the same scroll position.
5. Read a calendar cell with TalkBack.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
