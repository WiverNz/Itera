# 019 - Evening reflection

**Phase** 3 - Shell and core loop | **Depends on** 014, 018 | **Blocks** 020

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Reflection.kt`** - `ReflectionScreen`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the three-question evening reflection on the night surface, matching artboard `a11`.

## User value

Two minutes to close the day honestly, with the first answer already written for you.

## Scope

- The reflection screen on `NightSurface` (dark in both themes, R-08).
- Header: 44 dp close, "Evening reflection", a "2 min" chip.
- Title "How was today?".
- Three numbered steps, revealed progressively: completed steps collapse to their answer, the current step is expanded, future steps are dimmed.
  1. **What went well today?** - **pre-filled** from the day's completed activities and focus sessions, editable.
  2. **What didn't go well?** - four suggestion chips (Started late, Checked email first, Got distracted, Ran out of time) plus a one-line field.
  3. **What will you change tomorrow?** - one line.
- "Next" -> Day complete. "Skip tonight" -> records `skipped` and still goes to Day complete.
- Weekly look-back variant: when the activity carries the flag, the screen opens with a summary header (days trained this week, techniques practised, focus minutes, last week's changes) above question 1.
- Persist to `reflection_entry` and complete the linked activity.

## Non-goals

- The Day complete screen (issue 020).
- The evening notification (issue 033).

## Implementation notes

- The pre-fill for question 1 is generated, not typed by the user, and must be **editable**. Build it from the day's completed activities: technique names, focus minutes, and the count of quick tasks - e.g. "Cleared 2 small tasks - Finished a focus session." Keep it factual, never congratulatory.
- Store the pre-filled text as the user's answer only if they leave it or edit it; if they clear it, store empty. Do not invent an answer the user did not endorse.
- Question 3's answer becomes the next day's carry-over banner. It is the single most load-bearing string in the loop; make sure it round-trips exactly.
- "Skip tonight" writes `reflection_entry` with `skipped = 1` so History can show it honestly. It is not a silent no-op.
- Chips and free text coexist: selecting chips does not disable the field, and both are stored (`didNotGoWellChips` plus `didNotGoWell`).
- Each question is a heading for screen readers; step numbers are decorative; a completed step announces "Answered".
- The weekly look-back header adds no extra tap - the three questions follow immediately.

## Affected layers

`feature/reflection`, `data/repository`.

## Acceptance criteria

- [ ] Matches artboard `a11` including the night surface in light theme.
- [ ] Question 1 is pre-filled from the day's real activity and is editable.
- [ ] Clearing the pre-fill stores an empty answer, not the generated text.
- [ ] Chips toggle and coexist with free text; both are stored.
- [ ] Steps reveal progressively; completed steps collapse to their answer.
- [ ] "Next" saves the entry, completes the activity and navigates to Day complete.
- [ ] "Skip tonight" records `skipped = 1` and still navigates to Day complete.
- [ ] Question 3's answer appears as the next day's carry-over banner, verbatim.
- [ ] The weekly look-back variant renders the summary header on days 7, 14, 21.
- [ ] Drafts autosave; leaving and returning restores all three answers.
- [ ] Each question is a heading; the step numbers are decorative.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`ReflectionViewModelTest` - pre-fill generation from several day shapes (exercise only; exercise plus focus; nothing completed); editing and clearing the pre-fill; chip toggling; save; skip; the weekly-look-back flag; draft restore.

`ReflectionPrefillTest` - the generated sentence for: one exercise, one exercise plus one focus session, two quick tasks plus a focus session, and an empty day.

## UI test expectations

`ReflectionScreenTest` - the three steps reveal in order; chips toggle; "Skip tonight" navigates; the night surface is dark under a light theme; the weekly variant renders its header.

## Integration test expectations

- `ReflectionIntegrationTest` - complete a reflection, then generate the next day and assert the carry-over banner text matches question 3 exactly.

## Manual verification

1. Complete a day's exercise and a focus session, then open the reflection: question 1 names both.
2. Edit the pre-fill; save; check History shows the edited text.
3. Skip tonight; check History shows the day as skipped, not absent.
4. Next morning, the carry-over banner quotes question 3.
5. Confirm the screen is dark with the app in light theme.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
