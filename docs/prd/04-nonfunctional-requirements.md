# Non-functional requirements

## Offline-first

Core functionality works with no network access. The manifest declares **no `INTERNET` permission**, which makes this structural rather than aspirational. Fonts, the technique catalog and all copy are bundled.

No screen shows a connectivity banner, a sync indicator, or a retry-because-network message.

## Performance

| Metric | Budget | Measured in |
| --- | --- | --- |
| Cold start to Today's first frame | < 1.5 s | issue 040, mid-range device |
| Warm start to Today's first frame | < 500 ms | issue 040 |
| Today header visible | < 300 ms from `onCreate` | issue 017 |
| Progress screen first frame with ~1 500 activities | < 300 ms | issue 031 |
| Frame time on scroll (Library, History) | no jank frames over 16 ms at 60 Hz | issue 039 |
| Release APK size | < 12 MB | issue 040 |

Startup must not block on plan generation, WorkManager, or analytics. The Today screen renders its chrome from known state and fills in.

## Reliability

- Exercise state, drafts, focus sessions and progress survive process death and device restart.
- No screen depends on in-memory state surviving a configuration change or a process kill.
- Every background job is idempotent; a replayed job cannot double-advance the program or duplicate a row.
- An invalid state transition is a no-op, never a crash.
- A corrupt result payload degrades one row, never the screen.
- Release builds never destructively migrate the database.

## Accessibility

Full specification in `docs/ux/07-accessibility.md`. Minimums:

- every interactive target at least 44 x 44 dp;
- WCAG AA text contrast, 3:1 non-text contrast;
- every screen usable and scrollable at `fontScale 2.0`;
- meaning never carried by colour alone;
- TalkBack completes the full daily loop;
- all motion removable via the system animation setting;
- correct semantic roles, headings and merged nodes.

## Localisation

**Four languages ship in the MVP: English, Russian, German and Spanish.** Full specification in `docs/i18n/00-localization.md`; the `design/` prototype already implements it.

- Runtime switching from Settings (You > Language & region) and from the Welcome screen, applied immediately with no restart.
- The choice is persisted across restarts, by `AppCompatDelegate` on Android 13+ and by the `AppLocalesMetadataHolderService` backport below it (ADR-0017).
- The app also appears in the Android 13+ system per-app language settings, via `locales_config.xml`.
- **All** app and technique content is localised - 362 strings and 4 plurals per language.
- Plurals use `plurals.xml` with the correct categories per language; Russian declares `one`/`few`/`many`/`other`.
- No string is built by concatenation; positional format arguments only.
- Dates, times, months and first-day-of-week are resolved from the active locale with `java.time`, never hand-formatted.
- `start`/`end` padding only. Layouts must not clip in any of the four languages; `en-XA` and `en-XB` are run as a safety net.
- Copy stored in the database is a key plus arguments, never rendered text (`docs/data/01-room-schema.md` section 5), so switching language re-renders past history correctly.
- **User-authored text is never translated** - notes, reflections, explanations and task labels are stored and shown verbatim.
- Every user-facing string is externalised; `HardcodedText` and `MissingTranslation` lint are **errors**.

## Privacy

- All data is local. Nothing is transmitted. There is no account and no backend.
- No analytics SDK, no crash-reporting SDK, no advertising id.
- The local event log records event names and non-identifying parameters only, never user text, and never leaves the device.
- **No user-authored text is ever logged**, at any level, in any build (`docs/architecture/05-error-handling-and-logging.md` section 5.1).
- Cloud backup is disabled for the database and preferences; device-to-device transfer is allowed.
- The journal export contains everything the user wrote and is created only on explicit request, with a plain statement of what it contains.
- A Privacy screen in the You tab states all of the above in the user's own language.

## Security

- No network surface, so no transport security concerns.
- No exported components except the launcher activity. The `FileProvider` for export is not exported and grants URI permission per intent.
- No `WebView`.
- The database is not encrypted. Accepted: the data is personal but not credential-bearing, and device encryption covers the realistic threat. Revisit if sensitive content is ever added.

## Maintainability

- Technique-specific behaviour is added without touching navigation or persistence: a new template technique is JSON plus strings (ADR-0007).
- Layer boundaries are enforced by test (`docs/architecture/01-package-structure.md` section 3).
- Content and code version independently (`docs/data/05-migrations-and-content-versioning.md`).
- Every architectural decision has an ADR; changing one requires updating it.

## Testability

- All scheduling, unlock, progress, mastery and repetition logic is pure Kotlin with an injected `Clock`, testable on the JVM with no Android framework.
- No ambient time anywhere (`LocalDate.now()` is banned outside the clock module), so day boundaries, DST and timezone changes are testable.
- Every screen composable is stateless and previewable, so UI tests need no Hilt.
- Full strategy in `docs/testing/00-strategy.md`.

## Compatibility

| Axis | Support |
| --- | --- |
| minSdk | 26 (Android 8.0) |
| targetSdk / compileSdk | 37 |
| Form factors | Phone, portrait-first. Landscape must not break, but is not designed for |
| Screen width | 320 dp to 600 dp fully supported; wider screens centre the content column at a max width of 600 dp |
| Themes | Light, dark, system; three screens are always dark |
| Languages | English, Russian, German, Spanish |
| Dynamic colour | Off - the palette is product identity |
