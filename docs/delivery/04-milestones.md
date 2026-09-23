# Release milestones

The implementation issues are themselves milestones (`docs/issues/README.md`). For review and demos they group into four checkpoints.

| Checkpoint | Issues | Demonstrates | Exit |
| --- | --- | --- | --- |
| M1 - Skeleton | 001-004 | Builds, themed shell, persistence, catalog, full engine | `./gradlew build` green; catalog validates; `ArchitectureTest` and the engine's worked plan examples pass |
| M2 - The loop runs | 005-006 | Onboarding, Days 1-14 with real exercises, reflection, day complete | PRD acceptance groups A, B and D1-D5 pass; process death mid-exercise restores the draft; `programDay` advances exactly once per completed day |
| M3 - Feature complete | 007-009 | Every screen, reminders, settings, export, four languages, accessibility | Every screen in `docs/ux/00-screen-inventory.md` exists; acceptance groups C-H pass; no feature needs a network |
| M4 - Ready to ship | 010-011 | Tested and releasable | `docs/prd/09-mvp-acceptance-criteria.md`, `docs/testing/02-manual-qa-checklist.md` (two devices) and `docs/delivery/03-release-checklist.md` complete |

## Risks

- **Plan generator** (004) is the most rule-dense code; review it against `docs/engine/01-training-plan-engine.md` line by line.
- **Focus timer** (006) is the most device-dependent; test process death on the oldest supported device during 006.
- **R8** breaking serialization, Hilt or the AppCompat locale service; 009 smoke-tests a minified build before 011.
- **Goldens recorded too early**; they are recorded in 010, after 009 stops moving layouts.

There is no "subset of techniques" milestone: the combination days need the first week's techniques to exist.
