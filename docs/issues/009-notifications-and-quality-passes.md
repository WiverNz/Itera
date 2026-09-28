# 009 - Notifications & quality passes

**Status:** complete - implementation (2026-09-28). Remaining manual verification moved to [010](010-test-hardening.md#carried-over-from-009-2026-09-28). **Depends on** 005, 006, 007, 008, 012 | **Blocks** 010

## Goal

Make the app remind the user at the right times, complete the analytics privacy guard, and run the first full-app accessibility and localisation sweeps now that every screen exists.

## Included scope

- **Notifications and WorkManager** (old 033): the remaining channels (`training`, `reflection`, `habits`) added to the registry 006 created, `IteraNotifier`, all workers, the real `ReminderScheduler` replacing 004's no-op binding, boot/package-replaced/timezone receivers, every suppression rule (including during a running focus session), deep-link intents and stale-target fallbacks.
- **Analytics completion** (remainder of old 036): gap-fill any catalogue event whose call site is still missing, notification events, `EventLogTrimWorker` logic, `NoUserTextLoggedTest`, reset behaviour of `event_log`.
- **Accessibility pass** (old 037): audit every screen against `docs/ux/07-accessibility.md`, run the TalkBack script, font scales 1.0-2.0 in English and German, reduced motion, Switch Access; add `FontScaleTest` and `AccessibilityAuditTest`; fix findings.
- **Localisation audit** (old 041): integration and audit only. Strings, plurals, formatters and entry points were built by 001-008 and 012; this milestone diffs catalogues, checks plurals/concatenation/formatting, walks the app in German and Russian, checks pseudo-locales, runtime switching and persistence. A missing translation found here is fixed here, but is a defect of the milestone that introduced the string.
- Build and smoke-test a minified release build once here, so R8 problems surface before 011.
- Include 012's voice states, `voice_*` catalogues, permission-denied/unavailable paths, the ADR-0022 consent/picker states (amended 2026-09-28; the vivo device check is recorded in `docs/00-source-of-truth.md`), locale switching and no-transcript logging in the accessibility, localisation and privacy sweeps. Platform fallback must remain disabled.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/033-notifications.md`, `036-analytics-contract.md`, `037-accessibility-pass.md`, `041-localization-verification.md`.
- `docs/engine/06-workmanager-strategy.md`; `docs/ux/08-notification-ux.md`, `07-accessibility.md`; `docs/analytics/00-event-model.md`; `docs/i18n/00-localization.md`; ADR-0010, 0017; D-15, D-16.

## Key dependencies

- Needs every screen (005-008) and voice (012). Accessibility precedes the localisation walk because it can move layouts.
- Replaces the no-op `ReminderScheduler` binding; onboarding (005), habit stacking (006), day complete (005) and settings (008) start producing real reminders here without code changes on their side.

## Acceptance criteria

- [x] Four channels exist with documented importance; every notification type posts its documented copy and deep link; no exact alarms are declared.
- [x] `rescheduleAll()` is idempotent; each Settings toggle suppresses exactly its own reminder; changing a time or toggle reschedules.
- [x] Every suppression rule holds, including deferral of the evening reminder during a focus session; permission-denied schedules nothing.
- [x] `NoUserTextLoggedTest` passes; `event_log` is trimmed, kept by Reset program and deleted by Erase everything.
- [x] Every screen passes `AccessibilityAuditTest` and `FontScaleTest` (automated, green). The full TalkBack/Switch Access daily loops moved to 010.
- [x] All four catalogues are complete; plurals, positional arguments and formatters conform (localisation audit tests, green). Exhaustive four-language runtime/persistence walks moved to 010.
- [x] A minified release build launches and completes Day 1.
- [x] Only findings and their fixes are recorded (briefly, in this file); no full audit report.

## Required tests

`NotificationContentTest`, `NotificationSuppressionTest`, `ReminderSchedulerTest`, `RescheduleTest`, `WorkerIdempotencyTest`, `DeepLinkTest`, `AnalyticsFlowTest`, `EventLogTrimTest`, `ResetAnalyticsTest`, `NoUserTextLoggedTest`, `AccessibilityAuditTest`, `FontScaleTest`, `TranslationCompletenessTest`, `UntranslatedStringTest`, `UnreferencedStringTest`, `PluralCategoryTest`, `NoConcatenationTest`, `LanguageSwitchTest`, `LanguageHistoryTest`, `LocalizedRenderTest`, `PseudoLocaleTest`.

## Findings and fixes (2026-09-28)

- Replaced the no-op scheduler with unique WorkManager reminders, delivery-time suppression, focus-session deferral, habit nudges, immutable deep links and boot/time-change rescheduling. Added notification analytics, progression/review events and bounded local log trimming. Privacy/reset tests cover no user text, retention on Reset program and deletion on Erase everything.
- Channel names now refresh in the selected language on activity resume, including the AppCompat locale backport. The API 37 device test posts each reminder type through Android, checks four channel importance levels, immutable tap intents, single-visible-reminder replacement and foreground suppression. Live language-switch tests check German/Spanish channel names. A delivery integration regression covers running/paused focus deferral, the 23:00 cutoff and exactly one evening post after focus ends.
- Release 1.0.8's resource shrinking removed catalogue strings resolved by name, leaving the first-week list empty and training unavailable. Added an explicit resource keep file and a catalogue-key regression test. Checked all 82 dynamically referenced catalogue strings in the compiled APK. A locally signed copy of the optimized release (about 4.3 MB) completed onboarding, two-minute exercise, rating/note, reflection and Day 1 on API 37 in an isolated QA Android user. Small APK size is expected with optimization; missing resources were the defect.
- Corrected the voice panel's indistinguishable secondary button with a contrasting outline in production and `design/`; actions stack at large font scales. Also mirrored large-text fixes for the focus timer, Eisenhower labels, Feynman metadata and meaningful titles, and minimum-target/semantics fixes for History and habit-stack controls.
- Fixed non-positional string substitutions in all four catalogues. Background catalogue/notification copy uses the selected app language. Today/Library reload localized catalogue content; the exercise runner now refreshes translated copy without replacing answers, pending checklist input or stopwatches. The runner regression test covers an English-to-German change with user-authored text retained.
- Closed 003's final six gallery refreshes: Group, ValueRow, LanguageSheet, TimeRow, IntervalLadder and TimePickerSheet match production/prototype pixels in both themes on API 37 (system bars excluded). The earlier gallery comparisons remain recorded in 003.
- Automated render/semantics checks cover 31 representative screen/state fixtures in both themes, EN/DE at font scale 2.0, all four languages at 1.0/1.5, and Android's `en-XA`/`ar-XB` pseudo-locales. These do not establish manual TalkBack/Switch Access completion or exhaustive coverage of every dialog and populated state. Week-of-history language integration preserves stored results and resolves current-language copy. Both live language-picker entry points and force-stop persistence were exercised on API 37.

Verification: production `build lintDebug` passes (537 host tests, zero failures, formatting and coverage gate passed); all six `connectedDebugAndroidTest` tests pass on API 37. Prototype `assembleDebug lintDebug` passes. Existing lint warnings remain; neither lint run failed. Used `--no-daemon --max-workers=1` after intermittent KSP initialization failures; an emulator reboot cleared a stale UiAutomation registration before the successful device run. Production voice rationale, Android permission denial and return-to-manual-input UI were also checked on device. Version 1.0.8 is a development build per the user; no special upgrade migration was added.

Closed 2026-09-28 at the product owner's request as implementation-complete. The remaining manual verification (TalkBack/Switch Access loops, maximum font/display size, 005/006/012 visual comparisons, four-language runtime/persistence walks, reboot/process death, API 26-30, and the voice checks still relevant after 013) is owned by [010](010-test-hardening.md#carried-over-from-009-2026-09-28). Real installed speech-model behaviour was verified in milestone 013. Milestone 010 has not been started.
