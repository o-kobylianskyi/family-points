#!/usr/bin/env bash
# Publish an already signed APK independently of backend/frontend Docker deployments.
# Run on Contabo inside /opt/familypoints-beta. No database or Docker changes.
set -Eeuo pipefail
cd "$(dirname "$0")/../.."
input="${1:-}"
[[ -f "$input" ]] || { echo "Usage: bash scripts/android/publish-apk.sh /path/to/familypoints-beta.apk /path/to/familypoints-beta.json" >&2; exit 2; }
metadata="${2:-}"
[[ -f "$metadata" ]] || { echo "Missing release manifest JSON" >&2; exit 2; }
[[ -d downloads ]] || { echo "downloads directory missing. Confirm BETA mount before publishing." >&2; exit 2; }
command -v python3 >/dev/null
command -v sha256sum >/dev/null
command -v flock >/dev/null
# Source identity, application metadata, size and SHA-256 verified before public changes.
# Signing certificate is independently checked on the operator workstation.
python3 scripts/android/verify_release_manifest.py "$input" "$metadata"
exec 9>downloads/.apk-publish.lock
flock -x 9
if [[ -f downloads/familypoints-beta.json ]]; then
  python3 - downloads/familypoints-beta.json "$metadata" <<'PY'
import json, sys
old = json.load(open(sys.argv[1], encoding='utf-8-sig'))
new = json.load(open(sys.argv[2], encoding='utf-8-sig'))
assert new['versionCode'] > old['versionCode'], 'New versionCode must exceed published versionCode'
PY
fi
read -r -p "Publish signed APK publicly without touching Docker/DB? Type PUBLISH: " confirmation
[[ "$confirmation" == PUBLISH ]] || exit 2
staging="$(mktemp -d downloads/.apk-stage.XXXXXXXX)"
trap 'rm -rf "$staging"' EXIT
cp -- "$input" "$staging/familypoints-beta.apk"
cp -- "$metadata" "$staging/familypoints-beta.json"
printf '%s  %s\n' "$(sha256sum "$staging/familypoints-beta.apk" | cut -d' ' -f1)" 'familypoints-beta.apk' > "$staging/familypoints-beta.apk.sha256"
# Stage files on the same filesystem; atomically switch each. Publish metadata last.
mv -f -- "$staging/familypoints-beta.apk" downloads/familypoints-beta.apk
mv -f -- "$staging/familypoints-beta.apk.sha256" downloads/familypoints-beta.apk.sha256
mv -f -- "$staging/familypoints-beta.json" downloads/familypoints-beta.json
echo 'APK published to /downloads/familypoints-beta.apk'
echo 'Verify: curl -I https://family-point.com/downloads/familypoints-beta.apk'
