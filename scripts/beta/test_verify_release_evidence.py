"""Read-only release evidence tests. No network, credentials or deployment."""
import copy
import unittest
from verify_release_evidence import validate

SHA = "a" * 40
RUN = "https://github.com/o-kobylianskyi/family-points/actions/runs/12345"
NAMES = ("backend", "android", "web", "compose")


class ReleaseEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.push = {
            "sourceSha": SHA, "runUrl": RUN,
            "scopeResult": "success", "fullAttestationResult": "success",
            "scopedCiPassed": True, "fullCiVerified": True,
            "selected": {k: "true" for k in NAMES},
            "results": {k: "success" for k in NAMES},
            "qaAccepted": False, "releaseApproved": False,
            "deploymentExecuted": False,
        }
        self.full = {
            "sourceSha": SHA, "runUrl": RUN,
            **{k: "success" for k in NAMES},
            "fullCiVerified": True, "qaAccepted": False,
            "releaseApproved": False, "deploymentExecuted": False,
        }

    def check(self, sha=SHA):
        return validate(sha, self.push, self.full)

    def test_matching_full_evidence(self):
        self.assertEqual(self.check(), "12345")

    def test_candidate_is_short(self):
        with self.assertRaises(ValueError): self.check("a" * 7)

    def test_candidate_is_not_current_source(self):
        with self.assertRaises(ValueError): self.check("b" * 40)

    def test_full_artifact_source_differs(self):
        self.full["sourceSha"] = "b" * 40
        with self.assertRaises(ValueError): self.check()

    def test_different_run_id(self):
        self.full["runUrl"] = RUN[:-1] + "6"
        with self.assertRaises(ValueError): self.check()

    def test_non_github_host(self):
        self.full["runUrl"] = RUN.replace("github.com", "example.org")
        with self.assertRaises(ValueError): self.check()

    def test_url_query_mismatch(self):
        self.push["runUrl"] += "?spoof=1"
        with self.assertRaises(ValueError): self.check()

    def test_scoped_only_green_is_not_full(self):
        self.push["selected"]["backend"] = "false"
        self.push["results"]["backend"] = "skipped"
        self.push["fullAttestationResult"] = "skipped"
        self.push["fullCiVerified"] = False
        with self.assertRaises(ValueError): self.check()

    def test_failed_job(self):
        self.push["results"]["backend"] = "failure"
        with self.assertRaises(ValueError): self.check()

    def test_skipped_job(self):
        self.push["results"]["android"] = "skipped"
        with self.assertRaises(ValueError): self.check()

    def test_missing_job_selection(self):
        del self.push["selected"]["web"]
        with self.assertRaises(ValueError): self.check()

    def test_missing_attestation(self):
        self.push["fullAttestationResult"] = "skipped"
        with self.assertRaises(ValueError): self.check()

    def test_failed_scope(self):
        self.push["scopeResult"] = "failure"
        with self.assertRaises(ValueError): self.check()

    def test_false_as_string_is_rejected(self):
        self.push["fullCiVerified"] = "true"
        with self.assertRaises(ValueError): self.check()

    def test_qa_claim_cannot_be_copied_from_ci(self):
        self.push["qaAccepted"] = True
        with self.assertRaises(ValueError): self.check()

    def test_release_approved_flag_rejected(self):
        self.full["releaseApproved"] = True
        with self.assertRaises(ValueError): self.check()

    def test_deployment_claim_rejected(self):
        self.full["deploymentExecuted"] = True
        with self.assertRaises(ValueError): self.check()

    def test_full_attestation_individual_job_failure(self):
        self.full["compose"] = "failure"
        with self.assertRaises(ValueError): self.check()


if __name__ == "__main__":
    unittest.main()
