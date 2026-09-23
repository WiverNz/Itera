# Technique unlock rules

Deterministic and local (PRD: "Unlocking should initially be deterministic and local").

## 1. Rule

A technique unlocks when the program reaches its `introDay`. That is the whole rule.

```kotlin
fun shouldUnlock(technique: Technique, programDay: Int): Boolean =
    technique.introDay != null && programDay >= technique.introDay
```

Unlocking is performed by the plan engine at generation time (step 4) and recorded in `technique_state.unlockedAt`. It is never undone except by a program reset.

There is **no** performance gate: the user cannot fail to unlock the next technique by doing badly. Difficulty ratings, recall grades and skipped days do not affect unlocking. This follows directly from "Missing a day never resets anything" and from the product's anti-pressure stance.

## 2. Unlock table

| Program day | Technique | Skill | Exercise type |
| --- | --- | --- | --- |
| 0 (seed) | Daily reflection | Reflection | `REFLECTION` |
| 1 | 2-minute rule | Habits | `TEMPLATE` |
| 2 | Pomodoro | Focus | `FOCUS_TIMER` |
| 3 | Eisenhower matrix | Planning | `EISENHOWER` |
| 4 | 5-second rule | Focus | `TEMPLATE` |
| 5 | Habit stacking | Habits | `HABIT_STACK` |
| 6 | Feynman technique | Learning | `FEYNMAN` |
| 7 | Two-list strategy | Planning | `TEMPLATE` |
| 8 | Deep Work | Focus | `FOCUS_TIMER` |
| 9 | 80/20 principle | Planning | `TEMPLATE` |
| 10 | Spaced repetition | Learning | `TEMPLATE` |
| 11 | Information diet | Focus | `TEMPLATE` |
| 12 | Premortem | Reflection | `PREMORTEM` |
| 13 | 1% improvement | Habits | `TEMPLATE` |
| 14 | - (first combination day) | - | `COMBINATION` |

Daily reflection is unlocked at seed time because it runs every evening from Day 1.

Counts check out against the IA board's dots: Focus 4 (Pomodoro, 5-second, Deep Work, Information diet), Planning 3 (Eisenhower, Two-list, 80/20), Learning 2 (Feynman, Spaced repetition), Habits 3 (2-minute, Habit stacking, 1%), Reflection 2 (Daily reflection, Premortem). Total 14.

## 3. What a locked technique can do

Locked techniques are **visible**. The library shows all 14 from day one: "All 14 techniques. Your program unlocks them in order - you can open any of them to peek."

| Surface | Locked behaviour |
| --- | --- |
| Library row | Dimmed tile using `outlineDisabled`, a lock glyph, and a chip reading "Day {introDay}" instead of a mastery label |
| Technique detail | Name, short description and "Why it helps" are all readable. The mastery ladder renders empty. The primary action is replaced by a disabled row: "Unlocks on Day {introDay}" |
| Train path | Future nodes show the technique name greyed, with the day number |
| Plan engine | Never emits a locked technique into any activity, including practice prompts and generated combinations |
| Search / filter | Locked techniques are included in skill filters |

Peeking at a locked technique records nothing and changes nothing - it is not an app-open reward.

## 4. Relationship to mastery

Unlock and mastery are independent axes:

- `unlocked = false` forces `MasteryLevel.NONE`.
- `unlocked = true` with no completion is still `NONE` (seeing is not practising).
- `MET` requires a completed intro exercise.

## 5. Program reset

"Reset training data" (ADR-0014, tier 1) sets `technique_state.unlockedAt = null` for every technique except Daily reflection, resets `currentProgramDay = 1`, and deletes training days, activities, focus sessions, reflections, reviews and attempts. Learning topics and preferences survive.

"Erase everything" (tier 2) additionally deletes learning topics, habit stacks and the event log, and resets preferences to defaults, returning the app to its first-run state including onboarding.

## 6. Tests

`UnlockRulesTest`:

- for each program day 1..20, the exact expected unlocked set;
- an unlocked technique never re-locks when a day is abandoned;
- `programDay` does not advance on an abandoned day, so the same technique is offered again the next day;
- the plan engine never selects a locked technique across 1 000 generated days with random preference combinations.
