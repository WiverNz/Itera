# 026 - Spaced repetition review screen

**Phase** 4 - Specialised exercises | **Depends on** 012, 018 | **Blocks** none

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Practice.kt`** - `ReviewScreen`, `IntervalLadder`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the review screen, matching artboard `b05` and `docs/ux/02-screen-specs-exercise.md` section 2.5.

## User value

Explaining it again, from memory, is the whole mechanism. Seeing the old answer first would destroy it.

## Scope

- The header: close, "Review", the Learning skill chip.
- The tile and prompt: "{n} days ago you explained {topic}." and "Explain it again - without looking at your previous answer."
- A bordered text area labelled "Your explanation, from memory", placeholder "Start with the simplest true sentence you can."
- The redacted block: four grey bars plus "Day {n} answer is hidden until you finish".
- The `ReviewLadder` with the current stage highlighted and "If this goes well, the next review is in {n} days."
- Primary "Compare with my first answer" -> reveals both answers side by side and shows the three grade buttons.
- Grade submission via `SubmitReviewUseCase`, then the review result variant (the `ReviewLadder` in place of the mastery ladder, plus "Next review in {n} days").

## Non-goals

- Editing the previous answer.
- Any automatic grading or similarity scoring.
- Changing the ladder (issue 012 owns it).

## Implementation notes

- **The previous answer must not be in the composition before the reveal.** Hold it in a private field in the ViewModel and copy it into `UiState` only on the reveal event. A UI test asserts it is absent from the semantics tree beforehand - this is the one correctness property that matters most on this screen.
- The redacted block is `invisibleToUser` for screen readers, with a placeholder announcement: "Your previous answer is hidden until you finish."
- The "next review in {n} days" caption previews the `SOLID` outcome, from `previewNextInterval`. At stage 4 it should say the review will be retired rather than naming an interval.
- After revealing, the primary becomes the three grade buttons; there is no way back to editing. Closing before grading discards the attempt and leaves the review due - say so if the user tries to close after revealing.
- An empty answer can still be graded (the honest case is "I couldn't recall it"), so do not gate the reveal on a word count.
- Submitting is idempotent (issue 012); a double tap must not create two attempts.

## Affected layers

`feature/exercise/review`, `domain/review`.

## Acceptance criteria

- [ ] Matches artboard `b05`.
- [ ] The prompt shows the correct elapsed days and topic.
- [ ] The previous answer is absent from the semantics tree before the reveal.
- [ ] The redacted block renders and announces its placeholder.
- [ ] The ladder highlights the correct stage of five.
- [ ] The next-interval caption previews the `SOLID` outcome and handles stage 4.
- [ ] "Compare" reveals both answers side by side and shows three grade buttons.
- [ ] Each grade moves the ladder per `docs/engine/02-spaced-repetition.md`.
- [ ] A double tap on a grade creates one attempt.
- [ ] Closing before grading leaves the review due.
- [ ] Closing after revealing warns that the attempt will be discarded.
- [ ] An empty answer can be graded.
- [ ] The result screen shows the `ReviewLadder` and the next-review line.
- [ ] Drafts survive process death.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`ReviewViewModelTest` - the previous answer is not in `UiState` before reveal and is after; the elapsed-days calculation; the preview interval at each stage including 4; each grade's effect; idempotent submission; closing before and after reveal.

## UI test expectations

`ReviewScreenTest`:
- `onNodeWithText(previousAnswer).assertDoesNotExist()` before the compare tap, and exists after - the headline test for this issue;
- the redacted block's placeholder announcement;
- the ladder highlights the right node;
- the three grade buttons appear only after reveal.

## Integration test expectations

- `ReviewIntegrationTest` (extending issue 023's) - a review created on Day 6 appears Day 7, is graded `SOLID`, and reappears on Day 11 (4 days later).

## Manual verification

1. Do a real Feynman exercise, advance a day, take the review without peeking - confirm you cannot see the old answer.
2. Grade each of the three ways on separate items and check the next due dates.
3. Close after revealing; confirm the warning and that the review is still due.
4. Run with TalkBack; the hidden answer is not read out.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
