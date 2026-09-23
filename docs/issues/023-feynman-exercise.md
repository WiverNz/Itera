# 023 - Feynman exercise

**Phase** 4 - Specialised exercises | **Depends on** 012, 018 | **Blocks** none

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Practice.kt`** - `FeynmanScreen`, `FeynmanFeedbackScreen`, `FeedbackRow`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the two-step Feynman exercise (artboards `b03`, `b04`) with its non-AI self-assessment, and schedule the spaced review it produces.

## User value

Explaining something simply is how you find out you do not understand it. The app then makes sure you explain it again, from memory, four days later.

## Scope

**Step 1 - Explain**:
- The topic card with the topic title, and two text actions: "From your learning list" (a topic picker) and "Change topic".
- The prompt: "Explain it to a curious 12-year-old. If you use a term, define it."
- A large text area.
- A live word count and the note "No notes, no looking it up".
- Primary "Done explaining", enabled at 30 or more words.

**Step 2 - Reflect**:
- "Now look back at it".
- "What part was hardest to explain?" with suggestion chips and a note field.
- A footer: "You'll explain this again in {n} days, from memory."
- Primary "Finish".

**Coach placeholder** (D-10, reversing the earlier decision to hide it):
- Render the prototype's dashed box: `drawBehind` rounded rect, corner radius 24, `Stroke(2.dp, dashPathEffect(floatArrayOf(12f, 10f)))` in the Learning content colour, `padding(18)`, `spacedBy(14)`.
- Header row: `Spark` icon 20, "Coach feedback" in `body` SemiBold, `Pill("Coming later", Learning container, Learning content)`.
- A disabled `Secondary` `IteraButton` at `height = 44`.
- **One** explanatory line in place of the prototype's three sample `FeedbackRow`s - shipping fabricated coaching text would mislead. Keep `FeedbackRow` in the codebase for when a provider exists.

**Supporting**:
- The learning-topic picker and inline topic creation.
- `ActivityResult.Feynman` persistence with the topic, explanation, word count, hardest parts and note.
- Review scheduling via issue 012's `ScheduleReviewUseCase`.

## Non-goals

- **Working AI feedback.** The provider reports unavailable, so no real feedback is produced (ADR-0016). The *placeholder* box **is** rendered - see the scope note below.
- The review screen itself (issue 026).
- The topic editor in Settings (issue 034) - inline creation here is enough.

## Implementation notes

- The real feedback rows are behind `if (coachFeedback.isAvailable)`, which is always false in the MVP. The placeholder box itself is always shown, and is what the prototype draws.
- The distinction that matters: a clearly-labelled "Coming later" box sets an honest expectation; three rows of invented critique presented as output does not. Ship the former.
- The 30-word gate is a completion rule, not a hard block on typing. The disabled button carries a `stateDescription` naming the remaining words.
- The word count is a polite live region announced every 25 words, not per keystroke.
- If the user has no learning topics, step 1 opens with inline creation: a single field, "What are you learning?". Do not dead-end on an empty topic list.
- `{n}` in the footer is `INTERVALS_DAYS[0]` = 1 for a first explanation; for a re-explanation of an existing topic it is the item's current stage interval. Read it from `ReviewScheduler.previewNextInterval`, do not hard-code 4.
- Re-explaining an existing topic **updates** the existing review item's answer and keeps its stage (issue 012's rule). Make sure the UI does not imply a fresh start.
- The explanation text is user-authored and must never be logged.

## Affected layers

`feature/exercise/feynman`, `data/repository` (topics), `domain/review`.

## Acceptance criteria

- [ ] Both steps match artboards `b03` and `b04`.
- [ ] The dashed coach box renders with its header, `Spark` icon, "Coming later" pill and disabled button.
- [ ] No fabricated feedback text is shown; the box contains one explanatory line.
- [ ] The topic can be picked from the learning list or created inline.
- [ ] With no topics, inline creation is offered rather than an empty picker.
- [ ] The word count is live and accurate.
- [ ] "Done explaining" is disabled under 30 words, with a state description naming the shortfall.
- [ ] Step 2's chips and note both persist.
- [ ] The footer names the correct next interval, read from the scheduler.
- [ ] Finishing persists `ActivityResult.Feynman` and creates a review item at stage 0, due tomorrow.
- [ ] Re-explaining the same topic updates the existing item's answer and keeps its stage.
- [ ] Drafts survive process death across both steps.
- [ ] The explanation never appears in a log.
- [ ] Works at `fontScale 2.0`.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`FeynmanViewModelTest` - topic selection and inline creation; word counting including punctuation and multiple spaces; the 30-word gate at 29 and 30; step transition; chip and note persistence; the footer interval for a new and an existing topic; completion creating a review; re-explanation updating rather than duplicating.

## UI test expectations

`FeynmanScreenTest` - both steps render; the coach box is present and its button is disabled; none of the prototype's sample feedback strings appears in the semantics tree; the primary is disabled under 30 words with an explanatory description; the word count announces politely, not per keystroke.

## Integration test expectations

`ReviewIntegrationTest` - a Feynman completion on a seeded Day 6 creates a review due Day 7 that appears on Today.

## Manual verification

1. Write a real explanation of something; watch the word gate.
2. Create a topic inline with an empty topic list.
3. Finish, advance the clock one day, confirm the review appears.
4. Re-do the exercise on the same topic and confirm the review's stage did not reset.
5. Check `adb logcat` during the exercise: no explanation text appears.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
