# ADR-0012: Fixed 5-stage ladder with a three-way recall grade

Status: accepted (2026-09-22)

## Context

The design draws a five-node ladder (1 d, 4 d, 9 d, 3 wk, 2 mo) on the review screen, making the stage count a UI contract. The PRD proposed a different four-stage ladder.

## Decision

Five fixed stages: `1, 4, 9, 21, 60` days. Self-graded recall: `FORGOT` resets to stage 0, `PARTIAL` repeats the stage, `SOLID` advances. Stage 4 plus `SOLID` retires the item. Full spec in `docs/engine/02-spaced-repetition.md`.

## Alternatives considered

- **SM-2 / Anki-style ease factors** - better long-term scheduling, but the drawn ladder implies fixed, nameable stages, and an ease factor is not explainable in a product that values calm honesty. Rejected for the MVP.
- **The PRD's 1/3/7/14** - contradicts the artboard. Rejected (see R-02).
- **Binary pass/fail** - loses the common "roughly, with gaps" case, which is the most informative one. Rejected.

## Consequences

- The ladder maps 1:1 onto the five drawn nodes.
- Overdue items never multiply or decay; missing a week costs nothing.
- `ReviewScheduler.next()` is a single pure function - the seam for a future adaptive scheduler.

## Migration implications

An adaptive scheduler would add `easeFactor` and `intervalDays` columns (additive) and change the ladder's visual meaning, which needs design input.
