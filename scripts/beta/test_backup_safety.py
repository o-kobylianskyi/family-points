"""Non-destructive backup wrapper tests; fake docker, no real database/network."""
import os
import pathlib
import subprocess
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).parent / "backup.sh"


class BackupSafetyTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = pathlib.Path(self.tmp.name)
        (self.root / "scripts/beta").mkdir(parents=True)
        (self.root / "scripts/beta/backup.sh").write_bytes(SCRIPT.read_bytes())
        (self.root / ".env").write_text("FAKE_ONLY=1\n")
        fake = self.root / "bin"
        fake.mkdir()
        binary = fake / "docker"
        binary.write_text(
            "#!/bin/sh\n"
            "case \" $* \" in\n"
            "  *' pg_dump '*) printf 'FAKE_ARCHIVE' ; test \"$FAKE_DUMP_FAIL\" != 1 ;;\n"
            "  *' pg_restore --list '*) test \"$FAKE_RESTORE_FAIL\" != 1 && grep -q FAKE_ARCHIVE ;;\n"
            "  *) exit 99 ;;\n"
            "esac\n"
        )
        binary.chmod(0o755)
        self.env = dict(os.environ, PATH=str(fake) + os.pathsep + os.environ.get("PATH", ""))

    def run_backup(self, **env):
        return subprocess.run(
            ["bash", "scripts/beta/backup.sh"],
            cwd=self.root, env={**self.env, **env}, text=True, capture_output=True
        )

    def assertNoLeftovers(self):
        self.assertFalse(list((self.root / "backups").glob("*.dump")))
        self.assertFalse(list((self.root / "backups").glob("*.partial")))

    def test_success_archived_and_private(self):
        r = self.run_backup()
        self.assertEqual(r.returncode, 0, r.stderr)
        dumps = list((self.root / "backups").glob("*.dump"))
        self.assertEqual(len(dumps), 1)
        self.assertEqual(dumps[0].read_bytes(), b"FAKE_ARCHIVE")
        self.assertEqual(dumps[0].stat().st_mode & 0o777, 0o600)
        self.assertFalse(list((self.root / "backups").glob("*.partial")))

    def test_partial_dump_failure_never_publishes(self):
        r = self.run_backup(FAKE_DUMP_FAIL="1")
        self.assertNotEqual(r.returncode, 0)
        self.assertNoLeftovers()

    def test_invalid_archive_never_publishes(self):
        r = self.run_backup(FAKE_RESTORE_FAIL="1")
        self.assertNotEqual(r.returncode, 0)
        self.assertNoLeftovers()

    def test_existing_backup_not_overwritten(self):
        r = self.run_backup()
        self.assertEqual(r.returncode, 0, r.stderr)
        r2 = self.run_backup()
        self.assertNotEqual(r2.returncode, 0)
        self.assertEqual(len(list((self.root / "backups").glob("*.dump"))), 1)

    def test_missing_env_blocks(self):
        (self.root / ".env").unlink()
        r = self.run_backup()
        self.assertNotEqual(r.returncode, 0)
        self.assertFalse((self.root / "backups").exists())


if __name__ == "__main__":
    unittest.main()
