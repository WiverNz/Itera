# MVP scope

## MVP objective

A user can:

1. complete onboarding (welcome + 3 steps),
2. receive a daily guided exercise,
3. complete it and record a result,
4. use the specialised tools for the techniques that need them,
5. complete a two-minute evening reflection and close the day,
6. receive spaced-repetition reviews when due,
7. see honest progress across five skills and 14 techniques,
8. receive local reminders,
9. export their journal,
10. use the whole app in English, Russian, German or Spanish, switching at any time,
11. do all of it with no network connection, no account and no subscription.

## Technique coverage

All 14 techniques exist in the content model and are all reachable in the first release.

### Dedicated interactive experiences

| Technique | Screen |
| --- | --- |
| Pomodoro, Deep Work | Focus timer |
| Eisenhower matrix | 2x2 sorter |
| Feynman technique | Explain + reflect |
| Premortem | Failure-reasons + mitigation |
| Habit stacking | Anchor/habit builder |
| Daily reflection | Three-question flow |
| Spaced repetition | Review screen |
| (combination days) | Combination runner |

### Template-driven experiences

5-second rule, 2-minute rule, Information diet, 1% improvement, 80/20 principle, Two-list strategy - all rendered by the data-driven template body (ADR-0007). The 2-minute rule uses the stopwatch checklist variant.

## MVP screens

24 screens, each existing as a running composable in `design/`, listed with their prototype file in `docs/ux/00-screen-inventory.md` and `docs/ux/05-prototype-reference.md`:

onboarding (4) - Today - exercise intro / run / result - focus timer - evening reflection - day complete - combination - Eisenhower - Feynman (2) - review - premortem - habit stacking - Train - library - technique detail - Progress - history - You.

Plus the supporting surfaces the design does not draw (pickers, topic editor, privacy note, reset dialogs), listed in the same document.

## Explicit non-goals for the MVP

- login, accounts, cloud sync, any backend
- web or desktop app
- social features, leaderboards, sharing achievements
- subscriptions or any monetisation
- collaborative tasks
- a general task manager (no projects, tags, due dates, or recurring todos)
- calendar integration
- any workflow that requires an AI provider
- wearable or widget surfaces
- crash-reporting or analytics SDKs

## In scope but easy to mistake for out of scope

| Item | Why it is in |
| --- | --- |
| Four languages (en, ru, de, es) | Explicit requirement; the `design/` prototype already ships all four catalogues (D-03) |
| Journal export (Markdown) | Drawn in the You tab; local-only; one small issue (R-09) |
| Combination days | Drawn, and they are the mechanism that earns `INTEGRATED` (R-15) |
| A local display name | Drawn in the You tab; optional, never required, never leaves the device |
| The `CoachFeedbackProvider` interface | One file; the seam that keeps the Feynman feature from needing rework later (ADR-0016) |

## Out of scope but partially built

| Item | What ships |
| --- | --- |
| AI coach feedback | The interface and a no-op binding. The UI panel exists but is unreachable. The settings row is visible and disabled |
| Adaptive review scheduling | `ReviewScheduler.next()` is a single pure function, ready to be replaced |
| Multi-module build | The package tree already matches the eventual module tree |
| A fifth language | Adding one is four small edits (`docs/i18n/00-localization.md` section 10) |

## Post-MVP candidates

See `10-post-mvp-backlog.md`.

## MVP exit criteria

The MVP is done when `docs/prd/09-mvp-acceptance-criteria.md` passes in full, the manual QA checklist passes on two devices, and issue 040's release checklist is complete.
