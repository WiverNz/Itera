# 008 - Progress, history & settings

**Depends on** 003, 004, 005 | **Blocks** 009, 012

## Goal

The user can see how far they have come, look back at what they did, configure the app, switch language at runtime, export their journal, and reset or erase their data.

## Included scope

- **Progress** (old 031): trained-days summary and day strip, reassurance line, five skill rows with bars, entry points to History and Library (Q-06).
- **History** (old 032): locale-aware calendar with skill dots, day-grouped entries.
- **You / Settings** (old 034): identity, daily rhythm (M3 time pickers), program (pace, focus areas, topic editor), **Language & region** (runtime `LanguageSheet`, read-only time-format row, D-16), appearance, notification toggles and denied-permission state, disabled "Coming later" coach row, privacy screen, Reset program and Erase everything, debug-only "Load demo data".
- **Journal export** (old 035): export sheet, `MarkdownJournalWriter`, ranges and destinations.
- Strings in four languages; analytics call sites for these flows.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/031-progress-screen.md`, `032-history-screen.md`, `034-settings-you-tab.md`, `035-journal-export.md`.
- `docs/ux/02-screen-specs-train-progress-you.md` sections 4-6; `docs/ux/08-notification-ux.md` (settings rows only); `docs/data/03-journal-export-format.md`; `docs/i18n/00-localization.md`; ADR-0013, 0014, 0017; D-11, D-15, D-16, D-17.
- Prototype: `design/app/src/main/java/com/itera/app/ui/screens/Progress.kt` (progress, history), `Profile.kt` (You, export sheet).

## Key dependencies

- Needs 004 (progress derivation, reset use cases) and 005 (onboarding rows reused by focus areas).
- **Settings no longer waits for notifications.** Old 034 depended on old 033; now reminder toggles and times only write preferences and call `ReminderScheduler.rescheduleAll()` through the 004 seam (no-op until 009). 009 verifies that the toggles actually drive reminders.
- Can be done before, after or interleaved with 007.

## Acceptance criteria

- [ ] Progress counts trained days in the window, shows the reassurance line and five skill rows; no streak, XP, points or percentage anywhere (`NoStreakLanguageTest`).
- [ ] History's calendar follows the locale's first day of week and standalone month names; entries are grouped by day, newest first.
- [ ] You sections appear in prototype order; every setting reads and writes immediately; theme applies with no restart.
- [ ] Choosing a language re-renders the app immediately and survives a force-stop (Android 13+ and an API 26-30 device); the time-format row is read-only.
- [ ] Reset program and Erase everything use the documented confirmation copy, delegate to 004's use cases, and leave the app in the documented state.
- [ ] The coach row is visible, disabled and captioned "Coming later"; "Load demo data" is absent from release builds.
- [ ] The exported Markdown matches the golden fixture; user text is escaped; rest and skipped days are shown honestly.
- [ ] New strings exist in en/ru/de/es; each screen was compared against the prototype in light and dark, with only deviations recorded.

## Required tests

`ProgressViewModelTest`, `ProgressScreenTest`, `ProgressHonestyTest`, `NoStreakLanguageTest`, `HistoryViewModelTest`, `HistoryScreenTest`, `YouViewModelTest`, `YouScreenTest`, `ResetIntegrationTest` (UI entry points), `MarkdownJournalWriterTest`, `ExportJournalUseCaseTest`, `ExportSheetTest`, `ExportPerformanceTest`.
