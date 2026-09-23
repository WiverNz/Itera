# Coding guardrails

## Keep the app simple

Do not introduce a backend until a real requirement needs it.

Do not add authentication to MVP.

Do not create a generic task-management platform.

Do not make every technique a completely separate architecture.

Prefer shared primitives with specialized UI only where the technique benefits from it.

## Compose

- state flows down
- events flow up
- avoid business logic in composables
- keep screen state explicit
- prefer immutable UI state
- previews for reusable components and important screens
- avoid giant composables
- do not pass NavController deeply through the UI tree

## Domain

Keep scheduling, unlock, progress, and repetition logic testable without Android framework classes.

## Persistence

Room should be the source of truth for structured training history.

DataStore should hold lightweight preferences.

## Background work

Background jobs must be idempotent.

Do not assume exact alarm timing unless the product truly requires exact timing.

## AI readiness

Future AI feedback should be behind interfaces.

The MVP must remain useful if no AI provider is configured.
