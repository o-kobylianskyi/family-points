#!/usr/bin/env python3
"""Validate the signed APK's release manifest before publication.

Manifest SHA is a build provenance assertion, not cryptographic proof of source
content. The build script pins a clean HEAD before and after Gradle; signature
identity must still be verified separately with Android apksigner.
"""
import argparse
import hashlib
import json
import pathlib
import re
import sys

APP_ID = "com.olehkobylianskyi.familypoints.android"
FILE_NAME = "familypoints-beta.apk"
URL = "https://family-point.com/downloads/familypoints-beta.apk"
VERSION = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+(?:-beta\.[0-9]+)?$")
SHA = re.compile(r"^[0-9a-f]{40}$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")


def validate(apk, manifest, expected_source_sha=None):
    data = json.loads(pathlib.Path(manifest).read_text(encoding="utf-8-sig"))
    if not isinstance(data, dict):
        raise ValueError("Release manifest must be a JSON object")
    for key, value in (("applicationId", APP_ID), ("file", FILE_NAME), ("downloadUrl", URL)):
        if data.get(key) != value:
            raise ValueError(f"Invalid {key}")
    if type(data.get("versionCode")) is not int or not 1 <= data["versionCode"] <= 2147483647:
        raise ValueError("Invalid versionCode")
    if not isinstance(data.get("versionName"), str) or not VERSION.fullmatch(data["versionName"]):
        raise ValueError("Invalid versionName")
    if type(data.get("minSdk")) is not int or data["minSdk"] != 26:
        raise ValueError("Unexpected minSdk")
    source = data.get("sourceSha")
    if not isinstance(source, str) or not SHA.fullmatch(source):
        raise ValueError("Missing or invalid sourceSha; rebuild from a clean checkout")
    if expected_source_sha is not None and source != expected_source_sha:
        raise ValueError("Manifest sourceSha differs from the expected git revision")
    expected = data.get("sha256")
    if not isinstance(expected, str) or not SHA256.fullmatch(expected):
        raise ValueError("Invalid APK SHA-256")
    size = data.get("sizeBytes")
    if type(size) is not int or size <= 0:
        raise ValueError("Invalid APK size")
    apk = pathlib.Path(apk)
    if not apk.is_file() or apk.stat().st_size != size:
        raise ValueError("APK file missing or size mismatch")
    digest = hashlib.sha256()
    with apk.open("rb") as reader:
        for chunk in iter(lambda: reader.read(1024 * 1024), b""):
            digest.update(chunk)
    if digest.hexdigest() != expected:
        raise ValueError("APK SHA-256 mismatch")
    return data


def main():
    p = argparse.ArgumentParser()
    p.add_argument("apk")
    p.add_argument("manifest")
    p.add_argument("--expected-source-sha")
    args = p.parse_args()
    try:
        data = validate(args.apk, args.manifest, args.expected_source_sha)
    except (ValueError, OSError, json.JSONDecodeError) as exc:
        print(f"APK MANIFEST REJECTED: {exc}", file=sys.stderr)
        return 1
    print(f"APK MANIFEST PASS: version={data['versionName']} code={data['versionCode']} sourceSha={data['sourceSha']}")
    print("NOTE: APK signing certificate must be independently verified with apksigner.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
