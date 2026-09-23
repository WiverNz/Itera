# Product brief

## Name

**Itera**

(The bootstrap documents and the design artboards use the working name "Productivity Trainer". Itera is the product name; see `docs/00-source-of-truth.md` section 2.)

## Product category

Guided productivity training / personal skill development.

## Problem

Most productivity apps help users store tasks, notes, or habits, but they do not teach users how to improve focus, planning, learning, and reflection.

Productivity advice is consumed passively. Users read about Pomodoro, Eisenhower, Deep Work or the Feynman technique but never build a repeatable practice around them.

## Product hypothesis

Users internalise productivity techniques when the application:

- introduces them one at a time,
- gives a concrete exercise to do in real life,
- records a lightweight result,
- returns to the technique later,
- and combines techniques only after each has been practised alone.

## Primary experience

The user opens the app and sees one clear recommended action.

> **Today's training** - New
> **2-minute rule**
> Clear small tasks before they accumulate.
> About 5 min - Habits
> [ Start today's exercise ]

The application minimises decision fatigue. The home screen answers "what should I do right now?" within a few seconds.

## The daily loop

Three touchpoints, each a single clear action:

| When | What |
| --- | --- |
| Morning (08:30 by default) | One exercise from one technique. A short why, then a concrete action in real life. Mark the result: easy / okay / hard, plus an optional note. |
| Daytime (optional) | One focus suggestion - a Pomodoro or Deep Work block sized to the user's time budget. Never more than one nudge. |
| Evening (21:00 by default) | A two-minute reflection: what went well, what didn't, what changes tomorrow. |
| Overnight | The coach plans tomorrow: a new technique, a review, or a combination. Last night's "change" appears at the top of the next Today. |

## The four tabs

| Tab | Question it answers |
| --- | --- |
| **Today** | What should I do right now? |
| **Train** | What am I learning? |
| **Progress** | Am I actually getting better? |
| **You** | How does the coach fit my life? |

Today is the default and the only tab that pushes. The others are pulled on demand.

## Core training domains (skills)

Focus, Planning, Learning, Habits, Reflection. Each technique feeds exactly one skill.

## The 14 techniques

| Skill | Techniques |
| --- | --- |
| Focus | 5-second rule, Pomodoro, Deep Work, Information diet |
| Planning | Eisenhower matrix, 80/20 principle, Two-list strategy |
| Learning | Feynman technique, Spaced repetition |
| Habits | 2-minute rule, Habit stacking, 1% improvement |
| Reflection | Daily reflection, Premortem |

## Progression

**Techniques** progress through four honest steps, each earned by real use: **Met** (did the intro once) -> **Practiced** (used on 3 different days) -> **Applied** (6+ uses across 2+ weeks) -> **Integrated** (used inside combination days).

**Skills** show a level and a regularity, not a points total: **Starting -> Building -> Steady -> Strong**.

**What counts**: finished exercises, completed focus minutes, reviews done, days practised out of the last 14.
**What never counts**: opening the app, taps, streak freezes, points for logging in.

## Product values

- practical over theoretical
- calm over addictive
- focused over feature-heavy
- real practice over cosmetic gamification
- progressive learning over information overload
- honest over flattering - rest days are shown, not hidden
- offline-capable by default

## The anti-streak stance

Program days advance **when the user trains**, not by calendar. Missing a day never resets anything, never shows a warning colour, and never produces a "you broke your streak" message. This is a deliberate differentiator and is enforced in the data model (`docs/engine/01-training-plan-engine.md`) and by a UI test.

## Who this is not for

Someone looking for a task manager, a note app, a habit tracker with charts, or a social accountability product. Itera stores only what a training log needs. It will not grow a project hierarchy, tags, or collaboration.
