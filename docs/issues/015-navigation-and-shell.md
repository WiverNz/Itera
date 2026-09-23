# 015 - Navigation graph and app shell

**Phase** 3 - Shell and core loop | **Depends on** 004, 007 | **Blocks** 016, 017, 018, 028, 029, 031, 032

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/IteraApp.kt`** - `Routes`, `IteraApp`, `BottomBar`, `switchTab`, `backToToday`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the single-activity shell: the route graph, the four-tab bottom bar, theme wiring, start-destination resolution and deep-link plumbing, per `docs/ux/01-navigation-graph.md` and `docs/architecture/04-navigation-architecture.md`.

## User value

The app has a spine. Four tabs that remember where you were, and full-screen exercises that take over completely.

## Scope

- `core/navigation/Routes.kt`: all 20 `@Serializable` route types from section 3.
- `core/navigation/AppNavHost.kt`: the root graph with `OnboardingGraph`, `MainGraph` and the nine full-screen routes.
- `MainShell`: the `Scaffold`, the inner tab `NavHostController`, and the four nested tab graphs.
- `IteraBottomBar`: 86 dp tall, 1 dp top border, four 72x52 dp items, active/inactive styling per `docs/ux/04-design-system.md` section 4 of the navigation spec, gesture-inset bottom padding.
- `switchTab` with `saveState`/`restoreState`, `launchSingleTop`, and pop-to-root on re-tap.
- Start-destination resolution from `onboardingCompleted`, with the splash held until it resolves.
- `IteraTheme` wired to the theme preference at the app root.
- Deep-link plumbing: `MainActivity.onNewIntent` reads an `itera.deeplink` extra, holds it in a `pendingDeepLink` state until the `NavHost` is composed, navigates once, then clears it.
- Placeholder `XRoute` composables for every destination, each rendering its name and carrying the route's `testTag`. Real screens replace them in later issues.
- `BackHandler` conventions: result routes behave as their primary action; nothing else overrides back.

## Non-goals

- Any real screen content.
- Notification creation (issue 033 sends the deep-link extras; this issue accepts them).
- Onboarding content (issue 016).

## Implementation notes

- **Two controllers**: the root one owns onboarding, the shell and the full-screen routes; the shell owns the four tabs. This is what makes full-screen routes cover the bar without a conditional (ADR-0008). Do not collapse them into one.
- No composable receives a `NavController`. `AppNavHost` and `MainShell` are the only files that hold one; every screen gets plain lambdas.
- The splash must be held only until the preference resolves - a few milliseconds. Do not hold it for plan generation.
- Result routes navigate with `popUpTo(ExerciseIntro(id)) { inclusive = true }` so back does not re-enter the exercise.
- The deep-link `pendingDeepLink` must be cleared after navigating, or a rotation re-navigates. Store it in the activity's ViewModel, not in a `remember`.
- Every placeholder route sets `Modifier.testTag(RouteName)` on its root so `NavigationTest` can be written now and keep passing as screens land.
- Re-tapping the active tab at its root is a no-op, not a recomposition or a re-navigation.

## Affected layers

`core/navigation`, `MainActivity`, `core/designsystem/theme` (theme wiring).

## Acceptance criteria

- [ ] All 20 routes are defined and type-safe; arguments are compile-time checked.
- [ ] A first launch with `onboardingCompleted = false` starts on Welcome; `true` starts on Today.
- [ ] The four tabs are reachable, in the order Today, Train, Progress, You.
- [ ] Each tab keeps its own back stack; switching away and back restores position and scroll.
- [ ] Re-tapping a tab pops it to its root; re-tapping at the root does nothing.
- [ ] All nine full-screen routes hide the bottom bar, with no layout jump.
- [ ] Finishing onboarding pops the onboarding graph so back cannot return to it.
- [ ] The theme preference drives `IteraTheme` immediately on change, with no restart.
- [ ] Edge-to-edge is enabled and status-bar icon appearance follows the effective surface.
- [ ] A deep-link extra navigates once, builds a back stack ending at Today, and does not re-navigate on rotation.
- [ ] Every route root carries its `testTag`.
- [ ] No composable outside `core/navigation` references `NavController`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

- `RouteSerializationTest` - every route type round-trips through the type-safe API with its arguments.

## UI test expectations

`NavigationTest`:
- start destination for both preference values;
- all four tabs reachable by tag;
- tab back stacks are independent (navigate deep in Train, switch to Progress, switch back, still deep);
- re-tap pops to root;
- full-screen routes render without the bottom bar;
- onboarding completion cannot be reached by back;
- system back from a deep-linked route lands on Today;
- rotation does not re-trigger a deep link.

## Manual verification

1. Walk all four tabs; switch away mid-scroll and back; position is kept.
2. Open a full-screen placeholder; the bar is gone with no flicker.
3. Change the theme preference; every screen updates immediately.
4. Send a deep-link intent via `adb`; it lands correctly and back goes to Today.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 3, 4, 5, 7, 10, 11.
