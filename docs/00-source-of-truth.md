# Source of truth

Status: **authoritative**. Every other document in `docs/` conforms to this one.
Last reconciled: 2026-09-23 (prototype pass).

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
