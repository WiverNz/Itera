# Content writing guidelines

Voice, terminology and the review checklist live in `docs/ux/09-copy-deck.md`. This document is for the person writing or reviewing **technique content**.

> **The 14 techniques are already written, in four languages.** `design/app/src/main/res/values*/strings.xml` holds them. This guide governs review of that content and the writing of anything new (D-04).

## 1. The four strings

| String | Length | Job | Example (2-minute rule) |
| --- | --- | --- | --- |
| `t_{slug}_name` | 1-3 words | The technique's common name | 2-minute rule |
| `t_{slug}_short` | under 60 chars, one sentence | What it does - library row, hero subtitle, detail subtitle | Clear small tasks before they accumulate. |
| `t_{slug}_why` | 2-3 sentences | The framing on the intro screen **and** "Why it helps" on detail. Explains the mechanism, not the benefit | A timer turns "work on it" into a small unit you can finish. Short breaks keep attention fresh, and counting sprints shows how long tasks really take. |
| `t_{slug}_task` | 1-2 sentences, imperative | The actual thing to do today, inside the "Your exercise" card. Must be doable in the stated minutes | Look at your current to-dos. Find two tasks that take under two minutes - and finish them now. |

There is no per-technique example, result headline or result body. The result screen uses one generic headline and subtitle for every technique, so `_why` and `_task` carry the whole idea.

## 2. Rules for instructions

The instruction is the product. Everything else is packaging.

- **Concrete**: name what to look at and what to produce. "Find two tasks" not "handle small tasks".
- **Bounded**: it must finish inside the estimated minutes. If it cannot, shorten the exercise, not the estimate.
- **About real life**: the user's actual to-dos, their actual project, the thing they are actually learning. Never a hypothetical.
- **Single**: one instruction per exercise. If it needs "and then", it is a combination day.
- **Verifiable by the user**: they should know when they are done without asking the app.

## 3. Rules for explanations

- Explain the **mechanism**, not the benefit. "A timer turns 'work on it' into a small unit you can finish" beats "boosts your productivity".
- No research citations, no "studies show". The app is not making claims it cannot support.
- No origin stories. Who invented it is not why it works.
- Two or three sentences. Someone reading it on Day 2 has 20 seconds.

## 4. Rules for result copy

- State the fact first ("Two small things, done"), then the idea.
- Never congratulate the person ("Great work!", "You're on fire"). Congratulate nothing; describe what happened.
- The result body should be worth re-reading on Day 30. It is the line that carries the technique after the exercise is forgotten.

## 5. Suggestion chips

Where a screen offers chips (reflection, Feynman hardest-part, habit anchors), the options must be:

- **plausible for most people** - "Checked email first", not "Missed my 5am block";
- **specific enough to be honest** - "Got distracted" is weak but real; "Suboptimal execution" is neither;
- **four to six options** - more becomes a survey;
- **always escapable** - a free-text field is always available beside them.

## 6. Combination step prompts

Each step in a combination needs a `prompt` (the question) and a `hint` (how to answer it in one move):

| Step | Prompt | Hint |
| --- | --- | --- |
| Eisenhower | Most important right now? | Sort what's on your plate, then pick from Do now. |
| 80/20 | Which part moves it most? | Pick the one step that gives most of the result. |
| Deep Work | 50 minutes on that one step | Phone away, one tab open. |
| Daily reflection | Tonight: did the 20% hold? | Part of your evening reflection. |

## 7. Notification copy

Covered in `docs/ux/08-notification-ux.md` section 5. Summary: under 60 characters, no exact times, no counts, no exclamation marks, a question for optional things and a statement for scheduled ones.

## 8. Review checklist for technique content

- [ ] Can someone do the instruction today, in the stated minutes, with only their own work in front of them?
- [ ] Does the explanation describe a mechanism?
- [ ] Is the result body worth reading twice?
- [ ] Does any string praise the user? (Remove it.)
- [ ] Does any string mention days in a row, points, or being behind? (Remove it.)
- [ ] Is every string under its length budget at `fontScale 2.0` without clipping?
- [ ] Are all four keys present, in **all four languages**? (`CatalogValidationTest` will fail otherwise.)
- [ ] Does the German or Russian version clip any layout it appears in?
