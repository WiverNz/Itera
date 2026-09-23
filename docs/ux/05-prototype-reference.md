# Using the prototype

`design/` is a running Kotlin + Jetpack Compose Android app. It is the primary implementation reference for every UI issue (`docs/00-source-of-truth.md` section 1).

## 1. Run it first

```
Android Studio -> File > Open -> design/
```

AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, compileSdk 35, minSdk 26. It builds and runs on its own; it does not depend on the production module.

Two things make exploring it fast:

- **"Explore with demo data"** on Welcome, or **You -> Load demo data**, jumps to Day 9 with nine days of history (including a deliberate rest day), so every screen has content.
- The **language pill** on Welcome and **You -> Language & region** switch between English, Russian, German and Spanish at runtime.

Walk the whole loop once before writing any UI code: onboarding -> exercise -> result -> focus -> reflection -> day complete -> next day, then all four tabs in both themes and at least two languages.

## 2. Screen to file

| Screen | Prototype file | Composable | Issue |
| --- | --- | --- | --- |
| Theme, colours, type | `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` | `IteraTheme`, `IteraColors`, `IteraType` | 003 |
| Shared components | `ui/components/Components.kt` | 15 composables | 004 |
| Icons | `ui/components/IteraIcons.kt` | 33 `ImageVector`s | 003 |
| Navigation, bottom bar | `ui/IteraApp.kt` | `IteraApp`, `Routes`, `BottomBar`, `switchTab`, `backToToday` | 015 |
| Welcome | `ui/screens/Onboarding.kt` | `WelcomeScreen`, `RisingPath` | 016 |
| Goals | `ui/screens/Onboarding.kt` | `GoalsScreen`, `OnboardingHeader` | 016 |
| Rhythm | `ui/screens/Onboarding.kt` | `RhythmScreen`, `TimeRow` | 016 |
| First week | `ui/screens/Onboarding.kt` | `FirstWeekScreen` | 016 |
| Language picker | `ui/screens/Onboarding.kt` | `LanguageSheet`, `LanguagePill`, `AppLanguage`, `RadioDot` | 041 |
| Today | `ui/screens/Today.kt` | `TodayScreen`, `HeroCard` | 017 |
| Exercise intro | `ui/screens/Exercise.kt` | `ExerciseIntroScreen` | 018 |
| 2-minute rule | `ui/screens/Exercise.kt` | `TwoMinuteScreen`, `CheckCircle` | 018 |
| Exercise result | `ui/screens/Exercise.kt` | `ExerciseResultScreen`, `AnimatedCheck` | 018 |
| Focus timer | `ui/screens/Focus.kt` | `FocusScreen`, `TextControl` | 021 |
| Evening reflection | `ui/screens/Reflection.kt` | `ReflectionScreen` | 019 |
| Day complete | `ui/screens/Reflection.kt` | `DayCompleteScreen`, `DayRing` | 020 |
| Combination day | `ui/screens/Practice.kt` | `CombinationScreen`, `ChainStep` | 027 |
| Eisenhower | `ui/screens/Practice.kt` | `EisenhowerScreen`, `QuadrantBox`, `AxisLabel`, `SideLabel` | 022 |
| Feynman - explain | `ui/screens/Practice.kt` | `FeynmanScreen` | 023 |
| Feynman - reflect | `ui/screens/Practice.kt` | `FeynmanFeedbackScreen`, `FeedbackRow` | 023 |
| Review | `ui/screens/Practice.kt` | `ReviewScreen`, `IntervalLadder` | 026 |
| Premortem | `ui/screens/Practice.kt` | `PremortemScreen` | 024 |
| Habit stacking | `ui/screens/Practice.kt` | `HabitStackScreen` | 025 |
| Train | `ui/screens/Train.kt` | `TrainScreen`, `LinkRow` | 028 |
| Library | `ui/screens/Train.kt` | `LibraryScreen`, `MasteryDots` | 029 |
| Technique detail | `ui/screens/Train.kt` | `TechniqueDetailScreen` | 030 |
| Progress | `ui/screens/Progress.kt` | `ProgressScreen` | 031 |
| History | `ui/screens/Progress.kt` | `HistoryScreen` | 032 |
| Settings | `ui/screens/Profile.kt` | `ProfileScreen`, `Group`, `ValueRow`, `SwitchRow` | 034 |
| Strings, all 4 locales | `res/values*/strings.xml` | 362 strings + 4 plurals each | 008, 041 |
| Technique catalogue | `model/Model.kt` | `Technique`, `Skill`, `Program`, `Mastery` | 008 |

## 3. How to port a screen

1. **Run the prototype screen.** Interact with it. Note what changes on tap, what animates, what is disabled and when.
2. **Read its composable end to end**, including the private helpers below it.
3. **Reproduce the layout tree** - the same containers, in the same order, with the same `Arrangement`, `Alignment`, padding and size values.
4. **Reuse the ported components** (`IteraButton`, `IteraCard`, `ScreenColumn`, `StepRow`, ...) rather than re-inlining their contents.
5. **Replace the data source.** The prototype reads `AppViewModel` directly; production reads a `UiState` produced by a real ViewModel over repositories. The composable signature changes; the layout does not.
6. **Add what the prototype omits** - loading, empty and error states, accessibility semantics, and the product rules listed in `docs/00-source-of-truth.md` section 6.
7. **Compare side by side** on one device, in light and dark, before opening the change for review.

## 4. What to change when porting

| Prototype | Production |
| --- | --- |
| `com.itera.app` | `com.wivernz.itera` |
| `AppViewModel` passed into composables | `UiState` + `(UiEvent) -> Unit` (`docs/architecture/02-state-management.md`) |
| `LocalDate.now()` / `LocalTime.now()` inline | injected `Clock` (ADR-0011) |
| `remember { mutableStateListOf(...) }` holding sample data | repository-backed state, with drafts persisted |
| `R.string.eis_task_1` style sample content | user-entered content |
| String route constants | type-safe `@Serializable` route classes (ADR-0008) - same route set, same behaviour |
| No loading / empty / error states | per `docs/ux/03-ux-states.md` |
| `rememberSaveable` countdown in `FocusScreen` | wall-clock `endsAt` + foreground service (ADR-0009) |

## 5. What must not change

Everything in `docs/00-source-of-truth.md` section 1, row 2. Concretely: if your ported screen differs from the prototype in spacing, size, radius, colour, type style, ordering, wording, placement, or in what happens when something is tapped, that is a defect - unless a product rule in section 6 required it, in which case say so in the change description.

## 6. Known prototype limitations

Do not copy these; they are scaffolding, not design.

| Limitation | What production does |
| --- | --- |
| Time pickers step by 30 minutes per tap | A Material 3 `TimePicker` in a bottom sheet |
| `Change topic` on Feynman does nothing | Opens the learning-topic picker |
| Review screen is static sample content | Real review item, hidden previous answer, grading |
| Eisenhower starts with 4 of 7 tasks pre-placed | Starts from the user's own entered tasks |
| Premortem reasons are hard-coded | User-entered, reorderable |
| `Export journal` and `Privacy` rows do nothing | Implemented (issues 034, 035) |
| No notification permission request | Requested in onboarding (issue 033) |
| No reset actions | Two-tier reset (issue 034) |
| `practicedOn` recomputes by scanning the whole log | Indexed queries (issue 013) |
| The AI coach box contains sample feedback text | Box kept, sample rows replaced by one explanatory line (D-10) |

## 7. Keeping the prototype honest

The prototype is a reference, not a dependency: production code never imports from it, and it is excluded from the production Gradle build (`settings.gradle.kts` includes only `:app`).

If a design change is agreed during implementation, change the prototype too, in the same change, so it never becomes stale. A prototype that no longer matches the app is worse than no prototype.
