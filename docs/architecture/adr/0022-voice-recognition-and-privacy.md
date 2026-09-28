# ADR-0022: Explicit voice input and recogniser consent

Status: accepted (2026-09-26); implemented in milestone 012 (2026-09-27). **Amended 2026-09-28** by an explicit product/privacy decision from the user: the earlier "strictly on-device or unavailable" rule is replaced by the consented fallback chain below.

## Decision

Add `RECORD_AUDIO` only for user-initiated, foreground, single-utterance dictation/commands. Request it on first mic use with a rationale; denial or revocation leaves all keyboard/touch interactions available. No continuous listening, hotword, microphone foreground service, audio file or voice history.

## Recogniser selection

Itera picks one recogniser per session, in this order, inside the `core/voice` adapter (`AndroidVoiceRecognizer`). Features and UI only see availability states, never which provider listens.

1. **ON_DEVICE.** When `SpeechRecognizer.isOnDeviceRecognitionAvailable` is true (API 31+), use `createOnDeviceSpeechRecognizer`. No consent beyond `RECORD_AUDIO`.
2. **Usable SYSTEM DEFAULT, with consent.** Otherwise, if `Settings.Secure.voice_recognition_service` names a component that actually resolves as an enabled, exported `RecognitionService`, use it through `createSpeechRecognizer(context, component)`. First use shows a consent panel: on-device recognition is unavailable, the system provider may process audio remotely and use the network, Itera does not store the audio or transcript. "Use system recognition" / "Not now".
3. **USER-SELECTED INSTALLED PROVIDER, with consent.** Otherwise, if installed apps expose an enabled, exported `RecognitionService`, show a picker with each app's own label and icon. Nothing is pre-selected. Picking an app opens a consent panel naming it: it is a separate app, may process audio remotely and use the network under its own privacy terms, Itera does not store the audio or transcript. Only "Use <app>" stores the choice; the flattened `ComponentName` is persisted only after that acceptance, so a stored component is that consent. Sessions use `createSpeechRecognizer(context, component)`.
4. **UNAVAILABLE.** No recogniser: explain, offer speech settings, keep typing.

Rules:

- **Never pick a provider for the user.** No "first installed service" fallback. The current assistant app is not treated as the system default or as trusted; only the explicit `voice_recognition_service` selection counts, and only if it resolves (OEM builds can leave it empty or name a service they hide).
- **Never fall through.** A chosen provider that disappears or is disabled, or that cannot serve Itera (a bind/service error before it reports ready, or no callback at all within 8 seconds), is cleared, and the user is asked to choose again with a short explanation. Itera never silently switches to another provider.
- **Nothing is inferred to be offline** beyond step 1. `EXTRA_PREFER_OFFLINE` is sent as a hint and is never a privacy guarantee.
- A usable system default takes precedence over an installed-app choice; while it exists, step 3 is not offered.
- Consent is local (DataStore user preferences: `voice_system_recognition_allowed`, `voice_selected_recognizer`). Settings → Voice can turn system recognition off and change the chosen app or set it to None; choosing a different app there shows that app's consent first. "Erase all data" clears both.
- The manifest keeps the Android 11+ package-visibility query for `android.speech.RecognitionService`; no network permission is added. Itera has no `INTERNET` permission, but that does not restrict another app or service that performs recognition.

## Earlier rule (superseded)

The 2026-09-26 decision allowed only on-device recognition and blocked `createSpeechRecognizer`. On devices without an on-device recogniser (verified on a vivo V2405A, Android 16) voice was therefore always unavailable. The amendment keeps on-device first and adds steps 2 and 3 behind explicit, named consent.

## Consequences

The complete training app remains offline and fully usable without voice. Voice availability depends on Android version, the on-device service, the system selection, installed recognition apps and language models; four-language parser/UI support does not promise every device or provider handles all four. A discoverable `RecognitionService` does not prove it accepts third-party clients: on the vivo test device the Claude app's service is discoverable and bindable but rejects Itera's `startListening` ("caller without permission RECORD_AUDIO"; its microphone access is foreground-only), and when its process is not running no callback arrives at all. Itera then clears the choice as above. No automatic model download: direct users to platform settings if needed.

Audio and provisional transcripts are never stored, logged, exported or sent to analytics by Itera, whichever provider listens. Final dictation and accepted item text enter existing local drafts/results under the same privacy rules as typing. Recognition and parsing cannot bypass business rules or write Room. See [architecture](../07-voice-input.md) and [UX](../../ux/10-voice-input.md). [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer), [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent).
