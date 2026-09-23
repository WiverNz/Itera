# Training plan engine

`domain/training/GenerateDailyPlanUseCase.kt`. Pure, deterministic, Android-free. The only non-deterministic input is an injected `Clock`.

## 1. Contract

```kotlin
class GenerateDailyPlanUseCase(
    private val catalog: TechniqueCatalogRepository,
    private val plans: TrainingPlanRepository,
    private val reviews: ReviewRepository,
    private val topics: LearningTopicRepository,
    private val prefs: PreferencesRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(date: LocalDate): TrainingDay
}
```

**Idempotent**: calling it twice for the same date returns the same stored plan. It generates only when `training_day` has no row for `date`.

**Deterministic**: given the same `(programDay, date, unlocked set, due reviews, preferences)` it produces byte-identical output. No randomness anywhere. Where a choice is needed (which technique to re-surface as a practice prompt), it is made by a stable rule, never by `Random` (ADR-0011).

## 2. Inputs

| Input | Source |
| --- | --- |
| `programDay` | `UserPreferences.currentProgramDay` |
| `curriculumDay` | `Curriculum.days[programDay - 1]`, clamped (section 7) |
| `unlocked` | `technique_state` rows with `unlockedAt != null` |
| `dueReviews` | `review_item` where `dueOn <= date` and `state != RETIRED` |
| `carryOverIntent` | previous `reflection_entry.tomorrowChange` |
| `timeBudget`, `pace`, `focusAreas`, `morningTime`, `eveningTime` | preferences |

## 3. Algorithm

```
1. If a training_day exists for `date`, return it.
2. Roll over the previous day (see 00-exercise-state-machine.md section 6).
3. day   := curriculum.days[clampedProgramDay]
4. Unlock day.newTechniqueId if it is still locked; record unlockedOnProgramDay.
5. activities := []

6. PRIMARY
   if day.combination is not empty:
       activities += combinationActivity(day.combination)      // source = COMBINATION
   else:
       activities += programActivity(day.newTechniqueId)       // source = PROGRAM

7. REVIEWS
   for each item in dueReviews sorted by (dueOn asc, stageIndex desc, id asc):
       take at most reviewCap(pace)
       activities += reviewActivity(item)                      // source = REVIEW, optional = false

8. SECONDARY  (exactly one, only when the day is not a combination day)
   if focusSuggestionAllowed():
       activities += focusActivity(focusTechnique())           // source = FOCUS_SUGGESTION, optional = true
   else if practicePromptAllowed():
       activities += practicePrompt(practiceTechnique())       // source = PRACTICE_PROMPT, optional = true

9. REFLECTION  (always last)
   activities += reflectionActivity()                          // source = REFLECTION, dayPart = EVENING

10. Assign orderIndex by (dayPart, source priority, id).
11. Persist the day + activities in one transaction. Return it.
```

### 3.1 `reviewCap(pace)`

| Pace | Max reviews surfaced per day |
| --- | --- |
| `GENTLE` | 1 |
| `STANDARD` | 2 |
| `INTENSE` | 3 |

Overflow stays due and reappears the next day. The Train tab's review queue always shows the full backlog, so nothing is hidden - only the Today list is capped.

### 3.2 `focusSuggestionAllowed()`

True when **all** of:

- Pomodoro or Deep Work is unlocked (i.e. `programDay >= 2`);
- `timeBudget != SHORT` (a 5-minute budget gets a practice prompt instead);
- the day is not a combination day (combination days already contain a focus block).

`focusTechnique()`: Deep Work if unlocked **and** `timeBudget == LONG`, otherwise Pomodoro.

`dayPart = DAYTIME`, `scheduledAtMinutes = max(morningTime + 90 min, 11:00)`, `estimatedMinutes` = the technique's `defaults.focusMinutes`.

### 3.3 `practiceTechnique()` - the "Keep using" rule

Deterministic, no randomness. Choose the unlocked technique that maximises, in order:

1. lowest `MasteryLevel` that is not `NONE` (prefer `MET` over `PRACTICED` over `APPLIED`);
2. oldest `lastUsedOn` (never-used-since-intro first);
3. membership of `preferences.focusAreas`;
4. lowest `introDay` (stable tiebreak).

Exclude: the day's own new technique, any technique already in today's plan, Daily reflection, and anything at `INTEGRATED`.

If no candidate qualifies (Day 1), emit nothing - Day 1 is program + focus + reflection, which is the three-step day the prototype shows.

### 3.4 Copy resolution

Each generated activity stores a `copyKey` and `copyArgs` (see `docs/data/01-room-schema.md` section 5), not English. The mapping:

| Source | `copyKey` | Rendered subtitle |
| --- | --- | --- |
| `PROGRAM` | `activity_program` | "Now - {minutes} min" |
| `COMBINATION` | `activity_combination` | "Now - ~{minutes} min" |
| `REVIEW` | `activity_review` | "Due today - {minutes} min - from Day {sourceDay}" |
| `FOCUS_SUGGESTION` | `activity_focus` | "Suggested around {time}" (morning) / "Now - optional" (once reached) |
| `PRACTICE_PROMPT` | `activity_practice` | "Tap to log when you use it" |
| `REFLECTION` | `activity_reflection` | "{eveningTime} - 2 min" |

## 4. Worked examples

These are the fixtures for `GenerateDailyPlanUseCaseTest`. Days 1, 2 and 9 correspond to states the prototype can be driven into; Day 9 matches its demo data.

### Day 1 (`a05-Today-Morning`)

```
programDay = 1, unlocked = {daily_reflection}, dueReviews = [], budget = STANDARD
-> unlock two_minute_rule
1. PROGRAM        two_minute_rule    MORNING  "Now - 5 min"
2. FOCUS_SUGGEST  pomodoro           DAYTIME  "Suggested around 11:00"   optional
3. REFLECTION     daily_reflection   EVENING  "21:00 - 2 min"
requiredCount = 2 completed-required + 1 optional -> displayed "0 / 3"
```

Note: the counter reads `0 / 3`, counting the optional focus block in the denominator - the prototype hard-codes three steps and production reproduces the same number from a three-activity plan. **Decision**: the "x / y" counter counts *every* activity in the day, optional included, so a day with a due review reads "0 / 4"; `requiredCount` (used for day completion) counts only non-optional ones. The two numbers are deliberately different and only the counter is user-visible.

Day 1 offers a focus block although Pomodoro is not unlocked until Day 2. **Resolution**: the Day-1 focus suggestion is a *generic* 25-minute block attached to `pomodoro` as its technique but with `copyKey = activity_focus_generic` ("25-minute focus session"), and completing it does **not** grant Pomodoro mastery. Pomodoro's own intro still happens on Day 2. The prototype does the same thing - its Today focus step is titled by minutes, not by technique, and its hero uses the `DeepWork` token generically.

### Day 2 (`a13-Today-Day2`)

```
programDay = 2, carryOverIntent = "Start the focus session before opening email."
1. PROGRAM          pomodoro          MORNING  "Now - before email"
2. PRACTICE_PROMPT  two_minute_rule   DAYTIME  "Tap to log when you use it"   optional
3. REFLECTION       daily_reflection  EVENING  "21:00 - 2 min"
```

The practice prompt wins over a focus suggestion here because the program activity is *already* a focus block (Pomodoro). Additional rule: **if the day's PROGRAM technique has `skill == FOCUS` and `exerciseType == FOCUS_TIMER`, skip the focus suggestion and emit a practice prompt instead.**

### Day 9 (`c07-Today-Dark`)

```
programDay = 9, dueReviews = [redis_persistence @ stage 1]
1. PROGRAM     pareto_principle   MORNING  "Now - 10 min"
2. REVIEW      feynman_technique  MORNING  "Due today - 5 min - from Day 6"
3. REFLECTION  daily_reflection   EVENING  "21:00 - 2 min"
```

A due review displaces the optional secondary activity: **at most one optional activity per day, and reviews take precedence.**

### Day 14 (combination)

```
programDay = 14
1. COMBINATION  [eisenhower_matrix, pareto_principle, deep_work, daily_reflection]  MORNING  "~70 min"
2. REFLECTION   daily_reflection  EVENING  (the chain's final step is folded into the evening reflection)
```

## 5. Regeneration

A plan is regenerated only when:

- the day has no completed activity **and** `generatorVersion` is lower than the current one (an app update changed the engine); or
- the user explicitly resets the program (ADR-0014).

A plan is **never** regenerated because preferences changed mid-day. Changing the evening time updates the reflection's `scheduledAtMinutes` in place; changing pace or focus areas takes effect from the next generated day. This keeps "the plan is stable for a given day" (original issue 007's requirement).

## 6. Time budget effect

| Budget | Effect |
| --- | --- |
| `SHORT` (5) | No focus suggestion. Practice prompts preferred. Program activity's estimate is shown but never shortened - content is not truncated. |
| `STANDARD` (15) | Default behaviour as above. |
| `LONG` (30+) | Focus suggestion uses Deep Work once unlocked; `reviewCap` is raised by 1. |

Budget never changes *which* curriculum technique appears; it only changes the optional secondary activity. The curriculum is the product.

## 7. Past the end of the curriculum (Day 15+)

The authored curriculum is 14 days. From `programDay = 15`:

```
if (programDay > curriculum.days.size) {
    isCombinationDay := (programDay - 14) % 3 == 0
    if (isCombinationDay) emit a generated combination (section 7.1)
    else emit a PRACTICE_PROMPT-style program activity for the technique
         chosen by practiceTechnique(), source = PROGRAM
}
```

This matches the IA board: "Week 3+ - Combine. Daily chains built only from techniques already met, plus spaced reviews."

### 7.1 Generated combinations

Pick 3 techniques, all at `PRACTICED` or better, one each from three *different* skills, ordered Planning -> Focus/Learning -> Reflection, preferring the user's `focusAreas`. Deterministic tiebreak by `introDay`. If fewer than 3 qualify, fall back to a single `PROGRAM` practice activity.

## 8. Weekly look-back

`CurriculumDay.weeklyLookBack` is true on Day 7 and every 7th program day after. It does not add an activity; it sets a flag on the reflection activity so the Reflection screen opens with a 7-day summary header (`docs/history/issues-detailed/019-evening-reflection.md`, now milestone 005).

## 9. Test fixtures

`GenerateDailyPlanUseCaseTest` asserts the four worked examples above byte-for-byte, plus:

- idempotence (two calls, one row);
- determinism (same inputs -> equal output across 100 runs);
- review cap per pace;
- no locked technique ever emitted;
- exactly one reflection, at most one optional activity;
- Day 15+ generation for every pace.
