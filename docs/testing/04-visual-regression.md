# Visual fidelity and regression

The goal is that the production app **visually and behaviourally matches `design/` as closely as practical**. This document says how that is checked.

Milestone 012 adds voice before 009's quality passes. Include idle/listening/partial/failure/denied/unavailable/ambiguous/confirmation fixtures in 010's final golden set using fake recognition, with normal and always-dark hosts, large text and the existing locale matrix. Compare against the minimal prototype additions in `docs/ux/10-voice-input.md`; never capture live recognition timing in goldens.

## 1. Three layers

| Layer | Catches | Cost | When |
| --- | --- | --- | --- |
| **Side-by-side comparison** | Everything, including things no assertion would express | Manual, ~10 min per screen | Once per screen, during its issue; again in manual QA |
| **Semantic assertions** | Structure, state, behaviour | Cheap, automated | Every screen issue |
| **Golden images** | Silent pixel drift after a screen is signed off | Moderate setup | Issue 039 onwards |

Visual fidelity is established by the first, then protected by the third.

## 2. Side-by-side comparison

Part of every UI issue's definition of done. Run both apps on the same device, at the same system font size and theme, and walk the screen.

Check, in this order:

1. **Structure** - the same elements, in the same order, with the same grouping.
2. **Spacing** - gutters, gaps between blocks, padding inside cards.
3. **Size** - component heights, icon and tile sizes, corner radii.
4. **Type** - style per element; weight overrides; line counts at the same content length.
5. **Colour** - background, surface, text and skill colours in both themes.
6. **State** - what changes on tap, what is disabled and when, what animates and how fast.
7. **Copy** - the same strings in the same places.

Both apps can be installed at once: `com.itera.app` and `com.wivernz.itera` are different ids.

Record the comparison in the issue: which screen, which device, which themes and languages, and any deviation with its reason.

## 3. Semantic assertions

Compose UI tests already required per screen (`01-test-matrix.md`). For prototype parity, each screen test additionally asserts the things a reviewer would otherwise check by eye and forget:

- every element the spec lists is present, by text or `testTag`;
- element **order** in the semantics tree matches the spec;
- disabled controls are disabled in exactly the documented conditions;
- the screen renders in light and dark without throwing;
- the screen renders in all four languages without a truncated text node.

These are not pixel tests. They stop a screen from quietly losing an element.

## 4. Golden images

### Scope

One golden per screen per **(theme, font scale)** at a fixed device configuration, in English - roughly 24 x 2 x 2 = 96 images.

**Plus a Russian subset.** Russian resolves to a different type pair (Inter Tight / Inter rather than the Latin brand faces, ADR-0019), so its screens genuinely differ and an English-only set would not protect that path. Record one golden per screen in Russian, light theme, default font scale - 24 more images. That is enough to catch a broken font selection, a missing glyph or a Cyrillic layout regression without quadrupling the set.

German and Spanish use the same faces and layouts as English, so they are covered by the no-truncation assertion in section 3 rather than by goldens.

### Where they live

```
app/src/test/screenshots/          committed goldens
  en/light/…  en/dark/…  en/light-2x/…  en/dark-2x/…
  ru/light/…
app/build/outputs/roborazzi/       comparison output from a failing run, ignored
```

Goldens sit **outside** any `build/` directory so nothing in `.gitignore` excludes them. Comparison artefacts (`*_compare.png`, `*_actual.png`) are ignored. Set `roborazzi.record.outputDir` to the path above.

### Mechanism

Roborazzi over Robolectric, run on the JVM. Chosen over an instrumented screenshot runner because it needs no emulator in CI and produces stable output across machines.

```
./gradlew recordRoborazziDebug     # regenerate after an intended change
./gradlew verifyRoborazziDebug     # fail on drift
```

Configuration that must be pinned or the images are not reproducible: device spec (pixel density, size), font family (the bundled fonts, never the system fallback), locale, time zone, and `ANIMATOR_DURATION_SCALE = 0`.

### Rules

- Goldens are recorded **from the production app**, after its screen has been signed off against the prototype. They protect the agreed result; they do not define it.
- Every screen is rendered from a fixed fixture (`Fixtures`, `docs/testing/00-strategy.md` section 5), never from the clock, a random value or a real database.
- A golden that changes in a pull request must be re-recorded **deliberately**, with the reason in the change description. An unexplained re-record is a review failure.
- Goldens are committed. They are the reason the suite catches drift at all.

### What is deliberately not goldened

- The focus timer's running state (time-dependent).
- Anything with an in-flight animation - goldens are taken at end state.
- Loading skeletons (timing-dependent).
- German and Spanish (same faces and layouts as English; covered by assertions).
- Scroll positions beyond the first screenful.

## 5. Sequencing

| Phase | Visual work |
| --- | --- |
| 003, 004 | Port theme and components. Compare every component preview against the prototype's rendering |
| 015-034 | Per screen: implement against the prototype, compare side by side, add semantic assertions |
| 037 | Accessibility sweep may change semantics; re-run comparisons on any screen it touches |
| 041 | Four-language rendering pass and localisation audit |
| 039 | Set up Roborazzi, record the full golden set (English x 2 themes x 2 scales, plus the Russian subset), wire `verifyRoborazziDebug` into CI |
| 040 | Final comparison pass on a release build, on two devices |

Goldens are recorded **once, late** (issue 039), after accessibility and localisation have stopped moving the layouts. Recording them earlier means re-recording them repeatedly, which trains everyone to re-record without looking.

## 6. When the prototype is wrong

If a screen cannot be built as drawn - a real product rule intrudes, a state the prototype lacks has nowhere to go, an accessibility requirement conflicts - then:

1. Implement the correct behaviour.
2. Record the deviation in the issue and, if it is general, in `docs/00-source-of-truth.md`.
3. **Update `design/` to match**, in the same change.

A prototype that has silently diverged from the app is worse than none, because it makes every future comparison untrustworthy.
