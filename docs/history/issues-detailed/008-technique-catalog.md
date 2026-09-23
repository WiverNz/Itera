# 008 - Technique catalog and content

**Phase** 1 - Foundation | **Depends on** 005, 006 | **Blocks** 010, 011, 013, 016, 018, 029

## Prototype reference

**`design/app/src/main/java/com/itera/app/model/Model.kt` and `design/app/src/main/res/values*/strings.xml`** - all 14 techniques and their text in four languages.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Author and load the content: 14 techniques, the 14-day curriculum, the template definitions, and the 112+ content strings.

## User value

Everything the user reads. This issue is as much writing as engineering.

## Scope

**Assets** (`app/src/main/assets/catalog/`):
- `techniques.v1.json` - all 14 entries per `docs/data/04-technique-catalog-format.md` section 1. Skills, intro days, minutes, exercise kinds and related lists come from `design/app/src/main/java/com/itera/app/model/Model.kt`; reproduce them exactly.
- `curriculum.v1.json` - days 1-14 per section 2.

**Content** - **ported, not authored** (D-04):
- Copy the 14 techniques' four strings each (`t_{slug}_name`, `_short`, `_why`, `_task`) from `design/app/src/main/res/values/strings.xml` - 56 strings - **and their Russian, German and Spanish translations**.
- Copy the skill names and descriptions, the mastery level names, and the combination step strings.
- Review the English against `docs/prd/08-content-style-guide.md`; change it in `design/` and all four locales if it needs changing.

**Code**:
- Catalog DTOs and `kotlinx.serialization` parsing.
- `AssetTechniqueCatalogRepository` with process-lifetime caching and the debug fail-fast / release fallback behaviour in ADR-0005.
- DTO to domain mappers, resolving string-resource keys through `CopyResolver`.
- `ContentReconciler` per `docs/data/05-migrations-and-content-versioning.md` section 2.
- Replace issue 006's hard-coded seed id list with a catalog read.
- `di/CatalogModule`.

## Non-goals

- Unlock logic (issue 010) - this issue only supplies `introDay`.
- Any exercise UI.
- Re-authoring the English content. Port it; change it only if it fails the style guide, and then change all four locales together.

## Implementation notes

- **The content already exists in four languages.** The work is porting and validating it, not writing it. Read all 56 English strings once against the style guide, but do not rewrite them casually - every change costs four translations.
- `minutes` per technique is fixed by the prototype: 2-minute 5, Pomodoro 30, Eisenhower 10, 5-second 2, Habit stacking 5, Feynman 15, Two-list 10, Deep Work 50, 80/20 10, Spaced 5, Information diet 5, Premortem 10, 1% 5, Daily reflection 2.
- `related` lists are fixed by the prototype's `Technique.related`; reproduce them, and note they are **not** symmetric.
- The prototype's enum names map to the frozen catalogue ids: `TwoMinute` -> `two_minute_rule`, `Pomodoro` -> `pomodoro`, `Eisenhower` -> `eisenhower_matrix`, `FiveSecond` -> `five_second_rule`, `HabitStack` -> `habit_stacking`, `Feynman` -> `feynman_technique`, `TwoList` -> `two_list_strategy`, `DeepWork` -> `deep_work`, `Pareto` -> `pareto_principle`, `Spaced` -> `spaced_repetition`, `InfoDiet` -> `information_diet`, `Premortem` -> `premortem`, `OnePercent` -> `one_percent_improvement`, `DailyReflection` -> `daily_reflection`. String keys keep the prototype's short slugs.
- The template for the 2-minute rule is `Instruction` + `Checklist(withStopwatch = true, minItems = 2)` with `requireChecked(2)`. The remaining five template techniques need their blocks designed within the six available types - do not add a seventh block type.
- `reviewEligible` is `true` for exactly `feynman_technique` and `spaced_repetition`.
- `related` needs 2-3 ids per technique, each of which must exist and must not be itself.
- `CatalogValidationTest` is the deliverable that makes this issue safe. Write it first, then make it pass.
- The release fallback path (last-known-good parsed copy) needs somewhere to keep that copy; a single `catalog_cache` Preferences entry holding the raw JSON is sufficient and avoids a new table.

## Affected layers

`data/catalog`, `data/mapper`, `di`, assets, `res/values/strings.xml`, `res/values/arrays.xml`.

## Acceptance criteria

- [ ] Both assets parse and pass all eight `CatalogValidationTest` assertions.
- [ ] All 14 techniques have all four strings, in **all four languages**; none is a placeholder.
- [ ] `minutes` and `related` match the prototype exactly.
- [ ] Skill counts are Focus 4, Planning 3, Learning 2, Habits 3, Reflection 2.
- [ ] `introDay` covers 1-13 with no gaps or duplicates.
- [ ] The curriculum covers days 1-14; Day 14 is the combination with the four documented steps; Day 7 and Day 14 set `weeklyLookBack`.
- [ ] Every template technique has a valid `ExerciseTemplate` whose `completionRule` references an existing block key.
- [ ] Every string key referenced by the catalogue resolves in all four locales.
- [ ] The Russian technique content renders without a missing glyph, confirming the font work in issue 003.
- [ ] `exerciseType` for each technique matches the table in `docs/engine/04-unlock-rules.md` section 2.
- [ ] The catalog parses once and is cached for the process lifetime.
- [ ] A malformed catalog fails fast in debug and falls back in release.
- [ ] `ContentReconciler` is idempotent and never re-locks a met technique.
- [ ] The seed callback now reads the catalog.

## Unit test expectations

- `CatalogValidationTest` - all eight assertions from `docs/data/04-technique-catalog-format.md` section 6.
- `CatalogCompatibilityTest` - detects each of the four forbidden changes against a committed snapshot.
- `CatalogParsingTest` - malformed JSON, a missing required field, and an unknown extra field.
- `ContentReconcilerTest` - new technique inserted locked; one at or below the current program day unlocked; running twice changes nothing; a met technique is never re-locked.
- `CatalogCachingTest` - the asset is read exactly once across 100 calls.

## UI test expectations

None.

## Manual verification

1. Read all 56 English strings in order. Flag anything that praises the user, mentions counts, or cannot be acted on today.
2. Check each `_task` against its `minutes` - is it actually doable in that time?
3. Spot-check the Russian and German against the English for meaning, not just presence.
3. Corrupt the asset; confirm the debug build fails fast with a clear message.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 5, 6, 8, 10, 11. Plus: a content review by someone other than the author, against the style-guide checklist.
