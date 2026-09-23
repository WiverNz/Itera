# 016 - Onboarding

**Phase** 3 - Shell and core loop | **Depends on** 007, 008, 011, 015 | **Blocks** none

## Prototype reference

**`design/app/src/main/java/com/itera/app/ui/screens/Onboarding.kt`** - `WelcomeScreen`, `GoalsScreen`, `RhythmScreen`, `FirstWeekScreen`, `OnboardingHeader`, `RisingPath`, `TimeRow`, `RadioDot`.

Run the prototype, open that file, and reproduce it. It is the source of truth for everything visual and interactive in this issue (`docs/00-source-of-truth.md` section 2; how-to in `docs/ux/05-prototype-reference.md`). Where this issue's acceptance criteria and the prototype disagree, the prototype wins unless a product rule is named.


## Goal

Build the four onboarding screens exactly as specified in `docs/ux/02-screen-specs-onboarding.md`.

## User value

Sixty seconds from install to a real first exercise, with no account and no questions that do not change what the app does.

## Scope

- Welcome, Goals, Rhythm and First Week screens, matching artboards `a01`-`a04`.
- One `OnboardingViewModel` scoped to `OnboardingGraph`, holding all four steps' state in `SavedStateHandle`.
- Goals: five option rows, multi-select max 2 with oldest-drops behaviour. **Focus and Learning pre-selected; "Continue" enabled only while at least one is selected** (D-05).
- Rhythm: two `TimeRow`s opening the `TimePickerSheet` from issue 004 (D-17 - the prototype's 30-minute stepping must not ship), a 5 / 15 / 30+ `Segmented` defaulting to 15, and the inline notification-rationale card.
- Welcome: the `LanguagePill` and the `LanguageSheet` entry point. The component comes from issue 004; **this issue makes language switching reachable and working before onboarding completes** (D-15).
- Welcome: "Explore with demo data", **debug builds only** (D-11).
- `POST_NOTIFICATIONS` request on "Continue" from Rhythm, advancing regardless of the answer; a denial sets all four notify flags false.
- First Week: seven rows rendered **from the curriculum asset**, not hard-coded.
- "Start Day 1": the five-step transaction in section 4 of the spec, navigating even if plan generation fails.

## Non-goals

- Any account or sign-in affordance. The artboard's "I already have an account" link is **removed** (R-10).
- Reminder scheduling logic (issue 033 supplies `ReminderScheduler`; call it).
- Editing these values later (issue 034).

## Implementation notes

- Values are held in the ViewModel and written to DataStore only on "Start Day 1" - except the notification permission result, which is written when it is known.
- Goals pre-selects Focus and Learning and gates Continue on a non-empty selection, exactly as the prototype does (D-05). Do not "fix" this to allow zero.
- The third-selection behaviour is "drop the oldest", not "block the tap". The artboard's logic does this and it feels better.
- First Week rows must come from `curriculum.v1.json`. Hard-coding them would let the two drift.
- If plan generation fails in step 3, still navigate. Today generates its own plan and shows its loading state. Onboarding must never dead-end.
- Onboarding is unreachable once `onboardingCompleted` is true, except after "Erase everything".
- The progress bar is decorative; "1 of 3" carries the information for screen readers.

## Affected layers

`feature/onboarding`, `core/navigation` (replacing placeholders).

## Acceptance criteria

- [ ] The language pill opens the picker; switching language before onboarding completes re-renders the flow immediately and preserves entered values.
- [ ] Both time rows open a real Material 3 time picker and display the result in the device's 12/24-hour format.
- [ ] All onboarding strings exist in all four languages and none clips in German.
- [ ] Welcome has exactly one action, "Get started"; no sign-in link exists anywhere in the app.
- [ ] Goals starts with Focus and Learning selected.
- [ ] Goals allows 1 or 2 selections; a third drops the oldest, visibly; deselecting to zero disables Continue.
- [ ] "Continue" is enabled on Rhythm and First week regardless of input.
- [ ] Both time pickers open, set, and display in the device's 12/24-hour format.
- [ ] The time budget defaults to 15 min and offers 5 / 15 / 30+.
- [ ] The notification rationale card appears only when the permission is not granted.
- [ ] Denying the permission continues the flow and sets all four notify flags false.
- [ ] First Week renders seven rows from the curriculum, with Day 1 in skill colour and a "Today" badge.
- [ ] "Start Day 1" writes preferences, seeds unlocks, generates the plan, schedules reminders and lands on Today.
- [ ] Back navigation between steps preserves entered values.
- [ ] Process death mid-onboarding returns to Welcome with selections preserved.
- [ ] After completion, back cannot return to onboarding.
- [ ] Every `OptionRow` reports `Role.Checkbox` with its checked state to TalkBack.
- [ ] Every screen works at `fontScale 2.0` without clipping.

### Prototype parity

- [ ] The screen was compared **side by side** against its prototype composable, on one device, in light and dark, and the comparison is recorded in this issue (screen, device, themes, any deviation and its reason).
- [ ] Structure, element order, spacing, sizes, radii, type styles and colours match the prototype.
- [ ] Interactions match: what is tappable, what is disabled and when, what animates and with which spec.
- [ ] Copy matches, in the same positions.
- [ ] Any deviation is either required by a product rule named in `docs/00-source-of-truth.md` section 6, or was agreed and **`design/` was updated in the same change**.
- [ ] The screen renders in English, Russian, German and Spanish with no clipped or overlapping text.

## Unit test expectations

`OnboardingViewModelTest`:
- initial state;
- the initial selection is Focus + Learning;
- selecting a third goal drops the first;
- deselecting everything disables Continue;
- deselecting works;
- time and budget updates;
- permission denial sets all four flags false;
- "Start Day 1" performs all five steps in order;
- plan-generation failure still completes onboarding;
- state survives a `SavedStateHandle` round-trip.

## UI test expectations

`OnboardingScreenTest`:
- the three-step progress bar and counter render correctly;
- max-2 selection behaviour;
- "Continue" enabled with zero selections;
- First Week renders seven curriculum rows;
- completing the flow navigates to Today;
- back from step 2 returns to step 1 with selections intact.

## Integration test expectations

- `FirstRunIntegrationTest` - a clean database, full onboarding, then assert: preferences written, `technique_state` seeded with `daily_reflection` and `two_minute_rule` unlocked, a Day 1 `training_day` exists with three activities.

## Manual verification

1. Fresh install; complete onboarding in under 60 s; land on Today with the 2-minute rule hero.
2. Complete it again after "Erase everything", this time selecting nothing and denying notifications; Day 1 is still valid.
3. Kill the app on step 2 and reopen; selections are preserved.
4. Run the flow with TalkBack.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections.
