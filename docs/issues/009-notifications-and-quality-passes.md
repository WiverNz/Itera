# 009 - Notifications & quality passes

**Depends on** 005, 006, 007, 008, 012 | **Blocks** 010

## Goal

Make the app remind the user at the right times, complete the analytics privacy guard, and run the first full-app accessibility and localisation sweeps now that every screen exists.

## Included scope

- **Notifications and WorkManager** (old 033): the remaining channels (`training`, `reflection`, `habits`) added to the registry 006 created, `IteraNotifier`, all workers, the real `ReminderScheduler` replacing 004's no-op binding, boot/package-replaced/timezone receivers, every suppression rule (including during a running focus session), deep-link intents and stale-target fallbacks.
- **Analytics completion** (remainder of old 036): gap-fill any catalogue event whose call site is still missing, notification events, `EventLogTrimWorker` logic, `NoUserTextLoggedTest`, reset behaviour of `event_log`.
- **Accessibility pass** (old 037): audit every screen against `docs/ux/07-accessibility.md`, run the TalkBack script, font scales 1.0-2.0 in English and German, reduced motion, Switch Access; add `FontScaleTest` and `AccessibilityAuditTest`; fix findings.
- **Localisation audit** (old 041): integration and audit only. Strings, plurals, formatters and entry points were built by 001-008 and 012; this milestone diffs catalogues, checks plurals/concatenation/formatting, walks the app in German and Russian, checks pseudo-locales, runtime switching and persistence. A missing translation found here is fixed here, but is a defect of the milestone that introduced the string.
- Build and smoke-test a minified release build once here, so R8 problems surface before 011.
- Include 012's voice states, `voice_*` catalogues, permission-denied/unavailable paths, locale switching and no-transcript logging in the accessibility, localisation and privacy sweeps. Platform fallback must remain disabled.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/033-notifications.md`, `036-analytics-contract.md`, `037-accessibility-pass.md`, `041-localization-verification.md`.
- `docs/engine/06-workmanager-strategy.md`; `docs/ux/08-notification-ux.md`, `07-accessibility.md`; `docs/analytics/00-event-model.md`; `docs/i18n/00-localization.md`; ADR-0010, 0017; D-15, D-16.

## Key dependencies

- Needs every screen (005-008) and voice (012). Accessibility precedes the localisation walk because it can move layouts.
- Replaces the no-op `ReminderScheduler` binding; onboarding (005), habit stacking (006), day complete (005) and settings (008) start producing real reminders here without code changes on their side.

## Acceptance criteria

- [ ] Four channels exist with documented importance; every notification type posts its documented copy and deep link; no exact alarms are declared.
- [ ] `rescheduleAll()` is idempotent; each Settings toggle suppresses exactly its own reminder; changing a time or toggle reschedules.
- [ ] Every suppression rule holds, including deferral of the evening reminder during a focus session; permission-denied schedules nothing.
- [x] `NoUserTextLoggedTest` passes; `event_log` is trimmed, kept by Reset program and deleted by Erase everything.
- [ ] Every screen passes `AccessibilityAuditTest` and `FontScaleTest`; the TalkBack script completes the daily loop.
- [ ] All four catalogues are complete; plurals, positional arguments and formatters conform; the app runs in each language and switching persists across force-stop.
- [x] A minified release build launches and completes Day 1.
- [x] Only findings and their fixes are recorded (briefly, in this file); no full audit report.

## Required tests

`NotificationContentTest`, `NotificationSuppressionTest`, `ReminderSchedulerTest`, `RescheduleTest`, `WorkerIdempotencyTest`, `DeepLinkTest`, `AnalyticsFlowTest`, `EventLogTrimTest`, `ResetAnalyticsTest`, `NoUserTextLoggedTest`, `AccessibilityAuditTest`, `FontScaleTest`, `TranslationCompletenessTest`, `UntranslatedStringTest`, `UnreferencedStringTest`, `PluralCategoryTest`, `NoConcatenationTest`, `LanguageSwitchTest`, `LanguageHistoryTest`, `LocalizedRenderTest`, `PseudoLocaleTest`.

## Findings and fixes (2026-09-28)

- Replaced the no-op scheduler with unique WorkManager reminders, delivery-time suppression, focus-session deferral, habit nudges, immutable deep links and boot/time-change rescheduling. Added notification analytics, progression/review events and bounded local log trimming. Privacy/reset tests cover no user text, retention on Reset program and deletion on Erase everything.
- Release 1.0.8's resource shrinking removed catalogue strings resolved by name, leaving the first-week list empty and training unavailable. Added an explicit resource keep file and a catalogue-key regression test. Checked all 82 dynamically referenced catalogue strings in the compiled APK. A locally signed copy of the optimized release (about 4.3 MB) completed onboarding, two-minute exercise, rating/note, reflection and Day 1 on API 37 in an isolated QA Android user. Small APK size is expected with optimization; missing resources were the defect.
- Corrected the voice panel's indistinguishable secondary button with a contrasting outline in production and `design/`; actions stack at large font scales. Also mirrored large-text fixes for the focus timer, Eisenhower labels, Feynman metadata and meaningful titles, and minimum-target/semantics fixes for History and habit-stack controls.
- Fixed non-positional string substitutions in all four catalogues. Background catalogue/notification copy uses the selected app language. Today/Library reload localized catalogue content; the exercise runner now refreshes translated copy without replacing answers, pending checklist input or stopwatches. The runner regression test covers an English-to-German change with user-authored text retained.
- Closed 003's final six gallery refreshes: Group, ValueRow, LanguageSheet, TimeRow, IntervalLadder and TimePickerSheet match production/prototype pixels in both themes on API 37 (system bars excluded). The earlier gallery comparisons remain recorded in 003.
- Automated render/semantics checks cover 31 representative screen/state fixtures in both themes, EN/DE at font scale 2.0, all four languages at 1.0/1.5, and Android's `en-XA`/`ar-XB` pseudo-locales. These do not establish manual TalkBack/Switch Access completion or exhaustive coverage of every dialog and populated state. Week-of-history language integration preserves stored results and resolves current-language copy. Both live language-picker entry points and force-stop persistence were exercised on API 37.

Remaining acceptance evidence: full TalkBack and Switch Access daily loops (including voice confirmations), maximum display size/grayscale and exhaustive supporting-surface walks, the remaining 005/006 side-by-side visual checks, full four-language device walks and reboot persistence, real installed speech-model behavior, and API 26-30 language/timer checks. No API 26-30 image/device is installed here; available system images are 36.1 and 37.2. Do not treat the representative render tests as closing these manual gaps. Milestone 010 has not been started.
