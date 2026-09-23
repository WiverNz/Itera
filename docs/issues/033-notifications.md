# 033 - Notifications and WorkManager

**Phase** 6 - Infrastructure | **Depends on** 011, 014, 017 | **Blocks** 034

## Goal

Implement all background scheduling and every notification, per `docs/engine/06-workmanager-strategy.md` and `docs/ux/08-notification-ux.md`.

## User value

The app shows up at the right times, once, and never nags about something already done.

## Scope

- Four notification channels: `training`, `reflection`, `habits`, `focus_session`.
- `IteraNotifier`: builds and posts every notification with the documented titles, bodies and deep links.
- Eight workers: `DailyPlanWorker`, `MorningReminderWorker`, `FocusSuggestionWorker`, `EveningReminderWorker`, `SnoozeReminderWorker`, `HabitNudgeWorker`, `EventLogTrimWorker` (reviews are folded into the morning reminder).
- `ReminderScheduler.rescheduleAll()` and the per-event scheduling calls, idempotent and keyed by the documented unique names.
- Self-rescheduling one-shot chains for wall-clock reminders; a periodic worker for the daily plan and the log trim.
- Receivers for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` and `ACTION_TIMEZONE_CHANGED`.
- Every suppression rule in `docs/ux/08-notification-ux.md` section 3.
- Deep-link intents carrying the `itera.deeplink` extra that issue 015 consumes, with a synthetic back stack ending at Today.
- Stale-target fallbacks (a completed activity opens its result read-only).

## Non-goals

- The Settings toggles UI (issue 034) - this issue provides the scheduler those toggles drive.
- Exact alarms - explicitly excluded (ADR-0010).
- Do Not Disturb access.

## Implementation notes

- **No exact alarms.** Do not declare `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`. Reminder copy already tolerates delay.
- A periodic worker cannot hit a wall-clock time. Use a chain of one-shots, each re-enqueuing itself for the next day as its final step, with `ExistingWorkPolicy.REPLACE` on a stable unique name.
- **Every worker re-checks state before posting.** The suppression table is the specification; implement it as a single `shouldPost()` per worker and test each rule independently.
- At most one reminder exists at a time: use fixed notification ids per type so a new post replaces the old.
- The evening reminder is **deferred** (not dropped) when a focus session is running, unless deferral would push it past 23:00.
- `rescheduleAll()` must be idempotent - it computes the desired set from preferences plus today's plan and enqueues each unique name with `REPLACE`. It is called from seven triggers; any of them may fire twice.
- Boot and package-replace receivers exist because the one-shot chain needs a kick if the device was off across a scheduled time.
- `POST_NOTIFICATIONS` is requested only in onboarding (issue 016) and before the first focus session (issue 021). This issue must behave correctly when it is denied: schedule nothing, and let the Settings toggles reflect that.
- Notification bodies are under 60 characters and never state an exact time or a count.

## Affected layers

`core/notifications`, manifest (receivers, `RECEIVE_BOOT_COMPLETED`).

## Acceptance criteria

- [ ] All four channels are created with the documented importance.
- [ ] Each of the six notification types posts with the documented title and body.
- [ ] Every suppression rule prevents a post.
- [ ] No notification is posted while the app is in the foreground.
- [ ] At most one reminder is present at a time.
- [ ] The morning reminder mentions a due review when one exists.
- [ ] The evening reminder is deferred during a focus session and dropped if deferral passes 23:00.
- [ ] Each notification deep-links to the documented destination and builds a back stack ending at Today.
- [ ] A deep link to a completed activity opens its result read-only.
- [ ] `DailyPlanWorker` run twice for one date creates one `training_day` row.
- [ ] `rescheduleAll()` produces the same unique-work set when run twice.
- [ ] All seven reschedule triggers fire it.
- [ ] Boot and package-replace receivers re-establish the chain.
- [ ] A timezone change reschedules to the new local times.
- [ ] Completing an activity cancels its pending reminders.
- [ ] With the permission denied, nothing is scheduled and nothing crashes.
- [ ] The manifest declares no exact-alarm permission.
- [ ] `EventLogTrimWorker` trims to 2 000 rows under its constraints.

## Unit test expectations

`ReminderSchedulerTest` (with a fake WorkManager facade) - the exact unique-work set and delays for: all toggles on; all off; each toggle individually; a day with no focus activity; a snoozed activity; an active habit stack. Plus idempotence across two calls.

`NotificationContentTest` (Robolectric) - each notification's title, body, channel and deep-link extra; body length under 60 characters; no exact time in any body.

## Integration test expectations

`WorkerIdempotencyTest` - `DailyPlanWorker` twice, one row.

`NotificationSuppressionTest` - one test per suppression rule, each asserting nothing is posted.

`DeepLinkTest` - each notification type opens its destination with a back stack ending at Today.

`RescheduleTest` - a preference change reschedules; completion cancels.

## Manual verification

1. Set the morning time two minutes out; background the app; the reminder arrives (note the delay on a Doze-aggressive device).
2. Complete the exercise first; no reminder arrives.
3. With the app in the foreground at the reminder time: nothing.
4. Start a focus session spanning the evening time; the reflection reminder arrives after the session.
5. Reboot the device; reminders still fire the next day.
6. Change timezone; reminders follow local time.
7. Deny notifications; walk the app; nothing breaks.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
