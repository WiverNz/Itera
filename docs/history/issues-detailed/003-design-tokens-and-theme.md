# 003 - Design system: tokens and theme

**Phase** 1 - Foundation | **Depends on** 001 | **Blocks** 004

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/theme/Color.kt`, `Type.kt`, `Theme.kt``** - the whole theme package.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Port the prototype's theme and icon packages into production as Compose tokens, so no later issue has to pick a value.

## User value

The product's visual identity - calm, flat, warm - applied consistently and correctly in both themes.

**This is largely a port.** Copy the prototype's three theme files and its icon file, change the package, and add what production needs on top.

- `core/designsystem/theme/Color.kt`: port `IteraColors` (11 tokens x 2 themes), `SkillColors`, `Skill.colors(dark)` and `LocalIteraColors` verbatim.
- `core/designsystem/theme/Type.kt`: port `IteraType` (11 styles) and `LocalIteraType`, and implement the **script-aware family selection** in `rememberIteraType()` (ADR-0019):
  - use the four faces already bundled in `res/font/` by issue 001 - Bricolage Grotesque + Instrument Sans (Latin), Inter Tight + Inter (Cyrillic-capable);
  - chrome resolves its family from the active locale's script;
  - **user-authored text styles always use Inter**, in every locale;
  - **remove the prototype's system-font fallback** - a missing bundled font is a build failure, not a silent degradation.
- `FontCoverageTest` - for each of en, ru, de, es, assert the resolved typeface has a glyph for every character in that language's required set (`Paint.hasGlyph`).
- `core/designsystem/theme/Theme.kt`: port `IteraTheme`, the `Itera` accessor object, `ThemeMode.isDark()` and the M3 `ColorScheme` mapping.
- `core/designsystem/icon/IteraIcons.kt`: port all 33 `ImageVector`s (D-12). **Do not** add `material-icons-extended`.
- **Add** `core/designsystem/theme/Motion.kt`: the animation specs in `docs/ux/04-design-system.md` section 7 as named constants, plus `LocalReduceMotion` reading `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.
- **Add** a `NightSurface` wrapper for the three always-dark screens - the prototype only does this inline in `FocusScreen`.
- **Add** edge-to-edge setup and status-bar icon appearance from the effective surface (the prototype does this in `MainActivity` and `FocusScreen`; generalise it).
- **Add** `core/designsystem/preview/`: a `@PreviewLightDark` wrapper and a token gallery preview showing every colour, type style, shape and icon.
- Use all four font files bundled by issue 001. Verify coverage rather than assuming it.

## Non-goals

- Components (issue 004).
- Any screen.
- Reading the theme preference from DataStore - `IteraTheme` takes a `darkTheme: Boolean` parameter; wiring it to preferences is issue 015.

## Implementation notes

- Dynamic colour is **off**. Do not call `dynamicLightColorScheme`.
- Do not use M3 tonal elevation - set `tonalElevation = 0.dp` on surfaces. Tonal tinting would break the flat palette.
- Map the M3 slots exactly as the table in section 1.4 states; leave unlisted slots at their computed defaults, but verify none of them produce an off-palette colour in a component you plan to use.
- Skill colours are **not** in `ColorScheme`. Put them in `IteraSkillColors` with a `skillColors(skill: Skill)` accessor, provided by `IteraTheme`.
- `LocalReduceMotion` reads `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` once at the theme root; do not query it per animation.
- `NightSurface` re-provides the dark `ColorScheme` and the dark `IteraSkillColors`, and forces light status-bar icons.
- The `eyebrow` style needs `letterSpacing = 0.06.em` and uppercase applied at the call site via `text.uppercase()` - do not bake `textTransform` into the style, because it would break locales where uppercasing is lossy.

## Affected layers

`core/designsystem/theme`, `core/designsystem/icon`, `core/designsystem/preview`, resources.

## Acceptance criteria

- [ ] Every colour in `docs/ux/04-design-system.md` section 1.1 and 1.2 is present, with the exact hex value, in both themes, under the prototype's token names.
- [ ] All 11 type styles are present with the exact size, weight, line height and letter spacing.
- [ ] All 33 icons are present and render.
- [ ] The token gallery preview is visually identical to the same tokens rendered by the prototype.
- [ ] Every motion spec in section 7 is present with the exact duration and easing.
- [ ] `IteraTheme` switches cleanly between light and dark with no flash.
- [ ] `NightSurface` renders dark content inside a light-themed app.
- [ ] With `LocalReduceMotion = true`, all six animation specs resolve to a snap/end-state.
- [ ] The token gallery preview renders every token in both themes.
- [ ] No hex literal exists outside `Color.kt`; `ArchitectureTest` enforces this and passes.
- [ ] All four fonts load from assets; removing one fails `FontCoverageTest` rather than silently substituting.
- [ ] A Russian locale resolves display to Inter Tight and body to Inter; en, de and es resolve to the brand faces.
- [ ] Styles used for user-authored text resolve to Inter in **every** locale.
- [ ] `FontCoverageTest` passes for all four languages, and fails when a font is swapped for one lacking a required glyph.
- [ ] `IteraTheme` compiles against the prototype's `Skill` enum equivalent in the production domain model.

## Unit test expectations

- `ColorTokenTest` - asserts each token's exact `Color` value in both schemes, and each skill pair. A transcription check; catches typos a preview will not.
- `TypeTokenTest` - each style's size, weight, line height and letter spacing, identical across both face pairs.
- `FontCoverageTest` - per-language glyph coverage, with a deliberate-failure fixture.
- `MotionTest` - each spec's duration and easing coefficients, and that every spec collapses to its end state when `LocalReduceMotion` is true.

## UI test expectations

- `ThemeTest` - the same composable rendered under light and dark resolves `background` to the two documented values.
- `NightSurfaceTest` - content inside `NightSurface` uses the dark background while the surrounding app is light.

## Manual verification

1. Open the token gallery preview in light and dark; compare against the artboards side by side.
2. Set the device to dark; confirm the app follows.
3. Set animation scale to 0 in Developer options; confirm animated tokens render at end state in a test harness.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 3, 5, 10, 11.
