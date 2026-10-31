#!/usr/bin/env bash
set -euo pipefail
# Run only on the Docker host, from repository root; never overwrite existing backups.
cd "$(dirname "$0")/../.."
if [[ ! -f .env ]]; then
  echo "Missing .env in repository root" >&2
  exit 1
fi
mkdir -p backups
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
out="backups/family-points-beta-${stamp}.dump"
if [[ -e "$out" ]]; then
  echo "Backup already exists: $out" >&2
  exit 1
fi
docker compose --env-file .env -f compose.beta.yml exec -T database \
  pg_dump -U family_points -d family_points -Fc > "$out"
if [[ ! -s "$out" ]]; then
  rm -f "$out"
  echo "Backup is empty" >&2
  exit 1
fi
chmod 600 "$out"
echo "Backup created: $out"
echo "IMPORTANT: test restore into an isolated disposable database and copy encrypted off-host."
