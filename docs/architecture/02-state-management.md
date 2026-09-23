# State management conventions

One pattern, applied identically to all 24 screens. An agent implementing any screen issue copies this template.

## 1. The template

```kotlin
// TodayUiState.kt
data class TodayUiState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val greeting: Greeting = Greeting.MORNING,
    val programDay: Int = 1,
    val dateLabel: String = "",
    val carryOverIntent: String? = null,
    val hero: HeroUi? = null,
    val checklist: List<ChecklistItemUi> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0
) {
    val isEmpty: Boolean get() = !loading && hero == null && checklist.isEmpty()
}

sealed interface TodayUiEvent {
    data object Refresh : TodayUiEvent
    data class OpenActivity(val id: Long) : TodayUiEvent
    data class LogPractice(val id: Long) : TodayUiEvent
    data object DismissError : TodayUiEvent
}

sealed interface TodayEffect {
    data class NavigateToExercise(val activityId: Long) : TodayEffect
    data class NavigateToFocus(val activityId: Long) : TodayEffect
    data class ShowMessage(val text: UiText) : TodayEffect
}
```

```kotlin
// TodayViewModel.kt
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val prefs: PreferencesRepository,
    private val ensurePlan: EnsureTodayPlanUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _effects = Channel<TodayEffect>(Channel.BUFFERED)
    val effects: Flow<TodayEffect> = _effects.receiveAsFlow()

    val state: StateFlow<TodayUiState> =
        combine(plans.observeToday(), prefs.preferences) { day, p -> day to p }
            .map { (day, p) -> mapToUiState(day, p, clock) }
            .catch { emit(TodayUiState(loading = false, error = UiText.Res(R.string.error_generic))) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init { viewModelScope.launch { ensurePlan() } }

    fun onEvent(event: TodayUiEvent) { /* when(event) { ... } */ }
}
```

```kotlin
// TodayRoute.kt
@Composable
fun TodayRoute(
    onOpenExercise: (Long) -> Unit,
    onOpenFocus: (Long) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is TodayEffect.NavigateToExercise -> onOpenExercise(effect.activityId)
            is TodayEffect.NavigateToFocus -> onOpenFocus(effect.activityId)
            is TodayEffect.ShowMessage -> snackbar.showSnackbar(effect.text.asString(context))
        }
    }

    TodayScreen(state = state, onEvent = viewModel::onEvent, snackbarHostState = snackbar)
}
```

```kotlin
// TodayScreen.kt  - pure, previewable, no Hilt, no NavController
@Composable
fun TodayScreen(
    state: TodayUiState,
    onEvent: (TodayUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState
) { /* ... */ }
```

## 2. Rules

1. **One `StateFlow` per screen.** Never expose several flows and combine them in the composable.
2. **`UiState` is immutable** and made of stable types. Lists are `List`, not `MutableList`; wrap in `ImmutableList` (kotlinx.collections.immutable) when a list is passed to a composable that recomposes often - Today's checklist, Library's rows, History's entries.
3. **`loading` and `error` are fields, not a sealed wrapper.** A screen refreshing in the background keeps showing its content (`docs/ux/03-ux-states.md`). A sealed `Loading | Content | Error` forces content to disappear, which the design never does.
4. **Navigation is an effect, not state.** A `navigateTo` field in `UiState` re-fires on rotation.
5. **Effects use `Channel(BUFFERED).receiveAsFlow()`**, not `SharedFlow`, so an effect emitted while the screen is not collecting is delivered once when it returns, and never twice.
6. **`collectAsStateWithLifecycle()`**, never `collectAsState()`.
7. **`SharingStarted.WhileSubscribed(5_000)`** everywhere, so a rotation does not restart the upstream.
8. **ViewModels never touch Android types** beyond `SavedStateHandle` and `ViewModel`. No `Context`, no `Resources`. User-facing text is a `UiText`.
9. **`init` does at most one thing**: kick off an idempotent "ensure" operation. Everything else is declarative flow composition.
10. **Mapping domain -> UI happens in a pure top-level function** (`mapToUiState`), unit-tested directly.

## 3. `UiText`

```kotlin
sealed interface UiText {
    data class Raw(val value: String) : UiText                                  // user-typed content
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText

    @Composable fun asString(): String = /* ... */
}
```

This keeps `Context` out of ViewModels while still letting them decide *what* is said. Never put a formatted English string in `UiState`.

## 4. Saved state

`SavedStateHandle` holds only route arguments and genuinely transient input that has no home in the database - e.g. the currently focused Eisenhower task id. Everything the user typed goes to `draftPayload` in Room (`docs/ux/01-navigation-graph.md` section 6), not to `SavedStateHandle`, because drafts must survive process death *and* app restart.

## 5. Forms and text input

Text fields are **state-hoisted to the ViewModel**, with the value in `UiState` and edits sent as events. Reasons: drafts autosave, and a UI test can assert the exact value.

Debounce: edits update `UiState` immediately (no lag while typing) and a `Flow` with `debounce(2_000)` writes the draft. On `ON_STOP` the draft is flushed immediately without waiting for the debounce.

```kotlin
private val draftTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
init {
    draftTrigger.debounce(2_000).onEach { saveDraft() }.launchIn(viewModelScope)
}
```

## 6. Lists

A list item's identity is its database id; `LazyColumn(items, key = { it.id })`. Item UI models carry only what the row renders - never a domain object with a result payload attached.

## 7. Shared state between screens

There is none. Each screen observes the repository. Two screens showing the same activity both see the same Room-backed flow and stay consistent without a shared ViewModel. The one exception is the combination-day runner, whose steps share a single `CombinationViewModel` scoped to the `Exercise.Combination` nav entry via `hiltViewModel(navBackStackEntry)`.

## 8. Testing

Every ViewModel gets a unit test using `kotlinx-coroutines-test` with a `StandardTestDispatcher`, fake repositories (hand-written, not Mockito) and a `FakeClock`. Assertions are on emitted `UiState` values via Turbine. `mapToUiState` is tested separately as a pure function with table-driven cases.
