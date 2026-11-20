#!/usr/bin/env bash
# REL-BETA-VOLUME-UID-FIX-01: real production Compose initializer in ephemeral CI.
set -Eeuo pipefail
: "${GITHUB_ACTIONS:?Only ephemeral GitHub CI}"
: "${GITHUB_RUN_ID:?Missing run id}"
: "${GITHUB_RUN_ATTEMPT:?Missing run attempt}"
[[ "${RUNNER_ENVIRONMENT:-}" == "github-hosted" ]] || exit 90
project="fp-uid-init-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
volume="${project}_beta_backup_artifacts"
root="/var/lib/familypoints/backups"
if docker volume inspect "$volume" >/dev/null 2>&1; then
  echo "STORAGE_INIT_EXISTING_VOLUME_REJECTED" >&2; exit 91
fi
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
chmod 0700 "$tmp"
printf 'DB_PASSWORD=synthetic-only\nJWT_SECRET=synthetic-only\n' > "$tmp/isolated.env"
chmod 0600 "$tmp/isolated.env"
dc=(docker compose -p "$project" --env-file "$tmp/isolated.env" -f compose.beta.yml)
# No backend, Web or database starts. Only the production Compose initializer.
"${dc[@]}" config --quiet
"${dc[@]}" run --no-deps --rm backup-volume-init
[[ "$(docker volume inspect -f '{{.Name}}' "$volume")" == "$volume" ]]
check_owner() {
  docker run --rm --network none --read-only --user 10001:10001 \
    --mount "type=volume,source=$volume,target=$root" alpine:3.20 sh -euc '
      d=/var/lib/familypoints/backups
      [ "$(id -u)" = 10001 ] && [ -w "$d" ] && [ ! -L "$d" ]
      [ "$(stat -c "%u:%g:%a" "$d")" = "10001:10001:700" ]
    '
}
check_owner
docker run --rm --network none --read-only --user 10001:10001 \
  --mount "type=volume,source=$volume,target=$root" alpine:3.20 sh -euc '
  set -eu
  d=/var/lib/familypoints/backups
  mkdir "$d/synthetic-family"
  printf "ephemeral-archived-ciphertext-placeholder" > "$d/synthetic-family/.pending"
  hash="$(sha256sum "$d/synthetic-family/.pending" | cut -d" " -f1)"
  ln "$d/synthetic-family/.pending" "$d/synthetic-family/signed"
  if ln "$d/synthetic-family/.pending" "$d/synthetic-family/signed" 2>/dev/null; then
    echo "UNSAFE_OVERWRITE" >&2; exit 32
  fi
  mv "$d/synthetic-family/.pending" "$d/synthetic-family/renamed"
  printf "%s" "$hash" > "$d/synthetic-family/expected.sha"
'
# Idempotency: rerun EXACT same init container with existing nonempty archive.
"${dc[@]}" run --no-deps --rm backup-volume-init
check_owner
docker run --rm --network none --read-only --user 10001:10001 \
  --mount "type=volume,source=$volume,target=$root" alpine:3.20 sh -euc '
  d=/var/lib/familypoints/backups/synthetic-family
  want="$(cat "$d/expected.sha")"
  got="$(sha256sum "$d/signed" | cut -d" " -f1)"
  [ "$want" = "$got" ] && [ -f "$d/renamed" ] || exit 33
  echo "EPHEMERAL_POST_BOOTSTRAP_UID10001_PASS"
'
