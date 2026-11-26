# Post-MVP expansion: product, UX and integration proposal

Status: **draft for review, 2026-11-26**. Documentation only; no approved curriculum, production or prototype change. Read the [research](research.md), [proposed ADR-0023](../../architecture/adr/0023-prediction-testing-proposal.md) and [draft milestone](milestone.md) together.

## Combined recommendation and roadmap

This extends the original Prediction Testing proposal in place. Keep five skills and fourteen techniques. Prefer deeper exercises within existing techniques over a larger catalogue. The [expanded evidence review](research.md) distinguishes scientific evidence, professional convention and design hypotheses.

| Priority | Candidate / form | Unique value versus overlap | Evidence and fit | Relative implementation cost |
| --- | --- | --- | --- | --- |
| 1 | Environment Design: extension to Habits, with Planning/Focus examples | Makes an identified next action physically easier; substantial overlap with Habit stacking, 1% improvement and Information diet | Plausible, indirectly supported; strongest fit for a small change | Low for optional examples using existing fields; higher if automatic cross-day tracking is added, which is deferred |
| 2 | Prediction Testing: optional Reflection extension | Preserves a prediction before action and explicitly compares evidence; more distinct than ordinary reflection | Established practice, but app-specific benefit unproven | Medium–high: durable records, migration, history/export and two-phase UX |
| 3, conditional | Breathing pause: optional supporting tool near Focus | A brief transition before work; neither a technique to master nor a prerequisite | Some stress/mood evidence; task-performance and very-short-dose benefit unproven | Medium: timed accessible cues, interruption handling and safety review, but no Room table |
| Defer | BLUF / Main point first: possible future Communication technique | Reader-oriented message structure is distinct from Feynman, but expands the product's scope | Established professional convention; no strong BLUF-specific efficacy evidence identified | Low for a standalone worksheet, high for a coherent sixth skill and curriculum |

These are proposed sequencing decisions, not approval to build. Stage A evaluates preparation content; Stage B evaluates the existing Prediction Testing design; Stage C is a separate optional breathing-tool decision; Stage D is discovery only for communication. Each can stop independently. No stage requires all four features to ship. Do not assign milestone numbers until the owner selects scope; the former “candidate 014” label for Prediction Testing is superseded by this staged roadmap.

Promotion to a new technique requires evidence of distinct repeated practice that existing techniques cannot serve. A new skill additionally needs several complementary techniques, a coherent learning goal, demonstrated user demand and an approved onboarding/progress design. Neither threshold is met by these proposals today.

## Prediction Testing recommendation and unique value

Start with an optional **Prediction Testing** extension to Daily reflection, within **Reflection**. Do not add a fifteenth technique yet. The scientific rationale warrants exploration, but the evidence does not establish the incremental benefit of a standalone productivity technique ([evidence and limitations](research.md)).

Its distinctive operation is a preserved **before-action prediction → real observation → comparison**. Daily reflection asks what happened; Premortem imagines failure and prepares a mitigation; the 2-minute and 5-second rules help action begin; Pomodoro structures time. None requires prospective evidence checking. This is enough distinction for a bounded prototype evaluation, not enough to enlarge the compulsory curriculum on concept alone.

An experiment is useful when uncertainty about a specific outcome blocks a small action. It is unnecessary when the next action is already obvious, the user only needs a timer, the barrier is a real lack of resources, or more analysis is delaying work. Show a short optional flow, never a required worksheet for starting a task.

## Repository assessment

Reviewed the repository instructions and local setup guidance, entry point, source-of-truth resolutions, issue index, PRD scope/requirements/content/curriculum/backlog, exercise/training/review/mastery/unlock specifications, domain/Room/catalogue/versioning/export documents, architecture and relevant ADRs, UX/design-system/prototype mapping, definition of done, and relevant production/prototype sources. This is a cross-project integration review, not a line-by-line audit of every file. No build, device run or interactive prototype walkthrough was performed for this documentation-only proposal; visual and behavioural validation is an explicit future milestone gate.

| Existing contract / inspected source | Consequence |
| --- | --- |
| [Product brief](../../prd/00-product-brief.md), [functional requirements](../../prd/03-functional-requirements.md) | One hero, small daily plan, five skills, honest practice. Avoid another compulsory daily step or a dashboard of assumptions. |
| [Curriculum](../../prd/07-technique-curriculum.md), [training engine](../../engine/01-training-plan-engine.md) | Days 1–14 authored; later days generated. Keep this sequence and its combination cadence unchanged in the extension. |
| [Exercise state machine](../../engine/00-exercise-state-machine.md) | Drafts survive but daily activities expire. A draft inside yesterday's activity is not a resumable multi-day experiment. |
| [Review rules](../../engine/02-spaced-repetition.md), [mastery](../../engine/03-mastery-and-progress.md) | Only Feynman and Spaced repetition create recall reviews. Confidence is not recall accuracy; preparation is not completed practice. |
| [Catalogue format](../../data/04-technique-catalog-format.md), `data/catalog/CatalogValidator.kt` | Runtime validation fixes 14 IDs, skill counts, intro days 1–13 and 14 curriculum days. A new technique is not merely one JSON row. |
| `domain/model/ActivityResult.kt`, `domain/training/CompleteActivityUseCase.kt` | Results are typed; completion triggers mastery, reviews and day checks. Never call completion just to save a plan or keep an experiment alive. |
| `data/database/IteraDatabase.kt`, `migration/IteraMigrations.kt` | Current schema is version 1 with an empty migration list. A durable experiment table requires a real 1→2 migration if this remains the baseline. |
| `data/database/entity/PlanActivityEntity.kt`, [ADR-0006](../../architecture/adr/0006-exercise-result-representation.md) | Daily records have a day FK, practiceDate and JSON payloads. Retain all existing result discriminators and IDs. |
| `data/export/MarkdownJournalWriter.kt`, [export contract](../../data/03-journal-export-format.md), [ADR-0014](../../architecture/adr/0014-data-reset.md) | History, streaming export, date ranges and both reset tiers need explicit experiment handling. |
| `design/.../ui/screens/Reflection.kt`, `Practice.kt` (Premortem and Review), [exercise UX](../../ux/02-screen-specs-exercise.md) | Reuse progressive questions, collapsed answers, NoteField, choices, cards and bottom actions. Review's memory test is the wrong semantic model. |
| [State management](../../architecture/02-state-management.md), [package structure](../../architecture/01-package-structure.md) | Immutable UiState, typed events/effects, Room-backed autosave, pure domain use cases, injected clock; no composable business logic. |

Some older text is stale: the content-model PRD still mentions eight strings, but D-04 and the catalogue format require four; the post-MVP backlog lists Russian as future work although four languages ship; early entry-point milestone status predates the issue index. Use source priority and current source, not these obsolete statements. These unrelated inconsistencies are not repaired in this proposal.

## Integration alternatives

| Option | Value | Cost and limitations | Decision |
| --- | --- | --- | --- |
| Add a reflection prompt only | Very small, no new data model | Retrospective guesses can replace original predictions; poor resume/comparison; risks just more journaling | Fallback if structured interaction proves burdensome |
| Extend Premortem | Existing predictions, likelihood and mitigation | Failure-oriented framing; unlocked Day 12; current result has no later observation phase | Offer a future entry link, not the primary home |
| Optional Daily reflection extension | Fits evidence-based reflection; no new curriculum day; easy evening return | Still needs durable lifecycle and dedicated interaction; no independent mastery in initial scope | **Recommended first evaluation** |
| New fifteenth technique | Discoverable, distinct identity and practice accounting | Catalogue, curriculum, upgrade, mastery, async combination, localisation and UI expansion; unproven incremental value | Reconsider only after evaluation |
| Cross-cutting experiment layer on every technique | Wide reuse | Adds decisions everywhere; can turn Itera into a tracking framework | Reject |

Reflection is the best category because the trained operation is comparing expectations with experience and deciding what follows. Planning is relevant to preparation; Learning is relevant in a broad sense, but Itera's Learning techniques emphasize explanation/recall. Preserve one skill per technique. If promoted later, use Reflection without moving existing techniques between skills.

## Name and draft terminology

**Prediction Testing** describes the actual action without implying treatment. “Behavioral Experiments” is scientifically recognizable but sounds clinical and experimental to a general audience; retain it in research documentation. “Reality Testing” is broader, can imply that the user's grasp of reality is being assessed, and is less precise for the task. “Try and compare” is suitable helper copy but too broad as an identity.

Draft translations for review, not shipped resources:

| Meaning | English | Russian | German | Spanish |
| --- | --- | --- | --- | --- |
| Name | Prediction Testing | Проверка предположений | Erwartungen prüfen | Comprobar predicciones |
| Prepare | What do you expect to happen? | Что, по вашему предположению, произойдёт? | Was erwartest du? | ¿Qué esperas que ocurra? |
| Observe | What actually happened? | Что произошло на самом деле? | Was ist tatsächlich passiert? | ¿Qué ocurrió realmente? |
| Neutral outcome | The result may support your prediction. | Результат может подтвердить ваше предположение. | Das Ergebnis kann deine Erwartung bestätigen. | El resultado puede confirmar tu predicción. |
| Defer | Review later | Вернуться позже | Später ansehen | Revisar más tarde |
| Stop | Close without an outcome | Закрыть без результата | Ohne Ergebnis abschließen | Cerrar sin resultado |

All labels, choices, examples, accessibility descriptions, errors, history/export headings and safety copy must be authored and reviewed in all four locales before shipping. Store stable enum codes, never translated labels. Do not translate user text. Use existing locale-selected fonts and Inter for authored text.

## Recommended exercise flow

Two phases, approximately **2–3 minutes to prepare and 2 minutes to compare**, plus the real-world action's separately stated time. These are design targets to test, not clinical doses. Do not advertise the whole exercise as two minutes. Morning/evening is convenient but never required, and no timer or deadline follows from reconsolidation theory.

1. **Choose one everyday situation.** Ask for a task the user avoids, such as writing a private outline. Keep it concrete and under the user's control.
2. **State a prediction.** “If I write for five minutes without editing, I will have no usable ideas.” Avoid identity claims and predictions about another person's hidden thoughts. Preserve the original wording before the action.
3. **Rate confidence.** Use five labelled choices: Very unsure / Unsure / In between / Sure / Very sure, with no default. They measure conviction, not task difficulty, mood or mastery. Store an ordinal value; no percentage or progress gauge. “Not sure” can be represented by the middle choice with explanatory copy; allow skipping the rating rather than inventing precision.
4. **Design the test.** Record one small action and one observable criterion: “Write privately for five minutes; afterward mark whether any sentence could help the outline.” Record bounds/stop condition. Keep ordinary safety checks and supports. Do not ask users to deliberately provoke rejection or distress.
5. **Save and try outside Itera.** Save succeeds before showing “Ready to try.” No completion animation, mastery change or program advancement. The user can return immediately, later that evening, or another day.
6. **Record observations first.** Show the saved prediction read-only and ask what actually happened, in the same units as predicted. The app does not infer the result from elapsed time or timer completion. “I did not try it” returns to the plan; “I stopped” can remain unfinished or close without an outcome.
7. **Compare.** Equal-status choices: Supported / Partly supported / Not supported / Inconclusive. No preselection, right answer, warning colour or score. Interpretation is the user's, not an automatic sentiment or text classifier.
8. **Reassess.** Offer the same confidence choices for the same original prediction. Any direction, no change, or omission is valid. If the wording changes, store that as a separate updated prediction rather than overwriting the original.
9. **Record learning.** One short sentence, or the explicit choice “No clear conclusion yet.” Optional next step: keep the approach, adjust the conditions, seek practical support, stop, or try again. A repeat creates a new linked record with a fresh prediction and no copied outcome.

Saving a reviewed experiment requires an original prediction, an action/criterion, an observation, a comparison choice and a learning statement or explicit no-conclusion choice. Ratings and another experiment are optional. Inconclusive evidence counts as a completed comparison, but not as evidence of an ability or psychological change. An unsupported prediction is not rewarded more than a supported one.

Original predictions/actions may be edited while preparing. Saving “Ready to try” freezes that prospective snapshot. Corrections after that must preserve it; the first implementation can offer “Close and make a revised test,” with a linked new draft, rather than build revision history. A retrospective entry must be labelled as such and must not pretend that its prediction was saved before the action; defer retrospective-entry support in the first milestone.

## Safety and product boundaries

Intro helper copy should be short: “Choose a small, reversible task. Keep normal safety checks. You can stop or leave this unfinished.” A secondary “Choosing a safe test” sheet explains exclusions: health/medication, trauma memories, physical danger, financial or legal stakes, sensitive disclosures, harassment, unsafe relationships and deliberately creating mistakes with consequences for others. Do not ask users to endure distress, relive memories, withhold sleep/food, or remove protective checks.

Default examples use private drafts, familiar learning tasks, personal organization and reversible decisions. If the selected situation feels unsafe or overwhelmingly distressing, the static guidance is to stop this exercise and choose an ordinary task; it is not a treatment flow. No diagnosis, symptom assessment or automated safety certification. Static boundaries cannot guarantee that arbitrary free text describes a safe activity, so make that limitation explicit in review, not through a false “safe” badge.

For a supported prediction: “This result supports your prediction in these conditions. What would make the next step more workable?” For inconclusive evidence: “There may not be enough information yet. You can leave it here.” Both are proposed original copy. No mandatory positive reframe. Choosing not to repeat is a legitimate conclusion.

No networking, account, AI or new permission. For these new fields, use keyboard input or verified on-device/bundled offline dictation only; do not inherit the existing consented external-recognizer fallback. ADR-0022 permits that fallback elsewhere, but this request explicitly requires local-only authored data. A route-level capability restriction must fall back to typing without changing the user's global voice preference. No user text or conviction values in logs, analytics, notifications or crash diagnostics. User-initiated journal export retains the existing explicit destination choice and disclosure.

## UX proposal

Use the existing full-screen exercise chrome and back/close behaviour, no new tab. Entry: a secondary “Test a prediction” action within Daily reflection's Technique Detail, available whenever that existing technique is available. This permits morning preparation without opening the evening-only activity. An open record changes the action to “Continue prediction test.” Limit to one open experiment in the first version to avoid a task-manager queue; reviewed/closed records remain in History.

Today retains its current hero, activity list and counters. At evening reflection, offer a small optional link above the existing questions when an experiment is open. Opening it preserves the reflection draft and returns to that draft. Skipping reflection neither closes nor deletes the experiment; closing an experiment neither completes nor skips reflection. No extra notification or overdue badge. Users may resume from Technique Detail or History at any time, even on a rest day.

Text wireframe (proposed structure, not a replacement for prototype validation):

```text
Prepare — normal app theme             Compare — normal app theme
Back                 Reflection        Back                 Reflection
Prediction Testing                     What happened?
Choose one small task                  [Original prediction, read-only]
[Situation]                            [Observed outcome]
[Prediction]                           Supported / Partly supported /
Confidence choices (optional)          Not supported / Inconclusive
[Small action + observable criterion]  Confidence now (optional)
[Bounds / when to stop]                [What did you learn?]
Safety helper                          No clear conclusion yet
Save and try                           Save comparison
Close (keeps draft)                     Review later
```

Split dense preparation into progressive questions using Reflection's collapsed-answer pattern, not nine simultaneous form fields. Reuse `ScreenColumn(gap = 22.dp)`, `TopBar`, `StepDot`, `NoteField`, `ChoiceChip`, `IteraCard` and sticky `IteraButton`/Ghost actions; use Reflection colour tokens, existing radii and typography. Do not add a completion percentage. Outcome comparison should stack prediction and observation vertically on phones and at large text sizes. The existing evening reflection remains an always-dark surface; the new route follows the app theme consistently regardless of entry point.

Required UI states: empty draft with example hint; partial autosaved draft; ready/waiting; observation draft; reviewed read-only record; closed without outcome; missing/deleted record; local save failure with retry preserving input. Show “Saved” only after persistence succeeds. Close keeps a draft; explicit close-without-outcome preserves the record; destructive reset uses existing confirmation conventions. A save failure stays in context and never clears typed text.

All controls have at least the repository's 44 dp minimum target, radio-group semantics, visible text status, logical TalkBack order and keyboard focus. No state relies on colour. No compulsory animation; reuse reduced-motion behaviour. Validate 2x fonts, German expansion, Russian glyphs, Spanish punctuation and pseudo-locales. Actual layout and transitions must be prototyped in `design/` and reviewed alongside production in the implementation milestone.

## Example exercise set

Examples below are authoring drafts. Outcomes are illustrative, never prefilled as if the user experienced them.

| Stage | Task and prediction | Test and observation | Learning without forced optimism |
| --- | --- | --- | --- |
| Introduction | “Clearing these two small personal items will take more than four minutes.” | Select two genuinely sub-two-minute tasks, estimate, complete and time them. Example outcome: six minutes because one needed missing information. | Supported: choose a different pair or allow more time. The estimate was useful evidence, not failure. |
| Practice: perfectionism | “A five-minute private draft without editing will contain no useful point.” | Write privately, stop at five minutes, inspect for one point relevant to the outline. Example: one relevant point among rough sentences. | Narrow update: this task allowed an imperfect start; it does not prove all unchecked writing is adequate. |
| Practice: confidence | “I cannot solve even the first step of this familiar practice problem.” | Try one ungraded step with normal reference material. Example: stuck on a prerequisite. | Supported: review that prerequisite or ask for help. No claim of global inability. |
| Practice: overthinking | “I need twenty minutes to choose a title for my private note.” | Choose a temporary title in two minutes; inspect whether it lets you find the note later. | If retrieval is still unknown, mark inconclusive and leave it open or close without certainty. |
| Outcome review | Reopen the exact five-minute draft prediction. | Record observations before selecting the comparison and confidence. | “I found one usable point, but I still need an editing pass.” No automatic decrease in confidence. |
| Later revisit | A week later, voluntarily inspect a past experiment. | Ask whether the conditions still apply; optionally make a new bounded test on another task. | Not a memory quiz, no recall grade, no review-item creation. A repeated difficulty may change the plan. |

Combination examples preserve what each existing technique actually teaches:

| Partner | Combination | Accounting / scope |
| --- | --- | --- |
| 2-minute rule | Predict time/effort for two genuinely small tasks; do them with its checklist; compare. | Itera's rule clears small tasks; do not silently redefine it as “work two minutes on any large task.” Existing completed checklist counts normally. |
| 5-second rule | Predict that starting still will not happen after the familiar countdown cue; try it once on a safe private task; observe whether the first action occurred. | No claim of a scientifically special five-second brain window. The experiment may find the cue unhelpful. |
| Pomodoro | Predict what one existing supported focus duration permits; run the timer; compare actual output with the criterion. | Use existing duration choices and lifecycle. A finished timer proves elapsed focus time, not the predicted output. Waiting time is not focus time. |
| Premortem | Select one low-risk predicted obstacle and test a mitigation on a small sample. | Do not intentionally cause the feared project failure. One practical trial can inform the mitigation. |
| Daily reflection | Link the experiment's lesson to “What will you change tomorrow?” only when the user chooses. | No auto-written coaching or overwrite of existing answers; reflection still completes once. |

The first release of the extension offers these as examples and manual actions, not automatically generated asynchronous combination chains. Existing combination steps retain their rules and credits; linking an experiment does not confer `INTEGRATED` on anything.

## Architecture and persistence assessment

**Reuse components, not the current daily lifecycle.** Template blocks can express text and choices, but cannot alone express a frozen prediction, external waiting, cross-day resumption and paired observations. A dedicated two-phase feature is justified even without a new catalogue technique. No need for a new generic form engine or a new global `ActivityState`.

Proposed packages: pure model/rules in `domain/model` and `domain/experiment`, repository contract in `domain/repository`, Room entity/DAO and mapper in their current `data` packages, implementation in `data/repository`, presentation in `feature/reflection/experiment`, route wiring in `core/navigation`, Hilt bindings in `di`. Use one immutable UiState and events/effects per screen, repository Flows and injected Clock/dispatchers. Route by record ID only; never put user text in navigation arguments.

Proposed `prediction_experiment` table:

| Field group | Proposed representation |
| --- | --- |
| Identity/lifecycle | Stable generated ID; status `DRAFT`, `READY`, `REVIEWED`, `CLOSED`; createdAt, updatedAt, preparedAt, reviewedAt, closedAt; local date alongside each terminal timestamp for stable history grouping |
| Prospective snapshot | Situation, prediction, action, observation criterion, bounds/stop condition; nullable initial confidence enum; frozen when READY |
| Outcome | Observation text, comparison enum, nullable revised confidence, learning text or explicit no-conclusion flag; optional updated prediction and next step |
| Links | Nullable previousExperimentId for a repeat, nullable reflectionActivityId when explicitly linked; indexed FKs with SET_NULL where a referenced record can disappear |
| Compatibility | Payload version if structured text is kept in JSON; stable enum and serial names; revision number for preventing stale saves |

Store queryable status/timestamps/links as columns; keep richer draft content in a versioned DTO if that follows ADR-0006 more closely. This is one lifecycle entity, not one table per result field. Index status/updatedAt and history dates; use transactions to enforce one open record and to atomically freeze/save transitions. Do not depend on `training_day` ownership for survival. No cached score or inferred belief strength.

Lifecycle: DRAFT → READY → REVIEWED; DRAFT or READY → CLOSED. Closing records no training credit. READY accepts outcome drafts while remaining READY. REVIEWED/CLOSED are read-only; repeat creates a new linked DRAFT. Autosave uses the existing two-second debounce with lifecycle flush, while explicit transitions await the durable transaction. Rotation/process restart loads Room state; abrupt process death before debounce can lose the newest unsaved keystrokes, so never promise lossless typing beyond the persistence contract. Day rollover, DST, travel, notification denial and changing rhythm do not expire the record. No background service waits for the outcome.

**Practice and mastery:** the first extension keeps all progress queries unchanged. Preparation, waiting, comparison and archival are separate journal records and grant no extra Daily reflection completion. Existing evening reflection completion continues to count once; a linked experiment does not duplicate it. History labels the record “Prediction test — reviewed,” not “training completed.” Rest-day status remains based on existing training facts. Explain this scope in Technique Detail: it is an optional reflection tool, not a new mastery ladder.

**Spaced review:** no `review_item`, recall grade, overdue queue or 1/4/9/21/60-day ladder for experiments. Personal predictions have no fixed answer to memorize. Users can voluntarily revisit or repeat from History. Adding a scheduled follow-up would require a separate approved scheduling design; it is not part of this milestone.

**History/export:** group reviewed and closed records by their terminal local date, drafts/open records by preparation/creation date. Show status plus preparation and outcome dates together, so a five-day wait is not shown as five days of practice. Export each record once, with the full original prediction, action/criterion, both ratings if supplied, actual outcome, comparison, learning and optional next step. Include records whose terminal date (or preparation/creation date while open) is in the chosen range, including the complete paired context when its other date is outside the range. “Everything” includes open/closed records. Keep the Techniques table last; distinguish experiment count from activity count. Escape multiline Markdown using the existing writer contract. Export caching, user-selected destinations and cleanup stay unchanged.

**Migration/reset:** add a non-destructive Room migration from the then-current version (currently 1→2), exported schema and populated upgrade tests. No backfill from old reflection text, no reinterpreting old notes as predictions, no rewriting prior result JSON. Register in `IteraMigrations.ALL`. The new table belongs to the program-reset tier: Reset program removes experiments along with reflections and training history; Erase everything removes them too. Both confirmation texts must name experiments, and reset/table-coverage tests must include it. Recheck active export cache invalidation and backup exclusions for the new data.

**Versioning:** extension-only UI and table changes need schema/app versioning, not a fictional curriculum content bump. If later catalogued, bump both catalogue/curriculum content versions, preserve all original IDs/skills/block keys/serial names and extend compatibility tests. `ignoreUnknownKeys` does not make an older app understand an unknown result subtype or schema: retain the existing non-destructive downgrade refusal, never promise backwards execution by older APKs.

## If promoted to technique 15 later

Use a stable new ID such as `prediction_testing`, Reflection, non-review-eligible, and the four required content strings. Preserve authored Days 1–14 and their stored plans. A possible new-user introduction is Day 15 with a complete, immediate low-risk test; a multi-day waiting experiment must not become the only required program action. Update fixed validator counts/intro-day assumptions, icon/name mappings, analytics enum allowlists and prototype catalogue. The existing “append a day” prose underestimates this work.

Users already beyond Day 15 will not automatically receive a missed introduction merely because ContentReconciler unlocks a new ID. They need an explicit optional introduction from the library or a separately approved one-time next-plan offering. Never reset programDay, replay early lessons, relock anything, regenerate completed plans or mark the new technique Met automatically. Keep the old Day-17/20/23 combination phase stable rather than changing it accidentally by using the new curriculum length as its origin. Mid-program users retain already generated days; only future generation may use approved new content.

A promoted technique would grant one practice completion only after observation/comparison, irrespective of outcome or rating change; preserve existing Met/Practiced/Applied thresholds. A later-day completion needs a fresh manual completion record on the review date linked to the durable experiment, not reopening an EXPIRED activity or mutating past history. Credit must be unique per experiment. Asynchronous combination membership needs its own design because the current runner settles children when the parent completes. Exclude it from generated combinations until that design is approved; manual companion exercises alone do not earn Integrated. These are reasons to defer promotion, not changes approved here.

## Review and go/no-go criteria

The next decision is whether to approve the bounded extension/prototype evaluation, not whether to promise efficacy. Evaluate with representative users using synthetic or voluntarily chosen low-risk examples and voluntary feedback; do not add telemetry or upload journals. Compare against ordinary reflection and a copy-only prompt. Look for users preserving a testable prediction, returning without losing context, recognizing supported/inconclusive outcomes as valid, and finding a useful next action without extra pressure.

Suggested qualitative gate: in a small round of 6–8 participants, at least 6 can complete the core tasks without facilitator repair, including a supported or inconclusive result; investigate every unsafe interpretation or perceived pressure before proceeding. This is a proposed usability threshold, not statistical evidence of benefit. Ask specifically whether the comparison revealed something ordinary reflection would miss. If the structured flow mainly causes form filling, rumination or abandoned plans, retain a smaller optional prompt or decline the feature. Only revisit technique 15 after repeated use shows a distinct, wanted learning operation and its maintenance cost is justified.

## Environment Design: prepare the next attempt

**Classification:** extend existing Habits techniques; do not add a Planning technique just because preparation involves planning. Keep each existing technique's skill assignment. “Prepare to start” is a more approachable exercise label than “Friction Reduction”; “Environment Design” can remain the research umbrella.

### Curriculum overlap

| Existing technique / day | Existing job | Proposed added example, without a new lesson day |
| --- | --- | --- |
| 2-minute rule / 1 | Finish genuinely small tasks now | Put the required notebook and pen in place if that is itself a sub-two-minute task; do not redefine the rule as starting any large task |
| Pomodoro / 2; Deep Work / 8 | Bounded/protected work session | Before pressing Start, have the relevant file and materials ready; preparation time is not focus time |
| Eisenhower / 3; Two-list / 7; 80/20 / 9 | Select important work and exclude competing priorities | Prepare only the selected first action, rather than organizing every task |
| 5-second rule / 4 | A cue to initiate action | Preparation removes a practical obstacle that a countdown cannot remove |
| Habit stacking / 5 | Link a small action to an existing cue | “After I put away my evening mug, I set out the notebook for my next work session” |
| Information diet / 11 | Choose attention inputs | Move a distracting shortcut or put an optional device out of reach, preserving emergency/assistive access |
| Premortem / 12 | Anticipate an obstacle and mitigate it | Prepare a local copy of an allowed reference before an offline task; do not bypass workplace controls |
| 1% improvement / 13 | Make one repeatable improvement | Remove one recurring setup step, such as keeping the reusable outline easy to find |
| Daily reflection / every evening | Learn from the day and choose tomorrow's change | Choose one preparation action tonight; after the next opportunity, note whether it helped |

Feynman and Spaced repetition can use materials prepared in advance, but that is supporting context rather than a new recall exercise. No new spaced-review eligibility or generated combination rule is needed.

### Exercise and next-day evaluation

1. Select one task for the next realistic opportunity. Example: draft a project outline after breakfast.
2. Identify one actual obstacle: the template is hard to find, materials are in another room, or an irrelevant app is the first thing visible.
3. Make one reversible preparation now: bookmark the local template, lay out the materials, or move that shortcut. Keep the action bounded to about two minutes; stop before “preparing” becomes extended tidying.
4. Write the first physical action and cue using existing fields: “After breakfast, open the outline and write its first heading.” If preparing tonight is impractical, prepare just before the next session instead.
5. At the next evening reflection after an opportunity, record what happened: Was the setup available? Was starting easier, unchanged or harder? Was there no opportunity? What one adjustment, if any, follows? Treat these as suggested prompts, not required ratings or a success score.

First version: offer localized examples/help within Habit stacking and 1% improvement detail, plus an optional example for Daily reflection's existing tomorrow-change answer. Use existing input, draft, history and export paths; do not append compulsory questions or invent a preparation database. The existing carry-over text helps the next morning. If days are missed, the user may consult History; this version does **not** promise automatic paired next-day recall or multi-day resume. A preparation note remains a note, not a live task. This limitation is intentional and must be stated in evaluation.

Logging: selecting an example or preparing a plan earns nothing. Completing an existing exercise counts only under its existing completion rule and identity; a preparation note never completes the later target task. Do not create a separate “environment designed” activity or credit multiple techniques for one action. Routine daily reflection still counts once.

Combinations are initially manual examples, not new authored chains: Eisenhower selection → one setup change → Pomodoro; Habit stacking cue → preparation → next reflection; Information diet adjustment → Deep Work. If Prediction Testing is later approved, an uncertain setup can become a genuine test: “With the template ready, I expect to begin within five minutes of sitting down.” Keep the actual observation and accept that preparation may not help. Do not require a prediction worksheet for every obvious obstacle removal.

**Cost and fit decision:** standalone Environment Design would largely repackage existing habits/attention practice. Optional examples are the better initial investment. No schema migration, new technique ID, progress metric, notification or special permissions. Copy-only help does not require a catalogue version bump; if approved work later changes JSON templates/defaults, bump both content-version fields and preserve frozen block keys. Never silently overwrite a saved habit stack or a user's tomorrow-change answer when choosing an example.

## BLUF: useful, but defer a Communication category

**Classification:** a plausible standalone technique in a future Communication skill, outside the currently approved five-skill curriculum. Do not force it into Planning or relabel it as Feynman to avoid the category decision. A Feynman recap can place a main idea first, but learning explanation and communicating a decision are different goals.

### Candidate exercise, if later approved

Use a synthetic or non-sensitive draft; avoid collecting client, colleague or employer confidential content. No mailbox access, send button, recipient/contact fields or external integration. Draft in Itera only if the user chooses; sending remains an independent action in their own app.

1. Name the reader's need: information, a decision, or an action.
2. Write the main point in one or two sentences, including a requested action and deadline only when relevant.
3. Add essential reason/evidence and any material uncertainty or exception.
4. Put optional background last. Re-read as the recipient: can they tell what this means for them without searching?
5. Compare original and revised text. Self-check meaning/caveat preservation and clarity; no automated quality score or promise that the recipient agrees.

| Exercise | Original draft pattern | Main-point-first rewrite / check |
| --- | --- | --- |
| Introduction: email | “We discussed the draft on Tuesday. There were two options. I looked again…” | “Please review the revised outline by Thursday. The change resolves the two points from Tuesday.” Check that the deadline is actually agreed/appropriate. |
| Practice: request | Background followed by an unclear ask | “Could you confirm which of the two dates works for you?” Then only the context needed to decide; avoid inventing urgency. |
| Decision | Chronology hides a choice | “We will use option B for the pilot, subject to the security review.” Keep that condition in the lead, then rationale. |
| Summary | Several details with no takeaway | “The pilot met the delivery target, but reliability is still uncertain.” Separate observed facts from interpretation. |
| Review | Reopen a draft later | Find the message, action and caveat in the first two sentences; revise if necessary. This is not a spaced-recall quiz. |

Potential combinations: Feynman explanation → audience-specific short summary; 80/20 selection → one recommendation with evidence; Premortem → decision memo stating the risk and mitigation; Pomodoro → draft/rewrite within a work block. Do not auto-send or claim combined practice without actually performing the existing exercises. A prediction test could explore whether a particular rewrite makes an action easier to identify, but another person's response speed alone is confounded evidence, not a writing grade.

### Why a sixth skill is substantial

Production `domain/model/Enums.kt` currently has five skills. Skill enumeration drives onboarding choices, filters, Progress and exports; skill token mappings, prototype model, catalogue validation, analytics/copy mappings, tests and generated-combination ordering also need review. Adding an enum member alone is insufficient. A Communication track would need an approved category description, multiple complementary skills such as request formulation and audience adaptation, and an honest practice progression; do not build these merely to fill a bar.

A future local worksheet could reuse TextInput/Instruction blocks and typed JSON drafts, with original/main point/context fields. A new catalogue technique would still require stable ID/unlock/content-version rules and validator changes described above. A new skill must not remap historical Feynman or Planning records, replace existing focus-area preferences, or mark users behind. New skill facts start empty; existing mastery and programDay remain intact. There is no current reason to incur these costs. Discovery can use the written examples without adding a prototype route or product code now.

## Breathing pause: a conditional supporting tool

**Classification:** optional support for Focus, not a new technique, skill or daily activity. Prefer **Breathing pause** over “Focus Reset,” which can imply a guaranteed change. The evidence and comparison with box breathing are in the [research review](research.md#box-breathing-versus-a-gentle-breathing-pause).

### Minimal interaction proposal

Place one secondary entry on focus-session setup, leaving Start focus as the main action. It may also be opened from Pomodoro/Deep Work detail before a demanding task or meeting. Do not add it to Today, the daily checklist or every technique's intro. It is never compulsory before starting or resuming work.

Use the focus screen's established dark surface, type and button styling, with a simple expanding/contracting circle, an always-visible phase label and elapsed/remaining time. The circle is a pacing cue, not a progress score. Show “Breathe in gently” / “Breathe out gently,” a Stop button, and “Continue to setup.” Finishing returns to setup; it does not auto-start a focus timer or congratulate a health outcome.

Candidate default for usability testing: 60 seconds, four seconds in/four seconds out, **no hold**. Duration choices: 1, 2 or 3 minutes; a slower five-in/five-out option may be exposed in a compact setup control if useful. These are bounded design defaults, not a personalized or clinically optimal prescription. Allow normal breathing without following the pace and allow exit immediately. No breath-volume target; “gentle” must not be interpreted as maximal deep inhalation. At duration end, stop pacing and say “Return to your normal breathing”; never require finishing a hold or breathing cycle to exit.

**Retention decision:** consider but defer the box variant in the first version. Optional retention would need a clearly selected separate four-phase mode (in / hold / out / hold), never hidden behind a pace change. A user could skip either hold or switch to no-hold immediately, with no failure state. No escalating hold length, maximum test, inhale-until-full instruction or retention preference silently restored. Its additional controls and safety burden currently offer insufficient demonstrated incremental value. If the owner later wants it, require separate content/safety/usability review before enabling it; the minimal proposal implements no retention at all.

### Safety, accessibility and lifecycle

Proposed inline guidance: “Breathe gently at a comfortable pace. Stop and breathe normally if you feel dizzy, short of breath or uncomfortable.” This follows the official guidance cited in the research. Additional product boundaries: use while seated or otherwise safely at rest, never while driving, operating equipment or in water; do not use the guide to manage acute breathing difficulty. If a condition or professional advice affects breathing exercises, follow that advice and skip this tool or ask an appropriate clinician. No medical questionnaire, diagnosis, emergency treatment flow or stored health profile. Avoid presenting the trial's exclusion list as a proven universal contraindication list.

A quiet pause with normal breathing is an equally valid alternative. If attention to breathing feels unpleasant, stop; do not instruct the user to persevere until calm. After a stressful situation the tool can offer a pause, but it must not imply it resolves the situation or changes work capacity.

Reduced-motion mode removes expansion and retains large text/timing. No flashing. Meaning is never colour-only. TalkBack receives phase changes rather than per-second chatter; optionally provide bundled/local phase cues and opt-in haptics with a visible mute/disable control. A nonvisual mode must communicate phases in time without overlapping screen-reader announcements. No required sound, microphone, sensor or network. All controls meet existing touch-size and font-scale standards; test motor, visual and hearing access, not just the animation.

Use a small pure phase controller and an injected monotonic elapsed-time source; Compose renders state rather than driving timing from animation frames. This differs intentionally from the long-lived persisted focus timer. Rotation can retain current foreground elapsed state; backgrounding, screen lock, interruption or process death stops pacing and cues. Return to a neutral stopped/setup state, never an unexpected hold or resumed audio. No foreground service, wake lock, background countdown, auto-completion or modification of `focus_timer.pb`. Do not offer it while a focus session is running in the first version; users pause/end that session through the existing controls first. Any previously paused timer stays paused until the user resumes it explicitly.

No Room history, breathing streak, mastery, focus-minute credit, stress/HRV measurement or journal entry is needed. Keep configuration session-local initially, so no preferences migration or reset extension is necessary. Users may voluntarily mention it in ordinary reflection. Evaluate whether it provides a useful optional transition without delaying work or becoming a requirement to feel ready. Do not infer usefulness from completing a countdown, a lower anxiety rating or repeated use alone.

Candidate practice examples (supporting actions, not new curriculum exercises):

| Situation / partner | Optional action | Honest outcome |
| --- | --- | --- |
| First use before Pomodoro | Try the no-hold guide briefly, stop whenever desired, then choose whether to start the planned session | Familiarity with the control, not proof of improved focus |
| Difficult task / Deep Work | Prepare the file first; optionally pause for one minute; return to the same setup | The work starts through the existing Start action, with no readiness score |
| Stressful meeting / BLUF candidate | After writing the request, take a comfortable pause before reviewing it | The message and decision may still need work; feeling calmer is not required |
| Daily reflection | If useful, note whether the pause helped the transition, made no difference or was uncomfortable | No compulsory before/after mood survey or repeat prescription |

Do not use breathing to force completion of a Prediction Testing experiment or to manufacture the expected positive outcome. If used during an experiment, it is part of that experiment's conditions and should be described honestly, not treated as proof that the original assumption was false.

## Shared delivery boundaries and localization

Keep all current IDs, skill assignments, stored result types, programDay, past plans, review ladders and mastery thresholds. Only Prediction Testing currently proposes a new Room table. Preparation uses existing authored data paths; breathing uses transient state; BLUF is deferred. None changes the first 14 days, unlock pace, Day-14 combination or later combination cadence. Do not use a database migration to re-seed training content or reset progress.

No AI, backend, account, telemetry upload, new permission or external speech recognition for new fields. Existing explicit journal export remains user-controlled; authored text must never enter logs or notification bodies. All UI additions would land in `design/` with production after approval. Current documentation work changes neither. References remain developer documentation, not research claims in app copy.

Draft labels for native-speaker review (no resource changes in this proposal):

| English | Russian | German | Spanish |
| --- | --- | --- | --- |
| Prepare to start | Подготовка к началу | Den Start vorbereiten | Preparar el inicio |
| Main point first | Главное в начале | Das Wichtigste zuerst | Lo principal primero |
| Breathing pause | Пауза для дыхания | Atempause | Pausa para respirar |
| Breathe in gently | Спокойно вдохните | Sanft einatmen | Inspira suavemente |
| Breathe out gently | Спокойно выдохните | Sanft ausatmen | Espira suavemente |
| Stop | Остановить | Beenden | Detener |

Translate complete prompts, choices, examples, timing plurals, safety copy and accessibility labels in EN/RU/DE/ES; do not stitch phase text together or force the acronym BLUF into every locale. Native-speaker review must check tone and understandability as well as fit. The milestones below remain gated proposals, not an expanded implementation authorization.
