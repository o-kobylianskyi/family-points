#!/usr/bin/env python3
"""REL-WIN-LF-01: verify Windows checkout safety of tracked POSIX scripts.

Only reads source and runs two deterministic synthetic shell fixtures.
No Docker, network, real credentials, filesystem permissions or deployment.
"""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
POLICIES = {"/mvnw text eol=lf", "*.cmd text eol=crlf", "*.sh text eol=lf"}


def assert_lf(path: Path) -> None:
    raw = path.read_bytes()
    if b"\r" in raw:
        line = raw[:raw.index(b"\r")].count(b"\n") + 1
        raise AssertionError(f"CRLF or CR in {path}: line {line}; checkout must use LF")


def tracked_scripts() -> list[Path]:
    completed = subprocess.run(
        ["git", "ls-files", "-z", "--", "*.sh", "mvnw"],
        cwd=ROOT, capture_output=True, check=True,
    )
    files = [ROOT / path.decode("utf-8", errors="strict")
             for path in completed.stdout.split(b"\0") if path]
    if not files:
        raise AssertionError("Tracked shell script list unexpectedly empty")
    return files


def run_fixture(blob: bytes) -> subprocess.CompletedProcess:
    with tempfile.TemporaryDirectory(prefix="fp-shell-lf-test-") as d:
        script = Path(d) / "fixture.sh"
        script.write_bytes(blob)
        return subprocess.run(["/bin/sh", str(script)],
                              capture_output=True, text=True, check=False, timeout=5)


class ShellLfCheckoutTests(unittest.TestCase):
    def test_attributes_enforce_lf_and_preserve_existing_rules(self):
        lines = (ROOT / ".gitattributes").read_text(encoding="utf-8").splitlines()
        for policy in POLICIES:
            self.assertEqual(1, lines.count(policy), f"Missing or duplicate {policy}")
        actual = subprocess.run(
            ["git", "check-attr", "text", "eol", "--", "scripts/beta/init-backup-volume.sh", "mvnw"],
            cwd=ROOT, capture_output=True, text=True, check=True,
        ).stdout
        for name in ("scripts/beta/init-backup-volume.sh", "mvnw"):
            self.assertIn(f"{name}: text: set", actual)
            self.assertIn(f"{name}: eol: lf", actual)

    def test_all_tracked_sh_and_mvnw_physical_checkout_bytes(self):
        checked = tracked_scripts()
        for path in checked:
            with self.subTest(path=str(path.relative_to(ROOT))):
                self.assertTrue(path.is_file())
                assert_lf(path)
        print(f"TRACKED_SHELL_LF_PASS files={len(checked)}")

    def test_synthetic_lf_positive_and_crlf_negative(self):
        lf = b"#!/bin/sh\nset -eu\nprintf 'LF_FIXTURE_OK\\n'\n"
        crlf = lf.replace(b"\n", b"\r\n")
        with tempfile.TemporaryDirectory() as d:
            candidate = Path(d) / "synthetic.sh"
            candidate.write_bytes(lf)
            assert_lf(candidate)
            candidate.write_bytes(crlf)
            with self.assertRaisesRegex(AssertionError, "CRLF or CR"):
                assert_lf(candidate)
        ok = run_fixture(lf)
        self.assertEqual(0, ok.returncode, ok.stderr)
        self.assertIn("LF_FIXTURE_OK", ok.stdout)
        broken = run_fixture(crlf)
        self.assertTrue(broken.returncode != 0 or "LF_FIXTURE_OK" not in broken.stdout,
                        "CRLF POSIX fixture unexpectedly behaved identically to LF")
        print("SHELL_LF_POSITIVE_AND_CRLF_NEGATIVE_PASS")


if __name__ == "__main__":
    unittest.main()
