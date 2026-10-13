# FamilyPoints — Contabo deployment journal

Created: 2026-10-13. Historical source: user-provided server handover dated 2026-10-12.

## Journal rules
Append a dated entry after every server/deployment package. Preserve old entries. Each entry covers: action and goal; observed state; changed files; Git SHAs; verification (CI, SSH, HTTP, database); backup/rollback; failures and next steps. Clearly label REPORTED, COMMITTED, DEPLOYED, VERIFIED, BLOCKED. Never put secrets, private keys, credentials, full firewall access lists, personal data or database backups in Git. Update FAMILYPOINTS_DEVELOPMENT_LOG.md for cross-project milestones.

## 2026-10-08–12 — Existing server infrastructure (REPORTED, not live-verified)
- Contabo VPS running Ubuntu 24.04 with 4 vCPU and 8 GB RAM; administrated by SSH key.
- VPS hosts Nginx, UFW and WireGuard. The laptop's DEV frontend and backend are reached through WireGuard; VPS currently operates as a reverse proxy for DEV.
- DEV HTTPS: frontend at dev.family-point.com:8443, API at api-dev.family-point.com:8443. Nginx proxies the respective laptop Vite and Spring Boot ports. Frontend has Basic Auth; API uses Spring JWT.
- Existing separate 1C/BAS HTTP forwarding traverses VPN using a dedicated public-facing port and firewall/NAT restrictions. This production-dependent forwarding, WireGuard addresses/keys, and NAT/firewall configuration MUST NOT BE CHANGED as part of BETA deployment.
- Previous laptop browser tests on 2026-10-08 confirmed DEV HTTPS, React login, dashboard and balances. Phone browser access was not conclusively retested after correcting HTTP-versus-HTTPS URL format.
- DEV certificate renewal was configured; validity and current Nginx settings require a fresh live check.

## Target architecture (AGREED / NOT YET DEPLOYED)
- BETA runs autonomously on Contabo: Spring Boot Java 21, PostgreSQL with dedicated persistent volume, built React frontend, Docker Compose, TLS via host Nginx.
- DEV stays on Windows laptop, separately accessed through existing VPS reverse proxy/VPN; DEV and BETA use different databases/secrets.
- BETA Web proposed at https://family-point.com; optional separate api.family-point.com DNS/API endpoint remains a proposal, not an existing setup. Current BETA Web configuration uses a single origin and reverse proxies /auth, /api, /workspaces.
- Expose BETA Web container only to VPS loopback (port 18080). Host Nginx handles external HTTPS on 443; never repurpose DEV TLS on 8443 or the existing external 1C gateway.
- Android must use BETA HTTPS API and be verified on cellular network with laptop powered off.
- Public exposure requires identity bootstrap/invitation, authorization review, backup and restore proof, secure HTTPS, privacy controls and operational monitoring.

## Critical Flyway consideration (REPORTED)
- Existing Windows DEV database reportedly uses Flyway baseline version 1, but a full clean-install schema baseline V1 is not present in the repository. Later migrations alone may not initialize an empty PostgreSQL BETA database.
- Before starting the BETA backend, verify current migration history and initialization path. Consider a carefully controlled restore into separate BETA PostgreSQL only after privacy review and backup. Do not manufacture a V1 from the final schema and reapply V2+ blindly.
- No existing family/child data should be exposed or migrated without safeguards.

## 2026-10-13 — BETA infrastructure scaffolding (COMMITTED / NOT DEPLOYED)
- Branch: feature/task-soft-delete in o-kobylianskyi/family-points.
- Dockerfile.beta, frontend/Dockerfile.beta, frontend/deploy/nginx.beta.conf, compose.beta.yml, .env.beta.example, BETA_DEPLOYMENT.md added: commits 6ae7642, 792d2d2, 3c7fda5, b86e77d, ddaa157, ae0faa0.
- CI Docker Compose validation job added in bd0c84a.
- This is preparation only: no live VPS SSH inspection, image build, database deployment, TLS setup or end-to-end test has been performed.

## 2026-10-13 — Operational and Android follow-up (COMMITTED / NOT DEPLOYED)
- User reports local Android BUILD SUCCESSFUL. This does not establish APK connectivity to server or passing remote CI.
- Android debug API URL can be set explicitly using Gradle property familyPointsDebugApiUrl; commit c962ce1.
- Added scripts/beta/smoke.sh and scripts/beta/backup.sh in commits 955445f and ee62cf6.
- Excluded backup dumps from Git, expanded BETA_DEPLOYMENT.md and CI syntax verification: faf9478, a5ad51c, 720bd69.
- Two other persistent journals are FAMILYPOINTS_DEVELOPMENT_LOG.md and FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md. Deployment-specific commands and results belong in this journal.

## Next deployment packet (PENDING)
1. Inspect actual Contabo OS/resources/Docker status/Nginx/listeners/firewall/WireGuard via read-only checks; never expose credentials or private configuration dumps.
2. Back up relevant server configuration. Preserve existing DEV and 1C forwarding untouched.
3. Verify Flyway initialization, create separate BETA PostgreSQL and run local-only Docker Compose stack.
4. Run HTTP smoke check and authorized login/permissions tests; test PostgreSQL backup restoration.
5. Configure BETA HTTPS on host Nginx without changing existing services; verify TLS/DNS and certificate renewal.
6. Build Android debug APK against verified BETA HTTPS endpoint; test over cellular and with DEV laptop offline.
7. Append actual commands, verified outcomes, SHAs, risks, and rollback steps to this journal.

## 2026-10-13 — Log preservation decision
User requested a dedicated, permanently maintained Contabo deployment history in GitHub. Historical descriptions are not live-state assertions.


## 2026-10-13 — Reconciliation with captured Contabo commands from 2026-10-12 (EVIDENCE REVIEWED; NO LIVE SSH IN THIS SESSION)
- Ubuntu 24.04.5 x86_64, approx. 7.8 GiB RAM (7.3 GiB available) and 96 GB root filesystem (94 GB free), recorded by ssh/free/df. Login with admin SSH key succeeded. Shell banner reported restart required.
- nginx and wg-quick@wg0 were active; apache2, PostgreSQL and Docker services inactive. `nginx -t` succeeded. UFW was active with deny incoming, allow outgoing and deny routed defaults.
- `docker --version` and `docker compose version` returned command not found. Apt package candidates at the time: docker.io 29.1.3 and docker-compose-v2 2.40.3; `docker-compose-plugin` not found under that package name in configured Ubuntu repositories. Docker installation is still PENDING.
- Listening sockets observed: nginx TCP 80 and 8443, WireGuard UDP 51820; no listener recorded for 443/18080/5432. Absence of socket listener 8080 does NOT by itself disprove 1C DNAT forwarding, which acts in NAT instead of a local service.
- Observed UFW FORWARD rules covered eight main external source addresses **plus one additional test source**, not just the eight in the submitted report. No full external 1C route test was provided; do not mark end-to-end forwarding VERIFIED.
- `iptables -t nat -S PREROUTING` output listed the same 1C DNAT rule **twice**. Possible duplication requires comparison with persisted /etc/ufw/before.rules and other ruleset sources, not blind deletion.
- The operator ran commands to create /root/fp-before-docker and capture an iptables rules snapshot, and copy /etc/ufw and /etc/nginx. Commands returned to prompt without visible errors; confirm the snapshot files exist and their access restrictions before modifying Docker/UFW.
- Certbot listed two ECDSA DEV certificates expiring 2027-01-06; no BETA apex certificate in that output.
- Unresolved: recent WireGuard handshake/actual 1C HTTP path, reason for requested reboot, full 443 access setup, Docker installation, PostgreSQL baseline, offsite verified backup. No VPS deployment action was performed during this review.
- Operational safety: preserve port 8080 DNAT, current DEV HTTPS on 8443, WireGuard and UFW rules. Docker can modify firewall/forwarding rules, so snapshot then inspect Docker/UFW coexistence before enabling any public Docker bindings.


## 2026-10-13 — Docker preflight execution preparation (COMMITTED, NOT RUN ON VPS)
- User authorized beginning practical deployment on Contabo.
- Added read-only `scripts/beta/contabo-docker-preflight.sh` (commit `6b622e6`) to report OS/resources/services/socket listeners, firewall config fingerprints, existing NAT 8080 count and backup directory presence without dumping secrets or full allowlists.
- Deployment control: Docker/UFW packet-filter interactions are a material risk to existing WireGuard/1C forwarding; preserve current rules and compare before/after. Docker installation on VPS not performed by GitHub connector.
- Requested user to run read-only preflight first. Await observed results before executing potentially disruptive package installation or daemon startup. No claims of live Docker/BETA deployment or successful external 1C check.
