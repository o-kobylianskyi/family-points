"""Synthetic APK/manifest tests. No signing keys, server or live downloads."""
import hashlib
import json
import pathlib
import tempfile
import unittest
from verify_release_manifest import validate

SHA = "a" * 40


class ManifestSafetyTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = pathlib.Path(self.tmp.name)
        self.apk = self.root / "familypoints-beta.apk"
        self.apk.write_bytes(b"synthetic apk bytes: no credentials")
        self.meta = self.root / "familypoints-beta.json"
        self.data = dict(
            applicationId="com.olehkobylianskyi.familypoints.android",
            file="familypoints-beta.apk",
            downloadUrl="https://family-point.com/downloads/familypoints-beta.apk",
            versionCode=12, versionName="0.1.0-beta.12", minSdk=26, sourceSha=SHA,
            sha256=hashlib.sha256(self.apk.read_bytes()).hexdigest(),
            sizeBytes=self.apk.stat().st_size
        )

    def verify(self, expected=SHA):
        self.meta.write_text(json.dumps(self.data))
        return validate(self.apk, self.meta, expected_source_sha=expected)

    def test_valid(self):
        self.assertEqual(self.verify()["sourceSha"], SHA)
    def test_wrong_source_revision(self):
        with self.assertRaisesRegex(ValueError, "sourceSha"):
            self.verify("b" * 40)
    def test_missing_source_revision(self):
        del self.data["sourceSha"]
        with self.assertRaisesRegex(ValueError, "sourceSha"):
            self.verify()
    def test_malformed_source_revision(self):
        self.data["sourceSha"] = "main"
        with self.assertRaises(ValueError):
            self.verify()
    def test_apk_tampering(self):
        self.apk.write_bytes(self.apk.read_bytes() + b"x")
        with self.assertRaisesRegex(ValueError, "size mismatch"):
            self.verify()
    def test_hash_mismatch(self):
        self.data["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
            self.verify()
    def test_wrong_app(self):
        self.data["applicationId"] = "com.example.other"
        with self.assertRaisesRegex(ValueError, "applicationId"):
            self.verify()
    def test_bad_version(self):
        self.data["versionName"] = "malformed"
        with self.assertRaisesRegex(ValueError, "versionName"):
            self.verify()
    def test_noninteger_code(self):
        self.data["versionCode"] = True
        with self.assertRaisesRegex(ValueError, "versionCode"):
            self.verify()
    def test_wrong_sdk(self):
        self.data["minSdk"] = 21
        with self.assertRaisesRegex(ValueError, "minSdk"):
            self.verify()
    def test_wrong_url(self):
        self.data["downloadUrl"] = "https://example.invalid/apk"
        with self.assertRaisesRegex(ValueError, "downloadUrl"):
            self.verify()
    def test_manifest_list_not_object(self):
        self.meta.write_text("[]")
        with self.assertRaisesRegex(ValueError, "JSON object"):
            validate(self.apk, self.meta, SHA)


if __name__ == "__main__":
    unittest.main()
