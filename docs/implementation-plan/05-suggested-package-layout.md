# Suggested package layout

Claude should refine this, but the initial direction is:

com.example.productivitytrainer

- app
- core
  - designsystem
  - model
  - common
  - data
  - database
  - preferences
  - notifications
- feature
  - onboarding
  - today
  - exercise
  - focus
  - eisenhower
  - feynman
  - premortem
  - habitstacking
  - reflection
  - progress
  - library
  - history
  - settings
- domain
  - training
  - review
  - progress

A single app module with package boundaries is acceptable for the MVP.

Do not create multiple Gradle modules unless the documentation identifies a concrete benefit.

The implementation plan should state when modularization would become useful.
