<p align="center">
  <img src="preview.png" width="100%"
       alt="Itera: Practice. Reflect. Improve. Guided productivity training for Android.">
</p>

# Itera

A native Android app that teaches productivity techniques through daily practice, not through another list of things to do.

> **Status: milestones 001-002 complete; 003 implemented with final visual acceptance pending.**
> `app/` contains core architecture/storage, the design system and navigation shell with placeholder feature destinations. Milestone 004 is ready to start; 004 and later milestones have not been started. See the [milestone status and acceptance gaps](docs/issues/README.md#current-status-2026-09-24).

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
| [`docs/`](docs/README.md) | The specification: the product documents, ADRs and implementation issues. |
| `app/` | The production app: core architecture/storage, design system and navigation shell. Feature destinations remain placeholders. |
| [`AGENTS.md`](AGENTS.md) | Working rules for anyone — human or agent — implementing this. |
| [`CLAUDE_START_HERE.md`](CLAUDE_START_HERE.md) | Entry point for implementation. |

## Where to start

1. **Run the prototype.** It explains the product faster than any document.
2. [`docs/00-source-of-truth.md`](docs/00-source-of-truth.md) — source priority and every locked decision. Read before writing code.
3. [`docs/ux/05-prototype-reference.md`](docs/ux/05-prototype-reference.md) — screen-to-file map and the porting checklist.
4. [`docs/delivery/00-implementation-order.md`](docs/delivery/00-implementation-order.md) — 12 milestone issues; planned voice milestone 012 runs before 009's quality passes.

## Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 25 for production (the committed daemon JVM criteria select it); JDK 17+ for the independent prototype
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

The prototype builds and runs independently. See [`design/README.md`](design/README.md) (written in Russian) for its own notes.

### The production app

```bash
./gradlew build
./gradlew installDebug
```

This builds a single centered, localized app-name placeholder. The four fonts are bundled in `app/src/main/res/font/`; licenses and pinned source hashes are in [licenses/fonts](licenses/fonts/README.md).

Set `ANDROID_HOME` to your SDK, or add `sdk.dir=/path/to/sdk` in the ignored `local.properties`. Install platform `android-37.0` and build tools `36.0.0`. The wrapper downloads Gradle 9.7.1 and checks its SHA-256. The daemon JDK can be provisioned automatically. Dependencies need network access on the first build; the app itself has no INTERNET permission.

On Windows use `.\gradlew.bat` instead of `./gradlew`.

```bash
./gradlew clean build
./gradlew testDebugUnitTest lintDebug spotlessCheck koverVerify verifyRoborazziDebug
./gradlew -p buildSrc test
./gradlew spotlessApply                       # format production Kotlin/build scripts
./gradlew installDebug
adb shell am start -W -n com.wivernz.itera/.MainActivity
```

`build` runs formatting, lint, unit tests and the domain coverage gate. `verifyRoborazziDebug` is configured with an empty golden set; milestone 010 adds goldens. JVM tests render all four font previews in light/dark under API 34, fixed dimensions, en-US, UTC and disabled animations. Open `BootstrapPreview` in Android Studio for the IDE preview.

Dependency versions are pinned in `gradle/libs.versions.toml` and resolved artifacts in `app/gradle.lockfile`. Intentional changes require regenerating locks with `./gradlew :app:dependencies --write-locks`, then running the full task set. Built-in Kotlin and the configuration cache stay enabled. No compatibility opt-out is used.

The local instrumentation smoke test is deliberately separate from CI (instrumented CI remains milestone 010):

```bash
./gradlew installDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.wivernz.itera.LocaleSmokeTest -e phase select com.wivernz.itera.test/androidx.test.runner.AndroidJUnitRunner
adb shell am force-stop com.wivernz.itera
adb shell am instrument -w -e class com.wivernz.itera.LocaleSmokeTest -e phase assert com.wivernz.itera.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e class com.wivernz.itera.LocaleSmokeTest -e phase restore com.wivernz.itera.test/androidx.test.runner.AndroidJUnitRunner
```

Without a `phase` argument, `connectedDebugAndroidTest` performs a self-contained locale round trip. The `select` phase calls `AppCompatDelegate.setApplicationLocales` and checks the translated name; `assert` verifies it after process death; `restore` returns to the system language.

CI runs on branch pushes and pull requests, uploads a debug APK, and retains reports even when checks fail. Windows version/release scripts and signed tag builds are documented in [scripts/README.md](scripts/README.md). Pushed vX.Y.Z tags run the checks and create a draft GitHub Release with signed APK/AAB assets. Custom Compose lint checks are build-only tooling described in [ADR-0020](docs/architecture/adr/0020-bootstrap-lint.md).

### Side by side

Both install at once — `com.itera.app` and `com.wivernz.itera` are different application ids. That is the intended setup: every UI issue requires comparing the implementation against the prototype in light and dark before it is considered done.

## Conventions

- **Line endings** are normalised by Git via [`.gitattributes`](.gitattributes): LF in the repository and in the working tree, CRLF only for `.bat` and `.cmd`. After pulling a change to that file, run `git add --renormalize .`.
- **`design/` is a reference, not a dependency.** Production code never imports from it, and it is not part of the production Gradle build.
- **If you change anything visual, change `design/` to match in the same change.** A prototype that has drifted from the app is worse than none.

## Licence

Not yet chosen. Note that the four bundled typefaces (Bricolage Grotesque, Instrument Sans, Inter, Inter Tight) are SIL Open Font Licence and carry their own attribution requirements — see [ADR-0019](docs/architecture/adr/0019-typography-and-script-coverage.md).
