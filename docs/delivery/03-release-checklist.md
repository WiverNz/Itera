# Release checklist

For an internal distribution build. Executed as part of milestone 011 (detailed issue 040) and before every subsequent release.

## 1. Pre-flight

- [ ] Every MVP milestone (001-012, with 012 before 009) meets its acceptance criteria and is closed by the user; historical detailed numbers are mapped in `docs/issues/README.md`.
- [ ] `docs/prd/09-mvp-acceptance-criteria.md` passes in full, signed and dated.
- [ ] `docs/testing/02-manual-qa-checklist.md` passes on two devices, signed and dated.
- [ ] No open issue is labelled blocking.
- [ ] `docs/00-source-of-truth.md` reflects the shipped behaviour.

## 2. Versioning

Scheme: `versionName = MAJOR.MINOR.PATCH`, `versionCode` a monotonically increasing integer.

| Change | Bump |
| --- | --- |
| Any user-visible feature | MINOR |
| Fixes only | PATCH |
| A forbidden content change, a schema rewrite, or a change to a frozen id | MAJOR |

- [ ] `versionCode` incremented and never reused.
- [ ] `versionName` set and matches the release notes.
- [ ] The version is visible somewhere in the app (the Privacy screen footer).

## 3. Build configuration

- [ ] `release` build type has `isMinifyEnabled = true` and `isShrinkResources = true`.
- [ ] ProGuard/R8 keep rules exist for: Room entities, `kotlinx.serialization` serializers, Hilt-generated classes, `@HiltWorker` workers, and every `@Serializable` route type.
- [ ] `isDebuggable = false`; no debug-only code path reachable.
- [ ] The merged release manifest declares **no `INTERNET` permission** (`OfflineTest` asserts this, but check the merged output too).
- [ ] After milestone 012, declared permissions are exactly: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `RECORD_AUDIO`. Microphone is requested only on voice use, never onboarding; no microphone foreground service. ADR-0022 is the permission decision.
- [ ] `FOREGROUND_SERVICE_SPECIAL_USE` carries its `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` declaration.
- [ ] Only `MainActivity` is exported.
- [ ] `allowBackup` is true with `data_extraction_rules.xml` excluding the database and DataStore from cloud backup and including them in device transfer.

## 4. Signing

- [ ] A release keystore exists, is backed up outside the repository, and is **not** committed.
- [ ] `signingConfigs.release` reads credentials from `local.properties` or environment variables, never from a committed file.
- [ ] `local.properties` is in `.gitignore` (verify - it currently is not tracked but confirm).
- [ ] The signed artifact installs over a previous release without a data wipe.

## 5. Verification on the release build

The release build, not a debug build, because R8 is a real source of failure.

- [ ] Fresh install: onboarding completes and Day 1 runs end to end.
- [ ] Upgrade install over the previous version: data survives, no migration crash.
- [ ] Every screen opens without a `ClassNotFoundException` or a serialization failure (the most common R8 symptoms).
- [ ] A focus session runs, backgrounds and restores.
- [ ] A notification fires and deep-links correctly.
- [ ] Journal export produces a valid file.
- [ ] Both reset tiers work.
- [ ] Voice dictation/commands pass in EN/RU/DE/ES on a capable device; absent service/model, API 26-30 and denied/revoked permission keep all manual paths usable. No generic platform fallback, background listening or transcript logging. Privacy copy describes on-device use and normal local draft storage accurately.
- [ ] Cold start under 1.5 s on the reference device.
- [ ] APK/AAB under 12 MB.

## 6. Performance

- [ ] A baseline profile is generated and included, or its absence is a recorded, accepted decision.
- [ ] No jank frames over 16 ms while scrolling Library and History on the reference device.
- [ ] Progress renders within 300 ms with a seeded 1 500-activity database.

## 7. Content

- [ ] All 14 techniques have all eight strings, reviewed against `docs/prd/08-content-style-guide.md`.
- [ ] `CatalogValidationTest` and `CatalogCompatibilityTest` pass against the shipped assets.
- [ ] The string "Productivity Trainer" appears nowhere user-facing.
- [ ] No placeholder or lorem text anywhere.
- [ ] The Privacy screen is accurate about what is stored and that nothing is transmitted.

## 8. Store metadata (when applicable)

- [ ] App name, short and full descriptions written, matching the product's voice.
- [ ] Screenshots from the release build, light and dark, at least one per tab.
- [ ] Adaptive icon renders correctly on a launcher with a circular mask.
- [ ] Data safety form: no data collected, no data shared, no data transmitted off device.
- [ ] Target audience and content rating completed.

## 9. Rollback plan

- [ ] The previous release artifact is archived and installable.
- [ ] The database schema version of this release is recorded, so a rollback's downgrade behaviour is known in advance (release builds refuse to downgrade - `docs/data/05-migrations-and-content-versioning.md` section 4).
- [ ] If this release includes a schema migration, note that rolling back requires an uninstall, and say so in the release notes.

## 10. Post-release

- [ ] Tag the commit `v{versionName}`.
- [ ] Archive the mapping file (`mapping.txt`) alongside the artifact - without it, a stack trace is unreadable.
- [ ] Archive the exported Room schema for this version.
- [ ] Record the release in a changelog with the issues included.
- [ ] Watch Play Console vitals (or tester reports) for 72 hours before promoting further.
