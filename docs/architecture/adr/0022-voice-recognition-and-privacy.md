# ADR-0022: Explicit on-device voice input

Status: accepted (2026-09-26); implemented in milestone 012 (2026-09-27).

## Decision

Add `RECORD_AUDIO` only for user-initiated, foreground, single-utterance dictation/commands. Request it on first mic use with a rationale; denial or revocation leaves all keyboard/touch interactions available. No continuous listening, hotword, microphone foreground service, audio file or voice history.

The user explicitly chose **strictly on-device recognition**. On API 31+, prefer the on-device `SpeechRecognizer` factory after checking availability, then handle language/model support. If unavailable (including API 26-30), offer manual input and relevant speech settings; do not start the default platform recognizer. No network permission is added.

## Platform fallback (blocked)

The initial feature request proposed falling back to `createSpeechRecognizer`. That provider may send audio off-device; `EXTRA_PREFER_OFFLINE` is not an enforceable guarantee, and Itera's lack of `INTERNET` does not restrict another app/service. Therefore generic platform fallback is **blocked until an explicit product/privacy-policy change**. Do not implement a consent dialog or a hidden fallback in this milestone. A future change must specify provider disclosure, consent, retention and release privacy claims before enabling it. [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer), [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent).

## Consequences

The complete training app remains offline and fully usable without voice. Voice availability depends on Android version, on-device service and installed language models; four-language parser/UI support does not promise every device has all four models. No automatic model download: direct users to platform settings if needed.

Audio and provisional transcripts are never stored, logged, exported or sent to analytics. Final dictation and accepted item text enter existing local drafts/results under the same privacy rules as typing. Recognition and parsing cannot bypass business rules or write Room. See [architecture](../07-voice-input.md) and [UX](../../ux/10-voice-input.md).
