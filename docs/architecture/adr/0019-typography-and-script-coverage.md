# ADR-0019: Script-aware type families, with a bundled Cyrillic face

Status: accepted (2026-09-23)

## Context

The design system is built on two Latin faces: **Bricolage Grotesque** (display) and **Instrument Sans** (body).

Their subset coverage, read from the `@font-face` declarations in the original design bundle, is:

| Face | Subsets | Cyrillic (U+0400-04FF) |
| --- | --- | --- |
| Bricolage Grotesque | vietnamese, latin-ext, latin | **absent** |
| Instrument Sans | latin-ext, latin | **absent** |

`latin` plus `latin-ext` covers English, German (`ä ö ü ß`) and Spanish (`á é í ó ú ñ ¿ ¡`). It does **not** cover Russian.

The prototype acknowledges this and lets Cyrillic fall through to the platform default font. That is an uncontrolled result: the substituted face differs by OEM and Android version, its metrics are unknown, and nothing fails if it changes. Russian is a shipping language, so this is not acceptable.

Compose cannot chain `FontFamily`s by glyph coverage - a `FontFamily` selects by weight and style, and a missing glyph falls through to the platform default. `Typeface.CustomFallbackBuilder` would give a real per-glyph chain but is API 29+, above our `minSdk 26`.

## Decision

Bundle a second, Cyrillic-capable type pair and select the family deterministically rather than relying on fallback.

**The faces**, both variable and both OFL:

| Role | Latin brand face | Cyrillic-capable face |
| --- | --- | --- |
| Display | Bricolage Grotesque | **Inter Tight** |
| Body | Instrument Sans | **Inter** |

Inter is chosen for the body because it is a neutral UI grotesque with a large x-height and open apertures - the same category as Instrument Sans - with complete Cyrillic. Inter Tight is its display companion, drawn for headline settings with tighter default spacing, which matches how the display face is used here (-0.02em to -0.04em tracking at 30-80 sp). Using one superfamily for both roles preserves the display/body hierarchy the design depends on, which a single substituted face would flatten.

**Selection rule**, applied in `rememberIteraType()`:

1. **Chrome** - resolve from the active locale's script. Cyrillic script (`ru`) uses Inter Tight / Inter; every other locale uses the brand faces.
2. **User-authored text** - always Inter, in every locale. The app cannot predict what script someone types in, so the text styles used for notes, reflection answers, explanations, task labels and history entries always use the face with the widest coverage.

Rule 2 is the less obvious half. Without it, an English UI showing a Russian note reproduces exactly the uncontrolled fallback this ADR exists to remove.

**Verification, not assertion.** `FontCoverageTest` asserts, for each of the four languages, that the resolved typeface has a glyph for every character in that language's required set (`Paint.hasGlyph`, API 23+). The build fails if a bundled font is replaced by one that does not cover a shipping language.

## Alternatives considered

- **Keep the platform fallback** - what the prototype does. Unpredictable across devices; rejected by requirement.
- **`Typeface.CustomFallbackBuilder`** - a true per-glyph chain, but API 29+. Would leave API 26-28 on the uncontrolled path, which is the situation being fixed. Rejected.
- **Use Inter / Inter Tight for all four languages** - consistent, and removes the brand faces entirely. Rejected: it discards the visual identity for three of four languages to solve a problem in one.
- **Find a Cyrillic face that resembles Bricolage Grotesque** - Bricolage is idiosyncratic and no open Cyrillic face is close. A near-miss reads worse than a clean neutral. Rejected.
- **Subset the brand fonts and commission Cyrillic** - out of scope and cost for an MVP.

## Consequences

- Two extra variable font files, roughly 300-400 KB each pre-compression. The 12 MB artifact budget absorbs this; `docs/architecture/06-dependency-catalog.md` section 6 records it.
- Russian loses the brand display character. It keeps the display/body hierarchy, the type scale, and every other token. This is the honest cost of shipping Russian without commissioning type.
- An English screen showing user-authored Cyrillic renders that text in Inter while the chrome is Instrument Sans. Both are neutral grotesques and the difference is slight; it is intentional and documented rather than accidental.
- Switching language re-resolves the families through the normal activity recreate; no extra machinery.
- Adding a fifth language requires checking its script against the table and, if it is not Latin or Cyrillic, extending the rule and the coverage test.

## Migration implications

None - nothing is implemented yet. Issue 001 bundles the four files in `res/font/` and verifies loading. Issue 003 implements the selection rule and writes the glyph-coverage test.
