# 002 - Core architecture & storage

**Depends on** 001 | **Blocks** 003, 004

## Goal

Put every cross-cutting primitive and both persistent stores in place - DI, time, dispatchers, logging, localisation helpers, the domain model, Room, DataStore and the analytics sink - so no later milestone has to invent infrastructure.

## Included scope

- **Architecture foundation** (old 002): Hilt application/activity, `Clock` + `FakeClock`, dispatcher qualifiers, `DomainError`/`UiText`, injected `Logger` (debug/release), `ObserveEffects`, `AppLanguage` + locale-aware formatter helpers, debug StrictMode, on-demand WorkManager with `HiltWorkerFactory`, `ArchitectureTest` (all boundary rules, vacuous where targets do not exist yet).
- **Domain model** (old 005): every entity, value type and enum in `domain/model`, with invariants.
- **Room** (old 006): database, entities, DAOs, relations, converters, mappers, `CopyResolver`, the initial exported schema, `TrainingPlanRepository` and the progress queries.
- **DataStore** (old 007): `PreferencesRepository`, `FocusTimerRepository`, the write guard on `current_program_day`.
- **Analytics contract core** (moved forward from old 036 - see "Key dependencies"): `Analytics` interface, sealed `Event` catalogue, `LocalAnalytics` writing to `event_log` off the main thread, `NoOpAnalytics` for tests. Call sites are added by the milestones that own the flows.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/002-architecture-foundation.md`, `005-domain-model.md`, `006-room-persistence.md`, `007-datastore-preferences.md`, and the Scope/Notes of `036-analytics-contract.md` (interface, catalogue, sink only).
- `docs/architecture/00-overview.md`, `01-package-structure.md`, `02-state-management.md`, `05-error-handling-and-logging.md`; ADR-0002, 0003, 0004, 0006, 0014, 0017.
- `docs/data/00-domain-model.md`, `01-room-schema.md`, `02-datastore-preferences.md`, `05-migrations-and-content-versioning.md`.
- `docs/i18n/00-localization.md` section 6; `docs/analytics/00-event-model.md`.
- Prototype (for `AppLanguage` only): `design/app/src/main/java/com/itera/app/ui/screens/Onboarding.kt`.

## Key dependencies

- Needs 001 only.
- Analytics interface/catalogue/sink is pulled forward so 004-008 can emit their own events instead of 009 retro-fitting call sites across every feature.
- `AppLanguage` must not mirror the language into DataStore (ADR-0017).

## Acceptance criteria

- [ ] The app launches with Hilt; `Clock`, dispatchers and `Logger` are injectable; `ReleaseLogger` drops v/d.
- [ ] `ArchitectureTest` passes and fails on a deliberate violation of each rule, including `DateTimeFormatter.ofPattern` outside `core/common/time`.
- [ ] Formatter helpers are locale-correct for en/ru/de/es, including the Russian standalone month.
- [ ] Domain invariants are enforced; `domain` has no Android/Room/Compose import.
- [ ] The database creates from scratch, the schema JSON is exported to `app/schemas/`, and every table is assigned to a reset tier (ADR-0014).
- [ ] Preferences round-trip; only the permitted writer can change `current_program_day`.
- [ ] `Analytics.track` writes to `event_log` off the main thread and swallows failures; events carry no user text.

## Required tests

`ArchitectureTest`, `FakeClockTest`, `UiTextTest`, `AppLanguageTest`, `LocaleFormattingTest`, `LoggerTest`, `ObserveEffectsTest`, `DomainInvariantsTest`, `EnumCoverageTest`, `DatabaseCreationTest`, `ConvertersTest`, `MapperTest`, `ResultPayloadTest`, `CopyResolverTest`, `TrainingPlanRepositoryTest`, `ProgressQueryTest`, `DataStorePreferencesRepositoryTest`, `FocusTimerRepositoryTest`, `PreferencesWriteGuardTest`, `LocalAnalyticsTest`, `EventCatalogueTest`.
