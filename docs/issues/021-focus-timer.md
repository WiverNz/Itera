# 021 - Focus timer

**Phase** 4 - Specialised exercises | **Depends on** 018 | **Blocks** 027

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Focus.kt`** - `FocusScreen`, `TextControl`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the focus timer, its foreground service and its persistence, per `docs/engine/05-timer-lifecycle.md` and `docs/ux/02-screen-specs-exercise.md` section 2.2.

## User value

A 25- or 50-minute block that survives everything, on a screen with nothing to look at.

## Scope

- The pre-timer sheet: a "Pick one task" field and a duration `SegmentedControl` (15 / 25 / 50) seeded from the technique's `focusMinutes`.
- The timer screen on `NightSurface`, matching `a10`: the 300 dp ring, the 80 sp readout, the status line, the task, the three controls, the break hint.
- `FocusTimerState` logic: wall-clock `endsAt`, pause/resume shifting `endsAt`, `+5 min`, the 1 Hz render tick.
- `FocusSessionService`: a `specialUse` foreground service with a chronometer notification and Pause/Resume and End actions.
- Restore on app start, implementing every branch of the restore table.
- Completion: natural and early, the sub-60-second rule, the `focus_session` row, the focus result variant.
- The focus result screen: the standard result layout plus "{n} minutes of {planned} planned" and the break hint.
- Suppression of Itera's own reminders while a session runs.
- `FLAG_KEEP_SCREEN_ON` while running.

## Non-goals

- Silencing other apps' notifications - not possible, and the header copy says so honestly.
- Break timing as a separate timer - the break is a hint, not a countdown, in the MVP.

## Implementation notes

- **Never store a decrementing counter.** Remaining is always `endsAt - now`. A starved tick must self-correct on the next frame.
- Foreground service type is `specialUse` with the `focus_timer` subtype property. `shortService` caps at three minutes and is wrong; `mediaPlayback` is a misuse that risks a policy rejection.
- Use `setUsesChronometer(true)` + `setChronometerCountDown(true)` so the system renders the countdown. Do not post a notification update every second.
- The header reads "Itera notifications paused", not "Notifications silenced" - the app cannot silence other apps and must not imply it does.
- A session under 60 seconds writes nothing and returns to Today. This prevents a mis-tap from polluting focus minutes.
- The readout's live region announces at 5:00, 1:00 and 0:00 only. Announcing every second makes TalkBack unusable.
- If the notification permission is denied, the timer still runs; show a one-time inline note that it may be interrupted in the background.
- Clock changes: a backward jump over 60 s leaves the timer running against the original `endsAt`; a forward jump past it completes with clamped `actualSeconds`. Both are logged.
- Test process death early and on the oldest supported device - this is the most device-dependent code in the app.

## Affected layers

`feature/focus`, `data/focus`, `core/notifications`, manifest (two permissions and a service).

## Acceptance criteria

- [ ] The pre-timer sheet collects a task and a duration, seeded per technique.
- [ ] The timer screen matches `a10`, dark in both themes.
- [ ] The ring sweeps correctly, including after `+5 min`.
- [ ] Pause stops the countdown; resume shifts `endsAt` by exactly the pause duration.
- [ ] `+5 min` is additive and repeatable.
- [ ] Ending early confirms first; confirming records the session.
- [ ] A session under 60 s records nothing.
- [ ] A natural finish records `completedNaturally = true` with the correct `actualSeconds`.
- [ ] A foreground service runs while the timer runs and stops on pause, end or completion.
- [ ] The notification shows a live countdown and its Pause/Resume and End actions work.
- [ ] Tapping the notification returns to the session.
- [ ] Backgrounding for 10 minutes and returning shows the correct remaining time.
- [ ] Force-stopping and reopening restores the session within 2 s of accuracy.
- [ ] A session whose time elapsed while the app was dead auto-completes and routes to the result.
- [ ] A stored state older than 24 h is discarded.
- [ ] Itera's own reminders are suppressed during a session; the evening reflection is deferred, not dropped.
- [ ] Denying notifications still runs the timer, with the inline note.
- [ ] A backward clock jump does not complete the timer; a forward jump past `endsAt` completes it with clamped seconds.
- [ ] The readout announces only at 5:00, 1:00 and 0:00.
- [ ] Rotation loses nothing.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`FocusTimerStateTest` (with `FakeClock`) - remaining at start, mid and zero; pause/resume arithmetic; repeated `+5 min`; every branch of the restore table; the 24-hour discard; the sub-60-second rule; both clock-jump directions.

`FocusViewModelTest` - the pre-timer sheet; start; each control; early-end confirmation; completion writes the session and the result.

## UI test expectations

`FocusScreenTest` - controls render and fire; the end confirmation appears; the ring reflects progress; the screen is dark under a light theme; the readout does not announce every second.

## Integration test expectations

`FocusTimerServiceTest` - the service starts and stops with the timer; `am kill` mid-session then relaunch restores within tolerance; the notification deep link lands on the session.

`NotificationSuppressionTest` additions - no reminder is posted while a session runs.

## Manual verification

1. Run a real 25-minute Pomodoro with the phone face down; check the notification countdown and the final result.
2. Pause and resume from the notification.
3. Force-stop mid-session, reopen: the time is right.
4. Reboot mid-session, reopen: the session completes sensibly.
5. Start a session, wait past the evening reminder time: it is deferred, then arrives after the session.
6. Run on the oldest supported device.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
