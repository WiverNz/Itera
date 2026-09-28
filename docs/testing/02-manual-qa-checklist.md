# Manual QA checklist

Run before each internal release, on **two devices**: one recent (API 34+) and one at or near `minSdk 26`. Record device, OS, build number and result.

## Voice input (milestone 012; repeat at release)

- [ ] On API 31+ with installed on-device models, dictate/edit each supported field and run every command in EN/RU/DE/ES, including the Russian examples and 25-minute focus. Repeat offline.
- [ ] Check default/specified durations, missing task, already-active focus, pause/resume and confirmed end above/below 60 seconds; touch/notification controls retain the same state.
- [ ] Confirm duplicate/partial item matches, cancel confirmation, try invalid exercise completion/unsupported commands; no unintended writes or global item search.
- [ ] Deny/revoke microphone permission; test missing service/model and API 26-30. Manual input remains complete; no unconsented provider or repeated prompting.
- [ ] Without an on-device recognizer: with a system default, the system consent appears first; without one, the picker lists installed recognition apps with label/icon and nothing pre-selected; the named consent stores only on "Use <app>". Uninstall/disable the chosen app, or pick one that refuses third-party clients: the choice is cleared and the picker explains why. Settings → Voice revokes/changes both.
- [ ] Cancel, navigate, background, kill and switch locale while listening/confirming. No late insertion, replay or resumed listening; saved text survives and the next tap uses the new language.
- [ ] Stop/final results without partial support, selection replacement, text limits and service failure preserve editing; command-looking dictation only inserts text.
- [ ] Run voice states with TalkBack, Switch Access, reduced motion and 2.0 font scale in light/dark. Compare labels, Cancel access and four-language copy against `design/`.
- [ ] Inspect privacy copy and synthetic-data logs: no stored audio, transcript logging or background capture. Record device/service/model availability separately from parser coverage.

Automated coverage is in `01-test-matrix.md`; this list covers what automation cannot judge.

## 1. Setup

- [ ] Install a fresh build over a wiped app (uninstall first, do not just clear data).
- [ ] Device language English, system theme light.
- [ ] Notifications allowed at the OS level for the first pass.

## 2. First run

- [ ] Splash does not flash a blank white or black frame.
- [ ] Welcome copy is correct and there is **no** "I already have an account" link.
- [ ] Goals: selecting a third option drops the oldest, visibly.
- [ ] Goals: "Continue" works with nothing selected.
- [ ] Rhythm: both time pickers open, set and display correctly in 12h and 24h device formats.
- [ ] Rhythm: notification permission prompt appears once; denying it continues the flow.
- [ ] First week: the seven days match the curriculum, Day 1 is highlighted.
- [ ] "Start Day 1" lands on Today within a second, with no intermediate blank state.

## 3. The daily loop, by hand

- [ ] Today: the hero reads correctly, the badge says "New", the checklist reads 0 / 3.
- [ ] Exercise intro: copy is legible, the example line is present, "Not now" shows a sensible time.
- [ ] Do the 2-minute rule for real: enter two tasks, watch the stopwatches run, tick them.
- [ ] Finish: the result headline and body appear, the disc animates, the mastery bar grows to Met.
- [ ] Add a rating and a note. Back out, return: rating and note are still there.
- [ ] Today: the row is filled, the counter is 1 / 3, and the note is echoed in the subtitle.
- [ ] Start a focus session: pick a task, pick 25 min, start.
- [ ] Put the phone face down for two minutes; the notification shows a live countdown.
- [ ] Pause and resume from the notification.
- [ ] End early: the confirmation appears; ending records the session.
- [ ] Evening reflection: question 1 is pre-filled from what you did today, and is editable.
- [ ] Chips toggle; typing works alongside them.
- [ ] Day complete: the change is echoed and tomorrow's technique is named.
- [ ] Return to Today: the quiet completion card is shown and no new work is offered.

## 4. Across days

Use the device clock to move forward a day at a time (Settings -> Date & time).

- [ ] Day 2: the carry-over banner shows last night's change.
- [ ] Day 2: the hero is Pomodoro and the secondary row is "Keep using: 2-minute rule".
- [ ] Tap the practice prompt: it completes in place with an undo snackbar; undo works.
- [ ] Skip three days forward without opening the app: on return, the program day has **not** advanced and the same curriculum day is offered.
- [ ] Progress shows hollow dots for the skipped days and no warning colour anywhere.
- [ ] Reach Day 6, do the Feynman exercise, then advance one day: the review appears on Today and on Train.
- [ ] Review: the previous answer is genuinely hidden until "Compare".
- [ ] Grade "I had it": the next review is 4 days out.
- [ ] Reach Day 14: the combination day runs as a chain and the techniques show Integrated afterwards.

## 5. Interruption and recovery

- [ ] Force-stop mid-exercise with text typed; reopen: the same screen and the same text.
- [ ] Force-stop mid-focus-session; reopen: the timer is still running with the right time.
- [ ] Reboot mid-focus-session; reopen: the session completes sensibly.
- [ ] Rotate on every screen: nothing is lost, nothing is duplicated.
- [ ] Receive a phone call during a focus session: the timer keeps time.
- [ ] Airplane mode for a full session: nothing changes.
- [ ] Change the timezone forward 6 hours mid-session: the timer does not jump.
- [ ] Leave the app open across midnight: the day rolls over on the next interaction, not mid-view.

## 6. Notifications

- [ ] Set the morning time two minutes ahead, background the app, wait: the reminder arrives (allow up to 45 min on a Doze-aggressive device; note the delay).
- [ ] Tapping it opens the exercise intro; system back lands on Today, not outside the app.
- [ ] Complete the exercise, then wait past the reminder time: no reminder arrives.
- [ ] With the app open at the reminder time: no notification appears.
- [ ] Turn off "Morning exercise" in Settings: no reminder the next day; the evening one still arrives.
- [ ] At most one Itera reminder is ever in the shade at once.

## 7. Themes and display

- [ ] Switch System / Light / Dark: every screen updates immediately, with no restart.
- [ ] In light theme, the focus timer, reflection and day-complete screens are still dark.
- [ ] Set the system to dark with the app open: it follows.
- [ ] Font size at maximum: walk every screen; nothing is clipped, everything scrolls.
- [ ] Display size at maximum: same.
- [ ] Turn animations off in Developer options: walk the daily loop; nothing is stuck mid-animation.
- [ ] Landscape on every screen: usable, nothing overlapping (not designed-for, but must not break).

## 8. Accessibility

- [ ] Run the TalkBack script in `docs/ux/07-accessibility.md` section 8, end to end.
- [ ] Listen to the Today hero card: it reads as one sensible sentence, then the button.
- [ ] Listen to a checklist row: title, subtitle and state, in that order.
- [ ] The focus timer does not announce every second.
- [ ] The Eisenhower quadrants announce what will happen before you tap.
- [ ] Switch Access can reach and activate every control on Today and the exercise runner.

## 9. Data

- [ ] Export journal (Last 30 days), share to a notes app: the file opens and the notes are readable.
- [ ] Export with special characters in a note (`|`, `#`, a line break): the Markdown is not broken.
- [ ] "Reset program": the dialog names what is lost; confirming returns to Day 1 with settings and topics intact.
- [ ] "Erase everything": the app returns to Welcome.
- [ ] After a reset, the library shows only Day 1 unlocked.

## 10. Visual comparison against `design/`

Install the prototype alongside the app - `com.itera.app` and `com.wivernz.itera` coexist - and put the two devices, or two windows, side by side.

- [ ] Walk all 24 screens in **light** theme, comparing structure, spacing, sizes, radii, type and colour.
- [ ] Repeat in **dark** theme.
- [ ] Confirm the focus timer, evening reflection and day complete are dark in **both** app themes.
- [ ] Compare the bottom bar: height, item spacing, icon size, selected weight and colour.
- [ ] Compare the animations: step-dot pulse and check, mastery ladder fill, the result check draw, the day ring, the welcome path, the reflection step transitions.
- [ ] Confirm Library, History and Technique detail are full-screen with a back button and **no** bottom bar.
- [ ] Note every deviation, with its reason. An unexplained deviation is a defect.

## 11. Languages

- [ ] Switch to each of English, Russian, German and Spanish from You > Language & region.
- [ ] Each switch applies immediately, with no restart and no flash of the old language.
- [ ] Walk every screen in **German** (longest) and **Russian** (Cyrillic): nothing clipped, nothing overlapping, no mixed-language text.
- [ ] Confirm technique names, descriptions and exercise instructions are translated, not just the chrome.
- [ ] Check plural forms in Russian at counts 1, 2, 5, 11, 21 (one / few / many).
- [ ] Check dates: the History month header, the "training since" line, and weekday initials, in each language.
- [ ] Check the first day of week follows the locale.
- [ ] Set the language, force-stop the app, reopen: the language is retained.
- [ ] Android 13+: confirm Itera appears in Settings > Apps > Itera > Language, and that changing it there changes the app.
- [ ] Write a note in English, switch to Russian: **the note is unchanged**.
- [ ] Check a notification body in German on the lock screen - not truncated.

## 12. Content review

- [ ] Read every technique's intro, instruction and result copy aloud. Flag anything that praises the user, mentions streaks or counts, or cannot be acted on today.
- [ ] Re-read any string that changed since the last pass, in all four languages.
- [ ] Confirm the word "Itera" appears wherever a product name is shown, and "Productivity Trainer" appears nowhere.

## 13. Exploratory

- [ ] 30 minutes of unscripted use across all screens and states. No crash, no ANR, no visual break. Note anything surprising even if it is not a bug.

## 14. Sign-off

| Field | Value |
| --- | --- |
| Build | |
| Devices | |
| Languages checked | |
| Date | |
| Failures found | |
| Blocking issues | |
| Signed off by | |
