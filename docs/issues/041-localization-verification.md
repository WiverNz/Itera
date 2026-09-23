# 041 - Localisation verification and audit

**Phase** 7 - Hardening | **Depends on** 037 (and, transitively, every issue that ships a string) | **Blocks** 039

## This issue builds nothing

Localisation is **not** implemented here. By the time this issue runs, the app is already fully localised in English, Russian, German and Spanish - infrastructure, content, runtime switching, persistence and the Settings integration all landed in earlier issues (D-15).

| Part | Built in |
| --- | --- |
| `AppCompatActivity`, `locales_config.xml`, `AppLocalesMetadataHolderService`, the four `values-*` directories | `001` |
| `AppLanguage`, `currentLocale()`, the locale-aware formatter helpers, `is24Hour()` | `002` |
| Four font files in `res/font/` and loading smoke check | `001` |
| Script-aware family selection and `FontCoverageTest` | `003` |
| `LanguageSheet`, `LanguagePill`, `RadioDot`, `TimePickerSheet` | `004` |
| The 14 techniques' content in all four languages | `008` |
| The Welcome language entry point | `016` |
| The Settings "Language & region" section, runtime switching, persistence | `034` |
| Every screen's own strings, in all four languages | each screen issue, enforced by the definition of done section 8 |

**This issue proves it, and catches what drifted.** If it finds something unbuilt rather than something broken, that is a defect in the issue that owned it - fix it there, and note why the earlier issue's definition of done did not catch it.

## Goal

Verify that all four shipping languages are complete, correct and undamaged after every feature issue and the accessibility pass have landed.

## User value

The app is correct in all four languages **at release**, not merely at the moment each piece was written.

## Scope

**Completeness audit**

- Diff the string catalogues: every key in `values/` exists in `values-ru/`, `values-de/` and `values-es/`, with no empty values and no untranslated English left in a translated file.
- Every count-bearing string is a plural, with the categories its language requires - Russian needs `one`/`few`/`many`/`other`.
- No string is assembled by concatenation; every substitution is positional.
- Any string containing a literal `%` carries `formatted="false"` (as `t_onepct_name` does).
- Flag keys that exist but are unreferenced, and references to keys that do not exist.

**Correctness audit**

- Every date, time, month, weekday and number goes through the `core/common/time` helpers - nothing calls `DateTimeFormatter.ofPattern` directly.
- The History calendar's first day of week follows the locale.
- Russian month headers use the standalone form ("сентябрь", not "сентября").
- Times honour the system 12/24-hour preference in every locale (D-16).
- Spot-check the Russian and German content for meaning, not just presence - a present-but-wrong translation passes every automated check.

**Rendering audit**

- Walk all 24 screens, every sheet, every dialog and every notification in **German** (longest) and **Russian** (Cyrillic), at default scale and at `fontScale 1.5`.
- Confirm no missing glyphs anywhere in Russian - the font work in issue 003 should make this impossible, so any occurrence is a real finding.
- Notification bodies stay under 60 characters in every language.
- Run `en-XA` and `en-XB` as a safety net for layouts no shipping language happens to stress.

**Behaviour audit**

- Switching language applies immediately, from both entry points, with no restart.
- The choice persists across a force-stop and a reboot, on Android 13+ **and** on an API 26-30 device.
- Android 13+ system per-app language setting changes the app.
- Switching mid-exercise does not lose typed text (the activity recreates).
- User-authored text is unchanged by a language switch.
- Past history re-renders in the new language with no leftover text from the old one.

**Deliverable**: a report appended to this file listing every finding, the issue that should have caught it, and its fix.

## Non-goals

- Building any localisation feature. If something is missing, fix it in its owning issue.
- A fifth language.
- Translating user content.
- RTL as a shipping direction - `en-XB` stays a layout check.

## Implementation notes

- The highest-yield check is the **diff since each screen issue merged**: list every string key added across the whole project and confirm four translations each. Automate it; the manual pass then only has to judge quality.
- A present-but-wrong translation is the failure mode automation cannot catch. Budget real time for reading the Russian and German aloud, or have someone who speaks them do it.
- Russian plural categories are the most common mistake. Check at 1, 2, 5, 11 and 21 - `11` is `many`, not `one`, which trips naive implementations.
- German is the length case: expect roughly 40 % growth over English. If a layout only just fits in English, it will clip.
- If a fix requires a layout change, change `design/` too, in the same change (ADR-0018).

## Affected layers

`res/values*`, and whichever feature packages the audit finds defects in.

## Acceptance criteria

- [ ] Every string in `values/` exists in all three other locales, with no empty values; `MissingTranslation` passes as an error with no suppressions.
- [ ] No untranslated English remains in a translated catalogue.
- [ ] Every count-bearing string is a plural with the correct categories per language.
- [ ] Russian plurals render correctly at 1, 2, 5, 11 and 21.
- [ ] No string is assembled by concatenation; every substitution is positional.
- [ ] Every date, time, month and weekday uses a `core/common/time` helper.
- [ ] The History calendar's first day of week follows the locale.
- [ ] Russian month headers use the standalone form.
- [ ] Times honour the system 12/24-hour preference in all four languages.
- [ ] All 24 screens, every sheet and every dialog render in all four languages with no clipped, overlapping or missing-glyph text, at default scale and at `fontScale 1.5`.
- [ ] No missing glyph appears anywhere in Russian.
- [ ] Notification bodies are under 60 characters in every language.
- [ ] `en-XA` shows no clipping; `en-XB` shows no mirroring break.
- [ ] Switching language from Welcome and from Settings applies immediately, with no restart.
- [ ] The choice persists across a force-stop and a reboot, on Android 13+ and on an API 26-30 device.
- [ ] The Android 13+ system per-app language setting changes the app.
- [ ] Switching language during an exercise does not lose typed text.
- [ ] A note written in one language is unchanged after switching.
- [ ] Past history re-renders with no leftover text from the previous language.
- [ ] The audit report is appended to this file.

## Unit test expectations

These are **owned** by earlier issues; this issue verifies they exist, pass, and actually cover the shipped catalogue.

- `TranslationCompletenessTest` - all keys in all four locales, no empties.
- `PluralCategoryTest` - correct categories per language.
- `NoConcatenationTest` - no non-positional substitution.
- `LocaleFormattingTest` (issue 002) - every formatter helper across four locales.
- `FontCoverageTest` (issue 003) - per-language glyph coverage.

Added here if the audit finds a gap: `UnreferencedStringTest` and `UntranslatedStringTest`.

## UI test expectations

- `LocalizedRenderTest` - parameterised over 24 screens x 4 locales: renders, no truncated text node, no missing-glyph run.
- `LanguageSwitchTest` (issue 034) - verified here to cover both entry points and an activity recreate.

## Integration test expectations

- `LanguageHistoryTest` - populate a week of history, switch language, assert Today and History contain no string from the previous language.

## Manual verification

1. Walk the full daily loop in Russian, then in German.
2. Switch language from Welcome before onboarding, and from Settings after.
3. Force-stop and reopen in each language; the choice holds.
4. On an API 26-30 device, confirm persistence through the AppCompat backport.
5. On Android 13+, change the language from system settings and confirm the app follows.
6. Write a note in German, switch to Spanish, confirm the note is untouched.
7. Check a German notification on the lock screen.
8. Check Russian plurals at 1, 2, 5 and 11 practices.
9. Read the Russian and German technique content for meaning.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections, with section 8 satisfied for four shipping languages and the audit report appended below.

---

## Audit report

*(To be filled in: keys audited, gaps found and the issue that should have caught each, translations corrected, rendering findings and fixes, devices and OS versions used.)*
