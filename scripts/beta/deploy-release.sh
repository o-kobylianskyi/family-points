#!/usr/bin/env bash
# Controlled release of an already configured family-point.com BETA host.
# Run from /opt/familypoints-beta. Never overwrites secrets or volume.
set -Eeuo pipefail
cd "$(dirname "$0")/../.."
target="${1:-}"
if [[ ! "$target" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Usage: bash scripts/beta/deploy-release.sh <verified-full-40-character-commit-sha>" >&2
  exit 2
fi
[[ -f .env ]] || { echo "Missing private .env" >&2; exit 2; }
[[ -z "$(git status --porcelain)" ]] || { echo "Working tree is not clean" >&2; exit 2; }
prev="$(git rev-parse HEAD)"
git fetch --quiet origin feature/task-soft-delete
git cat-file -e "$target^{commit}" || { echo "Unknown commit" >&2; exit 2; }
git merge-base --is-ancestor "$target" origin/feature/task-soft-delete || {
  echo "SHA not present in the approved feature branch" >&2; exit 2;
}
docker compose --env-file .env -f compose.beta.yml config --quiet
echo "Current: $prev"
echo "Deploy:  $target"
echo "NOTE: Flyway migrations are forward-only; reverting images does NOT revert a database migration."
read -r -p "Backup BETA database and deploy pinned revision? Type DEPLOY: " answer
[[ "$answer" == DEPLOY ]] || exit 2
bash scripts/beta/backup.sh
git switch --detach "$target"
docker compose --env-file .env -f compose.beta.yml up -d --build --no-deps backend web
echo "Waiting for localhost health status (no public port changes)"
healthy=0
for i in $(seq 1 30); do
  if curl --fail --silent --show-error --max-time 4 http://127.0.0.1:18080/api/client-health >/dev/null 2>&1; then
    healthy=1
    break
  fi
  sleep 3
done
if (( healthy != 1 )); then
  echo "FAILED: BETA backend health did not recover. Inspect docker logs and migration history." >&2
  echo "Previous revision: $prev; currently checked out: $target" >&2
  echo "Do not blindly roll back app binaries if migrations are incompatible." >&2
  exit 1
fi
bash scripts/beta/smoke.sh http://127.0.0.1:18080
curl --fail --silent --show-error --max-time 12 https://family-point.com/api/client-health >/dev/null || {
  echo "Public HTTPS health failed; internal deployment works; check host Nginx / DNS / TLS" >&2; exit 1;
}
echo "BETA RELEASE OK at $target"
echo "Revision recorded in git HEAD. Android API origin: https://family-point.com/"
