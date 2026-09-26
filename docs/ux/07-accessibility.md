# Accessibility

Target: usable end to end with TalkBack, at `fontScale 2.0`, with reduced motion, and with a switch device. Verified per issue and swept in issue 037.

## 1. Non-negotiables

> **The prototype already sets many of these** - `Role.Checkbox` on chips and option rows, `Role.RadioButton` on selections, `Role.Tab` on segmented controls and tab items, `onClickLabel` on icon buttons and quadrants, `liveRegion = Polite` on the timer, `contentDescription` on calendar cells and the language pill. Port them, then add what section 3 lists beyond that.

| Rule | Value |
| --- | --- |
| Minimum touch target | 44 x 44 dp (`Modifier.minimumInteractiveComponentSize()` where the visual is smaller) |
| Text contrast | AA (4.5:1 body, 3:1 for >= 18 sp or >= 14 sp bold) |
| Non-text contrast | 3:1 for every control boundary, ring, and focus indicator |
| Font scaling | every screen legible and scrollable at 2.0; no `sp`-to-`dp` substitution for text |
| Colour alone | never the only carrier of meaning |
| Motion | fully removable (`03-ux-states.md` section 10) |

## 2. Colour is never alone

| Meaning | Colour cue | Redundant cue |
| --- | --- | --- |
| Activity completed | filled skill disc | a check glyph inside it, and "Done" in the accessible description |
| Activity skipped | grey disc | a dash glyph, and "Skipped" |
| "New" badge | accent fill | the word "New", read as "new technique" |
| Locked technique | dimmed | a lock glyph and a "Day {n}" chip |
| Skill identity | skill colour | the skill name in the eyebrow or chip |
| Trained day on the calendar | coloured dot | the cell's `contentDescription` states the activities |
| Selected chip | filled + bordered | `Role.Checkbox` / `Role.RadioButton` state |
| Mastery level | bar fill | the level word ("Applied") beside it |

## 3. Semantics per screen

| Screen | Key semantics |
| --- | --- |
| Today | Greeting is a heading. Hero card merges into one node ending with its button as a child. Counter reads "0 of 3 done". Each checklist row merges to "{title}, {subtitle}, {state}" |
| Exercise intro | Title is a heading. The "Your exercise" card merges; the instruction is read in full |
| Template checklist | Each row is `toggleable` with `Role.Checkbox`. The stopwatch value is in the description ("1 minute 12 seconds elapsed") and is **not** a live region |
| Focus timer | The readout has `role = Role.Timer` with `liveRegion = Polite`, announced only at 5:00, 1:00 and 0:00. The ring is decorative. The play/pause button's `contentDescription` switches between "Pause session" and "Resume session" |
| Eisenhower | Tasks are `selectable` with `Role.RadioButton`. Quadrants are buttons described as "Do now, urgent and important, {n} tasks. Double tap to place {selected task}." When nothing is selected they are disabled with `stateDescription = "Pick a task first"` |
| Feynman | The word count is a polite live region announced every 25 words, not every keystroke |
| Review | The redacted block is `invisibleToUser` until revealed; its placeholder reads "Your previous answer is hidden until you finish" |
| Reflection | Each of the three questions is a heading; step numbers are decorative; completed steps announce "Answered" |
| Mastery ladder | One merged node: "Level Applied, 3 of 4. Next: use it inside a combination day." Individual bars are decorative |
| Review ladder | One merged node: "Review stage 2 of 5. Next review in 9 days." |
| Progress skill row | One merged node: "Focus, Steady. 11 practices on 7 of 9 days." |
| History calendar | Each cell: "{weekday} {date}, {n} activities: Focus, Habits" or "{weekday} {date}, no training" |
| Bottom bar | Standard `NavigationBarItem` semantics with `selected`; no custom description |
| Settings | Switch rows are `toggleable` at row level so the whole row is one target |

## 4. Reading order

`Modifier.semantics { isTraversalGroup = true }` on the header, hero and checklist regions so traversal follows visual order. Where a chip sits to the right of a title but should be read after it (the day chip, the skill chip), `traversalIndex` is set explicitly.

## 5. Focus management

- Opening a full-screen route moves focus to its title.
- Closing returns focus to the element that opened it.
- After completing an exercise, focus lands on the result headline.
- Dialogs trap focus; the destructive action is never the first focusable element.
- Nothing steals focus while the user is typing.

## 6. Text fields

- Every field has a visible or `invisibleToUser`-hidden `<label>` equivalent; placeholder text is never the only label (the artboards already use visually-hidden labels - preserve that).
- `KeyboardOptions` set `capitalization = Sentences` for prose and `imeAction = Next`/`Done` appropriately.
- Character-limit counters are polite live regions announced only when within 500 of the cap.

## 7. Font scaling specifics

Tested at 1.0, 1.3, 1.5, 2.0:

- the Today hero title steps down one type level above 1.5;
- `SegmentedControl` stacks vertically above 1.6;
- the 2x2 Eisenhower grid keeps its shape and scrolls internally;
- the timer readout is capped so the ring never clips;
- no `maxLines = 1` on any title that carries meaning; metadata lines may truncate.

## 8. TalkBack test script (issue 037 and manual QA)

1. Complete onboarding using TalkBack only.
2. From Today, start and complete the day's exercise, including the rating and a note.
3. Start a focus session, pause, resume, end early.
4. Complete the evening reflection and reach Day complete.
5. Navigate all four tabs and open Library, a locked technique, Technique detail, and History.
6. Change the morning time and toggle a notification.
7. Run "Reset program" and cancel the dialog.

Every step must be completable without sighted assistance and without encountering an unlabelled control.

## 9. Localisation

Four languages ship in the MVP. Full specification in `docs/i18n/00-localization.md`. The accessibility-relevant parts:

- No layout may assume English length. German is the stress case and is a **shipping language**, not a pseudo-locale check.
- Every user-facing string is externalised; `HardcodedText` and `MissingTranslation` lint are errors.
- Plurals use `plurals.xml` with the categories each language requires.
- No concatenation; positional arguments only.
- Dates, times, month names and the first day of the week come from the active locale.
- `start`/`end` padding only. `supportsRtl="true"` is set and `en-XB` is run as a safety net, though no shipping language is RTL.
- The language picker names each language **in its own language**, so someone who has switched to a script they cannot read can still switch back.
- Screen-reader announcements are localised along with everything else; the `contentDescription`s specified in section 3 are string resources, not literals.

## 10. Voice input (milestone 012)

Voice is an optional input method, never the only way to act. All dictation/commands retain keyboard, touch, TalkBack and Switch Access equivalents. Mic targets are at least 44 dp and labelled “Dictate {field}” or “Voice command”; expose listening state and a reachable Stop/Cancel. Tap-to-start avoids a press-and-hold requirement.

Announce listening, final result, failure and confirmation politely; do not announce every partial token or depend on colour, animation, sound or vibration. Keep interim text readable without stealing input focus. Confirmation traps focus, names the target and returns focus to its opener on cancel; successful dictation restores field focus/caret. Do not start recognition automatically after a screen-reader announcement. Validate that TalkBack output cannot silently confirm a command.

Extend the TalkBack/Switch Access script: dictate/edit an answer, cancel listening, deny permission, inspect unavailable-language state, select among duplicate checklist labels, cancel/confirm an end-session action. Run light/dark and font scale 2.0 with EN/RU/DE/ES labels; reduced motion uses static listening text. See [voice UX](10-voice-input.md).
