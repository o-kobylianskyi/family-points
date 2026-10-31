#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${1:-http://127.0.0.1:18080}"
BASE_URL="${BASE_URL%/}"

echo "Checking Web health: ${BASE_URL}/healthz"
curl --fail --silent --show-error --max-time 15 "${BASE_URL}/healthz"
echo

echo "Checking frontend SPA: ${BASE_URL}/"
html="$(curl --fail --silent --show-error --max-time 15 "${BASE_URL}/")"
if [[ "${html}" != *"<html"* && "${html}" != *"<HTML"* ]]; then
  echo "Expected HTML page but got a different response" >&2
  exit 1
fi

echo "Checking backend readiness route: ${BASE_URL}/api/client-health"
status="$(curl --silent --show-error --max-time 15 --output /dev/null --write-out '%{http_code}' "${BASE_URL}/api/client-health")"
if [[ "${status}" != "200" ]]; then
  echo "Backend health endpoint returned HTTP ${status}; inspect application logs" >&2
  exit 1
fi
echo "Web + backend smoke checks passed"
