# Background work and scheduling

Guardrails: background jobs must be **idempotent**, and the app must not assume exact alarm timing.

## 1. No exact alarms

The MVP uses **no** `AlarmManager` exact alarms and declares neither `SCHEDULE_EXACT_ALARM` nor `USE_EXACT_ALARM`. Requesting exact alarms would need a user-granted special permission on Android 13+ and is unjustifiable for a reminder that says "some time around 08:30".

Consequence to accept and design for: a reminder set for 08:30 may fire between 08:30 and roughly 09:15 depending on Doze. All reminder copy is written to tolerate this - "Today's exercise is ready", not "It's 8:30".

## 2. Workers

All in `core/notifications/work`, all `CoroutineWorker`, all Hilt-injected via `@HiltWorker` + `HiltWorkerFactory`.

| Worker | Trigger | Idempotency key | Job |
| --- | --- | --- | --- |
| `DailyPlanWorker` | Periodic, once per day, window starting 03:00 local | `training_day.date` UNIQUE | Roll over yesterday; ensure today's plan exists |
| `MorningReminderWorker` | One-shot, scheduled for `morningTime` | notification id + date | Post the morning training notification if the program activity is not yet `COMPLETED` |
| `FocusSuggestionWorker` | One-shot, scheduled for the focus activity's `scheduledAtMinutes` | notification id + date | Post the focus nudge if a focus activity exists, is `AVAILABLE`, and the user has not already done a focus session today |
| `ReviewReminderWorker` | Folded into `MorningReminderWorker` | - | Reviews are surfaced in the morning notification, not separately - "Never more than one nudge" per day part |
| `EveningReminderWorker` | One-shot, scheduled for `eveningTime` | notification id + date | Post the reflection notification if the reflection is not `COMPLETED`/`SKIPPED` and no focus session is running |
| `SnoozeReminderWorker` | One-shot, at `snoozedUntil` | activity id | Re-post the exercise notification; transition `SNOOZED -> AVAILABLE` |
| `HabitNudgeWorker` | Daily, at the habit stack's `nudgeTimeMinutes` | habit stack id + date | Post the "right after coffee" nudge |
| `EventLogTrimWorker` | Periodic, weekly, unmetered+idle | - | Trim `event_log` to 2 000 rows |

## 3. Scheduling policy

```kotlin
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "daily_plan",
    ExistingPeriodicWorkPolicy.UPDATE,
    PeriodicWorkRequestBuilder<DailyPlanWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(delayUntilNext(3, 0), TimeUnit.MILLISECONDS)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
        .build()
)
```

- Reminders use `enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, OneTimeWorkRequest)` with `setInitialDelay` to the next occurrence, and each worker re-enqueues itself for the following day as its last step. A periodic worker cannot hit a specific wall-clock time, so a self-rescheduling chain of one-shots is used instead.
- Unique names are stable: `"reminder_morning"`, `"reminder_focus"`, `"reminder_evening"`, `"habit_nudge_{id}"`, `"snooze_{activityId}"`.
- No `Constraints` on reminders (they must fire offline, on battery). `EventLogTrimWorker` requires `requiresDeviceIdle` and `requiresBatteryNotLow`.
- `setExpedited` is used nowhere.

## 4. Rescheduling triggers

The full reminder set is re-derived and re-enqueued (cancel + enqueue, all unique names) when:

| Event | Handler |
| --- | --- |
| Onboarding completes | `ReminderScheduler.rescheduleAll()` |
| Morning or evening time changes | same |
| A notification toggle changes | same |
| A day's plan is generated | schedule that day's focus/evening one-shots |
| An activity is snoozed | enqueue `snooze_{activityId}` |
| An activity completes | cancel its pending reminders |
| Device boot (`BOOT_COMPLETED`) | `rescheduleAll()` - WorkManager survives reboot, but the self-rescheduling chain needs a kick if the device was off across a scheduled time |
| App update (`MY_PACKAGE_REPLACED`) | `rescheduleAll()` |
| Timezone change (`ACTION_TIMEZONE_CHANGED`) | `rescheduleAll()` |

`ReminderScheduler.rescheduleAll()` is itself idempotent: it computes the desired set from preferences + today's plan and enqueues each unique name with `REPLACE`.

## 5. Notification channels

Created at first launch, in `core/notifications/Channels.kt`.

| Channel id | Name | Importance | Used by |
| --- | --- | --- | --- |
| `training` | Daily training | `DEFAULT` | morning, focus suggestion |
| `reflection` | Evening reflection | `DEFAULT` | evening |
| `habits` | Habit nudges | `LOW` | habit stack |
| `focus_session` | Focus session | `LOW`, no sound, no badge | the ongoing timer notification |

Separate channels let a user mute habit nudges without losing the morning reminder - the Settings toggles mirror the channels and, when tapped, also update the app's own preference so the state is consistent whichever surface the user uses.

## 6. Notification content

Every notification: small icon (monochrome Itera glyph), no large icon, no image. Title + one line of body. A single tap opens the deep link (`docs/ux/01-navigation-graph.md` section 7); no action buttons except on the focus session.

| Notification | Title | Body |
| --- | --- | --- |
| Morning (no review due) | Today's exercise is ready | {Technique name} - about {n} min |
| Morning (review due) | Today's exercise and one review | {Technique name}, plus {topic} to recall |
| Focus suggestion | Time for one focus block? | {n} minutes on one thing |
| Evening | Two minutes, three questions | Close the day while it's fresh |
| Habit nudge | Right after {anchor} | {habit} |
| Snooze | Still up for it? | {Technique name} - about {n} min |

Only **one** nudge per day part, per the IA board ("Never more than one nudge").

## 7. Permission

`POST_NOTIFICATIONS` is requested at the end of the Rhythm onboarding step, framed by the inline rationale card. If denied:

- onboarding continues normally;
- reminder toggles in Settings show as off with a caption "Notifications are off for Itera" and a row that opens system settings;
- all in-app behaviour is unaffected - the app is fully usable with notifications denied, and no screen blocks on it.

## 8. Battery and OEM behaviour

Accepted limitations, documented rather than fought:

- Aggressive OEM task killers may drop scheduled work. The app re-derives the plan on open, so a missed reminder costs a notification, never data.
- No "ignore battery optimisations" prompt. It is not justified for reminders.
- The focus timer's foreground service is the one piece of work that must not be killed; that is why it is a service and not a worker.

## 9. Tests

Unit: `ReminderSchedulerTest` with a fake `WorkManager` facade asserts the exact set of unique work names and delays for each preference combination, including all toggles off (empty set) and a day with no focus activity.

Instrumented: `WorkManagerTestInitHelper` with `TestDriver` to run `DailyPlanWorker` twice for the same date and assert one `training_day` row; to run `MorningReminderWorker` after completion and assert no notification is posted.
