# Voice input architecture

Planned in [milestone 012](../issues/012-voice-input-and-commands.md). Keep the single module and existing screen state model; no voice database, service framework or AI dependency.

```text
SpeechRecognizer -> transcript
  dictation -> focused field's existing edit/autosave event
  command   -> VoiceCommandParser -> typed VoiceCommand
            -> context validation / confirmation -> existing use cases / repositories
```

## Boundaries

- `core/voice`: small injectable recognition adapter, capability/permission handling and Android lifecycle. It emits partial/final/error events and cannot import DAOs, Room or execute business actions.
- `domain/voice`: pure Kotlin `VoiceCommand`, `VoiceCommandParser`, duration and item matching rules. No Android, persistence or AI imports. Parse outcomes distinguish supported, unsupported, missing/invalid argument and ambiguity.
- Existing feature ViewModels own voice state within their immutable `UiState` and dispatch through existing screen actions, use cases and repository interfaces. Shared presentation belongs in `feature/voice`; generic icon/button primitives remain in the design system. No second navigation controller.
- `FocusSessionController` remains the single timer writer. Exercise completion goes through the existing validation and `CompleteActivityUseCase` path, including combination and day-advance rules. The recognizer/parser never writes Room, DataStore or repositories directly.

Use one active recognition session, bound to route/activity, field or command mode, locale, and a unique session token. Consume a final result at most once. Invalidate callbacks and pending confirmations on cancel, background, route/locale change or process death. Recheck target id, draft and current business state when confirming; if changed, show the refreshed choice or reject. Serialize execution with existing busy guards so repeated taps/callbacks cannot double-add, double-complete or double-start.

## Recognition adapter

Provider selection lives entirely in `AndroidVoiceRecognizer` behind `VoiceRecognizer`; the order is fixed by [ADR-0022](adr/0022-voice-recognition-and-privacy.md): ON_DEVICE (`isOnDeviceRecognitionAvailable` → `createOnDeviceSpeechRecognizer`) → usable SYSTEM DEFAULT (the resolvable `Settings.Secure.voice_recognition_service` component, with consent) → USER-SELECTED INSTALLED PROVIDER (picked from discovered enabled, exported `RecognitionService`s and agreed to by name) → UNAVAILABLE. Non-on-device sessions use `createSpeechRecognizer(context, component)`; the unbound `createSpeechRecognizer(context)` is never used, because some OEM builds leave the selection empty. `VoiceRoute.select` is the pure selection; `SpeechPlatform` is the Android seam (`AndroidSpeechPlatform`); `VoiceConsentStore` (`PreferencesVoiceConsentStore`, DataStore preferences) holds the system consent and the consented component. `VoiceAvailability` exposes only AVAILABLE / CONSENT_REQUIRED / CHOICE_REQUIRED / UNAVAILABLE. A chosen provider that disappears, errors with a bind/service code before it is ready, or gives no callback within 8 seconds is cleared (`PROVIDER_UNUSABLE`) and the user chooses again; there is no fall-through. On API 33+, sessions with a system-default or user-chosen provider get Itera's own capture (`PipedMicrophone`, 16 kHz mono PCM16 through a pipe and `EXTRA_AUDIO_SOURCE`); the on-device recogniser and API 26-32 keep the provider's capture. The pipe, `AudioRecord` and pump thread close on result, error, cancel, release and timeout; Stop sends end of audio; a competing capture ends the session; with caller audio a permission error is the provider's. See ADR-0022 "Caller-provided audio". Service availability alone does not prove the selected language/model is installed. Use API-gated support checks where available and handle runtime failures. Manage creation/listening/cancellation/destruction on the main thread; destroy when released. Declare `RECORD_AUDIO` and the `android.speech.RecognitionService` package-visibility query where required; no microphone foreground service or background worker. [Android SpeechRecognizer reference](https://developer.android.com/reference/android/speech/SpeechRecognizer).

Set `EXTRA_LANGUAGE` to the effective app language, request partial results without depending on them, and use ordinary free-form recognition for both modes. `EXTRA_PREFER_OFFLINE` is a provider hint, not a privacy guarantee. Do not use automatic language switching. [Android RecognizerIntent reference](https://developer.android.com/reference/android/speech/RecognizerIntent).

No automatic retry or switch of recognizer after a failure; retry requires another user action. Recording consent or a provider choice is a settings write through `VoiceConsentStore`, never recognition output. Map busy, timeout, no speech, permission, unsupported language (`ERROR_LANGUAGE_NOT_SUPPORTED`), missing language model (`ERROR_LANGUAGE_UNAVAILABLE`) and missing service to the [UX states](../ux/10-voice-input.md). The keyboard/touch path always works.

## Deterministic parser

Parse one final utterance under one app locale using an explicit, small phrase/alias table in [localisation](../i18n/00-localization.md#13-voice-language-and-command-vocabulary-milestone-012). Match anchored prefixes with token boundaries, longest phrase first; fixed commands must consume the whole utterance. Normalise Unicode/case, outer punctuation and whitespace for comparison only. Preserve original argument spans for `AddItem(text)` and display; never lowercase stored user text.

A recognised reserved phrase with invalid trailing text/arguments must fail; do not fall back to a shorter overlapping command prefix (for example, malformed “complete exercise” must not become CompleteItem).

Extract the suffix as text/query for Add/Complete; reject blank arguments. Treat command-looking words inside that suffix as data, not another command. Outside those free-text arguments, reject trailing garbage on fixed commands, negated commands, unsupported language/vocabulary and chained action utterances; no substring-triggered actions, fuzzy intent inference, LLMs or coaching. If recognition alternatives imply different commands/arguments, ask the user to select/confirm rather than executing silently.

`StartFocus` permits no duration or one positive whole-minute duration in the documented localised form. Support digits and the spelled-out equivalents of the allowed setup choices/suggestions; unit variants are explicit table entries. Malformed, zero, negative, overflow, fractional, mixed-unit or multiple durations return an invalid/unsupported result, not the default. Parse the number separately from validating the current setup choices; valid-but-unavailable minutes lead to setup, never rounding. Tests pin every accepted alias and number form.

For matching/confirmation, use the context policy in the UX spec. No new task entity or global search. Persist only normal edited drafts and accepted business results through existing paths; audio, interim transcripts, recognition alternatives and unexecuted commands are ephemeral, excluded from logs, analytics, saved state and export.
