# 012 - Spaced repetition scheduler

**Phase** 2 - Engine | **Depends on** 005, 006 | **Blocks** 014, 023, 026, 028

## Goal

Implement the five-stage ladder and the review lifecycle from `docs/engine/02-spaced-repetition.md`.

## User value

What the user learns comes back before they forget it, at growing intervals, with no penalty for being late.

## Scope

- `domain/review/ReviewScheduler.kt`: `INTERVALS_DAYS = [1, 4, 9, 21, 60]` and the pure `next(item, grade, reviewedOn)` function in section 3.
- `dueOn(items, date)` with the documented ordering.
- `ScheduleReviewUseCase`: creates or updates a review item on completion of a review-eligible exercise, per section 2, including the one-active-item-per-`(techniqueId, topicId)` rule.
- `SubmitReviewUseCase`: writes a `review_attempt` with `stageBefore`/`stageAfter`, updates the item via `ReviewScheduler.next()`, and completes the linked activity - all in one transaction, idempotent against a replayed submit.
- `RoomReviewRepository` completion of the write paths.
- `previewNextInterval(stageIndex)` for the "If this goes well..." caption.

## Non-goals

- The review screen (issue 026).
- The Feynman exercise that creates items (issue 023).
- Adaptive scheduling - explicitly out (ADR-0012).

## Implementation notes

- `next()` is pure and takes the date as a parameter; it must not read a clock.
- The `PARTIAL` grade repeats the *same* stage, which means `dueOn` moves forward by the current interval, not by zero. Test this - it is the easiest rule to get wrong.
- Stage 4 + `SOLID` retires the item. A retired item stays in the table and appears in Technique detail's practice history, but never becomes due.
- An item 90 days overdue is due **once**. Do not accumulate. Test explicitly.
- Idempotence of `SubmitReviewUseCase`: guard on the linked activity already being `COMPLETED`. A replayed tap or a re-delivered notification must not create a second `review_attempt`.
- `sourceAnswer` must never be loaded into a UI state before submission - that constraint belongs to issue 026, but the repository should make it easy by offering `item(id)` without the answer and `revealAnswer(id)` separately.

## Affected layers

`domain/review`, `data/repository`.

## Acceptance criteria

- [ ] Intervals are exactly 1, 4, 9, 21, 60 days.
- [ ] `FORGOT` from any stage returns stage 0 with `dueOn = reviewedOn + 1`.
- [ ] `PARTIAL` keeps the stage and moves `dueOn` forward by the current interval.
- [ ] `SOLID` advances one stage; at stage 4 it retires the item.
- [ ] A retired item never appears in `dueOn`.
- [ ] An item 90 days overdue appears exactly once.
- [ ] Due ordering is `(dueOn asc, stageIndex desc, id asc)`.
- [ ] Creating a review for a `(techniqueId, topicId)` pair that already has an active item updates that item's `sourceAnswer` and leaves its stage unchanged.
- [ ] `SubmitReviewUseCase` writes exactly one attempt for a replayed submit.
- [ ] `previewNextInterval` returns the `SOLID` outcome's interval, and `null` at stage 4.
- [ ] `ReviewScheduler` is pure and Android-free.

## Unit test expectations

`ReviewSchedulerTest`:
- a 15-case table: each of three grades from each of five stages, asserting `(stageIndex, dueOn, state)`;
- retirement at stage 4 + SOLID;
- due selection with items due today, yesterday and in a week;
- ordering with equal `dueOn` and different stages;
- a 90-day-overdue item.

`ScheduleReviewUseCaseTest`:
- first Feynman completion creates an item at stage 0 due tomorrow;
- a second completion on the same topic updates the answer and keeps the stage;
- a completion on a different topic creates a second item;
- a non-review-eligible technique creates nothing.

`SubmitReviewUseCaseTest`:
- attempt written with correct `stageBefore`/`stageAfter`;
- linked activity completed;
- replayed submit is a no-op.

## UI test expectations

None.

## Manual verification

None directly - verified through issues 023 and 026.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 10, 11.
