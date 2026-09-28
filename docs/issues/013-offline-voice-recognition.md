# 013 - Built-in offline voice recognition

**Status:** in progress (started 2026-09-28). **Depends on** 012 | **Blocks** 011 (release readiness). Pre-release milestone, appended without renumbering; 010 and 011 are not part of it.

## Goal and scope

Give voice a recogniser Itera owns, so dictation and commands work on phones whose Android recognition services are missing or unusable (verified on a vivo V2405A, Android 16: no on-device recogniser, Google's provider rejected by the platform, Claude does not serve third-party apps, Russian unsupported by Google). Technical proof: `spikes/vosk-android/` (exact Russian dictation, `StartFocus(25)`, cold load about 0.7 s, final result about 20 ms after Stop).

- Vosk Android + JNA behind the existing `VoiceRecognizer` abstraction; Itera owns `AudioRecord`; separate EN/RU/DE/ES models; never all models in the base APK.
- Provider order (locked): ON_DEVICE → ITERA OFFLINE (model for the current language installed) → usable SYSTEM DEFAULT → USER-SELECTED external → UNAVAILABLE. A missing model never blocks voice.
- One loaded model cached app-wide for the active language; released on background (`trim`), on language change and never mid-decode.
- Partial/final transcription and the deterministic parser unchanged; a restricted phrase grammar only in command contexts without free-text commands (focus, Today); dictation stays free-form.
- Model store: per-language state, path, version, checksum; Play download, document-picker import, remove; damaged/incompatible handling.
- Delivery: one on-demand Play Asset Delivery pack per language (AAB); document-picker import of the pinned official archive for APK/sideload builds; debug builds use either. No model archive in Git.
- Settings → Voice: the offline model for the current app language, size/state, Download/Import/Remove. A language change offers the new language's model; nothing downloads automatically. Manual input everywhere.

Non-goals: replacing the parser or voice UI/business actions, deleting the existing provider fallbacks, cloud recognition, automatic downloads, milestones 010/011.

## Source documents

- ADR-0022 (recogniser order, offline model, privacy), `docs/architecture/07-voice-input.md`, `docs/ux/10-voice-input.md`, `docs/00-source-of-truth.md` milestone 013 decisions, `docs/architecture/06-dependency-catalog.md`, `voicemodels/README.md`.
- Spike and measurements: `spikes/vosk-android/README.md`.

## Prototype reference

`design/`: `ui/screens/Profile.kt` Voice section (offline model row) and all four `milestone013.xml` catalogues.

## Acceptance criteria

- [ ] Offline provider sits second in the chain; a missing or damaged model continues down the chain; the external fallbacks remain.
- [ ] Itera's capture is 16 kHz mono PCM16 in memory only; no audio file, transcript log, cloud path or network permission.
- [ ] Model store: pinned catalog matches `voicemodels/models.properties`; import accepts only the pinned archives (checksum identifies the language), rejects others and unsafe zips; integrity check on required files; load failure marks the model damaged and removes it.
- [ ] PAD packs are on-demand, one per language; `bundleRelease` fetches and verifies the models; APK/test/lint builds do not; the merged manifest has no `INTERNET`.
- [ ] Settings shows state/size for the current language with Download (Play only)/Import/Remove; four-language copy; prototype matches.
- [ ] Real dictation and at least one command pass in EN, RU, DE and ES on a device.
- [ ] Tests for order, fallback, engine lifecycle, grammar, store and manifest; build, lint and scoped tests pass.

## Verification

Record device results by language here when the milestone is closed.
