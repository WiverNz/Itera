# 020 - Day complete

**Phase** 3 - Shell and core loop | **Depends on** 014, 019 | **Blocks** 037

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Reflection.kt`** - `DayCompleteScreen`, `DayRing`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the Day complete screen (artboard `a12`) and the quiet day-complete state on Today.

## User value

A clear, calm end to the day, and an app that then stops asking for anything.

## Scope

- The Day complete screen on `NightSurface`: a 3/3 chip, "Day {n} complete", the summary line ("Exercise, focus, reflection. That's a full training day."), a card echoing the user's change for tomorrow with "It'll be waiting on tomorrow's Today screen", a preview card ("Tomorrow - Day {n+1}: {technique}. Unlocks at {morning time}"), and "Good night".
- "Good night" pops to `TodayHome`; system back does the same.
- The quiet Today state per `docs/ux/02-screen-specs-today.md` section 7: the hero replaced by a completion card, the checklist fully ticked, no new primary action.
- `last_seen_day_complete` preference so the screen is not re-shown for the same day.
- Cancel any remaining reminders for the day on arrival.

## Non-goals

- The reflection itself (issue 019).
- Tomorrow's plan generation - the preview reads the curriculum, it does not generate a plan.

## Implementation notes

- The "tomorrow" preview reads `curriculum.days[programDay]` (the already-advanced day). It must **not** call the plan generator - generating tomorrow's plan tonight would fix it before the user's preferences or reviews change.
- If the reflection was skipped, the summary line must adapt honestly: name what was actually done rather than claiming "exercise, focus, reflection". Build the line from the day's completed sources.
- If the change-for-tomorrow is empty (skipped, or left blank), omit that card entirely rather than showing an empty one.
- Reaching Day complete twice for the same day (back, then forward again) is harmless and shows the same content; `last_seen_day_complete` only prevents an automatic re-entry.
- The quiet Today state is implemented in this issue, not 017, because it depends on day completion existing. Coordinate: issue 017 leaves the hook, this issue fills it.

## Affected layers

`feature/daycomplete`, `feature/today` (the quiet state), `data/preferences`.

## Acceptance criteria

- [ ] Matches artboard `a12`, including the night surface in light theme.
- [ ] The day number, summary line and tomorrow preview are correct.
- [ ] The summary line reflects what was actually done, including when the reflection was skipped.
- [ ] The change card is omitted when there is no change text.
- [ ] The tomorrow preview names the next curriculum technique and the morning time, without generating a plan.
- [ ] "Good night" and system back both return to Today.
- [ ] Today then shows the quiet completion card with no new primary action.
- [ ] Remaining reminders for the day are cancelled.
- [ ] Returning to the app later the same evening shows the quiet Today, not a new hero.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`DayCompleteViewModelTest` - content for a fully completed day; for a day with a skipped reflection; for a day with no change text; the tomorrow preview at program days 1, 13, 14 and 20 (past the authored curriculum).

## UI test expectations

`DayCompleteScreenTest` - renders all elements; the change card is absent when empty; "Good night" navigates.

`TodayScreenTest` additions - the quiet state renders and has no primary button.

## Integration test expectations

- `DayCompleteIntegrationTest` - complete every activity, reach Day complete, return to Today, confirm the quiet state and that `programDay` advanced exactly once.

## Manual verification

1. Complete a full day; the screen names the right day and technique.
2. Skip the reflection; the summary line is honest about it.
3. Return to Today; nothing new is offered.
4. Reopen the app an hour later; still quiet.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
