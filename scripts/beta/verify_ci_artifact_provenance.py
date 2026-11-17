#!/usr/bin/env python3
"""Fail-closed consistency check for separately fetched GitHub artifact metadata.

Read-only. Caller MUST obtain metadata independently from trusted GitHub API;
JSON files supplied locally are not authenticated merely by passing this check.
"""
import argparse
import json
import re
import sys
from verify_release_evidence import SHA_PATTERN, read_json, validate

REPO_ID = 1359430010
REQUIRED = {"full-ci-evidence", "ci-push-result"}
DIGEST = re.compile(r"^sha256:[0-9a-f]{64}$")


def verify_metadata(candidate_sha, scoped, full, metadata, *, expected_repo_id=REPO_ID):
    run_id = int(validate(candidate_sha, scoped, full))
    if type(metadata) is not dict or type(metadata.get("artifacts")) is not list:
        raise ValueError("Expected GitHub API artifact listing")
    found = {}
    ids = set()
    for artifact in metadata["artifacts"]:
        if type(artifact) is not dict:
            raise ValueError("Invalid artifact metadata")
        name = artifact.get("name")
        if not isinstance(name, str):
            raise ValueError("Invalid artifact name")
        if name not in REQUIRED:
            continue
        if name in found:
            raise ValueError("Duplicate required artifact name")
        aid = artifact.get("id")
        if type(aid) is not int or aid <= 0 or aid in ids:
            raise ValueError("Missing, reused or invalid artifact ID")
        ids.add(aid)
        run = artifact.get("workflow_run")
        if type(run) is not dict:
            raise ValueError("Missing workflow_run provenance")
        if type(run.get("id")) is not int or run["id"] != run_id:
            raise ValueError("Artifact belongs to different Actions run")
        if type(run.get("repository_id")) is not int or run["repository_id"] != expected_repo_id:
            raise ValueError("Unexpected source repository ID")
        if type(run.get("head_repository_id")) is not int or run["head_repository_id"] != expected_repo_id:
            raise ValueError("Unexpected head repository ID")
        if run.get("head_branch") != "main":
            raise ValueError("Artifact not built from main branch")
        if run.get("head_sha") != candidate_sha:
            raise ValueError("Artifact head SHA differs from CI candidate")
        if artifact.get("expired") is not False:
            raise ValueError("Artifact is expired or expiry is unknown")
        if type(artifact.get("size_in_bytes")) is not int or artifact["size_in_bytes"] <= 0:
            raise ValueError("Artifact is empty or missing size")
        if not isinstance(artifact.get("digest"), str) or not DIGEST.fullmatch(artifact["digest"]):
            raise ValueError("Missing or invalid sha256 artifact digest")
        expected_api = (f"https://api.github.com/repos/o-kobylianskyi/family-points"
                        f"/actions/artifacts/{aid}")
        if artifact.get("url") != expected_api or artifact.get("archive_download_url") != expected_api + "/zip":
            raise ValueError("Artifact API URLs inconsistent with repository and ID")
        found[name] = aid
    if set(found) != REQUIRED:
        raise ValueError("Missing required CI artifacts")
    return run_id, found


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--candidate-sha", required=True)
    p.add_argument("--push-report", required=True)
    p.add_argument("--full-evidence", required=True)
    p.add_argument("--github-artifacts", required=True, help="Metadata JSON independently fetched from GitHub API")
    a = p.parse_args()
    try:
        run, ids = verify_metadata(a.candidate_sha, read_json(a.push_report),
                                   read_json(a.full_evidence),
                                   read_json(a.github_artifacts))
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print("CI ARTIFACT PROVENANCE HOLD:", exc, file=sys.stderr)
        return 2
    print(f"CI ARTIFACT METADATA CONSISTENT: run {run}; IDs {ids}")
    print("NOT AUTHENTICATED: verify independent GitHub API origin, ZIP bytes against provider digest, job steps and QA.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
