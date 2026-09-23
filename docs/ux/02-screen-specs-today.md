# Screen specs: Today

**Source: `design/app/src/main/java/com/itera/app/ui/screens/Today.kt`.** Issue 017.

> **Open the prototype file named under each screen and reproduce it.** This document records what it does so a reviewer can check the work; the prototype is what the code must match (`docs/00-source-of-truth.md` section 2).

Today answers one question: what should I do right now? One hero card with a single action, then a short list of the day's steps.

## 1. Anatomy

`ScreenColumn(gap = 22)`, no bottom block.

```
Header row        greeting (display, weight 1) | Pill("Day N", surface2, ink)   [Alignment.Bottom]
Carry-over        optional: icon 18 + annotated text
Hero card         IteraCard(radius 28, padding 22, gap 14)
Section           SectionTitle("Today", "{done} / {total}")
Steps             StepRow x N, spacedBy(4)
```

## 2. Header

| Field | Rule |
| --- | --- |
| Greeting | `hour < 12` morning, `hour < 18` afternoon, else evening. `display` style |
| Day pill | `Pill(stringResource(R.string.day_n, programDay), surface2, ink)` |

Both are bottom-aligned in one `Row`.

## 3. Carry-over

Shown when the previous day's reflection recorded a change. `Row(spacedBy(10))`: `Reflection` icon at 18 tinted with the Reflection skill colour and `padding(top = 1)`, then an `AnnotatedString` whose label span is `ink` SemiBold and whose body is the user's own text, all in `bodySmall` `ink2`.

Not tappable.

## 4. Hero card

`HeroCard` is a private composable in `Today.kt`: `IteraCard(radius = 28, padding = 22, gap = 14)` containing an eyebrow row (`Eyebrow` weight 1 + optional `Pill`), a 60 dp `TechniqueToken`, the title in `display`, a line in `bodyLarge` `ink2`, an optional meta line in `bodySmall` `ink2`, and an `IteraButton` with `padding(top = 4)`.

**Selection**, in order (D-06):

| Condition | Eyebrow | Badge | Token | Title | CTA |
| --- | --- | --- | --- | --- | --- |
| Exercise not done, normal day | "Today's training" in the technique's skill colour | "New" in `accentSoft`/`accent` | the technique | technique name | "Start today's exercise" |
| Exercise not done, combination day | "Combination" | - | `Pareto` | combination title | "Start today's exercise" |
| Focus not done **and** `hour < 18` | "Next up - optional" in the Focus colour | - | `DeepWork` | "{n}-minute focus" | "Start focus session" |
| Reflection not done | "Evening" in the Reflection colour | - | `DailyReflection` | "Evening reflection" | "Start reflection" |
| Everything done | "Day N" in `ink2` | - | `OnePercent` | day-done title | day-done CTA -> Day complete |

The eyebrow always carries the hero's skill colour. The focus branch is gated on the hour so the app stops suggesting a focus block in the evening.

**Production addition (P-01)**: when a review is due it takes priority over the focus branch, using the Learning skill colour and the review's prompt.

## 5. Steps

`SectionTitle("Today", "{completed} / {total}")` then one `StepRow` per step, `spacedBy(4)`.

The prototype has exactly three: exercise, focus, reflection, and a hard-coded denominator of 3. **Production** renders one row per activity in the day's plan and uses the real total, so a due review adds a fourth row (P-01). Everything else about the rows is unchanged.

| Row | `StepState` | Subtitle when not done |
| --- | --- | --- |
| Exercise | `Done` if complete, else `Now` | "Now - {n} min" |
| Focus | `Done`, else `Now` if the exercise is done, else `Next` | "Suggested around {11:00}" |
| Reflection | `Done`, else `Now` if the exercise is done and (focus is done or `hour >= 18`), else `Next` | "{evening time} - 2 min" |

A completed row's subtitle is the user's note, falling back to "Done". Completed rows are not clickable and their title is struck through in `ink2` (`StepRow` handles this).

The exercise row's accent is the technique's skill colour; focus uses Focus; reflection uses Reflection.

## 6. States

| State | Behaviour |
| --- | --- |
| Loading | Header renders immediately from the clock and the stored program day. Hero and steps show `surface2` skeletons at the same sizes, after 150 ms. No spinner |
| Content | As above |
| Day complete | The fifth hero branch. No new work is offered |
| Error | The hero is replaced by `ErrorState` ("Couldn't build today's plan." + "Try again"); any stored steps still render |
| Empty | Cannot occur - a plan always has a reflection. Treat as Error |

Pull-to-refresh is not implemented.

## 7. Accessibility

- Greeting is a heading.
- The hero card merges into one node ending with its button as a separately focusable child.
- Each `StepRow` merges to "{title}, {subtitle}, {state}".
- The counter is announced as "{n} of {m} done", not "n / m".
- The pulsing dot in `StepDot(Now)` is decorative and honours reduced motion.

## 8. Performance

`ensureTodayPlan()` runs in `init`; the header does not wait for it. Budget: header visible within 300 ms of a warm start.
