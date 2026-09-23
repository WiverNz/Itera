# 024 - Premortem exercise

**Phase** 4 - Specialised exercises | **Depends on** 018 | **Blocks** none

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Practice.kt`** - `PremortemScreen`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the premortem exercise, matching artboard `b06` and `docs/ux/02-screen-specs-exercise.md` section 2.6.

## User value

Imagining the failure first surfaces the risks that optimism hides, and turns one of them into something to do today.

## Scope

- A project-name field with the "Project:" eyebrow.
- The headline: "It's {date}. The project failed completely. Why?" where the date is **computed** as six months out, formatted with the device locale.
- Reason rows: numbered, each with text plus a likelihood chip (Possible / Likely / Certain). "+ Add a reason". Minimum 3, maximum 6.
- A second section: "What can you do today to reduce risk #1?", an "Action for today" field, and a contextual idea line naming a related unlocked technique.
- "Add to today": creates a `MANUAL` activity in today's plan for that mitigation, then completes the exercise.
- `ActivityResult.Premortem` persistence and drafts.

## Non-goals

- AI analysis of the reasons (post-MVP, behind the same provider interface).
- Tracking whether the risk materialised - out of scope and would need a follow-up mechanic.
- Multiple projects or project history.

## Implementation notes

- The date is computed, not fixed: `today.plusMonths(6)`, formatted as month and year in the device locale. The artboard's "March 2027" is that rule applied to its mock date.
- Reason #1 is whichever reason is **first in the list**, not the highest likelihood. The user orders them; do not re-sort by likelihood, which would move the target under them.
- The contextual idea line names a related technique the user has already unlocked. The artboard suggests the Two-list strategy. Pick from the current technique's `related` list, filtered to unlocked; if none qualifies, omit the line rather than naming something locked.
- "Add to today" delegates to `AddManualPracticeUseCase`-style creation via `CompleteActivityUseCase` effect 5. If the premortem's own day is no longer today (a near-midnight completion), attach to today's plan.
- The project name and reasons are user-authored and must never be logged.
- Completing without "Add to today" is allowed - the action field is optional.

## Affected layers

`feature/exercise/premortem`.

## Acceptance criteria

- [ ] Matches artboard `b06`.
- [ ] The headline date is computed six months out and formatted for the locale.
- [ ] At least 3 reasons are required; up to 6 can be added.
- [ ] Each reason has a working likelihood chip.
- [ ] Reasons can be reordered or deleted.
- [ ] "Risk #1" refers to the first reason in the list and updates when the order changes.
- [ ] The idea line names an unlocked related technique, or is omitted.
- [ ] "Add to today" creates exactly one `MANUAL` activity visible on Today.
- [ ] Completing without an action is allowed.
- [ ] Near-midnight completion attaches the mitigation to today's plan, not to a past day.
- [ ] `ActivityResult.Premortem` persists the project, all reasons with likelihoods, the action and whether it was added.
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

`PremortemViewModelTest` - the computed date across a year boundary; adding, editing, reordering and deleting reasons; the 3-reason minimum and 6-reason maximum; risk #1 following the order; the idea line with all-locked, some-unlocked and no-related cases; "Add to today" creating one activity; near-midnight day attachment.

## UI test expectations

`PremortemScreenTest` - the headline renders a computed date; the primary is disabled under 3 reasons; likelihood chips toggle; "Add to today" fires once on a double tap.

## Integration test expectations

- `PremortemMitigationTest` - "Add to today" creates a `MANUAL` activity that appears in Today's checklist and can itself be completed.

## Manual verification

1. Run a real premortem on an actual project.
2. Reorder the reasons; the "risk #1" heading follows.
3. Add the action to today; find it on the Today checklist and complete it.
4. Complete a premortem at 23:58 and confirm the mitigation lands on the right day.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
