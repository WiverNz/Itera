# ADR-0003: Repository interfaces in domain; use cases only where logic is non-trivial

Status: accepted (2026-09-22)

## Context

A strict use-case-per-operation policy generates dozens of one-line classes. No use cases at all pushes engine logic into ViewModels, where it cannot be unit-tested without Android.

## Decision

- Repository **interfaces** live in `domain/repository`; implementations in `data/repository`.
- A **use case** exists only when an operation (a) coordinates more than one repository, (b) contains branching domain rules, or (c) must be transactional.
- Otherwise a ViewModel calls the repository directly.

The use cases that qualify: `GenerateDailyPlanUseCase`, `EnsureTodayPlanUseCase`, `CompleteActivityUseCase`, `SnoozeActivityUseCase`, `AdvanceProgramDayUseCase`, `ScheduleReviewUseCase`, `SubmitReviewUseCase`, `ObserveProgressUseCase`, `ResetProgramUseCase`, `EraseAllDataUseCase`, `ExportJournalUseCase`.

## Alternatives considered

- **A use case for every repository method** - ceremonial; rejected.
- **No use cases; fat repositories** - puts cross-aggregate transactions in `data`, losing the pure-Kotlin testability guarantee. Rejected.

## Consequences

- `CompleteActivityUseCase` is the single place post-completion effects happen, so the rules cannot drift between callers.
- Some ViewModels depend on repositories directly; that is intended and not a smell.

## Migration implications

None.
