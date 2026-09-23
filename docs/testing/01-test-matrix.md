# Test matrix

Per-area coverage. Each row names the subject, the test type, the file, and the issue that must deliver it.

Legend: **U** unit (JVM), **R** Robolectric, **C** Compose UI, **I** instrumented integration.

## Domain engines

| Subject | Type | Test | Issue |
| --- | --- | --- | --- |
| All 42 state-machine transitions | U | `ActivityStateMachineTest` | 009 |
| Availability rules per day part | U | `ActivityStateMachineTest` | 009 |
| Day rollover: expiry, abandoned vs complete | U | `DayRolloverTest` | 009 |
| Plan: Day 1 fixture exact match | U | `GenerateDailyPlanUseCaseTest` | 011 |
| Plan: Day 2 practice-prompt rule | U | same | 011 |
| Plan: Day 9 review displaces the optional | U | same | 011 |
| Plan: Day 14 combination | U | same | 011 |
| Plan: idempotence, determinism x100 | U | same | 011 |
| Plan: review cap per pace | U | same | 011 |
| Plan: never emits a locked technique (1 000 days) | U | same | 011 |
| Plan: Day 15+ generation, all paces | U | same | 011 |
| Unlock set for days 1..20 | U | `UnlockRulesTest` | 010 |
| Unlock never reverses on an abandoned day | U | same | 010 |
| Review ladder: each grade from each stage | U | `ReviewSchedulerTest` | 012 |
| Review: retirement at stage 4 + SOLID | U | same | 012 |
| Review: 90-day overdue is due once | U | same | 012 |
| Review: due ordering with mixed overdue | U | same | 012 |
| Mastery truth table incl. 14-day boundary | U | `MasteryTest` | 013 |
| Mastery: INTEGRATED overrides | U | same | 013 |
| Skill level: every artboard row | U | `SkillLevelTest` | 013 |
| Skill level: every threshold, both sides | U | same | 013 |
| Progress window across month end, DST, empty DB | U | `ProgressSummaryTest` | 013 |
| Completion effects 1-7 | U | `CompleteActivityUseCaseTest` | 014 |
| Day completion idempotence | U | same | 014 |
| Program advance exactly once | U | `AdvanceProgramDayUseCaseTest` | 014 |
| Focus timer remaining, pause, extend, restore | U | `FocusTimerStateTest` | 021 |
| Focus timer: backward clock jump | U | same | 021 |
| Focus timer: <60 s end records nothing | U | same | 021 |

## Data

| Subject | Type | Test | Issue |
| --- | --- | --- | --- |
| Entity / domain round-trip, all aggregates | U | `MapperTest` | 006 |
| Corrupt `resultPayload` yields `result = null` and logs | U | `ResultPayloadTest` | 006 |
| Every `ActivityResult` subtype round-trips | U | same | 006 |
| Copy resolution from `copyKey` + args | R | `CopyResolverTest` | 006 |
| Preferences: defaults, round-trip, unknown enum, IOException | R | `DataStorePreferencesRepositoryTest` | 007 |
| Catalog: all eight validations | U | `CatalogValidationTest` | 008 |
| Catalog: forbidden change detection | U | `CatalogCompatibilityTest` | 008 |
| Content reconciler idempotence and no re-lock | U | `ContentReconcilerTest` | 008 |
| Room schema v1 opens and seeds | I | `DatabaseCreationTest` | 006 |
| Migration per version step | I | `MigrationTest` | (each schema change) |
| Journal export golden file | U | `MarkdownJournalWriterTest` | 035 |
| Journal export: empty DB, special characters | U | same | 035 |

## ViewModels

One per screen. Each covers: initial state, every event, the error path, the empty/first-run path.

| ViewModel | Test | Issue |
| --- | --- | --- |
| `OnboardingViewModel` | `OnboardingViewModelTest` | 016 |
| `TodayViewModel` | `TodayViewModelTest` | 017 |
| `ExerciseRunnerViewModel` | `ExerciseRunnerViewModelTest` | 018 |
| `FocusViewModel` | `FocusViewModelTest` | 021 |
| `EisenhowerViewModel` | `EisenhowerViewModelTest` | 022 |
| `FeynmanViewModel` | `FeynmanViewModelTest` | 023 |
| `PremortemViewModel` | `PremortemViewModelTest` | 024 |
| `HabitStackViewModel` | `HabitStackViewModelTest` | 025 |
| `ReviewViewModel` | `ReviewViewModelTest` | 026 |
| `CombinationViewModel` | `CombinationViewModelTest` | 027 |
| `ReflectionViewModel` | `ReflectionViewModelTest` | 019 |
| `DayCompleteViewModel` | `DayCompleteViewModelTest` | 020 |
| `TrainViewModel` | `TrainViewModelTest` | 028 |
| `LibraryViewModel` | `LibraryViewModelTest` | 029 |
| `TechniqueDetailViewModel` | `TechniqueDetailViewModelTest` | 030 |
| `ProgressViewModel` | `ProgressViewModelTest` | 031 |
| `HistoryViewModel` | `HistoryViewModelTest` | 032 |
| `YouViewModel` | `YouViewModelTest` | 034 |

## Compose UI

| Subject | Type | Test | Issue |
| --- | --- | --- | --- |
| Every design-system component, light + dark | C | `ComponentScreenshotTest` (assertions, not screenshots) | 004 |
| Onboarding: 3 steps, max-2 selection, continue with zero | C | `OnboardingScreenTest` | 016 |
| Today: hero per source, checklist states, counter | C | `TodayScreenTest` | 017 |
| Today: practice-prompt tap completes in place with undo | C | same | 017 |
| Today: day-complete state | C | same | 020 |
| Exercise: intro -> run -> result chrome | C | `ExerciseRunnerScreenTest` | 018 |
| Template: each block type renders and edits | C | `TemplateBodyTest` | 018 |
| Focus: controls, ring, end confirmation | C | `FocusScreenTest` | 021 |
| Eisenhower: select then place, auto-advance, all-sorted | C | `EisenhowerScreenTest` | 022 |
| Feynman: word-count gate, step 2, no AI panel | C | `FeynmanScreenTest` | 023 |
| Review: previous answer absent before reveal | C | `ReviewScreenTest` | 026 |
| Reflection: three steps, skip tonight | C | `ReflectionScreenTest` | 019 |
| Library: filter, locked rendering, sort order | C | `LibraryScreenTest` | 029 |
| Technique detail: locked vs unlocked | C | `TechniqueDetailScreenTest` | 030 |
| Progress: no streak/points/percentage strings | C | `ProgressHonestyTest` | 031 |
| History: month bounds, rest-day row | C | `HistoryScreenTest` | 032 |
| You: every toggle writes, reset dialogs | C | `YouScreenTest` | 034 |
| Tab switching keeps back stacks; re-tap pops to root | C | `NavigationTest` | 015 |
| Full-screen routes hide the bottom bar | C | same | 015 |

## Integration

| Subject | Type | Test | Issue |
| --- | --- | --- | --- |
| Onboarding -> Day 1 plan exists | I | `FirstRunIntegrationTest` | 016 |
| Complete exercise -> mastery MET -> row filled | I | `DailyLoopIntegrationTest` | 018 |
| Complete all -> day complete -> program advances | I | same | 014 |
| Five-day gap -> no advance, same curriculum day | I | `GapIntegrationTest` | 014 |
| Feynman Day 6 -> review due Day 7 | I | `ReviewIntegrationTest` | 026 |
| Combination day -> INTEGRATED for each technique | I | `CombinationIntegrationTest` | 027 |
| Reset program: what is deleted and kept | I | `ResetIntegrationTest` | 034 |
| Erase everything: returns to first run | I | same | 034 |
| `DailyPlanWorker` twice -> one row | I | `WorkerIdempotencyTest` | 033 |
| Each suppression rule posts nothing | I | `NotificationSuppressionTest` | 033 |
| Reminder cancelled on completion | I | same | 033 |
| Focus timer survives process kill | I | `FocusTimerServiceTest` | 021 |
| Deep link from each notification | I | `DeepLinkTest` | 033 |

## Localisation

| Subject | Type | Test | Issue |
| --- | --- | --- | --- |
Ownership follows D-15: each test belongs to the issue that builds the thing it covers. Issue 041 audits that they exist and pass.

| Subject | Type | Test | Issue |
| --- | --- | --- | --- |
| `AppLanguage.set` / `current` round-trip for each tag | R | `AppLanguageTest` | 002 |
| Date, time, month, weekday and first-day-of-week per locale | U | `LocaleFormattingTest` | 002 |
| Russian standalone month names (`LLLL`) | U | same | 002 |
| 12/24-hour rendering follows the system preference | U | same | 002 |
| Per-language glyph coverage of the bundled fonts | U | `FontCoverageTest` | 003 |
| Cyrillic locale resolves to Inter / Inter Tight | U | same | 003 |
| User-authored text styles resolve to Inter in every locale | U | same | 003 |
| `LanguageSheet` lists, names and applies each language | C | `LanguageSheetTest` | 004 |
| Technique content present in all four locales | U | `CatalogValidationTest` | 008 |
| Language change re-renders immediately, from both entry points | C | `LanguageSwitchTest` | 016, 034 |
| Language persists across an activity recreate | C | same | 034 |
| Every key exists in all four locales | U | `TranslationCompletenessTest` | 041 |
| Plural categories per language (ru: one/few/many/other) | U | `PluralCategoryTest` | 041 |
| No non-positional format argument | U | `NoConcatenationTest` | 041 |
| Every screen renders in en/ru/de/es with no truncation or missing glyph | C | `LocalizedRenderTest` | 041 |
| History re-renders correctly after a language change | I | `LanguageHistoryTest` | 041 |

## Visual fidelity

| Subject | Type | Test | Issue |
| --- | --- | --- | --- |
| Every screen matches its golden, light and dark | Golden | `ScreenGoldenTest` | 039 |
| Every screen matches its golden at `fontScale 2.0` | Golden | same | 039 |
| Every screen matches its Russian golden (light, default scale) | Golden | same | 039 |
| Every design-system component matches its golden | Golden | `ComponentGoldenTest` | 039 |
| Per-screen element presence and order | C | each screen's test | per screen |
| Side-by-side comparison against `design/` | Manual | recorded in the issue | per screen |

## Cross-cutting guarantees

| Subject | Type | Test | Issue |
| --- | --- | --- | --- |
| Layer boundary rules (7) | U | `ArchitectureTest` | 002 |
| No hex literal outside the theme | U | same | 003 |
| No ambient time outside the clock | U | same | 002 |
| No streak/points/percentage language | U | `NoStreakLanguageTest` | 031 |
| No user text reaches the logger | U | `NoUserTextLoggedTest` | 036 |
| Every table assigned a reset tier | U | `ResetCoverageTest` | 034 |
| No `INTERNET` permission in the merged manifest | U | `OfflineTest` | 001 |
| Every screen at `fontScale 2.0` | C | `FontScaleTest` | 037 |
| Every screen has a content description for each control | C | `AccessibilityAuditTest` | 037 |
| Pseudo-locale `en-XA` / `en-XB` render | C | `PseudoLocaleTest` | 041 |

## Coverage gate

`domain` at 85 % lines, enforced in CI (issue 039). No gate elsewhere.
