# 012 - Voice input & commands

**Status:** implemented (2026-09-27); prototype comparison partial (see Verification gap). **Depends on** 006, 007, 008 | **Blocks** 009 (and therefore 010, 011). Appended without renumbering; execute after core features and their outstanding visual acceptance, before quality passes/hardening/release. Absorbs no historical issue.

## Goal and scope

Make text entry and common actions faster through explicit push-to-talk in English, Russian, German and Spanish. Implement field dictation and the eight typed commands in [the UX contract](../ux/10-voice-input.md), a small deterministic parser, Android recognition adapter, permission/service states, context validation and confirmations. Use existing exercise/task models and focus controller.

No production or prototype implementation is authorised by the documentation change that creates this milestone. Start only on a later user request.

## Source documents

- Product: `docs/prd/01-mvp-scope.md`, `03-functional-requirements.md` (FR-44-49), `04-nonfunctional-requirements.md`, `09-mvp-acceptance-criteria.md` (V).
- UX: `docs/ux/10-voice-input.md`, `03-ux-states.md`, `07-accessibility.md`, `05-prototype-reference.md`; `docs/i18n/00-localization.md` section 13.
- Architecture: `docs/architecture/07-voice-input.md`, ADR-0022, `01-package-structure.md`, `05-error-handling-and-logging.md`.
- Existing business rules: `docs/engine/00-exercise-state-machine.md`, `05-timer-lifecycle.md`, `docs/00-source-of-truth.md` milestone 006 decisions.
- Verification: voice rows in `docs/testing/01-test-matrix.md`, voice manual QA, `docs/delivery/02-definition-of-done.md` within scoped verification.

## Prototype reference

Run `design/` first. Use `ui/screens/Today.kt`, `Exercise.kt`, `Practice.kt`, `Reflection.kt`, `Focus.kt`, `Profile.kt` (privacy copy), `ui/components/Components.kt`, `IteraIcons.kt` and all four `res/values*/strings.xml` catalogues. Add only the shared voice states/mic affordances described in the UX contract. Update prototype and production together; compare light/dark, including always-dark surfaces and large text.

## Acceptance criteria

- [ ] Every listed dictation surface inserts final text once, keeps editing/validation/autosave, handles selections/limits, and never interprets dictation as commands.
- [ ] All eight commands and duration forms work in EN/RU/DE/ES; invalid/unsupported inputs have no business side effects.
- [ ] Add/Complete remain within the active compatible list. Eisenhower sorting is not silently treated as task completion; no global inbox/task manager is added.
- [ ] Matching ambiguity, competing recognition alternatives, EndFocus and CompleteCurrentExercise require visible confirmation; cancel/stale targets never mutate.
- [ ] Focus start/default/specified duration, pause, resume and confirmed end reuse the controller and preserve timer, early-end and combination rules. Exercise completion retains every existing gate.
- [ ] App locale controls recognition and parser; switching cancels the old session and retains committed text.
- [ ] Strictly on-device recognition, blocked platform fallback, missing service/model, denied/revoked permission and retry follow ADR-0022 and UX. API 26-30 remains fully usable through manual input.
- [ ] No background listening/hotword, raw audio storage, transcript logging, AI parser, direct recognition-to-Room writes or new network permission.
- [ ] TalkBack, Switch Access, large text and four-language copy cover all voice states; keyboard/touch can perform every action.
- [ ] Required tests, scoped build/lint and prototype comparison pass; record deviations only. 009 audits these surfaces, 010 owns final goldens/full regression, 011 checks release privacy/permissions.

## Required tests (to implement with this milestone)

`VoiceCommandParserTest`, `VoiceDurationTest`, `VoiceItemMatchingTest`, `VoiceRecognitionTest`, `VoicePermissionTest`, `VoiceConfirmationTest`, `VoiceDictationTest`, `VoiceLocaleTest`, `VoiceFocusIntegrationTest`, `VoiceExerciseIntegrationTest`, `VoicePrivacyTest`; extend `ArchitectureTest`, translation checks and scoped accessibility tests. Cases and test types are specified in the test matrix. This planning change adds test requirements, not executable tests.

## Non-goals

Conversational assistant, generated feedback, AI/NLU dependency, continuous/background listening, custom “Hey Itera”, spoken responses, arbitrary focus-duration expansion, global task CRUD/inbox, cross-exercise item search, voice reset/delete/export, new schema or new Gradle module.

## Verification gap

Required tests, `./gradlew build` (format, lint, unit tests, coverage) and the prototype build pass. On the API 36 emulator the Today action, command sheet and permission rationale were checked against the prototype in light theme only. Full light/dark, always-dark, 2.0 font scale, TalkBack/Switch Access, four-language and real-recogniser (installed model, API 26-30) checks are deferred to 009/010 with the earlier deferred visual checks. Implementation choices are in `docs/00-source-of-truth.md` ("Milestone 012 implementation decisions").

009 follow-up (2026-09-28): fixed the secondary button boundary contrast and stacked large-text actions in production and `design/`. Automated light/dark, four-language and EN/DE 2.0 render/target checks now cover permission, denied, listening and unavailable panels. Rechecked the prototype listening panel and production rationale/system permission-denial UI on API 37. This closes those representative layout checks only; full TalkBack/Switch Access, real recognizer/model and remaining voice-state/device comparisons are still open.
