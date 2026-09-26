# Screen inventory

**Source: `design/`.** Every screen below exists as a running composable in the prototype; open it before implementing. The file mapping is in `05-prototype-reference.md`.

## 1. Screens

`Route` is the prototype's route constant from `ui/IteraApp.kt` (production keeps the same route set as type-safe classes - ADR-0008). `Bar` says whether the bottom navigation is visible.

### Onboarding

| Screen | Route | Bar | Issue |
| --- | --- | --- | --- |
| Welcome | `welcome` | no | 016 |
| Choose goals | `goals` | no | 016 |
| Daily rhythm | `rhythm` | no | 016 |
| First week | `first-week` | no | 016 |

### Tabs

| Screen | Route | Bar | Issue |
| --- | --- | --- | --- |
| Today | `today` | **yes** | 017 |
| Train | `train` | **yes** | 028 |
| Progress | `progress` | **yes** | 031 |
| You | `you` | **yes** | 034 |

### Secondary (full-screen, back button, no bar)

| Screen | Route | Issue |
| --- | --- | --- |
| Library | `library` | 029 |
| History | `history` | 032 |
| Technique detail | `technique/{technique}` | 030 |

### Exercise flow

| Screen | Route | Issue |
| --- | --- | --- |
| Exercise intro | `intro/{technique}` | 018 |
| Exercise result | `result/{technique}` | 018 |
| 2-minute rule | `two-minute` | 018 |
| Focus timer | `focus/{minutes}?technique={technique}` | 021 |
| Eisenhower | `eisenhower` | 022 |
| Feynman - explain | `feynman` | 023 |
| Feynman - reflect | `feynman-feedback` | 023 |
| Review | `review` | 026 |
| Premortem | `premortem` | 024 |
| Habit stacking | `habit-stack` | 025 |
| Combination | `combination` | 027 |

### Evening

| Screen | Route | Issue |
| --- | --- | --- |
| Evening reflection | `reflection` | 019 |
| Day complete | `day-complete` | 020 |

**24 screens.**

## 2. Sheets and dialogs

| Surface | Where | Prototype | Issue |
| --- | --- | --- | --- |
| Language picker | Welcome pill, You > Language | `LanguageSheet` - `ModalBottomSheet`, `skipPartiallyExpanded`, `containerColor = surface`, `scrimColor = scrim`, padding h20 b28, `spacedBy(16)`, radio rows at `heightIn(min = 64)` with a 40 dp tile, a Divider after "Match device", and a Done button | 004 (component), 016 and 034 (entry points) |
| Time picker | Rhythm, You | **not in the prototype** - it steps 30 minutes per tap, which must not ship. Production: Material 3 `TimePicker` in a bottom sheet using `LanguageSheet`'s container styling (D-17) | 016, 034 |
| Focus setup | before a focus session | **not in the prototype** - it starts immediately with a fixed task. Production: a sheet with one "Pick one task" field and a `Segmented` duration control | 021 |
| Learning topic picker | Feynman "Change topic" | **not in the prototype** - the control is inert | 023 |
| Reset confirmations | You > Data | **not in the prototype** | 034 |
| Export range | You > Export journal | **not in the prototype** | 035 |
| Notification rationale | Rhythm | **not in the prototype** - inline card, not a sheet | 033 |

Every new sheet reuses `LanguageSheet`'s container treatment so they look like one family.

Planned **milestone 012** adds input microphones and one shared voice sheet on Today/current exercise/focus, with listening, transcript, failure, permission, unavailable, ambiguous-choice and confirmation states. These reuse existing components and add no top-level route or screen count. See [voice input](10-voice-input.md); prototype states are built in 012, not this documentation pass.

## 3. Screens that exist in the product but not the prototype

| Screen | Why | Issue |
| --- | --- | --- |
| Learning topics editor | Feynman needs a topic list; You shows a count | 023, 034 |
| Privacy note | A row exists in You, inert | 034 |
| Focus session complete | The prototype routes straight to the result screen; production records a real session first | 021 |
| Weekly look-back | A Day 7 / 14 variant of the reflection header | 019 |

Build these from the prototype's existing primitives - `ScreenColumn`, `TopBar`, `IteraCard`, `LinkRow`, `ValueRow`. None needs new visual language.

## 4. Always-dark screens

Focus timer (`IteraTheme(dark = true)` in `FocusScreen`), evening reflection, day complete. Dark in both app themes (R-08).

## 5. Responsive behaviour

The prototype targets a phone in portrait. Every screen is a single scrolling column at 20 dp gutters, so it reflows naturally. Production adds:

- a content column capped at 600 dp and centred above that width;
- landscape must not break, though it is not designed for;
- `fontScale` up to 2.0 without clipping (`07-accessibility.md`).

The one layout with a fixed aspect is the Eisenhower 2x2, whose quadrants are `heightIn(min = 168)`; it scrolls internally rather than collapsing.
