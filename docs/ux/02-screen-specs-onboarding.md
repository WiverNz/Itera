# Screen specs: onboarding

**Source: `design/app/src/main/java/com/itera/app/ui/screens/Onboarding.kt`.** Issue 016 (language picker: issue 041).

> **Open the prototype file named under each screen and reproduce it.** This document records what it does so a reviewer can check the work; the prototype is what the code must match (`docs/00-source-of-truth.md` section 2).

---

## 1. Welcome - `WelcomeScreen`

`ScreenColumn` with the default gap of 20 and a bottom block.

| Element | Spec |
| --- | --- |
| Brand row | 32 dp `ink` square, radius 10, `OnePercent` icon at 18 in `onInk`; app name in `headline` with `padding(start = 10)`, weight 1; `LanguagePill` on the right |
| `LanguagePill` | `heightIn(min = 44)`, `CircleShape`, `surface2`, padding h14, `spacedBy(6)`, `Globe` icon 18 + the current language code uppercased in `bodySmall` SemiBold. `contentDescription` = "Language: {native name}" |
| `RisingPath` | `fillMaxWidth().aspectRatio(1.3f)`. A dashed cubic path in `ink3`, stroke 2, `dashPathEffect(floatArrayOf(3f, 14f))`, round cap. Five `TechniqueToken`s positioned by fraction: TwoMinute (0.05, 0.73), Pomodoro (0.26, 0.56), Feynman (0.47, 0.39), HabitStack (0.68, 0.26), DailyReflection (0.86, 0.06). Sizes 56, last one 64; radius 20. Each pops in after `delay(120 * index)` with `spring(MediumBouncy)` from scale 0.4 |
| Headline | `hero` (42) |
| Body | `bodyLarge` in `ink2` |
| Bottom | `IteraButton` "Get started" (primary), then "Explore with demo data" (ghost) |

There is **no sign-in affordance** anywhere.

"Explore with demo data" loads the Day-9 demo state and jumps to Today. It ships in **debug builds only** (D-11).

## 2. Choose goals - `GoalsScreen`

Header: `OnboardingHeader(step = 1)`.

| Element | Spec |
| --- | --- |
| `OnboardingHeader` | `spacedBy(16)`: `CircleIconButton(Back)`, then three progress segments (weight 1, height 4, `CircleShape`, `line` track, `ink` fill animated by `animateFloatAsState`, `spacedBy(6)`), then "{step} of 3" in `caption` SemiBold `ink2` |
| Title | `title` (30) |
| Sub | `body` in `ink2` |
| Option rows | `spacedBy(10)`. Each: `heightIn(min = 72)`, radius 20, `surface`, 2 dp border (`ink` when selected, transparent otherwise), padding h16 v12, `spacedBy(14)`, `Role.Checkbox`. Leading 40 dp tile radius 13 in the skill container with a 12 dp circle of the skill content colour. Title `label`, description `bodySmall` `ink2`. `RadioDot(true)` trailing when selected |
| `RadioDot` | 26 dp circle, `ink` fill and `ink` border when on, `line` border when off, 15 dp `Check` in `onInk` |
| Bottom | "Continue", `enabled = focusSkills.isNotEmpty()` |

Order is the `Skill` enum order: Focus, Planning, Learning, Habits, Reflection.

**Selection rule**: maximum two. Selecting a third removes the oldest (`while (focusSkills.size > 2) removeAt(0)`). **Default: Focus and Learning pre-selected** (D-05), so Continue starts enabled.

## 3. Daily rhythm - `RhythmScreen`

Header: `OnboardingHeader(step = 2)`.

| Element | Spec |
| --- | --- |
| Title | `title` |
| Time card | `IteraCard(padding = 16, gap = 8)` containing `TimeRow` (morning), `Divider()`, `TimeRow` (evening) |
| `TimeRow` | `heightIn(min = 64)`, `spacedBy(14)`: 40 dp tile radius 13 on `surface2` with a 20 dp icon; title `body` SemiBold + sub `bodySmall` `ink2`; trailing time chip `heightIn(min = 44)`, radius 14, `surface2`, padding h14, value in `headline` SemiBold |
| Budget question | `label` |
| `Segmented` | `5 min / 15 min / 30+ min`, default **15** |
| Hint | `bodySmall` `ink2` |
| Bottom | "Continue", always enabled |

Icons: `Today` for morning, `Reflection` for evening.

**Production uses a real Material 3 `TimePicker` in a bottom sheet** (D-17), styled with the same container treatment as `LanguageSheet`. The prototype's tap-to-add-30-minutes interaction is scaffolding and must not ship. The row, its 40 dp tile and its value chip are reproduced exactly; only what happens on tap changes.

The chip displays the time via `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)`, which already honours the system 12/24-hour preference (D-16).

**Production adds** the notification rationale card below the time card, shown only when the permission is not granted, and requests `POST_NOTIFICATIONS` on Continue - advancing either way (issue 033).

## 4. First week - `FirstWeekScreen`

Header: `OnboardingHeader(step = 3)`. `ScreenColumn(gap = 18)`.

| Element | Spec |
| --- | --- |
| Title | `title` |
| Sub | `body` `ink2` |
| Day list | `spacedBy(2)`, rendered from `Program.days.take(7)` - never hard-coded |
| Day 1 row | radius 20 on `surface`, padding h14 v12, `spacedBy(14)`: day label in `caption` Bold `accent` with `width(52)`, `TechniqueToken(40)`, title `label` + skill `caption` `ink2`, trailing `Pill("Today", accentSoft, accent)` |
| Days 2-7 | `heightIn(min = 44)`, padding h14, `spacedBy(14)`: day label `caption` SemiBold `ink2` `width(52)`, an 8 dp skill-coloured dot centred in a 40 dp box, technique name in `body`. Day 4 appends the review suffix |
| Footer | `Reflection` icon 18 + "Every evening: a two-minute reflection" in `bodySmall` `ink2`, padding h14 |
| Bottom | "Start Day 1" |

On "Start Day 1" production performs the real transaction (persist preferences, seed unlocks, generate the plan, schedule reminders) and then navigates. If plan generation fails it still navigates - Today generates its own.

## 5. Language picker - `LanguageSheet`

Reachable from Welcome's `LanguagePill` and from You > Language & region. Full spec in `docs/i18n/00-localization.md`.

| Element | Spec |
| --- | --- |
| Container | `ModalBottomSheet(skipPartiallyExpanded = true)`, `containerColor = surface`, `scrimColor = scrim` |
| Padding | h20, bottom 28, `spacedBy(16)` |
| Title / sub | `title` / `bodySmall` `ink2` |
| Rows | Five: Match device, English, Русский, Deutsch, Español. `heightIn(min = 64)`, `Role.RadioButton`, `spacedBy(14)`: 40 dp tile radius 13 on `surface2` holding either the `Globe` icon at 20 or the two-letter code in `caption` Bold; title `label` + subtitle `bodySmall` `ink2`; `RadioDot` trailing |
| Divider | After the "Match device" row only |
| Bottom | `IteraButton` "Done" |

Each row's title is the language **in that language**; the subtitle is the same name **in the current UI language**. Selecting applies immediately - the sheet and the screen behind it re-render in the new language before the sheet is dismissed.

## 6. Cross-cutting

- One `OnboardingViewModel` scoped to the onboarding graph; values written to DataStore on "Start Day 1", except the language (applied immediately) and the notification permission result.
- Back preserves entered values.
- Process death returns to Welcome with selections preserved (`SavedStateHandle`).
- Onboarding is unreachable once completed, except after "Erase everything".
- The progress segments are decorative; "{step} of 3" carries the information for screen readers.
