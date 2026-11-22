"""No-server release scenario checks using fake git/sudo/curl/sleep executables."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

DEPLOY = Path(__file__).with_name("deploy-release.sh")


class DeploySafetyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "scripts/beta").mkdir(parents=True)
        (self.root / "scripts/beta/deploy-release.sh").write_bytes(DEPLOY.read_bytes())
        (self.root / "scripts/beta/smoke.sh").write_text("#!/bin/sh\necho smoke >> \"$FAKE_LOG\"\nexit 0\n")
        (self.root / ".env").write_text("TEST_ONLY=1\n")
        bin_dir = self.root / "fake-bin"
        bin_dir.mkdir()
        tools = {
            "git": """#!/bin/sh
echo "git $*" >> "$FAKE_LOG"
case "$1" in
  status) exit 0 ;;
  rev-parse)
    case "$2" in
      HEAD) echo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa ;;
      *) echo bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb ;;
    esac ;;
  cat-file)
    if [ "$2" = "-t" ]; then echo tag; fi ;;
  fetch) exit 0 ;;
  switch) exit 0 ;;
  *) exit 99 ;;
esac
""",
            "sudo": """#!/bin/sh
echo "sudo $*" >> "$FAKE_LOG"
if [ "$1" = "bash" ]; then
  if [ "$FAKE_BACKUP_FAIL" = 1 ]; then exit 19; fi
  exit 0
fi
case " $* " in
  *' config --quiet '*) exit 0 ;;
  *' run --no-deps --rm backup-volume-init '*)
     if [ "$FAKE_INIT_FAIL" = 1 ]; then exit 27; fi
     exit 0 ;;
  *' up -d --build --no-deps backend web '*)
     if [ "$FAKE_DOCKER_FAIL" = 1 ]; then exit 21; fi
     exit 0 ;;
esac
exit 98
""",
            "curl": """#!/bin/sh
echo "curl $*" >> "$FAKE_LOG"
case " $* " in
 *'127.0.0.1'*)
   if [ "$FAKE_HEALTH_FAIL" = 1 ]; then exit 22; fi ;;
 *'family-point.com'*)
   if [ "$FAKE_PUBLIC_FAIL" = 1 ]; then exit 23; fi ;;
esac
exit 0
""",
            "sleep": "#!/bin/sh\nexit 0\n",
        }
        for name, body in tools.items():
            exe = bin_dir / name
            exe.write_text(body)
            exe.chmod(0o755)
        self.log = self.root / "events.log"
        self.env = {
            **os.environ,
            "PATH": str(bin_dir) + os.pathsep + os.environ["PATH"],
            "FAKE_LOG": str(self.log),
        }

    def invoke(self, *args, answer="DEPLOY\n", **settings):
        return subprocess.run(
            ["bash", "scripts/beta/deploy-release.sh", *args],
            cwd=self.root, input=answer, text=True, capture_output=True,
            env={**self.env, **settings}, timeout=15
        )

    def events(self):
        return self.log.read_text() if self.log.exists() else ""

    def test_skip_backup_rejected_before_external_calls(self):
        p = self.invoke("v1.2.3-beta.4", "--skip-backup")
        self.assertNotEqual(p.returncode, 0)
        self.assertIn("forbidden", p.stderr)
        self.assertEqual(self.events(), "")

    def test_missing_tag_rejected(self):
        p = self.invoke()
        self.assertNotEqual(p.returncode, 0)
        self.assertEqual(self.events(), "")

    def test_refused_confirmation_has_no_backup_or_checkout(self):
        p = self.invoke("v1.2.3-beta.4", answer="NO\n")
        self.assertNotEqual(p.returncode, 0)
        self.assertNotIn("sudo bash", self.events())
        self.assertNotIn("git switch", self.events())

    def test_backup_failure_aborts_before_checkout_or_compose_up(self):
        p = self.invoke("v1.2.3-beta.4", FAKE_BACKUP_FAIL="1")
        self.assertNotEqual(p.returncode, 0)
        self.assertIn("sudo bash scripts/beta/backup.sh", self.events())
        self.assertNotIn("git switch", self.events())
        self.assertNotIn("up -d", self.events())

    def test_archive_init_failure_aborts_before_backend_start(self):
        p = self.invoke("v1.2.3-beta.4", FAKE_INIT_FAIL="1")
        self.assertNotEqual(p.returncode, 0)
        self.assertIn("stage: archive-volume-init", p.stderr)
        self.assertIn("run --no-deps --rm backup-volume-init", self.events())
        self.assertNotIn("up -d --build --no-deps backend web", self.events())
        self.assertNotIn("pg_restore", self.events())

    def test_docker_failure_reports_stage_without_rollback(self):
        p = self.invoke("v1.2.3-beta.4", FAKE_DOCKER_FAIL="1")
        self.assertNotEqual(p.returncode, 0)
        self.assertIn("stage: compose-up", p.stderr)
        self.assertNotIn("switch --detach aaaaaaaaaa", self.events())
        self.assertNotIn("pg_restore", self.events())

    def test_health_failure_reports_stage_without_rollback(self):
        p = self.invoke("v1.2.3-beta.4", FAKE_HEALTH_FAIL="1")
        self.assertNotEqual(p.returncode, 0)
        self.assertIn("stage: localhost-health", p.stderr)
        self.assertNotIn("pg_restore", self.events())

    def test_public_failure_reports_stage_without_rollback(self):
        p = self.invoke("v1.2.3-beta.4", FAKE_PUBLIC_FAIL="1")
        self.assertNotEqual(p.returncode, 0)
        self.assertIn("stage: public-health", p.stderr)
        self.assertIn("smoke", self.events())
        self.assertNotIn("pg_restore", self.events())

    def test_success_runs_mandatory_backup_before_checkout(self):
        p = self.invoke("v1.2.3-beta.4")
        self.assertEqual(p.returncode, 0, p.stderr)
        e = self.events()
        self.assertLess(e.index("sudo bash scripts/beta/backup.sh"), e.index("git switch --detach"))
        self.assertLess(e.index("git switch --detach"), e.index("run --no-deps --rm backup-volume-init"))
        self.assertLess(e.index("run --no-deps --rm backup-volume-init"), e.index("up -d"))
        self.assertIn("BETA RELEASE OK", p.stdout)


if __name__ == "__main__":
    unittest.main()
