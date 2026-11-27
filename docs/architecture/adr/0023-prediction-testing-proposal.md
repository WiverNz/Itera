# ADR-0023: Bounded post-MVP extensions before new techniques or skills

Status: **accepted in stages (2026-11-26)**. Environment Design curriculum extensions and a Prediction Testing interactive prototype are authorized. Production Prediction Testing still requires prototype approval. Breathing pause remains future-only; BLUF remains deferred.

## Approved implementation boundary

Milestone 014 extends existing localized task/help copy and reflection guidance without changing technique IDs, curriculum order, completion rules, storage, or progress. Preparation is optional and distinct from performing the target behavior. Existing reflection answers, tomorrow-change carryover and journal export retain their current meaning.

Prediction Testing lives only in `design/`: a separate immutable state model, progressive preparation/review screens and one local experiment stored in an atomic file under Android noBackupFilesDir. It survives navigation, process restart and prototype day advancement. This deliberately bounded prototype retains one reviewed record until the user explicitly replaces it; production history/export and multi-record persistence remain gated. No experiment action grants practice credit. Existing reflection drafts stay in the prototype ViewModel when visiting the extension. Keyboard input only. All new copy ships in four languages.

The user requested no desktop capture. Validation uses build checks and Android emulator interaction/tests without capturing the host display. Visual sign-off and any unperformed accessibility walkthrough remain explicit review gates, not assumed passes.

## Context

The user requested scientific research and a post-MVP design assessment of Behavioral Experiments, then extended the request to Environment Design, BLUF and breathing support. Existing activities expire at calendar rollover; Daily reflection stores three retrospective answers; review scheduling tests recall. None provides a durable prospective prediction/outcome pair. A fifteenth technique would also require changes to the fixed catalogue validator and curriculum assumptions. A sixth skill would affect onboarding, progress, content and combination rules.

## Staged decision

Prioritize optional **Environment Design examples** within existing Habits techniques and reflection, with no new record type or progress credit. Next evaluate **Prediction Testing** as described below. Consider a **Breathing pause** only as a separately approved supporting tool before Focus: no holds in the first version, no practice credit or persistence, and foreground-only pacing that stops on interruption. **Defer BLUF and a Communication skill** until a coherent broader product case exists. Retain all five skills and fourteen techniques. No compulsory curriculum additions or new daily checklist items.

These stages remain independently gated. Milestone 014 covers Environment Design and the Prediction Testing prototype. The expanded proposal owns the later production design; approving this ADR does not authorize that production work.

Evaluate **Prediction Testing** as an optional Daily reflection extension in the Reflection skill. Preserve all 14 techniques, existing mastery thresholds, curriculum days, generated combinations and review scheduling. Introduce a dedicated, resumable two-phase interaction backed by a local experiment record, while reusing existing UI components. No progress credit for preparing or merely reopening an experiment; the initial extension grants no additional mastery credit of its own.

The existing evening reflection can link to an experiment but does not require it. Completing the existing reflection continues to count once under current rules. Experiment records appear in history/export with a separate factual status, not as completed training activities. This keeps the Prediction Testing evaluation bounded and avoids inventing mastery rules before the distinct technique is justified.

Keep experiments outside the daily activity expiration lifecycle. Do not add a new global activity state or use the recall ladder for personal assumptions. No neuroscience claims, clinical treatment, AI, networking, automatic text interpretation or external voice recognition in the new fields.

## Alternatives and consequences

Full analysis, interaction rules, compatibility constraints and proposed acceptance criteria are in the [proposal](../../proposals/prediction-testing/proposal.md), [research](../../proposals/prediction-testing/research.md) and [draft milestone](../../proposals/prediction-testing/milestone.md).

A copy-only prompt is cheaper but cannot reliably preserve a pre-action prediction across days. A dedicated fifteenth technique offers clearer independent practice accounting but is premature without evidence of incremental product value. A Premortem extension is possible, but assumes failure while this method must permit either direction of evidence. Reflection is the closest existing home.

The Prediction Testing stage requires an additive Room table if implemented, plus history/export and reset integration. It proposes an explicit exception to the content style rule that every exercise finishes in one sitting: preparation and outcome review have separate estimates and no deadline. Preparation examples reuse existing notes; the breathing tool adds no persistent session or health data. BLUF has no implementation scope. All other product rules remain binding. The owner approved the extension-first scope and prototype work. Production storage, export and reset changes remain future work, subject to prototype approval. The prototype combines situation and prediction in one field and action with its limit in another; this reduces the number of questions while keeping the prospective comparison explicit.
