# Journal export format

Implements the "Export journal - Markdown" row in the You tab (R-09).

## 1. Behaviour

1. User taps **Export journal**.
2. A bottom sheet offers a range: **Last 30 days / Last 12 months / Everything**, and a destination: **Share** or **Save to a file**.
3. `ExportJournalUseCase` renders Markdown to a file in `context.cacheDir/export/`.
4. **Share** launches `ACTION_SEND` with a `FileProvider` URI and `text/markdown`. **Save** launches `ACTION_CREATE_DOCUMENT` (SAF) and copies the file to the chosen location.
5. The cache file is deleted after the intent resolves or after 24 hours by `EventLogTrimWorker`.

No network, no account, no cloud. The file never leaves the device unless the user picks a target.

Filename: `itera-journal-YYYY-MM-DD.md`.

## 2. Format

```markdown
# Itera journal

Exported 2026-09-22 - Days 1-9 - 41 activities

---

## Tuesday, 22 September 2026 - Day 9

**80/20 principle** - Planning - 10 min - felt Okay
> Picked "define the 3 bets" over collecting requests. Obvious in hindsight.

**Review: Redis persistence** - Learning - 5 min - recall: Roughly, with gaps
> Snapshots vs the append log. Forgot what happens to writes after the last snapshot.

**Evening reflection**
- Went well: Protected the morning block.
- Didn't go well: Checked email first.
- Tomorrow: Start the focus session before opening email.

---

## Monday, 21 September 2026 - Day 8

**Deep Work** - Focus - 50 min of 50 planned
> Started rough, last 20 minutes were great.

...

---

## Techniques

| Technique | Skill | Level | Uses | Days | First | Last |
| --- | --- | --- | --- | --- | --- | --- |
| Pomodoro | Focus | Applied | 6 | 4 | 2026-09-14 | 2026-09-21 |
| 2-minute rule | Habits | Applied | 8 | 6 | 2026-09-13 | 2026-09-22 |
...
```

Rules:

- Newest day first.
- A day with no training renders as `## Saturday, 19 September 2026 - no training logged` with no body. Rest days are shown honestly, not hidden.
- User-authored text is emitted verbatim inside a block quote, with Markdown special characters escaped at the start of a line (`>`, `#`, `-`, `|`, backtick).
- Skipped activities render as `**{name}** - skipped`.
- The techniques table is always last and always complete, including techniques at level `MET`.
- Locked techniques are omitted.

## 3. Implementation

`data/export/MarkdownJournalWriter.kt`, a pure function:

```kotlin
fun render(
    days: List<JournalDay>,
    techniques: List<TechniqueProgress>,
    exportedOn: LocalDate,
    locale: Locale
): String
```

It takes already-loaded data and returns a string, so it is unit-testable with golden-file assertions and no Android dependency. The use case handles I/O.

Large exports are written incrementally through a `BufferedWriter` rather than building one `String`; the pure `render` overload is used for tests and small ranges, and a streaming `render(writer, ...)` overload is used in production.

## 4. Localisation

Dates and the day-of-week use the device locale. Section labels ("Went well", "Tomorrow", "Techniques") come from string resources. Technique names come from the catalog's localised strings. The user's own text is never transformed.

## 5. Privacy

The export contains **everything the user typed**. The share sheet is preceded by a one-line note in the bottom sheet: "This file contains your notes and reflections." No warning dialog - the user asked for their journal - but the statement is present so the content is never a surprise.

## 6. Tests

- Golden-file test for a fixture covering: a normal day, a rest day, a skipped activity, a focus session ended early, a review, a reflection with only one of three answers filled, and text containing `|`, `#` and a newline.
- Empty-database export produces a valid file with a header, no day sections and an empty techniques table.
- An export of 2 years of data completes within 3 seconds and does not exceed 50 MB of heap.
