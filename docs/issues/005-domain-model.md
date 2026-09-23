# 005 - Domain model

**Phase** 1 - Foundation | **Depends on** 002 | **Blocks** 006, 007, 008, 009, 010, 012

## Goal

Create the pure-Kotlin domain types and repository interfaces from `docs/data/00-domain-model.md`, with no Android, Room or serialization annotations.

## User value

None directly. It is the vocabulary every other issue speaks.

## Scope

- All enumerations in section 1.
- Catalog types in section 2: `Technique`, `TechniqueId`, `TechniqueDefaults`, `Curriculum`, `CurriculumDay`, `CombinationStep`.
- Plan types in section 3: `TrainingDay`, `PlanActivity`, with their computed properties.
- Template types in section 4: `ExerciseTemplate`, `ExerciseBlock` (six variants), `CompletionRule` (three variants).
- Result types in section 5: `ActivityResult` (nine variants), `BlockValue` (five variants), and the supporting data classes.
- Review types in section 6: `ReviewItem`, `ReviewState`, `LearningTopic`.
- Progress types in section 7: `TechniqueProgress`, `SkillProgress`, `ProgressSummary`, `DayDot`, `HistoryEntry`.
- `UserPreferences` in section 8.
- All nine repository interfaces in section 9.
- `domain/coach/CoachFeedbackProvider.kt` and its request/response types (ADR-0016) - interface only.

## Non-goals

- Any implementation of any interface.
- Any serialization (that lives on DTOs in `data`).
- Engine logic (issues 009-014).

## Implementation notes

- Use `java.time` types directly (`LocalDate`, `LocalTime`, `Instant`). `minSdk 26` allows it.
- `TechniqueId` is a `@JvmInline value class` over `String`.
- Prefer non-null with sensible defaults over nullable where the invariants in section 10 guarantee a value.
- `ActivityResult` and its members must be plain data classes - **no** `@Serializable`. Serialization lives on mirrored DTOs in `data/mapper` so the domain stays framework-free (ADR-0004, ADR-0006).
- `PlanActivity.title`/`subtitle`/`instruction` are resolved strings; the domain type does not know about copy keys. That translation happens in the mapper.
- Add KDoc to each type naming the document section it comes from, so a future reader can find the spec.
- Repository interfaces return `Flow` for reads and are `suspend` for writes, exactly as documented. Do not add convenience overloads.

## Affected layers

`domain/model`, `domain/repository`, `domain/coach`.

## Acceptance criteria

- [ ] Every type in `docs/data/00-domain-model.md` exists with the documented name, fields and nullability.
- [ ] `ArchitectureTest` confirms `domain` has no `android.*`, `androidx.*`, Room or Compose import.
- [ ] No domain type carries a serialization or persistence annotation.
- [ ] `TrainingDay.completedCount` / `requiredCount` and `PlanActivity.countsTowardDay` / `isCountedComplete` behave per the documented definitions.
- [ ] All nine repository interfaces compile with the documented signatures.
- [ ] The project still builds with no implementations present.

## Unit test expectations

- `DomainInvariantsTest` - the computed properties on `TrainingDay` and `PlanActivity` across: an all-optional day, an all-required day, a mixed day, and an empty day.
- `EnumCoverageTest` - each enum's constant set matches the document exactly (a transcription guard, cheap and catches omissions).

## UI test expectations

None.

## Manual verification

1. `./gradlew build` succeeds.
2. Read `docs/data/00-domain-model.md` side by side with the source and confirm no field was renamed or dropped.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 10, 11.
