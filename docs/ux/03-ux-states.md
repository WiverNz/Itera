# UX states

Every screen must define loading, empty, error and first-run behaviour. This document is the default; a screen spec overrides it only where it says so.

> **The prototype has none of these.** `design/` always has data, never fails and never waits, so loading, empty and error states are the main thing production adds on top of it. Build them from the prototype's own primitives - `surface2` blocks at the size of the content they replace, `IteraCard`, `body`/`bodySmall` type - so they look like they belong.

## 1. Loading

**Principle: never blank a screen that has chrome.** Headers, titles and navigation render immediately from state that is already known (the greeting, the tab labels, the month name). Only the data region shows a placeholder.

| Pattern | Use |
| --- | --- |
| **Skeleton blocks** | The default. `surfaceVariant` rectangles at the exact size of the content they replace, radius matching the real component. **No shimmer animation** - the design has no shimmer anywhere and it would fight the calm tone |
| **Nothing** | Where the data resolves in under one frame (preferences, catalog). Do not flash a skeleton for 16 ms |
| **Blocking progress** | Only for user-initiated destructive or long operations: reset, export. A centred dialog with a label naming the operation |
| **Spinner** | Never as a full-screen state. Only inside a button that is performing an action |

Skeletons appear only after **150 ms** of loading, to avoid a flash on fast paths.

## 2. Empty

Each empty state names the situation and, where an action exists, offers exactly one.

| Screen | Empty condition | Copy | Action |
| --- | --- | --- | --- |
| Today | impossible (a plan always has a reflection) | treat as error | - |
| Train review card | no reviews due | the card is absent, not empty | - |
| Library | impossible (14 techniques always) | - | - |
| Technique detail practice | never practised | "No practice logged yet." | the Practice button above |
| Progress | Day 1, nothing done | skills show "Starting" with empty bars and "No practice yet" | none - this is a legitimate first-day view, not an error |
| History month | a month with no training | "No training logged in {Month}." | none |
| History day | a day with no training | "Rest day. No training logged." | none |
| Learning topics | no topics | "No topics yet. Add something you're learning." | "Add a topic" |

**No empty-state illustrations.** The prototype has none, and the two empty states it does render - "Your practice" on technique detail, and the History log - are a single `bodySmall` `ink2` line. A line of text and the surrounding chrome is the pattern.

Rest days are shown, not hidden. That is a product decision: the calendar tells the truth and the copy never scolds.

## 3. Error

| Severity | Presentation | Example |
| --- | --- | --- |
| Screen cannot load | `ErrorState` replacing the data region only: a 48 dp muted icon, a one-line cause in `bodyMedium`, a "Try again" text button | "Couldn't build today's plan." |
| Action failed | `Snackbar`, 4 s, with an action where one makes sense | "Couldn't save. Try again." |
| One row is unreadable | the row renders with its title and "Couldn't read this entry" as its subtitle | a corrupt result payload |
| Environment | a specific message with a route to fix it | "Notifications are off for Itera" + "Open settings" |

Error copy rules: say what happened, never blame the user, never show an exception or a code, and where data is involved add "Your data is safe." Never use the word "Error" in user-facing text.

`ErrorState` and `EmptyState` are shared components in `core/designsystem/component`.

## 4. First run

| Surface | First-run behaviour |
| --- | --- |
| App launch | Splash held only until `onboarding_completed` resolves, then Welcome |
| Today, Day 1 | Full hero with the "New" badge; the checklist has three rows; no carry-over banner |
| Train | Week 1 header, Day 1 expanded, Days 2-7 future, no review card |
| Progress | The legitimate first-day view (section 2), one filled dot |
| History | The current month with one populated day |
| Library | All 14 shown, one unlocked (plus Daily reflection), thirteen locked with day chips |
| You | Values as chosen in onboarding; "Training since {today} - Day 1" |

No coach marks, no tooltips, no feature tour. Onboarding is the tour.

## 5. Day-complete

| Surface | Behaviour |
| --- | --- |
| Day complete screen (`a12`) | Night surface. 3/3 chip, "Day {n} complete", the summary line, a card echoing the user's "change for tomorrow", and a preview card "Tomorrow - Day {n+1}: {technique}. Unlocks at {morning time}". Primary: "Good night" |
| Today afterwards | The hero is replaced by the quiet completion card (`02-screen-specs-today.md` section 7) |
| Notifications | No further nudges that day; the evening reminder is cancelled on completion |

If the user completes the day and then opens the app again the same evening, they get the quiet Today. The app does not manufacture more work.

## 6. Offline

There is no offline state, because there is no online. No screen shows a connectivity banner, a retry-because-network message, or a sync indicator. If one appears in a review, it is a bug.

## 7. Permission-denied

| Permission | Denied behaviour |
| --- | --- |
| `POST_NOTIFICATIONS` | Every feature still works. Settings toggles read off and disabled with a route to system settings. The focus timer runs without a notification and shows a one-time inline note that it may be interrupted in the background |

No permission is required for the app to function. None is requested outside onboarding and the first focus session.

## 8. Process death

Every screen re-derives from Room or DataStore. Drafts are in `plan_activity.draftPayload`; the timer is in its own DataStore. Restoring mid-exercise returns the user to the same route with their text intact. There is no state that is lost, and therefore no "restoring..." UI.

## 9. Large font and small screens

At `fontScale = 2.0` every screen scrolls rather than clipping. Specific accommodations:

- Today's hero title drops to `displayMedium` above `fontScale 1.5`;
- the `SegmentedControl` wraps to a vertical stack above `fontScale 1.6`;
- the Eisenhower matrix keeps its 2x2 shape and scrolls internally;
- the focus timer readout scales but is capped so the ring never clips.

## 10. Reduced motion

The prototype does not handle this; production adds it. When `ANIMATOR_DURATION_SCALE == 0`, every animation in `docs/ux/04-design-system.md` section 7 renders at its **end state**:

| Animation | End state |
| --- | --- |
| `StepDot` done check | drawn, full size |
| `StepDot` now pulse | static dot at full opacity |
| `MasteryLadder`, `ProgressBar`, skill bars | filled to the target |
| `AnimatedCheck` | circle at full scale, check fully drawn |
| `DayRing` | all earned arcs drawn |
| Focus ring | at the current progress |
| Welcome `RisingPath` | all five nodes at full scale |
| Reflection step transition | the new step in place, no slide |
| `animateContentSize` | resized immediately |

No transition is merely shortened - it is removed. A `LocalReduceMotion` flag is read once at the theme root.
