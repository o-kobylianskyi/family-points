"""Isolated real Git objects for tag-SHA binding; no remote, host or secrets."""
import pathlib
import subprocess
import tempfile
import unittest

from test_verify_release_evidence import ReleaseEvidenceTests, SHA, RUN
from verify_release_tag import verify_tag


class TagBindingTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = pathlib.Path(self.tmp.name)
        self.run_git("init", "-q")
        self.run_git("config", "user.name", "CI Fixture")
        self.run_git("config", "user.email", "ci-fixture@example.invalid")
        (self.root / "fixture.txt").write_text("one")
        self.run_git("add", "fixture.txt")
        self.run_git("commit", "-qm", "initial")
        self.sha = self.run_git("rev-parse", "HEAD")
        self.tag = "v0.1.0-beta.42"
        self.run_git("tag", "-am", "annotated", self.tag)
        baseline = ReleaseEvidenceTests()
        baseline.setUp()
        self.push = baseline.push
        self.full = baseline.full
        self.push["sourceSha"] = self.sha
        self.full["sourceSha"] = self.sha

    def run_git(self, *args):
        p = subprocess.run(["git", "-C", str(self.root), *args],
                           capture_output=True, text=True, check=True)
        return p.stdout.strip()

    def check(self):
        return verify_tag(self.root, self.tag, self.sha, self.push, self.full)

    def test_annotated_matching_tag(self):
        self.assertEqual(self.check(), "12345")

    def test_lightweight_tag_rejected(self):
        self.run_git("tag", "-d", self.tag)
        self.run_git("tag", self.tag)
        with self.assertRaisesRegex(ValueError, "annotated"):
            self.check()

    def test_missing_tag_rejected(self):
        self.tag = "v0.1.0-beta.99"
        with self.assertRaises(ValueError):
            self.check()

    def test_invalid_tag_rejected(self):
        self.tag = "--delete"
        with self.assertRaisesRegex(ValueError, "versioned"):
            self.check()

    def test_mismatched_tag_target(self):
        (self.root / "fixture.txt").write_text("two")
        self.run_git("add", "fixture.txt")
        self.run_git("commit", "-qm", "next commit")
        self.sha = self.run_git("rev-parse", "HEAD")
        self.push["sourceSha"] = self.sha
        self.full["sourceSha"] = self.sha
        with self.assertRaisesRegex(ValueError, "tag commit differs"):
            self.check()

    def test_mismatched_checkout_head(self):
        self.sha = "b" * 40
        with self.assertRaisesRegex(ValueError, "checkout SHA"):
            self.check()

    def test_dirty_tracked_file_rejected(self):
        (self.root / "fixture.txt").write_text("dirty")
        with self.assertRaisesRegex(ValueError, "dirty"):
            self.check()

    def test_dirty_untracked_file_rejected(self):
        (self.root / "unexpected.txt").write_text("untracked")
        with self.assertRaisesRegex(ValueError, "dirty"):
            self.check()

    def test_scoped_only_green_rejected(self):
        self.push["selected"]["web"] = "false"
        with self.assertRaisesRegex(ValueError, "web selected"):
            self.check()

    def test_other_ci_sha_rejected(self):
        self.full["sourceSha"] = "b" * 40
        with self.assertRaisesRegex(ValueError, "sourceSha"):
            self.check()

    def test_other_ci_run_rejected(self):
        self.full["runUrl"] = RUN[:-1] + "6"
        with self.assertRaisesRegex(ValueError, "run ID"):
            self.check()

    def test_false_release_approval_rejected(self):
        self.full["releaseApproved"] = True
        with self.assertRaisesRegex(ValueError, "releaseApproved"):
            self.check()


if __name__ == "__main__":
    unittest.main()
