# Notification UX

Behaviour and scheduling are in `docs/engine/06-workmanager-strategy.md`. This document covers what the user sees and why.

## 1. Principles

1. **At most one nudge per day part.** The IA board states it: "Never more than one nudge."
2. **A notification is an offer, not a demand.** No counts, no streaks, no "don't break the chain", no guilt.
3. **Never notify about something already done.** Every worker re-checks state before posting.
4. **Silence when the app is in use.** No notification is posted while Itera is in the foreground.
5. **No notification is required.** Denying the permission costs reminders and nothing else.

## 2. The four reminders

| When | Channel | Title | Body | Opens |
| --- | --- | --- | --- | --- |
| Morning time | `training` | Today's exercise is ready | {Technique} - about {n} min | Exercise intro |
| Morning time, review due | `training` | Today's exercise and one review | {Technique}, plus {topic} to recall | Exercise intro |
| Focus slot | `training` | Time for one focus block? | {n} minutes on one thing | Today |
| Evening time | `reflection` | Two minutes, three questions | Close the day while it's fresh | Reflection |
| Habit nudge time | `habits` | Right after {anchor} | {habit} | Today |
| Snooze expiry | `training` | Still up for it? | {Technique} - about {n} min | Exercise intro |

All are `DEFAULT` importance except habit nudges (`LOW`) and the ongoing focus notification (`LOW`).

## 3. Suppression rules

A reminder is **not** posted when:

| Reminder | Suppressed if |
| --- | --- |
| Morning | the program activity is `COMPLETED`, `IN_PROGRESS` or `SNOOZED` past this time |
| Focus | no focus activity exists, it is not `AVAILABLE`, a focus session already completed today, or a session is currently running |
| Evening | the reflection is `COMPLETED` or `SKIPPED`, or a focus session is running (deferred to the session's end, or dropped if that is past 23:00) |
| Habit nudge | the stack is archived, or the user already logged that habit today |
| Any | the app is in the foreground |
| Any | its preference toggle is off |

## 4. The ongoing focus notification

Separate from reminders. Present for the whole session on channel `focus_session`:

- non-dismissable, `LOW` importance, no sound, no badge;
- `setUsesChronometer(true)` + `setChronometerCountDown(true)` so the countdown is rendered by the system;
- content: "Focus session" / the task label;
- actions: **Pause** (or **Resume**) and **End**;
- tapping it returns to the session screen.

Ending a session from the notification completes the activity and clears the notification; it does not open the app.

## 5. Copy rules

- Never state an exact time in the body ("It's 8:30") - delivery is inexact (ADR-0010).
- Never count anything: no "Day 9", no "3 in a row", no "You've missed 2 days".
- Never use an exclamation mark.
- Use a question for optional things ("Time for one focus block?") and a statement for scheduled ones ("Today's exercise is ready").
- Body lines are under 60 characters so they do not truncate on a lock screen.

## 6. Grouping and channels

No group summaries. At most one Itera reminder exists at a time; posting a new one with the same notification id replaces the old one. Ids are fixed constants per reminder type, so a stale morning reminder can never coexist with an evening one.

The four settings toggles map 1:1 to user intent, not to channels (focus and morning share the `training` channel). Toggling in the app updates the preference; the channel remains enabled so the other reminder type still works. The Settings screen's caption explains that system channel settings win.

## 7. Permission

Requested once, at the end of the Rhythm onboarding step, after an inline rationale card. Never re-requested. If denied:

- the four toggles show off and disabled with "Notifications are off for Itera";
- a row opens the system notification settings for the app;
- the app polls `areNotificationsEnabled()` on `ON_RESUME` so returning from system settings updates the toggles immediately.

## 8. Testing

- A test per suppression rule (section 3), asserting no notification is posted.
- A test that completing an activity cancels its pending reminder.
- A manual QA step per reminder, listed in `docs/testing/02-manual-qa-checklist.md`.
