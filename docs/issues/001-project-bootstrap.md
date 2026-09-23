# 001 - Project bootstrap and dependency baseline

**Phase** 1 - Foundation | **Depends on** none | **Blocks** 002, 003

## Goal

Take the existing Android Studio scaffold to a state where every later issue can add code without touching build configuration or resolving a dependency conflict.

## User value

None directly. This is the floor everything else stands on; getting versions wrong here costs every later issue.

## Scope

- Extend `gradle/libs.versions.toml` with the dependencies in `docs/architecture/06-dependency-catalog.md` section 1-3, resolving to mutually compatible versions and pinning them exactly.
- Apply the KSP, Hilt, Room and kotlinx-serialization Gradle plugins.
- Configure the Room plugin with `schemaDirectory("$projectDir/schemas")` and commit the directory with a `.gitkeep`.
- Configure Kover with the `domain` 85 % line rule (rule defined, gate wired; it will pass trivially until 005 lands).
- Add Spotless or ktlint with a single formatting configuration.
- Add `lint.xml` raising `HardcodedText`, `MissingTranslation`, `ContentDescription` and `TouchTargetSizeCheck` to `error`. Supply the missing Compose text/touch checks as build-only lint tooling (ADR-0020); `ComposeHardcodedText` is also an error.
- Create the empty package tree from `docs/architecture/01-package-structure.md` with a `.gitkeep` per leaf directory.
- Add **`androidx.appcompat`**; make `MainActivity` an `AppCompatActivity`; set the app theme parent to `Theme.AppCompat.DayNight.NoActionBar` (ADR-0017, D-13).
- Add `res/xml/locales_config.xml` listing `en`, `ru`, `de`, `es`, referenced from the manifest as `android:localeConfig`, and declare `AppLocalesMetadataHolderService` with `autoStoreLocales = true`. Copy all four from `design/app/src/main/`.
- Create `res/values-ru/`, `values-de/` and `values-es/` with `app_name` only, so the locale structure exists from the first commit. Every later issue adds its own strings to all four (D-15).
- Add the Roborazzi Gradle plugin and its test dependency, configured but with no goldens yet (`docs/testing/04-visual-regression.md`).
- Replace the template `MainActivity` content with a themed placeholder that renders a single centred string.
- Add the **four** variable fonts to `res/font/`: Bricolage Grotesque, Instrument Sans, Inter Tight, Inter (ADR-0019). Issue 003 wires the selection rule; this issue only places the files and confirms they load.
- Add `OfflineTest`, a unit test asserting the merged manifest declares no `INTERNET` permission.
- Add the basic CI workflow from `docs/testing/03-ci.md` section 1 (unit tests, lint, assemble).
- Write `docs/../README` build instructions: how to build, run, test, and lint.

## Non-goals

- Any feature code, any Hilt module, any entity, any screen.
- Porting the string catalogues (issue 041) or any golden image (issue 039).
- Instrumented CI (issue 039).
- Release signing or minification (issue 040).
- `material-icons-extended` - the decision belongs to issue 003.

## Implementation notes

- AGP 9 has built-in Kotlin support; do **not** add an `org.jetbrains.kotlin.android` plugin alias. Only the Compose compiler plugin is applied explicitly, as it already is.
- Use a supported KSP2 release compatible with AGP 9.4.1 built-in Kotlin. KSP versions are independent of the Kotlin version; do not require a Kotlin version prefix. Keep built-in Kotlin and the new Android DSL enabled. `android.disallowKotlinSourceSets=false` is a last-resort documented exception only if no supported KSP works.
- `gradle.properties` already enables the configuration cache. Hilt and KSP are compatible with it; if a plugin breaks it, record the incompatibility in the issue rather than silently disabling the cache.
- Keep `minSdk 26`. `java.time` is available without desugaring, so do not add `coreLibraryDesugaring`.
- Do not add `kotlinx-datetime`; `java.time` is the project's time library.
- Fonts: download the variable `.ttf` for each family, place in `res/font/`, and reference them in issue 003. Do not use `ui-text-google-fonts` - it fetches over the network.

## Affected layers

Build configuration, resources, a placeholder `MainActivity`.

## Acceptance criteria

- [x] `./gradlew clean build` succeeds from a fresh checkout with no manual steps beyond a local SDK.
- [x] `./gradlew testDebugUnitTest`, `lintDebug`, `spotlessCheck` (or `ktlintCheck`) and `koverVerify` all succeed.
- [x] The debug app installs and launches to the placeholder screen.
- [x] The package tree matches `docs/architecture/01-package-structure.md` exactly.
- [x] Every dependency in the catalog document is present and pinned to an exact version; no dynamic versions or snapshots.
- [x] `app/schemas/` exists and is tracked.
- [x] `lint.xml` raises the four listed checks to error, and the build fails if a hard-coded string is introduced (verify by adding one temporarily).
- [x] `OfflineTest` passes.
- [x] `MainActivity` is an `AppCompatActivity` and the app launches.
- [x] `locales_config.xml` lists the four locales and the manifest references it.
- [x] `AppLocalesMetadataHolderService` is declared with `autoStoreLocales = true`.
- [x] `AppCompatDelegate.setApplicationLocales(forLanguageTags("ru"))` changes `app_name` and survives a force-stop (smoke check; full coverage in issue 041).
- [x] `./gradlew verifyRoborazziDebug` runs and passes with an empty golden set.
- [x] All four fonts are present in `res/font/` and load in a preview.
- [ ] CI runs on push and fails on a broken test. Workflow configured; equivalent local commands and deliberate broken-test failure verified. Hosted execution awaits a push.
- [x] The README documents build, run, test and lint commands.

## Unit test expectations

- `OfflineTest` - the merged manifest contains no `android.permission.INTERNET`.

## UI test expectations

None.

## Manual verification

1. Delete `.gradle/` and `build/`, run `./gradlew clean build`. It succeeds.
2. Install and launch the debug build; the placeholder renders with the correct font.
3. Add `Text("hello")` with a literal string; `lintDebug` fails. Remove it.
4. Open a preview in Android Studio; it renders.

## Definition of done

`docs/delivery/02-definition-of-done.md`, sections 1, 2, 10, 11.

## Verification (2026-09-23)

Implementation and local checks pass. See [the verification report](../testing/issue-001-verification.md) for commands, resolved versions, manual evidence and deviations. The user confirmed Android Studio sync and all four font previews. No work on issue 002 was started. Hosted push CI remains a delivery step; this working change is not represented as merged or fully accepted.
