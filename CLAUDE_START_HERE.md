# Claude - start here

You are implementing **Itera**, a native Android app built with Kotlin and Jetpack Compose.

> **Note**: this file went missing from the working tree between documentation passes and was restored. It is the entry point referenced by `README.md`, `AGENTS.md` and `docs/README.md`. If you meant to retire it in favour of `AGENTS.md`, delete it and repoint those three references - but do not leave it dangling.

## Status

**Documentation phase: complete. All decisions are locked.** Milestones 001 and 002 are complete. Milestone 003 is implemented with passing tests/build checks; final visual acceptance remains pending. Milestone 004 is complete. See the [current milestone status](docs/issues/README.md#current-status-2026-09-24).

## The most important thing on this page

**`design/` is a running Kotlin + Jetpack Compose prototype of this app, and it is the source of truth for all UI and interaction.**

Open it in Android Studio and run it before you write any UI code. It has all 24 screens, the theme, the component library, the icons, navigation, and four complete languages. When you implement a screen, you open its prototype file and reproduce it - you do not design it, and you do not derive it from a Markdown table.

## Source priority

1. **Explicit product requirements** - `docs/prd/**`, this file, direct instructions.
2. **`design/`** - everything UI and interaction.
3. **`docs/**`** - architecture, persistence, engines, curriculum, scheduling, testing.

`Itera.html` - a bundle of 51 static artboards from an earlier design pass - has been **removed from the repository**. `design/` supersedes it. Documents that mention an artboard are recording why an older decision was made, not telling you where to look.

Where `design/` conflicts with a product rule, the product rule wins and the discrepancy is documented. The nine known cases are in `docs/00-source-of-truth.md` section 6.

## What to read

**In this order:**

1. [`docs/00-source-of-truth.md`](docs/00-source-of-truth.md) - authoritative. Source priority, how to use the prototype, every resolved conflict, and the locked decisions. Read this before anything else.
2. [`docs/ux/05-prototype-reference.md`](docs/ux/05-prototype-reference.md) - how to run `design/`, the screen-to-file map, and the porting checklist.
3. [`docs/README.md`](docs/README.md) - the map of the full documentation set.
4. [`docs/delivery/00-implementation-order.md`](docs/delivery/00-implementation-order.md) - 11 milestone issues.
5. [`docs/issues/README.md`](docs/issues/README.md) - the issue index, old-to-new mapping and verification scope.

Also read [`AGENTS.md`](AGENTS.md), the short version of the working rules.

## How to implement

1. Take the lowest-numbered milestone issue whose dependencies are all done.
2. Read it in full, the detailed issues it absorbs (`docs/history/issues-detailed/`), and every document it references.
3. **If it is a UI issue: run `design/`, walk the screen, open the file its "Prototype reference" section names.**
4. Implement only what its **Scope** names. Its **Non-goals** are binding.
5. Satisfy every acceptance criterion and write every test listed.
6. Compare the result side by side against the prototype, in light and dark, and record **deviations only** in the issue.
7. Check against [`docs/delivery/02-definition-of-done.md`](docs/delivery/02-definition-of-done.md), within the verification scope in [`docs/issues/README.md`](docs/issues/README.md).

**If an issue forces a decision the documentation does not cover: stop.** Record it in `docs/00-source-of-truth.md` or as a new ADR, then continue.

**If you change anything visual: change `design/` to match, in the same change.** A prototype that has drifted from the app is worse than none.

## Non-negotiables

Product decisions, not preferences. Each is enforced by a test.

- **No streak, no XP, no points, no percentage-complete, anywhere, in any language.**
- **Program days advance when the user trains, not by calendar.** Missing a day never resets anything and never shows a warning.
- **Offline.** No backend, no account, no subscription, no AI dependency. No `INTERNET` permission.
- **No user-authored text is ever logged or transmitted**, in any build, at any level.
- **No fabricated or generated coaching feedback is ever shown.** The AI coach container ships in its "Coming later" state; its content does not.
- **Four languages ship: English, Russian, German, Spanish.** Every new string needs all four, in the issue that adds it.
- **The home screen answers "what should I do right now?"** - one hero action, one short list. Not a dashboard.
- Rest days are shown honestly, never hidden or scolded.

## Locked decisions worth knowing before issue 001

| Decision | Where |
| --- | --- |
| Four fonts are bundled. The brand faces are Latin-only, so **Inter Tight / Inter** cover Cyrillic, selected by the locale's script. No uncontrolled system fallback | ADR-0019, D-14 |
| Localisation is built **across milestones 001-008**, not in one late issue. Milestone 009 only audits it | D-15, section 12 |
| Time format follows the Android system 12/24-hour preference. No in-app setting | D-16 |
| Real Material 3 time pickers. The prototype's 30-minute stepping must not ship | D-17 |
| The generic exercise runner is kept for the four techniques that do not need a bespoke interaction | Q-04 |
| Progress keeps both entry points, to History and to Library | Q-06 |

## Technical baseline

Already in the production scaffold: Kotlin, Compose, Material 3, Gradle Kotlin DSL, AGP 9.4.1, Kotlin 2.2.10, compileSdk/targetSdk 37, minSdk 26, package `com.wivernz.itera`.

Added by issue 001: Hilt, Room, DataStore, WorkManager, Navigation Compose, kotlinx.serialization, **appcompat** (required for per-app language switching), KSP, Kover, Roborazzi. Full list and exclusions in [`docs/architecture/06-dependency-catalog.md`](docs/architecture/06-dependency-catalog.md).

Single activity extending `AppCompatActivity`, single Gradle module with enforced package boundaries, MVVM with one immutable `UiState` per screen, coroutines and `StateFlow`.

`design/` builds independently with its own older toolchain. That is expected; do not try to unify them.

## Start

Milestone [`005`](docs/issues/005-onboarding-and-daily-flow.md) is implemented. Next implementation milestone: `006`, when requested. Keep [003's final visual acceptance](docs/issues/003-design-system-and-app-shell.md#verification-gap-2026-09-24) and 005's prototype comparison open until verified on a device.
