# 035 - Journal export

**Phase** 6 - Infrastructure | **Depends on** 013, 034 | **Blocks** 039

## Goal

Implement Markdown journal export per `docs/data/03-journal-export-format.md`.

## User value

Your notes and reflections are yours, in a plain file you can read anywhere.

## Scope

- The export bottom sheet: a range selector (Last 30 days / Last 12 months / Everything), a destination (Share / Save to a file), and the note "This file contains your notes and reflections."
- `MarkdownJournalWriter`: a pure `render(...)` for tests and small ranges, plus a streaming `render(writer, ...)` for production.
- `ExportJournalUseCase`: loads the range, renders to `cacheDir/export/itera-journal-YYYY-MM-DD.md`, and returns the file.
- Share via `ACTION_SEND` with a `FileProvider` URI and `text/markdown`.
- Save via `ACTION_CREATE_DOCUMENT` (SAF) with a copy.
- Cache cleanup after the intent resolves, plus a 24-hour sweep in `EventLogTrimWorker`.
- `FileProvider` configuration, not exported, granting URI permission per intent.

## Non-goals

- Import (post-MVP).
- Any format other than Markdown.
- Any cloud destination.
- Scheduled or automatic export.

## Implementation notes

- The format is specified exactly in `docs/data/03-journal-export-format.md` section 2. Build a golden-file test from that example first, then make it pass.
- **Escape user text** at the start of a line: `>`, `#`, `-`, `|` and backtick. A note containing a pipe must not break the techniques table; a note starting with `#` must not become a heading.
- Rest days render as `## {date} - no training logged` with no body. Do not omit them - the export tells the same truth the app does.
- The techniques table is always last and always complete, including `MET`-level techniques. Locked techniques are omitted.
- Use the streaming writer in production. A two-year export built as one `String` is a real OOM risk on a low-memory device.
- Section labels come from string resources; dates use the device locale; user text is never transformed.
- The `FileProvider` authority must be `${applicationId}.fileprovider` and the provider must not be exported. Grant read permission per intent, never broadly.
- If no app can receive the share intent, show a specific message offering "Save to a file" instead - not a generic error.

## Affected layers

`data/export`, `feature/you`, manifest (`FileProvider`), `res/xml/file_paths.xml`.

## Acceptance criteria

- [ ] The sheet offers all three ranges and both destinations, with the content note.
- [ ] The rendered Markdown matches the documented format exactly for the golden fixture.
- [ ] Days are newest first with the documented headers.
- [ ] Rest days are shown.
- [ ] Skipped activities render as skipped.
- [ ] User text is escaped so no note can break the document structure.
- [ ] The techniques table is complete, last, and omits locked techniques.
- [ ] Share opens the system sheet and the file opens in a text app.
- [ ] Save opens SAF and writes the file to the chosen location.
- [ ] The cache file is deleted after the intent resolves.
- [ ] An empty database exports a valid file with a header and an empty table.
- [ ] A two-year export completes within 3 seconds and does not exceed 50 MB of heap.
- [ ] The `FileProvider` is not exported and grants permission per intent.
- [ ] No suitable share target shows a specific message, not a generic error.
- [ ] Works at `fontScale 2.0`.

## Unit test expectations

`MarkdownJournalWriterTest` - a golden-file test covering a normal day, a rest day, a skipped activity, an early-ended focus session, a review, a partially-filled reflection, and text containing `|`, `#`, a leading `-` and a newline. Plus: an empty database; identical output from the pure and streaming renderers.

`ExportJournalUseCaseTest` - each range selects the right days; the filename format; the cache path.

## UI test expectations

`ExportSheetTest` - the sheet renders all options; selecting a destination fires the right intent (asserted with an `Intents` stub); the no-target message.

## Integration test expectations

- `ExportPerformanceTest` - two years of seeded data exports within 3 seconds.

## Manual verification

1. Export Last 30 days and share to a notes app; the file opens and reads correctly.
2. Write a note containing `|`, `#` and a line break; export; the Markdown is intact.
3. Export Everything on a populated database; check the techniques table.
4. Save to a file via SAF; open it from a file manager.
5. Export on an empty database; the file is valid.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
