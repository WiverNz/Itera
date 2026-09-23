# 040 - Release readiness

**Phase** 7 - Hardening | **Depends on** 039 | **Blocks** none

## Goal

Make the app releasable: minification that does not break it, signing, performance verification, and a completed acceptance and QA pass.

## User value

The product can actually reach someone.

## Scope

- **Release build type**: `isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false`.
- **R8 keep rules** for Room entities, `kotlinx.serialization` serializers, Hilt-generated classes, `@HiltWorker` workers, every `@Serializable` route type, and the AppCompat locale service.
- **`resConfigs`** is either unset or lists all four languages - restricting it would silently drop translations.
- **Signing**: a release keystore, backed up outside the repository, with `signingConfigs.release` reading from `local.properties` or environment variables. Document the setup; commit nothing secret.
- **Manifest audit**: permissions are exactly `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`; only `MainActivity` is exported; the special-use subtype property is declared; no `INTERNET`.
- **Performance**: measure cold and warm start, Progress first frame, and scroll jank on Library and History. Generate a baseline profile, or record the decision not to.
- **Size**: verify the APK/AAB is under 12 MB; investigate anything unexpected in the size report.
- **Verification on the release build**: the full checklist in `docs/delivery/03-release-checklist.md` section 5, because R8 is a real source of failure that debug builds never show.
- **Languages**: verify a release build in all four languages, including persistence across a force-stop and the Android 13+ system language setting.
- **Acceptance**: execute `docs/prd/09-mvp-acceptance-criteria.md` in full and record the result.
- **QA**: execute `docs/testing/02-manual-qa-checklist.md` on two devices and record the result.
- **Versioning**: set `versionCode` and `versionName`; surface the version in the Privacy screen footer.
- **Archival**: tag the commit, archive `mapping.txt` and the exported schema.
- Append the completed checklists to this file.

## Non-goals

- Publishing to a public track.
- Automating release builds in CI (`docs/testing/03-ci.md` section 6 - not until a real release channel exists).
- Store listing copy beyond what the release checklist names.

## Implementation notes

- **Build a minified release build early**, during the work on this issue and ideally before it. The classic failures are a `kotlinx.serialization` serializer stripped from a result payload, a Room entity's constructor removed, or a `@Serializable` route class losing its fields - all of which look like "the app opens then crashes on one screen".
- Test an **upgrade** install, not just a fresh one. A migration that works on a fresh database and fails on a real one is the worst release bug available here.
- The performance numbers are budgets from `docs/prd/04-nonfunctional-requirements.md`, not aspirations. If cold start exceeds 1.5 s, generate a baseline profile before accepting it.
- A baseline profile requires a benchmark module or the Gradle managed-device workflow. If that is judged too much for the first internal release, record the decision and the measured start time rather than silently skipping it.
- The release keystore must be backed up somewhere that survives this machine. Losing it means never updating this app under the same identity.
- The Data safety declaration is "no data collected, no data shared, no data transmitted off device" - which is true and easy to defend precisely because there is no `INTERNET` permission.

## Affected layers

`build.gradle.kts`, `proguard-rules.pro`, manifest, `feature/you` (version footer), release documentation.

## Acceptance criteria

- [ ] A minified, shrunk, signed release build installs and runs.
- [ ] Every screen opens in the release build with no `ClassNotFoundException` or serialization failure.
- [ ] A focus session, a notification deep link, a journal export and both resets all work in the release build.
- [ ] An upgrade install over the previous version preserves data with no migration crash.
- [ ] The manifest declares exactly the four permissions listed, and no `INTERNET`.
- [ ] Only `MainActivity` is exported; the `FileProvider` is not.
- [ ] Cold start is under 1.5 s and warm start under 500 ms on the reference device, or a baseline profile is included.
- [ ] Progress renders within 300 ms with 1 500 seeded activities.
- [ ] No jank frames over 16 ms scrolling Library and History.
- [ ] The artifact is under 12 MB, with four string catalogues and the bundled fonts included.
- [ ] A release build runs correctly in English, Russian, German and Spanish, and the choice persists.
- [ ] R8 has not stripped `AppLocalesMetadataHolderService` or the locale config.
- [ ] `versionCode` and `versionName` are set and the version is visible in the app.
- [ ] `docs/prd/09-mvp-acceptance-criteria.md` passes in full, signed and dated.
- [ ] `docs/testing/02-manual-qa-checklist.md` passes on two devices, signed and dated.
- [ ] `docs/delivery/03-release-checklist.md` is complete.
- [ ] The commit is tagged; `mapping.txt` and the exported schema are archived.
- [ ] The keystore is backed up outside the repository and its location is documented (not the keystore itself).

## Unit test expectations

None new - issue 039 owns the suite.

## UI test expectations

The existing suite must pass against a **minified** build at least once, to catch R8 stripping something the tests exercise.

## Manual verification

1. Build a signed release; install on a clean device; run the full daily loop.
2. Install the previous version, populate it with a week of data, upgrade in place, verify everything.
3. Open every screen in the release build.
4. Measure cold start ten times; record the median.
5. Complete both manual checklists on two devices, one at or near `minSdk 26`.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections, plus `docs/delivery/03-release-checklist.md` complete and appended below.

---

## Release record

*(To be filled in: version, build date, devices tested, acceptance result, QA result, measured performance, artifact size, any accepted deviations.)*
