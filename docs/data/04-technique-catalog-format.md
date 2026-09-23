# Technique catalog format

Two JSON assets under `app/src/main/assets/catalog/`, parsed by `kotlinx.serialization` (ADR-0005). Structure lives in JSON; prose lives in string resources.

> **The content already exists.** `design/app/src/main/java/com/itera/app/model/Model.kt` defines all 14 techniques with their skill, minutes, exercise kind and related list, and `design/app/src/main/res/values*/strings.xml` holds their text in four languages. Issue 008 ports that data into this format rather than authoring it (D-04).

## 1. `techniques.v1.json`

```json
{
  "contentVersion": 1,
  "techniques": [
    {
      "id": "two_minute_rule",
      "skill": "HABITS",
      "introDay": 1,
      "exerciseType": "TEMPLATE",
      "estimatedMinutes": 5,
      "reviewEligible": false,
      "retired": false,
      "related": ["five_second_rule", "eisenhower_matrix"],
      "defaults": {},
      "template": {
        "completionRule": { "type": "requireChecked", "key": "tasks", "count": 2 },
        "blocks": [
          { "type": "instruction", "key": "how", "emphasis": true },
          { "type": "checklist", "key": "tasks", "minItems": 2, "maxItems": 5, "withStopwatch": true }
        ]
      }
    },
    {
      "id": "pomodoro",
      "skill": "FOCUS",
      "introDay": 2,
      "exerciseType": "FOCUS_TIMER",
      "estimatedMinutes": 30,
      "reviewEligible": false,
      "related": ["deep_work", "five_second_rule", "eisenhower_matrix"],
      "defaults": { "focusMinutes": 25, "breakMinutes": 5 }
    }
  ]
}
```

### Field reference

| Field | Required | Notes |
| --- | --- | --- |
| `id` | yes | stable, snake_case, **never changed or reused** |
| `skill` | yes | one of the five; one-to-one (R-03) |
| `introDay` | no | 1..13; omit for `daily_reflection`, which is seeded unlocked |
| `exerciseType` | yes | selects the UI body |
| `estimatedMinutes` | yes | shown as "About {n} min" |
| `reviewEligible` | yes | only `feynman_technique` and `spaced_repetition` are `true` in v1 |
| `retired` | no, default `false` | hides from curriculum and library; history still resolves (ADR-0015) |
| `related` | yes | ids for "Works well with"; 2-3 entries |
| `defaults` | yes | type-specific seed values |
| `template` | required iff `exerciseType == TEMPLATE` | |

### Block types

| `type` | Fields | Renders |
| --- | --- | --- |
| `instruction` | `key`, `emphasis` | a paragraph; `emphasis` uses the 23 sp display style inside the white card |
| `textInput` | `key`, `minLines`, `maxLines` | a text field with an autosaved draft |
| `checklist` | `key`, `minItems`, `maxItems`, `withStopwatch` | user-entered tick list; the stopwatch variant is the 2-minute rule |
| `pickOne` | `key`, `itemCount`, `suggestionsKey` | N entry rows, then a single selection |
| `twoLists` | `key`, `primaryCount`, `secondaryCount` | the Two-list strategy |
| `chipSelect` | `key`, `optionsKey`, `allowCustom`, `maxSelections` | suggestion chips plus optional free text |

Every block's label, placeholder and suggestion list is a **string-resource key derived from the block key**: `exercise_{techniqueId}_{blockKey}_label`, `..._placeholder`, `..._suggestions` (a string-array). This is why prose is absent from the JSON.

### Completion rules

```json
{ "type": "always" }
{ "type": "requireBlocks", "keys": ["explanation"] }
{ "type": "requireChecked", "key": "tasks", "count": 2 }
```

## 2. `curriculum.v1.json`

```json
{
  "contentVersion": 1,
  "days": [
    { "day": 1,  "newTechnique": "two_minute_rule" },
    { "day": 2,  "newTechnique": "pomodoro" },
    { "day": 3,  "newTechnique": "eisenhower_matrix" },
    { "day": 4,  "newTechnique": "five_second_rule" },
    { "day": 5,  "newTechnique": "habit_stacking" },
    { "day": 6,  "newTechnique": "feynman_technique" },
    { "day": 7,  "newTechnique": "two_list_strategy", "weeklyLookBack": true },
    { "day": 8,  "newTechnique": "deep_work" },
    { "day": 9,  "newTechnique": "pareto_principle" },
    { "day": 10, "newTechnique": "spaced_repetition" },
    { "day": 11, "newTechnique": "information_diet" },
    { "day": 12, "newTechnique": "premortem" },
    { "day": 13, "newTechnique": "one_percent_improvement" },
    { "day": 14, "combination": [
        { "technique": "eisenhower_matrix" },
        { "technique": "pareto_principle" },
        { "technique": "deep_work" },
        { "technique": "daily_reflection" }
      ], "weeklyLookBack": true }
  ]
}
```

Each combination step's prompt and hint are string resources: `combination_day14_step1_prompt`, `..._hint`.

## 3. The 14 technique ids

Frozen. Any change is a breaking content change.

`minutes` and `related` come from the prototype's `Technique` enum; reproduce them exactly.

| id | Skill | introDay | exerciseType | minutes | related |
| --- | --- | --- | --- |
| `daily_reflection` | REFLECTION | - (seeded) | `REFLECTION` | 2 | premortem, one_percent, feynman |
| `two_minute_rule` | HABITS | 1 | `TEMPLATE` | 5 | five_second, habit_stacking, eisenhower |
| `pomodoro` | FOCUS | 2 | `FOCUS_TIMER` | 30 | deep_work, five_second, eisenhower |
| `eisenhower_matrix` | PLANNING | 3 | `EISENHOWER` | 10 | pareto, two_list, two_minute |
| `five_second_rule` | FOCUS | 4 | `TEMPLATE` | 2 | two_minute, pomodoro, habit_stacking |
| `habit_stacking` | HABITS | 5 | `HABIT_STACK` | 5 | one_percent, two_minute, daily_reflection |
| `feynman_technique` | LEARNING | 6 | `FEYNMAN` | 15 | spaced, deep_work, daily_reflection |
| `two_list_strategy` | PLANNING | 7 | `TEMPLATE` | 10 | pareto, eisenhower, premortem |
| `deep_work` | FOCUS | 8 | `FOCUS_TIMER` | 50 | pomodoro, information_diet, pareto |
| `pareto_principle` | PLANNING | 9 | `TEMPLATE` | 10 | eisenhower, deep_work, two_list |
| `spaced_repetition` | LEARNING | 10 | `TEMPLATE` | 5 | feynman, habit_stacking, daily_reflection |
| `information_diet` | FOCUS | 11 | `TEMPLATE` | 5 | deep_work, two_list, pomodoro |
| `premortem` | REFLECTION | 12 | `PREMORTEM` | 10 | two_list, daily_reflection, pareto |
| `one_percent_improvement` | HABITS | 13 | `TEMPLATE` | 5 | habit_stacking, daily_reflection, two_minute |

## 4. String resource naming

**Four** keys per technique, matching the prototype (D-04). The eight-key scheme previously documented here is superseded.

| Purpose | Key | Shown on |
| --- | --- | --- |
| Name | `t_{slug}_name` | everywhere |
| Short description | `t_{slug}_short` | library row, hero subtitle, technique detail |
| Why it helps | `t_{slug}_why` | exercise intro (the framing paragraph) and technique detail ("Why it helps") |
| The exercise | `t_{slug}_task` | the "Your exercise" card on the intro screen |

`why` serves as both explanation and intro framing; `task` is the concrete instruction. There is no per-technique example, result headline or result body - the result screen uses one generic `result_title` and `result_sub` for every technique.

`{slug}` is the prototype's short form, not the full id: `two, pomodoro, eisenhower, five, stack, feynman, twolist, deep, pareto, spaced, diet, premortem, onepct, reflect`.

Missing keys are a build-time failure via the catalog validation test.

## 5. Loading

```kotlin
class AssetTechniqueCatalogRepository(
    private val assets: AssetManager,
    private val json: Json,
    private val copy: CopyResolver,
    @IoDispatcher private val io: CoroutineDispatcher
) : TechniqueCatalogRepository {
    private val cached = AtomicReference<List<Technique>?>(null)
    // parse once, map DTO -> domain resolving string resources, cache for the process
}
```

`Json { ignoreUnknownKeys = true; explicitNulls = false }`.

## 6. Validation test

`CatalogValidationTest` (JVM, reads the real asset) asserts:

1. exactly 14 techniques, ids matching the frozen table;
2. `introDay` values are 1..13 with no gaps and no duplicates;
3. skill counts are Focus 4, Planning 3, Learning 2, Habits 3, Reflection 2;
4. every `related` id exists and no technique relates to itself;
5. `template` is present exactly when `exerciseType == TEMPLATE`, and every `completionRule` references an existing block key;
6. every derived string-resource key resolves;
7. the curriculum covers days 1..14 with no gaps; every referenced technique exists and its `introDay` is at or before the day it is used in a combination;
8. `contentVersion` matches between the two files.

This test is what makes a release-build catalog failure impossible.
