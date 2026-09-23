# ADR-0020: Enforce bootstrap lint gates for Compose

Status: accepted during issue 001 (2026-09-23)

## Evidence

AGP 9.4.1 lint accepts `Text("hello")` with HardcodedText raised to error.
It also reports UnknownIssueId for TouchTargetSizeCheck. HardcodedText covers
XML, so severity configuration alone cannot enforce issue 001's Compose gate.

## Decision

Build a small lint registry in buildSrc (build tooling, not another Android
module). Keep HardcodedText for XML, add ComposeHardcodedText for constant
strings passed to Compose Text/BasicText, and provide TouchTargetSizeCheck for
explicit undersized Compose interactive-control modifiers. Raise all to error.
The latter is a conservative static check of explicit literal dimensions;
actual hit regions, dynamic dimensions and semantics remain device/UI checks
in screen issues and issue 037. Do not pretend static analysis proves accessibility.

Use lint-api/lint-tests 32.4.1, matching AGP 9.4.1. The lint JAR is a lintChecks
input, never packaged in the app. Unit-test positive and negative examples.
Disable only dependency-update advertisements in lint: dependency versions are
intentionally reviewed and pinned. Preserve all correctness checks.

## Other bootstrap reconciliation

Compose resolves Kotlin runtime 2.2.20 while the compiler stays 2.2.10. Explicitly
include kotlin-stdlib-common 2.2.20 so Gradle's generated locks contain this
runtime node consistently in standalone tasks as well as combined builds.
