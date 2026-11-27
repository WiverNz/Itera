# Post-MVP roadmap and milestone scopes

Status: **staged direction approved, 2026-11-27**. [Milestone 014](../../issues/014-environment-design-and-prediction-prototype.md) implements Stage A and prepares the Stage B interactive prototype for review. Stage B production work below is not authorized until prototype approval. Stage C is future-only; Stage D remains deferred.

Depends on the shipped daily flow, exercise infrastructure, history/export and privacy contracts. Review [research](research.md), [proposal](proposal.md) and [ADR-0023](../../architecture/adr/0023-prediction-testing-proposal.md) first. The Stage B production backlog remains proposed; current implementation and actual checks are tracked in milestone 014.

## Roadmap and independent gates

| Stage | Scope | Advance only if | Stop / reduce scope if |
| --- | --- | --- | --- |
| A — first | Environment Design examples in existing techniques and reflection | Users can remove a real obstacle and assess the next attempt using existing fields | It becomes tidying, mandatory preparation, redundant advice, or needs a new tracker to be usable |
| B — second | Optional structured Prediction Testing, detailed below | Prospective comparison adds useful learning beyond ordinary reflection and the migration/UX cost is accepted | Users mainly fill forms or feel pushed to disprove their prediction |
| C — conditional, separate | Optional no-hold breathing pause before Focus | Users want the pause, can use/exit it accessibly, and do not treat it as required readiness | It delays work, creates discomfort/pressure, or an ordinary quiet pause serves equally well |
| D — deferred discovery | BLUF and possible Communication scope | Repeated user need and a coherent multi-technique communication curriculum justify a sixth skill | One writing exercise is the only rationale for expanding categories |

No dependency requires A users to adopt B or C. B's experiment data must not become a generic tracking framework for every candidate. No automatic promotion to technique 15. Select and approve each implementation milestone separately after documentation/prototype review.

## Stage A: Environment Design extension

Approved scope: extend Habit stacking (cue plus ready materials), Information diet (reversible friction for distractions), 1% improvement (evening first-step preparation and next-opportunity evaluation), and the existing reflection questions. Ship EN/RU/DE/ES copy in production and prototype. Keep all 14 techniques, five skills, days, completion rules and result fields. Content version advances to 2 in both assets; schema remains 1. There is no follow-up scheduler, new tracker or new practice credit.

Acceptance criteria:

- [x] A user can identify one obstacle, make one bounded reversible change and express the first action/cue using existing fields.
- [x] Evening preparation is optional; “before the next opportunity” works for irregular schedules. No-opportunity, unchanged and harder outcomes are accepted in ordinary reflection.
- [x] Choosing an example never overwrites existing text without a deliberate user action, completes a target task, or creates duplicate training credit.
- [x] Preparation neither blocks Start focus nor introduces a required reflection question, new plan row, notification or metric.
- [x] Existing draft persistence, carry-over, History and export retain the note. UI does not promise automatic paired follow-up when the existing carry-over no longer surfaces it.
- [x] No schema change, new technique/skill, habit-formation deadline, security bypass or coercive environmental restriction.
- [ ] All copy is EN/RU/DE/ES; applicable large-text, accessibility, prototype-parity and changed-area checks pass.

Evaluation: use a small voluntary usability round with a real low-risk preparation opportunity, followed by a next-opportunity interview; do not require a calendar-day return. Compare with the current generic tomorrow-change prompt. Ask what obstacle actually disappeared, whether the task was attempted, and whether the prompt added burden. No journal collection/telemetry. If added value is unclear, retain examples only or stop. These are usefulness observations, not causal efficacy evidence.

## Stage B: Prediction Testing — objective and scope

Let a user preserve one prediction before a small everyday action and compare it with the observed result later, without forced positive conclusions or changes to existing curriculum/progress. Provide a local, optional Daily reflection extension with one open experiment, durable cross-day resume, neutral outcomes and complete journal export.

Non-goals: technique 15; curriculum edits; new mastery thresholds; automatic recall reviews; generated combination integration; clinical treatment; trauma/memory exercises; neuroscience timing; automatic text analysis; AI/networking/accounts; task manager; notifications for experiments; retrospective prediction entry; new global activity states; external recognition for experiment fields.

## Stage B approval gate

- [x] Product owner approved the direction and prioritization, with prototype review before production.
- [ ] Settle ADR-0023: Reflection placement, no independent mastery credit, one-open-record limit, reset tier, discovery location, two-phase duration wording and local-only input restriction.
- [ ] Approve prototype evaluation and its go/no-go criteria before production work. If unsuccessful, narrow to a prompt or close the candidate.
- [x] Assign milestone 014 to Environment Design and prototype work only.
- [ ] Obtain prototype approval before assigning a separate production milestone. Keep the original 14-day curriculum intact.

## Stage B production tasks after prototype approval

1. **Content and prototype.** Read/run `design/`, use Explore with demo data, walk Today, Daily reflection, Technique Detail, History and Premortem. Add preparation/comparison/read-only/error states using the existing components; update all four prototype locales. Conduct the bounded usability review and resolve supported/inconclusive outcomes and safety misunderstandings before production coding.
2. **Domain and storage.** Add the experiment model, repository and pure transition/validation rules. Implement one-open-record enforcement, immutable prepared snapshot, outcome drafts, linked repeats and closed-without-outcome. Add Room migration/schema, transaction-safe writes, indexes, reset assignment and fixture coverage. Retain existing IDs and result serial names.
3. **Presentation.** Add typed routes, immutable UiState, events/effects, ViewModel autosave and scoped errors. Wire manual entry/resume from Daily reflection detail and optional evening link. Preserve reflection drafts when entering/exiting. Provide accessible choices and no-default confidence/comparison controls. Keep Today hero and counters unchanged.
4. **Privacy/input.** Restrict the new fields to keyboard and verified local speech engines, falling back to typing. Do not change the global voice preference or permit a silent provider substitution. Keep authored data out of diagnostics, navigation arguments, analytics and notification content.
5. **History and export.** Add status-labelled experiment records, dual dates, detail view and repeat action. Extend the pure/streaming writer and range selection with one complete paired record per experiment. Preserve existing exports and techniques summary order. Update reset confirmation and export disclosure text where needed.
6. **Compatibility and delivery.** Verify upgrades with populated v1 data and older result payloads; preserve all progress, preferences, unlocks, plans, reviews and timer state. Update relevant data/UX/architecture specs and ADR status only after approval. Document actual deviations or blocked checks, not a repository-wide re-audit.

## Stage B future production acceptance criteria

- [ ] A user can record situation, prediction, optional confidence, bounded action and observable criterion before acting; explicit save persists before showing readiness.
- [ ] The original snapshot cannot silently change after preparation. A revised test is a linked new record with an empty outcome.
- [ ] An experiment survives leaving the app, rotation, process restart, midnight, multiple missed days, DST/time-zone changes and notification denial. Existing daily activity expiration remains unchanged.
- [ ] The user can record observations, choose Supported / Partly supported / Not supported / Inconclusive, optionally reassess confidence and save a learning statement or explicit no-conclusion choice.
- [ ] Unchanged or increased confidence and a supported prediction save exactly as readily as a contradicted prediction. No result implies success/failure, diagnosis or emotional improvement.
- [ ] “Not tried,” “Review later,” and “Close without an outcome” do not invent evidence or grant credit. An open record never blocks ordinary training, evening reflection, rest days or program advancement.
- [ ] One open record is enforced even under rapid taps/concurrent resume attempts. Replayed save actions do not create duplicates. Reviewed/closed records are read-only; repeating creates a fresh linked draft.
- [ ] Existing reflection saves/skip/carry-over work unchanged; entering an experiment preserves an unfinished reflection. An experiment completion alone does not complete reflection, affect practice counters, alter mastery or create a review item.
- [ ] History distinguishes experiment work from counted training; date-range exports include the original context with the outcome exactly once. Open/closed/inconclusive states are represented honestly, with localized labels and unchanged authored text.
- [ ] Upgrading a populated database preserves historical rows and payloads, programDay, unlocks, current plan, review due dates and preferences. No content re-seeding or forced reset. Older APK downgrade remains non-destructive.
- [ ] Reset program and Erase everything remove all experiment data and stale export copies under their existing policies, with updated disclosure. Reset coverage includes every new table.
- [ ] Every user-facing addition exists in EN/RU/DE/ES. Large-text, TalkBack, focus/keyboard and reduced-motion checks pass for the new flow; no clipping or colour-only meaning.
- [ ] The feature works in airplane mode and without a microphone model. The external recognizer fallback cannot receive experiment text/audio, even when separately consented elsewhere.
- [ ] No new permission, dependency capable of networking, remote service, authored-data log or analytics payload is introduced. Examples exclude dangerous/clinical tests and never claim a neuroscience window.
- [ ] Production and `design/` match in both themes and all new states. Existing always-dark screens retain their treatment.

## Stage B proportional verification plan

Pure rules/VM tests: every permitted/forbidden transition; blank observation; explicit no-conclusion; all comparison and confidence directions; frozen snapshot; duplicate save; save failure retaining edits; repeat; close; concurrent open-record creation; resumed draft. Test real failure/race behaviour, not just setters.

Room/integration tests: populated 1→next migration and schema guard; original JSON decoding; FK behaviour; both resets; transactional rollback; day rollover without experiment expiry; completion isolation from program/mastery/review machinery. Test export ranges crossing preparation/outcome dates, multiline special characters, all locales, unfinished records and no duplicate entries. Preserve old export fixtures except for intentional format additions.

UI/device checks: new flow content and neutral choices; draft round-trip from reflection; supported and inconclusive examples; local-only voice fallback; process death after a confirmed save; midnight; 2x font/display settings; TalkBack; themes and four languages. Use only synthetic/demo content for screenshots and tests.

Run `./gradlew build` (including relevant lint/format/unit/coverage gates) and changed-area tests, plus required migration/device and prototype parity checks. Apply [definition of done](../../delivery/02-definition-of-done.md) within [verification scope](../../issues/README.md#verification-scope). Record unavailable device checks accurately. No new exhaustive whole-repository verification report. Human review remains required by the definition of done. Do not commit or push.

## Stage C: conditional breathing-pause tool

Tasks after separate approval: prototype a no-hold guide using existing Focus styling; review original safety copy and pacing controls; implement a pure foreground phase controller and accessible visual/nonvisual cues; connect to focus setup without altering the focus timer or completion machinery. Keep duration and pace choices session-local. Retention, health measurement and background operation are excluded.

Acceptance criteria:

- [ ] Start focus remains the primary action; the guide is optional and easy to stop at any moment, including mid-phase.
- [ ] Configurable 1/2/3-minute duration and agreed comfortable no-hold pacing work without promising a validated therapeutic dose. End returns to normal breathing and setup, not automatic focus start.
- [ ] Phase labels, reduced-motion display, TalkBack and opt-in cues provide usable alternatives to animation. No colour-only meaning, flashing or overlapping announcements.
- [ ] Backgrounding, lock, interruption and process death stop all cues; returning never resumes a hold, sound or exercise unexpectedly. Rotation follows the explicit controller contract.
- [ ] A running focus timer cannot be silently paused/replaced; a previously paused one remains paused. Breathing time never contributes to focus minutes or activity completion.
- [ ] Safety copy includes gentle breathing, stopping for discomfort and safe-at-rest use. No “reset your nervous system,” medical claim, compulsory calm state or breath-hold challenge.
- [ ] No Room/history record, metric, notification, new permission, sensor, foreground service or network. All four languages and new UI states match the prototype.

Verification: deterministic phase/end/stop/interruption tests with fake monotonic time; UI checks with motion disabled, muted audio, screen reader and 2x font; device rotation/background/lock and paused-focus coexistence; four-language safety-copy review. Compare voluntary usefulness with a simple unpaced pause before deciding to ship. A discomfort report or misunderstanding is a design issue to resolve, never a user failure. Apply scoped build/lint/test and definition-of-done requirements when implemented.

## Stage D: BLUF discovery only

Use the proposal's synthetic email/request/decision examples to assess whether users can identify the main point and preserve important caveats. Ask intended readers for voluntary clarity feedback without connecting mail or collecting real messages. Do not infer usefulness from word count alone. No product route, skill enum, catalogue entry or sending feature is in scope.

Before any implementation proposal: demonstrate recurring communication needs beyond a single writing convention; compare with existing Feynman and planning exercises; specify a coherent Communication curriculum and upgrade behaviour; approve skill tokens, onboarding/progress/combination implications, privacy, and four-language terminology. Otherwise leave BLUF outside Itera. Research can conclude that a technique is useful without recommending that this product teach it.
