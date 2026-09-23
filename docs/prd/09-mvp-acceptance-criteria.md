# MVP acceptance criteria

The MVP is complete when every criterion below passes on a physical device running the release build. Each maps to issues and to the test matrix.

Twelve groups: A first run, B daily loop, C reliability, D program mechanics, E progress honesty, F surfaces, G settings, H notifications, L localisation, P prototype parity, I accessibility, J quality gates.

## A. First run

- [ ] **A1** A clean install opens on Welcome within 2 s.
- [ ] **A2** Onboarding can be completed in under 60 s, including choosing times.
- [ ] **A3** Onboarding can be completed with zero goal selections and still produces a valid Day 1.
- [ ] **A4** Denying the notification permission does not block onboarding and does not break any screen.
- [ ] **A5** After "Start Day 1", Today shows the 2-minute rule hero with a "New" badge and a 0 / 3 checklist.
- [ ] **A6** Killing the app mid-onboarding and relaunching returns to Welcome with selections preserved.

## B. The daily loop

- [ ] **B1** From Today, the day's exercise can be opened, performed and completed with a rating and a note in under 5 minutes.
- [ ] **B2** On completion the checklist row fills, the counter advances, and the mastery ladder shows `Met`.
- [ ] **B3** "Not now - remind me at 12:00" snoozes the activity and the row reads "Snoozed until 12:00".
- [ ] **B4** A focus session can be started, paused, resumed, extended by 5 minutes and ended.
- [ ] **B5** The evening reflection can be completed in under 2 minutes, with question 1 pre-filled from the day's activity.
- [ ] **B6** "Skip tonight" records a skipped reflection and still reaches Day complete.
- [ ] **B7** Day complete shows the day number, the user's change for tomorrow, and tomorrow's technique.
- [ ] **B8** The next day's Today shows the carry-over banner with last night's change.

## C. Reliability

- [ ] **C1** Force-stopping the app mid-exercise and reopening restores the route and the typed draft.
- [ ] **C2** A focus session survives force-stop and resumes with the correct remaining time (within 2 s).
- [ ] **C3** A focus session survives a device reboot mid-session by completing correctly on next open.
- [ ] **C4** Rotating the device on every screen loses nothing.
- [ ] **C5** Airplane mode for an entire session changes nothing anywhere.
- [ ] **C6** Changing the device timezone does not shift a running timer or corrupt the day.
- [ ] **C7** Crossing midnight with the app open rolls the day over correctly on next interaction.

## D. Program mechanics

- [ ] **D1** Completing a day advances `programDay` exactly once.
- [ ] **D2** Skipping three calendar days does not advance the program and shows no warning or penalty.
- [ ] **D3** After a 5-day gap, the missed curriculum day is offered again.
- [ ] **D4** A technique unlocks on its curriculum day and never re-locks.
- [ ] **D5** Locked techniques are visible and openable in the library, showing "Day {n}".
- [ ] **D6** A Feynman exercise on Day 6 produces a review due the next day.
- [ ] **D7** A review hides the previous answer until "Compare with my first answer" is tapped.
- [ ] **D8** Grading a review moves the ladder by the documented rule (1 -> 4 -> 9 -> 21 -> 60).
- [ ] **D9** An overdue review appears exactly once, not once per day missed.
- [ ] **D10** Day 14 presents a combination day, and completing it sets the used techniques to `Integrated`.

## E. Progress honesty

- [ ] **E1** Opening the app 20 times changes nothing on Progress.
- [ ] **E2** No screen anywhere shows a streak, a points total, or a program-completion percentage. (Automated string assertion.)
- [ ] **E3** Untrained days render as hollow dots beside the standing reassurance line, with no warning colour.
- [ ] **E4** Mastery levels match the documented thresholds for a seeded history fixture.
- [ ] **E5** History shows rest days explicitly.

## F. Surfaces

- [ ] **F1** All four tabs are reachable, keep independent back stacks, and restore scroll position.
- [ ] **F2** Re-tapping the active tab pops that tab to its root.
- [ ] **F3** Library lists all 14 techniques, filters by skill, and sorts unlocked before locked.
- [ ] **F4** Technique detail shows the mastery ladder, the next-level hint, recent practice with notes, and related techniques.
- [ ] **F5** "Practice now" creates a manual activity and opens it directly.
- [ ] **F6** History navigates months, bounded by the program start and the current month.
- [ ] **F7** Journal export produces a valid Markdown file that opens in a text editor and contains the user's notes.

## G. Settings

- [ ] **G1** Changing the morning time reschedules the reminder and is reflected immediately.
- [ ] **G2** Theme System / Light / Dark applies immediately across every screen.
- [ ] **G3** The three night-surface screens stay dark in light theme.
- [ ] **G4** Each notification toggle suppresses exactly its own reminder.
- [ ] **G5** "Reset program" deletes history and unlocks, keeps settings and topics, and returns to Day 1.
- [ ] **G6** "Erase everything" returns the app to first run.
- [ ] **G7** The AI coach row is visible, disabled and captioned "Coming later".

## H. Notifications

- [ ] **H1** The morning reminder fires and deep-links to the exercise intro.
- [ ] **H2** No reminder is posted for an already-completed activity.
- [ ] **H3** No reminder is posted while the app is in the foreground.
- [ ] **H4** At most one reminder exists at a time.
- [ ] **H5** The focus-session notification shows a live countdown and its Pause/End actions work.
- [ ] **H6** System back from a notification lands on Today, not outside the app.

## L. Localisation

- [ ] **L1** The app is fully usable in English, Russian, German and Spanish.
- [ ] **L2** Switching language from You applies immediately, with no restart.
- [ ] **L3** Switching from the Welcome pill works before onboarding is complete.
- [ ] **L4** The choice survives a force-stop and a reboot, on Android 13+ and on an API 26-30 device.
- [ ] **L5** Itera appears in the Android 13+ system per-app language settings and follows changes made there.
- [ ] **L6** Technique names, descriptions, "why" and "task" are translated in all four languages.
- [ ] **L7** Russian plurals are correct at 1, 2, 5, 11 and 21.
- [ ] **L8** Dates, times, month names and first-day-of-week are locale-correct.
- [ ] **L9** No screen clips or overlaps text in any of the four languages.
- [ ] **L10** A note written in one language is unchanged after switching to another.
- [ ] **L11** Past history re-renders in the new language with no leftover text from the old one.
- [ ] **L12** No missing glyph appears anywhere in Russian; the bundled Cyrillic faces are used, not a system substitute (ADR-0019).
- [ ] **L13** Times honour the Android system 12/24-hour preference in all four languages, and the Settings time-format row is read-only.
- [ ] **L14** Morning and evening times are set with a real Material 3 time picker.

## P. Prototype parity

- [ ] **P1** Every one of the 24 screens has passed a recorded side-by-side comparison against `design/`, in light and dark.
- [ ] **P2** Every deviation from the prototype is documented with its reason, and is either a named product rule or a change also made in `design/`.
- [ ] **P3** The bottom bar appears only on the four tab routes; Library, History and Technique detail are full-screen with a back button.
- [ ] **P4** The focus timer, evening reflection and day complete are dark in both app themes.
- [ ] **P5** Animations match the prototype's specs - step dot, mastery ladder, result check, day ring, welcome path, reflection transitions.
- [ ] **P6** `verifyRoborazziDebug` passes against the committed golden set.
- [ ] **P7** The AI coach container renders in its "Coming later" state, and **no fabricated or generated coaching feedback is shown anywhere**.

## I. Accessibility

- [ ] **I1** The TalkBack script in `docs/ux/07-accessibility.md` section 8 completes without an unlabelled control.
- [ ] **I2** Every screen is usable and scrollable at `fontScale 2.0` with no clipped text.
- [ ] **I3** With animations disabled, every screen renders at its end state and nothing is stuck.
- [ ] **I4** No meaning is carried by colour alone.
- [ ] **I5** Every interactive target is at least 44 x 44 dp.
- [ ] **I6** The pseudo-locale `en-XA` shows no clipped label; `en-XB` shows no mirrored-layout break.
- [ ] **I7** Every screen is usable at `fontScale 2.0` in German as well as English.

## J. Quality gates

- [ ] **J1** `./gradlew build` passes from a clean checkout with no warnings treated as suppressed.
- [ ] **J2** Unit test coverage of `domain` is at least 85 % of lines.
- [ ] **J3** Every documented state-machine transition has a test.
- [ ] **J4** `CatalogValidationTest` and `ArchitectureTest` pass.
- [ ] **J5** No `TODO`, `FIXME` or commented-out code in `main`.
- [ ] **J6** `MissingTranslation` lint passes as an error with no suppressions.
- [ ] **J7** Release APK is under 12 MB, including four string catalogues and the bundled fonts.
- [ ] **J8** Cold start to Today's first frame is under 1.5 s on a mid-range device; warm start under 500 ms.
- [ ] **J9** No crash in a 30-minute exploratory session across all screens, in at least two languages.
