# Package structure

Root: `com.wivernz.itera`. Single Gradle module (`:app`).

Planned milestone 012 adds only `core/voice` (Android recognition adapter), `domain/voice` (typed commands, pure parser/matching) and shared `feature/voice` presentation. Existing feature ViewModels dispatch to current use cases/repository interfaces. No voice DAO, new module or speech-to-storage dependency; details in [voice architecture](07-voice-input.md).

## 1. Tree

```
com.wivernz.itera
├── IteraApplication.kt              @HiltAndroidApp
├── MainActivity.kt                  single activity, edge-to-edge, splash
│
├── core
│   ├── common
│   │   ├── Clock.kt                 injected clock + FakeClock in tests
│   │   ├── dispatchers/             @IoDispatcher, @DefaultDispatcher qualifiers
│   │   ├── result/                  Result helpers, DomainError
│   │   └── time/                    LocalDate/LocalTime formatting helpers
│   ├── designsystem
│   │   ├── theme/                   Color, Type, Shape, Motion, SkillColors, Theme, NightSurface
│   │   ├── component/               PrimaryButton, Badge, SelectableChip, OptionRow,
│   │   │                            TechniqueTile, HeroCard, ChecklistRow, MasteryLadder,
│   │   │                            ReviewLadder, SegmentedControl, SettingsRow,
│   │   │                            StepHeader, ProgressSegmentBar, EmptyState, ErrorState
│   │   ├── icon/                    IteraIcons (hand-authored ImageVectors)
│   │   └── preview/                 @PreviewLightDark wrappers, sample data
│   ├── navigation
│   │   ├── Routes.kt                @Serializable route types
│   │   └── AppNavHost.kt            graph builder
│   └── notifications
│       ├── Channels.kt
│       ├── IteraNotifier.kt         builds and posts every notification
│       ├── ReminderScheduler.kt
│       ├── work/                    the CoroutineWorkers
│       └── receiver/                BootReceiver, TimezoneReceiver
│
├── domain                           PURE KOTLIN - no android.*, no Room, no Hilt android deps
│   ├── model/                       everything in docs/data/00-domain-model.md
│   ├── repository/                  interfaces only
│   ├── training/
│   │   ├── ActivityStateMachine.kt
│   │   ├── GenerateDailyPlanUseCase.kt
│   │   ├── CompleteActivityUseCase.kt
│   │   ├── SnoozeActivityUseCase.kt
│   │   └── AdvanceProgramDayUseCase.kt
│   ├── review/
│   │   ├── ReviewScheduler.kt
│   │   ├── ScheduleReviewUseCase.kt
│   │   └── SubmitReviewUseCase.kt
│   ├── progress/
│   │   ├── Mastery.kt
│   │   ├── SkillLevels.kt
│   │   └── ObserveProgressUseCase.kt
│   ├── unlock/
│   │   └── UnlockRules.kt
│   └── coach/
│       └── CoachFeedbackProvider.kt  interface, AI seam (ADR-0016)
│
├── data
│   ├── catalog/                     JSON DTOs, asset loader, DTO -> domain mappers
│   ├── database/
│   │   ├── IteraDatabase.kt
│   │   ├── Converters.kt
│   │   ├── entity/
│   │   ├── dao/
│   │   ├── relation/                TrainingDayWithActivities, ...
│   │   └── migration/
│   ├── preferences/                 DataStore, UserPreferences serializer
│   ├── focus/                       FocusTimerState DataStore + FocusTimerRepository impl
│   ├── copy/                        CopyResolver (copyKey + args -> localised strings)
│   ├── mapper/                      entity <-> domain
│   ├── repository/                  implementations
│   └── export/                      Markdown journal writer
│
├── feature
│   ├── onboarding/                  welcome, goals, rhythm, firstweek
│   ├── today/
│   ├── exercise/
│   │   ├── runner/                  ExerciseRunnerScreen host + intro + result
│   │   ├── template/                data-driven blocks
│   │   ├── eisenhower/
│   │   ├── feynman/
│   │   ├── premortem/
│   │   ├── habitstack/
│   │   ├── review/
│   │   └── combination/
│   ├── focus/
│   ├── reflection/
│   ├── daycomplete/
│   ├── train/                       Train home, library, technique detail
│   ├── progress/                    Progress home, history
│   └── you/                         settings, topics, privacy
│
├── analytics
│   ├── Analytics.kt                 interface
│   ├── LocalAnalytics.kt            writes to event_log
│   └── Event.kt                     the sealed event catalogue
│
└── di/                              Hilt modules (see 00-overview.md section 4)
```

## 2. Feature package convention

Each feature package contains exactly:

```
feature/today/
├── TodayScreen.kt          @Composable, stateless, takes UiState + (UiEvent) -> Unit
├── TodayRoute.kt           @Composable, hoists the ViewModel, wires navigation lambdas
├── TodayViewModel.kt
├── TodayUiState.kt         data class + UiEvent sealed interface + Effect sealed interface
└── component/              composables used only by this screen
```

`XRoute` is the only place a feature touches `hiltViewModel()` and navigation callbacks. `XScreen` is pure and previewable. This split is what makes every screen UI-testable without Hilt.

## 3. Boundary rules

| Rule | Enforced by |
| --- | --- |
| `domain` has no `android.*`, `androidx.*`, `javax.inject` beyond `@Inject`, Room or Compose imports | unit test scanning source files (`ArchitectureTest`) |
| `feature` never imports `com.wivernz.itera.data` | `ArchitectureTest` |
| `data` never imports `com.wivernz.itera.feature` | `ArchitectureTest` |
| A DAO never returns a domain type | code review + `ArchitectureTest` on return-type packages |
| No hex colour literal outside `core/designsystem/theme/Color.kt` | `ArchitectureTest` regex over sources |
| No hard-coded user-facing string in a composable | lint `HardcodedText` raised to `error` |
| No `LocalDate.now()` / `Instant.now()` / `System.currentTimeMillis()` outside `core/common/Clock.kt` | `ArchitectureTest` regex |

`ArchitectureTest` is a plain JVM unit test that walks `src/main/java` and asserts these with readable failure messages. It is cheap, needs no extra dependency, and runs on every build.

## 4. Naming

- Screens: `<Feature>Screen`, `<Feature>Route`, `<Feature>ViewModel`, `<Feature>UiState`, `<Feature>UiEvent`, `<Feature>Effect`.
- Use cases: `<Verb><Noun>UseCase` with `operator fun invoke`.
- Entities: `<Noun>Entity`. DAOs: `<Noun>Dao`. DTOs: `<Noun>Dto`.
- Mappers: extension functions `fun XEntity.toDomain(): X` / `fun X.toEntity(): XEntity` in `data/mapper`.
- Test files mirror the source path exactly.

## 5. When to split into Gradle modules

Do it when **any two** of these become true; not before:

1. A clean build exceeds 90 s on the reference machine.
2. More than one team or agent regularly edits the codebase concurrently.
3. A second app target appears (Wear, a widget-only process, an instant app).
4. `feature` exceeds roughly 15 000 lines.

The split, when it comes, is: `:core:designsystem`, `:core:data`, `:core:domain`, `:feature:*`, `:app`. The package tree above already matches that shape, so the move is file relocation plus `build.gradle.kts` files - no code changes.
