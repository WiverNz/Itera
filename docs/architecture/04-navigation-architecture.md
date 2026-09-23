# Navigation wiring

The route graph itself is specified in `docs/ux/01-navigation-graph.md`. This document covers how it is wired in code.

## 1. `AppNavHost`

`core/navigation/AppNavHost.kt` is the only file that knows about both routes and features.

```kotlin
@Composable
fun AppNavHost(
    startDestination: Any,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController, startDestination) {

        navigation<OnboardingGraph>(startDestination = OnboardingWelcome) {
            composable<OnboardingWelcome> {
                WelcomeRoute(onStart = { navController.navigate(OnboardingGoals) })
            }
            // ...
            composable<OnboardingFirstWeek> {
                FirstWeekRoute(onFinish = {
                    navController.navigate(MainGraph) {
                        popUpTo(OnboardingGraph) { inclusive = true }
                    }
                })
            }
        }

        composable<MainGraph> { MainShell(rootNavController = navController) }

        composable<ExerciseIntro> { entry ->
            val args = entry.toRoute<ExerciseIntro>()
            ExerciseIntroRoute(
                activityId = args.activityId,
                onClose = navController::popBackStack,
                onStart = { navController.navigate(ExerciseRun(args.activityId)) }
            )
        }
        // ... the remaining full-screen routes
    }
}
```

## 2. `MainShell`

`MainShell` owns the `Scaffold`, the bottom bar and an **inner** `NavHostController` for the four tabs. Lambdas that leave the shell (opening an exercise, a review, the timer) are passed the **root** controller.

```kotlin
@Composable
fun MainShell(rootNavController: NavHostController) {
    val tabNav = rememberNavController()
    val backStack by tabNav.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = { IteraBottomBar(current = backStack?.destination, onSelect = { tabNav.switchTab(it) }) }
    ) { padding ->
        NavHost(tabNav, startDestination = TodayGraph, modifier = Modifier.padding(padding)) {
            navigation<TodayGraph>(startDestination = TodayHome) {
                composable<TodayHome> {
                    TodayRoute(
                        onOpenExercise = { rootNavController.navigate(ExerciseIntro(it)) },
                        onOpenFocus = { rootNavController.navigate(FocusSession(it)) },
                        onOpenReview = { rootNavController.navigate(ReviewRun(it)) },
                        onOpenReflection = { rootNavController.navigate(ReflectionRun(it)) }
                    )
                }
            }
            // TrainGraph, ProgressGraph, YouGraph
        }
    }
}
```

Two controllers is deliberate: tab state is saved and restored inside the shell, while full-screen routes replace the whole shell rather than appearing inside a padded content area.

## 3. `switchTab`

```kotlin
fun NavHostController.switchTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

Re-tapping the active tab pops to that tab's root instead:

```kotlin
if (currentTabRoot == route) popBackStack(route, inclusive = false) else switchTab(route)
```

## 4. Result routes

`ExerciseResult` must not be re-enterable by back. It is navigated to with:

```kotlin
navController.navigate(ExerciseResult(id)) {
    popUpTo(ExerciseIntro(id)) { inclusive = true }
}
```

so the back stack after completion is `MainGraph -> ExerciseResult`, and its "Done" action pops back to the shell.

## 5. Deep links

Each notification builds its stack explicitly rather than relying on `TaskStackBuilder` inference:

```kotlin
val pending = NavDeepLinkBuilder(context)
    .setGraph(R.navigation.placeholder)   // not used; see note
    .createPendingIntent()
```

Because the graph is Compose-defined, deep links are handled by passing an `Intent` extra (`itera.deeplink` = a serialised route) to `MainActivity`, which reads it in `onNewIntent` and calls `navController.navigate(route)` after the shell is composed. A `pendingDeepLink` state in the activity holds the route until the `NavHost` is ready, then clears it so a rotation does not re-navigate.

## 6. Back handling

- Full-screen routes with unsaved work (`ExerciseRun`, `ExerciseCombination`, `FocusSession`) install a `BackHandler` in their **Route** composable that shows a confirmation.
- `ExerciseResult` installs a `BackHandler` that performs the same action as "Done".
- Nowhere else overrides back.

## 7. Testing

`AppNavHost` is testable without Hilt by passing fake `Route` composables. The navigation tests in `docs/testing/01-test-matrix.md` drive the real graph with fake repositories and assert on the `testTag` of the screen root (`docs/ux/01-navigation-graph.md` section 9).
