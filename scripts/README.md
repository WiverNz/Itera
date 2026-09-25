# Windows versioning and releases

Windows PowerShell 5.1 or PowerShell 7 and Git on PATH are required. No Bash/WSL
scripts are provided. Run these from the repository root; scripts also resolve
the checkout correctly when launched from another directory.

The production app reads root `version.properties`.
Initial version: 1.0.0, Android versionCode 1. The prototype has independent versioning.

## Inspect and preview

```powershell
.\scripts\version.ps1
.\scripts\version.ps1 minor
.\scripts\release.ps1 patch --dry-run --push
```

Version previews do not write. `version.ps1 minor -Write` updates only
version.properties, without Git operations; commit that change manually if using
this standalone path. `version.ps1 -Tag v1.0.0` checks a tag against the file.

Stable X.Y.Z only, no prefixes, prerelease suffixes, or leading zeros. Every bump
increments versionCode by one, up to Android's 2100000000 limit.
Use minor for features, patch for fixes, major for breaking content/schema changes.

## Create a release

First review and commit these tooling files and intended app changes yourself.
Configure the four signing secrets below before pushing a release tag.
Start from a clean branch and fetch full history and tags:

```powershell
git fetch origin --tags
.\scripts\release.ps1 patch --dry-run --push
.\scripts\release.ps1 patch --push
```

The script asks for confirmation, changes version.properties, commits only that
file as Release vX.Y.Z, checks the committed version, creates an annotated tag,
and pushes the current branch before pushing the tag. Without --push it leaves
the commit/tag local and prints push commands. --yes/-y skips confirmation;
--message/-m supplies a tag annotation; --dry-run/-n never changes files or refs
and allows a dirty tree with a warning. patch is the default; minor, major, or
an explicit greater X.Y.Z are also supported.

Agents may inspect and run --dry-run only, following AGENTS.md. The real release
commands above are for the human repository owner.

The script stops on failure without rollback. A failed commit can leave the
version staged; a failed tag can leave a release commit. Inspect git status and
git log -1 before completing the failed step manually. Do not rerun a bump to
retry a push: push the existing branch, then the existing tag.

## GitHub setup and output

Enable Actions and set repository Actions secrets:

| Secret | Value |
| --- | --- |
| ITERA_KEYSTORE_BASE64 | Base64 of the backed-up release JKS/PKCS12 file |
| ITERA_STORE_PASSWORD | Keystore password |
| ITERA_KEY_ALIAS | Signing key alias |
| ITERA_KEY_PASSWORD | Key password |

Create/back up the key using Android Studio's Generate Signed App Bundle or APK
dialog. Use the same key for every sideloaded update. Base64 is encoding, not
encryption; never commit it. To copy an existing keystore's base64 to the clipboard:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\private\itera-release.jks')) | Set-Clipboard
```

Signing uses Android's [Gradle signing configuration](https://developer.android.com/build/building-cmdline).
For local signing set ITERA_KEYSTORE_PATH to an absolute path and set the three
password/alias variables above, then run gradlew.bat assembleRelease bundleRelease
--no-configuration-cache. Set ITERA_REQUIRE_SIGNING=true to require credentials.
Without signing variables local release builds remain unsigned; partial
credentials always fail. Do not put credentials in command-line arguments.

Branch/PR builds keep the existing Ubuntu Android checks and upload Itera-debug
plus test/lint reports. The PowerShell regression tests run on Windows.
Pushing vX.Y.Z validates the tag against version.properties, runs all checks,
then signs and verifies APK/AAB files on Windows. It archives:

- Itera-vX.Y.Z.apk (installable signed APK).
- Itera-vX.Y.Z.aab (signed app bundle for distribution tooling).
- Room schemas ZIP, version.properties, and SHA256SUMS.txt.
- mapping.txt when R8 is enabled and generates it.

Assets are retained in Actions and attached to a **draft GitHub Release** with
generated notes. Review the draft and complete the
[release checklist](../docs/delivery/03-release-checklist.md) before publishing.
This automation does not mark milestone 011 complete or change R8 settings.

Missing signing secrets stop the release instead of uploading unsigned files.
If draft creation fails after a successful build, download the Actions artifacts
and attach them manually. Re-running a workflow with an existing draft does not
overwrite it; manage that draft manually. GitHub's automatic GITHUB_TOKEN supplies
contents:write only to the release job; no personal token is needed.

## Regression tests

```powershell
python .\scripts\tests\test_release.py
```

Uses temporary fixtures and a fake Git executable; never mutates a real repository.
Exercises both installed PowerShell versions, bumps, bounds, tag validation,
dirty/detached/shallow guards, dry runs, confirmation, commit/tag/push order, and
failure propagation. Python is needed only for tests.