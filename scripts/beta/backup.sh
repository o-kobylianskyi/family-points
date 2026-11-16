#!/usr/bin/env bash
set -Eeuo pipefail
# BETA host-only database snapshot. Publish a final filename only after
# pg_dump succeeds and pg_restore can read the archive table of contents.
cd "$(dirname "$0")/../.."
if [[ ! -f .env ]]; then
  echo "Missing .env in repository root" >&2
  exit 1
fi
umask 077
mkdir -p backups
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
out="backups/family-points-beta-${stamp}.dump"
if [[ -e "$out" ]]; then
  echo "Backup already exists: $out" >&2
  exit 1
fi
# Temp file is created privately in the destination filesystem. An incomplete
# archive is never advertised as a finished .dump file.
tmp="$(mktemp "backups/.family-points-beta-${stamp}.XXXXXXXX.partial")"
cleanup() { rm -f -- "$tmp"; }
trap cleanup EXIT
if ! docker compose --env-file .env -f compose.beta.yml exec -T database \
  pg_dump -U family_points -d family_points -Fc > "$tmp"; then
  echo "pg_dump failed; no completed backup was created" >&2
  exit 1
fi
if [[ ! -s "$tmp" ]]; then
  echo "Backup is empty; no completed backup was created" >&2
  exit 1
fi
# Use pg_restore shipped with the same PostgreSQL container; no host dependency.
# This validates archive metadata/readability, NOT a successful data restore.
if ! docker compose --env-file .env -f compose.beta.yml exec -T database \
  pg_restore --list < "$tmp" > /dev/null; then
  echo "Backup archive verification failed; no completed backup was created" >&2
  exit 1
fi
chmod 600 "$tmp"
# Hard-link provides an atomic no-overwrite final name, including concurrent
# backups started in the same second. Then remove the hidden temporary name.
if ! ln -- "$tmp" "$out"; then
  echo "Final backup name already exists or could not be created: $out" >&2
  exit 1
fi
rm -f -- "$tmp"
trap - EXIT
echo "Backup created and archive TOC verified: $out"
echo "IMPORTANT: archive TOC verification is NOT a restore test. Before deployment,"
echo "verify recovery in an isolated disposable database and protect the backup."
