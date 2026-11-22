#!/usr/bin/env bash
# REL-PG17 Gate C: GitHub ephemeral runner only; no hosts, no secrets.
set -Eeuo pipefail
: "${GITHUB_ACTIONS:?GitHub-hosted CI context required}"
: "${GITHUB_RUN_ID:?Run id required}"
: "${GITHUB_RUN_ATTEMPT:?Attempt required}"
if [[ "${RUNNER_ENVIRONMENT:-}" != "github-hosted" ]]; then
  echo "::error::Refusing non-GitHub-hosted runner" >&2; exit 1
fi

name="fp-ci-backup-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
if docker volume inspect "$name" >/dev/null 2>&1; then
  echo "::error::Runner volume unexpectedly already exists: $name" >&2; exit 1
fi
docker volume create --label "fp-ci-run=${GITHUB_RUN_ID}" --label "fp-ci-attempt=${GITHUB_RUN_ATTEMPT}" "$name" >/dev/null
label="$(docker volume inspect -f '{{ index .Labels "fp-ci-run" }}' "$name")"
[[ "$label" == "$GITHUB_RUN_ID" ]] || { echo "::error::Volume ownership mismatch" >&2; exit 1; }

# Do NOT chmod/chown or elevate this mount to make the test green.
# Fresh named volumes may be root-owned, in which case this is intentionally RED.
docker run --rm --network none --read-only --user 10001:10001 \
  --mount "type=volume,source=$name,target=/var/lib/familypoints/backups" \
  alpine:3.20 sh -euc '
    d=/var/lib/familypoints/backups
    [ "$(id -u)" = 10001 ] || exit 23
    mkdir "$d/isolated-10001"
    printf "ephemeral-only-backup-byte-payload" > "$d/isolated-10001/.pending"
    before="$(sha256sum "$d/isolated-10001/.pending" | cut -d" " -f1)"
    ln "$d/isolated-10001/.pending" "$d/isolated-10001/published"
    if ln "$d/isolated-10001/.pending" "$d/isolated-10001/published" 2>/dev/null; then
      echo "OVERWRITE_PROTECTION_BROKEN" >&2; exit 24
    fi
    mv "$d/isolated-10001/.pending" "$d/isolated-10001/renamed"
    after="$(sha256sum "$d/isolated-10001/published" | cut -d" " -f1)"
    [ "$before" = "$after" ] || { echo "HASH_MISMATCH" >&2; exit 25; }
    printf "%s" "$after" > "$d/isolated-10001/expected.sha256"
  '

# Fresh container (not the original process) remounts the very same named volume.
docker run --rm --network none --read-only --user 10001:10001 \
  --mount "type=volume,source=$name,target=/var/lib/familypoints/backups" \
  alpine:3.20 sh -euc '
    d=/var/lib/familypoints/backups/isolated-10001
    expected="$(cat "$d/expected.sha256")"
    actual="$(sha256sum "$d/published" | cut -d" " -f1)"
    [ "$expected" = "$actual" ] && [ -f "$d/renamed" ] || exit 26
    echo "EPHEMERAL_UID10001_VOLUME_PASS"
  '
