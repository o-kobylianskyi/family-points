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

## 2026-10-13 — Docker installed on Contabo (USER-VERIFIED CLI)
- User installed Ubuntu distribution packages: Docker Engine 29.1.3 and Docker Compose 2.40.3.
- Commands and reported outputs: `sudo docker --version` Docker version 29.1.3; `sudo docker compose version` version 2.40.3; `systemctl is-active docker` active; `systemctl is-active wg-quick@wg0` active; `sudo nginx -t` syntax and configuration test successful.
- Existing 1C PREROUTING TCP 8080 NAT entry count stayed at 2 after Docker install.
- This establishes component availability and unchanged count of 1C DNAT entries, **not** complete packet-level availability of the 1C route, WireGuard handshake, or firewall isolation.
- No BETA containers, database, migrations, HTTPS BETA or Android-to-BETA connections have yet been run or verified.
- Next: recheck UFW and existing VPN/1C external path, safely validate Compose configuration, and investigate Flyway baseline before starting backend. Preserve existing Docker/firewall/NAT behavior and do not publish new ports.

## 2026-10-13 — Repository deployed as source checkout (USER-VERIFIED)
- User created /opt/familypoints-beta, assigned owner adminfp, and cloned o-kobylianskyi/family-points branch feature/task-soft-delete via Git HTTPS.
- Git 2.43.0; git status reported clean, up to date with origin/feature/task-soft-delete. Project deployment Docker/Compose files, scripts and all three journals present on VPS.
- This is a **source checkout**, not running BETA containers or a released service.
- Inspected current GIT_FLYWAY_SETUP.md: existing local DB is version-1-baselined; generating a proper initial clean-install SQL baseline needs careful reconciliation with later migrations. Fresh empty PostgreSQL startup is NOT yet proven.
- Inspected JwtService; JWT_SECRET MUST contain sufficiently strong Base64-encoded random bytes. Corrected .env.beta.example (commit 9012064).
- Next: pull latest commit; create private local .env with independent secrets (do not print/upload); validate Compose config syntax with --quiet; check database bootstrap plan before starting backend; maintain external 1C forwarding and VPN intact.

## 2026-10-13 — BETA environment validated on VPS; V1 discovery (USER-VERIFIED / REPO-VERIFIED)
- User ran `git pull --ff-only` in `/opt/familypoints-beta`, created an untracked private `.env` via cryptographic random DB password and Base64 JWT signing key, permissions 0600, and ran `sudo docker compose --env-file .env -f compose.beta.yml config --quiet`. Command completed with `BETA configuration validation completed`. Values were not shared.
- **Correction to old Flyway warnings:** the CURRENT remote branch DOES contain `src/main/resources/db/migration/V1__baseline_schema.sql` (42,990 bytes, historical pre-V2 JPA schema, no user rows according to header) and `scripts/db/generate-v1-baseline.ps1`. Prior handover statement that V1 is absent was out of date. `GIT_FLYWAY_SETUP.md` itself has outdated pre-generation instructions. Clean V1+later migration execution remains untested on VPS; absence of V1 is no longer the blocker.
- User has not run BETA containers or Flyway migrations yet. Next: validate full fresh DB bootstrap and actual backend startup, starting with private-only Docker network and loopback Web. Check WireGuard/1C NAT before and after. Preserve existing persistent data and never run `down -v`.

## 2026-10-13 — PostgreSQL BETA started (USER-VERIFIED)
- On VPS under /opt/familypoints-beta, operator ran `sudo docker compose --env-file .env -f compose.beta.yml up -d database`.
- Docker pulled `postgres:17-alpine`, created `family-points-beta_internal` network and `family-points-beta_beta_postgres` persistent volume, and started `family-points-beta-database-1`.
- Immediate `docker compose ps`: database Up, health starting, no host port binding (only internal 5432/tcp).
- `pg_isready -U family_points -d family_points` returned `accepting connections`. This proves database listener availability, not Flyway schema correctness.
- Backend/Web not started, migrations not tested, public HTTPS and Android not deployed.
- NEXT: confirm database healthy and check Docker/UFW/WireGuard/1C safety; build backend and run Flyway migrations; verify migration count and HTTP readiness. Never run `down -v`.

## 2026-10-13 — BETA backend first successful start (USER-VERIFIED LOG)
- Operator supplied Docker Compose backend log for server-side startup at 2026-10-13 18:14–18:15 UTC.
- Spring Boot 4.1.1 with Java 21.0.12.1 started using `beta` profile. Tomcat started on internal HTTP port 8080; application completed startup in 18.304 seconds. Log says process running at startup; continuous health still needs a fresh check.
- Hikari acquired PostgreSQL connection via `jdbc:postgresql://database:5432/family_points`; DB reports PostgreSQL 17.11.
- Flyway validated and executed 26 migrations on the fresh, previously empty schema: V1 initial baseline schema through V26 task-instance occurrence slot; log explicitly states `Successfully applied 26 migrations ... version v26`.
- Hibernate 7.4.5.Final initialized JPA EntityManagerFactory; no schema validation error in supplied startup log. Historic assumption that missing V1 blocks fresh database deployment is decisively **SUPERSEDED**.
- Non-fatal warnings: custom AuthenticationProvider bypassing auto UserDetailsService configuration; spring.jpa.open-in-view enabled by default. Review before public beta.
- Still unverified: remote REST health response, initial user bootstrap, external HTTPS, React Web routing, Android access, persistence across reboot and actual external 1C forwarding.
- Next: start the BETA Web/Nginx container on loopback only; run host-local `/healthz` and `/api/client-health` smoke tests; inspect UFW/WireGuard/1C after Docker network creation; proceed to isolated HTTPS edge and controlled account provisioning.

## 2026-10-13 — Web/API internal smoke result (USER-REPORTED)
- After instructions to build Web and check loopback `/healthz` and `/api/client-health`, the operator replied `200`.
- HTTP 200 was reported, but it was not stated whether **both** endpoints separately returned 200; full response headers and `docker compose ps` were not supplied. Record internal HTTP smoke as **PARTIALLY VERIFIED**, not both endpoints independently proven.
- No public HTTPS on host Nginx, authenticated onboarding, external 1C forwarding test or Android BETA verification yet.
- NEXT: repeat labeled health checks, inspect existing host Nginx 80/443 sites and certificate availability, create separate HTTPS host for BETA with no changes to existing DEV 8443 and 1C NAT 8080.
