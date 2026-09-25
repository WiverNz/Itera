# Technique curriculum

The 14-day program. Source of truth for the content in `curriculum.v1.json`; engine behaviour is in `docs/engine/01-training-plan-engine.md`.

## 1. Shape

- **Week 1 - Foundations**: small wins. Six techniques plus the first review.
- **Week 2 - Depth**: longer focus and learning. Six techniques, ending in the first combination.
- **Week 3 onwards - Combine**: daily chains built only from techniques already met, plus spaced reviews at 1, 4, 9, 21 and 60 days.

Daily reflection runs every evening from Day 1 and is never "the day's technique".

## 2. The 14 days

| Day | Technique | Skill | Type | Also that day |
| --- | --- | --- | --- | --- |
| 1 | 2-minute rule | Habits | template (stopwatch checklist) | a generic 25-min focus suggestion |
| 2 | Pomodoro | Focus | focus timer | practice prompt: 2-minute rule |
| 3 | Eisenhower matrix | Planning | 2x2 sorter | |
| 4 | 5-second rule | Focus | template | **review: 2-minute rule** |
| 5 | Habit stacking | Habits | anchor builder | |
| 6 | Feynman technique | Learning | explain + reflect | creates the first spaced review |
| 7 | Two-list strategy | Planning | template (two lists) | **weekly look-back** |
| 8 | Deep Work | Focus | focus timer | |
| 9 | 80/20 principle | Planning | template (pick one) | review from Day 6 falls due |
| 10 | Spaced repetition | Learning | template | names the mechanism the user has been using since Day 7 |
| 11 | Information diet | Focus | template | |
| 12 | Premortem | Reflection | reasons + mitigation | |
| 13 | 1% improvement | Habits | template | |
| 14 | **First combination** | - | combination | Eisenhower -> 80/20 -> Deep Work -> tonight's reflection. **weekly look-back** |

Day 14 is the first combination (R-07).

## 3. Why this order

| Position | Reason |
| --- | --- |
| 2-minute rule first | The smallest possible win, finishable in five minutes on day one, with a visible result |
| Pomodoro second | Introduces the timer, which several later techniques reuse |
| Eisenhower third | The first thinking exercise, once two doing-exercises have built the habit of opening the app |
| 5-second rule on Day 4 | Paired with the first review, so the review mechanic arrives alongside an easy technique |
| Feynman on Day 6 | Needs more time and attention; lands mid-first-week when the routine is forming |
| Two-list on Day 7 | Ends the week with a decision about what *not* to do, next to the weekly look-back |
| Deep Work on Day 8 | Only after Pomodoro has been practised; it is the same skill at a longer scale |
| Spaced repetition on Day 10 | Deliberately *after* the user has already done reviews. The exercise explains something they have experienced |
| Premortem on Day 12 | The hardest reflective exercise, once the reflection habit is two weeks old |
| Combination on Day 14 | Three techniques the user has met, used as one move |

## 4. Day 15 onwards

Generated, not authored:

- Every third day (Days 17, 20, 23, ...) is a **combination**: three techniques at `PRACTICED` or better, one each from three different skills, ordered Planning -> Focus/Learning -> Reflection, preferring the user's focus areas, with a deterministic tiebreak by intro day.
- Other days are a **deepening practice** of a technique chosen by the practice-prompt rule (lowest non-zero mastery, then oldest last use, then focus-area membership).
- Reviews continue on the ladder regardless.
- If fewer than three techniques qualify for a combination, the day falls back to a single practice.

## 5. Pace

Pace changes the **optional** parts, never the curriculum order.

| Pace | Effect |
| --- | --- |
| Gentle | At most 1 review per day surfaced on Today. Techniques are introduced at the curriculum rate, as for every pace |
| Standard | At most 2 reviews |
| Intense | At most 3 reviews. The focus suggestion prefers Deep Work once unlocked |

Pace never changes when techniques are introduced or unlocked: every pace follows the curriculum table (`programDay >= introDay`). Pace changes only the review workload (the cap above, plus one on a long time budget) and the focus suggestion. `programDay` counts training days, not calendar days. Decided 2026-09-25; see `docs/00-source-of-truth.md`, "Milestone 004 implementation decisions".

## 6. Weekly look-back

Days 7, 14, 21, ... set a flag on that evening's reflection. The reflection opens with a summary header: days trained this week, techniques practised, focus minutes, and the previous week's "change for tomorrow" answers. It adds no extra activity and no extra tap - the three questions follow as usual.

## 7. Content changes

Re-ordering `introDay` for an already-started user does not re-order their program: past days are never regenerated, and future days read the curriculum fresh at generation time. This means a user mid-program may get a different order than a new user. That is acceptable and documented; it is the price of never rewriting someone's history (ADR-0015).
