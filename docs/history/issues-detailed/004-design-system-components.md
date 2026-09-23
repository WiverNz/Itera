# 004 - Design system: components

**Phase** 1 - Foundation | **Depends on** 003 | **Blocks** 015

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/components/Components.kt`, `IteraIcons.kt`** - all 15 components and 33 icons.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the 14 reusable components in `docs/ux/04-design-system.md` section 5, plus the shared empty and error states, so every screen issue assembles rather than draws.

## User value

Visual and behavioural consistency across 24 screens, and accessibility done once instead of 24 times.

## Scope

**Port the prototype's `Components.kt`**, then add what production needs.

Port verbatim (name them as the prototype does, so the two stay comparable):

`ScreenColumn`, `Modifier.screenInsets()`, `IteraCard`, `Divider`, `Eyebrow`, `Pill`, `IteraButton` (3 kinds), `CircleIconButton`, `TopBar`, `TechniqueToken`, `ChoiceChip`, `Segmented`, `StepRow`, `StepDot` (3 states), `MasteryLadder`, `ProgressBar`, `NoteField`, `SectionTitle`.

Also port these, which live in screen files in the prototype but are reused across screens: `CheckCircle`, `RadioDot`, `LinkRow`, `MasteryDots`, `IntervalLadder`, `AnimatedCheck`, `Group` / `ValueRow` / `SwitchRow`, `TimeRow`.

Also port the **language picker**, which the prototype keeps in `Onboarding.kt` but which is a shared component (D-15): `LanguageSheet`, `LanguagePill` and `RadioDot`. Spec in `docs/i18n/00-localization.md` section 4.

**Add** (not in the prototype): `EmptyState`, `ErrorState`, a `Skeleton` block, and `TimePickerSheet` - a Material 3 `TimePicker` in a bottom sheet using `LanguageSheet`'s container treatment (D-17).

The previously listed names (`PrimaryButton`, `Badge`, `ChecklistRow`, ...) were invented from artboards and are superseded by the prototype's.

Each component must:

- take plain data plus callbacks - never a ViewModel, `NavController` or `Context`;
- apply `modifier` as its last defaulted parameter, to the outermost node;
- carry the correct semantics and role per `docs/ux/07-accessibility.md`;
- meet the 44 dp minimum target;
- honour `LocalReduceMotion`;
- have a `@PreviewLightDark` preview, and a `fontScale = 2f` preview where it contains text that can wrap.

Also add `core/designsystem/preview/Samples.kt` with sample data for previews.

## Non-goals

- Screen-specific composables (they live in each feature's `component/` package).
- The bottom navigation bar (issue 015 - it is shell, not a reusable component).
- Time pickers and bottom sheets (built by the issues that need them, using Material 3 components with app tokens).

## Implementation notes

- `ChecklistRow`'s **active** indicator uses the `breathe` spec on a 10 dp dot inside a 28 dp skill ring. With reduce-motion it is a static full-opacity dot.
- `MasteryLadder` takes `level: MasteryLevel` and `skill: Skill` and renders four tracks; the fill animates with `grow` only when `animate = true` (the result screen sets it; the library and detail screens do not).
- `ReviewLadder` takes `stageIndex: Int` and renders five nodes with four connectors; the connector before the current node is filled.
- `SelectableChip` must expose `Modifier.selectable` or `toggleable` with the right `Role` so TalkBack announces state - do not fake it with `clickable` plus a `contentDescription` suffix.
- `HeroCard` takes a slot for its action so the button can be a `PrimaryButton` or a disabled row.
- `SegmentedControl` stacks vertically above `fontScale 1.6`; implement with `BoxWithConstraints` or `LocalDensity.fontScale`, not a hard-coded width check.
- `EmptyState` and `ErrorState` take a message plus an optional action; neither takes an illustration - the design has none.
- Use `Modifier.minimumInteractiveComponentSize()` on `Badge` if it is ever made clickable; as specified it is not.

## Affected layers

`core/designsystem/component`, `core/designsystem/preview`.

## Acceptance criteria

- [ ] Every ported component matches its prototype original in dimensions, radii, weights, colours and animation specs.
- [ ] A side-by-side comparison of the component gallery against the prototype shows no difference.
- [ ] Every component has a `@PreviewLightDark` preview that renders.
- [ ] Every component with wrappable text has a `fontScale = 2f` preview that does not clip.
- [ ] No component accepts a ViewModel, `NavController` or `Context`.
- [ ] `StepDot` renders all three states, with the pulse and the check animation.
- [ ] `MasteryLadder` renders all five levels (none + four).
- [ ] `IntervalLadder` renders all five stages.
- [ ] `Pill` renders with caller-supplied colours, including the accent and skill variants.
- [ ] `TechniqueToken` renders at 36, 40, 44, 48, 56, 60 and 64 dp with radius `size * 0.32`.
- [ ] `IteraButton` renders all three kinds, enabled and disabled.
- [ ] `LanguageSheet` lists all five options, titles each language in its own language, subtitles it in the current language, and applies the choice immediately.
- [ ] `TimePickerSheet` returns a `LocalTime` and renders in 12- and 24-hour device formats.
- [ ] Every interactive component reports the correct role and state to the semantics tree.
- [ ] With reduce-motion on, no component animates.

## Unit test expectations

None - these are rendering components. Behaviour is covered by UI tests.

## UI test expectations

`ComponentTest`, one method per component, plus a golden per component per theme once issue 039 sets up Roborazzi:

- renders without crashing in light and dark;
- exposes the expected semantics (role, state description, merged description);
- invokes its callback exactly once on a single tap;
- `SelectableChip` reports `selected` correctly;
- `SegmentedControl` stacks at `fontScale 2.0`;
- `ChecklistRow` in the done state exposes a description containing the state;
- `MasteryLadder` at `INTEGRATED` merges into one node naming the level.

## Manual verification

1. Open every preview in both themes and compare against the artboards.
2. Run a harness screen showing all components; navigate it with TalkBack and confirm each announcement is sensible.
3. Set font size to maximum; confirm nothing clips.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 3, 5, 7, 10, 11.
