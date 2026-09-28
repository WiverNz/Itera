# ADR-0017: Per-app language via AppCompatDelegate, four languages in MVP

Status: accepted (2026-09-23)

## Context

English, Russian, German and Spanish are all MVP scope, with runtime switching from Settings and persistence across restarts. The app targets `minSdk 26`, so the platform per-app language API (Android 13+) covers only part of the install base.

The `design/` prototype already implements this and ships complete catalogues in all four languages.

## Decision

Use `AppCompatDelegate.setApplicationLocales` with `LocaleListCompat`, which is the platform API on Android 13+ and an AppCompat backport below it.

Required together:

- `MainActivity` extends `AppCompatActivity`;
- `androidx.appcompat` is a dependency;
- `res/xml/locales_config.xml` lists the four locales and is referenced by `android:localeConfig`;
- `AppLocalesMetadataHolderService` is declared with `autoStoreLocales = true`, which persists the choice on Android 12 and below;
- the app theme derives from `Theme.AppCompat.DayNight.NoActionBar`.

The language is **not** mirrored into DataStore.

## Alternatives considered

- **Store the tag in DataStore and apply it at startup** - duplicates state that `AppCompatDelegate` already owns, and drifts from the system per-app language setting that Android 13+ users can change outside the app. Rejected.
- **Wrap the `Context` and override the configuration manually** - the pre-AndroidX approach; fragile across process restarts and Compose recompositions, and loses the system settings integration. Rejected.
- **Ship English only and add languages later** - contradicts the requirement, and the translations already exist. Rejected.

## Consequences

- `AppCompatActivity` is mandatory, which pulls in `appcompat` and an AppCompat theme parent. Accepted; it is a small dependency and the only one that solves this below API 33.
- The language appears in Android 13+ system settings for free.
- Switching does not recreate the activity: `MainActivity` declares `android:configChanges="locale|layoutDirection"`, so the new configuration reaches Compose in place and the screen does not flash. Drafts still live in the database rather than in `remember`, because other configuration changes and process death do recreate.
- Adding a fifth language is four small edits (`docs/i18n/00-localization.md` section 10).

## Migration implications

None. A future move to a DataStore-backed override would have to explain how it stays consistent with the system setting.
