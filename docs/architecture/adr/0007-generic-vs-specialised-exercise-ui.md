# ADR-0007: One runner host, a data-driven template body, six specialised bodies

Status: accepted (2026-09-22)

## Context

14 techniques. Building 14 screens duplicates the intro/result chrome 14 times; building one configurable screen cannot express the Eisenhower matrix or the focus timer.

## Decision

Three tiers:

1. **`ExerciseRunnerScreen`** - the host. Owns the intro step, the close/back chrome, draft autosave, the completion call and the result step. Every exercise goes through it.
2. **`TemplateBody`** - a data-driven renderer over `ExerciseBlock` (Instruction, TextInput, Checklist, PickOne, TwoLists, ChipSelect). Serves 6 techniques: 5-second rule, 2-minute rule, Information diet, 1% improvement, 80/20, Two-list strategy.
3. **Six specialised bodies** - Focus timer, Eisenhower, Feynman, Premortem, Habit stacking, Review. Plus the Combination runner, which is a host of its own because it chains several activities.

A body is a `@Composable (activity, draft, onDraftChange, onComplete) -> Unit` selected by a `when (exerciseType)` in exactly one place.

## Alternatives considered

- **All bespoke** - 14x the UI surface and 14x the accessibility work. Rejected.
- **All generic** - the Eisenhower and focus-timer artboards are not expressible as form blocks. Rejected.

## Consequences

- Chrome, drafts, completion and analytics are implemented once.
- Adding a template technique is a JSON entry and no Kotlin.
- The block set is a public contract; a new block type touches the renderer, the `BlockValue` union and the catalog schema.

## Migration implications

A new block type is additive to `BlockValue`; old payloads stay readable.
