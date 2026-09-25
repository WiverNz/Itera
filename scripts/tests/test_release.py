"""Windows regression checks; Git is a stub. No real commits, tags, or pushes."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]
SHELLS = [p for name in ("powershell.exe", "pwsh.exe") if (p := shutil.which(name))]
FAKE_GIT = r"""
import json, os, pathlib, sys
args = sys.argv[1:]
root = pathlib.Path(os.environ["RELEASE_FIXTURE"])
with (root / "git-log.jsonl").open("a", encoding="utf-8") as log:
    log.write(json.dumps(args) + "\n")
command = args[0]
if command == os.environ.get("FAIL_GIT"):
    sys.exit(42)
if args == ["rev-parse", "--verify", "HEAD"]:
    print("fixture-head")
elif args == ["rev-parse", "--is-shallow-repository"]:
    print(os.environ.get("SHALLOW", "false"))
elif args == ["symbolic-ref", "--short", "HEAD"]:
    if os.environ.get("DETACHED"): sys.exit(1)
    print("main")
elif args == ["status", "--porcelain"]:
    if os.environ.get("DIRTY"): print(" M example")
elif command == "show-ref":
    sys.exit(0 if os.environ.get("EXISTING_TAG") else 1)
elif args == ["tag", "--list", "v*"]:
    if os.environ.get("OLD_CODE"): print("v1.0.0")
elif command == "show":
    if args[1] == "HEAD:version.properties":
        print((root / "version.properties").read_text(), end="")
    else:
        print("versionName=1.0.0\nversionCode=" + os.environ["OLD_CODE"])
elif command == "remote":
    print("https://example.invalid/repo.git")
elif command in ("add", "commit", "tag", "push"):
    pass
else:
    raise SystemExit("Unexpected Git command: " + repr(args))
"""


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="itera-release-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "scripts").mkdir()
        for name in ("version.ps1", "release.ps1"):
            shutil.copyfile(SCRIPTS / name, self.root / "scripts" / name)
        (self.root / "fake_git.py").write_text(FAKE_GIT, encoding="utf-8")
        (self.root / "git.cmd").write_text(
            '@echo off\n"' + sys.executable + '" "%~dp0fake_git.py" %*\n',
            encoding="utf-8",
        )
        self.env = dict(os.environ, RELEASE_FIXTURE=str(self.root))
        self.env["PATH"] = str(self.root) + os.pathsep + self.env["PATH"]
        self.reset()

    def reset(self, text="versionName=1.0.0\nversionCode=1\n"):
        (self.root / "version.properties").write_bytes(text.encode())
        (self.root / "git-log.jsonl").write_text("")

    def run_script(self, shell, script, *args, ok=True, **env):
        result = subprocess.run(
            [shell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
             str(self.root / "scripts" / script), *args],
            cwd=self.root, env=dict(self.env, **env), text=True,
            input="n\n", capture_output=True, timeout=30,
        )
        self.assertEqual(result.returncode == 0, ok, result.stdout + result.stderr)
        return result.stdout + result.stderr

    def log(self):
        return [json.loads(line) for line in
                (self.root / "git-log.jsonl").read_text().splitlines()]

    def test_bumps_and_newlines(self):
        for shell in SHELLS:
            for spec, expected in (("patch", "1.0.1"), ("minor", "1.1.0"),
                                   ("major", "2.0.0"), ("2.3.4", "2.3.4")):
                for newline in ("\n", "\r\n"):
                    with self.subTest(shell=shell, spec=spec, newline=repr(newline)):
                        self.reset(newline.join(("versionName=1.0.0", "versionCode=1", "")))
                        self.run_script(shell, "version.ps1", spec, "-Write")
                        self.assertEqual((self.root / "version.properties").read_bytes(),
                                         newline.join((f"versionName={expected}", "versionCode=2", "")).encode())

    def test_invalid_versions(self):
        for shell in SHELLS:
            for spec in ("1.0.0", "0.9.9", "01.2.3", "1.2.3-rc.1", "v1.2.3", "oops"):
                self.run_script(shell, "version.ps1", spec, "-Write", ok=False)

    def test_invalid_codes_and_duplicates(self):
        for shell in SHELLS:
            for text in ("versionName=1.0.0\nversionCode=0\n",
                         "versionName=1.0.0\nversionCode=2100000000\n",
                         "versionName=1.0.0\nversionCode=1\nversionCode=2\n"):
                self.reset(text)
                self.run_script(shell, "version.ps1", "patch", "-Write", ok=False)
                self.assertEqual((self.root / "version.properties").read_text(), text)

    def test_release_requires_bump(self):
        for shell in SHELLS:
            self.reset()
            self.run_script(shell, "release.ps1", "current", "--yes", ok=False)
            self.assertFalse(any(a[0] == "add" for a in self.log()))

    def test_tag_guard(self):
        for shell in SHELLS:
            self.run_script(shell, "version.ps1", "-Tag", "v1.0.0")
            for tag in ("v1.0.1", "v1.0.0-rc.1", "1.0.0"):
                self.run_script(shell, "version.ps1", "-Tag", tag, ok=False)

    def test_dry_run_never_writes(self):
        for shell in SHELLS:
            self.reset()
            original = (self.root / "version.properties").read_bytes()
            self.run_script(shell, "release.ps1", "minor", "--dry-run", "--push", DIRTY="1")
            self.assertEqual((self.root / "version.properties").read_bytes(), original)
            self.assertFalse(any(a[0] in ("add", "commit", "push") or a[:2] == ["tag", "-a"]
                                 for a in self.log()))

    def test_preflight_guards(self):
        for shell in SHELLS:
            for options in ({"DIRTY": "1"}, {"SHALLOW": "true"}, {"DETACHED": "1"},
                            {"EXISTING_TAG": "1"}, {"OLD_CODE": "2"}):
                self.reset()
                self.run_script(shell, "release.ps1", "--yes", ok=False, **options)
                self.assertFalse(any(a[0] == "add" for a in self.log()))

    def test_confirmation_aborts(self):
        for shell in SHELLS:
            self.reset()
            self.run_script(shell, "release.ps1", ok=False)
            self.assertFalse(any(a[0] == "add" for a in self.log()))

    def test_commit_tag_push_order(self):
        for shell in SHELLS:
            self.reset()
            self.run_script(shell, "release.ps1", "minor", "--yes", "--push")
            mutations = [a for a in self.log() if a[0] in ("add", "commit", "push") or a[:2] == ["tag", "-a"]]
            self.assertEqual(mutations, [
                ["add", "--", "version.properties"],
                ["commit", "--only", "-m", "Release v1.1.0", "--", "version.properties"],
                ["tag", "-a", "v1.1.0", "-m", "Release v1.1.0"],
                ["push", "origin", "main"],
                ["push", "origin", "refs/tags/v1.1.0:refs/tags/v1.1.0"],
            ])

    def test_failures_stop_without_rollback(self):
        for shell in SHELLS:
            for command in ("add", "commit", "push"):
                self.reset()
                self.run_script(shell, "release.ps1", "--yes", "--push", ok=False, FAIL_GIT=command)
                self.assertEqual(self.log()[-1][0], command)
                self.assertIn("versionName=1.0.1", (self.root / "version.properties").read_text())
                if command == "push":
                    self.assertEqual(self.log()[-1], ["push", "origin", "main"])


if __name__ == "__main__":
    if os.name != "nt" or not SHELLS:
        raise SystemExit("Run these checks on Windows with PowerShell installed.")
    print("Testing: " + ", ".join(SHELLS), flush=True)
    unittest.main(verbosity=2)