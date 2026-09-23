# Screen specs: exercise flow

**Sources: `design/app/src/main/java/com/itera/app/ui/screens/Exercise.kt`, `Focus.kt`, `Reflection.kt`, `Practice.kt`.** Issues 018, 021-027.

> **Open the prototype file named under each screen and reproduce it.** This document records what it does so a reviewer can check the work; the prototype is what the code must match (`docs/00-source-of-truth.md` section 2).

---

## 1. Exercise intro - `ExerciseIntroScreen` (Exercise.kt)

`ScreenColumn(gap = 22)` with a bottom block.

| Element | Spec |
| --- | --- |
| `TopBar` | "Exercise", close icon |
| Token | `TechniqueToken(technique, 64)` |
| Eyebrow | "{Skill} - Day {n}" in the skill content colour |
| Title | technique name, `hero` (42) |
| Why | `technique.why`, `bodyLarge` `ink2` |
| Card | `IteraCard(radius = 28, padding = 22)`: `Eyebrow("Your exercise")` then `technique.task` in `headline` SemiBold |
| Primary | "Start exercise" with the `Play` icon - or, for `ExerciseKind.Generic`, "I did it" with the `Check` icon |
| Secondary | "Not now" as a `Ghost` button at `height = 48` |

Closing leaves the activity available; it is not a skip. Production wires the secondary to a real snooze (next whole hour at least two hours out, capped at 20:00).

## 2. 2-minute rule - `TwoMinuteScreen` (Exercise.kt)

`ScreenColumn(gap = 22)`.

| Element | Spec |
| --- | --- |
| `TopBar` | technique name, **Back** icon, trailing `Pill("{done} / {total}", skill container, skill content)` |
| Title / sub | `title` / `body` `ink2` |
| Task rows | `spacedBy(10)`. Each: `heightIn(min = 68)`, radius 20, `surface`, `Role.Checkbox`, padding h16 v12, `spacedBy(14)`. Leading `CheckCircle`, then the label in `body` Medium, struck through and `ink2` when checked |
| `CheckCircle` | 30 dp circle, filled with the skill colour and 2 dp border when checked (`line` border otherwise), 16 dp `Check` in `surface` |
| Add row | `NoteField(minLines = 1)` with the add-task hint |
| Primary | "Finish", `enabled = done > 0` |

Production replaces the two sample tasks with user-entered ones and adds the per-item stopwatch described in `docs/data/00-domain-model.md`.

## 3. Exercise result - `ExerciseResultScreen` (Exercise.kt)

`ScreenColumn(gap = 20)`.

| Element | Spec |
| --- | --- |
| `AnimatedCheck` | 96 dp circle in the skill container, centre-aligned, `padding(top = 24)`. Scale animates 0.5 -> 1 with `spring(MediumBouncy, StiffnessLow)`, then a `Canvas` draws the check path over `tween(450)` using `PathMeasure.getSegment`; stroke width `2.4 * (size / 24)`, round cap and join |
| Title | `title`, centred, `fillMaxWidth` |
| Sub | `body` `ink2`, centred |
| "How did it feel?" | `label` |
| Feeling row | `spacedBy(8)`, three options each weight 1, `heightIn(min = 48)`, radius 16, `surface`, 2 dp `ink` border when selected, `Role.RadioButton`, label `body` SemiBold. No default selection |
| Note | `NoteField` with the result hint |
| Mastery card | `IteraCard(padding = 18, gap = 12)`: a row with the technique name in `body` SemiBold (weight 1) and a `Pill` showing the level; then `MasteryLadder`; then the next-level hint in `caption` `ink2` |
| Primary | "Done" -> `backToToday()` |

Rating and note save on change, not on "Done".

Production variants: a focus result adds "{n} minutes of {planned} planned" plus the break hint; a review result replaces `MasteryLadder` with `IntervalLadder` and a "next review in {n} days" line.

## 4. Focus timer - `FocusScreen` (Focus.kt)

Always dark: the screen wraps itself in `IteraTheme(dark = true)` and forces light system-bar icons, restoring them on dispose. `keepScreenOn` is bound to `running`.

Layout is a `Column` with `screenInsets()`, `padding(horizontal = 24, vertical = 16)`, `CenterHorizontally`.

| Element | Spec |
| --- | --- |
| Header row | `DeepWork` icon 18 in the Focus colour, "Focus session" in `bodySmall` SemiBold Focus-coloured with `padding(start = 8)` and weight 1, then the notifications note in `caption` `ink2` |
| Spacer | `weight(1f)` above and below the ring block |
| Ring | `Box(300.dp)`. `Canvas`: track arc in `surface2`, full circle, `Stroke(6.dp)`; progress arc in the Focus colour from **-90 degrees**, sweep `360 * progress`, `StrokeCap.Round`. Progress is `animateFloatAsState(1f - left / total, tween(900))` |
| Readout | `"%02d:%02d"` in `timer` (80 sp), `liveRegion = Polite` |
| Status | "remaining" / "paused" in `body` `ink2` |
| Task block | `padding(top = 20)`, `spacedBy(6)`: uppercase "Working on" in `eyebrow` `ink2`, then the task in `headline`, centred |
| Controls | `Row(spacedBy(28))`: `TextControl("+5 min")`, an 84 dp `ink` circle with a 30 dp `Play`/`Pause` icon in `bg`, `TextControl("End")` |
| `TextControl` | `widthIn(min = 72)`, `heightIn(min = 48)`, radius 16, `body` SemiBold `ink2` |
| Break hint | `bodySmall` `ink2`, centred, `padding(top = 20)` |

"+5 min" adds 300 seconds to **both** `total` and `left`, so the ring does not jump.

**Production keeps this layout exactly** and replaces the countdown mechanism with a wall-clock `endsAt` plus a foreground service (P-03, ADR-0009), adds the pre-timer task/duration sheet, an "End session?" confirmation, and announcements only at 5:00, 1:00 and 0:00.

## 5. Evening reflection - `ReflectionScreen` (Reflection.kt)

Night surface. `ScreenColumn(gap = 22)`.

| Element | Spec |
| --- | --- |
| `TopBar` | "Evening reflection", close, trailing `Pill("2 min", surface2, ink2)` |
| Title | `display` |
| Questions | Three, each a `Row(spacedBy(14))` of `StepDot(state, Reflection colour)` and an `AnimatedContent` |
| Transition | `(fadeIn(tween(250)) + slideInVertically { it / 6 }) togetherWith fadeOut(tween(150))` |
| `Done` state | question in `bodySmall` `ink2`, answer in `body` `ink` (em dash when blank) |
| `Now` state | `spacedBy(14)`: question in `headline`; a `FlowRow` of `ChoiceChip`s (`spacedBy(8)` both axes); a `NoteField` |
| `Next` state | question in `body` `ink2` with `padding(top = 3)` |
| Primary | "Next" for steps 1-2, "Done" on step 3 |
| Secondary | "Skip tonight", `Ghost`, `height = 44` |

Chips are per-question and multi-select; toggling one rewrites that answer as the selected chips joined with " · ". Typing into the field overrides it. Both are kept.

Question 3's answer becomes the next morning's carry-over text.

**Production adds**: question 1 pre-filled from the day's real activity (editable, and stored empty if cleared); "Skip tonight" recording a skipped reflection rather than silently closing; the weekly look-back header on days 7, 14, 21.

## 6. Day complete - `DayCompleteScreen` (Reflection.kt)

Night surface. `ScreenColumn(gap = 18)`.

| Element | Spec |
| --- | --- |
| `DayRing` | 160 dp, `padding(top = 20)`, centre-aligned. Track arc `surface2` `Stroke(12.dp)`. Three arcs of `120 - 14` degrees starting at `-90 + index * 120 + 7`, coloured Habits / Focus / Reflection, each animating `tween(500)` in sequence. Centre text "{done}/3" in `display` |
| Title | "Day {n} complete" in `display`, centred |
| Sub | `body` `ink2`, centred |
| Change card | Shown only when the change is non-blank. `IteraCard`: `Eyebrow` in the Reflection colour, the change in `headline` SemiBold wrapped in typographic quotes, then a hint in `bodySmall` `ink2` |
| Tomorrow card | `IteraCard(padding = 16)`: `TechniqueToken(next, 48)` + a column with "Tomorrow - Day {n+1}" in `caption` SemiBold `ink2`, the technique name in `label`, and "Unlocks at {morning time}" in `bodySmall` `ink2` |
| Primary | "Good night" -> `startNextDay()` then `backToToday()` |

The tomorrow card reads the curriculum; it does not generate tomorrow's plan.

## 7. Combination - `CombinationScreen` (Practice.kt)

`ScreenColumn(gap = 20)`.

| Element | Spec |
| --- | --- |
| `TopBar` | "Day {n} - Combination", close, trailing `Pill("70 min", surface2, ink2)` |
| Title / sub | `display` / `body` `ink2` |
| Chain | `Column(spacedBy(18))` of four `ChainStep`s |
| `ChainStep` | `Row(spacedBy(14))`. Leading 36 dp: **Done** = filled skill circle with an 18 dp `Check` in `surface`; **Now** = `TechniqueToken(t, 36, radius 18)`; **Next** = 2 dp `line` circle with the technique icon at 18 in `ink2`. Body column has `padding(top = 4)`, `spacedBy(2)`: `Eyebrow` (technique name, skill colour or `ink2` when Next), title (`label` when Now, `body` SemiBold otherwise), sub in `bodySmall` `ink2`, and an optional inline body with `padding(top = 10)` |
| Step 2 inline body | A radio group: container radius 20 on `surface2` with `padding(6)` and `spacedBy(6)`; each option `heightIn(min = 46)`, radius 14, `surface`, 2 dp `ink` border when selected, padding h12, `spacedBy(12)`, an 18 dp circle whose border is 6 dp `ink` when selected and 2 dp `ink3` otherwise, label `bodySmall` |
| Primary | "Continue to Deep Work" with the `Play` icon |

The Day-14 chain is Eisenhower -> 80/20 -> Deep Work -> Daily reflection; the last step is shown but completed in the evening, not here.

Production persists each step as its own activity with `source = COMBINATION` so each technique reaches `Integrated`, and carries each step's result into the next.

## 8. Eisenhower - `EisenhowerScreen` (Practice.kt)

`ScreenColumn(gap = 16)`.

| Element | Spec |
| --- | --- |
| `TopBar` | technique name, close, trailing `Pill("Planning", ...)` |
| Title | `title` |
| Sort header | A row: "To sort" in `body` SemiBold (weight 1), then the hint in `bodySmall` `ink2` - "Tap a task" or "Now tap a square" depending on whether one is selected |
| Inbox | `FlowRow(Modifier.animateContentSize(), spacedBy(8) both axes)`. Each chip: `heightIn(min = 44)`, radius 14, `ink` fill and `onInk` text when selected, else `surface` with a `line` border; 2 dp border; padding h14; `bodySmall` SemiBold; `Role.RadioButton`. When empty, a single "All sorted" line in `body` SemiBold in the Habits colour |
| Axis labels | A row with `padding(start = 30)`: "Urgent", "Not urgent", each weight 1, `caption` SemiBold `ink2`, centred |
| Side labels | 22 x 168 box with the text rotated -90 degrees and `requiredWidth(168)` |
| Quadrants | Two rows of two, `spacedBy(8)`. Each `QuadrantBox`: `heightIn(min = 168)`, radius 22, padding 12, `spacedBy(8)`, `animateContentSize()`. Title in `headline` at `label`'s font size, sub in `caption` `ink2`, then placed tasks as `bodySmall` Medium chips on `surface` with radius 12 and padding h10 v8 |
| Quadrant colours | Do now: `accentSoft` / `accent`. Schedule: Planning container / content. Delegate: `surface2` / `ink`. Drop: transparent with a dashed `ink3` border - `Stroke(2.dp, dashPathEffect(floatArrayOf(10f, 8f)))`, corner radius 22 |
| Primary | "Continue" |

Interaction is **tap a task, then tap a quadrant**; placing auto-selects the next unsorted task. Quadrants carry `onClickLabel` for TalkBack.

Production starts from the user's own entered tasks rather than four pre-placed samples.

## 9. Feynman - `FeynmanScreen` and `FeynmanFeedbackScreen` (Practice.kt)

### Explain - `ScreenColumn(gap = 16)`

| Element | Spec |
| --- | --- |
| `TopBar` | technique name, close, trailing Learning `Pill` |
| Topic card | `IteraCard(color = Learning container, gap = 10)`: `Eyebrow("Topic")` in the Learning content colour, the topic in `headline`, then a row with "From your learning list" in `bodySmall` `ink2` (weight 1) and a "Change topic" chip - `bodySmall` SemiBold on `surface`, radius 12, padding h12 v8 |
| Instruction | `body` `ink2` |
| Field label | `caption` SemiBold `ink2` |
| Field | `NoteField(minLines = 9, textStyle = bodyLarge, bordered = true)` |
| Footer | word count via `pluralStringResource` (weight 1) and "No notes, no looking it up", both `caption` `ink2` |
| Primary | "Done explaining", `enabled = words > 0` |

Production raises the gate to 30 words with a `stateDescription` explaining the shortfall, and wires "Change topic" to the topic picker.

### Reflect - `ScreenColumn(gap = 18)`

| Element | Spec |
| --- | --- |
| `TopBar` | technique name, **Back** |
| Title | `title` |
| "What was hardest?" | `label`, then a `FlowRow` of `ChoiceChip`s |
| Note | `NoteField` |
| Coach box | `fillMaxWidth`, a dashed rounded rect drawn with `drawBehind`: corner radius 24, `Stroke(2.dp, dashPathEffect(floatArrayOf(12f, 10f)))` in the Learning content colour, `padding(18)`, `spacedBy(14)`. Header row: `Spark` icon 20, "Coach feedback" in `body` SemiBold (weight 1), `Pill("Coming later", Learning container, Learning content)`. Then the body. Then a disabled `Secondary` button at `height = 44` |
| Review note | `Spaced` icon 18 + "You'll explain this again in {n} days" in `bodySmall` `ink2` |
| Primary | "Finish" |

**Locked (Q-01, D-10)**: the coach box **ships** - its dashed border, `Spark` icon, "Coach feedback" header, "Coming later" pill and disabled button are all reproduced. What must **not** ship is the prototype's three sample `FeedbackRow`s of fabricated coaching text; they are replaced by **one** explanatory line saying that written feedback on explanations is coming. No generated or invented feedback is ever rendered until a real provider exists. `FeedbackRow` stays in the codebase, unreachable, for that day.

## 10. Review - `ReviewScreen` (Practice.kt)

`ScreenColumn(gap = 16)`.

| Element | Spec |
| --- | --- |
| `TopBar` | "Review", close, trailing Learning `Pill` |
| Token | `TechniqueToken(Spaced, 48)` |
| Days-ago line | `pluralStringResource`, `body` `ink2` |
| Topic | `title` |
| Prompt | `bodyLarge` `ink2` |
| Field | `NoteField(minLines = 7, textStyle = bodyLarge, bordered = true)` |
| Hidden-answer box | Row, radius 20 on `surface2`, padding 16, `spacedBy(14)`: `Lock` icon 20 `ink2` + the hidden-answer line in `bodySmall` `ink2` |
| `IntervalLadder` | Five nodes at `width(48)` each, `spacedBy(6)` internally: done = filled skill circle with a 13 dp `Check`; current = `surface` circle with a 3 dp skill border; future = 2 dp `line` circle. All 22 dp. Connectors between nodes: weight 1, height 2, `padding(top = 10)`, skill-coloured before the current node and `line` after. Labels in `caption`, Bold on the current node |
| Next-interval line | `pluralStringResource`, `caption` `ink2` |
| Primary | "Compare with my first answer", `enabled = text.isNotBlank()` |

**The previous answer must not be in the composition before the reveal** - it is held privately in the ViewModel and copied into state only on the compare tap. Production then shows both answers and the three grade buttons.

## 11. Premortem - `PremortemScreen` (Practice.kt)

`ScreenColumn(gap = 18)`.

| Element | Spec |
| --- | --- |
| `TopBar` | technique name, close, trailing Reflection `Pill` |
| Project line | `AnnotatedString` in `bodySmall` `ink2` with the project name in `ink` SemiBold |
| Title | "It's {month year}. The project failed completely. Why?" in `title`. The date is **computed** as `LocalDate.now().plusMonths(6)` formatted `"LLLL yyyy"` in the current locale |
| Reasons | `spacedBy(8)`. Each: `heightIn(min = 54)`, radius 16, `surface`, 2 dp `ink` border when focused, padding h14 v8, `spacedBy(12)`: index in `bodySmall` Bold `ink2`, text in `bodySmall` Medium (weight 1), then a likelihood `Pill` - Reflection colours for "Likely", `surface2`/`ink2` for "Possible" |
| Add | `NoteField(minLines = 1)` |
| Question | `headline` with `padding(top = 6)` |
| Action | `NoteField(bordered = true)` |
| Idea | `caption` `ink2` |
| Primary | "Add to today" |

Production makes reasons user-entered, reorderable, minimum 3, and creates a real `MANUAL` activity on "Add to today".

## 12. Habit stacking - `HabitStackScreen` (Practice.kt)

`ScreenColumn(gap = 20)`.

| Element | Spec |
| --- | --- |
| `TopBar` | technique name, close, trailing Habits `Pill` |
| Sentence | "After I {anchor}, I will {habit}." in `title` with `lineHeight = fontSize * 1.4`. Both slots are styled with a `SpanStyle` of the Habits content colour on the Habits container, underlined |
| Sub | `body` `ink2` |
| Group labels | `body` SemiBold |
| Chip groups | Two `FlowRow`s of `ChoiceChip`s, `spacedBy(8)` both axes. Anchors: make coffee, brush my teeth, open my laptop, sit down for lunch. Habits: read one page, write one line, drink a glass of water, plan tomorrow's top task |
| Nudge row | `fillMaxWidth`, radius 20, `surface`, padding h16 v10, `spacedBy(14)`: `Bell` icon 20 `ink2`, label `body` (weight 1), then a `Switch` with `checkedTrackColor` = Habits content, `checkedThumbColor` = `surface`, `uncheckedTrackColor` = `surface2`, `uncheckedBorderColor` = `line` |
| Bottom | The "log it on 3 days" hint in `caption` `ink2`, centre-aligned, above the primary "Save my stack" |

Production adds custom anchor/habit entry, a derived and editable nudge time, and real scheduling.
