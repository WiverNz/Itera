# Offline voice models (milestone 013)

Itera's built-in recogniser (Vosk, ADR-0022) needs one model per language. Models are never in the base APK and never in Git.

| Language | Archive | Zip | Unpacked |
| --- | --- | --- | --- |
| English | `vosk-model-small-en-us-0.15` | 41 MB | about 68 MB |
| Russian | `vosk-model-small-ru-0.22` | 46 MB | 87 MB (measured) |
| German | `vosk-model-small-de-0.15` | 46 MB | about 70 MB |
| Spanish | `vosk-model-small-es-0.42` | 40 MB | about 75 MB |

All four are Apache-2.0, from <https://alphacephei.com/vosk/models>, pinned by SHA-256 in `models.properties`. The app's `VoiceModelCatalog` must match it (`VoiceModelCatalogTest`).

## Google Play (AAB)

Each `voice_model_<lang>` module is an **on-demand** Play Asset Delivery pack carrying the model under `assets/model/`. `bundleRelease`/`bundleDebug` run `fetchVoiceModel` for every pack: download into `<gradle user home>/caches/itera-voice-models` (or read from `-Pitera.voiceModels.dir=<dir>`), verify size and SHA-256, unpack into `voice_model_<lang>/src/main/assets/model/` (git-ignored). APK, test and lint builds never fetch. Users download a pack only from Settings → Voice → Offline speech model → Download; the Play Store downloads and extracts it, so Itera needs no network permission.

Local testing of the Play path: `bundletool build-apks --local-testing --bundle app-debug.aab --output itera.apks` then `bundletool install-apks --apks itera.apks`.

## APK / sideload / debug

Settings → Voice → Offline speech model → Import file opens the system document picker. Pick the official zip for the language (the sheet names it). Only the pinned archives are accepted; the checksum decides the language. Files pushed with adb must be indexed first (`adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/<file>.zip`) or some pickers will not list them.
