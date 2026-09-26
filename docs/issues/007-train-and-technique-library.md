# 007 - Train & technique library

**Depends on** 003, 004, 006 | **Blocks** 009

## Goal

The user can see the curriculum they are on and browse every technique: the Train tab, the technique library and technique detail with "Practice now".

## Included scope

- **Train tab** (old 028): week header, curriculum path with past/today/future nodes, review card, library entry.
- **Technique library** (old 029): all 14 techniques at every program day, locked/unlocked rows, mastery strip, sticky skill filter, sort order.
- **Technique detail** (old 030): explanation, level card, "Practice now" (creates a `MANUAL` activity, opens runner or pre-timer sheet), locked state, recent practice.
- Strings in four languages; analytics call sites for these flows.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/028-train-tab.md`, `029-technique-library.md`, `030-technique-detail.md`.
- `docs/ux/02-screen-specs-train-progress-you.md` sections 1-3; `docs/engine/03-mastery-and-progress.md`, `04-unlock-rules.md`.
- Prototype: `design/app/src/main/java/com/itera/app/ui/screens/Train.kt`.

## Key dependencies

- Needs 004 (unlocks, mastery, reviews) and 006 (runner, pre-timer sheet, review screen that nodes and "Practice now" open).
- Past Train nodes open a day in History (008). Until 008 lands, that tap routes to the History route placeholder; 008 verifies it.
- Can be done before, after or interleaved with 008.

## Acceptance criteria

- [x] Train shows the correct week and node states; today's node opens the day's exercise; future nodes are not tappable and are announced as locked; the review card appears only when a review is due.
- [x] The library lists all 14 techniques from Day 1, with the documented locked styling, mastery strip and Day-9 sort order; the filter is sticky, single-select, default All.
- [x] Detail renders the full explanation for locked and unlocked techniques; "Practice now" creates one `MANUAL` activity and opens the runner (skipping the intro) or the pre-timer sheet.
- [x] New strings exist in en/ru/de/es; each screen was compared against the prototype in light and dark, with only deviations recorded.

## Reconciled differences

- Follow the current prototype's four mastery dots and unlock-day ordering, rather than the older artboard's six segments and mastery-first ordering. Daily reflection makes two techniques unlocked on Day 1.
- Updated the prototype alongside production for sticky single-select filters, locked semantics and detail controls, factual mastery counts, actual timer duration, and the header after Day 14. Production history summaries use saved exercise results rather than demo content.
- Past nodes intentionally retain the History placeholder until 008. See the implementation decisions in `docs/00-source-of-truth.md`.

## Required tests

`TrainViewModelTest`, `TrainScreenTest`, `LibraryViewModelTest`, `LibraryScreenTest`, `TechniqueDetailViewModelTest`, `TechniqueDetailScreenTest`, `PracticeNowTest`.
