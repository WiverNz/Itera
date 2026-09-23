# 007 - DataStore preferences

**Phase** 1 - Foundation | **Depends on** 005 | **Blocks** 011, 015, 016, 034

## Goal

Implement both DataStore instances from `docs/data/02-datastore-preferences.md`: user preferences and the focus-timer state.

## User value

The user's rhythm, theme and choices persist, and a running focus session survives anything.

## Scope

- `user_prefs` Preferences DataStore with the 16 keys, defaults and enum handling in section 1.
- `DataStorePreferencesRepository` implementing `PreferencesRepository`, with the `IOException -> emptyPreferences()` recovery and a whole-object `update` transform.
- `focus_timer.pb` typed DataStore holding a nullable `FocusTimerState`, with a `kotlinx.serialization` serializer and `ReplaceFileCorruptionHandler { null }`.
- `FocusTimerRepository` with `observe()`, `save(state)`, `clear()` and `restore()` implementing the restore table in `docs/engine/05-timer-lifecycle.md` section 2. The *use* of restore is issue 021; the storage and the decision table are here.
- `di/DataStoreModule`.
- Update `data_extraction_rules.xml` for both files.
- Make `current_program_day` writable only through `PreferencesRepository.update`, with the key `internal` to `data/preferences`.

## Non-goals

- The Settings UI (issue 034).
- The focus timer UI or service (issue 021).
- Reading the theme preference into `IteraTheme` (issue 015).

## Implementation notes

- Unknown enum values on read must fall back to the default **and log a warning**. A user downgrading must not crash.
- `update` takes `(UserPreferences) -> UserPreferences` so a caller cannot accidentally drop a field by writing a partial object.
- The focus-timer store must be readable before the database opens - do not make it depend on Room in any way.
- `restore()` returns a sealed result (`None`, `Running`, `Paused`, `AutoComplete`, `Discarded`) rather than a nullable state, so issue 021 cannot forget a branch.
- Do not add a `PreferencesCleanupWorker`. Removed keys are left in the file (section 5).

## Affected layers

`data/preferences`, `data/focus`, `di`, resources.

## Acceptance criteria

- [ ] All 16 keys read and write with the documented defaults.
- [ ] A fresh install reads every default without error.
- [ ] An unknown enum string falls back to the default and logs.
- [ ] A corrupt preferences file yields defaults rather than a crash.
- [ ] A corrupt focus-timer file yields `null` rather than a crash.
- [ ] `FocusTimerState` round-trips including `endsAt`, `pausedAt` and `accumulatedPauseMs`.
- [ ] `restore()` returns the correct branch for each row of the restore table, driven by `FakeClock`.
- [ ] `current_program_day` cannot be written from outside `data/preferences` (verified by the key's visibility and a test).
- [ ] Both files are excluded from cloud backup and included in device transfer.

## Unit test expectations

- `DataStorePreferencesRepositoryTest` (temp-folder DataStore) - defaults; round-trip of every field; unknown enum; `IOException` recovery; `update` preserves untouched fields.
- `FocusTimerRepositoryTest` - round-trip; every branch of the restore table with `FakeClock`; a state older than 24 h is discarded.
- `PreferencesWriteGuardTest` - no production call site outside the reset use cases passes a changed `currentProgramDay`.

## UI test expectations

None.

## Manual verification

1. Set values, force-stop, reopen: values persist.
2. Corrupt `user_prefs.preferences_pb` with `adb`; the app opens with defaults instead of crashing.
3. Confirm the extraction rules file lists both DataStore files.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 6, 10, 11.
