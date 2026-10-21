# Changelog

Versions follow `docs/delivery/03-release-checklist.md` section 2. Releases 1.0.0-1.0.17 were internal draft builds made while milestones 001-013 were implemented; their GitHub drafts carry generated notes.

## 1.0.18 - first public release (planned; tag with `scripts/release.ps1 patch`)

Milestone 011, release readiness. Includes every MVP milestone: 001-010, 012 and 013.

### What's in it

- Daily practice of fourteen productivity techniques: one exercise in the morning, an optional Pomodoro or Deep Work focus block, a two-minute evening reflection. Program days advance when you train, never by the calendar.
- Train, the technique library, Progress, History, journal export to Markdown, reminders.
- English, Russian, German and Spanish, switchable at any time.
- Push-to-talk voice input and commands. Itera's own offline speech model per language (Play download or file import, 40-46 MB) keeps audio on the phone.
- Offline by design: no account, no backend, no `INTERNET` permission.

### Changed in this release

- The Privacy screen shows the app version.
- Sideload downloads are split per ABI (see below). The universal APK no longer carries dead JNA libraries for `armeabi`, `mips` and `mips64`.

### Which file to download

| File | For | Size |
| --- | --- | --- |
| `Itera-v1.0.18-arm64-v8a.apk` | Almost every phone from the last several years | about 14.7 MB |
| `Itera-v1.0.18-armeabi-v7a.apk` | Older 32-bit phones | about 13.6 MB |
| `Itera-v1.0.18-x86_64.apk` | Emulators and Chromebooks | about 14.9 MB |
| `Itera-v1.0.18.apk` | Any device, if unsure | about 44.9 MB |
| `Itera-v1.0.18.aab` | Google Play upload only | - |

Every APK is signed with the same key, so any of them installs over an earlier Itera release and keeps your data.

### Upgrade and rollback

- Database schema version 1, unchanged since 1.0.0; no migration runs.
- Android refuses to install an older version over a newer one. Rolling back means uninstalling, which deletes local data; export the journal first.
- A focus session running during an update or a reboot resumes, with its notification, the next time Itera is opened.

### Known limitations

- Not tested on Android 8-11 (API 26-30) hardware or emulators.
- The external speech-provider path (system or chosen recognition app after consent) is untested with a working provider.
- Spanish offline voice is untested on a real device; English and German free-form dictation are best-effort with the small models.
- Play Console on-demand delivery of the voice-model packs is verified with `bundletool` local testing only.
