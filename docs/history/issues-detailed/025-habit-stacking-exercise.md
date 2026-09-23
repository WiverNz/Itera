# 025 - Habit stacking

**Phase** 4 - Specialised exercises | **Depends on** 018 | **Blocks** none

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Practice.kt`** - `HabitStackScreen`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the habit-stacking exercise, matching artboard `b07` and `docs/ux/02-screen-specs-exercise.md` section 2.7.

## User value

One sentence - "After I make coffee, I will read one page" - plus an optional nudge at the right moment.

## Scope

- The sentence with two inline editable slots: "After I **{anchor}**, I will **{habit}**." Filled words are underlined and tappable.
- Guidance text: "Anchor it to something you already do every day. Keep the new habit so small it feels almost silly."
- Two chip groups, each allowing a custom entry:
  - "Something you already do": make coffee, brush my teeth, open my laptop, sit down for lunch.
  - "A tiny new habit": read one page, write one line, drink a glass of water, plan tomorrow's top task.
- A nudge row: a switch, "Nudge me right after {anchor}", and a derived time, editable.
- Footer: "Log it on 3 days to reach Practiced."
- "Save my stack": persists `habit_stack`, `ActivityResult.HabitStack`, and schedules the nudge.

## Non-goals

- Multiple simultaneous stacks in the MVP - saving replaces the active one. (Archiving the old stack keeps the history.)
- Tracking habit completion outside the practice-prompt mechanic on Today.
- Location- or sensor-triggered nudges.

## Implementation notes

- The inline slots are the interaction: tapping a word opens its chip group plus a custom field. The sentence should read correctly at all times, including before either slot is filled (use a placeholder that still parses: "After I ___, I will ___.").
- The nudge time is derived from a known anchor (coffee -> ~07:45, lunch -> ~12:30, laptop -> the morning time) and is always editable. An unknown custom anchor defaults to the morning time.
- Saving replaces the active stack: archive the previous row rather than deleting it, so history and mastery remain intact.
- The nudge is a daily `HabitNudgeWorker` keyed by the stack id; scheduling belongs to issue 033's `ReminderScheduler`, called from here.
- The anchor and habit are user-authored; never log them.
- The footer's "3 days" is the `PRACTICED` threshold. Read it from the mastery rules rather than hard-coding, so the two cannot drift.

## Affected layers

`feature/exercise/habitstack`, `data/repository`, `core/notifications` (scheduling call).

## Acceptance criteria

- [ ] Matches artboard `b07`.
- [ ] The sentence reads correctly with zero, one or two slots filled.
- [ ] Tapping a slot opens its chip group with a custom-entry option.
- [ ] Both chip groups offer the four documented suggestions.
- [ ] A custom anchor or habit can be entered and is used in the sentence.
- [ ] The nudge switch works; the derived time is correct for known anchors and defaults sensibly for custom ones.
- [ ] The nudge time is editable.
- [ ] "Save my stack" persists the stack and the result, and schedules the nudge when enabled.
- [ ] Saving a second stack archives the first rather than deleting it.
- [ ] The nudge fires at the set time and deep-links to Today.
- [ ] Turning the nudge off cancels the scheduled work.
- [ ] Drafts survive process death.
- [ ] No user text is logged.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`HabitStackViewModelTest` - sentence rendering with 0/1/2 slots; chip selection and custom entry; the derived nudge time for each known anchor and for a custom one; save persisting and scheduling; saving twice archiving the first; disabling the nudge cancelling.

## UI test expectations

`HabitStackScreenTest` - slots are tappable and open their groups; the sentence updates; the nudge row toggles and reveals the time; the primary is disabled until both slots are filled.

## Integration test expectations

- `HabitNudgeTest` - enabling the nudge enqueues `habit_nudge_{id}` at the right delay; disabling cancels it; the worker posts nothing when the habit was already logged today.

## Manual verification

1. Build a stack with the chips, then with a custom anchor.
2. Enable the nudge for two minutes out and confirm it arrives and opens Today.
3. Save a second stack; confirm the first is archived and the old nudge is gone.
4. Check logcat during the exercise: no habit text appears.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
