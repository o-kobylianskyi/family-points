"""Synthetic ZIP-byte digest and embedded JSON binding checks; no network."""
import copy
import hashlib
import io
import json
import pathlib
import tempfile
import unittest
import zipfile

from test_verify_ci_artifact_provenance import ArtifactProvenanceTests
from verify_ci_artifact_provenance import verify_metadata, verify_zip_bytes


class ZipIntegrityTests(unittest.TestCase):
    def setUp(self):
        fixture = ArtifactProvenanceTests()
        fixture.setUp()
        self.push, self.full = fixture.push, fixture.full
        self.metadata = fixture.data
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.payloads = {"full-ci-evidence": self.full, "ci-push-result": self.push}
        self.paths = {}
        for index, name in enumerate(("full-ci-evidence", "ci-push-result")):
            raw = self.make_zip({name + ".json": json.dumps(self.payloads[name]).encode()})
            p = pathlib.Path(self.tmp.name) / (name + ".zip")
            p.write_bytes(raw)
            self.paths[name] = p
            self.metadata["artifacts"][index]["digest"] = "sha256:" + hashlib.sha256(raw).hexdigest()

    @staticmethod
    def make_zip(entries):
        bio = io.BytesIO()
        with zipfile.ZipFile(bio, "w", zipfile.ZIP_DEFLATED) as z:
            for name, value in entries.items():
                z.writestr(name, value)
        return bio.getvalue()

    def validate(self):
        verify_metadata(self.push["sourceSha"], self.push, self.full, self.metadata)
        return verify_zip_bytes(self.metadata, self.paths, self.payloads)

    def replace_zip(self, name, entries, *, refresh_digest=False):
        data = self.make_zip(entries)
        self.paths[name].write_bytes(data)
        if refresh_digest:
            item = next(a for a in self.metadata["artifacts"] if a["name"] == name)
            item["digest"] = "sha256:" + hashlib.sha256(data).hexdigest()

    def rejected(self):
        with self.assertRaises(ValueError):
            self.validate()

    def test_valid_zip_hash_and_json_binding(self):
        value = self.validate()
        self.assertEqual(len(value), 2)

    def test_tampered_zip_byte(self):
        p = self.paths["full-ci-evidence"]
        p.write_bytes(p.read_bytes() + b"x")
        self.rejected()

    def test_swapped_zip_files(self):
        self.paths["full-ci-evidence"], self.paths["ci-push-result"] = (
            self.paths["ci-push-result"], self.paths["full-ci-evidence"])
        self.rejected()

    def test_missing_zip(self):
        self.paths["ci-push-result"].unlink()
        self.rejected()

    def test_symlink_zip(self):
        target = self.paths["ci-push-result"]
        link = pathlib.Path(self.tmp.name) / "link.zip"
        link.symlink_to(target)
        self.paths["ci-push-result"] = link
        self.rejected()

    def test_invalid_zip_with_matching_digest(self):
        name = "full-ci-evidence"
        self.paths[name].write_bytes(b"not a zip file")
        self.metadata["artifacts"][0]["digest"] = "sha256:" + hashlib.sha256(b"not a zip file").hexdigest()
        self.rejected()

    def test_zip_unexpected_entry(self):
        name = "full-ci-evidence"
        self.replace_zip(name, {"bad.json": b"{}"}, refresh_digest=True)
        self.rejected()

    def test_zip_extra_entry(self):
        name = "full-ci-evidence"
        self.replace_zip(name, {name + ".json": json.dumps(self.full).encode(),
                                "extra.txt": b"x"}, refresh_digest=True)
        self.rejected()

    def test_zip_payload_mismatch_even_with_matching_digest(self):
        name = "full-ci-evidence"
        changed = {**self.full, "qaAccepted": True}
        self.replace_zip(name, {name + ".json": json.dumps(changed).encode()}, refresh_digest=True)
        self.rejected()

    def test_zip_invalid_json_even_with_matching_digest(self):
        name = "full-ci-evidence"
        self.replace_zip(name, {name + ".json": b"{broken"}, refresh_digest=True)
        self.rejected()

    def test_zip_oversize_json(self):
        name = "full-ci-evidence"
        self.replace_zip(name, {name + ".json": b" " * (1024 * 1024 + 1)}, refresh_digest=True)
        self.rejected()

    def test_digest_missing(self):
        del self.metadata["artifacts"][1]["digest"]
        self.rejected()


if __name__ == "__main__":
    unittest.main()
