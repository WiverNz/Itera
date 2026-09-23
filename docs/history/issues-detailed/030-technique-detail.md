# 030 - Technique detail

**Phase** 5 - Learning-loop surfaces | **Depends on** 013, 018, 029 | **Blocks** none

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Train.kt`** - `TechniqueDetailScreen`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the technique detail screen, matching artboard `c04` and `docs/ux/02-screen-specs-train-progress-you.md` section 3.

## User value

Everything about one technique in one place: what it is, why it works, how far you have taken it, and what you actually did with it.

## Scope

- Header: back, 48 dp skill tile, skill eyebrow, technique name, short description.
- "Why it helps" with the full explanation.
- A level card: "Your level" plus the level badge, the full `MasteryLadder`, the factual practice line and the next-level hint.
- Primary "Practice now" (with minutes for timer techniques), or, when locked, a disabled row "Unlocks on Day {introDay}".
- "Your practice": up to 5 recent entries with date, a one-line summary, and the user's note in quotes or a context line.
- "Works well with": three related techniques as tiles, each navigating to its own detail.
- Scrollable column.

## Non-goals

- Editing content.
- A full practice history (that is History, issue 032) - five entries is the cap.
- Practising a locked technique.

## Implementation notes

- "Practice now" creates a `MANUAL` activity in today's plan and opens its runner **directly**, skipping the intro screen - the user is already reading the explanation, so repeating it would be redundant. This is a deliberate difference from the Today path.
- For a timer technique the label includes the duration ("Practice now - 25 min") and it goes straight to the pre-timer sheet.
- The factual practice line and the next-level hint are two separate strings, both rendered. The artboard shows "6 sprints on 4 days. Use it inside a combination day to reach Integrated."
- The practice summary line per entry is type-specific: "2 sprints" for focus, "Explained {topic}" for Feynman, "Sorted 7 tasks" for Eisenhower, and so on. Derive it from the result payload; when the payload is unreadable, fall back to the technique name alone.
- Locked state: the explanation is fully readable (peeking is the point), the ladder renders empty, and the practice section is hidden entirely rather than showing an empty state.
- Related tiles come from the catalog's `related` list and may include locked techniques - that is fine and intended.
- Never-practised unlocked state shows "No practice logged yet." under the practice heading.

## Affected layers

`feature/train` (technique package), `core/navigation`.

## Acceptance criteria

- [ ] Matches artboard `c04`.
- [ ] The explanation renders in full for both locked and unlocked techniques.
- [ ] The level card shows the correct level, ladder position, factual line and hint.
- [ ] "Practice now" creates a `MANUAL` activity and opens the runner directly, skipping the intro.
- [ ] For a timer technique, the label includes the duration and it opens the pre-timer sheet.
- [ ] Locked techniques show a disabled "Unlocks on Day {n}" row and no practice section.
- [ ] Up to 5 recent practice entries render with type-appropriate summaries and the user's notes.
- [ ] A never-practised technique shows "No practice logged yet."
- [ ] An unreadable result payload degrades that one entry, not the screen.
- [ ] Related tiles render and navigate, including to locked techniques.
- [ ] Navigating between related techniques builds a sensible back stack.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`TechniqueDetailViewModelTest` - locked and unlocked states; each mastery level's card content; the practice list capped at 5, newest first; the type-specific summary for each `ActivityResult` variant; an unreadable payload; "Practice now" creating exactly one activity.

## UI test expectations

`TechniqueDetailScreenTest` - locked renders the disabled row and hides practice; unlocked renders the primary; "Practice now" navigates to the runner and not to the intro; related tiles navigate.

## Integration test expectations

- `PracticeNowTest` - "Practice now" creates a `MANUAL` activity that appears on Today and, on completion, counts toward mastery.

## Manual verification

1. Open Pomodoro with a seeded history; compare against the artboard.
2. "Practice now" a timer technique and a template technique; confirm both skip the intro.
3. Open a locked technique; read the explanation; confirm no practice section.
4. Follow three related links in a row; back returns through them correctly.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
