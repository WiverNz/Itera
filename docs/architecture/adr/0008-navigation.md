# ADR-0008: Single NavHost, flat routes, conditionally shown bottom bar

Status: accepted (2026-09-23; revises the 2026-09-22 version)

## Context

Four tabs, three secondary screens, fourteen flow screens, and notification deep links.

The original decision - taken from static artboards - used two `NavHostController`s and a nested graph per tab, with Library and History nested inside the Train and Progress tabs so they kept the bottom bar.

The `design/` prototype does something different and simpler, and it is the source of truth for navigation behaviour (D-02).

## Decision

Follow the prototype:

- **One** `NavHost` and one `NavHostController`.
- A **flat** route list; no nested graphs.
- The bottom bar is rendered only when the current route is one of the four tab routes: `showBar = tabs.any { it.route == currentRoute }`.
- **Library, History and Technique detail are full-screen with a back button and no bottom bar.**
- Tab switching: `popUpTo(TODAY) { saveState = true }`, `launchSingleTop = true`, `restoreState = true`. Today is the anchor, so the back stack never grows by tab switching and system back from any tab lands on Today.
- `backToToday()` = `navigate(TODAY) { popUpTo(TODAY) { inclusive = true }; launchSingleTop = true }`, used after an exercise result and after day complete.

One production change, which alters no behaviour: routes are declared as `@Serializable` classes rather than string constants, so arguments are compile-time checked. The route set, the argument names and every navigation option above are unchanged.

No composable receives a `NavController`; `AppNavHost` is the only file that holds one.

## Alternatives considered

- **Two controllers with a nested graph per tab** (the previous decision) - keeps the bar on Library and History, which the prototype does not do, and adds a second controller for no behaviour the product needs. Rejected.
- **Keeping string routes** - stringly-typed arguments and runtime crashes, for no benefit. Rejected.
- **A third-party navigator** - non-standard for an agent-implemented codebase. Rejected.

## Consequences

- Secondary screens get the full width and their own back affordance, which is what the prototype demonstrates and what the deeper content (a 14-row library, a calendar) wants.
- The `Scaffold` exists in exactly one place; the bar's presence is a function of the route, with no conditional-visibility logic inside screens.
- Deep links build a synthetic stack ending at Today.
- Tab state is saved and restored by `saveState` / `restoreState` on a single controller.

## Migration implications

None - nothing is implemented yet. The previously documented two-controller design is superseded and must not be built.
