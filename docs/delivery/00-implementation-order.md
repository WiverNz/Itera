# Implementation order

Eleven milestones ([milestone index](../issues/README.md)). Current implementation status and acceptance gaps live in that index; the table below describes the dependency order and exit conditions, not completion status. The previous 40-issue ordering is preserved in `docs/history/issues-detailed/`.

**Rule: do not start a milestone until every milestone in its `Depends on` list is done.** If a decision the documentation does not cover is needed, stop and update `docs/00-source-of-truth.md` first.

**Rule: for any UI work, run `design/` first** and open the prototype files the milestone names (`docs/ux/05-prototype-reference.md`).

| # | Milestone | Depends on | Exit - the app can |
| --- | --- | --- | --- |
| 001 | Project bootstrap (complete) | - | Build, launch a themed placeholder |
| 002 | Core architecture & storage | 001 | Open and seed the database, round-trip preferences, record events |
| 003 | Design system & app shell | 002 | Navigate four placeholder tabs with real components |
| 004 | Content & training engine | 002 | Run the full daily loop in unit tests under a fake clock |
| 005 | Onboarding & daily flow | 003, 004 | Onboard and show a real Today, reflection and day complete |
| 006 | Exercise system | 005 | Complete Days 1-14 with every technique's real experience |
| 007 | Train & technique library | 006 | Browse the curriculum and every technique |
| 008 | Progress, history & settings | 005 | Show progress and history, configure, switch language, export, reset |
| 009 | Notifications & quality passes | 006, 007, 008 | Remind; pass the accessibility and localisation sweeps |
| 010 | Test hardening | 009 | Pass the full test matrix, goldens and instrumented CI |
| 011 | Release readiness | 010 | Be released internally |

Recommended solo sequence: `002, 003, 004, 005, 006, 007, 008, 009, 010, 011`. 003 and 004 are interchangeable; 007 and 008 are interchangeable.

Verification scope per milestone is defined in `docs/issues/README.md` ("Verification scope"): build + lint + tests for the changed area during 002-008; exhaustive passes in 009-011.
