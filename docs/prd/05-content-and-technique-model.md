# Technique and content model

The machine-readable format is `docs/data/04-technique-catalog-format.md`. This document is the product view.

## 1. Every technique defines

| Field | Purpose |
| --- | --- |
| stable id | never changes, never reused |
| name | display |
| short description | library row, hero subtitle |
| full explanation | "Why it helps" |
| skill | exactly one (section 2) |
| intro day | its position in the 14-day curriculum |
| exercise type | which UI body runs it |
| estimated duration | "About 5 min" |
| review eligibility | whether completing it schedules a spaced review |
| related techniques | "Works well with", 2-3 entries |
| unlock requirement | `programDay >= introDay`; nothing else |
| content version | carried by the catalog file, not per technique |

## 2. Skill mapping is one-to-one

Each technique feeds exactly one skill (R-03). This keeps progress explainable - a user can see why a practice moved a particular bar - and removes weighting decisions from the engine.

| Skill | Count | Techniques |
| --- | --- | --- |
| **Focus** | 4 | 5-second rule, Pomodoro, Deep Work, Information diet |
| **Planning** | 3 | Eisenhower matrix, 80/20 principle, Two-list strategy |
| **Learning** | 2 | Feynman technique, Spaced repetition |
| **Habits** | 3 | 2-minute rule, Habit stacking, 1% improvement |
| **Reflection** | 2 | Daily reflection, Premortem |

Two placements differ from the bootstrap draft and are deliberate:

- **2-minute rule -> Habits** (not Planning). The exercise is about not letting small things accumulate - a behavioural default, not a prioritisation method. The design labels it Habits on three separate screens.
- **Premortem -> Reflection** (not Planning). The exercise is structured hindsight applied forward; the design labels it Reflection.
- **Information diet -> Focus**. Choosing what you consume is attention protection.

## 3. Exercise types

| Type | Techniques | UI |
| --- | --- | --- |
| `TEMPLATE` | 2-minute rule, 5-second rule, Two-list strategy, 80/20, Spaced repetition, Information diet, 1% improvement | data-driven blocks |
| `FOCUS_TIMER` | Pomodoro, Deep Work | timer |
| `EISENHOWER` | Eisenhower matrix | 2x2 sorter |
| `FEYNMAN` | Feynman technique | explain + reflect |
| `PREMORTEM` | Premortem | reasons + mitigation |
| `HABIT_STACK` | Habit stacking | anchor/habit builder |
| `REFLECTION` | Daily reflection | three questions |
| `REVIEW` | (any review-eligible technique) | recall + compare + grade |
| `COMBINATION` | (combination days) | chained steps |

## 4. Template blocks

Six composable blocks cover every template technique: `Instruction`, `TextInput`, `Checklist` (optionally with a per-item stopwatch), `PickOne`, `TwoLists`, `ChipSelect`. Adding a template technique is a JSON entry plus its strings - no Kotlin.

## 5. Review eligibility

Only techniques that produce something recallable are review-eligible. In v1 that is **Feynman technique** and **Spaced repetition**. The others produce actions, not knowledge, and re-testing them would be theatre.

## 6. Content writing

Style rules, the voice, and the per-technique string keys are in `docs/ux/09-copy-deck.md`. The short descriptions are fixed by the design and must be used verbatim.

Each technique needs eight strings: name, short, explanation, intro, instruction, example, result headline, result body.

## 7. Adding a technique later

1. Add its entry to `techniques.vN.json` with a new `introDay` past the current maximum.
2. Add its eight strings.
3. Add it to the curriculum for that day.
4. Bump `contentVersion` in both files.
5. Run `CatalogValidationTest` and `CatalogCompatibilityTest`.

No schema migration, no code change, unless it needs a new exercise type.
