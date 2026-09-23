# Testing strategy

## 1. Shape of the pyramid

| Layer | Where | Count (target) | Runs in |
| --- | --- | --- | --- |
| **Domain unit tests** | `src/test`, pure JVM | the majority - every engine rule, every state transition, every threshold | milliseconds |
| **Mapper / repository tests** | `src/test`, some Robolectric | one per mapper, one per repository | seconds |
| **ViewModel tests** | `src/test` with fakes + `FakeClock` + Turbine | one per screen | seconds |
| **Compose UI tests** | `src/androidTest` | one per screen for its content state; extra for critical flows | minutes |
| **Integration tests** | `src/androidTest` with a real Room DB | plan generation, completion effects, migrations, workers, reset | minutes |
| **Golden images** | `src/test`, Roborazzi + Robolectric | ~96: 24 screens x 2 themes x 2 font scales | `docs/testing/04-visual-regression.md` |
| **Manual QA** | `docs/testing/02-manual-qa-checklist.md` | per release | - |

The domain layer carries the load because it holds every rule that matters and costs nothing to test.

## 2. No mocking framework

Fakes are hand-written, in `src/test/.../fake/`:

```kotlin
class FakeTrainingPlanRepository : TrainingPlanRepository {
    val days = MutableStateFlow<List<TrainingDay>>(emptyList())
    val completed = mutableListOf<Pair<Long, ActivityResult>>()
    override fun observeToday() = days.map { it.lastOrNull() }
    override suspend fun completeActivity(...) { completed += id to result }
    // ...
}
```

Reasons: the repository interfaces are small; fakes read better than `every { ... } returns ...`; a fake that must grow logic is a signal the interface is wrong. Fakes must stay **dumb** - if a fake needs a branch on its input, move that logic into a real test double of the engine instead.

## 3. Time

Every test that touches time injects `FakeClock`:

```kotlin
class FakeClock(var instant: Instant, val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    fun advance(d: Duration) { instant += d }
    fun setDate(d: LocalDate) { instant = d.atStartOfDay(zone).toInstant() }
}
```

There is no test anywhere that reads the real clock. `ArchitectureTest` enforces this for production code; review enforces it for tests.

## 4. What must be tested

### 4.1 Always

| Subject | Requirement |
| --- | --- |
| `ActivityStateMachine` | All 42 `(state, event)` pairs asserted; adding a state or event without extending the table fails the build |
| `GenerateDailyPlanUseCase` | The four worked examples from `docs/engine/01-training-plan-engine.md` byte-for-byte; idempotence; determinism over 100 runs; review caps; no locked technique emitted |
| `ReviewScheduler` | Every grade from every stage; retirement; 90-day overdue behaves as due-once |
| `masteryOf` / `skillLevel` | Full truth tables including every threshold boundary from both sides, and the artboard fixtures |
| `UnlockRules` | Exact unlocked set for days 1..20 |
| `CompleteActivityUseCase` | Each of the seven post-completion effects, plus idempotence of day completion |
| Mappers | Round-trip, plus the corrupt-payload path |
| `CatalogValidationTest` | The eight assertions in `docs/data/04-technique-catalog-format.md` section 6 |
| `ArchitectureTest` | The seven boundary rules in `docs/architecture/01-package-structure.md` section 3 |
| Room migrations | One `MigrationTest` per version step, against the exported schema |

### 4.2 Per screen

Every screen issue must add:

- a ViewModel test covering: initial state, each event, the error path, and the empty/first-run path;
- a `mapToUiState` test with table-driven cases;
- one Compose UI test rendering the content state and asserting the key nodes by `testTag` or text;
- Compose UI tests for the empty and error states where the screen has them;
- **a prototype-parity assertion set** - every element the spec lists is present, in order, with disabled states matching (`04-visual-regression.md` section 3);
- **a four-language render test** - the screen renders in en, ru, de and es with no truncated text node.

And, outside the automated suite, a **side-by-side comparison against `design/`**, recorded in the issue.

### 4.3 Product-guarantee tests

These exist to stop the product drifting, not to catch bugs:

| Test | Asserts |
| --- | --- |
| `NoStreakLanguageTest` | No string resource contains "streak", "in a row", "don't break", "XP", "points", or "% complete" |
| `NoUserTextLoggedTest` | No production source passes a note, reflection, explanation, topic title or task label to the `Logger` |
| `ProgressHonestyTest` (UI) | The Progress screen shows no streak, total or percentage after a seeded history |
| `ResetCoverageTest` | Every table in the database is assigned to a reset tier (ADR-0014) |
| `OfflineTest` | The merged manifest declares no `INTERNET` permission |
| `TranslationCompletenessTest` | Every key in `values/` exists in `values-ru`, `values-de` and `values-es` |
| `PluralCategoryTest` | Every plural declares the categories its language requires |
| `NoConcatenationTest` | No user-facing string is assembled from parts; every substitution is positional |

## 5. Test data

One shared fixture source, `src/test/.../fixture/Fixtures.kt`, mirroring the artboards:

- `Fixtures.day1` - the Day 1 plan (`a05`)
- `Fixtures.day1Midday` - after the exercise (`a09`)
- `Fixtures.day2` - with a carry-over and a practice prompt (`a13`)
- `Fixtures.day9` - with a due review (`c07`)
- `Fixtures.day14Combination`
- `Fixtures.historyDay9User` - nine days of activity with one rest day, matching what the prototype's `AppViewModel.loadDemo()` builds

The same fixtures back the Compose previews (`core/designsystem/preview/Samples.kt` delegates to them in debug) **and** the golden images, so previews, goldens and tests cannot disagree.

`loadDemo()` in the prototype is the reference for `historyDay9User`: program day 9, nine days back, day 3 deliberately skipped as a rest day, Pomodoro logged from day 2 and the 2-minute rule on alternate days, with a reflection every training day.

## 6. Compose UI tests

- Test `XScreen` directly with a hand-built `UiState`, not through Hilt, for anything that is pure rendering.
- Use `createAndroidComposeRule<MainActivity>` with `@HiltAndroidTest` and test doubles bound via `@TestInstallIn` only for navigation and integration flows.
- Assert on `testTag` for structure and on `onNodeWithText` for copy that is part of the contract.
- Always add `composeRule.onRoot().printToLog()` behind a debug flag, never committed enabled.
- Disable animations in the test runner (`--no-window-animation` plus `ANIMATOR_DURATION_SCALE = 0`), which also exercises the reduced-motion path.

## 7. Instrumented integration tests

Use an in-memory Room database except for migration tests. Cover:

- plan generation -> completion -> day completion -> program advance, end to end;
- a five-day gap and the resulting rollover;
- review creation on Day 6 and its appearance on Day 7;
- a combination day granting `INTEGRATED`;
- both reset tiers;
- `WorkManagerTestInitHelper` for each worker's idempotency and suppression rules;
- the focus timer across process death (`am kill`).

## 8. Coverage

- `domain` package: **at least 85 % line coverage**, enforced in CI.
- Everything else: no numeric gate. A coverage number on UI code encourages tests that assert nothing.
- Coverage is measured with Kover or Jacoco, configured in issue 001.

## 9. Flakiness policy

A flaky test is deleted or fixed within one working session - never retried, never `@Ignore`d without an issue link. The most likely sources here are Compose idling with the focus timer and WorkManager timing; both have documented deterministic approaches (`TestDriver`, `FakeClock`, `mainClock.autoAdvance = false`).

## 10. What is not tested automatically

Documented so the gap is deliberate, and covered by manual QA:

- actual notification delivery timing under Doze;
- OEM battery-optimisation behaviour;
- TalkBack announcement quality (correctness is tested; naturalness is judged);
- visual fidelity beyond what goldens catch - overall feel, rhythm, whether a screen reads in the right order;
- whether the copy reads well.
