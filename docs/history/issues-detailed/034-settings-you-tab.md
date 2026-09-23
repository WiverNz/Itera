# 034 - Settings / You tab

**Phase** 6 - Infrastructure | **Depends on** 007, 033 | **Blocks** 035, 037

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Profile.kt`** - `ProfileScreen`, `Group`, `ValueRow`, `SwitchRow`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the You tab, matching artboard `c06` and `docs/ux/02-screen-specs-train-progress-you.md` section 6, plus the topic editor, the privacy screen and both reset flows.

## User value

Fit the coach to your life - times, pace, theme, reminders - and leave cleanly if you want to.

## Scope

- Identity: an avatar initial, an optional local display name, and "Training since {date} - Day {n}".
- **Daily rhythm**: morning time, evening time, time-on-most-days - each opening a picker.
- **Program**: a pace block with a `Segmented` and the pace description; focus areas; learning topics ("{n} topics", opening the editor).
- **Language & region**: the language row opening `LanguageSheet` - **this is the app's primary runtime language switch and is built here, not deferred** (D-15) - and the **read-only** time-format row showing the format resolved from `DateFormat.is24HourFormat`, with a caption naming Android settings. No chevron, not clickable, no in-app override (D-16).
- **Appearance**: theme `Segmented` (System / Light / Dark).
- **Notifications**: four switches, plus the denied-permission state with a row opening system settings.
- **Coach**: the AI feedback row, visible, **disabled**, captioned "Coming later".
- **Data**: Export journal (issue 035 supplies the sheet), Privacy, Reset program, Erase everything.
- The learning-topic editor: add, rename, archive.
- "Load demo data" as a `Secondary` button at the bottom, **debug builds only** (D-11).
- The privacy screen: static text stating what is stored, that nothing is transmitted, and what the export contains.
- Both reset dialogs with the exact confirmation copy from `docs/ux/09-copy-deck.md`, and a blocking progress dialog while running.
- `areNotificationsEnabled()` polled on `ON_RESUME` so returning from system settings updates the toggles.

## Non-goals

- The export sheet itself (issue 035).
- Any cloud, account or sync setting.
- Enabling the AI coach.

## Implementation notes

- Every change writes immediately; there is no save button.
- Changing a time or a toggle calls `ReminderScheduler.rescheduleAll()`.
- The AI row is **disabled**, not hidden, and captioned "Coming later" (R-11). It must not be toggleable.
- The display name is optional, local, and appears only here and in the avatar initial. Empty shows "Add your name (optional)" and the app glyph.
- Reset dialogs must name exactly what is lost, in the documented copy, and say it cannot be undone. The destructive action is never the first focusable element in the dialog.
- Both resets run in one transaction, cancel all scheduled work, then reschedule - delegate to issue 014's use cases rather than writing deletion logic here.
- Changing pace or focus areas takes effect from the **next** generated day; today's plan is not regenerated. Do not show a message implying otherwise.
- Switch rows are `toggleable` at row level so the whole row is one target.
- The notification section's disabled state must be visually clear and must not look like the toggles are simply off by choice.

## Affected layers

`feature/you`, `data/preferences`, `core/notifications`.

## Acceptance criteria

- [ ] Sections appear in the prototype's order: identity, Daily rhythm, Program, Language & region, Appearance, Notifications, Coach, Data.
- [ ] The Language row shows the current language in its own language and opens the picker.
- [ ] Choosing a language re-renders the whole app immediately, with no restart.
- [ ] The choice survives a force-stop, on Android 13+ and on an API 26-30 device.
- [ ] The Time format row is read-only, shows the system-resolved format, and is not clickable.
- [ ] Both time rows open a real Material 3 time picker.
- [ ] "Load demo data" is absent from a release build.
- [ ] Every setting reads its current value and writes immediately.
- [ ] Both time pickers work in 12- and 24-hour device formats.
- [ ] The pace control shows the selected pace's description.
- [ ] Focus areas reuse the onboarding rows and keep the max-2 rule.
- [ ] The theme control applies immediately across the app, with no restart.
- [ ] Each notification toggle suppresses exactly its own reminder.
- [ ] Changing a time or toggle reschedules reminders.
- [ ] With the OS permission denied, all four toggles are off and disabled, with a row opening system settings.
- [ ] Returning from system settings updates the toggles on resume.
- [ ] The AI row is visible, disabled and captioned "Coming later".
- [ ] The display name is optional and shows the documented empty state.
- [ ] The topic editor adds, renames and archives topics, and shows an empty state with an "Add a topic" action.
- [ ] The privacy screen states what is stored and that nothing is transmitted.
- [ ] Both reset dialogs use the documented copy and name what is kept and lost.
- [ ] "Reset program" returns to Day 1 with settings and topics intact.
- [ ] "Erase everything" returns the app to Welcome.
- [ ] Both resets show a blocking progress dialog and a confirmation afterwards.
- [ ] Changing pace does not regenerate today's plan.
- [ ] Works at `fontScale 2.0`; the segmented controls stack.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`YouViewModelTest` - each setting's read and write; permission-denied state; resume re-check; topic add/rename/archive; both resets delegating to the right use case; pace change not regenerating today.

## UI test expectations

`YouScreenTest` - every section renders; each toggle writes; the AI row is disabled and not toggleable; both reset dialogs render with the documented copy and their destructive action is not first in focus order; the denied-permission state renders.

## Integration test expectations

`ResetIntegrationTest` (from issue 014, extended through the UI) - both tiers driven from the screen, asserting the resulting database and preference state, and that scheduled work was cancelled and re-established.

## Manual verification

1. Change every setting; force-stop; reopen; all persist.
2. Change the morning time; confirm the next day's reminder moves.
3. Deny notifications at the OS level; open Settings; toggles are off and disabled; tap the row; system settings open; return; state updates.
4. Try to toggle the AI row; it does not move.
5. Run both resets on a populated database and inspect the result.
6. Navigate the whole tab with TalkBack, including both dialogs.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
