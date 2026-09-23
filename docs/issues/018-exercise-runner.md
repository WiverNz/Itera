# 018 - Exercise runner and template body

**Phase** 3 - Shell and core loop | **Depends on** 008, 014, 015 | **Blocks** 019, 021-027, 030

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Exercise.kt`** - `ExerciseIntroScreen`, `TwoMinuteScreen`, `ExerciseResultScreen`, `CheckCircle`, `AnimatedCheck`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the three-step runner host (intro, run, result) and the data-driven template body, per `docs/ux/02-screen-specs-exercise.md` sections 1 and 2.1.

## User value

Every exercise gets the same calm, predictable frame, and six of the fourteen techniques work with no bespoke UI at all.

## Scope

**Host**:
- Intro screen matching `a06`: header, tile, eyebrow, title, framing, "Your exercise" card, primary and the snooze secondary.
- Run screen chrome: header with close, technique name and an optional progress indicator; draft autosave (2 s debounce, flush on `ON_STOP`); a `BackHandler` confirming abandonment only when the draft is non-empty; the primary button gated by the technique's `CompletionRule`.
- Result screen matching `a08`: success disc, headline, body, the Easy/Okay/Hard rating, the note field, the mastery card with `MasteryLadder` and the next-level hint, and "Done".
- Body selection: a single `when (exerciseType)` in one file (ADR-0007).
- Snooze time rule: the next whole hour at least two hours out, capped at 20:00.

**Template body**:
- All six `ExerciseBlock` renderers per section 2.1: `Instruction`, `TextInput`, `Checklist` (with the stopwatch variant), `PickOne`, `TwoLists`, `ChipSelect`.
- `BlockValue` state management and draft serialisation.
- The three `CompletionRule` evaluations.

## Non-goals

- The six specialised bodies (issues 021-026) - the `when` gets its remaining branches then.
- The combination runner (issue 027).
- The reflection (issue 019), which is a specialised body of its own.

## Implementation notes

- The host owns everything that is identical across techniques. If a later specialised-body issue needs to duplicate chrome, that is a signal this issue under-delivered.
- Rating and note save on **every change**, not on "Done". Backing out of the result screen must keep them. "Done" is navigation only.
- The stopwatch variant: a per-item timer starts on first edit of that item's text and freezes when ticked. Elapsed shows `m:ss`, `onSurfaceVariant`, turning `accent` past the technique's threshold (2:00 for the 2-minute rule). Store elapsed seconds in the `ChecklistItem`.
- Draft autosave must flush on `ON_STOP` as well as on the debounce, or a swipe-away loses up to two seconds of typing.
- The completion-gate reason must be available to TalkBack via `stateDescription` on the disabled button - a silently disabled button is an accessibility failure.
- Every free-text field caps at 4 000 characters with a counter visible past 3 500.
- `ExerciseResult` navigation uses `popUpTo(ExerciseIntro(id)) { inclusive = true }` so back does not re-enter the exercise.
- The result screen's mastery card has three variants (standard, focus, review); build the standard one here and leave clean extension points for issues 021 and 026.

## Affected layers

`feature/exercise/runner`, `feature/exercise/template`, `core/navigation`.

## Acceptance criteria

- [ ] Intro, run and result match artboards `a06`, `a07` and `a08`.
- [ ] "Not now - remind me at {time}" computes the documented time and snoozes the activity.
- [ ] Closing from intro leaves the activity `AVAILABLE`, not skipped.
- [ ] All six block types render and edit correctly.
- [ ] The stopwatch variant starts on first edit, freezes on tick, and turns `accent` past the threshold.
- [ ] All three completion rules gate the primary button correctly, with a `stateDescription` explaining why it is disabled.
- [ ] Drafts autosave on a 2 s debounce and flush on `ON_STOP`.
- [ ] Leaving and returning to a run restores the draft exactly.
- [ ] Force-stopping mid-exercise and reopening restores the route and the draft.
- [ ] Back from the run confirms only when the draft is non-empty.
- [ ] The result screen saves the rating and note on change; backing out and returning keeps them.
- [ ] The mastery ladder animates with `grow` on arrival and shows the correct level and hint.
- [ ] "Done" returns to Today and back does not re-enter the exercise.
- [ ] Completing the 2-minute rule end to end produces a valid `ActivityResult.Template` with two ticked items and their elapsed times.
- [ ] All six template techniques from the catalog complete successfully.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`ExerciseRunnerViewModelTest` - load by id; start; snooze time computation across several morning times and past 18:00; draft save and restore; completion with each rule; rating and note persistence; an unknown activity id yields an error state.

`CompletionRuleTest` - `Always`, `RequireBlocks` with empty and filled values, `RequireChecked` at N-1 and N.

`BlockValueSerializationTest` - every `BlockValue` variant round-trips through the draft codec.

## UI test expectations

`ExerciseRunnerScreenTest` - the three steps render; the snooze button shows the computed time; the primary is disabled until the rule passes; the result rating persists across a back-and-forward.

`TemplateBodyTest` - one method per block type: renders, accepts input, and contributes to the completion rule.

## Integration test expectations

- `ExerciseCompletionIntegrationTest` - complete the 2-minute rule from Today and assert the activity is `COMPLETED`, mastery is `MET`, the Today row is filled and the counter advanced.

## Manual verification

1. Do the 2-minute rule for real; watch the stopwatches run and freeze.
2. Type into a draft, swipe the app away, reopen: the text is there.
3. Rate and note, back out, return: both are there.
4. Snooze from the intro; the Today row reads "Snoozed until {time}".
5. Run the flow with TalkBack, including the disabled primary button.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
