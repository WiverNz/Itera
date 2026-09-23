# Analytics event model

**Local only.** Events are written to the `event_log` table and never transmitted. There is no analytics SDK, no network permission, and no identifier of any kind.

## 1. Why it exists at all

Three reasons, none of them measurement of users:

1. **Debugging** - reconstructing what happened before a reported problem, from the device.
2. **Manual QA** - asserting that a flow fired the expected sequence.
3. **A defined seam** - if remote analytics is ever added, the event vocabulary already exists and has been reviewed for privacy, so the decision is "do we send these?", not "what would we send?".

## 2. Interface

```kotlin
interface Analytics {
    fun track(event: Event)
}

sealed interface Event {
    val name: String
    val params: Map<String, Any?>
}
```

`LocalAnalytics` writes to `event_log`. The only other implementation is `NoOpAnalytics`, used in tests.

Calls are fire-and-forget on `@IoDispatcher`. A failure to log is swallowed - analytics must never affect behaviour or surface an error.

## 3. Event catalogue

Every event, with its parameters. **No parameter may contain user-authored text.**

### Onboarding

| Event | Params |
| --- | --- |
| `onboarding_started` | - |
| `onboarding_step_completed` | `step` (1-3) |
| `onboarding_completed` | `focusAreaCount`, `timeBudget`, `morningHour`, `eveningHour`, `notificationsGranted` |

### Plan

| Event | Params |
| --- | --- |
| `plan_generated` | `programDay`, `activityCount`, `reviewCount`, `hasCombination`, `generatorVersion` |
| `day_rolled_over` | `previousStatus`, `expiredCount` |
| `day_completed` | `programDay`, `completedCount`, `skippedCount`, `durationMinutes` |
| `program_day_advanced` | `from`, `to` |

### Activities

| Event | Params |
| --- | --- |
| `exercise_opened` | `techniqueId`, `exerciseType`, `source` |
| `exercise_started` | `techniqueId`, `exerciseType` |
| `exercise_completed` | `techniqueId`, `exerciseType`, `source`, `durationSeconds`, `difficulty`, `hasNote` (boolean, not the note) |
| `exercise_snoozed` | `techniqueId`, `minutesDeferred` |
| `exercise_skipped` | `techniqueId`, `exerciseType` |
| `exercise_abandoned` | `techniqueId`, `secondsSpent` |
| `practice_logged` | `techniqueId` |

### Focus

| Event | Params |
| --- | --- |
| `focus_started` | `techniqueId`, `plannedMinutes` |
| `focus_paused` | `remainingSeconds` |
| `focus_extended` | `addedSeconds`, `timesExtended` |
| `focus_completed` | `plannedSeconds`, `actualSeconds`, `completedNaturally` |
| `focus_restored` | `remainingSeconds`, `afterProcessDeath` |

### Review

| Event | Params |
| --- | --- |
| `review_due` | `techniqueId`, `stageIndex`, `daysOverdue` |
| `review_opened` | `techniqueId`, `stageIndex` |
| `review_revealed` | `techniqueId`, `stageIndex` |
| `review_graded` | `techniqueId`, `stageBefore`, `stageAfter`, `grade` |

### Progression

| Event | Params |
| --- | --- |
| `technique_unlocked` | `techniqueId`, `programDay` |
| `mastery_changed` | `techniqueId`, `from`, `to` |
| `skill_level_changed` | `skill`, `from`, `to` |

### Navigation and settings

| Event | Params |
| --- | --- |
| `screen_viewed` | `route` (the route class name only) |
| `setting_changed` | `key`, `value` (enums and booleans only; times as hour integers) |
| `reset_performed` | `tier` (`program` / `all`) |
| `journal_exported` | `range`, `dayCount`, `sizeBytes` |

### Notifications

| Event | Params |
| --- | --- |
| `notification_scheduled` | `type`, `delayMinutes` |
| `notification_posted` | `type` |
| `notification_suppressed` | `type`, `reason` |
| `notification_opened` | `type` |

## 4. Privacy rules

Enforced by `NoUserTextLoggedTest` and by review:

1. No parameter carries a note, reflection answer, explanation, task label, topic title, habit name, project name or display name.
2. Booleans replace content: `hasNote`, not the note.
3. No device identifier, advertising id, install id or user id exists. There is nothing to correlate across devices because there is nothing to send.
4. `screen_viewed` records a route name, never its arguments.
5. `setting_changed` records times as an hour integer, not a precise schedule that would profile a routine any more than necessary.

## 5. Storage

- `event_log` is trimmed to the newest **2 000 rows** weekly by `EventLogTrimWorker`.
- It is deleted entirely by "Erase everything" and left intact by "Reset program" (it is diagnostic, not user content).
- It is excluded from cloud backup along with the rest of the database.
- There is no UI to read it. In debug builds, `adb shell` plus a `Room` inspection is the intended access path; a debug-only export is acceptable if it proves useful.

## 6. If remote analytics is ever added

It would require, in this order: a privacy-policy update, an explicit opt-in that defaults to off, an `INTERNET` permission (currently absent, which makes the change reviewable), a re-audit of every parameter above, and a documented retention period. This is a post-MVP decision with a product cost, not a technical convenience.
