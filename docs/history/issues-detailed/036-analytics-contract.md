# 036 - Analytics contract

**Phase** 6 - Infrastructure | **Depends on** 002, 006 | **Blocks** 039

## Goal

Implement the local analytics contract in `docs/analytics/00-event-model.md` - the `Analytics` interface, the event catalogue, the `event_log` sink, and the privacy guard test.

## User value

None directly. It exists for debugging, for QA assertions, and so that the question "what would we send?" is already answered and reviewed before anyone can send anything.

## Scope

- `analytics/Analytics.kt` (interface), `analytics/Event.kt` (the sealed catalogue), `analytics/LocalAnalytics.kt` (writes to `event_log`), `analytics/NoOpAnalytics.kt` (tests).
- Every event in the catalogue's seven groups, with exactly the documented parameters.
- Instrumentation call sites across the features already built - onboarding, plan, activities, focus, review, progression, navigation, settings, notifications.
- `EventLogTrimWorker` trimming to 2 000 rows weekly (the worker shell exists from issue 033; the trim logic lands here).
- `NoUserTextLoggedTest`: a source scan asserting no production call passes a note, reflection, explanation, topic title, task label, habit name, project name or display name to `Analytics` **or** to `Logger`.

## Non-goals

- Any remote analytics, SDK or upload path.
- A UI for reading the log.
- Any identifier - there is no user id, install id or advertising id, deliberately.

## Implementation notes

- Fire-and-forget on `@IoDispatcher`. A failure to log is swallowed. Analytics must never affect behaviour, block a transaction, or surface an error.
- **No parameter may carry user text.** Booleans replace content: `hasNote`, not the note. `screen_viewed` records a route class name, never its arguments. `setting_changed` records times as an hour integer.
- The catalogue is a sealed hierarchy, not free-form strings, so a new event is a reviewable code change and a typo is a compile error.
- `NoUserTextLoggedTest` is the deliverable that makes this issue safe. Write it as a source scan over `src/main` looking for known user-text identifiers (`note`, `explanation`, `answer`, `taskLabel`, `anchor`, `habit`, `projectName`, `title` on a topic, `displayName`) appearing in an `Analytics.track` or `Logger` call argument. Accept some conservatism - a false positive is cheap to work around and a false negative is a privacy leak.
- The trim worker requires `requiresDeviceIdle` and `requiresBatteryNotLow`.
- `event_log` is deleted by "Erase everything" and kept by "Reset program" (it is diagnostic, not user content) - verify against issue 014's reset tiers.

## Affected layers

`analytics`, call sites across `feature/*` and `domain/*`, `core/notifications`.

## Acceptance criteria

- [ ] Every event in the catalogue exists with exactly the documented name and parameters.
- [ ] No event parameter can carry user-authored text.
- [ ] Events are written to `event_log` off the main thread.
- [ ] A write failure is swallowed and does not affect the calling flow.
- [ ] `screen_viewed` records the route class name only.
- [ ] `setting_changed` records times as hour integers.
- [ ] No identifier of any kind is recorded.
- [ ] Call sites exist for every documented event across the already-built features.
- [ ] `EventLogTrimWorker` trims to 2 000 rows, newest kept, under its constraints.
- [ ] `event_log` is cleared by "Erase everything" and kept by "Reset program".
- [ ] `NoUserTextLoggedTest` passes and fails when a violation is introduced deliberately.
- [ ] No network permission and no analytics dependency was added.

## Unit test expectations

`EventCatalogueTest` - every event's name and parameter keys match the document (a transcription guard).

`LocalAnalyticsTest` - events are written; a failing DAO does not throw to the caller; writes happen off the main thread.

`NoUserTextLoggedTest` - the source scan, with a deliberate violation fixture proving it fails.

`EventLogTrimTest` - 2 500 rows trims to the newest 2 000.

## UI test expectations

None.

## Integration test expectations

- `AnalyticsFlowTest` - completing a full daily loop produces the expected event sequence in `event_log`, in order.
- `ResetAnalyticsTest` - "Reset program" keeps the log; "Erase everything" clears it.

## Manual verification

1. Complete a daily loop; inspect `event_log` with the database inspector; confirm the sequence and that no row contains user text.
2. Write a reflection containing a distinctive phrase; search the whole `event_log` and logcat for it; it appears nowhere.
3. Introduce a violation (log a note); confirm the test fails; revert.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections, with particular attention to section 9.
