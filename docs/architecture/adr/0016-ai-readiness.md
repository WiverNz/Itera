# ADR-0016: AI coach behind a provider interface, with the slot rendered

Status: accepted (2026-09-23; revises the 2026-09-22 version)

## Context

The Feynman exercise has a place for coach feedback. The product forbids an AI dependency in the MVP and requires the feature to be useful without one.

The original decision hid the panel entirely. The `design/` prototype instead **renders** it, as a dashed-border box with a `Spark` icon, a "Coming later" pill and a disabled action - a deliberate, honest placeholder rather than a hidden feature (D-10).

## Decision

```kotlin
interface CoachFeedbackProvider {
    val isAvailable: Boolean
    suspend fun feedback(request: CoachFeedbackRequest): Result<CoachFeedback>
}
```

`NoOpCoachFeedbackProvider` (`isAvailable = false`) is the only implementation shipped, bound in `CoachModule`.

The Feynman reflect screen ships the **self-assessment** variant - "What part was hardest to explain?" with chips and a note - which is the real exercise and stands alone.

Below it, the prototype's dashed coach box **is rendered**: same border, corner radius 24, dashed stroke, `Spark` icon, "Coach feedback" header, "Coming later" pill, and a disabled secondary button.

**One deviation.** The prototype fills that box with three rows of sample coaching text ("Clear", "Gap to check", "A beginner might ask") whose own source comment says the content is sample. Shipping fabricated feedback that reads as though a coach produced it would mislead a user about what the app did. Production keeps the box and its chrome and replaces those three rows with **one** explanatory line saying that written feedback on explanations is coming. When a provider exists, the rows render real output.

The Settings row for the feature is present and disabled with a "Coming later" caption.

## Alternatives considered

- **Hide the panel entirely** (the previous decision) - the prototype shows it, and a visible, clearly-labelled placeholder sets an honest expectation. Reversed.
- **Ship the sample rows as drawn** - presents invented text as product output. Rejected; recorded as open question Q-01 in case the product owner disagrees.
- **Omit the interface** - the seam would have to be retrofitted through the Feynman feature later, for the cost of one file. Rejected.
- **Ship a real provider behind a flag** - needs a network layer, a key and a privacy story the MVP does not have. Rejected.

## Consequences

- The MVP is fully useful with no provider configured.
- The Feynman exercise's value does not depend on AI: self-explanation plus a scheduled review *is* the technique.
- Adding a provider later touches `CoachModule`, a settings toggle, a network layer and the three feedback rows. No other feature code.
- The MVP manifest declares no `INTERNET` permission, so adding one is an explicit, reviewable change.

## Migration implications

None.
