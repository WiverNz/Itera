# 002 - Architecture foundation

**Phase** 1 - Foundation | **Depends on** 001 | **Blocks** 005, 036

## Goal

Put the cross-cutting primitives in place - DI, time, dispatchers, logging, result types, boundary enforcement - so no later issue has to invent them.

## User value

None directly. It is what makes the engine testable and the boundaries real.

## Scope

- `IteraApplication` annotated `@HiltAndroidApp`; `MainActivity` annotated `@AndroidEntryPoint`.
- `core/common/Clock.kt`: expose `java.time.Clock` through Hilt; provide `FakeClock` in `src/test`.
- `core/common/dispatchers/`: `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` qualifiers and a `DispatcherModule`.
- `core/common/result/`: `DomainError` sealed interface, `DomainException`, and `Throwable.toUiText()` helpers per `docs/architecture/05-error-handling-and-logging.md`.
- `core/common/Logger.kt` with `DebugLogger` and `ReleaseLogger`, bound by build type.
- `core/designsystem/.../UiText.kt` (the `UiText` type from `docs/architecture/02-state-management.md` section 3).
- **Localisation primitives** (D-15) - port from the prototype's `Onboarding.kt`:
  - `AppLanguage` with `tags = ["", "en", "ru", "de", "es"]`, `current()`, `set(tag)`, `nativeName(tag)`, `localName(tag, inLocale)`.
  - `currentLocale()` reading `LocalConfiguration.current.locales[0]`.
  - `core/common/time/` formatter helpers: `formatTime`, `formatDateMedium`, `formatDateFull`, `formatMonthYear` (standalone `LLLL` form), `formatShortDate`, `weekdayNarrow`, `firstDayOfWeek` - each taking the active locale, per `docs/i18n/00-localization.md` section 6.
  - `is24Hour()` wrapping `DateFormat.is24HourFormat` for the read-only Settings row (D-16).
- `ObserveEffects` helper composable for consuming effect channels with lifecycle awareness.
- `di/` module skeletons: `DispatcherModule`, `ClockModule`, `LoggerModule`. Others are added by the issues that need them.
- `ArchitectureTest`: a JVM test walking `src/main/java` and asserting the seven boundary rules in `docs/architecture/01-package-structure.md` section 3. Rules whose targets do not exist yet (e.g. no `data` package) must pass vacuously, not be omitted.
- Enable `StrictMode` in debug with `detectAll()` + `penaltyLog()`.
- Wire `HiltWorkerFactory` in the application's `Configuration.Provider`, with WorkManager on-demand initialisation (remove the default initializer from the manifest so startup is not blocked).

## Non-goals

- Any repository, entity or use case.
- Any screen.
- Notification channels (issue 033).

## Implementation notes

- `ArchitectureTest` is a regex/source walk, not a bytecode analysis - keep it simple and give each failure a message naming the offending file and line. Example rules:
  - files under `domain/` must not match `^import (android|androidx)\.`
  - files under `feature/` must not match `^import com\.wivernz\.itera\.data\.`
  - no file outside `core/designsystem/theme/Color.kt` may match `#[0-9A-Fa-f]{6}`
  - no file outside `core/common/Clock.kt` may match `(LocalDate|LocalTime|Instant)\.now\(\)|System\.currentTimeMillis\(\)`
- `Logger` must be injected, never a static object, so `NoUserTextLoggedTest` (issue 036) can reason about call sites.
- `ObserveEffects` should use `repeatOnLifecycle(STARTED)` so an effect emitted while backgrounded is delivered on return, exactly once.
- Do not add a `BaseViewModel`. The template in `docs/architecture/02-state-management.md` is inheritance-free on purpose.
- `AppLanguage` must **not** mirror the language into DataStore - `AppCompatDelegate` owns persistence (ADR-0017). It is a thin wrapper, not a store.
- The formatter helpers exist so no screen ever calls `DateTimeFormatter.ofPattern` directly. `ArchitectureTest` gains a rule for that.

## Affected layers

`core/common`, `di`, application class.

## Acceptance criteria

- [ ] The app launches with Hilt installed; injecting `Clock` into a test entry point succeeds.
- [ ] `FakeClock` supports `advance(Duration)` and `setDate(LocalDate)` and is used by at least one test.
- [ ] `ArchitectureTest` passes and **fails** when a violation is introduced deliberately (demonstrate each of the seven rules once).
- [ ] `ReleaseLogger` drops verbose and debug calls.
- [ ] `UiText.Res` and `UiText.Plural` resolve correctly in a composable.
- [ ] `ObserveEffects` delivers an effect emitted while the screen is stopped, once, on resume.
- [ ] WorkManager initialises on demand and the default initializer is removed from the manifest.
- [ ] StrictMode logs (and does not kill) on debug.
- [ ] `AppLanguage.set` then `current()` round-trips for each of the five tags; the empty tag clears the list.
- [ ] Every formatter helper returns locale-correct output for en, ru, de and es, including the Russian standalone month form.
- [ ] `ArchitectureTest` fails if a screen calls `DateTimeFormatter.ofPattern` outside `core/common/time`.

## Unit test expectations

- `ArchitectureTest` - one test method per rule, each with a positive and a negative fixture.
- `FakeClockTest` - advancing and setting produce the expected instants across a DST boundary.
- `UiTextTest` - `Res` with args, `Plural` with counts 0/1/2.
- `AppLanguageTest` (Robolectric) - round-trip per tag; empty tag clears.
- `LocaleFormattingTest` - every helper across the four locales; Russian standalone months; locale first-day-of-week; 12- and 24-hour rendering.
- `LoggerTest` - `ReleaseLogger` drops v/d and keeps w/e.

## UI test expectations

- `ObserveEffectsTest` - an effect sent while the lifecycle is `CREATED` is delivered once on `STARTED`, and not again on a subsequent stop/start.

## Manual verification

1. Launch the debug build; no StrictMode crash.
2. Introduce an `android.util.Log` import into a `domain` file; `ArchitectureTest` fails with a message naming the file. Revert.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 9, 10, 11.
