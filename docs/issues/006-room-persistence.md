# 006 - Room persistence foundation

**Phase** 1 - Foundation | **Depends on** 005 | **Blocks** 008, 011, 012, 013, 036

## Goal

Implement the complete database from `docs/data/01-room-schema.md` at version 1, with DAOs, converters, mappers, the result-payload codec, and the seed callback.

## User value

Nothing is lost. Training history survives process death, restarts and updates.

## Scope

- All 11 entities with the documented columns, types, nullability, foreign keys and indices.
- `Converters` for `LocalDate` (epoch day), `LocalTime` (minutes), `Instant` (epoch millis) and the enum-as-`TEXT` mappings.
- All 11 DAOs with the queries listed in section 3, plus the `TrainingDayWithActivities` relation POJO.
- `IteraDatabase` at version 1, `exportSchema = true`, WAL journal mode, seed callback.
- `data/mapper/`: entity to domain and back for every aggregate.
- `data/mapper/ResultPayloadCodec`: `kotlinx.serialization` polymorphic codec over mirrored `@Serializable` DTOs, with frozen `@SerialName` values and `ignoreUnknownKeys = true`. An undecodable payload returns `null` and logs a warning naming the row id and type only.
- `data/copy/CopyResolver`: resolves `copyKey` + `copyArgs` to localised strings.
- `data/repository/`: `RoomTrainingPlanRepository`, `RoomReviewRepository`, `RoomProgressRepository`, `RoomLearningTopicRepository` - the read and simple-write paths. Complex transactional writes land in issue 014.
- `di/DatabaseModule` and `di/RepositoryModule`.
- Update `data_extraction_rules.xml` to exclude the database from cloud backup and include it in device transfer.

## Non-goals

- DataStore (issue 007).
- Catalog loading (issue 008).
- `CompleteActivityUseCase` and the post-completion effects (issue 014).
- Any migration (there is only version 1).

## Implementation notes

- `plan_activity.practiceDate` is denormalised from the parent day on insert. It must be written on every insert path, including manual practices; a missing value breaks every mastery query. Assert it in a test.
- The seed callback writes one `technique_state` row per catalog technique. The catalog is not available until issue 008, so seed from a hard-coded id list in this issue and replace it with a catalog read in 008. Note the temporary coupling in a comment referencing this issue.
- Aggregate queries for mastery (`countDistinctPracticeDays`, `usesSince`, `firstUse`, `usedInCombination`) belong here as DAO methods; the *classification* belongs to issue 013.
- Result DTOs mirror the domain sealed hierarchy one-for-one. Do not annotate domain types (ADR-0004). Keep the mirror mechanical so the mapping is obviously total.
- `CopyResolver` needs `Context`; inject it into the repository, not into the mapper functions, and keep mapper functions pure by passing the resolver in.
- Use `@Transaction` for `observeWithActivities`. Without it, Room emits a torn read.
- Do not enable `allowMainThreadQueries` even in tests; use `runTest`.

## Affected layers

`data/database`, `data/mapper`, `data/copy`, `data/repository`, `di`.

## Acceptance criteria

- [ ] All 11 tables exist with the exact columns, types, foreign keys and indices in `docs/data/01-room-schema.md`.
- [ ] The exported schema JSON for version 1 is committed.
- [ ] The database opens on a fresh install and seeds `technique_state`.
- [ ] `observeToday()` emits a `TrainingDay` with its activities, and re-emits on any activity change.
- [ ] Every `ActivityResult` subtype round-trips through the codec unchanged.
- [ ] A deliberately corrupted `resultPayload` yields `result = null`, logs a warning containing the row id and **not** the payload, and does not throw.
- [ ] `practiceDate` is populated on every insert path.
- [ ] Foreign-key cascades behave as documented (deleting a training day removes its activities).
- [ ] Cloud backup excludes the database; device transfer includes it.
- [ ] `ArchitectureTest` confirms no DAO returns a domain type.

## Unit test expectations

- `MapperTest` - round-trip for each aggregate, including all-null optional fields.
- `ResultPayloadTest` - round-trip for all nine `ActivityResult` subtypes; unknown key ignored; corrupt payload returns null and logs.
- `ConvertersTest` - each converter across a DST boundary and the epoch.
- `CopyResolverTest` (Robolectric) - each `copyKey` in `docs/engine/01-training-plan-engine.md` section 3.4 resolves with its arguments.

## UI test expectations

None.

## Integration test expectations

- `DatabaseCreationTest` - fresh database opens, seeds, and survives close/reopen.
- `TrainingPlanRepositoryTest` - insert a day with activities, observe, update a state, observe the change; cascade delete.
- `ProgressQueryTest` - the four mastery aggregate queries against a seeded fixture return the expected numbers.

## Manual verification

1. Install, let the database create, inspect it with the App Inspection database viewer: 11 tables, seeded `technique_state`.
2. Force-stop and reopen; data is intact.
3. Back up and restore via device transfer (or verify the rules file); cloud backup excludes the database.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 6, 9, 10, 11.
