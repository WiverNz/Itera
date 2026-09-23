# AGENTS.md

Instructions for any agent working in this repository. Read this before touching anything.

If `AGENTS.local.md` exists in this checkout, read it for machine-specific build, run, and verification instructions. It supplements this file and is not committed.

## What this is

**Itera** - a native Android app (Kotlin + Jetpack Compose) that teaches productivity techniques through daily practice. Documentation is complete; production implementation has not started.

## Repository layout

| Path | What it is | Status |
| --- | --- | --- |
| `docs/` | The full specification - the PRD, UX specs, architecture, data model, engines, testing,  delivery, and 40 issues | Authoritative |
| `design/` | **A running Kotlin + Compose prototype of the app.** 24 screens, theme, components, icons, navigation, four languages | The UI/UX source of truth |
| `app/` | The production Android app | An empty scaffold |
| `CLAUDE_START_HERE.md` | Entry point for implementation | |

## Source priority

Stated in full in `docs/00-source-of-truth.md` section 1. In short:

1. **Explicit product requirements** - `docs/prd/**`, `CLAUDE_START_HERE.md`, direct instructions from the user. These win.
2. **`design/`** - everything UI and interaction: layout, spacing, sizes, colour, type, icons, states, transitions, navigation, copy placement.
3. **`docs/**`** - architecture, persistence, engines, curriculum, scheduling, testing, delivery.

`Itera.html`, a bundle of static artboards from an earlier pass, has been removed from the repository; `design/` supersedes it.

When `design/` and a product rule conflict, the product rule wins and the discrepancy is documented. The known cases are listed in `docs/00-source-of-truth.md` section 6.

## Before implementing a UI issue

1. Open `design/` in Android Studio and **run it**.
2. Use "Explore with demo data" on Welcome to reach Day 9 with history.
3. Walk the screen you are about to build. Note what animates, what is disabled, what changes on tap.
4. Open the file named in the issue's **Prototype reference** section.
5. Reproduce it. Do not redesign it.

`docs/ux/05-prototype-reference.md` has the screen-to-file map and the porting checklist.

## Working rules

- **Read the issue and every document it names before writing code.** Issues are written so no product or architecture decision is needed while implementing.
- **If a decision is needed anyway, stop.** Record it in `docs/00-source-of-truth.md` or as a new ADR, then continue. Do not decide it silently inside an implementation.
- **Implement only what the issue's Scope names.** Its Non-goals are binding.
- **Check against `docs/delivery/02-definition-of-done.md`** before calling anything done.
- **If you change anything visual, change `design/` to match, in the same change.** A prototype that has drifted from the app makes every future comparison worthless.
- **Never import from `design/` in production code.** It is a reference, not a dependency, and is not part of the production Gradle build.

## Non-negotiables

Product decisions, not preferences. Each is enforced by a test.

- **No streak, XP, points or percentage-complete, anywhere, in any language.** Progress represents real practice only.
- **Program days advance when the user trains, not by calendar.** Missing a day never resets anything and never shows a warning.
- **Offline.** No backend, no account, no subscription, no AI dependency. The manifest declares no `INTERNET` permission.
- **No user-authored text is ever logged or transmitted**, in any build, at any level.
- **No fabricated or generated coaching feedback is ever shown.** The AI coach container ships in its "Coming later" state; its content does not.
- **Four languages ship: English, Russian, German, Spanish.** Every new string needs all four, added in the issue that introduces it - not deferred to the audit issue.
- **The home screen answers "what should I do right now?"** - one hero action, one short list. Not a dashboard.
- Rest days are shown honestly, never hidden or scolded.

## Technical baseline

Production (`app/`): Kotlin, Compose, Material 3, AGP 9.4.1, Kotlin 2.2.10, compileSdk/targetSdk 37, minSdk 26, package `com.wivernz.itera`. Single activity extending **`AppCompatActivity`** (required for per-app language switching), single Gradle module with enforced package boundaries, MVVM with one immutable `UiState` per screen.

Added by issue 001: Hilt, Room, DataStore, WorkManager, Navigation Compose, kotlinx.serialization, appcompat, KSP, Kover, Roborazzi, and four bundled fonts. Full list and exclusions: `docs/architecture/06-dependency-catalog.md`.

Four faces ship because the two brand fonts are Latin-only: **Inter Tight** and **Inter** cover Cyrillic and are selected by the active locale's script (ADR-0019). There is no uncontrolled system-font fallback.

Prototype (`design/`): AGP 8.7.3, Kotlin 2.0.21, compileSdk 35, package `com.itera.app`. Builds and runs independently. Version differences between the two are expected and fine.

## Commands

```bash
# production app
./gradlew build
./gradlew installDebug
./gradlew testDebugUnitTest
./gradlew lintDebug

# prototype - open design/ as its own Android Studio project
cd design && ./gradlew installDebug
```

Both apps can be installed at once: `com.itera.app` and `com.wivernz.itera` are different application ids. That is the intended setup for side-by-side comparison.

## Where to start

`CLAUDE_START_HERE.md`, then `docs/00-source-of-truth.md`, then `docs/delivery/00-implementation-order.md`, then issue 001.
