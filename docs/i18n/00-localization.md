# Localisation

**Four languages, all MVP scope: English, Russian, German, Spanish.** Runtime switching from Settings and from the Welcome screen, persisted across restarts, covering the whole app including technique content.

**Source: `design/`.** The prototype already implements this end to end - 362 strings and 4 plurals in each of four locales, a locale config, the AppCompat backport, and a working picker. Porting it is mostly file movement.

## 1. Mechanism

Per-app language via `AppCompatDelegate.setApplicationLocales`, which is the platform API on Android 13+ and an AppCompat backport below it.

```kotlin
object AppLanguage {
    val tags = listOf("", "en", "ru", "de", "es")   // "" = follow the device

    fun current(): String =
        AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(tag)
        )
    }
}
```

Three things make it work, and all three are required:

1. **`MainActivity` extends `AppCompatActivity`**, not `ComponentActivity`. The backport needs it. This is why `androidx.appcompat` is a dependency (D-13).
2. **`res/xml/locales_config.xml`**, referenced from the manifest as `android:localeConfig`, listing `en`, `ru`, `de`, `es`. On Android 13+ this also puts Itera in the system per-app language settings.
3. **`AppLocalesMetadataHolderService`** declared in the manifest with `autoStoreLocales = true`. This is what **persists the choice across restarts** on Android 12 and below; the platform handles it above.

```xml
<service
    android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
    android:enabled="false"
    android:exported="false">
    <meta-data android:name="autoStoreLocales" android:value="true" />
</service>
```

The app theme is `Theme.AppCompat.DayNight.NoActionBar`.

**Do not** store the language in DataStore and re-apply it at startup. `AppCompatDelegate` already owns persistence; a second copy would drift from the system setting the user can change outside the app.

## 2. Switching at runtime

Calling `AppLanguage.set(tag)` recreates the activity with the new configuration. Compose re-reads every `stringResource` and the whole UI, including the open bottom sheet, renders in the new language immediately.

No restart, no dialog, no "changes take effect later".

## 3. Where the user switches

| Surface | Control |
| --- | --- |
| Welcome | `LanguagePill` in the brand row - a `surface2` capsule with the `Globe` icon and the current language code |
| Settings | You > **Language & region** > Language |
| System (Android 13+) | Settings > Apps > Itera > Language, because `localeConfig` is declared |

Both in-app entry points open the same `LanguageSheet`.

## 4. `LanguageSheet`

`ModalBottomSheet(skipPartiallyExpanded = true)`, `containerColor = surface`, `scrimColor = scrim`, padding h20 bottom 28, `spacedBy(16)`.

Five rows, `heightIn(min = 64)`, `Role.RadioButton`, `spacedBy(14)`:

| Row | Leading tile (40 dp, radius 13, `surface2`) | Title | Subtitle |
| --- | --- | --- | --- |
| Match device | `Globe` icon at 20 | "Match device" | "Currently {device language, named in the UI language}" |
| en | "EN" in `caption` Bold | "English" | "English" |
| ru | "RU" | "Русский" | "Russian" |
| de | "DE" | "Deutsch" | "German" |
| es | "ES" | "Español" | "Spanish" |

The **title** is the language in its own language (`Locale.getDisplayLanguage(itself)`, first letter title-cased in that locale). The **subtitle** is the same language named in the current UI language. Someone who has accidentally switched to a language they cannot read can still find their way back by recognising the native name.

A `Divider()` sits after the "Match device" row only. `RadioDot` trails the selected row. A "Done" `IteraButton` closes the sheet.

## 5. Resources

```
res/values/strings.xml        en (default)   362 strings + 4 plurals
res/values-ru/strings.xml     ru
res/values-de/strings.xml     de
res/values-es/strings.xml     es
res/xml/locales_config.xml
```

### Key naming

Prefix by area, already established by the prototype:

| Prefix | Area | Count |
| --- | --- | --- |
| `t_*` | Technique content - `t_{short}_name`, `_short`, `_why`, `_task` | 56 |
| `skill_*`, `level_*`, `skill_level_*` | Skills and mastery | 22 |
| `nav_*`, `sec_*`, `action_*` | Chrome | 17 |
| `welcome_*`, `goals_*`, `rhythm_*`, `week_*`, `onb_*` | Onboarding | 22 |
| `today_*`, `hero_*`, `step_*`, `greeting_*`, `day_*` | Today and the day loop | 33 |
| `exercise_*`, `result_*`, `feel_*`, `two_*` | Exercise flow | 20 |
| `focus_*` | Focus timer | 12 |
| `reflection_*`, `chip_*`, `good_*` | Reflection | 18 |
| `eis_*`, `fey_*`, `pm_*`, `hs_*`, `combo_*`, `review_*`, `interval_*` | Technique screens | 95 |
| `train_*`, `library_*`, `detail_*`, `filter_*` | Train, library, detail | 20 |
| `progress_*`, `history_*` | Progress and history | 9 |
| `language_*`, `theme_*`, `pace_*`, `notif_*`, `coach_*`, `time_*` | Settings | 27 |
| `app_*`, `minutes_*`, `not_*`, `your_*`, `privacy_*`, `export_*`, `load_*` | Misc | 11 |

Keep this scheme. A new string goes in its area's prefix.

### Plurals

Four `plurals` sets: `practices`, `words`, `review_days_ago`, `review_next`.

English declares `one` / `other`. **Russian declares `one` / `few` / `many` / `other`** - the prototype gets this right and it is the main reason plurals must never be faked with a format argument:

```xml
<plurals name="practices">
    <item quantity="one">%d практика</item>
    <item quantity="few">%d практики</item>
    <item quantity="many">%d практик</item>
    <item quantity="other">%d практики</item>
</plurals>
```

German and Spanish use `one` / `other`.

Any new count-bearing string becomes a plural in all four locales.

## 6. Dates, times and numbers

Always locale-aware, never hand-formatted.

```kotlin
@Composable fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable fun formatTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale()))
```

| Use | Formatter |
| --- | --- |
| Times | `ofLocalizedTime(SHORT)` - gives 12- or 24-hour by locale |
| Dates in lists and detail | `ofLocalizedDate(MEDIUM)` |
| Full dates in History | `ofLocalizedDate(FULL)` |
| Month headers | `ofPattern("LLLL yyyy")` - `LLLL` is the standalone month form, which matters in Russian ("сентябрь", not "сентября") |
| Short dates | `ofPattern("d MMM")` |
| Weekday initials | `DayOfWeek.getDisplayName(TextStyle.NARROW, locale)` |
| First day of week | `WeekFields.of(locale).firstDayOfWeek` - Monday in most of Europe, Sunday in some locales |

Month names and weekday names from `ofPattern` are title-cased with `replaceFirstChar { it.titlecase(locale) }` where the design shows them capitalised - Russian and German differ from English here.

## 6a. Fonts and scripts

The two brand faces are Latin-only. Russian is a shipping language, so an uncontrolled platform fallback is not acceptable.

Four faces are bundled; the family is resolved from the active locale's script, and user-authored text always uses the widest-coverage face:

| Role | en / de / es | ru | User-authored text, any locale |
| --- | --- | --- | --- |
| Display | Bricolage Grotesque | Inter Tight | - |
| Body | Instrument Sans | Inter | **Inter** |

Full reasoning, the selection rule and the glyph-coverage test in **ADR-0019** and `docs/ux/04-design-system.md` section 2.

Adding a language whose script is neither Latin nor Cyrillic requires extending the rule and the coverage test.

## 7. What is never translated

**User-authored text.** Notes, reflection answers, Feynman explanations, premortem reasons, habit names, task labels, topic titles, the display name. These are stored verbatim and rendered verbatim, whatever the UI language. The prototype's `LogEntry` says so in a comment; keep that guarantee.

The language sheet's subtitle states it: "Your own notes stay exactly as you wrote them."

## 8. Persistence and history

The database stores **copy keys plus arguments**, never rendered English (`docs/data/01-room-schema.md` section 5). Consequence: switching language re-renders past history correctly, including activity titles and subtitles generated months ago.

This is testable and must be tested: populate a week of history, switch language, and confirm Today and History contain no leftover text from the previous language.

## 9. Layout under translation

German is the length stress case; Russian is the second. Both are in the MVP, so pseudo-locales are a pre-check, not the test.

- No string is built by concatenation. Every substitution is a positional argument (`%1$s`, `%2$d`).
- Button labels, chips, pills and tab labels must not clip in any of the four languages.
- Notification bodies stay under 60 characters **in every language**, not just English.
- `start` / `end` padding only, never `left` / `right`.
- `en-XA` (long pseudo-locale) and `en-XB` (RTL) are run as a safety net for layouts no real language happens to stress.

## 10. Adding a language later

1. Add `values-xx/strings.xml`.
2. Add `<locale android:name="xx" />` to `locales_config.xml`.
3. Add the tag to `AppLanguage.tags`.
4. Declare the right plural categories for that language.

Nothing else. The picker builds its rows from `tags` and names them through `Locale`.

## 11. Where each part is built

Localisation is **not** one late issue. Each part lands in the earliest issue that owns that surface (D-15):

| Part | Issue |
| --- | --- |
| `AppCompatActivity`, `locales_config.xml`, `AppLocalesMetadataHolderService`, the four `values-*` directories | `001` |
| `AppLanguage`, `currentLocale()`, `formatTime()`, locale-aware formatter helpers | `002` |
| The four bundled fonts, script-aware family selection, `FontCoverageTest` | `003` |
| `LanguageSheet`, `LanguagePill`, `RadioDot` | `004` |
| The 14 techniques' content in all four languages | `008` |
| The Welcome language entry point | `016` |
| The Settings "Language & region" section, runtime switching, persistence | `034` |
| Each screen's own strings, in all four languages | every screen issue (definition of done section 8) |

**Issue `041` builds none of this.** It is the final verification and audit: by the time it runs the app is already fully localised, and `041` proves it and catches drift.

## 12. Testing

| Test | Type |
| --- | --- |
| Every key in `values/` exists in all three other locales | unit, parses the XML |
| Every count-bearing string is a plural, with the right categories per language | unit |
| No string contains a non-positional `%s` | unit |
| `AppLanguage.set` then `current()` round-trips for each tag | Robolectric |
| Every screen renders in all four languages with no clipped text node | UI, parameterised |
| Switching language re-renders history with no stale text | integration |
| Date, time and month formatting per locale, including Russian standalone months | unit |
| `en-XA` / `en-XB` render | UI |

Issue `041` owns the port and these tests; every screen issue is responsible for its own strings being externalised and non-clipping.
