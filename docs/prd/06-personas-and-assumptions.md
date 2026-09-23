# Target users and assumptions

Not market research - a written record of who the product is for, so scope arguments have a reference point.

## 1. Primary user

**Someone who already reads about productivity and has not changed anything.**

- Has heard of Pomodoro, has tried it twice, does not use it.
- Has a task app and a notes app; both are fine and neither taught them anything.
- Wants to be better at focusing and deciding, not better at recording.
- Has 10-15 minutes a day, most days. Not every day.
- Works knowledge-ish hours with interruptions they can partly control.

What they need: someone to say *do this specific thing today*, and to come back tomorrow whether or not yesterday went well.

What breaks them: a dashboard, a backlog, a streak counter, a setup wizard, anything that becomes a second job.

## 2. Secondary user

**A student or self-directed learner** who wants the Feynman technique and spaced repetition in a form they will actually do. They care most about Learning; the Focus and Planning techniques are welcome extras.

## 3. Explicitly not the user

| Not for | Why |
| --- | --- |
| Someone looking for a task manager | Itera stores practice, not work |
| A team wanting shared accountability | No accounts, no social layer, ever in the MVP |
| Someone who wants analytics on their time | No time tracking beyond focus minutes |
| Someone who wants to be gamified into compliance | The product's stance is the opposite |

## 4. Assumptions

Stated so they can be challenged rather than assumed silently.

| # | Assumption | If wrong |
| --- | --- | --- |
| A1 | One technique a day is the right pace | Pace setting already exists (Gentle / Standard / Intense); the curriculum would need re-ordering, not rebuilding |
| A2 | Users will do the exercise in real life, not just tap through | Completion rules require real input (two ticked tasks, 30+ words), but nothing can force honesty. Accepted |
| A3 | A two-minute reflection is short enough to sustain | Chips and pre-fill reduce it further; "Skip tonight" keeps it guilt-free |
| A4 | Removing streaks does not remove motivation | The mastery ladder and the skill bars carry progression instead. This is the product's central bet |
| A5 | 14 days of authored curriculum is enough before generated combinations | Day 15+ generation is specified; if it feels thin, authoring more days is content work, not engineering |
| A6 | Users accept notifications arriving up to ~45 min late | Copy avoids stating times. If users complain, exact alarms are an option with a permission cost |
| A7 | Local-only storage is acceptable - no backup, no device transfer via cloud | Device-to-device transfer is enabled; cloud backup is deliberately off (`docs/data/01-room-schema.md` section 8). Post-MVP export/import exists |
| A8 | English first, Russian later, is the right order | Everything is externalised, so the order is cheap to change |

## 5. Success signals (not KPIs)

The MVP has no analytics backend and cannot measure these remotely. They are what internal testers should be asked about:

- Does the Today screen answer "what now?" without thinking? (time-to-first-action)
- Do testers do the exercise *away from the phone*, or only in the app?
- After a missed week, do they come back? Does the app make that easy or awkward?
- Can they say what any technique is for, a week later, without opening the app?
- Does anyone ask for a streak? (If yes, understand why before adding one.)

## 6. Anti-goals

| Anti-goal | Guard |
| --- | --- |
| Becoming a task manager | No entity in the schema represents a task outside an exercise |
| Becoming a dashboard | Progress has five bars, ten dots and one sentence - nothing configurable |
| Optimising for daily active use | The app deliberately produces no work once the day is done |
| Growing settings | Every setting maps to a decision the engine actually makes |
