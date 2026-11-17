"""Synthetic GitHub metadata checks. No network, credentials or deployment."""
import copy
import unittest
from test_verify_release_evidence import ReleaseEvidenceTests, SHA
from verify_ci_artifact_provenance import verify_metadata

REPO = 1359430010

class ArtifactProvenanceTests(unittest.TestCase):
    def setUp(self):
        base = ReleaseEvidenceTests()
        base.setUp()
        self.push, self.full = base.push, base.full
        self.data = {"artifacts": [self.make("full-ci-evidence", 50),
                                   self.make("ci-push-result", 51)]}

    def make(self, name, aid):
        url = f"https://api.github.com/repos/o-kobylianskyi/family-points/actions/artifacts/{aid}"
        return {"name": name, "id": aid, "url": url,
                "archive_download_url": url + "/zip", "expired": False,
                "size_in_bytes": 300, "digest": "sha256:" + "a"*64,
                "workflow_run": {"id": 12345, "repository_id": REPO,
                                 "head_repository_id": REPO,
                                 "head_branch": "main", "head_sha": SHA}}

    def check(self):
        return verify_metadata(SHA, self.push, self.full, self.data)

    def reject(self):
        with self.assertRaises(ValueError): self.check()

    def test_valid_two_artifact_metadata(self):
        run, ids = self.check()
        self.assertEqual(run, 12345)
        self.assertEqual(ids["full-ci-evidence"], 50)

    def test_missing_artifact(self):
        self.data["artifacts"].pop()
        self.reject()

    def test_duplicate_name(self):
        self.data["artifacts"].append(self.make("ci-push-result", 52))
        self.reject()

    def test_reused_id(self):
        self.data["artifacts"][1] = self.make("ci-push-result", 50)
        self.reject()

    def test_foreign_run(self):
        self.data["artifacts"][1]["workflow_run"]["id"] = 12346
        self.reject()

    def test_foreign_repository(self):
        self.data["artifacts"][0]["workflow_run"]["repository_id"] = 7
        self.reject()

    def test_foreign_head_repository(self):
        self.data["artifacts"][0]["workflow_run"]["head_repository_id"] = 7
        self.reject()

    def test_wrong_sha(self):
        self.data["artifacts"][1]["workflow_run"]["head_sha"] = "b"*40
        self.reject()

    def test_other_branch(self):
        self.data["artifacts"][1]["workflow_run"]["head_branch"] = "feature"
        self.reject()

    def test_expired(self):
        self.data["artifacts"][0]["expired"] = True
        self.reject()

    def test_missing_expiry_flag(self):
        del self.data["artifacts"][0]["expired"]
        self.reject()

    def test_zero_size(self):
        self.data["artifacts"][0]["size_in_bytes"] = 0
        self.reject()

    def test_missing_digest(self):
        del self.data["artifacts"][1]["digest"]
        self.reject()

    def test_invalid_digest(self):
        self.data["artifacts"][1]["digest"] = "sha256:not-a-digest"
        self.reject()

    def test_wrong_api_url(self):
        self.data["artifacts"][0]["url"] += "?fake=1"
        self.reject()

    def test_wrong_zip_url(self):
        self.data["artifacts"][0]["archive_download_url"] = "https://example.invalid/artifact"
        self.reject()

    def test_fake_internal_ci_status(self):
        self.push["fullCiVerified"] = False
        self.reject()

    def test_wrong_json_root(self):
        self.data = []
        self.reject()

if __name__ == "__main__":
    unittest.main()
