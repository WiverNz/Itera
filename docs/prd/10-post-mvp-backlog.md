# Post-MVP backlog

Not scheduled. Recorded so MVP scope arguments have somewhere to put things, and so the MVP's seams are justified.

## 1. Already seamed for

These have a deliberate hook in the MVP and need no rework to start.

| Item | Existing seam | Remaining work |
| --- | --- | --- |
| AI coach feedback on Feynman explanations | `CoachFeedbackProvider` + `NoOpCoachFeedbackProvider` (ADR-0016); the UI panel is written | A provider, a network layer, an `INTERNET` permission, a key story, a privacy update, an opt-in toggle |
| AI-assisted premortem analysis | same interface, different request type | as above |
| Adaptive spaced repetition (SM-2 style) | `ReviewScheduler.next()` is one pure function (ADR-0012) | Two additive columns, a new scheduler, and a design decision about what the five drawn ladder nodes then mean |
| Multi-module build | The package tree already matches (ADR-0001) | Build files and a convention plugin, when a split trigger fires |
| Russian localisation | Every string externalised, plurals declared, pseudo-locales verified | Translation and a review pass |
| More curriculum days | Day 15+ is generated; authored days would simply extend the curriculum asset | Content writing only |

## 2. Product candidates

| Item | Note |
| --- | --- |
| Home-screen widget showing today's one action | Strong fit with the product principle. Needs a Glance implementation and a second data path |
| Wear OS companion for the focus timer | The timer is the only feature that makes sense on a watch |
| Calendar integration for focus blocks | Read-only "when are you free?" first; writing events is a bigger commitment |
| Cloud backup and device sync | Requires accounts, which the MVP deliberately refuses. Would need a privacy model for user-authored text |
| Journal **import** | Pairs with export; makes the local-only stance survivable across devices |
| Custom techniques authored by the user | Attractive, but risks turning the app into a framework. Would need its own product thinking |
| Sharing a single reflection or result | Needs a rendered card and a decision about what is private |
| Richer analytics for the user (time-of-day patterns, technique effectiveness) | Must not become a dashboard. Would be one screen, at most |
| Onboarding variant for users who already know some techniques | "I already use Pomodoro" would skip its intro day |
| A second program track (e.g. a learning-focused curriculum) | The curriculum format already supports it; the UI would need a track picker |

## 3. Deliberately rejected

| Item | Reason |
| --- | --- |
| Streaks, XP, badges, leaderboards | Contradicts the product's central bet (A4 in `06-personas-and-assumptions.md`) |
| Accounts and login | No feature in the MVP or the near backlog needs identity |
| Social accountability, friends, groups | A different product |
| A task manager layer (projects, tags, due dates) | The schema deliberately has no entity for a task outside an exercise |
| In-app purchases or a subscription | Not until the product is proven |
| Crash/analytics SDKs | No network permission; Play Console vitals covers the need |
| Push notifications from a server | There is no server |
| Ads | No |

## 4. Technical debt accepted in the MVP

Recorded so it is a decision, not a surprise.

| Debt | Why accepted | Trigger to fix |
| --- | --- | --- |
| No paging on History | Bounded data for years | History exceeds ~2 000 rows |
| Hand-written fakes instead of a mocking framework | Faster, more readable tests | If fakes start carrying logic |
| `ArchitectureTest` as a regex over sources | Cheap, no new dependency | If it produces false positives |
| No baseline profile until issue 040 | Startup is already simple | If cold start exceeds 1.5 s |
| Progress recomputed on every read | Correctness over speed (ADR-0013) | If the 300 ms budget is missed |
| Combination days beyond Day 14 are generated, not authored | Content cost | If generated days feel arbitrary in testing |
| No cloud backup | A restored program day would corrupt state | When a proper sync model exists |
