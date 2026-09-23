# 027 - Combination day runner

**Phase** 4 - Specialised exercises | **Depends on** 018, 021, 022 | **Blocks** 037

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Practice.kt`** - `CombinationScreen`, `ChainStep`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the combination-day runner, matching artboard `b01` and `docs/ux/02-screen-specs-exercise.md` section 2.8.

## User value

Three techniques the user already knows, used as one move. This is what the second week has been building toward, and the only way to earn `Integrated`.

## Scope

- The chain screen: a header ("Day {n} - Combination", a "~{total} min" chip), a title and subtitle from the curriculum, and a vertical rail of step cards.
- Step card states: **completed** (collapsed to a result summary), **current** (expanded with its inline body), **future** (dimmed).
- Each step card: skill tile, technique name, the step's prompt, the step's hint, and - once done - the captured summary ("Most important: Q4 roadmap", "Do now - picked 08:42").
- Inline bodies: each step reuses the body from its own issue (Eisenhower from 022, the focus timer from 021, template bodies from 018). The runner hosts them; it does not reimplement them.
- The final step is always Daily reflection, shown as "Part of your evening reflection" and **not** completed here.
- The primary button label is the next step's action ("Continue to Deep Work").
- Persistence: a parent `COMBINATION` activity plus one child `plan_activity` per step, each with `source = COMBINATION`, so every technique earns `INTEGRATED`.
- `ActivityResult.Combination` on the parent, holding each step's summary.

## Non-goals

- Authoring combinations beyond Day 14 (issue 011 generates them).
- A new UI for any step - every body already exists.
- Completing the reflection inside the chain.

## Implementation notes

- **The reason this issue depends on 021 and 022** is the Day-14 chain: Eisenhower -> 80/20 -> Deep Work -> reflection. 80/20 is a template body from 018.
- One `CombinationViewModel` scoped to the `Exercise.Combination` nav entry via `hiltViewModel(navBackStackEntry)` - this is the single documented exception to "no shared ViewModels" (`docs/architecture/02-state-management.md` section 7).
- The focus step navigates out to the full-screen timer and returns; the chain must survive that round trip. Keep chain state in the database (child activity states), not in the ViewModel.
- Each step completing writes its own child activity immediately. Do not batch them until the end, or an abandoned chain loses the work already done.
- The `INTEGRATED` grant depends on child rows having `source = COMBINATION` - verify against issue 013's query in an integration test, not by inspection.
- Closing mid-chain confirms abandonment only when at least one step is complete. Completed steps stay completed; the chain can be resumed.
- Step results carry forward: Eisenhower's chosen task becomes 80/20's subject, and 80/20's chosen step becomes Deep Work's task label. Wire this explicitly - it is what makes the chain feel like one move rather than three exercises.
- The total-minutes chip sums the steps' estimates.

## Affected layers

`feature/exercise/combination`, `domain/training`.

## Acceptance criteria

- [ ] Matches artboard `b01`.
- [ ] The header shows the correct day and a summed duration.
- [ ] Step cards render in all three states with the documented content.
- [ ] Each step's inline body is the same component used by its own screen.
- [ ] The Day-14 chain is Eisenhower -> 80/20 -> Deep Work -> reflection.
- [ ] Eisenhower's chosen task appears as 80/20's subject.
- [ ] 80/20's chosen step appears as the Deep Work task label.
- [ ] The focus step opens the full-screen timer and returns to the chain with the step completed.
- [ ] The reflection step is shown but not completed in the chain.
- [ ] Each completed step writes its own child activity with `source = COMBINATION`.
- [ ] Each technique used reaches `MasteryLevel.INTEGRATED`.
- [ ] Closing mid-chain confirms only when a step is complete, and completed steps survive.
- [ ] A resumed chain continues at the right step.
- [ ] Process death mid-chain loses nothing.
- [ ] The primary label names the next step.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`CombinationViewModelTest` - step progression; carry-forward of each step's result into the next; the summed duration; resume at the right step; abandonment rules; the reflection step never completing here.

## UI test expectations

`CombinationScreenTest` - the three card states render; the primary label follows the next step; closing with one step complete confirms; resuming lands on the right step.

## Integration test expectations

`CombinationIntegrationTest`:
- run the full Day-14 chain against a real database;
- assert one parent plus four child activities, all with `source = COMBINATION`;
- assert Eisenhower, 80/20 and Deep Work each reach `INTEGRATED`;
- assert the chain survives the round trip through the full-screen timer.

## Manual verification

1. Seed a database at Day 14 and run the whole chain for real, including a 50-minute Deep Work block (or a shortened one in debug).
2. Check Technique detail for all three techniques: each shows `Integrated`.
3. Abandon mid-chain, reopen, resume.
4. Confirm the chosen task really does carry from Eisenhower through to the timer's task label.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
