# 037 - Accessibility pass

**Phase** 7 - Hardening | **Depends on** 020, 027, 032, 034 | **Blocks** 041

## Goal

Sweep every screen against `docs/ux/07-accessibility.md`, fix what previous issues missed, and add the automated audits that stop regressions.

## User value

The app is usable by everyone who wants to use it, including with a screen reader, at double font size, with no animation, or with a switch device.

## Scope

- **Audit** all 24 screens plus the supporting surfaces against the checklist in `docs/ux/07-accessibility.md` sections 1-7.
- Start from the semantics the prototype already sets (roles on chips, option rows, segmented controls and tabs; `onClickLabel` on icon buttons and quadrants; `liveRegion` on the timer; `contentDescription` on calendar cells) and add what is missing.
- Fix every finding.
- Run the full TalkBack script (section 8) and fix everything it surfaces.
- Verify every screen at `fontScale` 1.0, 1.3, 1.5 and 2.0, and at maximum display size, **in German as well as English** - the two stress the layout differently.
- Verify the reduced-motion path on every animated element.
- Verify Switch Access can complete the daily loop.
- Add `FontScaleTest`: every screen renders and scrolls at `fontScale 2.0` with no clipped text.
- Add `AccessibilityAuditTest`: every interactive node has a non-empty content description or merged text, and a target of at least 44 dp.
- Add `@Preview(fontScale = 2f)` to any screen preview still lacking one.
- Produce an accessibility report listing every finding and its fix, appended to this issue file.

## Non-goals

- Redesigning any screen. Fixes are semantic and layout-tolerance changes, not visual changes. If a layout genuinely cannot work at `fontScale 2.0`, raise it as a design question, fix it in `design/` first, and then port - never silently shrink text.
- The localisation audit (issue 041). This issue runs first, because accessibility fixes can move layouts.

## Implementation notes

- The specific accommodations already specified must be verified, not re-invented: the Today hero title stepping down above `fontScale 1.5`; `SegmentedControl` stacking above 1.6; the Eisenhower 2x2 keeping its shape and scrolling internally; the timer readout capped so the ring never clips.
- The focus timer's announcement policy (5:00, 1:00, 0:00 only) is the one most likely to have been implemented as a per-second live region. Check it specifically.
- The review screen's hidden previous answer must be absent from the semantics tree, not merely visually covered. Verify with TalkBack, not by looking.
- `AccessibilityAuditTest` will produce false positives on decorative nodes; allow an explicit opt-out via a `testTag` prefix rather than loosening the rule.
- Switch Access is not automatable here; it is a manual check and the result goes in the report.
- Where a fix changes a shared component, re-run every screen that uses it - a semantics change in `StepRow` affects Today and Reflection.
- Any change that alters layout must be mirrored into `design/`, or the parity comparisons in later issues become untrustworthy (ADR-0018).

## Affected layers

All `feature/*`, `core/designsystem/component`.

## Acceptance criteria

- [ ] Every interactive element on every screen is at least 44 x 44 dp.
- [ ] Every icon-only control has a content description; every decorative icon has `contentDescription = null`.
- [ ] Every composite row merges into one sensible announcement.
- [ ] Correct `Role` and toggle/select state on every control.
- [ ] Headings are marked on every screen title and section header.
- [ ] Traversal order follows visual order on every screen.
- [ ] Focus moves to the title on opening a full-screen route, and returns on close.
- [ ] Dialogs trap focus and do not put the destructive action first.
- [ ] Every screen renders and scrolls at `fontScale 2.0` with no clipped text.
- [ ] Every documented font-scale accommodation behaves as specified.
- [ ] With animations off, every animated element renders at its end state and nothing is stuck.
- [ ] The timer announces only at 5:00, 1:00 and 0:00.
- [ ] The review screen's previous answer is absent from the semantics tree before reveal.
- [ ] No meaning is carried by colour alone anywhere.
- [ ] The full TalkBack script completes without an unlabelled control.
- [ ] Switch Access completes the daily loop.
- [ ] `FontScaleTest` and `AccessibilityAuditTest` pass.
- [ ] The accessibility report is appended to this file, listing every finding and its fix.

## Unit test expectations

None - this is a UI-level sweep.

## UI test expectations

`FontScaleTest` - every screen at `fontScale 2.0`: renders, scrolls to the bottom, and no text node reports truncation.

`AccessibilityAuditTest` - for every screen: every node with a click action has a non-empty description or text, and a bounds size of at least 44 dp.

Plus regression tests for each individual finding fixed.

## Manual verification

1. Run the full TalkBack script end to end, on a real device, with the screen off where possible.
2. Walk every screen at maximum font size and maximum display size.
3. Complete the daily loop with Switch Access.
4. Turn off animations; walk the loop; nothing is stuck.
5. View every screen in greyscale (Developer options) and confirm nothing becomes ambiguous.

## Definition of done

`docs/delivery/02-definition-of-done.md`, all sections, with section 7 fully evidenced by the appended report.

---

## Accessibility report

*(To be filled in during implementation. One row per finding: screen, issue, severity, fix, regression test.)*
