#!/usr/bin/env python3
"""Read-only, fail-closed evidence cross-check; NEVER grants deployment approval.

Checks that both independently obtained CI JSON artifacts describe a genuine
four-component full run for the requested immutable candidate SHA. These files
are not signatures: Release must authenticate source, job steps and artifacts
through GitHub before considering these results authoritative.
"""
import argparse
import json
import pathlib
import re
import sys
from urllib.parse import urlparse

COMPONENTS = ("backend", "android", "web", "compose")
SHA_PATTERN = re.compile(r"^[0-9a-f]{40}$")
RUN_PATH = re.compile(r"^/o-kobylianskyi/family-points/actions/runs/([0-9]+)$")
EXPECTED_HOST = "github.com"


def read_json(path):
    obj = json.loads(pathlib.Path(path).read_text(encoding="utf-8-sig"))
    if not isinstance(obj, dict):
        raise ValueError("Evidence must be a JSON object")
    return obj


def canonical_run(url):
    if not isinstance(url, str):
        raise ValueError("Missing CI run URL")
    parsed = urlparse(url)
    match = RUN_PATH.fullmatch(parsed.path)
    if (parsed.scheme != "https" or parsed.netloc != EXPECTED_HOST
            or not match or parsed.params or parsed.query or parsed.fragment):
        raise ValueError("CI run URL is not an exact repository Actions run")
    return match.group(1)


def require_value(actual, expected, label):
    # Avoid Python bool == 1, and avoid truthy strings such as "false".
    if type(actual) is not type(expected) or actual != expected:
        raise ValueError(f"{label}: expected {expected!r}, got {actual!r}")


def validate(candidate, scoped, full):
    if not isinstance(candidate, str) or not SHA_PATTERN.fullmatch(candidate):
        raise ValueError("Candidate SHA must be a full lowercase 40-hex revision")
    for label, report in (("ci-push-result", scoped), ("full-ci-evidence", full)):
        require_value(report.get("sourceSha"), candidate, label + " sourceSha")
        for flag in ("qaAccepted", "releaseApproved", "deploymentExecuted"):
            require_value(report.get(flag), False, label + " " + flag)
    run = canonical_run(scoped.get("runUrl"))
    require_value(canonical_run(full.get("runUrl")), run, "Same GitHub Actions run ID")
    require_value(scoped.get("scopeResult"), "success", "Changed-file scope")
    require_value(scoped.get("fullAttestationResult"), "success", "Full attestation job result")
    require_value(scoped.get("scopedCiPassed"), True, "Scoped CI status")
    require_value(scoped.get("fullCiVerified"), True, "Full CI status")
    require_value(full.get("fullCiVerified"), True, "Full CI attestation evidence")
    for key in COMPONENTS:
        selected = scoped.get("selected")
        results = scoped.get("results")
        if type(selected) is not dict or type(results) is not dict:
            raise ValueError("Missing component selection/result maps")
        require_value(selected.get(key), "true", key + " selected")
        require_value(results.get(key), "success", key + " job result")
        require_value(full.get(key), "success", key + " full evidence job result")
    return run


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--candidate-sha", required=True)
    parser.add_argument("--push-report", required=True)
    parser.add_argument("--full-evidence", required=True)
    args = parser.parse_args()
    try:
        run = validate(args.candidate_sha, read_json(args.push_report),
                       read_json(args.full_evidence))
    except (ValueError, OSError, json.JSONDecodeError) as exc:
        print("CI EVIDENCE HOLD:", exc, file=sys.stderr)
        return 2
    print(f"CI EVIDENCE CONSISTENT: SHA={args.candidate_sha} RUN={run}")
    print("NOT AN AUTHORIZATION: verify GitHub artifact provenance and actual non-skipped test steps.")
    print("QA acceptance, BKL-002 restoration, signing and explicit deployment approval remain separate.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
