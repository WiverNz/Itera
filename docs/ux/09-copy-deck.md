# Copy

**The copy exists.** `design/app/src/main/res/values*/strings.xml` holds 362 strings and 4 plurals in English, Russian, German and Spanish. That catalogue is the source; this document is the style guide that governs it and anything added to it.

Do not rewrite existing copy while porting. If a string reads badly, change it in `design/` and in all four locales, in the same change.

## 1. Voice

| Do | Don't |
| --- | --- |
| Speak plainly, in second person | Use jargon, or the word "user" |
| Be specific: "Find two tasks that take under two minutes" | Be vague: "Try to be productive" |
| Be calm and short | Use exclamation marks, ever |
| State facts about the user's practice | Praise effort generically ("Great job!") |
| Say what happens next | Create urgency or fear of loss |
| Admit rest days exist | Call a missed day a failure |

Sentence case everywhere except technique names. No title case headings. No emoji.

The existing catalogue is the reference tone. When writing a new string, find a neighbouring one and match it: "That's the whole idea: finish it before it becomes something to remember."

## 2. Naming

| Concept | Term | Never |
| --- | --- | --- |
| The app | Itera | Productivity Trainer |
| A day's work | today's training | your tasks, your goals |
| One practice | exercise | lesson, module, quest |
| The 14 | techniques | skills, hacks, tools |
| The five | skills | categories, areas, stats |
| A repeat | review | test, quiz, exam |
| Mastery steps | Met, Practiced, Applied, Integrated | level 1-4, beginner/expert |
| Skill bands | Starting, Building, Steady, Strong | novice, master, rank |
| The evening entry | reflection | journal entry, diary |

These are already the terms in all four catalogues, including the translations. Changing one means changing four files.

## 3. Key scheme

Prefix by area. The full prefix table, with counts, is in `docs/i18n/00-localization.md` section 5. A new string goes in its area's prefix; a new area needs a new prefix and a note in that table.

Technique content is four keys each - `t_{slug}_name`, `_short`, `_why`, `_task` (D-04). Writing guidance in `docs/prd/08-content-style-guide.md`.

## 4. Rules that are enforced

Each of these is a test, not a preference (`docs/testing/01-test-matrix.md`).

- No hard-coded user-facing string in a composable. `HardcodedText` lint is an **error**.
- Every key in `values/` exists in `values-ru`, `values-de` and `values-es`. `MissingTranslation` is an **error**.
- Every count-bearing string is a plural, with the categories its language requires - Russian needs `one`/`few`/`many`/`other`.
- No string is assembled by concatenation. Every substitution is a positional argument (`%1$s`, `%2$d`).
- No string contains "streak", "in a row", "don't break", "XP", "points" or "% complete", in any language (`NoStreakLanguageTest`).
- Notification bodies are under 60 characters **in every language**.

## 5. Writing a new string

1. Pick the right prefix.
2. Write it in English, matching the neighbouring tone.
3. Check it against the review list below.
4. Translate to ru, de and es - or mark it for translation, but never ship a key that exists in only one locale.
5. If it carries a count, make it a plural in all four.
6. Render the screen in German and Russian and confirm nothing clips.

## 6. Review list

- [ ] Sentence case, no exclamation marks, no emoji.
- [ ] No count of days in a row, no points, no percentage complete.
- [ ] No blame for a missed day.
- [ ] Specific enough to act on without re-reading.
- [ ] Under 60 characters for a notification body, in every language.
- [ ] Positional format arguments, no concatenation.
- [ ] Plurals declared for every language.
- [ ] Present in all four locales.
