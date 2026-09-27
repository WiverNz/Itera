# Issue 001 verification - 2026-09-23

## Status

Issue 001 implementation and local acceptance checks pass. Issue 002 has not started. One acceptance criterion remains externally unverified: a hosted GitHub Actions run triggered by push. The workflow and its failure gates are implemented and tested locally; no push, commit, merge, or hosted CI result is claimed. Complete that delivery check before marking issue 001 fully accepted and starting issue 002.

The required empty package directories and schema placeholder are staged for tracking; other implementation changes remain in the working tree.

## Dependency compatibility and reproducibility

| Tool | Resolved version |
| --- | --- |
| Gradle | 9.7.1, wrapper distribution SHA-256 pinned |
| AGP | 9.4.1 |
| Kotlin Gradle plugin / Compose compiler / serialization compiler plugin | 2.2.10 |
| KSP2 | 2.3.12 |
| Hilt plugin and processor | 2.59.2 |
| Room plugin and processor | 2.8.4 |
| Roborazzi plugin and libraries | 1.61.0 |
| Kover | 0.9.9 |
| Spotless / ktlint | 7.2.1 / 1.7.1 |
| Android lint API and test tooling | 32.4.1 |
| Compose BOM | 2026.02.01 |
| Compose UI / Material 3 | 1.10.4 / 1.4.0 |
| Production daemon JDK / SDK | 25 / compile and target 37, minimum 26 |

KSP 2.2.10-2.0.2 failed on AGP's generated Kotlin source-set registration. KSP 2.3.12 works with built-in Kotlin, the new Android DSL, and configuration caching. No `android.disallowKotlinSourceSets=false`, legacy Kotlin Android plugin, or other compatibility opt-out is used.

A disposable source copy compiled an actual `@HiltAndroidApp` application and Room entity/DAO/database. Generated `Hilt_ProbeApplication`, `DaggerProbeApplication_HiltComponents_SingletonC`, and `ProbeDatabase_Impl` were present and compiled successfully. Those fixtures exist only in the temporary checkout, not production.

Kotlin application runtime resolves to 2.2.20 through Compose; common runtime is explicitly pinned to that version to make lock generation and ordinary task resolution agree. This does not change compiler 2.2.10. Build-tool classpaths separately contain Kotlin runtime 2.4.0 (Gradle) and 2.3.20 (KSP). `app/gradle.lockfile` records configuration-specific transitive versions; `gradle/libs.versions.toml` records requested direct versions. No dynamic versions or snapshots are used.

### Direct library versions

Multiple resolved versions mean different compile/runtime/test configurations, not multiple versions within one classpath. In particular lifecycle compiles against 2.9.1 and runs against 2.9.4; activity-compose compiles against 1.8.0 and runs against 1.8.2. Test core is 1.6.1 for JVM tests and 1.5.0 for instrumentation.

| Artifact | Requested | Resolved across configurations |
| --- | --- | --- |
| `com.android.tools.lint:lint-api` | 32.4.1 | 32.4.1 |
| `com.android.tools.lint:lint-tests` | 32.4.1 | 32.4.1 (buildSrc only) |
| `org.jetbrains.kotlin:kotlin-stdlib-common` | 2.2.20 | 2.2.20 |
| `androidx.core:core-ktx` | 1.10.1 | 1.10.1, 1.16.0 |
| `junit:junit` | 4.13.2 | 4.13.2 |
| `androidx.test.ext:junit` | 1.1.5 | 1.1.5 |
| `androidx.test.espresso:espresso-core` | 3.5.1 | 3.5.1 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.6.1 | 2.9.1, 2.9.4 |
| `androidx.activity:activity-compose` | 1.8.0 | 1.8.0, 1.8.2 |
| `androidx.compose:compose-bom` | 2026.02.01 | 2026.02.01 |
| `androidx.compose.ui:ui` | Compose BOM | 1.10.4 |
| `androidx.compose.ui:ui-graphics` | Compose BOM | 1.10.4 |
| `androidx.compose.ui:ui-tooling` | Compose BOM | 1.10.4 |
| `androidx.compose.ui:ui-tooling-preview` | Compose BOM | 1.10.4 |
| `androidx.compose.ui:ui-test-manifest` | Compose BOM | 1.10.4 |
| `androidx.compose.ui:ui-test-junit4` | Compose BOM | 1.10.4 |
| `androidx.compose.material3:material3` | Compose BOM | 1.4.0 |
| `com.google.dagger:hilt-android` | 2.59.2 | 2.59.2 |
| `com.google.dagger:hilt-compiler` | 2.59.2 | 2.59.2 |
| `com.google.dagger:hilt-android-testing` | 2.59.2 | 2.59.2 |
| `androidx.hilt:hilt-navigation-compose` | 1.3.0 | 1.3.0 |
| `androidx.hilt:hilt-work` | 1.3.0 | 1.3.0 |
| `androidx.hilt:hilt-compiler` | 1.3.0 | 1.3.0 |
| `androidx.room:room-runtime` | 2.8.4 | 2.8.4 |
| `androidx.room:room-ktx` | 2.8.4 | 2.8.4 |
| `androidx.room:room-compiler` | 2.8.4 | 2.8.4 |
| `androidx.room:room-testing` | 2.8.4 | 2.8.4 |
| `androidx.datastore:datastore-preferences` | 1.1.7 | 1.1.7 |
| `androidx.datastore:datastore` | 1.1.7 | 1.1.7 |
| `androidx.work:work-runtime-ktx` | 2.10.1 | 2.10.1 |
| `androidx.work:work-testing` | 2.10.1 | 2.10.1 |
| `androidx.navigation:navigation-compose` | 2.9.0 | 2.9.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.9.0 | 1.8.1, 1.9.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 | 1.10.2 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.10.2 | 1.10.2 |
| `org.jetbrains.kotlinx:kotlinx-collections-immutable` | 0.4.0 | 0.4.0 |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.9.1 | 2.9.1, 2.9.4 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.9.1 | 2.9.1, 2.9.4 |
| `androidx.core:core-splashscreen` | 1.0.1 | 1.0.1 |
| `androidx.appcompat:appcompat` | 1.7.0 | 1.7.0 |
| `app.cash.turbine:turbine` | 1.2.0 | 1.2.0 |
| `org.robolectric:robolectric` | 4.16 | 4.16 |
| `io.github.takahirom.roborazzi:roborazzi-compose` | 1.61.0 | 1.61.0 |
| `androidx.test:core-ktx` | 1.6.1 | 1.6.1 |

## Commands and outcomes

Commands below use `gradlew.bat` on Windows; CI uses `bash ./gradlew` with the same Gradle tasks.

| Command/check | Result |
| --- | --- |
| `clean build testDebugUnitTest lintDebug spotlessCheck koverVerify verifyRoborazziDebug --console=plain` | PASS; clean production build, 124 tasks, 121 executed |
| `clean build --console=plain` in a fresh source copy | PASS; 123/123 tasks executed; no project caches or build outputs copied |
| `--no-daemon spotlessCheck lintDebug testDebugUnitTest verifyRoborazziDebug koverVerify assembleDebug connectedDebugAndroidTest --console=plain` | PASS; final restored-source checks, 118 tasks, 44 executed |
| `-p buildSrc test` | PASS; 3 custom lint tests, no failures or skips |
| `testDebugUnitTest` | PASS; 10 tests: OfflineTest, font resource loading, eight light/dark font preview cases |
| `lintDebug` | PASS; empty issue report, zero errors and warnings |
| `spotlessCheck` | PASS |
| `koverVerify` | PASS; domain-only 85% line gate wired; no domain code yet |
| `verifyRoborazziDebug` | PASS; intentionally empty golden set |
| `installDebug assembleDebugAndroidTest` | PASS; production and instrumentation APKs installed |
| `connectedDebugAndroidTest` | PASS; one locale round-trip test, no failures or skips, API 36.1 emulator |
| Three `am instrument` phases: select / assert / restore, with `am force-stop` between select and assert | PASS; Russian app_name persists through process death; restored system locale |
| Temporary real Compose `Text("hello")`, then `lintDebug` | Expected failure: ComposeHardcodedText; source restored |
| Temporarily invert OfflineTest, then `--no-daemon testDebugUnitTest` | Expected failure: assertion error, nonzero exit; source restored and all ten tests subsequently passed |
| `assembleDebug` with temporary Hilt/Room fixtures outside repository | PASS; both processors generate and compile source |
| Prototype `assembleDebug` and `installDebug` | PASS; independent build and launch |
| Dependency report / buildEnvironment / lock generation | PASS; exact resolutions recorded in catalog and lock files |
| Package-tree audit | PASS; exactly 46 specified empty leaves, only MainActivity.kt in production Kotlin source |
| Android Studio sync and four font previews | PASS; explicitly confirmed by user: “Synced; all four previews render” |

Fresh-source verification used a copy of tracked and untracked deliverable source files rather than a Git clone, because this implementation is uncommitted. A local SDK properties file was the only machine setup added. An initial verification attempt failed because that temporary file contained an unescaped Windows drive colon; corrected to `sdk.dir=G\:/Android/SDK`, then repeated from a new empty directory. Global downloaded dependency caches were available.

Local raw logs and screenshots are in ignored `captures/`; machine-readable results are in `app/build/test-results`, `app/build/outputs/androidTest-results`, and `buildSrc/build/test-results`. JDK native-access/Unsafe and native-library stripping notices are toolchain output, not lint findings.

## Manual checks and scope

Production launched on Medium_Phone_API_36.1, 1080×2400, density 420. The centered placeholder was inspected in English/light and Russian/dark. Locale switching, process kill, relaunch, and theme checks produced no observed crash. The prototype was run and its demo-data entry explored. Its matching bootstrap preview and font resources were added in this change. No product screen, application class, Hilt module, entity, navigation graph, business rule, or issue 002 code was introduced.

The merged-manifest OfflineTest confirms no INTERNET permission. The placeholder does not request notifications or depend on notification permission. All four locale resource directories contain app_name; manifest locale configuration and AppCompat locale metadata match the specification. Schema and package `.gitkeep` files are included. ArchitectureTest and state-management classes remain later-issue work; there is no domain or feature implementation to exercise here.

## Documented changes and limitations

- User-authorized corrections: independent KSP version selection and `res/font/` in both projects. Typography selection and glyph coverage remain issue 003.
- Hilt 2.59.2, Room 2.8.4, Roborazzi 1.61.0 and associated tooling versions replace illustrative catalog versions; the dependency catalog and source-of-truth document record them. AGP and Kotlin compiler versions are unchanged.
- ADR-0020 supplies build-only Compose literal-text and undersized-control lint checks. Built-in HardcodedText alone does not enforce Compose text, and TouchTargetSizeCheck was not a built-in issue. Full semantic touch-region tests remain later work.
- Only dependency-update advertisement lint checks are disabled, because this task deliberately pins reviewed versions. Correctness, localization and accessibility lint remain enabled.
- CI uses JDK 25 to match the existing daemon criteria. The production architecture remains one Android module; buildSrc is build tooling.
- Prototype `screenInsets()` needed a missing `@Composable` annotation to compile. Font loading now uses bundled resources. These prerequisite/parity fixes are documented and do not implement production features.
- Hosted CI execution remains pending; locally forcing a broken test proves the task's failure behavior, not GitHub trigger execution.

## Changed files

The following manifest includes added, modified, staged, and deleted paths for this issue. Generated build outputs, screenshots, machine-local settings, and disposable processor fixtures are excluded.

```text
.editorconfig
.github/workflows/android.yml
.gitignore
README.md
app/build.gradle.kts
app/gradle.lockfile
app/lint.xml
app/schemas/.gitkeep
app/src/androidTest/java/com/wivernz/itera/ExampleInstrumentedTest.kt
app/src/androidTest/java/com/wivernz/itera/LocaleSmokeTest.kt
app/src/main/AndroidManifest.xml
app/src/main/java/com/wivernz/itera/MainActivity.kt
app/src/main/java/com/wivernz/itera/analytics/.gitkeep
app/src/main/java/com/wivernz/itera/core/common/dispatchers/.gitkeep
app/src/main/java/com/wivernz/itera/core/common/result/.gitkeep
app/src/main/java/com/wivernz/itera/core/common/time/.gitkeep
app/src/main/java/com/wivernz/itera/core/designsystem/component/.gitkeep
app/src/main/java/com/wivernz/itera/core/designsystem/icon/.gitkeep
app/src/main/java/com/wivernz/itera/core/designsystem/preview/.gitkeep
app/src/main/java/com/wivernz/itera/core/designsystem/theme/.gitkeep
app/src/main/java/com/wivernz/itera/core/navigation/.gitkeep
app/src/main/java/com/wivernz/itera/core/notifications/receiver/.gitkeep
app/src/main/java/com/wivernz/itera/core/notifications/work/.gitkeep
app/src/main/java/com/wivernz/itera/data/catalog/.gitkeep
app/src/main/java/com/wivernz/itera/data/copy/.gitkeep
app/src/main/java/com/wivernz/itera/data/database/dao/.gitkeep
app/src/main/java/com/wivernz/itera/data/database/entity/.gitkeep
app/src/main/java/com/wivernz/itera/data/database/migration/.gitkeep
app/src/main/java/com/wivernz/itera/data/database/relation/.gitkeep
app/src/main/java/com/wivernz/itera/data/export/.gitkeep
app/src/main/java/com/wivernz/itera/data/focus/.gitkeep
app/src/main/java/com/wivernz/itera/data/mapper/.gitkeep
app/src/main/java/com/wivernz/itera/data/preferences/.gitkeep
app/src/main/java/com/wivernz/itera/data/repository/.gitkeep
app/src/main/java/com/wivernz/itera/di/.gitkeep
app/src/main/java/com/wivernz/itera/domain/coach/.gitkeep
app/src/main/java/com/wivernz/itera/domain/model/.gitkeep
app/src/main/java/com/wivernz/itera/domain/progress/.gitkeep
app/src/main/java/com/wivernz/itera/domain/repository/.gitkeep
app/src/main/java/com/wivernz/itera/domain/review/.gitkeep
app/src/main/java/com/wivernz/itera/domain/training/.gitkeep
app/src/main/java/com/wivernz/itera/domain/unlock/.gitkeep
app/src/main/java/com/wivernz/itera/feature/daycomplete/.gitkeep
app/src/main/java/com/wivernz/itera/feature/exercise/combination/.gitkeep
app/src/main/java/com/wivernz/itera/feature/exercise/eisenhower/.gitkeep
app/src/main/java/com/wivernz/itera/feature/exercise/feynman/.gitkeep
app/src/main/java/com/wivernz/itera/feature/exercise/habitstack/.gitkeep
app/src/main/java/com/wivernz/itera/feature/exercise/premortem/.gitkeep
app/src/main/java/com/wivernz/itera/feature/exercise/review/.gitkeep
app/src/main/java/com/wivernz/itera/feature/exercise/runner/.gitkeep
app/src/main/java/com/wivernz/itera/feature/exercise/template/.gitkeep
app/src/main/java/com/wivernz/itera/feature/focus/.gitkeep
app/src/main/java/com/wivernz/itera/feature/onboarding/.gitkeep
app/src/main/java/com/wivernz/itera/feature/progress/.gitkeep
app/src/main/java/com/wivernz/itera/feature/reflection/.gitkeep
app/src/main/java/com/wivernz/itera/feature/today/.gitkeep
app/src/main/java/com/wivernz/itera/feature/train/.gitkeep
app/src/main/java/com/wivernz/itera/feature/you/.gitkeep
app/src/main/java/com/wivernz/itera/ui/theme/Color.kt
app/src/main/java/com/wivernz/itera/ui/theme/Theme.kt
app/src/main/java/com/wivernz/itera/ui/theme/Type.kt
app/src/main/res/font/bricolage_grotesque.ttf
app/src/main/res/font/instrument_sans.ttf
app/src/main/res/font/inter.ttf
app/src/main/res/font/inter_tight.ttf
app/src/main/res/values-de/strings.xml
app/src/main/res/values-es/strings.xml
app/src/main/res/values-ru/strings.xml
app/src/main/res/values/colors.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/xml/data_extraction_rules.xml
app/src/main/res/xml/locales_config.xml
app/src/test/java/com/wivernz/itera/BootstrapPreviewTest.kt
app/src/test/java/com/wivernz/itera/ExampleUnitTest.kt
app/src/test/java/com/wivernz/itera/FontLoadingTest.kt
app/src/test/java/com/wivernz/itera/OfflineTest.kt
app/src/test/resources/robolectric.properties
app/src/test/screenshots/.gitkeep
build.gradle.kts
buildSrc/build.gradle.kts
buildSrc/settings.gradle.kts
buildSrc/src/main/java/com/wivernz/itera/lint/BootstrapDetector.java
buildSrc/src/main/java/com/wivernz/itera/lint/BootstrapIssueRegistry.java
buildSrc/src/test/java/com/wivernz/itera/lint/BootstrapDetectorTest.java
design/README.md
design/app/src/main/assets/fonts/README.txt
design/app/src/main/java/com/itera/app/ui/components/Components.kt
design/app/src/main/java/com/itera/app/ui/theme/BootstrapPreview.kt
design/app/src/main/java/com/itera/app/ui/theme/Type.kt
design/app/src/main/res/font/bricolage_grotesque.ttf
design/app/src/main/res/font/instrument_sans.ttf
design/app/src/main/res/font/inter.ttf
design/app/src/main/res/font/inter_tight.ttf
design/app/src/main/res/values-de/strings.xml
design/app/src/main/res/values-es/strings.xml
design/app/src/main/res/values-ru/strings.xml
design/app/src/main/res/values/strings.xml
docs/00-source-of-truth.md
docs/architecture/06-dependency-catalog.md
docs/architecture/adr/0019-typography-and-script-coverage.md
docs/architecture/adr/0020-bootstrap-lint.md
docs/i18n/00-localization.md
docs/issues/001-project-bootstrap.md
docs/issues/003-design-tokens-and-theme.md
docs/issues/041-localization-verification.md
docs/testing/03-ci.md
docs/testing/issue-001-verification.md
docs/ux/04-design-system.md
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties
licenses/fonts/README.md
licenses/fonts/bricolagegrotesque/OFL.txt
licenses/fonts/instrumentsans/OFL.txt
licenses/fonts/inter/OFL.txt
licenses/fonts/intertight/OFL.txt
settings-gradle.lockfile
settings.gradle.kts
```
