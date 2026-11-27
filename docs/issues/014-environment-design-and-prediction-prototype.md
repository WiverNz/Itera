# 014 — Environment Design and Prediction Testing prototype

Status: **implemented; prototype review and visual/accessibility sign-off pending** (2026-11-27).
Authorized by the owner's staged request. No production Prediction Testing, breathing tool or Communication category is included.

## Scope and references

Read [ADR-0023](../architecture/adr/0023-prediction-testing-proposal.md), the existing [research](../proposals/prediction-testing/research.md), [proposal](../proposals/prediction-testing/proposal.md), [roadmap](../proposals/prediction-testing/milestone.md), [curriculum](../prd/07-technique-curriculum.md), [content versioning](../data/05-migrations-and-content-versioning.md), [exercise UX](../ux/02-screen-specs-exercise.md), [design system](../ux/04-design-system.md), and [definition of done](../delivery/02-definition-of-done.md).

Prototype references: `Practice.kt` (HabitStackScreen), `Exercise.kt` (intro/result), `Reflection.kt`, `Train.kt` (TechniqueDetailScreen), `Progress.kt` (HistoryScreen), `Components.kt`, and the new `Prediction.kt`.

## Delivered behavior

Stage 1 changes existing task/help copy for Habit stacking, Information diet and 1% improvement, plus optional helper text on reflection questions 1 and 3. Examples cover materials, digital templates, work surfaces and bags; increasing distraction friction stays reversible and preserves essential/emergency/accessibility access. Evening preparation and next-opportunity evaluation reuse ordinary reflection and Today carryover. There is no requirement to prepare before starting, no extra checklist, new deadline, notification, or credit.

Both catalog assets advance to content version 2. Technique IDs, five skills, fourteen techniques, curriculum order, template keys, completion rules, Room schema 1 and historical result payloads remain unchanged. Three existing tests now derive expected catalog versions or mismatches from the loaded catalog. Existing reconciliation, reset, reflection, history and journal export paths remain in use.

Stage 2 is confined to `design/`:

1. Record the avoided task and prediction in one short note; optionally choose confidence.
2. Plan a small bounded action and an observable criterion. Save before acting.
3. See the frozen prediction and plan; leave, return later, or close without an outcome.
4. Record the observation.
5. Compare using Confirmed this time / Contradicted this time / Mixed evidence / Inconclusive.
6. Optionally reassess confidence, then record learning or explicitly choose no clear conclusion.
7. Keep a read-only review. Starting another experiment explicitly confirms replacement.

All outcomes have the same action and validation. Confidence can remain unchanged, rise, fall, or stay unrated. No experiment action calls training completion, mastery, spaced review or program advancement.

## Prototype boundary and design decisions

- Entry from Daily Reflection detail and the existing evening reflection; History opens the same record. Today retains its hero and existing task list.
- Two preparation pages, a readiness page, three review pages, one read-only result. Each page reuses Itera type, card, field, button and chip components, 22 dp vertical rhythm, scrolling content and sticky actions. Radio semantics, visible field labels and non-colour outcome labels are included. No new animations.
- Keyboard only in experiment fields. Strings ship in EN/RU/DE/ES; authored text remains unchanged on language switch.
- One record is stored as an atomic file in Android `noBackupFilesDir`. It survives leaving, activity/ViewModel recreation and process restart independently of program-day state. Preparation is frozen only after a successful write. Save failure retains the in-memory draft and prevents leaving through the in-app save action. An unreadable file is not overwritten. This synchronous small-file prototype adapter is not the future production repository.
- Existing prototype reflection answers and selected chips stay in AppViewModel when visiting the extension. They are cleared on next program day; unlike the experiment, the rest of the historical prototype remains in-memory and is not promised to survive process death.
- Both confirmed profile reset paths erase the prototype experiment. Explore with demo data does not erase it.
- Prototype intentionally retains just one record. Replacement is destructive and explicitly confirmed. Multi-record history, linked repeats, date-range journal export, Room migration, full reset integration and production input/privacy architecture stay in the gated Stage B backlog.
- The context is included in the prediction note, and the action's limit is included in its note, rather than adding two extra questions. Dates use the active locale. No ratings default to a numeric confidence.
- Test-only AndroidX dependencies are isolated to `design/`. Espresso 3.7.0 avoids the old reflective InputManager call on current emulators; see [AndroidX release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0).

## Acceptance and verification

- [x] Existing skills/techniques and program-day sequence preserved; no production experiment model or route.
- [x] Environment Design examples and guidance are present in all four locales, mirrored in the prototype.
- [x] Prediction preparation, neutral comparison, reassessment and lessons implemented in all four locales.
- [x] Local persistence, frozen snapshot, save/load failure handling and no-credit boundary implemented.
- [x] Production build, lint, format and tests pass.
- [x] Prototype build, unit tests, lint and emulator tests pass.
- [x] Full-app emulator walkthrough: entry, leave/resume, next program day and process restart.
- [ ] Visual approval of all new pages, both themes, all four locales and 2x text.
- [ ] Spoken TalkBack walkthrough and independent review.

### Validation constraints

The owner explicitly requested no display capture after stopping Computer Use. No further desktop capture is used. The existing prototype was built and launched before implementation; the Android Studio/demo visual walkthrough was interrupted. This deviation is recorded rather than claiming the prerequisite visual review passed. Automated Android interaction, semantic/text-layout checks and local persistence tests can proceed without capturing the display. They do not substitute for visual or spoken accessibility sign-off.

Review entry: install `design/app/build/outputs/apk/debug/app-debug.apk` (`com.itera.app`), choose **Explore with demo data**, then **Today → Evening reflection → Prediction testing · optional**. Daily Reflection detail and History provide alternate entry points. Ordinary reflection and Good night advance the prototype program day without finishing the experiment.

Only synthetic notes should be used for device verification. No desktop/device screenshots, recordings or authored-text diagnostics are required. Synthetic headless comparison renders are available under `app/build/outputs/roborazzi/` and the comparison report under `app/build/reports/roborazzi/`.

### Results recorded 2026-11-27

- Production `gradlew build --no-daemon`: passed, including 736 tests, lint, formatting and Kover verification. Lint reports 0 errors and 66 existing warnings. The sandbox initially blocked local Gradle/KSP cache access; the successful run used approved external execution.
- Prototype `assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest`: passed. Lint reports 0 errors and 173 existing warnings, with no findings in the new screen, storage or resource files. Four pure state tests and four Android tests cover prerequisites, all four evidence directions, unchanged/optional confidence, atomic persistence and frozen snapshot, save/load failure, a complete mixed-evidence review, and all seven pages across four locales and two themes at 2x text. Text-layout checks measure rendered line widths (not the paragraph's offered width) and permit only pixel rounding.
- Full app on headless Itera_Release_QA_36 (API 36): Explore demo → evening reflection → prediction; preserve the reflection draft on return; ordinary reflection/Good night advances the program day while the experiment remains open; force-stop/relaunch and Explore demo restore the prepared record; a confirmed outcome saves neutrally with no clear conclusion. No display images were captured.
- Static compatibility check: both JSON assets are structurally identical to version 1 apart from `contentVersion`; all new keys are populated in EN/RU/DE/ES. No production schema, result, mastery or review rules changed.
- No actual calendar-clock alteration, device reboot, spoken TalkBack or independent visual approval was performed. The prototype has no date-based expiry path; tests cover a later review date, and the device walkthrough covers program-day advancement and a real process restart.
- Headless Roborazzi comparison passed for the two affected production fixtures (Reflection and HabitStack), across their five existing variants. The resulting Russian Reflection and HabitStack renders were visually inspected: the new guidance fits without overlap. This is synthetic JVM rendering, not desktop/device capture. Comparison output uses shared basenames and retains the last variant; this inspection does not certify every variant. Existing goldens remain unchanged pending visual sign-off under `docs/testing/04-visual-regression.md`; strict golden verification will still report the intended text/layout differences until that review and refresh. Automated layout checks are not a substitute.

## Next gates

1. Review Environment Design wording and the interactive prototype, including confirmed and inconclusive outcomes.
2. Resolve visual/accessibility findings and decide whether the extension earns its interaction cost.
3. Only after explicit prototype approval, schedule a separate production milestone for Room migration, history/export, reset and lifecycle integration.
4. Keep Breathing Pause future-only and BLUF deferred.

