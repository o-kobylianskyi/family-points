#!/usr/bin/env python3
"""Read-only release tag and CI SHA binding. Not a deployment authorization.

Do NOT fetch, create tags or change repository state. The GitHub provider must
still independently authenticate run jobs, artifacts and remote tag identity.
"""
import argparse
import pathlib
import re
import subprocess
import sys
from verify_release_evidence import SHA_PATTERN, read_json, validate

TAG = re.compile(r"^v[0-9]+\.[0-9]+\.[0-9]+-beta\.[0-9]+$")


def git(root, *args):
    process = subprocess.run(
        ["git", "-C", str(root), *args], text=True, capture_output=True,
        check=False, timeout=20,
    )
    if process.returncode:
        raise ValueError(f"git {' '.join(args)} failed: {process.stderr.strip()[:150]}")
    return process.stdout.strip()


def verify_tag(root, tag, candidate, scoped, full):
    if not TAG.fullmatch(tag):
        raise ValueError("Expected an explicit versioned BETA tag")
    if not SHA_PATTERN.fullmatch(candidate):
        raise ValueError("Candidate must be full lowercase 40-hex SHA")
    root = pathlib.Path(root)
    if not root.is_dir():
        raise ValueError("Missing repository root")
    # Never accept a stale checkout even if the tag and copied JSON match.
    if git(root, "status", "--porcelain", "--untracked-files=normal"):
        raise ValueError("Checkout is dirty")
    head = git(root, "rev-parse", "HEAD").lower()
    if head != candidate:
        raise ValueError("Current checkout SHA differs from candidate")
    ref = "refs/tags/" + tag
    if git(root, "cat-file", "-t", ref) != "tag":
        raise ValueError("BETA tag must be annotated, not lightweight")
    resolved = git(root, "rev-parse", ref + "^{commit}").lower()
    if resolved != candidate:
        raise ValueError("Annotated tag commit differs from candidate")
    run = validate(candidate, scoped, full)
    return run


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", default=".")
    parser.add_argument("--tag", required=True)
    parser.add_argument("--candidate-sha", required=True)
    parser.add_argument("--push-report", required=True)
    parser.add_argument("--full-evidence", required=True)
    args = parser.parse_args()
    try:
        run = verify_tag(args.repo_root, args.tag, args.candidate_sha,
                         read_json(args.push_report), read_json(args.full_evidence))
    except (ValueError, OSError, subprocess.TimeoutExpired) as exc:
        print(f"RELEASE TAG HOLD: {exc}", file=sys.stderr)
        return 2
    print(f"TAG/CI SOURCE CONSISTENT: {args.tag} @ {args.candidate_sha}; run {run}")
    print("NOT APPROVAL: verify remote annotated tag, GitHub job execution/artifact provenance, QA, backup/restore and user authorization.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
