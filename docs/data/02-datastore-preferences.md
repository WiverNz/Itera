# DataStore preferences

Two DataStore instances. Neither holds structured training history - that is Room's job (guardrail).

## 1. `user_prefs` (Preferences DataStore)

File: `user_prefs.preferences_pb`. Keys and defaults:

| Key | Type | Default | Set by |
| --- | --- | --- | --- |
| `onboarding_completed` | Boolean | `false` | onboarding step 3 |
| `focus_areas` | String set | `[]` | onboarding step 1 |
| `morning_time_minutes` | Int | `510` (08:30) | onboarding step 2, You |
| `evening_time_minutes` | Int | `1260` (21:00) | onboarding step 2, You |
| `time_budget` | String | `STANDARD` | onboarding step 2, You |
| `pace` | String | `STANDARD` | You |
| `theme` | String | `SYSTEM` | You |
| `notify_morning` | Boolean | `true` | onboarding step 2, You |
| `notify_focus` | Boolean | `true` | You |
| `notify_reviews` | Boolean | `true` | You |
| `notify_evening` | Boolean | `true` | onboarding step 2, You |
| `ai_coach_enabled` | Boolean | `false` | always false in the MVP |
| `program_started_on` | Long (epoch day) | absent | onboarding step 3 |
| `current_program_day` | Int | `1` | `AdvanceProgramDayUseCase` only |
| `content_version` | Int | `0` | `ContentReconciler` |
| `last_seen_day_complete` | Long (epoch day) | absent | Day complete screen |

Enums are stored as their `name`. Unknown values on read fall back to the default and log a warning - a downgrade must never crash.

## 2. Repository

```kotlin
class DataStorePreferencesRepository(
    private val store: DataStore<Preferences>
) : PreferencesRepository {

    override val preferences: Flow<UserPreferences> =
        store.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it.toUserPreferences() }
            .distinctUntilChanged()

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        store.edit { prefs -> prefs.applyAll(transform(prefs.toUserPreferences())) }
    }
}
```

The `IOException -> emptyPreferences()` catch is required: a corrupt file must degrade to defaults, not crash the app at startup.

`update` takes a whole-object transform so callers cannot forget to keep a field.

## 3. `current_program_day` is privileged

Only `AdvanceProgramDayUseCase` and the reset use cases may write it. Everything else reads it. This is the single most important invariant in the app - a stray write would skip or repeat a curriculum day. Enforced by:

- the key being `internal` to `data/preferences`;
- `PreferencesRepository.update` being the only public writer, with a unit test asserting no other production call site passes a changed `currentProgramDay`.

## 4. `focus_timer` (typed DataStore)

File: `focus_timer.pb`. Holds a single serialised `FocusTimerState` (`docs/engine/05-timer-lifecycle.md`), with `kotlinx.serialization` and a custom `Serializer<FocusTimerState?>`.

Separate from `user_prefs` because:

- it is written every few seconds during a session and preferences are not;
- it must be readable before the database opens;
- corruption here is harmless (discard the timer), while corrupting preferences would reset the user's rhythm.

`corruptionHandler = ReplaceFileCorruptionHandler { null }`.

## 5. Migration

DataStore has no schema version. Policy:

- Adding a key with a default is free.
- Removing a key: stop reading it; leave it in the file. A `PreferencesCleanupWorker` is deliberately **not** written - the cost of a stale key is bytes.
- Changing a key's type requires a **new key name** plus a one-shot read-old/write-new migration registered with `DataStoreFactory`'s `migrations` parameter. Never reuse a key name with a different type.

## 6. Backup

Both files are excluded from cloud backup and included in device-to-device transfer (`docs/data/01-room-schema.md` section 8), for the same reason: a restored `current_program_day` that disagrees with a restored database would corrupt the program.

## 7. Tests

- `DataStorePreferencesRepositoryTest` (Robolectric or a temp-folder `DataStore`): defaults on an empty store; round-trip of every field; unknown enum falls back; `IOException` yields defaults.
- `PreferencesMigrationTest`: a file written by the previous key set reads correctly.
