# 003 - Design system & app shell

**Depends on** 002 | **Blocks** 005, 006, 007, 008

## Goal

Port the prototype's theme, tokens, fonts, icons and component library, and build the navigation graph and four-tab shell, so every screen milestone only composes existing pieces.

## Included scope

- **Tokens and theme** (old 003): colours (light/dark), 11 type styles, script-aware font selection (ADR-0019), 31 `IteraIcons` (complete prototype set; older count corrected below), motion specs, `IteraTheme`, `NightSurface`, `LocalReduceMotion`, token gallery preview.
- **Components** (old 004): every shared component in the prototype's `Components.kt`, including `LanguageSheet`, `LanguagePill`, `RadioDot`, `TimePickerSheet` (real M3 pickers, D-17), with previews.
- **Navigation and shell** (old 015): one `NavHost` and one `NavHostController` with a flat list of type-safe routes (ADR-0008, D-02), start destination from `onboardingCompleted`, bottom bar shown only on the four tab routes (Library, History and Technique detail are full-screen), tab switching via `switchTab` with `saveState`/`restoreState` anchored on Today, `backToToday()`, theme preference applied live, edge-to-edge, deep-link extra handling. Tabs and routes render placeholders until their milestones land.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/003-design-tokens-and-theme.md`, `004-design-system-components.md`, `015-navigation-and-shell.md`.
- `docs/ux/04-design-system.md`, `01-navigation-graph.md`, `05-prototype-reference.md`; `docs/architecture/03-compose-conventions.md`, `04-navigation-architecture.md`; ADR-0008, 0018, 0019. Navigation follows ADR-0008, `docs/ux/01-navigation-graph.md` and the prototype; where `04-navigation-architecture.md` or the detailed 015 describe two controllers, nested tab graphs or per-tab back stacks, that design is superseded and must not be built.
- Prototype: `design/app/src/main/java/com/itera/app/ui/theme/` (`Color.kt`, `Type.kt`, `Theme.kt`), `ui/components/Components.kt`, `ui/components/IteraIcons.kt`, `ui/IteraApp.kt`.

## Key dependencies

- Needs 002 for DataStore (`onboardingCompleted`, theme preference) and `AppLanguage`. This removes the old parallel track {tokens, components} alongside architecture; for a solo developer that costs nothing.
- Deep links are accepted here; notifications that send them arrive in 009.

## Acceptance criteria

- [ ] Every token in `docs/ux/04-design-system.md` exists with the prototype's exact values and names; no hex literal outside `Color.kt`.
- [ ] Russian resolves to Inter Tight / Inter; en, de, es to the brand faces; user-authored text styles use Inter in every locale.
- [ ] Each component matches its prototype original and has light/dark previews (plus `fontScale = 2f` where text wraps); with reduce-motion on, nothing animates.
- [ ] No component takes a ViewModel, `NavController` or `Context`.
- [ ] Start destination follows `onboardingCompleted`; four tabs in order Today, Train, Progress, You; switching tabs restores each tab's saved state and never grows the back stack; system back from any tab lands on Today; re-tapping the active tab is a no-op.
- [ ] The bar shows only on the four tab routes; `AppNavHost` is the only holder of a `NavController`; finishing onboarding cannot be undone with back; a deep-link extra navigates once.
- [ ] The component gallery and shell were compared against the prototype in light and dark; only deviations are recorded.

## Required tests

`ColorTokenTest`, `TypeTokenTest`, `MotionTest`, `ThemeTest`, `NightSurfaceTest`, `FontCoverageTest`, `ComponentTest`, `NavigationTest`, `RouteSerializationTest`.

## Implementation notes

- Ports the prototype's complete 31-vector / 24-route enumeration; older counts were inaccurate. See the milestone 003 reconciliation in `docs/00-source-of-truth.md`.
- Feature destinations intentionally remain placeholders. One flat typed graph; DataStore drives initial routing and live theme; SavedStateHandle retains a pending deep link until onboarding permits navigation.
- Components take plain data/callbacks, including the language selector. Its owning feature applies AppLanguage; no settings/onboarding feature implementation is introduced here.
- The repository's last-defaulted-parameter convention conflicts with Compose's `ModifierParameter` preference. Only that lint rule is suppressed on the three affected component files.
- Debug-only gallery activities provide synthetic fixtures in both apps and are excluded from release.
