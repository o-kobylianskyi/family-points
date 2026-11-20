#!/bin/sh
# REL-BETA-VOLUME-UID-FIX-01: single mount-root ownership bootstrap.
# Runs ONLY inside the isolated backup-volume-init container.
set -eu
archive=/var/lib/familypoints/backups

# Fail-closed: exact, mounted archive root, not a symlink or host directory.
[ "$(id -u)" = "0" ] || { echo "ARCHIVE_INIT_NOT_ROOT" >&2; exit 20; }
[ -d "$archive" ] && [ ! -L "$archive" ] || {
  echo "ARCHIVE_INIT_INVALID_MOUNT" >&2; exit 21;
}
awk -v path="$archive" '$5 == path { found=1 } END { exit !found }' /proc/self/mountinfo || {
  echo "ARCHIVE_INIT_NOT_MOUNTPOINT" >&2; exit 22;
}

# IMPORTANT: only the exact archive mount root; no recursive ownership,
# no parent directories, no PostgreSQL/Web mounts, no broad chmod.
chown 10001:10001 "$archive"
chmod 0700 "$archive"
[ "$(stat -c '%u:%g:%a' "$archive")" = "10001:10001:700" ] || {
  echo "ARCHIVE_INIT_OWNER_MISMATCH" >&2; exit 23;
}
echo "ARCHIVE_INIT_VOLUME_ROOT_READY_UID10001"
