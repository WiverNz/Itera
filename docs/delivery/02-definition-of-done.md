# Definition of done

Applies to every issue. An issue that does not meet all applicable items is not done, regardless of whether the feature appears to work.

## 1. Function

- [ ] Every acceptance criterion in the issue is satisfied and demonstrated.
- [ ] Every item in the issue's manual verification steps has been executed on a device or emulator.
- [ ] Behaviour matches the referenced spec documents exactly; any deviation is either fixed or recorded as a documentation change (section 6).
- [ ] The feature works with no network, with notifications denied, and after a process kill.

## 2. Architecture

- [ ] Code sits in the package the issue names, following `docs/architecture/01-package-structure.md`.
- [ ] The screen follows the `UiState` / `UiEvent` / `Effect` template (`02-state-management.md`) without variation.
- [ ] `domain` gained no Android, Room or Compose import.
- [ ] No `NavController` was passed into a screen or component.
- [ ] No business logic entered a composable.
- [ ] No ambient time (`LocalDate.now()`, `Instant.now()`, `System.currentTimeMillis()`) outside the clock module.
- [ ] `ArchitectureTest` passes.

## 3. Design and prototype parity

- [ ] The screen was run **side by side** against its composable in `design/`, on one device, in light and dark - not compared from memory or from a Markdown table.
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Every deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed - in which case **`design/` was updated in the same change**.
- [ ] The comparison is recorded in the issue: screen, device, themes, deviations and their reasons.
- [ ] No hex colour literal outside `core/designsystem/theme/Color.kt`.
- [ ] Only tokens from `docs/ux/04-design-system.md` are used.
- [ ] The three night-surface screens stay dark in light theme.
- [ ] Previews exist for the screen and for any new reusable component, in both themes, plus a `fontScale = 2f` preview for dense screens.
- [ ] Motion matches the prototype's specs exactly, and is fully removed when animations are off.

## 4. States

- [ ] Loading, empty, error and first-run states are implemented per `docs/ux/03-ux-states.md`, or the issue states explicitly that one does not apply.
- [ ] A failure degrades the smallest possible region - a row, not a screen; a screen, not the app.
- [ ] No error message exposes an exception, a code, or the word "Error".
- [ ] Destructive actions are confirmed and name what is lost.

## 5. Tests

- [ ] Every test listed in the issue's test expectations exists and passes.
- [ ] Every new domain rule has a unit test, including its boundary values.
- [ ] Every new ViewModel has a test covering initial state, each event, and the error path.
- [ ] Every new screen has at least one Compose UI test for its content state.
- [ ] No test is `@Ignore`d, and no test was made to pass by loosening an assertion.
- [ ] `domain` coverage remains at or above 85 %.

## 6. Data and content

- [ ] Any schema change ships a migration, a migration test, and the updated exported schema JSON.
- [ ] Any catalog change bumps `contentVersion` in both asset files and passes `CatalogValidationTest` and `CatalogCompatibilityTest`.
- [ ] Any new table is assigned to a reset tier (ADR-0014) and `ResetCoverageTest` passes.
- [ ] No user-authored English string was written into the database; copy keys and arguments were used.

## 7. Accessibility

- [ ] Every interactive element is at least 44 x 44 dp.
- [ ] Every icon-only control has a content description; every decorative icon has `contentDescription = null`.
- [ ] Composite rows merge into one sensible announcement.
- [ ] Correct `Role` and toggle/select semantics are set.
- [ ] The screen is usable and scrollable at `fontScale 2.0`.
- [ ] No meaning is carried by colour alone.
- [ ] TalkBack was actually run on the new screen, not assumed.

## 8. Localisation

- [ ] Every user-facing string is in `strings.xml` with the documented key prefix.
- [ ] **Every new string exists in all four languages** - `values/`, `values-ru/`, `values-de/`, `values-es/`. `MissingTranslation` passes as an error.
- [ ] Plurals use `plurals.xml` with the categories each language requires; no concatenation; positional format arguments only.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.
- [ ] Layout survives `en-XA` without clipping and `en-XB` without a mirroring break.
- [ ] Dates, times, months and first-day-of-week use locale-aware formatters.
- [ ] No user-authored text is transformed by a language change.

## 9. Privacy and logging

- [ ] No user-authored text is passed to the `Logger`, at any level.
- [ ] No new permission was added without an ADR.
- [ ] No network call, no new dependency that can make one.
- [ ] Any new analytics event follows `docs/analytics/00-event-model.md` and carries no user text.

## 10. Hygiene

- [ ] No `TODO`, `FIXME`, commented-out code, or debug logging left in `main`.
- [ ] Lint passes with no new warnings; `HardcodedText` and `MissingTranslation` are errors.
- [ ] Formatting passes (`spotlessCheck` / `ktlintCheck`).
- [ ] No crash in the implemented flow during 10 minutes of exploratory use.
- [ ] The app still builds and launches from a clean checkout.

## 11. Documentation

- [ ] If the implementation changed a documented decision, `docs/00-source-of-truth.md` and the affected document were updated **in the same change**.
- [ ] If the implementation changed anything visual, `design/` was updated to match, in the same change (ADR-0018).
- [ ] If the implementation changed anything visual, `design/` was updated to match, in the same change (ADR-0018).
- [ ] If a new decision was needed, an ADR was added.
- [ ] The issue file itself is updated if its scope changed during implementation.

## 12. Review

- [ ] A reviewer (human or a separate agent) confirmed sections 1-11 rather than the author self-certifying.
- [ ] The diff contains nothing outside the issue's stated scope.
