"""Standalone CI outcome tests; no credentials, network or database."""
import unittest
from report_outcome import evaluate

class TestCiOutcomes(unittest.TestCase):
    def setUp(self):
        self.env = {"SOURCE_SHA": "a" * 40, "EVENT_NAME": "push",
                    "RUN_URL": "https://github.com/example/repo/actions/runs/1",
                    "SCOPE_RESULT": "success", "FULL_ATTESTATION_RESULT": "success"}
        for k in ("BACKEND", "ANDROID", "WEB", "COMPOSE"):
            self.env[k + "_SELECTED"] = "true"
            self.env[k + "_RESULT"] = "success"

    def assertStatus(self, scoped, full):
        actual = evaluate(self.env)
        self.assertEqual((actual["scopedCiPassed"], actual["fullCiVerified"]), (scoped, full))
        for key in ("qaAccepted", "releaseApproved", "deploymentExecuted"):
            self.assertFalse(actual[key])

    def test_four_component_pass_and_attestation(self): self.assertStatus(True, True)
    def test_docs_only_is_scoped_not_full(self):
        for k in ("BACKEND", "ANDROID", "WEB", "COMPOSE"): self.env[k + "_SELECTED"] = "false"
        self.env["FULL_ATTESTATION_RESULT"] = "skipped"
        self.assertStatus(True, False)
    def test_selected_job_failed(self):
        self.env["WEB_RESULT"] = "failure"
        self.assertStatus(False, False)
    def test_selected_job_cancelled(self):
        self.env["ANDROID_RESULT"] = "cancelled"
        self.assertStatus(False, False)
    def test_selected_job_skipped(self):
        self.env["BACKEND_RESULT"] = "skipped"
        self.assertStatus(False, False)
    def test_attestation_skipped_blocks_full_not_scoped(self):
        self.env["FULL_ATTESTATION_RESULT"] = "skipped"
        self.assertStatus(True, False)
    def test_missing_selection_fails_closed(self):
        del self.env["COMPOSE_SELECTED"]
        self.assertStatus(False, False)
    def test_invalid_selection_fails_closed(self):
        self.env["WEB_SELECTED"] = "unknown"
        self.assertStatus(False, False)
    def test_missing_result_fails_closed(self):
        del self.env["ANDROID_RESULT"]
        self.assertStatus(False, False)
    def test_scope_failed_fails_closed(self):
        self.env["SCOPE_RESULT"] = "failure"
        self.assertStatus(False, False)
    def test_one_component_scope_not_full(self):
        for k in ("BACKEND", "ANDROID", "COMPOSE"): self.env[k + "_SELECTED"] = "false"
        self.env["FULL_ATTESTATION_RESULT"] = "skipped"
        self.assertStatus(True, False)
    def test_invalid_nonselected_result_fails_closed(self):
        self.env["WEB_SELECTED"] = "false"
        self.env["WEB_RESULT"] = ""
        self.assertStatus(False, False)

if __name__ == "__main__":
    unittest.main()
