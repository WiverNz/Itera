# Core user flows

## Flow 1 - First launch

1. Launch. Splash resolves `onboarding_completed = false`.
2. **Welcome**: value proposition, "Get started".
3. **Step 1 of 3 - Goals**: pick up to two of Focus, Planning, Learning, Habits, Reflection. Selecting a third drops the oldest. Zero selections is allowed.
4. **Step 2 of 3 - Rhythm**: morning time (08:30), evening time (21:00), daily budget (5 / 15 / 30+ min, default 15). Notification permission requested here on Android 13+.
5. **Step 3 of 3 - First week**: a read-only preview of Days 1-7 from the curriculum, plus "Every evening: a two-minute reflection".
6. "Start Day 1" writes preferences, seeds unlocks, generates the plan, schedules reminders, and lands on Today.

No name, no email, no account, no permissions beyond notifications during onboarding. Microphone permission is requested only on later explicit voice use (milestone 012).

## Flow 2 - Morning training

1. Open the app (or tap the morning notification).
2. Today shows one hero action: the day's technique, with a "New" badge the first time.
3. Tap "Start today's exercise" -> **Intro**: why it works, then the concrete exercise, with an example.
4. Tap "Start exercise" -> the technique's body.
5. Do the thing in real life; record what the body asks for.
6. Tap "Finish exercise" -> **Result**: a headline, "How did it feel?" (Easy / Okay / Hard), an optional note, and the mastery ladder showing what just changed and what is next.
7. "Done" returns to Today with the checklist row filled and the counter advanced.

Alternative at step 3: "Not now - remind me at 12:00" snoozes the activity and schedules one reminder.

## Flow 3 - Focus session

1. From Today's focus suggestion, Technique detail's "Practice now", or a combination day's focus step.
2. A pre-timer sheet asks for the one task and the length (seeded from the technique: 25 min Pomodoro, 50 min Deep Work).
3. The timer runs on a dark, near-empty screen: a ring, the time, the task, three controls.
4. Itera's own reminders are suppressed for the duration.
5. Pause / resume / +5 min / End are available. The session survives backgrounding and process death.
6. At zero (or on End), the result screen records the session and shows the break hint.

## Flow 4 - Evening reflection

1. The evening notification arrives, or the user taps the reflection row.
2. **Question 1** - "What went well today?" is **pre-filled** from what was logged: completed exercises and focus sessions are summarised, and the user can edit it.
3. **Question 2** - "What didn't go well?" offers suggestion chips (Started late, Checked email first, Got distracted, Ran out of time) plus a one-line field.
4. **Question 3** - "What will you change tomorrow?" - one line.
5. "Next" -> **Day complete**: 3/3, the day's summary, the change echoed back with "It'll be waiting on tomorrow's Today screen", and a preview of tomorrow.
6. "Good night" returns to a quiet Today.

"Skip tonight" is always available and records the reflection as skipped - honestly, without penalty.

## Flow 5 - Spaced repetition

1. A Feynman explanation on Day 6 creates a review item due Day 7 (stage 0).
2. When due, the review appears as a row on Today and as a card on Train.
3. The review screen asks for the explanation again, **from memory**; the previous answer is hidden.
4. "Compare with my first answer" reveals both side by side.
5. The user grades recall: couldn't recall / roughly, with gaps / I had it.
6. The ladder advances (1 -> 4 -> 9 -> 21 -> 60 days), repeats, or restarts.

## Flow 6 - Browse a technique

1. Train -> Library (or a tab tap from anywhere in Train).
2. All 14 are listed, filterable by skill. Locked ones show the day they unlock and can still be opened.
3. Technique detail shows why it helps, the mastery ladder with the next step, recent practice with notes, and related techniques.
4. Unlocked techniques offer "Practice now", which creates a manual activity and opens it immediately.

## Flow 7 - A combination day (Day 14 onwards)

1. Today's hero is a combination rather than a single technique.
2. The runner shows a chain of steps: Eisenhower (pick what matters) -> 80/20 (pick the one step) -> Deep Work (50 minutes on it) -> tonight's reflection.
3. Each step is completed in turn; earlier steps collapse to their result.
4. Every technique used earns `INTEGRATED`.

## Flow 8 - Missing days

1. The user does not open the app for five days.
2. On return, yesterday's unfinished activities are `EXPIRED`; the day is `ABANDONED`; `programDay` did **not** advance.
3. Today offers the same curriculum day that was missed.
4. Progress shows the untrained days as hollow dots with the standing reassurance line.
5. Overdue reviews are due once, sorted first, capped per day by pace.
6. Nothing is lost, nothing is reset, and no message mentions the gap.

## Flow 9 - Changing the rhythm

1. You -> Daily rhythm -> Morning training.
2. A time picker sets a new time.
3. Reminders are rescheduled immediately; today's reflection row updates its subtitle in place.
4. The day's plan is **not** regenerated - the plan is stable once generated.

## Flow 10 - Starting over

1. You -> Reset program. A dialog names exactly what is deleted and what is kept.
2. On confirm: training history, sessions, reflections and reviews are deleted; unlocks are cleared; the program returns to Day 1. Settings and learning topics survive.
3. "Erase everything" additionally removes topics, habit stacks, the event log and all preferences, returning the app to first run.
