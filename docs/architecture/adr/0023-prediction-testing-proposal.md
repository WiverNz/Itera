# ADR-0023: Bounded post-MVP extensions before new techniques or skills

Status: **proposed (2026-11-26), awaiting product review**. No implementation authorized by this ADR.

## Context

The user requested scientific research and a post-MVP design assessment of Behavioral Experiments, then extended the request to Environment Design, BLUF and breathing support. Existing activities expire at calendar rollover; Daily reflection stores three retrospective answers; review scheduling tests recall. None provides a durable prospective prediction/outcome pair. A fifteenth technique would also require changes to the fixed catalogue validator and curriculum assumptions. A sixth skill would affect onboarding, progress, content and combination rules.

## Proposed decision

Prioritize optional **Environment Design examples** within existing Habits techniques and reflection, with no new record type or progress credit. Next evaluate **Prediction Testing** as described below. Consider a **Breathing pause** only as a separately approved supporting tool before Focus: no holds in the first version, no practice credit or persistence, and foreground-only pacing that stops on interruption. **Defer BLUF and a Communication skill** until a coherent broader product case exists. Retain all five skills and fourteen techniques. No compulsory curriculum additions or new daily checklist items.

These stages are independently gated and unscheduled. The expanded proposal/milestone file owns their detailed boundaries; the old tentative Prediction Testing milestone number is no longer reserved. Extending this proposed ADR does not accept any of its decisions.

Evaluate **Prediction Testing** as an optional Daily reflection extension in the Reflection skill. Preserve all 14 techniques, existing mastery thresholds, curriculum days, generated combinations and review scheduling. Introduce a dedicated, resumable two-phase interaction backed by a local experiment record, while reusing existing UI components. No progress credit for preparing or merely reopening an experiment; the initial extension grants no additional mastery credit of its own.

The existing evening reflection can link to an experiment but does not require it. Completing the existing reflection continues to count once under current rules. Experiment records appear in history/export with a separate factual status, not as completed training activities. This keeps the Prediction Testing evaluation bounded and avoids inventing mastery rules before the distinct technique is justified.

Keep experiments outside the daily activity expiration lifecycle. Do not add a new global activity state or use the recall ladder for personal assumptions. No neuroscience claims, clinical treatment, AI, networking, automatic text interpretation or external voice recognition in the new fields.

## Alternatives and consequences

Full analysis, interaction rules, compatibility constraints and proposed acceptance criteria are in the [proposal](../../proposals/prediction-testing/proposal.md), [research](../../proposals/prediction-testing/research.md) and [draft milestone](../../proposals/prediction-testing/milestone.md).

A copy-only prompt is cheaper but cannot reliably preserve a pre-action prediction across days. A dedicated fifteenth technique offers clearer independent practice accounting but is premature without evidence of incremental product value. A Premortem extension is possible, but assumes failure while this method must permit either direction of evidence. Reflection is the closest existing home.

The Prediction Testing stage requires an additive Room table if implemented, plus history/export and reset integration. It proposes an explicit exception to the content style rule that every exercise finishes in one sitting: preparation and outcome review have separate estimates and no deadline. Preparation examples reuse existing notes; the breathing tool adds no persistent session or health data. BLUF has no implementation scope. All other product rules remain binding. Approval must settle each selected stage's scope, non-credit policy, duration copy, privacy restriction and discovery placement before implementation. Until then, existing accepted ADRs and specifications remain unchanged.
