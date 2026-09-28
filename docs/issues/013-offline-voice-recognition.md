# 013 - Built-in offline voice recognition

**Status:** complete (2026-09-28). **Depends on** 012 | **Blocks** 011 (release readiness). Pre-release milestone, appended without renumbering; 010 and 011 are not part of it.

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

## Verification (2026-09-28, vivo V2405A, Android 16; production app)

| Language | Model install | Dictation (heard) | Command |
| --- | --- | --- | --- |
| RU | import, 3 s; survived two app updates | «позвонить сантехнику завтра утром» (exact, with a short pause after «Слушаю»); «подготовить отчёт ли клиента» («для» → «ли»). Speaking at the instant of the tap lost «по» twice | Today «Начни фокус на двадцать пять минут» → focus setup with 25 min (grammar) |
| EN | import, 2 s | "of blender about kitchen sink"; "the report for the client" (first words lost). The spike gives the same class of errors with the same model and speaker, so it is model/accent accuracy, not integration | Today "What should I do now?" → "Here's what to do now." (grammar) |
| DE | import, 2 s; later Play Asset Delivery (local testing) | "dann grip namen morgen früh anrufen"; "den bericht führte kunden für bereiten" | Today "Starte Fokus für fünfundzwanzig Minuten" → setup with 25 min (grammar) |
| ES | not tested (skipped at the user's request) | - | - |

- Lifecycle: Remove deletes the model; a model missing `graph/Gr.fst` fails the integrity check and voice falls through to the provider picker without loading Vosk; a language without a model falls through; switching language uses that language's model.
- Play Asset Delivery (`bundletool --local-testing`): Download appears only when Play is available; only `voice_model_de` was installed on tap, extracted to `assetpacks/voice_model_de/…/assets/voice_model_de/`; the offline engine then listened with it. The installed app declares no `INTERNET`; `FOREGROUND_SERVICE_DATA_SYNC` is present, as pinned in `OfflineTest`.
- Sizes: base download (arm64, bundletool) 6.97 MB; packs EN 41.2, RU 46.2, DE 46.5, ES 39.8 MB; installed EN 68 MB, RU 87 MB, DE 91 MB on disk; release AAB 195.3 MB; universal release APK 45.3 MB (1.0.10: 4.3 MB) because `libvosk.so` for four ABIs is stored uncompressed (40.7 MB).
- Latency: cold model load about 0.5 s (RU, production log timeline); the spike measured 0.4-0.7 s cold and a final result 17-20 ms after Stop; production ends at the first endpoint with words.
- Build: full `build`, `spotlessCheck lintDebug testDebugUnitTest verifyRoborazziDebug koverVerify assembleDebug` (600 host tests), `bundleRelease` with the configuration cache, and the prototype build pass.

## Closing decisions (product owner, 2026-09-28)

- **Spanish device run waived** for 013. ES implementation, model metadata, localisation and automated coverage remain required and are in place.
- **Small Vosk models accepted for MVP.** EN/DE free-form dictation accuracy is best-effort; no move to the larger models. Restricted-grammar commands are the reliability-critical path and pass in RU, EN and DE.
- **Deferred:** first-syllable loss when speaking at the instant of the tap → [010](010-test-hardening.md); until then recognition should begin after the Listening state appears. Universal sideload APK size / ABI packaging → [011](011-release-readiness.md). Real Play Console Asset Delivery → [011](011-release-readiness.md); bundletool local testing is sufficient for 013.
