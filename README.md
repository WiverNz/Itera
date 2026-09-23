# Itera

A native Android app that teaches productivity techniques through daily practice, not through another list of things to do.

> **Status: specification and prototype complete; production implementation has not started.**
> `design/` is a working prototype you can run today. `app/` is still the unmodified Android Studio template. Implementation begins at [issue 001](docs/issues/001-project-bootstrap.md).

## What it is

Most productivity apps store your tasks. Itera teaches you how to work: one technique at a time, one small exercise a day, done in real life rather than in the app.

```
Today's training                              New
2-minute rule
Clear small tasks before they accumulate.
About 5 min · Habits

              [ Start today's exercise ]
```

Three touchpoints a day: one exercise in the morning, an optional focus block during the day, a two-minute reflection at night. Techniques unlock one a day across fourteen days, then start combining.

Ships in English, Russian, German and Spanish, switchable at any time without a restart.

## The fourteen techniques

| Skill | Techniques |
| --- | --- |
| Focus | 5-second rule, Pomodoro, Deep Work, Information diet |
| Planning | Eisenhower matrix, 80/20 principle, Two-list strategy |
| Learning | Feynman technique, Spaced repetition |
| Habits | 2-minute rule, Habit stacking, 1% improvement |
| Reflection | Daily reflection, Premortem |

## What it deliberately is not

- **Not a task manager.** No entity in the database represents a task outside an exercise.
- **Not gamified.** No streak, no XP, no points, no leaderboard, no percentage complete.
- **Not online.** No account, no backend, no sync, no subscription, no AI requirement. The app declares no internet permission.
- **Not a dashboard.** Progress is five bars, a strip of days and one sentence.

Missing a day never resets anything. Program days advance when you train, not by the calendar.

## Repository layout

| Path | What it is |
| --- | --- |
| [`design/`](design/) | **A runnable Compose prototype of the whole app** — 24 screens, theme, components, icons, navigation, four languages. The source of truth for all UI and interaction. |
| [`docs/`](docs/README.md) | The specification: 65 documents, 19 ADRs and 40 issues. |
| `app/` | The production app. Currently the untouched Android Studio template; issue 001 replaces it. |
| [`AGENTS.md`](AGENTS.md) | Working rules for anyone — human or agent — implementing this. |
| [`CLAUDE_START_HERE.md`](CLAUDE_START_HERE.md) | Entry point for implementation. |

## Where to start

1. **Run the prototype.** It explains the product faster than any document.
2. [`docs/00-source-of-truth.md`](docs/00-source-of-truth.md) — source priority and every locked decision. Read before writing code.
3. [`docs/ux/05-prototype-reference.md`](docs/ux/05-prototype-reference.md) — screen-to-file map and the porting checklist.
4. [`docs/delivery/00-implementation-order.md`](docs/delivery/00-implementation-order.md) — 40 issues in 7 phases.

## Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17 or newer
- Android SDK; both modules download their own Gradle distribution on first sync
- A device or emulator running Android 8.0 (API 26) or newer

## Building

The two projects are **independent** — separate Gradle builds, separate application ids, different toolchain versions. That is intentional.

### The prototype

```bash
cd design
./gradlew installDebug
```

Or open the `design/` folder as its own Android Studio project.

Two things make exploring it fast: **"Explore with demo data"** on the Welcome screen jumps to day 9 with realistic history, and the **language pill** switches between the four languages at runtime.

The prototype was authored without an Android SDK available, so its first Gradle sync may need a few trivial import fixes — Android Studio will point at them. See [`design/README.md`](design/README.md) (written in Russian) for its own notes.

### The production app

```bash
./gradlew build
./gradlew installDebug
```

This currently builds the Android Studio template, not Itera.

### Side by side

Both install at once — `com.itera.app` and `com.wivernz.itera` are different application ids. That is the intended setup: every UI issue requires comparing the implementation against the prototype in light and dark before it is considered done.

## Conventions

- **Line endings** are normalised by Git via [`.gitattributes`](.gitattributes): LF in the repository and in the working tree, CRLF only for `.bat` and `.cmd`. After pulling a change to that file, run `git add --renormalize .`.
- **`design/` is a reference, not a dependency.** Production code never imports from it, and it is not part of the production Gradle build.
- **If you change anything visual, change `design/` to match in the same change.** A prototype that has drifted from the app is worse than none.

## Licence

Not yet chosen. Note that the four bundled typefaces (Bricolage Grotesque, Instrument Sans, Inter, Inter Tight) are SIL Open Font Licence and carry their own attribution requirements — see [ADR-0019](docs/architecture/adr/0019-typography-and-script-coverage.md).
