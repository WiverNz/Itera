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
- [ ] `NoUserTextLoggedTest` passes; `event_log` is trimmed, kept by Reset program and deleted by Erase everything.
- [ ] Every screen passes `AccessibilityAuditTest` and `FontScaleTest`; the TalkBack script completes the daily loop.
- [ ] All four catalogues are complete; plurals, positional arguments and formatters conform; the app runs in each language and switching persists across force-stop.
- [ ] A minified release build launches and completes Day 1.
- [ ] Only findings and their fixes are recorded (briefly, in this file); no full audit report.

## Required tests

`NotificationContentTest`, `NotificationSuppressionTest`, `ReminderSchedulerTest`, `RescheduleTest`, `WorkerIdempotencyTest`, `DeepLinkTest`, `AnalyticsFlowTest`, `EventLogTrimTest`, `ResetAnalyticsTest`, `NoUserTextLoggedTest`, `AccessibilityAuditTest`, `FontScaleTest`, `TranslationCompletenessTest`, `UntranslatedStringTest`, `UnreferencedStringTest`, `PluralCategoryTest`, `NoConcatenationTest`, `LanguageSwitchTest`, `LanguageHistoryTest`, `LocalizedRenderTest`, `PseudoLocaleTest`.
