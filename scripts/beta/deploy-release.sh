#!/usr/bin/env bash
# Controlled release of an already configured family-point.com BETA host.
# Run from /opt/familypoints-beta. Never overwrites secrets or volume.
set -Eeuo pipefail
cd "$(dirname "$0")/../.."
tag="${1:-}"
# A standard BETA deployment never allows a missing database snapshot.
# This guards against forward-only Flyway migrations and accidental rollback.
if (( $# != 1 )); then
  echo "Usage: bash scripts/beta/deploy-release.sh vX.Y.Z-beta.N" >&2
  echo "A validated BETA database backup is mandatory; --skip-backup is forbidden." >&2
  exit 2
fi
if [[ ! "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+-beta\.[0-9]+$ ]]; then
  echo "Usage: bash scripts/beta/deploy-release.sh vX.Y.Z-beta.N" >&2
  exit 2
fi
[[ -f .env ]] || { echo "Missing private .env" >&2; exit 2; }
[[ -z "$(git status --porcelain)" ]] || { echo "Working tree is not clean" >&2; exit 2; }
prev="$(git rev-parse HEAD)"
git fetch --quiet origin "refs/tags/$tag:refs/tags/$tag"
[[ "$(git cat-file -t "refs/tags/$tag")" == "tag" ]] || { echo "Release tag must be annotated" >&2; exit 2; }
target="$(git rev-parse "refs/tags/$tag^{commit}")"
git cat-file -e "$target^{commit}" || { echo "Unknown commit" >&2; exit 2; }
echo "Release tag: $tag"
sudo docker compose --env-file .env -f compose.beta.yml config --quiet
echo "Current: $prev"
echo "Deploy:  $target"
echo "NOTE: Flyway migrations are forward-only; reverting images does NOT revert a database migration."
read -r -p "Backup BETA database and deploy pinned revision? Type DEPLOY: " answer
[[ "$answer" == DEPLOY ]] || exit 2
echo "Creating mandatory verified BETA database backup before any checkout or container changes."
sudo bash scripts/beta/backup.sh
echo "Backup command succeeded; this is NOT proof a complete restore rehearsal passed."
deployment_stage="checkout"
release_failure() {
  code=$?
  if (( code != 0 )); then
    echo "BETA DEPLOY FAILED at stage: $deployment_stage (exit $code)" >&2
    echo "Previous application revision: $prev" >&2
    echo "Requested revision: $target" >&2
    echo "Do not auto-rollback application or restore database: Flyway may have migrated forward." >&2
    echo "Inspect container logs, Flyway schema history, backup and restore compatibility before recovery." >&2
  fi
}
trap release_failure EXIT

git switch --detach "$target"
# --no-deps below intentionally excludes dependency startup; run the confined
# one-shot archive initializer explicitly before backend starts.
# Only the approved archive mount-root is changed; this is NOT a DB operation.
deployment_stage="archive-volume-init"
sudo docker compose --env-file .env -f compose.beta.yml run --no-deps --rm backup-volume-init
deployment_stage="compose-up"
sudo docker compose --env-file .env -f compose.beta.yml up -d --build --no-deps backend web
deployment_stage="localhost-health"
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
deployment_stage="local-smoke"
bash scripts/beta/smoke.sh http://127.0.0.1:18080
deployment_stage="public-health"
curl --fail --silent --show-error --max-time 12 https://family-point.com/api/client-health >/dev/null || {
  echo "Public HTTPS health failed; internal deployment works; check host Nginx / DNS / TLS" >&2; exit 1;
}
deployment_stage="complete"
trap - EXIT
echo "BETA RELEASE OK at $target"
echo "Revision recorded in git HEAD. Android API origin: https://family-point.com/"
