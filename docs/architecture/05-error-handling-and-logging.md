# Error handling and logging

## 1. Classification

| Class | Examples | Handling |
| --- | --- | --- |
| **Expected domain outcome** | illegal state transition, activity already completed, empty required field | Not an error. Returned as `Result.failure(DomainError.X)` or simply ignored. Never logged at error level, never shown as a crash. |
| **Recoverable data problem** | undecodable `resultPayload`, missing technique id in the catalog, a review item whose source activity was deleted | Degrade the UI (title-only row, empty section), log at warning with the row id, carry on. |
| **Programming error** | a null that cannot be null, a `when` that is not exhaustive at runtime, a failed Room migration | Crash in debug. In release, caught at the ViewModel boundary, turned into a generic error state, logged. |
| **Environment** | notification permission denied, storage full on export, no app to receive a share intent | Explicit, specific UI message with a next step. Never a generic error. |

There is no network layer, so there is no transient-failure/retry class at all.

## 2. `DomainError`

```kotlin
sealed interface DomainError {
    data class IllegalTransition(val from: ActivityState, val event: String) : DomainError
    data object ActivityNotFound : DomainError
    data object ReviewNotFound : DomainError
    data object CatalogUnavailable : DomainError
    data class ContentMissing(val techniqueId: String) : DomainError
    data class ExportFailed(val reason: String) : DomainError
}
```

Use cases return `Result<T>`; `DomainError` travels in the failure as a `DomainException(error)` wrapper so `Result` semantics still work.

## 3. ViewModel boundary

Every ViewModel wraps its upstream flow:

```kotlin
.catch { t ->
    logger.e(TAG, "state stream failed", t)
    emit(currentState.copy(loading = false, error = t.toUiText()))
}
```

and every event handler:

```kotlin
viewModelScope.launch {
    completeActivity(id, result).onFailure { t ->
        logger.w(TAG, "complete failed", t)
        _effects.send(Effect.ShowMessage(t.toUiText()))
    }
}
```

`Throwable.toUiText()` maps known `DomainError`s to specific strings and everything else to `R.string.error_generic` ("Something went wrong. Your data is safe.").

A `catch` that swallows without logging is a review failure.

## 4. UI presentation

| Situation | Presentation |
| --- | --- |
| Screen cannot load at all | Full-screen `ErrorState` component: icon, one-line cause, a "Try again" button that re-triggers the flow |
| Screen loaded but an action failed | `Snackbar` via an effect. Never a dialog for a recoverable failure |
| A single row is broken | The row renders with its title and a subdued "Couldn't read this entry" subtitle. The list still works |
| A destructive action | `AlertDialog` naming exactly what will be lost |

`ErrorState` and `EmptyState` are shared components (`docs/ux/03-ux-states.md`).

## 5. Logging

```kotlin
interface Logger {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, t: Throwable? = null)
    fun e(tag: String, message: String, t: Throwable? = null)
}
```

- `DebugLogger` delegates to `android.util.Log`.
- `ReleaseLogger` drops `v`/`d`, keeps `i`/`w`/`e` at `Log` level only. **Nothing leaves the device.**
- Injected, never called statically, so tests can assert on log output where it matters (the corrupt-payload path).

### 5.1 What must never be logged

Enforced by review and by a `LoggingPolicyTest` that greps sources for the banned patterns:

- reflection text, exercise notes, Feynman explanations, premortem reasons, habit names, learning-topic titles, task labels - **any user-authored string**;
- full result payloads.
- voice audio, partial/final transcripts, recognition alternatives, command arguments and recognizer bundles/exceptions that might contain user speech (milestone 012). Log only sanitised error categories; extend the existing privacy tests.

Log identifiers and types instead: `"activity 412 (FEYNMAN) payload undecodable"`, never the payload.

### 5.2 Tags

One `private const val TAG` per file, equal to the class name, max 23 characters.

## 6. Crash reporting

None in the MVP. No Crashlytics, no Sentry, no network permission. Crashes are surfaced by Play Console vitals for internal testing builds, which needs no SDK.

If crash reporting is added later it must be opt-in, it must be documented in the privacy note, and it must strip user text.

## 7. StrictMode

Debug builds enable `StrictMode` with `detectAll()` and `penaltyLog()` (not `penaltyDeath`, which would make Room and DataStore startup noisy on some devices). Disk-read violations on the main thread are treated as bugs to fix, not to suppress.

## 8. Fail-fast points

Deliberate hard failures in debug builds only:

- catalog JSON fails to parse or fails validation;
- a Room migration is missing;
- an `ActivityResult` type does not match its `exerciseType` on write;
- a string resource is missing for a `copyKey`.

Each of these is a content or code bug that must not reach release, and each is covered by a unit test so release builds never encounter it.
