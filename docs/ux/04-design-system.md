# Design system

**Source: `design/app/src/main/java/com/itera/app/ui/theme/` and `ui/components/`.** Every value below is transcribed from that code so a reviewer can check work without reading Kotlin. When the two disagree, the prototype is right and this document is stale - fix it.

Porting note: the theme and component packages are the most directly reusable code in the prototype. Copy them, change the package, and keep going.

## 1. Colour

### 1.1 `IteraColors`

Eleven tokens per theme, in `theme/Color.kt`. Exposed as `Itera.colors` via `LocalIteraColors`.

| Token | Light | Dark | Used for |
| --- | --- | --- | --- |
| `bg` | `#F4F2EC` | `#111311` | App canvas, bottom bar |
| `surface` | `#FFFFFF` | `#1B1E1B` | Cards, text fields, raised rows |
| `surface2` | `#EAE7DF` | `#262A26` | Chips, icon buttons, tracks, segmented background |
| `ink` | `#1A1C19` | `#EDECE6` | Primary text, filled-button background |
| `ink2` | `#585C55` | `#A6AAA2` | Secondary text, inactive tab labels, placeholders |
| `ink3` | `#A9ACA4` | `#5C605A` | Disabled glyphs, chevrons, dashed guides |
| `line` | `#E0DCD2` | `#2D312D` | Hairlines, unfilled rings, unselected borders |
| `accent` | `#BF4526` | `#F2825F` | "New" badge text, today's outline, day labels |
| `accentSoft` | `#FBE4DA` | `#3A231B` | "New" badge background, the Do-now quadrant |
| `onInk` | `#F4F2EC` | `#111311` | Text and icons on an `ink` fill |
| `scrim` | `#6B1A1C19` | `#99000000` | Bottom-sheet scrim |
| `isDark` | `false` | `true` | Lets components pick a skill palette |

### 1.2 Skill colours

`Skill.colors(dark: Boolean): SkillColors(container, content)` in `theme/Color.kt`.

| Skill | Light container | Light content | Dark container | Dark content |
| --- | --- | --- | --- | --- |
| Focus | `#E0E9F6` | `#2F5B93` | `#1C2736` | `#93B5E6` |
| Planning | `#F3E6CD` | `#835610` | `#2E2618` | `#E2B669` |
| Learning | `#E9E3F6` | `#5C409A` | `#261F36` | `#B9A3EC` |
| Habits | `#DCEEE2` | `#2D6A42` | `#18291E` | `#88C99E` |
| Reflection | `#F5E0E0` | `#983A45` | `#321C1F` | `#EA9CA4` |

Applied to: technique tiles, eyebrows, step dots, mastery bars, skill rows, calendar dots, switch tracks, the focus ring.

### 1.3 Material 3 mapping

`IteraTheme` builds an M3 `ColorScheme` from the tokens:

```
primary = ink            onPrimary = onInk        background = bg          onBackground = ink
surface = surface        onSurface = ink          surfaceVariant = surface2
onSurfaceVariant = ink2  surfaceContainerLow = surface
surfaceContainerHigh = surface                    outline = line
secondary = accent       scrim = scrim
```

Dynamic colour is off. Skill colours are **not** in `ColorScheme`; they are content colours reached through `Skill.colors(isDark)`.

### 1.4 Always-dark screens

The focus timer wraps itself in `IteraTheme(dark = true)` regardless of the app theme, and restores the system-bar icon appearance on dispose. Evening reflection and Day complete follow the same rule (R-08).

## 2. Typography

`theme/Type.kt`. Eleven styles, exposed as `Itera.type`.

### 2.1 Four bundled faces

The two brand faces do **not** cover Cyrillic, so a second Cyrillic-capable pair is bundled and selected deterministically (D-14, ADR-0019). All four are variable and OFL, and all four ship in the APK - there is no runtime download and no system fallback.

| Role | Latin locales (en, de, es) | Cyrillic locales (ru) |
| --- | --- | --- |
| Display | **Bricolage Grotesque** (Medium / SemiBold / Bold) | **Inter Tight** |
| Body | **Instrument Sans** (Regular / Medium / SemiBold / Bold) | **Inter** |

Measured coverage of the brand faces: Bricolage Grotesque ships `vietnamese`, `latin-ext`, `latin`; Instrument Sans ships `latin-ext`, `latin`. That covers `ä ö ü ß` and `á é í ó ú ñ ¿ ¡`; it does not cover U+0400-04FF.

### 2.2 Selection rule

Applied once, in `rememberIteraType()`:

1. **Chrome** - resolve from the active locale's script. Cyrillic script uses Inter Tight / Inter; everything else uses the brand faces.
2. **User-authored text** - **always Inter**, in every locale. The app cannot know what script someone types in, so note fields, reflection answers, explanations, task labels and history entries always use the widest-coverage face.

Both projects load bundled variable fonts from `res/font/` by resource ID. Missing resources fail the build. Issue 003 implements script-aware selection and `FontCoverageTest`; no silent fallback is permitted in production.

### 2.3 The scale

| Style | Family | Size | Weight | Line height | Letter spacing | Used for |
| --- | --- | --- | --- | --- | --- | --- |
| `hero` | Display | 42 | Bold | 44 | -0.02em | Welcome headline, exercise intro title, technique detail title |
| `display` | Display | 34 | Bold | 36 | -0.02em | Screen titles, hero card title, day-complete title |
| `title` | Display | 30 | Bold | 33 | -0.02em | Flow-screen titles, result title, sheet titles |
| `headline` | Display | 22 | Bold | 27 | -0.01em | Section headlines, card questions, time values |
| `bodyLarge` | Body | 18 | Normal | 26 | 0 | Hero subtitle, intro explanation, long text fields |
| `body` | Body | 16 | Normal | 23 | 0 | Default body, list titles, field text |
| `bodySmall` | Body | 14 | Normal | 20 | 0 | Secondary text, metadata, chips |
| `caption` | Body | 13 | Normal | 18 | 0 | Tab labels, pills, ladder labels, captions |
| `label` | Body | 17 | SemiBold | 22 | 0 | Button labels, row titles, section titles |
| `eyebrow` | Body | 13 | SemiBold | 16 | 0.06em | Uppercase eyebrows |
| `timer` | Display | 80 | SemiBold | 84 | -0.04em | Focus timer readout |

`Eyebrow` uppercases at the call site (`text.uppercase()`), never through a style property, so locales where uppercasing is lossy are unaffected.

Weight is varied at call sites with `.copy(fontWeight = ...)` - commonly `Medium` or `SemiBold` on `body` and `bodySmall`.

The sizes, weights, line heights and tracking above are the same for both face pairs. Only the family changes.

### 2.4 Coverage is verified, not assumed

`FontCoverageTest` asserts that, for each of the four shipping languages, the resolved typeface has a glyph for every character in that language's required set (`Paint.hasGlyph`). Replacing a bundled font with one that does not cover a shipping language fails the build.

Required sets, at minimum: EN basic Latin; DE `ä ö ü Ä Ö Ü ß`; ES `á é í ó ú ü ñ Á É Í Ó Ú Ñ ¿ ¡`; RU `а-я А-Я ё Ё`. Plus the punctuation the copy actually uses: `- · " " ' ' %`.

## 3. Layout primitives

### `ScreenColumn`

The standard page. Every screen except the focus timer uses it.

```
fillMaxSize, background = bg, windowInsetsPadding(safeDrawing)
  content column: weight 1, optional verticalScroll,
                  padding horizontal 20, top 12, bottom 24,
                  Arrangement.spacedBy(gap)          // gap default 20
  bottom block (optional): padding start/end/bottom 20, Arrangement.spacedBy(4)
```

Per-screen `gap` values in use: 16 (Eisenhower, Feynman, Review, Progress, History), 18 (First week, Day complete, Train, Library, Premortem, Feynman feedback), 20 (default; Welcome, Goals, Rhythm, Combination, Habit stack, Result), 22 (Today, Exercise intro, 2-minute, Reflection, Technique detail, Settings).

The bottom block is a sticky action area, outside the scroll. It holds the primary button and, where present, a ghost secondary beneath it.

### Insets

`Modifier.screenInsets()` = `windowInsetsPadding(WindowInsets.safeDrawing)` - status bar, cutout, navigation bar and IME. The `Scaffold` sets `contentWindowInsets = WindowInsets(0,0,0,0)` and passes only the bottom-bar padding down, so each screen owns its own insets.

## 4. Components

`ui/components/Components.kt`. All 15 are stateless and take plain data plus callbacks.

| Component | Spec |
| --- | --- |
| `IteraCard` | `fillMaxWidth`, radius **24** (28 on hero and intro cards), background `surface`, padding **20** (18 or 16 where noted), `Arrangement.spacedBy(14)` |
| `Divider` | `fillMaxWidth`, height 1, colour `line` |
| `Eyebrow` | uppercased text, `eyebrow` style, colour `ink2` or a skill content colour |
| `Pill` | `CircleShape`, padding h10 v4, `caption` SemiBold, caller supplies container + content colours |
| `IteraButton` | `fillMaxWidth`, `heightIn(min = 56)` (48 or 44 for ghosts), radius **18**, padding h22 v12, `spacedBy(10, CenterHorizontally)`, optional leading icon at 20. Kinds: `Primary` = `ink` on `onInk`; `Secondary` = `surface2` on `ink`; `Ghost` = transparent on `ink2`. Disabled multiplies the background alpha by 0.4 |
| `CircleIconButton` | 44 dp circle, `surface2`, icon 20, `onClickLabel` carries the description |
| `TopBar` | `CircleIconButton` (Close or Back) + centred title in `bodySmall` SemiBold `ink2` + trailing slot with `widthIn(min = 44)` |
| `TechniqueToken` | square, size default 48, radius `size * 0.32`, skill container fill, technique icon at `size / 2` in the skill content colour. Sizes in use: 36, 40, 44, 48, 56, 60, 64 |
| `ChoiceChip` | `heightIn(min = 40)`, `CircleShape`, border 1.5 (`ink` when selected, else `line`), fill `ink` when selected, padding h14 v9, `bodySmall` Medium, `Role.Checkbox` |
| `Segmented` | outer radius 16 on `surface2` with padding 4 and `spacedBy(4)`; each segment weight 1, `heightIn(min = 44)`, radius 12, `surface` when selected, `bodySmall` SemiBold, `Role.Tab` |
| `StepRow` | `heightIn(min = 56)`, `spacedBy(14)`, leading `StepDot`, title `body` SemiBold (strikethrough and `ink2` when done), subtitle `bodySmall` `ink2`, trailing chevron 18 when clickable |
| `StepDot` | 28 dp. **Done**: filled skill circle, check at `size * 0.57` in `surface`, entering with `scaleIn(spring(MediumBouncy)) + fadeIn`. **Now**: 2 dp skill border with a 10 dp skill dot whose alpha pulses 0.35 to 1 on `infiniteRepeatable(tween(1200), Reverse)`. **Next**: 2 dp `line` border, empty |
| `MasteryLadder` | four bars, `spacedBy(6)`, each height 8 `CircleShape` on `surface2` filled with the skill colour; fill animates `tween(700, delayMillis = 150 * index)`; label below in `caption`, SemiBold and `ink` when reached |
| `ProgressBar` | height 6 (default), `CircleShape`, `surface2` track, `tween(800)` fill |
| `NoteField` | `BasicTextField` in a box: radius 16, `surface`, padding h16 v14, placeholder in `ink2`, cursor `ink`, optional 2 dp `ink` border (`bordered = true` for primary writing surfaces) |
| `SectionTitle` | `label` on the left, optional `bodySmall` SemiBold `ink2` counter on the right, bottom-aligned |

Screen-local components worth reusing verbatim: `CheckCircle` (Exercise.kt, 30 dp), `RadioDot` (Onboarding.kt, 26 dp), `LinkRow` (Train.kt, min height 60), `AnimatedCheck` (Exercise.kt), `DayRing` (Reflection.kt), `IntervalLadder` (Practice.kt), `QuadrantBox` (Practice.kt), `MasteryDots` (Train.kt), `Group`/`ValueRow`/`SwitchRow` (Profile.kt), `TimeRow` (Onboarding.kt).

## 5. Bottom navigation

`ui/IteraApp.kt`, `BottomBar`.

```
Column(fillMaxWidth, background = bg)
  Divider()                                   // 1 dp line
  Row(fillMaxWidth, navigationBarsPadding, height 68, padding horizontal 8,
      SpaceAround, CenterVertically)
    4 items: Column(widthIn(min = 72), clip RoundedCornerShape(16),
                    clickable(role = Role.Tab),
                    padding vertical 6 horizontal 8,
                    CenterHorizontally, spacedBy(4))
      Icon 24 dp   tint = ink when selected else ink2
      Text caption, SemiBold when selected else Medium, maxLines 1
```

Order is fixed: **Today, Train, Progress, You**. The bar is present only on those four routes.

## 6. Shape vocabulary

| Radius | Where |
| --- | --- |
| 28 | Hero card, exercise intro card |
| 24 | Default card, dashed coach box |
| 22 | Eisenhower quadrant |
| 20 | Onboarding option row, settings group, list rows, nudge row, review hint box |
| 18 | Buttons |
| 16 | Text fields, segmented outer, tab item, focus text controls |
| 14 | Inline selectable rows, time chip |
| 13 | 40 dp icon tiles |
| 12 | Calendar cell, segmented inner, quadrant task chip |
| 8 | Progress day bar |
| `CircleShape` | Pills, chips, dots, avatars, ladder bars, switches |
| `size * 0.32` | `TechniqueToken` |

## 7. Motion

All from the prototype. Reproduce the specs, not approximations.

| Where | Spec |
| --- | --- |
| `StepDot` done | `scaleIn(spring(dampingRatio = MediumBouncy)) + fadeIn()` |
| `StepDot` now | `infiniteRepeatable(tween(1200), RepeatMode.Reverse)` on alpha 0.35 -> 1 |
| `MasteryLadder` | per-bar `tween(700, delayMillis = 150 * index)` |
| `ProgressBar` | `tween(800)` |
| Skill bars (Progress) | `tween(700, delayMillis = 80 * index)` |
| `AnimatedCheck` | `Animatable` scale 0.5 -> 1 `spring(MediumBouncy, StiffnessLow)`, then the check path draws over `tween(450)` via `PathMeasure.getSegment` |
| `DayRing` | three 120-degree arcs (14-degree gap), each `tween(500)`, drawn in sequence |
| Focus ring | `animateFloatAsState(tween(900))` |
| Welcome `RisingPath` | each node `delay(120 * index)` then `spring(MediumBouncy)` scale 0.4 -> 1 |
| Reflection steps | `AnimatedContent` with `(fadeIn(tween(250)) + slideInVertically { it / 6 }) togetherWith fadeOut(tween(150))` |
| Eisenhower inbox and quadrants | `Modifier.animateContentSize()` |
| Onboarding progress segments | `animateFloatAsState` on fill |

**Reduced motion** is not handled by the prototype. Production adds it: a `LocalReduceMotion` flag from `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, and every animation above renders at its end state when set. See `docs/ux/03-ux-states.md` section 10.

## 8. Iconography

`ui/components/IteraIcons.kt`: 33 hand-authored `ImageVector`s on a 24x24 grid, stroke width **1.9**, round caps and joins, no fill except `Play`.

Technique icons: `TwoMinute, Pomodoro, Eisenhower, Feynman, FiveSecond, InfoDiet, OnePercent, DeepWork, Premortem, HabitStack, Pareto, TwoList, Reflection, Spaced`.
Navigation and UI: `Today, Train, Progress, You, Close, Back, Chevron, Check, Plus, Lock, Play, Pause, Spark, Bell, ArrowDown, Eye, Globe`.

Port the file as-is. **No `material-icons-extended` dependency** (D-12).

## 9. Elevation

None. Surfaces are separated by colour. No `shadowElevation`, no M3 tonal elevation tinting anywhere in the prototype - do not add either.

## 10. Touch targets

The prototype already meets 44 dp: buttons 56, ghost buttons 44-48, circle icon buttons 44, chips 40 with 8 dp gaps, step rows 56, settings rows 56, option rows 72, tab items 72x52, focus controls 72x48. Preserve these when porting; do not shrink a row to fit more on screen.

## 11. Production additions

Things the design system needs that the prototype does not have. Add them without changing anything above.

- `LocalReduceMotion` and the reduced-motion end states.
- `EmptyState` and `ErrorState` components (`docs/ux/03-ux-states.md`).
- Skeleton placeholders: `surface2` rectangles at the size of the content they replace, no shimmer, shown only after 150 ms.
- `@PreviewLightDark` previews for every component, plus `fontScale = 2f` previews for text-bearing ones.
- Accessibility semantics beyond what the prototype sets (`docs/ux/07-accessibility.md`).
