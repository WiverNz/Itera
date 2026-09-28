# Vosk offline-recogniser spike

Throwaway prototype (2026-09-28). Proves an Itera-owned, fully offline recogniser on the vivo V2405A (Android 16), where every Android `RecognitionService` path failed (see ADR-0022). Not part of any production build; nothing here is wired into `app/`.

- Separate app id `com.wivernz.itera.voskspike`, no `INTERNET` permission, arm64 only.
- Itera captures the microphone itself (`AudioRecord`, `VOICE_RECOGNITION`, 16 kHz mono PCM16) and feeds Vosk in memory. No audio is written anywhere; the log carries numbers only.
- Command mode passes the transcript to Itera's real parser, compiled from `app/src/main/java/com/wivernz/itera/domain/voice` (not copied).

## Run

```sh
./gradlew installDebug                     # needs local.properties with sdk.dir
curl -O https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip   # into models/ (git-ignored)
unzip vosk-model-small-ru-0.22.zip -d models
adb push models/vosk-model-small-ru-0.22 /data/local/tmp/
adb shell run-as com.wivernz.itera.voskspike sh -c 'mkdir -p files && cp -r /data/local/tmp/vosk-model-small-ru-0.22 files/model'
adb shell rm -rf /data/local/tmp/vosk-model-small-ru-0.22
```

Files adb pushes into `Android/data/<pkg>` are not readable by the app on Android 11+, hence `run-as` into internal storage (debuggable builds only).

## Results on the vivo (vosk-android 0.3.75, vosk-model-small-ru-0.22)

| Test | Heard | Parser | Final after Stop | RTF |
| --- | --- | --- | --- | --- |
| Dictation «Позвонить сантехнику завтра утром» | «позвонить сантехнику завтра утром» (exact) | - | 17 ms | 0.15 |
| Command «Начни фокус на двадцать пять минут» | «начни фокус на двадцать пять минут» (exact) | `Recognised(StartFocus(minutes=25))` | 20 ms | 0.17 |

- Model: 46 MB zip, 87 MB unpacked. Cold load 646-734 ms; recogniser creation 29-39 ms.
- Memory: RSS 136 → 365-384 MB peak (includes memory-mapped model files); native heap about 175-195 MB; total PSS 279 MB.
- APK: `libvosk.so` 10.0 MB + `libjnidispatch.so` 0.18 MB (arm64, stored uncompressed); spike APK 11.2 MB total.
- Microphone capture logged by Android as `not silenced`.
