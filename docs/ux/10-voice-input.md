# Voice input and commands

Planned MVP capability, owned by [milestone 012](../issues/012-voice-input-and-commands.md); implemented 2026-09-27. Voice speeds up existing interactions. It is not a conversational assistant, task manager or global inbox.

## Entry points and dictation

Use explicit push-to-talk: tap a microphone to start one utterance; tap Stop to request the final result, or Cancel to discard it. Do not require a sustained press. Never listen on launch, in the background, after a command, or for a hotword.

| Surface | Dictation targets |
| --- | --- |
| Feynman | Explanation and editable reflection/note fields |
| Premortem | Project, individual failure reasons, mitigation action |
| Evening reflection | Each editable answer, including the weekly variant |
| Eisenhower | Task-entry field |
| 2-minute rule | Checklist task-entry field |
| Habit stacking | Custom anchor and habit fields |
| Other exercise text | Template free-text/list-entry fields, review answer, focus setup task and result note |

Place a microphone beside each eligible editable input, using the existing trailing-action treatment. Tapping it focuses and captures that field and its selection. Dictation never runs the command parser: saying “pause” inserts text. Insert the final transcript once at the captured caret, replacing only the selection; preserve other text and normal undo/editing, limits, validation and autosave. Partial results are a separate preview, never repeated appends. Do not submit a form or add a list row just by dictating. Cancel if the field, selection or text changes while recognition is pending; a late result must not overwrite a new edit or enter another field. Over-limit results remain editable in a preview with an explanation rather than silently losing words.

A separate labelled “Voice command” action in Today, the active exercise and focus setup/timer opens a shared sheet. Do not compete with Today's hero or add a new tab. State the mode, recognition language and available commands for this context. Do not infer mode from the transcript.

## Commands and context

| Typed command | Behaviour |
| --- | --- |
| `AddItem(text)` | Add through the current list's existing Add action: Eisenhower tasks, 2-minute/template checklist entries, or a Premortem reason when that list is active. Required fields, caps and stopwatch semantics remain unchanged. No active compatible list: explain and do nothing. |
| `CompleteItem(query)` | Match only incomplete items in the active exercise checklist that already has a completion action. Eisenhower sorts/selects tasks; it has no task-completion action, so this command is unavailable there. No search across days, history or other exercises. |
| `StartFocus(duration?)` | Use the current focus setup or Today's existing focus entry. Keep its task, technique, unlock/availability checks and suggested duration when omitted. If setup is incomplete, open the existing setup sheet with the parsed duration and require its normal Start action. If ready, start through the existing controller. Never invent a task or replace a running/paused session. |
| `PauseFocus` / `ResumeFocus` | Control the one active focus session via the same actions as buttons. Wrong state: explain, no mutation. |
| `EndFocus` | Always show the existing end-session confirmation. Preserve elapsed-time accounting and the under-60-second discard rule. |
| `CompleteCurrentExercise` | Show a confirmation only when the current exercise's actual completion action is valid. Invoke that action with the existing draft/result. Do not skip steps, supply missing answers/grades, finish a timer, or complete a combination parent prematurely. A next-step/Compare/Done-navigation button is not completion. |
| `ShowCurrentRecommendation` | Show/navigate to Today's existing hero recommendation, including rest/day-complete state; do not create work, generated advice or spoken coaching. |

For `CompleteItem`, compare normalised labels: exact equality first, then contiguous whole-token phrase matching. A unique exact match can complete immediately. Duplicate exact matches or any partial match require a candidate selection and explicit Confirm; zero matches changes nothing. Show full labels and list position to distinguish duplicates. Never fuzzy-match or choose the first row silently. Destructive or ambiguous commands always require visible confirmation; spoken “yes” is not an MVP command. Cancel, Back and dismiss never execute.

Specified durations use the current focus setup's allowed minute choices (currently 15/25/50 plus its seeded suggestion). An unsupported duration opens setup with an explanation and choices, never rounds or silently substitutes. Supporting arbitrary durations is outside this milestone.

## Shared states

| State | UI and next step |
| --- | --- |
| Ready | Field mic or command action; keyboard/touch remains available |
| Permission needed / denied | Brief microphone rationale, Android permission request after the tap; denial keeps typing available. On permanent denial offer system settings, without repeated prompts |
| Listening | Explicit “Listening”, Stop and Cancel; visible language and recognition mode. Static icon/text works with reduced motion |
| Partial / processing | Optional interim text, clearly provisional; Stop waits for final, Cancel still works. No command executes from partial text |
| Final dictation | Insert once, keep field focus and permit editing |
| Recognised command | Show the interpreted action/arguments; execute an unambiguous valid action once and give brief existing-style feedback |
| Ambiguous / confirmation | Reuse sheet radio rows and confirmation dialog, naming the exact item/action and any lost data; Confirm and Cancel |
| Unsupported / missing argument | Short explanation plus localised examples valid here; retry explicitly or cancel. Never reinterpret as a different action |
| No speech / recognition failure | Inline “Couldn't hear that” or specific service failure; Retry and Keep typing. Do not erase the field |
| Service/language unavailable | Explain that voice is unavailable for this language/device; keep manual controls, offer relevant platform settings. Fallback follows the privacy decision in ADR-0022 |
| Cancelled / interrupted | Stop listening; discard pending transcript/confirmation. Navigation, background, locale change and process death never resume listening or replay a command |

## Design and accessibility

Use `IteraIcons` stroke style, existing input/action components, `IteraCard`, `IteraButton`, `surface2`, `ink2`, and `LanguageSheet` container/radio-row treatment. Keep the dark focus/reflection theme. Add only a mic glyph if missing and one shared voice sheet/state preview, with confirmation reusing existing dialogs. No waveform dashboard, chat screen or new visual system.

Milestone 012 must first run the existing prototype, then add these minimal states to `design/` and match them in production in the same change. This documentation pass changes no composables. See [prototype mapping](05-prototype-reference.md) and [accessibility](07-accessibility.md).

All labels, errors, examples and announcements ship in EN/RU/DE/ES with `voice_*` keys; the parser vocabulary and locale contract are in [localisation](../i18n/00-localization.md#13-voice-language-and-command-vocabulary-milestone-012).
