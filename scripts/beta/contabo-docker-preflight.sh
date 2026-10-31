#!/usr/bin/env bash
set -euo pipefail
echo '=== Contabo BETA Docker preflight (read-only, secrets not printed) ==='
date -Is
uname -srmo
. /etc/os-release; echo "OS: $PRETTY_NAME"
free -h
df -h /
echo '=== Existing services ==='
for s in nginx wg-quick@wg0 docker; do printf '%s: ' "$s"; systemctl is-active "$s" || true; done
echo '=== Docker versions ==='
command -v docker >/dev/null && { docker --version; docker compose version || true; } || echo 'Docker not installed'
echo '=== Listening TCP ports (numeric endpoints, no processes) ==='
sudo ss -lnt | awk 'NR==1 || /:(80|443|8443|8080|18080|5432)[[:space:]]/'
echo '=== UFW active? ==='
sudo ufw status | head -n 2
echo '=== Firewall configuration fingerprints ==='
sudo sha256sum /etc/ufw/before.rules /etc/nginx/nginx.conf
echo '=== NAT DNAT count for existing 1C gateway ==='
sudo iptables -t nat -S PREROUTING | grep -c -- '--dport 8080' || true
echo '=== Existing administrative backup directory ==='
sudo test -d /root/fp-before-docker && echo 'present' || echo 'not found'
echo '=== Completed read-only preflight ==='
