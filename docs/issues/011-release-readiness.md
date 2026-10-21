# 011 - Release readiness

**Status:** complete (2026-10-21) for the GitHub sideload 1.0 release. **Depends on** 010 | **Blocks** -

## Release decision (2026-10-21, product owner)

- **GitHub sideload release: approved.**
- **Google Play release: not yet approved.** Real Play Console Asset Delivery of the `voice_model_<lang>` packs (Download, progress, install, Remove) has not been verified; it needs an internal-testing upload and a pass on a device before a Play release.
- **Known verification gaps, not blockers for the GitHub 1.0 release:** API 26-30; the external speech-provider flow; Spanish Vosk on a real device; spoken TalkBack and Switch Access walkthroughs; a full two-device rerun of the manual QA checklist on the release build; long-history performance (Progress with 1 500 activities, History scroll). The acceptance items below that stay unticked are open for these reasons only.
- **Artifacts:** the per-ABI APKs are the recommended sideload downloads; the universal APK is a compatibility fallback. The AAB is for a later Play upload.
- **Version:** v1.0.18 is produced by `scripts/release.ps1 patch` from the committed 1.0.17; no manual edit of `version.properties`.

## Goal

Produce a signed, minified release build that passes the MVP acceptance criteria and the manual QA checklist.

## Included scope

Old 040 unchanged in intent: release build type (minify, shrink, not debuggable), R8 keep rules, `resConfigs` covering all four languages, signing from local/environment config (nothing secret committed), manifest and permission audit, performance and size checks, release-build verification in all four languages, versioning, archival of `mapping.txt` and the schema.

## Source documents

- Detailed scope and notes: `docs/history/issues-detailed/040-release-readiness.md`.
- `docs/delivery/03-release-checklist.md`; `docs/prd/09-mvp-acceptance-criteria.md`; `docs/testing/02-manual-qa-checklist.md`; `docs/prd/04-nonfunctional-requirements.md`.

## Key dependencies

- Needs 010 (green CI and goldens).
- Tagging the release is left to the user (AGENTS.md: agents do not create tags).

## Acceptance criteria

- [x] The minified release build installs, upgrades cleanly and runs the daily loop in all four languages. *Daily loop end to end in English (emulator, vivo) and the exercise in Russian (vivo); all four languages switch, persist and render Today on the release build. See the release record.*
- [x] Merged permissions equal the reviewed set pinned in `OfflineTest` (012's `RECORD_AUDIO`; 013's Play Asset Delivery `FOREGROUND_SERVICE_DATA_SYNC`, declared in Play Console); no `INTERNET` or microphone foreground service. Voice follows the amended ADR-0022 chain (on-device, consented system default, user-chosen installed provider); Privacy copy, consent, picker, Settings and unavailable/denied paths match it.
- [x] The Play base download is under 12 MB (013 measured 6.97 MB for arm64; the voice-model packs are on-demand and excluded); start-up and scroll measurements are recorded.
- [x] From 013: decide universal sideload APK size / ABI packaging (45.3 MB with uncompressed `libvosk.so` for four ABIs; options include compressed native libs, per-ABI APKs, dropping x86 from release).
- [ ] From 013: verify real Play Console on-demand delivery of the `voice_model_<lang>` packs (Download, progress, install, Remove); 013 verified with bundletool local testing only. *Open: needs a Play Console internal-testing upload by the owner; the AAB itself is validated (release record).*
- [ ] `docs/prd/09-mvp-acceptance-criteria.md` passes in full. *Partially: results per group in the release record; the environment-limited rows stay open.*
- [ ] `docs/testing/02-manual-qa-checklist.md` passes on two devices. *Partially: release smoke on an API 36 emulator and the vivo; the full checklist was not run (release record).*
- [ ] `docs/delivery/03-release-checklist.md` is complete. *Engineering sections done; owner steps (tag, publish, Play Console, store metadata, keystore backup confirmation) listed in the release record.*

## Required tests

The full unit, Robolectric, golden and instrumented suites against the release configuration where applicable; the manual QA checklist on two devices.

## Release record (2026-10-21)

Candidate: source at `9be17b7` (v1.0.17) plus this change set; to be released as **1.0.18 / versionCode 19** by the owner (`scripts/release.ps1 patch --push`). `version.properties` stays at 1.0.17 because the script bumps and commits it. PATCH, not MINOR: the only user-visible change is the version label the release checklist requires.

### Defects found and fixed

- The app version was shown nowhere (release checklist section 2). The Privacy screen now ends with "Version X.Y.Z" in all four languages; `design/` matches; `YouScreenTest.privacyFooterShowsTheReleaseVersion` covers it.
- Every APK and the AAB carried JNA's `libjnidispatch.so` for `armeabi`, `mips` and `mips64` (dead ABIs with no matching `libvosk.so`). These are now excluded.
- No release-blocking defect was found in the app itself.

### Packaging and sizes

Decision and rationale: `docs/00-source-of-truth.md`, "Milestone 011 decisions"; ADR-0021 amendment.

| Artifact | Size |
| --- | --- |
| Play base download (bundletool `get-size`, all ABI × SDK splits) | 6.89-7.30 MB; arm64 7.00-7.03 MB |
| On-demand packs (download) | EN 41.2, RU 46.2, DE 46.4, ES 39.7 MB |
| Sideload `arm64-v8a` / `armeabi-v7a` / `x86_64` APK | 14.7 / 13.6 / 14.9 MB |
| Sideload universal APK | 44.9 MB (v1.0.17: 45.3 MB) |
| AAB (upload only) | 195.4 MB |

Every release APK verifies with `apksigner` and is zip-aligned for 16 KB pages, and its 64-bit libraries have 16 KB ELF LOAD alignment.

### Play/AAB and Play Asset Delivery

- `bundletool validate` passes; `jarsigner` verifies the signed AAB. The bundle config keeps native libraries uncompressed with 16 KB alignment and language splits disabled.
- There are four `voice_model_<lang>` asset packs, each `dist:on-demand`. No model is under `base/assets`; the packs are filled from the pinned, checksum-verified archives.
- **Not verified:** real Play Console delivery (Download, progress, install, Remove). It needs an internal-testing upload, which only the owner can do. Milestone 013's bundletool `--local-testing` run remains the only end-to-end delivery evidence. On a sideloaded build Play Core returns `onError(-1)` (not installed from Play), and Settings offers Import only, as designed.
- Play Console declarations still owed by the owner: `FOREGROUND_SERVICE_DATA_SYNC` (Play Asset Delivery extraction) and `FOREGROUND_SERVICE_SPECIAL_USE` (focus timer); Data safety "no data collected, shared or transmitted".

### Permissions, privacy, offline

The merged release manifest (aapt2) declares `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECORD_AUDIO`, `FOREGROUND_SERVICE_DATA_SYNC`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE` and the dynamic-receiver permission, exactly `OfflineTest`'s set. There is no `INTERNET`, and the build is not debuggable. Exported components: `MainActivity`, plus library endpoints guarded by the platform: WorkManager `SystemJobService` (`BIND_JOB_SERVICE`), Play Core `SessionStateBroadcastReceiver` (`INSTALL_PACKAGES`) and `AssetPackExtractionService` (bound by the Play Store). The FileProvider is not exported. Cloud backup excludes the database and DataStore, and imported models live in `noBackupFilesDir`. During voice use only fixed library/OEM log lines appeared; no transcript was logged.

### Data, upgrade and install

- Room schema version 1 since 1.0.0, no migration; `SchemaMigrationGuardTest` and the schema diff guard pass. A rollback needs an uninstall (Android refuses downgrades).
- Upgrade with the real signing key: signed v1.0.16 → signed v1.0.17, over onboarding, a completed exercise with a note, a reflection and a running focus session. Data was intact; the session resumed on next open, within 2 s.
- Clean install of this change set's QA-signed x86_64 split (emulator) and arm64 split (vivo); both start on Welcome. Switching between universal and per-ABI APKs is an ordinary same-key update.

### Release-build verification (QA-signed, minified)

Run on an API 36.1 Google Play emulator (x86_64) and the vivo V2405A (Android 16, OriginOS 6, arm64):

- Onboarding with the notification permission denied and with it allowed; the Day 1 exercise with a rating and a note containing `|` and `#`; reflection; Day complete; Today's completion state (A3-A5, B1, B2, B5, B7).
- Every tab, Library, a locked technique, History, Progress, You, Privacy, and the language, export, voice-model and reset sheets opened with no `ClassNotFoundException` or serialization failure. Neither device's crash buffer shows a crash.
- Focus: start, +5 min, pause/resume in the app and from the notification, the live countdown notification, the deep link, Back to Today, and confirmed End (B4, H5, H6). With the screen off for 2 minutes on the vivo, the service and the time held. After a force-stop the session restored with a 1 s difference over 34 s (C2). After a reboot on the emulator it restored and kept running (C3).
- The morning reminder (forced WorkManager job) posts once and deep-links to the exercise intro; Back lands on Today (H1, H6).
- Journal export produces valid Markdown with special characters escaped (F7). Reset program keeps settings and returns to Day 1; Erase everything returns to Welcome (G5, G6).
- Languages: English → Russian (in the app), German (Android per-app language setting), Spanish (in the app). Each applies at once, persists over a force-stop and is mirrored in the system setting; user text is unchanged (L1-L3, L5, L10).
- Russian offline voice on the vivo, with the model imported through the document picker (91 MB installed): three phrases dictated while starting to speak at the instant of the tap all arrived complete, first syllable included («позвонить врачу», «купить хлеб», «ответить на письмо»). Itera's own capture ran, so this was the built-in Vosk engine. This closes 010's real-speech gap for Russian.

Performance (release build, median of 10 `am start -W`):

| Device | Cold start to Today | Warm start | Library scroll |
| --- | --- | --- | --- |
| vivo V2405A, 120 Hz | 220 ms | 62 ms | 704 frames, 0 janky, p99 9 ms |
| API 36.1 emulator | 1 247 ms | 324 ms | not representative (host GPU, two emulators) |

No baseline profile was generated; this is recorded as an accepted decision. History scroll was measured only with a few days of data. Progress with 1 500 seeded activities was **not measured**: a release build cannot be seeded, and no seeding tool exists for it.

### Automated suites

- CI task set: `scripts/tests/test_release.py` (10 tests), the `buildSrc` tests, `spotlessCheck lintDebug testDebugUnitTest verifyRoborazziDebug koverVerify assembleDebug` (736 host tests, 354 goldens), the schema guard, and the prototype `assembleDebug`.
- Release: `assembleRelease` with splits, then `bundleRelease lintRelease`, signed with a throwaway QA key.
- Instrumented: `connectedDebugAndroidTest`, 6/6 on API 36. It was not run against a minified build, because the suite installs the debug variant; R8 coverage comes from the release smoke above.
- The edited `release.yml` parses. Its signature-verification and packaging blocks ran locally against these outputs and produced the expected asset set and `SHA256SUMS.txt`. The workflow itself runs only on the owner's tag push.

### Known verification limitations

- API 26-30: no image or device available (L4 below Android 13, the API 26-30 voice paths, the oldest-device timer check from 006).
- External speech provider (consented system default or chosen app): no working provider on the available devices; untested end to end.
- Spanish offline voice on a real device: waived in 013. EN/DE free-form dictation is best-effort with the small models.
- Spoken TalkBack and Switch Access walkthroughs: declined by the owner in this pass. 010's automated label and target audit is the evidence; I1 and the related QA items stay open.
- The full manual QA checklist on two devices was not rerun on the release build: multi-day clock changes, timezone, midnight roll-over, rotation on every screen, maximum font/display size and the prototype side-by-side. 010's debug-build device passes cover most of it.
- Progress with 1 500 activities, and History scroll with a long history: unmeasured.
- Real Play Console pack delivery: an owner action, above.

### Owner steps before and after publishing

1. Review and commit this change set, push, and confirm the `Android` workflow is green.
2. Confirm the release keystore is backed up outside this machine and the four GitHub secrets are still set.
3. Run `.\scripts\release.ps1 patch --dry-run --push`, then `.\scripts\release.ps1 patch --push` (v1.0.18).
4. Review the draft release: universal and three per-ABI APKs, AAB, schemas, mapping, checksums. Paste the 1.0.18 section of `CHANGELOG.md` into the notes, then publish.
5. Optional, for Play: upload the AAB to internal testing, complete the foreground-service and Data safety declarations, and check Settings → Voice → Download/Remove for one language.
6. On the vivo, uninstall the QA-signed build before installing the published APK (the signatures differ).
