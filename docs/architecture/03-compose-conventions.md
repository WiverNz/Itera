# Compose conventions

## 1. Composable shape

```kotlin
@Composable
fun ChecklistRow(
    item: ChecklistItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

- `modifier: Modifier = Modifier` is the **last** parameter with a default, and is applied to the outermost layout node, first in the chain.
- Required parameters before optional ones.
- Lambdas named `onX`, not `xListener`.
- Return `Unit`. A composable that returns a value is a mistake here.
- No default values that hide behaviour (no `enabled: Boolean = true` on something that is meaningless when disabled).

## 2. Statelessness

- Screen composables take `UiState` + `(UiEvent) -> Unit`. Nothing else.
- Reusable components take plain data + callbacks. They never take a ViewModel, a `NavController`, a repository or a `Context`.
- `remember` is for UI-only state: scroll position, whether a bottom sheet is open, an animation's `Animatable`. Anything the user would expect to survive a process death is not UI-only state.
- `derivedStateOf` only where a cheap-to-read state drives an expensive recomposition (e.g. `scrollState.firstVisibleItemIndex > 0` for a shadow).

## 3. Size

A composable longer than **~80 lines** or nesting more than **4** layout levels gets split. Screens are assembled from named sections:

```kotlin
@Composable
private fun TodayHeader(...)
@Composable
private fun TodayHeroCard(...)
@Composable
private fun TodayChecklist(...)
```

`private` section composables live in the same file as the screen; anything reused across screens moves to `core/designsystem/component`.

## 4. No business logic in composables

Forbidden inside a composable: date arithmetic, mastery calculation, plan filtering, string formatting beyond `stringResource`, sorting, any `if` on a domain enum that decides *what the user gets* rather than *how it looks*.

Allowed: mapping a `UiState` field to a colour, choosing an icon, `if (state.loading)`.

The test: if a rule would need to change when the product changes, it belongs in the ViewModel or the domain.

## 5. Previews

Required:

- every component in `core/designsystem/component` - light **and** dark, via `@PreviewLightDark`;
- every screen - at minimum its default content state; additionally its empty and error states where those exist;
- the night-surface screens previewed inside `NightSurface`.

```kotlin
@PreviewLightDark
@Composable
private fun TodayScreenPreview() {
    IteraTheme { TodayScreen(state = SampleToday.day1, onEvent = {}, snackbarHostState = remember { SnackbarHostState() }) }
}
```

Sample data lives in `core/designsystem/preview/Samples.kt`, is shared across previews, tests and golden images, and mirrors the prototype's `AppViewModel.loadDemo()` state (Day 1, Day 2, Day 9 with a rest day, empty, error).

Add `@Preview(fontScale = 2f)` for any screen with a dense layout: Today, exercise result, Progress, Train.

## 6. Theming

- Colours come from `MaterialTheme.colorScheme` or `LocalSkillColors.current`. Never a literal.
- Text styles come from `MaterialTheme.typography`. Never an inline `TextStyle` with a hard-coded size, except inside `core/designsystem/theme/Type.kt`.
- Shapes come from `MaterialTheme.shapes` or the named shape tokens.
- Spacing uses literal `dp` values that match `docs/ux/04-design-system.md` section 4. A spacing token object is deliberately **not** introduced - the design uses irregular, intentional values (22, 14, 6) that a 4-point scale would flatten.

## 7. Lists

- `LazyColumn` with a stable `key`. `contentPadding` for edge spacing, not a padded parent.
- Never `Column { items.forEach { } }` for a list that can exceed ~15 items (Library, History).
- `Modifier.animateItem()` only where the design shows reordering (the Eisenhower inbox).

## 8. Text

- Every literal goes through `stringResource`. Lint `HardcodedText` is an **error**.
- Plurals use `pluralStringResource`. "2 sprints" / "1 sprint" is a plural, not a format argument.
- `overflow = TextOverflow.Ellipsis` with an explicit `maxLines` on every single-line row. Titles that must never truncate (technique names on the hero card) get `maxLines = 2` and are checked at `fontScale = 2`.
- `textAlign` is set explicitly wherever it is not start-aligned.

## 9. Semantics

- Decorative icons: `contentDescription = null`.
- Icon-only buttons: `contentDescription` on the `IconButton`, not the `Icon`.
- Composite rows (checklist item, library row, settings row) use `Modifier.semantics(mergeDescendants = true) { }` and supply a single `contentDescription` that reads naturally, replacing the fragments.
- Toggleable things use `Modifier.toggleable` / `Modifier.selectable` with the correct `Role`, which gives the right TalkBack verbs for free.
- Full details in `docs/ux/07-accessibility.md`.

## 10. Modifier discipline

- Order matters: `Modifier.padding().clickable()` has a smaller ripple than `Modifier.clickable().padding()`. Clickable first, then padding, unless the design shows otherwise.
- `Modifier.minimumInteractiveComponentSize()` on anything visually under 44 dp.
- Avoid `Modifier.weight` inside a `LazyColumn` item.
- No `Modifier.fillMaxSize()` on a screen root that also scrolls; use `fillMaxWidth()` + `verticalScroll`.

## 11. Performance

- Pass lambdas as stable references (`viewModel::onEvent`), not fresh lambdas per item where avoidable; where a per-item lambda is needed, capture only the id.
- Mark UI models `@Immutable` when they contain a `List`.
- No `Modifier.graphicsLayer` or `drawBehind` without a measured reason.
- Baseline profile generation is issue 040.

## 12. Navigation

Composables never receive a `NavController`. `AppNavHost` builds each `composable<Route> { }` and passes plain lambdas into the `XRoute`. Back handling uses `BackHandler` in the route, never in the screen.
