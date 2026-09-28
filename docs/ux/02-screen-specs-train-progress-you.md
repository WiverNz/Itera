# Screen specs: Train, Progress, You

**Sources: `design/app/src/main/java/com/itera/app/ui/screens/Train.kt`, `Progress.kt`, `Profile.kt`.** Issues 028-032, 034.

> **Open the prototype file named under each screen and reproduce it.** This document records what it does so a reviewer can check the work; the prototype is what the code must match (`docs/00-source-of-truth.md` section 2).

---

## 1. Train - `TrainScreen` (Train.kt)

`ScreenColumn(gap = 18)`. Bottom bar visible.

| Element | Spec |
| --- | --- |
| Title | "Train" in `display` |
| Sub | `body` `ink2` |
| Week block | `spacedBy(10)`: `SectionTitle("Week {n} - {name}", "Day {d} of 14")`, then `ProgressBar(programDay / 14f, ink)` |
| Week names | 1 Foundations, 2 Depth, 3+ Combine. `week = (programDay - 1) / 7 + 1` |
| Day rows | `spacedBy(2)`, showing the seven days of the current week |
| Today's row | radius 20 on `surface`, clickable to Today, padding h14 v12, `spacedBy(14)`: day label in `caption` Bold `accent` at `width(52)`, `TechniqueToken(40)`, a column with the technique name in `label` and "Today · {Skill}" in `caption` `ink2`, trailing `Chevron` 18 `ink2` |
| Past rows | `heightIn(min = 44)`, padding h14: day label `caption` SemiBold `ink2` at `width(52)`, a 20 dp `Check` in the skill colour inside a 40 dp box, name in `body` `ink2`. Clickable to that technique's detail |
| Future rows | Same, but an 8 dp skill dot instead of the check, name in `body` `ink`, not clickable |
| Review card | Shown when a review is due. `fillMaxWidth`, radius 20, **Learning container background**, padding h16 v14, `spacedBy(14)`: `TechniqueToken(Spaced, 40)`, a column with "Review due today" in `caption` SemiBold in the Learning content colour and the item in `body` SemiBold `ink`, trailing `Chevron` |
| Library row | `LinkRow(Eisenhower icon, "Library", "All {n} techniques · {m} unlocked")` |
| `LinkRow` | `heightIn(min = 60)`, radius 16, `spacedBy(14)`: 40 dp tile radius 13 on `surface2` with a 20 dp icon, a column with the title in `body` SemiBold and the sub in `bodySmall` `ink2`, trailing `Chevron` 18 |

Production adds bounded auto-scroll to today's row on first composition (non-animated), loading skeletons, and a real due-review query instead of the prototype's placeholder condition.

## 2. Library - `LibraryScreen` (Train.kt)

Full-screen, no bottom bar. `ScreenColumn(gap = 18)`.

| Element | Spec |
| --- | --- |
| `TopBar` | empty title, **Back** |
| Title / sub | `display` / `body` `ink2` |
| Filter row | `Modifier.horizontalScroll(...)`, `spacedBy(8)`: "All" plus one `ChoiceChip` per skill, single-select |
| Rows | For each technique: `heightIn(min = 72)`, clickable, `alpha = 0.62f` when locked, `padding(vertical = 10)`, `spacedBy(14)`: `TechniqueToken(44)`, a column (`spacedBy(2)`) with the name in `body` SemiBold and the short description in `caption` `ink2`, then the trailing state. A `Divider()` after every row |
| Locked trailing | `Lock` icon 14 `ink2` + "Day {n}" in `caption` SemiBold `ink2`, `spacedBy(4)` |
| Unlocked trailing | `MasteryDots`: four 7 dp circles `spacedBy(3)`, filled in the skill colour up to the level and `line` beyond, with the level name below in `caption` at 0.85x size, `ink2`, end-aligned, `spacedBy(5)` |

Sort: `compareBy({ !isUnlocked }, { unlockDay ?: 0 })` - unlocked first, then by unlock day.

All 14 are always listed, including locked ones, and locked rows are tappable.

## 3. Technique detail - `TechniqueDetailScreen` (Train.kt)

Full-screen. `ScreenColumn(gap = 22)`.

| Element | Spec |
| --- | --- |
| `TopBar` | empty title, **Back** |
| Token | `TechniqueToken(64)` |
| Eyebrow | skill name in the skill content colour |
| Title | `hero` |
| Short | `bodyLarge` `ink2` |
| Why block | `spacedBy(8)`: "Why it helps" in `label`, then `technique.why` in `body` `ink` |
| Level card | `IteraCard(padding = 18)`: a row with "Your level" in `body` SemiBold (weight 1) and a `Pill` of the level (or "Not started"); `MasteryLadder`; then either the next-level hint or, when locked, "Unlocks on day {n}", in `caption` `ink2` |
| Primary | "Practice now · {n} min" with the `Play` icon |
| History | "Your practice" in `label` with `padding(bottom = 4)`; up to five entries, each a row with `padding(vertical = 12)` and `spacedBy(14)`: the date in `bodySmall` `ink2` at `width(110)`, then the note in `bodySmall` `ink` (em dash when blank), followed by a `Divider()`. Empty state: one `bodySmall` `ink2` line with `padding(vertical = 8)` |
| Related | "Works well with" in `label`, then a `FlowRow(spacedBy(8))` of pills: `CircleShape` on `surface`, padding start 6 / end 14 / v6, `spacedBy(10)`, `TechniqueToken(40, radius 20)` + name in `bodySmall` SemiBold |

Related lists come from `Technique.related` in `model/Model.kt` and may include locked techniques.

Production: "Practice now" creates a `MANUAL` activity and opens the runner directly; the locked state hides the practice button and the history section.

## 4. Progress - `ProgressScreen` (Progress.kt)

`ScreenColumn(gap = 16)`. Bottom bar visible.

| Element | Spec |
| --- | --- |
| Title | "Progress" in `display` |
| Summary card | `IteraCard(padding = 18, gap = 12)` |
| Summary row | Bottom-aligned: "{practiced} of {span} days" in `headline` (weight 1), then "since {date}" in `caption` `ink2`. `span` grows with the program and caps at **14** |
| Day strip | `Row(spacedBy(4))`, one bar per day in the window: weight 1, **height 28**, radius 8, `ink` when practised and `surface2` otherwise, with a 2 dp `accent` border on today |
| Reassurance | The missed-days line in `caption` `ink2`. Always shown |
| Skill rows | One per skill, `padding(vertical = 10)`, `spacedBy(8)` |
| Skill header | Bottom-aligned: skill name in `label` (weight 1), level name in `bodySmall` SemiBold in the skill content colour |
| Skill bars | `Row(spacedBy(4))` of **four** segments: weight 1, height 8, `CircleShape`, skill container track, filled with the skill content colour, animating `tween(700, delayMillis = 80 * index)` |
| Skill detail | `pluralStringResource(practices, count)` in `caption` `ink2` |
| Links | `LinkRow` to History, then `LinkRow` to Library |

Level: `score = practiceCount + distinctActiveDays`; `>= 20` Strong, `>= 10` Steady, `>= 4` Building, `>= 1` Starting, `0` Starting (D-09).

**Forbidden on this screen**: streaks, points, percentage-complete, comparisons, or any warning colour for a missed day. Asserted by a UI test.

## 5. History - `HistoryScreen` (Progress.kt)

Full-screen. `ScreenColumn(gap = 16)`.

| Element | Spec |
| --- | --- |
| `TopBar` | empty title, **Back** |
| Title | "History" in `display` |
| Month | `"LLLL yyyy"` in the current locale, first letter title-cased, in `headline` |
| Weekday row | Seven `NARROW` weekday names, each weight 1, `caption` SemiBold `ink2`, centred. **First day of week comes from `WeekFields.of(locale).firstDayOfWeek`** - not hard-coded to Monday |
| Grid | Rows of seven, `spacedBy(4)` both axes. Each cell `weight(1).aspectRatio(0.95f)` |
| Cell | `padding(1)`, radius 12, `surface` background when the day has activity, a 2 dp `ink` border on today, `padding(vertical = 6)`, `spacedBy(4)`, centre-aligned: the day number in `bodySmall` (SemiBold when active, `ink3` when in the future), then a 5 dp-tall row of up to **three** 5 dp skill dots, `spacedBy(2)` |
| Cell semantics | `contentDescription` = the full localized date |
| Legend | `FlowRow(spacedBy(12) / spacedBy(6))` of five entries: an 8 dp skill dot + the skill name in `caption` `ink2` |
| Log | Day by day, newest first, walking back to the oldest entry. Each day: the full localized date in `caption` SemiBold `ink2` with `padding(top = 8)`; then either its entries or the rest-day line in `bodySmall` `ink2`; then a `Divider()` |
| Entry | `spacedBy(14)`: `TechniqueToken(40)`, a column with the technique name in `body` SemiBold and, when present, the note in `bodySmall` `ink2` |

**Rest days are rendered, not skipped.**

Production adds bounded month navigation (P-06) - previous/next chevrons in the month row, disabled at the program start and the current month - and an empty-month line.

## 6. You - `ProfileScreen` (Profile.kt)

`ScreenColumn(gap = 22)`. Bottom bar visible.

| Element | Spec |
| --- | --- |
| Title | "You" in `display` |
| Identity row | `spacedBy(14)`: a 56 dp circle in the Focus container with a 26 dp `You` icon in the Focus content colour; a column with the name in `label` and "Training since {date} · Day {n}" in `bodySmall` `ink2` |
| `Group` | `spacedBy(8)`: an uppercase section title in `eyebrow` `ink2` with `padding(start = 4)`, then a container - `fillMaxWidth`, radius 20, `surface`, `padding(horizontal = 16)` - holding the rows |
| `ValueRow` | `heightIn(min = 56)`, clickable, `spacedBy(12)`: label in `body` Medium (weight 1), value in `bodySmall` `ink2`, trailing `Chevron` 16 in **`ink3`** |
| `SwitchRow` | `heightIn(min = 56)`, `alpha = 0.55f` when disabled, `spacedBy(12)`: a column with the label in `body` Medium and an optional sub in `caption` `ink2`, then a `Switch` with `checkedTrackColor` = Habits content, `checkedThumbColor` = `surface`, `uncheckedTrackColor` = `surface2`, `uncheckedThumbColor` = `ink3`, `uncheckedBorderColor` = `line` |
| Rows are separated by `Divider()` | |

### Sections, in order

| Section | Rows |
| --- | --- |
| **Daily rhythm** | Morning time, Evening time, Time on most days |
| **Program** | A pace block - label in `body` Medium, hint in `caption` `ink2`, then a `Segmented` (Gentle / Standard / Intense) with `padding(top = 4)`, the whole block padded `top = 14, bottom = 14`, `spacedBy(6)` - then a Focus areas `ValueRow` |
| **Language & region** | Language (opens `LanguageSheet`; value is the current language in its own language). **Time format** - a **read-only** row: no chevron, not clickable, showing "24-hour" or "12-hour" as resolved from `DateFormat.is24HourFormat(context)`, with a caption naming Android settings as where to change it. There is no in-app override (D-16) |
| **Appearance** | A theme block padded `vertical = 14`, `spacedBy(8)`: label in `body` Medium, then a `Segmented` (System / Light / Dark) |
| **Notifications** | Four `SwitchRow`s: morning exercise, focus suggestions, review reminders, evening reflection |
| **Voice** | ADR-0022 (amended 2026-09-28): `SwitchRow` "Use system speech recognition" (sub: only without on-device recognition; the system service may process audio remotely), and, when installed apps offer recognition or one is chosen, `ValueRow` "Speech recognition app" (chosen app's label or "None") opening a sheet with None and each app; choosing an app shows its named consent before it is stored. See [voice UX](10-voice-input.md#settings) |
| **Coach** | One **disabled** `SwitchRow`: "AI feedback on explanations", sub "Coming later" |
| **Data** | Export journal ("Markdown"), Privacy |
| Footer | "Load demo data" as a `Secondary` `IteraButton` at `height = 48` - **debug builds only** (D-11) |

Production adds: real Material 3 time pickers (D-17), a focus-areas picker, the Reset program and Erase everything rows in the Data section, the denied-notification-permission state, and a working export and privacy screen. Everything keeps the row styling above.

Milestone 012 extends the existing Privacy screen with microphone use, transient speech handling, local storage of accepted text and manual input when voice is unavailable. Since the ADR-0022 amendment it also states that recognition is on-device where the device offers it, and otherwise, only with consent, uses Android's system speech service or a recognition app the user chose, which may process audio remotely; both can be changed or turned off in the Voice settings section. No separate voice-settings screen; see [voice UX](10-voice-input.md) and ADR-0022.

The **Language** row is the primary runtime language switch and is built in issue `034`, not deferred to the audit issue (D-15).
