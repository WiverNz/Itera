# Navigation wiring

The route graph itself is specified in `docs/ux/01-navigation-graph.md`. This document covers how it is wired in code.

## 1. Shape

Per ADR-0008 and the prototype (`design/app/src/main/java/com/itera/app/ui/IteraApp.kt`):

- **One** `NavHost` and **one** `NavHostController`.
- A **flat** list of `@Serializable` routes. There are no nested graphs.
- The bottom bar is visible only when the current destination is one of the four tab routes: `Today`, `Train`, `Progress`, `You`.
- Every other route is full-screen with no bar, **including `Library`, `History` and `TechniqueDetail`**, which have their own back button.
- `AppNavHost` is the only component that owns a `NavController`. Screens and components receive lambdas.

## 2. `AppNavHost`

`core/navigation/AppNavHost.kt` is the only file that knows about both routes and features. It owns the `Scaffold`, the bottom bar and the single controller.

```kotlin
private val TabRoutes = listOf(Today, Train, Progress, You)

@Composable
fun AppNavHost(
    startDestination: Any,
    navController: NavHostController = rememberNavController()
) {
    val entry by navController.currentBackStackEntryAsState()
    val showBar = TabRoutes.any { entry?.destination?.hasRoute(it::class) == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) IteraBottomBar(
                current = entry?.destination,
                onSelect = { navController.switchTab(it) }
            )
        }
    ) { padding ->
        val barPadding = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(barPadding).consumeWindowInsets(barPadding)
        ) {
            // onboarding
            composable<OnboardingWelcome> { WelcomeRoute(onStart = { navController.navigate(OnboardingGoals) }) }
            // OnboardingGoals, OnboardingRhythm ...
            composable<OnboardingFirstWeek> {
                FirstWeekRoute(onStart = {
                    navController.navigate(Today) { popUpTo<OnboardingWelcome> { inclusive = true } }
                })
            }

            // tabs
            composable<Today> {
                TodayRoute(
                    onOpenExercise = { navController.navigate(ExerciseIntro(it)) },
                    onOpenFocus = { navController.navigate(FocusSession(it)) },
                    onOpenReview = { navController.navigate(ReviewRun(it)) },
                    onOpenReflection = { navController.navigate(ReflectionRun(it)) }
                )
            }
            composable<Train> {
                TrainRoute(
                    onToday = { navController.switchTab(Today) },
                    onReview = { navController.navigate(ReviewRun(it)) },
                    onLibrary = { navController.navigate(Library) },
                    onTechnique = { navController.navigate(TechniqueDetail(it)) }
                )
            }
            composable<Progress> {
                ProgressRoute(
                    onHistory = { navController.navigate(History) },
                    onLibrary = { navController.navigate(Library) }
                )
            }
            composable<You> { YouRoute() }

            // full-screen, no bar
            composable<Library> { LibraryRoute(onBack = navController::popBackStack /* ... */) }
            composable<History> { HistoryRoute(onBack = navController::popBackStack) }
            composable<TechniqueDetail> { TechniqueDetailRoute(onBack = navController::popBackStack /* ... */) }
            composable<ExerciseIntro> { entry ->
                val args = entry.toRoute<ExerciseIntro>()
                ExerciseIntroRoute(
                    activityId = args.activityId,
                    onClose = navController::popBackStack,
                    onStart = { navController.navigate(ExerciseRun(args.activityId)) }
                )
            }
            // ... the remaining full-screen routes from docs/ux/01-navigation-graph.md section 2
        }
    }
}
```

The route set and arguments are those in `docs/ux/01-navigation-graph.md` section 2. The bar's presence is a function of the current route only; no screen controls it.

## 3. Tab switching

**Today is the navigation anchor.** Tab switching pops back to Today while saving state:

```kotlin
fun NavHostController.switchTab(route: Any) {
    navigate(route) {
        popUpTo<Today> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

- Each tab's state (scroll position, anything above it) is saved when leaving it and restored when returning, via `saveState` / `restoreState` on the single controller.
- The back stack never grows by switching tabs. System back from Train, Progress or You returns to Today, and back from Today exits.
- **Re-tapping the active tab is a no-op** (`launchSingleTop`).

Returning to Today from a flow clears everything above it:

```kotlin
fun NavHostController.backToToday() {
    navigate(Today) {
        popUpTo<Today> { inclusive = true }
        launchSingleTop = true
    }
}
```

It is used by the exercise result ("Done") and by day complete ("Good night").

## 4. Result routes

`ExerciseResult` must not be re-enterable by back. It is navigated to with:

```kotlin
navController.navigate(ExerciseResult(id)) {
    popUpTo(ExerciseIntro(id)) { inclusive = true }
}
```

so the back stack after completion is `Today -> ExerciseResult`, and its "Done" action calls `backToToday()`.

## 5. Deep links

Each notification builds its stack explicitly rather than relying on `TaskStackBuilder` inference:

```kotlin
val pending = NavDeepLinkBuilder(context)
    .setGraph(R.navigation.placeholder)   // not used; see note
    .createPendingIntent()
```

Because the graph is Compose-defined, deep links are handled by passing an `Intent` extra (`itera.deeplink` = a serialised route) to `MainActivity`, which reads it in `onNewIntent` and calls `navController.navigate(route)` after the `NavHost` is composed, building a synthetic stack ending at `Today`. A `pendingDeepLink` state in the activity holds the route until the `NavHost` is ready, then clears it so a rotation does not re-navigate.

## 6. Back handling

- Full-screen routes with unsaved work (`ExerciseRun`, `ExerciseCombination`, `FocusSession`) install a `BackHandler` in their **Route** composable that shows a confirmation.
- `ExerciseResult` installs a `BackHandler` that performs the same action as "Done".
- Nowhere else overrides back.

## 7. Testing

`AppNavHost` is testable without Hilt by passing fake `Route` composables. The navigation tests in `docs/testing/01-test-matrix.md` drive the real graph with fake repositories and assert on the `testTag` of the screen root (`docs/ux/01-navigation-graph.md` section 9).
