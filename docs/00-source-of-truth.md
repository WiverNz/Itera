# Source of truth

Status: **authoritative**. Every other document in `docs/` conforms to this one.
Last reconciled: 2026-09-23 (prototype pass).

## Planned voice capability (2026-09-26)

Milestone [012](issues/012-voice-input-and-commands.md) is appended without renumbering and runs after 006/007/008 plus outstanding core visual acceptance, before 009/010/011. Documentation/backlog only: no implementation is started by this change.

- Dictation uses focused editable exercise fields; a separate command mode supports eight typed commands through existing business actions. No global inbox/task model, AI parser, chat assistant, background listening or hotword.
- Item commands are context-bound. Eisenhower supports Add but has sorting/selection rather than item completion. Only existing checklists support CompleteItem; Premortem's reason list supports Add. Dictation handles other free-text fields.
- Focus commands preserve the controller, setup task, unlock and completion rules. Specified minutes use the existing setup choices plus the seeded suggestion; unsupported durations return to setup without silent substitution. This milestone does not expand duration choices.
- **Privacy decision explicitly selected by the user (amended 2026-09-28):** recognition order ON_DEVICE → usable SYSTEM DEFAULT (with consent) → USER-SELECTED INSTALLED PROVIDER (picked by the user and agreed to by name) → UNAVAILABLE. Never a first-installed or assistant-app fallback, never a silent switch; consent is local and revocable in Settings ([ADR-0022](architecture/adr/0022-voice-recognition-and-privacy.md)). No new `INTERNET` permission and no transmitted/logged user speech. ADR-0022 documents the real permission/platform boundary decision for `RECORD_AUDIO`; no other ADR is needed.
- UI follows [voice UX](ux/10-voice-input.md). Minimal prototype affordances/states are pending milestone 012 and must be added together with production, not silently assumed present in `design/` today.
- No unresolved MVP product decisions. The earlier "strictly on-device" rule was replaced by the consented chain above at the user's explicit request.

## 1. Source priority

| # | Source | Authority over |
| --- | --- | --- |
| 1 | **Explicit product requirements** - `docs/prd/**`, `CLAUDE_START_HERE.md`, and direct instructions | Product and business rules, scope, constraints. Wins over everything. |
| 2 | **`design/` prototype** - a running Kotlin + Compose Android app | All UI/UX: screen structure, navigation, layout, component hierarchy, spacing, dimensions, typography, colour, themes, icons, states, interactions, transitions, bottom navigation, dialogs and sheets, forms, progress UI, timers, technique screens, settings, onboarding, responsive behaviour, copy and its placement, visual hierarchy. |
| 3 | **Generated documentation** - the rest of `docs/**` | Architecture, persistence, engines, curriculum, mastery, scheduling, testing, delivery. |
| 4 | - | - |

**`Itera.html` has been removed from the repository.** It was a bundle of 51 static artboards from an earlier design pass; `design/` supersedes it completely. Where a document still mentions an artboard, it is recording why an older decision was made, not instructing. Nothing in the repository depends on it.

## 2. How to use `design/`

`design/` is not a transcription exercise that has already been completed. It is a **running app** and the primary implementation reference for UI work.

While implementing any UI issue: open the corresponding file in `design/app/src/main/java/com/itera/app/`, read it, and reproduce its structure, values and behaviour in the production codebase. The mapping from screen to prototype file is in `docs/ux/05-prototype-reference.md`.

Where the prototype already establishes a value - a padding, a radius, a size, a colour, a text style, a state transition, an animation spec, a button position - **reproduce it**. Do not redesign it, do not "improve" it, and do not derive it again from a Markdown table. The tables in `docs/ux/04-design-system.md` exist so that reviewers can check work without reading Kotlin; the prototype is what the code must match.

## 3. What the prototype is not

The prototype is a **UX** prototype. It deliberately has no persistence, no scheduling engine, no background work and no real domain logic; state lives in one in-memory `AppViewModel` and several screens use hard-coded sample data.

So the prototype is **not** authoritative for:

- persistence, migrations or any storage decision;
- the training-plan engine, unlock rules, spaced-repetition scheduling, or day rollover;
- notifications, WorkManager, or anything that must survive process death;
- architecture (DI, layering, state management, repository boundaries);
- mastery and progress **rules** (though its *rendering* of them is authoritative - see D-09);
- test strategy.

For all of those, priority 3 applies: the existing generated documentation stands.

## 4. Product naming

- Product name: **Itera**.
- Production application id and package: `com.wivernz.itera` (the existing scaffold). The prototype uses `com.itera.app`; when porting, rewrite the package.
- Gradle root project name: `Itera`.
- "Productivity Trainer" is a dead working name and must not appear anywhere user-facing.

## 5. Prototype-era resolutions (D-series)

These resolve `design/` against the previously generated documentation.

### D-01 - The design system is the prototype's theme package

`design/app/src/main/java/com/itera/app/ui/theme/` defines 11 colour tokens per theme, 5 skill colour pairs, and an 11-style type scale. This **replaces** the 16-style scale and the token names that were transcribed from `Itera.html`.

Token names change: `outlineVariant` becomes `line`, `onSurfaceVariant` becomes `ink2`, `outlineDisabled` becomes `ink3`, `surfaceVariant` becomes `surface2`, `accentContainer` becomes `accentSoft`. The **values** are unchanged - the prototype's palette is identical to the one derived from the artboards, which is a useful cross-check. Two tokens are new: `onInk` and `scrim`.

Applies to `docs/ux/04-design-system.md`.

### D-02 - Navigation is a single controller with a conditionally shown bar

The prototype uses **one** `NavHostController`, string-keyed routes, and shows the bottom bar only when the current route is one of the four tab routes. Library, History and Technique detail are **full-screen with a back button and no bottom bar**.

This replaces the previously documented two-controller design and the claim (taken from the artboards) that Library and History keep the bar.

The implementation keeps type-safe route classes (an internal code-quality choice that changes no behaviour); everything observable - the route set, which screens show the bar, tab-switch semantics, back behaviour - comes from the prototype.

Applies to `docs/ux/01-navigation-graph.md`, `docs/architecture/adr/0008-navigation.md`.

### D-03 - Localisation is four languages, in MVP scope

The prototype ships complete `values/`, `values-ru/`, `values-de/` and `values-es/` catalogues - 362 strings and 4 plurals each - plus `locales_config.xml`, an `AppCompatActivity`, and the `AppLocalesMetadataHolderService` backport.

**English, Russian, German and Spanish are all MVP scope**, with runtime switching from Settings and from the Welcome screen, and the choice persisted across restarts. This replaces "English first, Russian later".

Applies to `docs/i18n/00-localization.md` (new), `docs/prd/04-nonfunctional-requirements.md`, `docs/architecture/adr/0017-localization.md` (new), issue `041`.

### D-04 - Technique content is 4 strings per technique, already written

The prototype defines each technique with `name`, `short`, `why` and `task`. `why` carries both the explanation and the intro framing; `task` is the concrete instruction. There is no per-technique example, result headline or result body - the result screen uses one generic headline and subtitle.

This **replaces** the eight-strings-per-technique scheme. Content authoring for issue `008` drops from 112 strings to porting 56 that already exist in four languages.

Applies to `docs/data/04-technique-catalog-format.md`, `docs/prd/08-content-style-guide.md`, issue `008`.

### D-05 - Onboarding requires at least one goal, pre-selected

The prototype seeds `focusSkills = [Focus, Learning]` and gates Continue on `focusSkills.isNotEmpty()`. This replaces "start empty, Continue always enabled".

Applies to `docs/ux/02-screen-specs-onboarding.md`, issue `016`.

### D-06 - Today has three fixed steps and a four-branch hero

The prototype's Today shows exactly three `StepRow`s - exercise, focus, reflection - with a `completed / 3` counter, and picks its hero by: exercise not done -> exercise; focus not done and before 18:00 -> focus; reflection not done -> reflection; otherwise -> a "day is done" card whose action opens Day complete.

This replaces the six-level hero priority and the seven row states derived from the artboards.

**Product rules preserved on top of it** (see section 6, P-01): due reviews still appear on Today as an additional step, and the counter still counts every activity in the day rather than a hard-coded 3.

Applies to `docs/ux/02-screen-specs-today.md`, issue `017`.

### D-07 - The Progress day strip is bars, over 14 days

The prototype renders regularity as a row of full-width rounded bars (28 dp tall, radius 8, one per day, today outlined in `accent`) across a window that grows to 14 days - not ten dots. Skill level renders as **four** segments, one per band.

Applies to `docs/ux/02-screen-specs-train-progress-you.md`, issue `031`.

### D-08 - Settings gains a Language section

Prototype sections, in order: identity, **Daily rhythm**, **Program**, **Language & region**, **Appearance**, **Notifications**, **Coach**, **Data**. The Language section holds the language picker and a time-format row.

Applies to `docs/ux/02-screen-specs-train-progress-you.md`, issue `034`.

### D-09 - Skill-level thresholds come from the prototype

The previously documented thresholds were reverse-engineered to reproduce a single artboard. That artboard is obsolete, so the derivation has no source. The prototype computes `score = practiceCount + distinctActiveDays` over the window and bands it at `>= 20` Strong, `>= 10` Steady, `>= 4` Building, `>= 1` Starting, `0` Starting.

Adopted. Band **names** are unchanged (Starting / Building / Steady / Strong), and the mastery ladder is unchanged.

Applies to `docs/engine/03-mastery-and-progress.md`.

### D-10 - The AI coach slot is rendered, as a dashed placeholder

The prototype renders the Feynman coach panel as a dashed-border box with a `Spark` icon, a "Coming later" pill and a disabled action. This **reverses the UI half of R-11**, which removed it.

One deviation, deliberate: the prototype fills the box with three rows of **sample coaching text**. Shipping fabricated feedback that reads as though a coach produced it would mislead. The production screen keeps the prototype's box, header, pill and disabled button, and replaces the three sample rows with one explanatory line. Flagged in section 7 as open.

Applies to `docs/ux/02-screen-specs-exercise.md`, `docs/architecture/adr/0016-ai-readiness.md`, issue `023`.

### D-11 - Demo data is a debug-only affordance

The prototype offers "Explore with demo data" on Welcome and "Load demo data" in Settings, jumping to Day 9 with history. Useful for QA and for screenshot fixtures; not a product feature.

Ships in **debug builds only**, behind `BuildConfig.DEBUG`. Applies to issues `016`, `034`, `039`.

### D-12 - Icons are the prototype's hand-authored set

`IteraIcons` defines 33 `ImageVector`s on a 24x24 grid, stroke width 1.9, round caps and joins. This settles issue `003`'s open question: **no `material-icons-extended` dependency**; port `IteraIcons.kt` as-is.

### D-14 - Brand fonts do not cover Cyrillic; a second pair is bundled

Measured from the original `@font-face` declarations: **Bricolage Grotesque** ships `vietnamese`, `latin-ext` and `latin`; **Instrument Sans** ships `latin-ext` and `latin`. Neither includes U+0400-04FF.

So English, German and Spanish are covered, and **Russian is not**. The prototype lets Cyrillic fall through to the platform default font, which varies by OEM and Android version - an uncontrolled substitution, and not acceptable for a shipping language.

**Decision**: bundle **Inter** (body) and **Inter Tight** (display) alongside the brand faces, and resolve the family from the active locale's script, plus always use Inter for user-authored text in any locale. Full reasoning, the selection rule and the coverage test in **ADR-0019**.

Applies to `docs/ux/04-design-system.md` section 2, `docs/i18n/00-localization.md`, issue `003`.

### D-15 - Localisation lands in its dependency issues; `041` only audits it

Localisation is **not** built in one late issue. Every part of it lands in the earliest issue that owns that surface:

| Part | Issue |
| --- | --- |
| `AppCompatActivity`, `locales_config.xml`, `AppLocalesMetadataHolderService`, the four `values-*` directories | `001` |
| `AppLanguage`, `currentLocale()`, `formatTime()` and the locale-aware formatter helpers | `002` |
| Bundled fonts, script-aware family selection, the glyph-coverage test | `003` |
| `LanguageSheet`, `LanguagePill`, `RadioDot` as shared components | `004` |
| The 14 techniques' content in all four languages | `008` |
| The Welcome language entry point | `016` |
| The Settings "Language & region" section, including runtime switching and its persistence check | `034` |
| Every screen's own strings, in all four languages | each screen issue, enforced by the definition of done section 8 |

`041` is a **final verification and audit** issue. It builds nothing. By the time it runs, the app is already fully localised; `041` proves it, and catches what drifted.

Consequence: issue `038` (previously "localisation verification pass") is retired, its content folded into `041`. The number is not reused.

Applies to `docs/i18n/00-localization.md` section 12, `docs/delivery/**`, issues `001`-`008`, `016`, `034`, `041`.

### D-16 - Time format is read-only and follows the system

The prototype shows a "Time format" row with a value and no behaviour. Production renders it as a **read-only** row - no chevron, not clickable - displaying the format resolved from `DateFormat.is24HourFormat(context)`, with a caption naming Android settings as the place to change it. There is no in-app override in the MVP.

All times are formatted with `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)` against the active locale, which already honours the system preference.

### D-17 - Real time pickers

Morning and evening times are set with a Material 3 `TimePicker` in a bottom sheet styled like `LanguageSheet`. The prototype's 30-minute stepping is scaffolding and must not ship. The row and value chip around it are reproduced exactly.

### D-13 - `appcompat` is a required dependency

Runtime language switching on Android 12 and below needs `AppCompatActivity` plus `AppCompatDelegate.setApplicationLocales`. `androidx.appcompat` joins the dependency baseline, and `MainActivity` extends `AppCompatActivity`, not `ComponentActivity`.

Applies to `docs/architecture/06-dependency-catalog.md`, issues `001`, `002`, `041`.

## 6. Where the prototype loses (P-series)

Per the standing rule: a product or business rule beats a prototype simplification. Each of these is a place the prototype is thinner than the product, not a place the product should shrink.

| # | Product rule | What the prototype does | Resolution |
| --- | --- | --- | --- |
| P-01 | Due reviews appear in the day's plan (FR-03) | Today has three fixed steps; the review screen is reachable only from Train | Keep the rule. A due review adds a fourth `StepRow`, styled exactly like the others, and the counter becomes `done / total` rather than `done / 3`. |
| P-02 | Mastery `Applied` requires 6+ uses **across 2+ weeks** (R-13) | `uses >= 6`, no span check | Keep the rule. The prototype dropped the span because it has no real history. |
| P-03 | The focus timer survives process death without drift (FR-31, ADR-0009) | `rememberSaveable` countdown, no service | Keep the rule: wall-clock `endsAt` plus a foreground service. The prototype's **visual** timer spec is authoritative; its persistence is not. |
| P-04 | `programDay` advances only on a completed day, and an abandoned day never advances it (R-06) | `startNextDay()` always increments | Keep the rule. |
| P-05 | Spaced repetition is a real 1/4/9/21/60 ladder with grading (ADR-0012) | The review screen is static sample content; the ladder is drawn with fixed stages | Keep the rule. The prototype's **ladder rendering** is authoritative. |
| P-06 | History can browse months, bounded by program start (FR-34) | Current month only | Keep the rule; add bounded month navigation using the prototype's calendar rendering. |
| P-07 | Two-tier reset, journal export, notification permission flow (ADR-0014, R-09, FR-26) | Rows exist but do nothing | Keep the rules; implement behind the prototype's row styling. |
| P-08 | Plan generation is deterministic and idempotent (ADR-0011) | No plan engine at all | Keep the rule. |
| P-09 | No user-authored text is ever logged (FR-43) | Not applicable - no logging | Keep the rule. |

## 7. Locked decisions

Previously open; now settled. These are binding, not defaults.

| # | Decision |
| --- | --- |
| Q-01 | **The AI coach container ships.** Keep the prototype's dashed box, `Spark` icon, "Coming later" pill and disabled button. **Do not render fabricated or generated coaching feedback** - the three sample rows are replaced by one explanatory line until a real provider exists (D-10, ADR-0016). |
| Q-02 | **Time format follows the Android system 12/24-hour preference.** There is no in-app setting in the MVP. The Settings row is a read-only display of the resolved format (D-16). |
| Q-03 | **A real Material 3 `TimePicker` is used in production**, in a bottom sheet styled like `LanguageSheet`. The prototype's tap-to-add-30-minutes interaction is a placeholder and must not be shipped (D-17). |
| Q-04 | **The generic exercise runner is kept** for techniques that do not need a bespoke interaction: 5-second rule, Two-list strategy, Information diet, 1% improvement. They route intro -> result with an "I did it" primary. The data-driven template body (ADR-0007) remains available if any of them is upgraded later. |
| Q-05 | **Resolved by measurement and superseded by D-14.** Both brand faces are Latin-only; an uncontrolled Cyrillic system fallback is rejected. A Cyrillic-capable pair is bundled and selected deterministically (ADR-0019). |
| Q-06 | **Progress keeps both entry points**, to History and to Library, as the prototype does. |

No open questions remain. If implementation surfaces a new one, record it here before deciding it in code.

## 8. Pre-prototype resolutions (R-series)

The 16 R-series items resolved `Itera.html` - a bundle of static artboards, since removed from the repository - against the original PRD. They are retained because later documents cite them, but their **source is now obsolete**.

The artboards themselves are no longer in the repository; these rows record where a decision came from, not where to look.

| Still in force | Superseded by the prototype |
| --- | --- |
| R-01 four tabs, R-02 review intervals, R-03 one-to-one skill map, R-04 time budgets, R-05 three onboarding steps, R-06 training-day advance, R-07 Day 14 combination, R-08 always-dark screens, R-09 journal export in MVP, R-12 `snoozed` state, R-13 mastery ladder, R-14 skill band names, R-15 added surfaces, R-16 renumbering | R-10 (no sign-in link) - the prototype's Welcome has no sign-in either, so the outcome holds but the reasoning now comes from `design/`. R-11 (hide the AI panel) - **reversed** by D-10. |

Every R-series item above that is "still in force" is independently confirmed by the prototype, which is a strong signal that the earlier transcription was accurate.

## 9. Architecture decisions

| Topic | ADR |
| --- | --- |
| Single Gradle module | `adr/0001` |
| MVVM with one immutable `UiState` per screen | `adr/0002` |
| Repository and use-case boundaries | `adr/0003` |
| Room / domain mapping | `adr/0004` |
| Catalog as a bundled asset | `adr/0005` |
| Exercise result representation | `adr/0006` |
| Generic vs specialised exercise UI | `adr/0007` |
| Navigation | `adr/0008` **(revised for the prototype)** |
| Timer persistence | `adr/0009` |
| WorkManager scheduling | `adr/0010` |
| Engine determinism | `adr/0011` |
| Spaced repetition model | `adr/0012` |
| Progress derived on read | `adr/0013` |
| Data reset | `adr/0014` |
| Content versioning | `adr/0015` |
| AI readiness | `adr/0016` **(revised by D-10)** |
| Localisation | `adr/0017` |
| The prototype as the UI reference | `adr/0018` |
| Typography and script coverage | `adr/0019` **(new)** |

## 10. Changing a decision

If an implementation forces a change to anything here, update this file **and** the affected documents in the same change. That is part of the definition of done.

## 11. Issue 001 compatibility corrections (2026-09-23)

User-approved: KSP2 has independent versioning; remove the Kotlin-prefix constraint.
Use KSP 2.3.12 with AGP 9.4.1 built-in Kotlin and Kotlin/Compose compiler 2.2.10.
The old KSP 2.2.10-2.0.2 fails while registering generated Kotlin source sets.
Do not disable built-in Kotlin or the new DSL. The source-set compatibility escape
hatch is a last resort, not part of this baseline. Hilt 2.59.2 supplies AGP 9 support;
Room 2.8.4 and Roborazzi 1.61.0 replace the older illustrative catalog versions.

All four variable fonts live in `res/font/`, loaded by resource ID, in both projects.
Issue 001 bundles and smoke-tests them; script-aware typography and glyph coverage
remain issue 003. This replaces the earlier asset-loading instructions.

ADR-0020 records the Compose lint enforcement required by issue 001; buildSrc is build tooling, not a production module. Kotlin runtime 2.2.20 is recorded separately from compiler 2.2.10.

## 12. Backlog consolidation (2026-09-23)

The 40 detailed issues were consolidated into eleven milestone issues (`docs/issues/README.md`); the detailed issues are preserved unchanged in `docs/history/issues-detailed/`. No product, UX or architecture decision changed. Documents in this repository that cite an issue number (including D-15 above and the test matrix) use the **detailed** numbering; translate with the mapping in `docs/issues/README.md`.

Consequences recorded here:

- **D-15 still holds.** Localisation is built by the milestone that introduces each string (001-008); the audit formerly in `041` is part of milestone `009`.
- **Analytics core moves earlier.** The `Analytics` interface, event catalogue and `event_log` sink are built in milestone `002`; each milestone instruments its own flows. `NoUserTextLoggedTest`, the trim worker and gap-filling remain in `009`.
- **`ReminderScheduler` seam.** The interface and a no-op binding are introduced in milestone `004`; `009` binds the real scheduler. Settings (`008`) no longer depends on notifications.
- **Focus notification channel.** Milestone `006` creates the `focus_session` channel for the timer's foreground service; `009` adds the other channels to the same registry.
- **Verification is proportional.** Milestones `002`-`008` verify the changed area; exhaustive accessibility, localisation, golden, regression and release verification is done once, in `009`-`011`.

## 11. Milestone 002 storage-contract clarifications (2026-09-23)

Approved during implementation of milestone 002:

- The schema enumerates **10** application tables and DAOs, not 11. Implement that enumeration; do not invent a table. Domain section 9 enumerates **six** repository interfaces, not nine; add the separately specified FocusTimerRepository as the seventh.
- `HistoryEntry` is a storage projection of a `PlanActivity` and its calendar `date`. Coach request carries `topicTitle` and `explanation`; response carries `strengths`, `gaps`, and `questions` as lists of strings. These are contracts only, with no coaching implementation or generated feedback.
- `UserPreferences` includes nullable `lastSeenDayComplete`, matching the sixteenth DataStore key. Defaults belong to the model. Constructor checks enforce local structural bounds; plan composition and result/type matching remain the responsibility of the engine/write boundary.
- Milestone 002 implements storage reads/simple writes and raw progress queries. Engine-dependent repository methods (plan generation/manual practice, coordinated completion, review scheduling/submission, classified progress summaries) are deferred to milestone 004. No placeholder success, invented plan or invented mastery classification is returned. Narrow storage implementations remain usable without those engines.
- Entity-to-domain mapping loses persistence-only fields by design. Reverse mapping requires the original entity or explicit persistence metadata, preserving copy keys, creation timestamps, topic links, and generator versions rather than reconstructing them from rendered text.
- Time formatting explicitly uses Android's 12/24-hour setting with a locale-specific best pattern. `ofLocalizedTime` alone follows locale defaults and does not honor a user's Android override.
- Reset tiers enumerate all ten tables: program reset deletes six training/review tables and resets technique unlock facts; erase-all additionally deletes topics, habits and analytics. Execution of reset remains milestone 004.

- The concrete 002 repository bindings use `TrainingPlanStorage`, `ReviewStorage`, and `ProgressStorage`; the full repository interfaces extend those contracts for composition in 004. No engine-dependent binding is installed early. `CopyResolver` ports only the 14 technique names/instructions needed to resolve stored rows; catalog assets/loading remain 004. Unknown event strings are prevented with closed parameter enums, including route class names and setting values.
- Additional indices cover foreign-key child columns so cascades do not scan tables; no columns or additional tables are introduced. Room entities store primitives, with explicit date/time/enum converters available to mappers and future queries.

## Milestone 003 prototype reconciliation (2026-09-24)

- The prototype contains **31 named icon vectors and 24 routes**. Older counts of 33 icons / 20 routes are transcription errors; port the complete enumerated set without inventing additions.
- ADR-0019 also applies to mixed-script language names: native language names use bundled Inter so Russian remains covered in a Latin UI. Both implementations share that treatment.
- Required accessibility adaptations (44 dp targets, selected/toggle semantics, vertical segmented controls above font scale 1.6, scrollable language sheet, reduced-motion handling) follow detailed 004 / accessibility requirements and are reflected in the prototype.
- Added empty/error/skeleton and D-17 real time-picker components also live in the prototype for comparison. Material typography uses app tokens in both projects. Feature screen wiring remains with its designated milestone.

- Milestone 003 large-text checks additionally require ValueRow to reserve label space when its value wraps and IntervalLadder labels to wrap instead of truncating. Both implementations include these accessibility corrections; ordinary short-value layouts retain prototype geometry.

## Milestone 004 implementation decisions (2026-09-25)

Recorded while implementing the content and training engine. Each resolves a gap or conflict in the documents it names.

- **Pace affects workload, not introduction cadence (final, 2026-09-25).** Every pace introduces and unlocks techniques at the curriculum rate (`programDay >= introDay`, `docs/engine/04`; engine 01 section 6, "the curriculum is the product"). Pace changes only the review cap (Gentle 1 / Standard 2 / Intense 3, plus 1 on a LONG budget) and the focus suggestion (Intense prefers Deep Work once unlocked). The earlier PRD 07 wording that Gentle introduces a technique every other day is withdrawn; PRD 07 section 5 now states this rule.
- **Progress uses D-09 and one 14-day window.** Issue 013's 10-day dot window, its threshold table and its artboard rows (for example "Habits (7,5) Building") are superseded by D-09 and `docs/engine/03` sections 3-4. `ProgressDerivationTest` asserts the D-09 bands over the derived log.
- **Combination rows.** The parent row (`exerciseType = COMBINATION`) takes the first step's technique id. Steps are separate required rows with `source = COMBINATION`, placed directly after the parent in `orderIndex`; their position in the chain is `copyArgs.step`. The Daily reflection step folds into the evening reflection and gets no row. When the parent is completed, any step still open is COMPLETED if the chain result names it, otherwise SKIPPED. The parent row is excluded from mastery counts; the steps carry the INTEGRATED credit.
- **Activity shapes.** Practice prompts are TEMPLATE rows completed under the `always` rule. A Day 15+ practice day runs the technique's own exercise with `source = PROGRAM` and `activity_program` copy. MORNING rows are generated AVAILABLE and later rows SCHEDULED. Availability is computed from each row's stored `scheduledAt`, so a plan stays stable for its day. Completing from AVAILABLE or SNOOZED passes through `Start`.
- **Program-day advance.** `current_program_day` lives in DataStore, so it is written after the Room transaction commits, as a compare-and-set on the completed day's `programDay`. `AdvanceProgramDayUseCase.reconcile()` recovers an advance lost between the commit and that write.
- **Mastery counting.** A completion counts unless it is the combination parent, or a FOCUS_SUGGESTION made before the technique's `introCompletedAt`. This replaces 002's copyKey filter. Effect 1 sets `introCompletedAt` only for an unlocked technique.
- **Reviews.** The one-item-per-`(techniqueId, topicId)` rule applies to topic-bound (Feynman) items. Each Spaced-repetition item has no topic, so every completion creates a new item. `review_item.prompt` stores the topic title (user text); the UI renders the `review_prompt_*` sentence.
- **Reflection persistence.** Completing the reflection writes `reflection_entry`, which is the source of `carryOverIntent`. Skipping it writes `skipped = 1`.
- **Premortem mitigation.** The mitigation becomes an optional MANUAL TEMPLATE row (`activity_mitigation` copy, action text in the draft), attached to today's plan when one exists and otherwise to the premortem's own day.
- **Template bodies.** Under Q-04, the 5-second rule, Two-list strategy, Information diet and 1% improvement carry an empty template with the `always` rule. The 2-minute rule is as specified. 80/20 is a `pickOne`, and Spaced repetition is a `textInput` named `item`, which becomes the review answer. Content added in 004 beyond the ported strings: the Day-14 step-1 prompt and hint (from the style guide, section 6), the Spaced-repetition item label and placeholder, and `activity_mitigation`, all in four languages.
- **Model and seam changes.** `Technique.retired`; `PlanActivity.weeklyLookBack` (read from the reflection's `copyArgs`) and `isCombinationStep`; `TechniqueProgress.nextLevelHint` is a structured `LevelHint` because the UI owns the copy; `SkillProgress.detail` is removed because the UI renders the plural; `ProgressSummary` gains focus minutes; `Analytics.append` for the in-transaction effect 7; `PreferencesRepository.clear()` for erase. The id `feynman` used in the 002 seed, CopyResolver and analytics was corrected to the frozen `feynman_technique`.
- **Integration tests run on the JVM.** `PlanPersistenceTest`, `ProgressPerformanceTest`, the reset tests and `DailyLoopIntegrationTest` run under Robolectric against real Room databases (file-backed for reopen), not as instrumented tests. The 300 ms budget is measured there; device measurement belongs to milestone 009/010.

## Milestone 005 implementation decisions (2026-09-25)

- **Carry-over reads only the latest reflection.** `latestIntent()` returns the most recent reflection's "change for tomorrow", or nothing when that reflection was skipped; an older answer is never resurfaced after a skipped night.
- **Reflection storage.** Chips are stored as stable ids (`started_late`, `focus_done`, ...), rendered in the current language. Answers are stored verbatim, including an empty question 1 when the pre-fill was cleared. `TrainingPlanRepository.draft()` reads the autosaved draft.
- **Weekly look-back** (not in the prototype) shows days trained and techniques practised over the last 7 days, focus minutes, and the "change for tomorrow" answers carried into that week's days.
- **Tomorrow preview** past the authored curriculum says "Practice day"; it does not predict a generated combination, because generation can fall back to a single practice.
- **Demo data (D-11)** is a debug-source-set `DemoDataLoader` bound through `@BindsOptionalOf`; release builds contain no implementation. It erases local data and runs the real engine over eight past days (one rest day) with a stepped clock.
- **`POST_NOTIFICATIONS`** is declared in the manifest and requested once from Rhythm; a denial turns all four reminder flags off immediately.


## Milestone 006 implementation decisions (2026-09-26)

- **Routes.** An `ExerciseRun(activityId, technique)` route hosts the template body's run step; the existing `TwoMinute` route renders the same body. `Feynman` holds both steps (the step is saved state) and `FeynmanFeedback` renders the same screen. Every result opens directly above Today, whether or not the exercise came through its intro, so back never re-enters an exercise.
- **Completion moment.** The run step's primary completes the activity; the result screen then saves "How did it feel?" and the note on change through `TrainingPlanRepository.updateFeedback`. "Done" is navigation only.
- **Snooze.** From 20:00 no snooze fits the rule, so the intro's secondary reads "Not now" and only closes.
- **Template layout.** An emphasised instruction followed by an input block renders as that block's subtitle (the prototype's 2-minute layout). The catalogue's completion rule gates the primary (2 ticked tasks for the 2-minute rule), not the prototype's `done > 0`. A checklist item's stopwatch starts at the first keystroke of that item in the add field.
- **Focus timer.** `FocusSessionController` is the single writer for the screen, the notification actions (a broadcast receiver) and app start (restore runs on a cold start and deep-links to the session, its result, or the combination chain). A paused session stops the foreground service and shows a plain notification with Resume and End; the notification's End cannot confirm, so it ends directly (the sub-minute rule still applies). The pre-timer sheet is part of the dark focus route and needs a task. `FocusSessionController.sessionRunning` is the state milestone 009's workers read to stay quiet.
- **Eisenhower.** Once every task is sorted, the Do-now choice is a radio group under the matrix; finishing needs everything sorted and, when Do now is not empty, one choice.
- **Feynman.** The prototype's hardest-part chips are sample-specific; production uses four generic chips stored by id. The footer interval is `INTERVALS_DAYS[stage]` of the topic's active item (1 for a new topic). `NoOpCoachFeedbackProvider` is bound; the coach box shows its one explanatory line.
- **Review.** "Compare" is not gated on text (issue 026: an empty answer is the honest "I couldn't recall it"), deviating from the prototype's `enabled = text.isNotBlank()`. "Days ago" counts from the source activity's completion. A Spaced-repetition item (no topic) shows "Something you chose to remember".
- **Premortem.** A project name and three reasons are required; tapping a reason focuses it and reveals move up / move down / remove. The primary "Add to today" needs an action; a ghost "Finish without adding" completes without one. A mitigation row's intro shows its action as the task.
- **Habit stacking.** Slots start blank ("After I ___, I will ___."); each chip group has a "Something else" chip for custom words. Saving archives the active stack (`insertHabitStack`) and always hands the new stack to `ReminderScheduler.scheduleHabitNudge`, which schedules it when enabled and otherwise cancels it and any archived stack's nudge.
- **Combination.** Per the 004 row shape the Day-14 chain persists a parent plus three step rows; the reflection step is shown, never completed in the chain, and folds into the evening reflection. Each step completes its own row at once; the parent completes itself with the step summaries when the last step before the reflection is done. Steps of generated chains that are not Eisenhower, a pick-one template, focus or an empty template open their own screen.

## Milestone 007 implementation decisions (2026-09-26)

- **Prototype reconciliation.** Preserve Train.kt's current-week path, four mastery dots, 44 dp library tokens and unlock-day sort (Daily reflection first). The older six-segment strip and mastery-first artboard order are superseded by the prototype. Daily reflection is always unlocked, so Day 1 has two unlocked techniques, not one.
- **Required additions.** Pin the library filter; announce future path nodes and locked library rows; hide locked practice/history; show factual use/day counts and result-derived recent summaries. Reflect these additions in the prototype. Train's curriculum-position bar is retained without a percentage label; after Day 14 the header has no total or bar.
- **Generated days.** Do not predict future generated combinations. Beyond the authored curriculum, show “Practice day” until a stored plan supplies its actual exercise. Today's node opens that plan's main exercise; past nodes open the existing History placeholder until 008 supplies day selection.
- **Manual practice.** Empty-template techniques open the existing run step with their concrete task and an explicit “I did it” action; opening detail or pressing Practice now never grants completion. Timer labels use the actual focus duration. Repeated taps share one pending manual activity, including retry after a navigation/start failure.
- **Recent practice.** An indexed, limited query returns the five newest completed activities for the selected technique across all months. Unreadable result payloads retain the technique name and note. No History screen work is included.
- **Today's position.** Use today's stored plan day after completion, as Today does; preferences already point to the next program day. Move the path when the next day's plan exists.
- **Explicit review.** The Train card can open the next due review even after the plan's automatic review slots are exhausted. Reuse an existing open activity or add one optional review activity transactionally; the automatic scheduling cap is unchanged.

## Milestone 008 implementation decisions (2026-09-26)

- **Reset copy.** The detailed issue references confirmation copy absent from the copy deck. Reset program says: “Delete your training history, notes, reflections and reviews and return to Day 1. Keep your settings, learning topics and habit stacks. This cannot be undone.” Erase everything says: “Delete all local training history, notes, reflections, reviews, learning topics, habit stacks and settings. Return to Welcome. This cannot be undone.” Cancel precedes the destructive action.
- **Identity.** The optional local display name is a DataStore preference, kept by Reset program and removed by Erase everything. It is never an analytics parameter.
- **History.** Calendar dots count completed activities only; skipped entries remain visible in the day list. The cap is three (the detailed test's mention of four is a typo). Read-only results never reuse the editable exercise result screen.
- **Export.** Load one month at a time and stream days into the writer. The 12-month range starts one year before today plus one day; the 30-day range includes today. The 24-hour worker sweep remains with milestone 009's EventLogTrimWorker; export also removes expired cache files on use.
- **Export reflection layout.** The format's block-quote rule applies to reflection answers too: put each translated question label on its own list line, followed by the escaped answer in a block quote. This resolves the example's inline answers conflicting with its rule for all user-authored text.
- **Offline language availability.** Disable App Bundle language splitting so all four bundled languages remain available to the existing AppCompat runtime picker without a download.

## Milestone 012 implementation decisions (2026-09-27)

Decisions the voice documents left to implementation; none changes a product rule.

- **Command contexts.** Today offers StartFocus and ShowCurrentRecommendation. Focus setup offers StartFocus; the running/paused timer offers Pause, Resume and End. Exercises offer AddItem/CompleteItem only for their one active list (template with exactly one checklist, Eisenhower entry, Premortem reasons, the combination's Eisenhower entry step) plus CompleteCurrentExercise where a single completion action exists. A template with more than one checklist rejects item commands rather than guessing a list.
- **No voice completion where the action needs user input or would finish early.** Review completion needs the user's own recall grade, so Review offers only ShowCurrentRecommendation. A combination parent completes through its steps, so CompleteCurrentExercise is not offered there. Feynman and Reflection count only the final step's Finish/Done.
- **Premortem completion** by voice uses "finish without adding"; the confirmation states that the action is not added to Today. Adding a new activity stays a touch action.
- **ShowCurrentRecommendation.** On Today the hero is already on screen: feedback only, nothing created. In an exercise it asks first (existing leave copy), takes the screen's normal leave path (draft kept, activity released) and opens Today. From focus it opens Today directly; a running session keeps running.
- **StartFocus from Today** uses only Today's actionable focus entry and always opens its setup (Today has no task), carrying the requested length as the `FocusSession.requested` route argument; an unoffered length is explained in setup, never rounded. EndFocus raises the existing end-session dialog.
- **Spoken minutes** cover 15/25/50 and the seeded suggestions 25/30/50 (Pomodoro's 30-minute estimate is a Today seed).
- **Availability.** The mic and command action are shown on every API level. A tap walks the ADR-0022 chain: on-device (API 31+), else the resolvable system default after its consent, else a picker of installed recognition apps with a named consent, else an explanation that voice is unavailable (amended 2026-09-28). Missing language models are detected from the recogniser's language errors, not a pre-check; settings links go to the platform voice-input settings.
- **Placement.** The "Voice command" action sits end-aligned under each exercise/focus top bar and after Today's steps, never in the hero. The field mic sits in the note field's end padding. Prototype and production carry the same affordances and `milestone012.xml` catalogues.

## Voice recogniser amendment (2026-09-28)

- **Decision.** The user replaced "strictly on-device or unavailable" with ON_DEVICE → usable SYSTEM DEFAULT → USER-SELECTED INSTALLED PROVIDER → UNAVAILABLE ([ADR-0022](architecture/adr/0022-voice-recognition-and-privacy.md)).
- **System default** means only the component in `Settings.Secure.voice_recognition_service`, and only when it resolves. The assistant app is never inferred as the default; an empty or hidden selection counts as absent.
- **Installed providers** are chosen explicitly from a picker (app label and icon, nothing pre-selected) and need a consent naming the app; the component is stored only on acceptance. No first-installed fallback.
- **Unusable provider.** Disappeared/disabled, a bind or service error before ready, or no callback within 8 seconds: the choice is cleared and the picker returns with an explanation. No fall-through to another provider.
- **Consent UI** is an inline voice-panel state (same panel as the microphone rationale), before the Android permission prompt. Settings → Voice revokes system consent and changes/clears the app.
- **Verified device limitation.** On the vivo V2405A (Android 16) there is no on-device recogniser, the system selection is empty, and Google's recogniser is hidden and unbindable; the only discoverable provider (Claude) rejects Itera's requests. Voice therefore still cannot recognise speech on that device.

## Milestone 013 decisions: built-in offline voice (2026-09-28)

- **Order (locked by the user).** ON_DEVICE → ITERA OFFLINE (Vosk, model for the current language installed) → usable SYSTEM DEFAULT → USER-SELECTED → UNAVAILABLE. A missing or damaged model continues the chain; the external fallbacks stay.
- **Consent.** None beyond `RECORD_AUDIO` for the offline recogniser: audio never leaves the device.
- **Models.** One per language, pinned official Vosk small models; never in the base APK or Git. Play: one on-demand asset pack per language, downloaded only on the user's tap. APK/sideload: document-picker import of the pinned archive. "Erase all data" keeps models (no user data); Settings → Remove deletes one.
- **Command grammar.** Only where the context's commands are all fixed phrases (focus, Today); exercises with Add/Complete stay free-form. Dictation is always free-form one-best; up to 3 alternatives are requested only with a grammar, where competing interpretations matter. (On the vivo, «Позвонить сантехнику завтра утром» came back as «звонить сантехнику завтра утром» with both n-best and one-best decoding, so the alternatives setting is not the cause; see milestone 013 verification.)
- **Model lifecycle.** One model loaded app-wide; closed on background and when another language is needed.
- **Permissions.** Play Asset Delivery adds `FOREGROUND_SERVICE_DATA_SYNC` and its extraction service; no `INTERNET`. The merged set is pinned in `OfflineTest`.

- **Closing (2026-09-28, product owner).** 013 complete. Spanish device run waived (automated and localisation coverage remain). Small models accepted for MVP; EN/DE free-form dictation is best-effort, grammar commands are the reliability-critical path. Recognition should begin after the Listening state appears; the first-syllable loss on an immediate start moves to 010. Sideload APK packaging and real Play Console delivery move to 011.

## Caller-provided audio (2026-09-28)

- **Decision (user).** On API 33+, system-default and user-chosen providers receive Itera's own 16 kHz mono PCM16 capture through `EXTRA_AUDIO_SOURCE`; on-device and API 26-32 are unchanged. Audio is memory-only for the phrase, never stored, uploaded by Itera or logged ([ADR-0022](architecture/adr/0022-voice-recognition-and-privacy.md)).
- **Errors.** Unsupported language and missing model are separate states; with caller audio a permission error is attributed to the provider.
- **vivo result.** Google's provider consumes the stream and Itera's capture is not silenced, but the platform's `RecognitionService` permission check rejects the provider's background app, so recognition still fails there. Russian is unsupported by that provider (error 12); Spanish needs its pack; English and German packs are installed.

## Milestone 009 implementation decisions (2026-09-27)

- Reminder IDs remain fixed per type; before posting, cancel the other reminder IDs (never the foreground timer). This resolves notification UX section 6's contradictory same-ID/per-type wording while enforcing one visible reminder.
- Reviews share the morning slot. With training off and reviews on, a due review can still be offered; with reviews off, morning copy never mentions a review. Habit nudges use the saved stack's own switch. No additional settings toggle is introduced.
- A paused focus session suppresses nudges too. Evening work rechecks in short intervals while paused, or at the running timer's expected end; it stops at 23:00. Other reminders are dropped during focus. All workers recheck persisted state.
- Maintenance (daily plan, weekly log trim, daily expired-export cleanup) runs independently of notification permission. Permission denial cancels all reminder work, not local data maintenance.
- Habit logging has no separate occurrence table in the shipped model. A completed habit-stacking activity today counts as logged; do not invent a new habit-tracking flow or match user text.
- Reminder bodies are capped at 59 Unicode code points including an ellipsis when user content or a translated technique name is long. Review-without-topic copy uses the existing generic review label.
