#!/usr/bin/env python3
"""Fail-closed consistency check for separately fetched GitHub artifact metadata.

Read-only. Caller MUST obtain metadata independently from trusted GitHub API;
JSON files supplied locally are not authenticated merely by passing this check.
"""
import argparse
import hashlib
import json
import pathlib
import re
import sys
import zipfile
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



def verify_zip_bytes(metadata, paths, payloads):
    """Verify GitHub-issued SHA256 against complete, local, downloaded ZIP bytes.

    This is a content-integrity check only: provenance of metadata remains an
    independent trust prerequisite. ZIP extraction is never performed.
    """
    if set(paths) != REQUIRED or set(payloads) != REQUIRED:
        raise ValueError("Exactly two named artifact ZIPs and JSON payloads are required")
    matched = {}
    for artifact in metadata.get("artifacts", []):
        if not isinstance(artifact, dict):
            raise ValueError("Malformed artifact metadata")
        name = artifact.get("name")
        if not isinstance(name, str):
            raise ValueError("Malformed artifact name")
        if name not in REQUIRED:
            continue
        if name in matched:
            raise ValueError("Duplicate artifact name")
        matched[name] = artifact
    if set(matched) != REQUIRED:
        raise ValueError("Missing matching archive records")
    result = {}
    for name in sorted(REQUIRED):
        artifact = matched[name]
        digest = artifact.get("digest")
        if not isinstance(digest, str) or not DIGEST.fullmatch(digest):
            raise ValueError("Invalid provider ZIP digest")
        raw_path = paths[name]
        if not isinstance(raw_path, (str, pathlib.Path)):
            raise ValueError("Invalid ZIP path")
        archive = pathlib.Path(raw_path)
        if not archive.is_file() or archive.is_symlink():
            raise ValueError("ZIP is missing or a symlink")
        total = 0
        hasher = hashlib.sha256()
        with archive.open("rb") as fp:
            while chunk := fp.read(1024 * 1024):
                total += len(chunk)
                hasher.update(chunk)
        if total <= 0:
            raise ValueError("Empty ZIP bytes")
        if hasher.hexdigest() != digest.split(":", 1)[1]:
            raise ValueError(f"ZIP SHA-256 mismatch for {name}")
        if not zipfile.is_zipfile(archive):
            raise ValueError("Matching bytes are not a ZIP archive")
        # No extraction, no filesystem writes. Require expected single root
        # payload and deny ambiguous duplicate/path-traversal content names.
        with zipfile.ZipFile(archive, "r") as z:
            expected = name + ".json"
            items = z.infolist()
            if len(items) != 1 or items[0].filename != expected or items[0].is_dir():
                raise ValueError(f"ZIP contents are not the expected {expected}")
            info = items[0]
            if info.file_size > 1024 * 1024:
                raise ValueError("Artifact JSON exceeds safe size")
            with z.open(info, "r") as fp:
                content = fp.read(1024 * 1024 + 1)
            if len(content) > 1024 * 1024:
                raise ValueError("Artifact JSON exceeds safe size")
            try:
                actual = json.loads(content.decode("utf-8-sig"))
            except (UnicodeError, json.JSONDecodeError) as exc:
                raise ValueError("Invalid JSON inside ZIP") from exc
            if type(actual) is not dict or actual != payloads[name]:
                raise ValueError(f"ZIP JSON payload differs from supplied {name} report")
        result[name] = hasher.hexdigest()
    return result


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--candidate-sha", required=True)
    p.add_argument("--push-report", required=True)
    p.add_argument("--full-evidence", required=True)
    p.add_argument("--github-artifacts", required=True, help="Metadata JSON independently fetched from GitHub API")
    p.add_argument("--full-zip", required=True, help="Downloaded full-ci-evidence artifact ZIP")
    p.add_argument("--push-zip", required=True, help="Downloaded ci-push-result artifact ZIP")
    a = p.parse_args()
    try:
        run, ids = verify_metadata(a.candidate_sha, read_json(a.push_report),
                                   read_json(a.full_evidence),
                                   read_json(a.github_artifacts))
        zips = verify_zip_bytes(read_json(a.github_artifacts), {"full-ci-evidence": a.full_zip, "ci-push-result": a.push_zip}, {"full-ci-evidence": read_json(a.full_evidence), "ci-push-result": read_json(a.push_report)})
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print("CI ARTIFACT PROVENANCE HOLD:", exc, file=sys.stderr)
        return 2
    print(f"CI ARTIFACT METADATA + ZIP SHA256 CONSISTENT: run {run}; IDs {ids}; ZIP digests {zips}")
    print("NOT AUTHORIZATION: independently authenticate GitHub API metadata and actual non-skipped jobs; QA/user approval remains separate.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
