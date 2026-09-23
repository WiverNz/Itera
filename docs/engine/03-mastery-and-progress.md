# Mastery and progress

All values are **derived on read** from `plan_activity` and `focus_session`. Nothing here is a stored counter (ADR-0013). Consequences: no drift, no migration when the rules change, and a data reset is a single delete.

Implemented in `domain/progress/` as pure functions over query results.

## 1. What counts

> **Counts**: finished exercises, completed focus minutes, reviews done, days practised out of the last 14.
> **Never counts**: opening the app, taps, streak freezes, points for logging in.

Concretely, a `plan_activity` contributes iff `state == COMPLETED`. `SKIPPED` and `EXPIRED` contribute nothing. Opening a screen contributes nothing. There is no XP, no score, no streak counter anywhere in the UI or the schema.

## 2. Technique mastery

Four levels, earned by real use (R-13).

| Level | Rule | Wording |
| --- | --- | --- |
| `NONE` | technique locked, or unlocked but never completed | (bar empty) |
| `MET` | the intro exercise completed once | "Did the intro exercise once" |
| `PRACTICED` | completed on **3 or more distinct calendar days** | "Used it on 3 different days" |
| `APPLIED` | **6 or more completions** spanning **14 or more days** between first and last use | "6+ uses across 2+ weeks" |
| `INTEGRATED` | at least one completion inside a `COMBINATION` activity | "Used inside combination days" |

> The prototype's `masteryOf` checks `uses >= 6` with no span test, because it has no real history to span against. The product rule keeps the two-week requirement (P-02). The ladder's **rendering** is the prototype's: `MasteryLadder`, four bars, `tween(700, delayMillis = 150 * index)`.

```kotlin
fun masteryOf(
    unlocked: Boolean,
    introCompleted: Boolean,
    distinctDays: Int,
    totalUses: Int,
    firstUse: LocalDate?,
    lastUse: LocalDate?,
    usedInCombination: Boolean
): MasteryLevel = when {
    !unlocked || !introCompleted -> MasteryLevel.NONE
    usedInCombination            -> MasteryLevel.INTEGRATED
    totalUses >= 6 && firstUse != null && lastUse != null &&
        DAYS.between(firstUse, lastUse) >= 14 -> MasteryLevel.APPLIED
    distinctDays >= 3            -> MasteryLevel.PRACTICED
    else                         -> MasteryLevel.MET
}
```

Levels are monotone in practice but the function is **not** monotone by construction - it is a pure classification of the current facts. Because completions are append-only, the level can only rise. A data reset drops it to `NONE`, which is correct.

`INTEGRATED` dominates: a technique used in a combination day is `INTEGRATED` even if its standalone use count is low. That is what "used as one move" means.

### 2.1 Next-level hint

The result screen and Technique detail show a one-line hint. Rendered from the current level:

| Level | Hint (string resource, with the gap filled in) |
| --- | --- |
| `MET` | "Practice it on {3 - distinctDays} more days to reach Practiced." |
| `PRACTICED` | "{6 - totalUses} more uses, spread over 2 weeks, to reach Applied." |
| `APPLIED` | "Use it inside a combination day to reach Integrated." |
| `INTEGRATED` | "You use this one without thinking about it." |

Technique detail renders a factual line plus the hint - "6 sprints on 4 days." then "Use it inside a combination day to reach Integrated." The factual line is `technique_practice_summary` with `{uses}` and `{days}`.

### 2.2 Queries

```sql
-- distinct practice days
SELECT COUNT(DISTINCT practiceDate) FROM plan_activity
 WHERE techniqueId = :id AND state = 'COMPLETED';

-- total uses / first / last
SELECT COUNT(*), MIN(practiceDate), MAX(practiceDate) FROM plan_activity
 WHERE techniqueId = :id AND state = 'COMPLETED';

-- used in combination
SELECT EXISTS(SELECT 1 FROM plan_activity
 WHERE techniqueId = :id AND state = 'COMPLETED' AND source = 'COMBINATION');
```

`plan_activity(techniqueId, practiceDate)` is indexed, so all three are index-only scans.

For the combination case each step of a combination day is persisted as its **own** `plan_activity` row with `source = COMBINATION`, so per-technique credit falls out of the same query. The chain is held together by a parent `COMBINATION` activity; children carry `orderIndex` within it.

## 3. Skill progress

One skill per technique (R-03), so a skill's practice set is the completions of its 2-4 techniques.

**Source: `design/app/src/main/java/com/itera/app/ui/screens/Progress.kt`** (D-09). The thresholds previously in this section were reverse-engineered to reproduce a single `Itera.html` artboard; that artboard is obsolete, so the derivation had no source. The prototype's rule replaces it.

Window: the trailing **14 days**, which is also what the prototype's day strip spans (`span = daysSinceStart + 1`, capped at 14).

```kotlin
data class SkillFacts(
    val practiceCount: Int,   // completions in the window
    val activeDays: Int       // distinct days in the window with >= 1 completion of this skill
)

fun skillLevel(f: SkillFacts): SkillLevel {
    val score = f.practiceCount + f.activeDays
    return when {
        score >= 20 -> SkillLevel.STRONG
        score >= 10 -> SkillLevel.STEADY
        score >= 4  -> SkillLevel.BUILDING
        score >= 1  -> SkillLevel.STARTING
        else        -> SkillLevel.STARTING
    }
}
```

The score deliberately sums depth and regularity, so ten practices crammed into one day scores less than six practices across six days.

`score == 0` and `score in 1..3` both render as **Starting**; the distinction exists only so the bar shows one filled segment once the user has practised at all.

### 3.1 Rendering

**Four** segments, one per band (not nine). Each: `weight(1)`, height 8, `CircleShape`, track in the skill **container** colour, fill in the skill **content** colour, animating `tween(700, delayMillis = 80 * index)`. Segment `i` is filled when `i < level.ordinal + 1`.

Header: skill name in `label` on the left, level name in `bodySmall` SemiBold in the skill content colour on the right, bottom-aligned.

Detail line: `pluralStringResource(R.plurals.practices, count)` in `caption` `ink2`.

## 4. Progress summary

**Source: the prototype's `ProgressScreen`.**

| Element | Definition |
| --- | --- |
| "{n} of {span} days" | distinct dates in the window with >= 1 `COMPLETED` activity. `span` = days since the program started, plus one, capped at **14** |
| "since {date}" | the program start date, formatted `"d MMM"` in the current locale |
| Day strip | one **bar** per day in the window - not a dot. `weight(1)`, height 28, radius 8, `ink` when practised and `surface2` otherwise, with a 2 dp `accent` border on today (D-07) |
| Focus minutes | `SUM(focus_session.actualSeconds) / 60`, all time and per window |

The line under the strip is fixed: "Missed days don't reset anything. The program simply continues where you left off." Static, never conditional - making it conditional would turn it into a reproach.

## 5. What Progress must never show

Enforced by review and by a UI test that asserts these strings are absent:

- a streak count or a "days in a row" phrase;
- a total points / XP figure;
- a percentage-complete of the whole program;
- a comparison with other users;
- any red or warning treatment for a missed day.

## 6. Performance

Progress is a single `combine` of six `Flow`s, each backed by an indexed aggregate query. Measured target: first frame of the Progress screen within **300 ms** on a mid-range device with 2 years of data (~1 500 activities). If a query exceeds that, add a `@DatabaseView`, not a cached counter.

## 7. Tests

`MasteryTest`: the full truth table for `masteryOf`, including the `INTEGRATED` override and the 14-day span boundary (13 days -> `PRACTICED`, 14 -> `APPLIED`). Note the prototype drops the span check (P-02); the product rule keeps it.

`SkillLevelTest`: each band boundary from both sides - score 0, 1, 3, 4, 9, 10, 19, 20 - and a case proving that concentrating practices into one day scores lower than spreading them.

`ProgressSummaryTest`: window arithmetic across a month boundary, a DST change, the window cap at 14, and an empty database (all `STARTING`, 0 of 1 days, no crash).
