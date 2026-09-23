# Dependency catalog

The existing `gradle/libs.versions.toml` already pins AGP 9.4.1, Kotlin 2.2.10, the Compose BOM and the default test libraries. This document lists what issue 001 adds and why. **No dependency is added outside this list without an ADR.**

## 1. Versions to add

```toml
[versions]
ksp                  = "2.3.12"         # independent KSP2 version; supports built-in Kotlin
hilt                 = "2.59.2"
hiltNavigationCompose = "1.3.0"
room                 = "2.8.4"
datastore            = "1.1.7"
work                 = "2.10.1"
navigationCompose    = "2.9.0"
kotlinxSerialization = "1.9.0"
kotlinxCoroutines    = "1.10.2"
kotlinxImmutable     = "0.4.0"
lifecycle            = "2.9.1"
splashscreen         = "1.0.1"
appcompat            = "1.7.0"
turbine              = "1.2.0"
robolectric          = "4.16"
roborazzi            = "1.61.0"
androidxTestCore     = "1.6.1"
```

Exact patch versions are whatever is current at implementation time; KSP2 must support AGP 9.4.1 built-in Kotlin, and Room, Hilt and Navigation must be mutually compatible. Issue 001 resolves and pins them.

## 2. Libraries

| Dependency | Why | Baseline? |
| --- | --- | --- |
| `hilt-android`, `hilt-compiler` | DI | yes |
| `androidx.hilt:hilt-navigation-compose` | `hiltViewModel()` | yes |
| `androidx.hilt:hilt-work` + `hilt-compiler` | `@HiltWorker` | yes |
| `androidx.room:room-runtime`, `room-ktx`, `room-compiler` | structured training history | yes |
| `androidx.room:room-testing` | migration tests | yes |
| `androidx.datastore:datastore-preferences` | user preferences | yes |
| `androidx.datastore:datastore` (typed) | focus timer state | yes |
| `androidx.work:work-runtime-ktx` | reminders, daily plan | yes |
| `androidx.work:work-testing` | worker tests | yes |
| `androidx.navigation:navigation-compose` | navigation | yes |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | catalog, result payloads, type-safe routes | yes |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | coroutines | yes |
| `androidx.lifecycle:lifecycle-runtime-compose` | `collectAsStateWithLifecycle` | yes |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `viewModel()` | yes |
| `androidx.core:core-splashscreen` | startup | yes |
| `androidx.appcompat:appcompat` | **required** for `AppCompatActivity` + `AppCompatDelegate.setApplicationLocales`, which is how per-app language works below Android 13 (ADR-0017, D-13) | yes |
| `androidx.compose.material3:material3` | components | already present |
| ~~`material-icons-extended`~~ | **not used** - `IteraIcons.kt` covers every icon (D-12) | no |
| `org.jetbrains.kotlinx:kotlinx-collections-immutable` | stable list params for Compose | yes |

### Notes
- **kotlinx-datetime**: listed but **not adopted**. `minSdk 26` gives `java.time` natively. Use `java.time` throughout; drop this line.
- **Fonts**: **four** variable faces are bundled, all OFL - Bricolage Grotesque and Instrument Sans for Latin locales, Inter Tight and Inter for Cyrillic and for all user-authored text (ADR-0019). Roughly 300-400 KB each pre-compression. Both projects bundle fonts in `res/font/` and load them by resource ID; production **has no silent system-font fallback**: a missing font is a build failure, caught by `FontCoverageTest`, not a silent degradation. No `androidx.compose.ui:ui-text-google-fonts` - it fetches over the network and would break offline-first.
- **Icons**: settled. `IteraIcons.kt` from the prototype - 33 hand-authored `ImageVector`s. **No `material-icons-extended`** (D-12).

## 3. Test dependencies

| Dependency | Scope | Why |
| --- | --- | --- |
| `junit:junit` | test | already present |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | test | `runTest`, `TestDispatcher` |
| `app.cash.turbine:turbine` | test | asserting on `Flow` emissions |
| `androidx.room:room-testing` | androidTest | migration tests |
| `androidx.work:work-testing` | androidTest | worker tests |
| `androidx.test.ext:junit`, `espresso-core` | androidTest | already present |
| `androidx.compose.ui:ui-test-junit4`, `ui-test-manifest` | androidTest / debug | already present |
| `com.google.dagger:hilt-android-testing` + compiler | androidTest | `@HiltAndroidTest` |
| `org.robolectric:robolectric` | test | JVM-side Android tests (notification builders, `CopyResolver`, `AppLanguage`) and the golden-image host |
| `io.github.takahirom.roborazzi:roborazzi-compose` + the Gradle plugin | test | golden images (`docs/testing/04-visual-regression.md`) |

**No mocking framework.** Fakes are hand-written. Rationale in `docs/testing/00-strategy.md`.

## 4. Deliberately excluded

| Not used | Reason |
| --- | --- |
| Retrofit / OkHttp / Ktor | no network layer; the manifest declares no `INTERNET` permission |
| Firebase (any) | no backend, no analytics SDK, offline-first |
| Coil / Glide | no remote images; all imagery is vector |
| Accompanist | its remaining useful pieces are now in AndroidX |
| Moshi / Gson | `kotlinx.serialization` covers both catalog and payloads |
| Paging 3 | History and Library are bounded; `LazyColumn` with a `Flow<List<T>>` suffices until a user has thousands of entries. Revisit if History exceeds ~2 000 rows |
| Timber | one small `Logger` interface, injected, is enough and keeps release logging honest |
| Mockito / MockK | see testing strategy |
| Anything from `design/` | the prototype is a reference, never a dependency; production must not import it (ADR-0018) |
| Dynamic feature modules | single APK |

## 5. Build plugins

```toml
[plugins]
android-application  = { id = "com.android.application", version.ref = "agp" }        # present
kotlin-compose       = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }  # present
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp                  = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt                 = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room                 = { id = "androidx.room", version.ref = "room" }
```

AGP 9 has built-in Kotlin support, so no `org.jetbrains.kotlin.android` alias is required.

The Room Gradle plugin is used for `schemaDirectory("$projectDir/schemas")`, which keeps the exported schema out of `build/` and in version control.

## 6. Size budget

Release APK target: **under 12 MB** with R8 and resource shrinking. The four variable fonts are the largest single cost, roughly 1.2-1.6 MB together, and four string catalogues add a little more. If `material-icons-extended` is added it must be justified against this budget; it is the main reason the budget exists.

## 7. Bootstrap tooling (ADR-0020)

Kover 0.9.9, Spotless 7.2.1 / ktlint 1.7.1; lint-api and lint-tests 32.4.1 are buildSrc-only. The app explicitly pins Kotlin common runtime 2.2.20, already selected by Compose. Compiler and compiler plugins remain 2.2.10.
