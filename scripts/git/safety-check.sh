#!/usr/bin/env bash
# FP-SAFE-001: reject destructive staged changes before committing.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

deleted=0
protected=()
is_protected() {
  case "$1" in
    AGENTS.md|.gitignore|pom.xml|mvnw|mvnw.cmd|\
.github/workflows/ci.yml|.github/workflows/beta-release.yml|\
frontend/package.json|frontend/package-lock.json|\
android/settings.gradle.kts|android/build.gradle.kts|\
compose.beta.yml|Dockerfile.beta|\
src/main/resources/db/migration/V*.sql) return 0 ;;
    *) return 1 ;;
  esac
}
# -z preserves filenames containing whitespace; rename records contain old+new paths.
while IFS= read -r -d '' status; do
  IFS= read -r -d '' path || { echo "Malformed git diff output" >&2; exit 2; }
  case "$status" in
    D)
      deleted=$((deleted + 1))
      if is_protected "$path"; then protected+=("$path"); fi
      ;;
    R*)
      IFS= read -r -d '' new_path || { echo "Malformed rename entry" >&2; exit 2; }
      if is_protected "$path"; then protected+=("$path (renamed to $new_path)"); fi
      ;;
  esac
done < <(git diff --cached --name-status --diff-filter=DR -z)

if (( deleted > 20 || ${#protected[@]} > 0 )); then
  echo "FP-SAFE-001: commit blocked."
  if (( deleted > 20 )); then echo "Deleted tracked files: $deleted (maximum 20)."; fi
  for path in "${protected[@]}"; do echo "Protected path removed: $path"; done
  echo "Unstage unwanted deletions with git restore --staged <path>."
  exit 1
fi
exit 0
