#!/usr/bin/env python3
"""Fail fast on Testcontainers 2 PostgreSQLContainer generic syntax.

The new org.testcontainers.postgresql.PostgreSQLContainer is non-generic.
Do not flag the older org.testcontainers.containers.PostgreSQLContainer API.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
TEST_ROOT = ROOT / "src" / "test" / "java"
NEW_IMPORT = re.compile(r"^\s*import\s+org\.testcontainers\.postgresql\.PostgreSQLContainer\s*;", re.M)
INVALID_USAGE = re.compile(r"\b(?:new\s+)?PostgreSQLContainer\s*<")


def scan(root=TEST_ROOT):
    violations = []
    for source in sorted(root.rglob("*.java")):
        content = source.read_text(encoding="utf-8")
        if not NEW_IMPORT.search(content):
            continue
        for line_number, line in enumerate(content.splitlines(), 1):
            if INVALID_USAGE.search(line):
                violations.append(f"{source.relative_to(ROOT)}:{line_number}: non-generic Testcontainers 2 PostgreSQLContainer must not use <>")
    return violations


if __name__ == "__main__":
    errors = scan()
    for error in errors:
        print(error, file=sys.stderr)
    if errors:
        print(f"Testcontainers 2 preflight FAIL: {len(errors)} incompatible usage(s)", file=sys.stderr)
        sys.exit(1)
    print("Testcontainers 2 PostgreSQL preflight PASS")
