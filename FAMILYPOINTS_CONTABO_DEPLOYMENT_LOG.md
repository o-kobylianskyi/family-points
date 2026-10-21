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

## 2026-10-13 — BETA internal HTTP and HTTPS readiness verified (USER-VERIFIED CLI)
- Host-loopback GET `http://127.0.0.1:18080/api/client-health` returned HTTP 200, JSON `{"status":"UP","service":"family-points"}` through Nginx. Confirms Web proxy to Spring Boot; this is not authenticated end-to-end functionality.
- Host sockets: Nginx listening TCP 80 and 8443; Docker BETA Web bound only to `127.0.0.1:18080`. No TCP 443 listener at this time. Existing 1C DNAT 8080 should be preserved.
- Nginx sites-enabled were: default, familypoints-dev, familypoints-api-dev. No BETA host TLS site exists yet.
- Certbot listed DEV frontend/API certificates only, both expiring 2027-01-06; BETA apex domain certificate is not yet issued.
- Agreed next step: reuse host Nginx as TLS edge on 443 for `family-point.com`, proxy all paths to `127.0.0.1:18080`, preserve DEV 8443 and 1C NAT. Confirm DNS and Cloudflare credential existence without exposing secrets, issue apex certificate, configure separate server block and verify nginx -t before reload.

## 2026-10-13 — Apex BETA TLS certificate issued (USER-VERIFIED CERTBOT)
- `sudo certbot certificates` output includes `family-point.com`, ECDSA certificate expiring 2027-01-11 17:25:23 UTC. Certificate and private key reside in standard `/etc/letsencrypt/live/family-point.com/` locations; no private key contents stored in this journal.
- Existing `dev.family-point.com` and `api-dev.family-point.com` certificates remain listed, expiration 2027-01-06.
- This verifies certificate issuance only. BETA port 443 host Nginx listener, UFW allow 443, browser HTTPS, redirect, renewal reloading and authorization remain PENDING.
- Next: add a separate Nginx TLS vhost for apex on 443 using loopback upstream 127.0.0.1:18080, run nginx -t then reload and local curl with --resolve; inspect DNS and UFW then enable 443 with least privilege. Preserve DEV/1C rules.

## 2026-10-13 — Local HTTPS 443 through host Nginx verified (USER-VERIFIED)
- Operator reloaded host Nginx successfully (`sudo systemctl reload nginx`).
- `curl -i --resolve family-point.com:443:127.0.0.1 https://family-point.com/api/client-health` returned HTTP/1.1 200 with JSON `{"status":"UP","service":"family-points"}`. TLS validation succeeded using curl defaults.
- Confirms local TLS host Nginx -> BETA loopback web proxy -> Spring Boot route. Explicit DNS override means **public reachability was not tested**.
- Dedicated host port 443 still needs UFW allowance and external network verification; independently check React HTML, login bootstrap, permissions, Android access and 1C forwarding.
- Preserve existing DEV port 8443 and 1C DNAT; after UFW change compare NAT count and WireGuard service, then check from external client.


## 2026-10-13 — BETA initial family onboarding policy (AGREED / NOT DEPLOYED)
- Public HTTPS Web/API was locally verified; user reports Web opens externally, but BETA database does not yet have a safely provisioned family/account.
- User stopped manual initial administrator SQL creation and reaffirmed original product design: a visitor registers and creates their own new tenant-isolated family; one person may belong to multiple families/workspaces. Optional initial tasks and economy templates should be customizable to family composition (age and optionally gender/preferences).
- `docs/MULTI_FAMILY_ONBOARDING_PLAN.md` updated in commit 941cb3e. No admin SQL was applied and no new registration API deployed on Contabo.
- Before opening real signup to the internet implement transactional first-family creation, auth security/rate limiting, tenant isolation regression and optional bootstrap templates; preserve existing DEV and 1C services.


## 2026-10-13 — Release policy confirmed: DEVELOP → ACCEPT → DEPLOY → TEST
- User directed that remaining registration/multi-family/onboarding work be completed and tested locally on DEV first, then promoted to BETA on Contabo for end-to-end testing.
- Existing BETA containers and public HTTPS must not be automatically updated from ongoing DEV commits. Pin and deploy a tested revision only, after backup and migration review.
- DEV acceptance must include one login with multiple workspace memberships, two unrelated families unable to read or mutate each other's resources, registration/setup and template initialization.
- No server deployment was executed by this policy documentation change.


## 2026-10-14 — Separate client WireGuard transit via Contabo (USER-VERIFIED; REBOOT/RELOAD NOT YET VERIFIED)

- **Scope and approval:** A client-owned 1C environment is accessed over an existing personal WireGuard peer, now relocated from the Windows laptop to the operator's Contabo VPS as \`wg1\`, with the client's VPN use and any subsequent external exposure subject to client-administrator authorization. This is independent of the FamilyPoints application; do not expose customer RDP publicly. Existing \`wg0\`, FP BETA/DEV, and the laptop test API DNAT/whitelist must be preserved.
- **Preflight (observed via user-provided SSH output):** \`wg-quick@wg0\`, Nginx, Docker active; \`wg0=10.77.0.1/24\`; existing Docker subnets 172.17/16 and 172.18/16; no visible route conflict with \`wg1=10.9.0.9/24\` or customer target \`192.168.2.146/32\`. Public client WireGuard endpoint still routed via \`eth0\`. Existing port-8080 DNAT appeared twice in live iptables and this count remained unchanged. Server reports reboot required; **do not reboot casually**.
- **Backup (USER-VERIFIED):** root-only snapshot at \`/root/contabo-before-wg1-20261014-155206\`, including \`/etc/wireguard\`, \`/etc/ufw\`, \`/etc/nginx\`, iptables/ip6tables/nft rules, routes and forwarding settings. Root-only directory permissions confirmed. No secrets copied to GitHub.
- **WG1 configuration:** root-owned \`/etc/wireguard/wg1.conf\` mode 0600; Windows tunnel configuration transferred securely, Windows CRLF removed, DNS override removed; \`Address=10.9.0.9/24\`, \`MTU=1420\`, peer \`AllowedIPs=192.168.2.146/32\`, \`PersistentKeepalive=25\`. \`wg-quick strip wg1\` passed. Client tunnel on laptop disabled to avoid simultaneous reuse of the same WireGuard identity. \`wg-quick@wg1\` started and later enabled at boot.
- **Tunnel verification:** \`wg1\` active, WireGuard handshake and nonzero RX/TX confirmed; customer \`192.168.2.146\` routes via \`wg1\`; public WireGuard endpoint via \`eth0\`. VPS GET \`https://192.168.2.146/KUP/uk/\` returned HTTP 200 with certificate validation bypassed **for this diagnostic only**. Operator subsequently confirmed Windows laptop RDP and HTTPS worked via \`wg0 → Contabo → wg1\`.
- **Forwarding scope:** laptop \`10.77.0.2/32\` to customer server \`192.168.2.146/32\`, TCP ports 443 (1C HTTPS) and 44322 (RDP) only; return ESTABLISHED/RELATED traffic accepted; SNAT to \`10.9.0.9\`. Three matching live iptables rules were first inserted and shown idempotent (2 FORWARD / 1 POSTROUTING before and after reapplication).
- **Routing startup:** \`/usr/local/sbin/client-wg1-routing.sh\` implements \`up|down\` with iptables existence checks; \`/etc/systemd/system/client-wg1-routing.service\` was created, syntax verified, and started successfully with \`active (exited)\`. Initially both service and \`wg-quick@wg1\` were enabled.
- **UFW persistence work:** Operator created \`/root/wg1-before.rules.candidate\` from \`/etc/ufw/before.rules\`; added precisely 3 rules (1 NAT POSTROUTING SNAT in \`*nat\`, 2 \`ufw-before-forward\` ACCEPT rules). \`iptables-restore --test\` on candidate succeeded, and \`diff -u\` showed only those additions, preserving existing laptop port-8080 DNAT/SNAT. Candidate was installed as \`/etc/ufw/before.rules\`; \`grep -c 192.168.2.146\` returned 3. **No UFW reload, VPS reboot, or post-reboot recovery test performed.**
- **Avoid duplicate ownership after reboot:** \`client-wg1-routing.service\` was **disabled**, not stopped, so it remained active in the current boot while UFW is intended to load the permanent three rules at future startup. \`wg-quick@wg1\` remains \`enabled\`. User verified \`wg1 enabled\`, routing service \`disabled\` but \`active\`. Live iptables rules were not duplicated by this change.
- **Non-regression checks supplied:** \`nginx -t\` OK; FamilyPoints BETA \`https://family-point.com/api/client-health\` returned 200 through local TLS; DEV returned expected 401 from Basic Auth; client 1C \`/KUP/uk/\` 200; Nginx and Docker active; old 1C DNAT rule count still 2; UFW active with prior whitelisted forwarding. The existing *external* laptop API path was not independently tested from an allowlisted external IP during this package.
- **Remaining checks / risks:** User was asked to test \`iptables-restore --test\` on installed \`/etc/ufw/before.rules\`, Windows \`Test-NetConnection\` to ports 443 and 44322, public FP and laptop 1C API from an approved external address; final results not yet supplied for these exact checks. Arrange an out-of-band VPS console/recovery window before any \`ufw reload\` or reboot; after such an event, recheck WG handshakes, FORWARD/SNAT, return route, public 8080 whitelist and FP HTTPS. No public customer API reverse proxy or whitelist configured yet. Customer 1C API paths known privately; TLS trust, external IP allowlist, and authorization for third-party exposure must be validated before publishing.

## 2026-10-14 — Client test 1C/API HTTPS publication on IP:9443 (USER-VERIFIED / PUBLICLY ACTIVATED)

- **Authorization/scope:** Operator confirmed customer permission for limited exposure of **test** 1C base and its HTTP API. Main production 1C base `/KUP/` and RDP must remain inaccessible through public gateway. Publication intentionally uses Contabo public IPv4 on TCP 9443 with no additional DNS name.
- **Upstream reachability:** `https://192.168.2.146/Test_KUP_API/hs/CreatioExch` responded HTTP 401 on VPS over `wg1` (reachable, requires 1C authentication). Upstream presents Cloudflare Origin CA certificate with SAN `bas.mardal.eu`. Operator installed Cloudflare Origin CA RSA and ECC roots in local PEM bundle and verified `openssl s_client` with SNI/hostname `bas.mardal.eu`: TLS 1.3, `Verification: OK`. No insecure backend TLS bypass in final proxy.
- **Public-IP TLS certificate:** Separate Python venv Certbot 5.8.0 at `/opt/certbot-ip`, separate config/work/log dirs `/etc/letsencrypt-ip`, `/var/lib/letsencrypt-ip`, `/var/log/letsencrypt-ip`. Host Nginx existing HTTP port 80 serves `/.well-known/acme-challenge/` and returns 404 otherwise; UFW opened TCP 80 for ACME. Two staging attempts failed at Let's Encrypt side with `No such authorization` and `Certificate not found`; subsequent **production** issuance succeeded for `184.174.37.206`. Certificate files `/etc/letsencrypt-ip/live/184.174.37.206/{fullchain.pem,privkey.pem}`, expiry 2026-10-21; no private key committed.
- **Nginx:** Added `/etc/nginx/sites-available/client-test-1c`, enabled via `sites-enabled`. Listens TCP `9443 ssl default_server`. `location ^~ /Test_KUP_API/` proxies HTTPS to `192.168.2.146`, sending upstream SNI and Host `bas.mardal.eu` and verifying against local Cloudflare Origin CA trust bundle. Other paths (including `/KUP/`) return 404. IP allowlist in `/etc/nginx/snippets/client-1c-whitelist.conf` has the same nine IPs as existing laptop test 1C/API UFW allowlist, followed by **`deny all;`**. An initial missing `deny all;` caused local loopback test to return 200; corrected, reloaded Nginx, and loopback access to test base then returned HTTP 403 as expected. Local main-base path returned 404. Public client RDP **not** exposed.
- **Firewall:** Nine targeted UFW `allow from <approved-source> to any port 9443 proto tcp` rules added, no unrestricted 9443 ALLOW. `sudo ufw status | grep 9443` showed exactly nine source-specific entries. Existing Nginx 443 (FamilyPoints), 8443 (DEV), old port-8080 forwarding and VPN were intended to remain unchanged; no fresh full non-regression run supplied after 9443 was enabled.
- **External result:** User reported successful browser access to the test base via public IP:9443. A different Windows host did not have `curl.exe` installed, so API status and external main-base 404 were not independently recorded in that check. Outside-whitelist source connection denial has not yet been demonstrated from an external host; local 403 and targeted UFW allow rules were verified.
- **Renewal:** Separate `certbot-ip-renew.service` invokes venv Certbot with isolated dirs and `--deploy-hook "/usr/bin/systemctl reload nginx"`; `certbot-ip-renew.timer` enabled with two daily checks. Operator supplied `systemctl list-timers --all certbot-ip-renew.timer`: next run 2026-10-15 03:17:44 CEST. **`certbot renew --dry-run` succeeded**, reported simulated renewal of IP cert and `all simulated renewals succeeded`. This does **not yet prove** real renewal or successful deploy-hook reload; monitor certificate expiry/reload, given ~6–7 day certificate lifetime.
- **Remaining:** Verify external `/KUP/` gives 404 from an allowlisted source and 9443 is inaccessible from a non-allowlisted source; verify permitted API works with appropriate client authorization. Record application behavior (GET/POST as appropriate), check 9443 Nginx access logs and upstream errors without secrets. Confirm actual IP certificate renewal before Oct 21. Avoid unplanned UFW reload/VPS reboot until recovery and wg1 persistence tested, and independently check FamilyPoints and old laptop 1C/API externally.

## 2026-10-21 — Priority shift: promote BETA + installable Android app (COMMITTED, NOT DEPLOYED)
- User explicitly prioritized (1) update existing Contabo BETA on release domain family-point.com and (2) build phone APK pointed to Contabo, with straightforward repeatable server and Android updates.
- Existing BETA Docker/PostgreSQL/TLS deployment confirmed historically by operator; no new host installation required. Current runtime state, Flyway version, backups and public HTTPS were NOT verified from live VPS in this turn.
- Added scripts/beta/deploy-release.sh: operator-invoked, full-SHA pinning, working tree preflight, private .env validation, mandatory database backup, rebuild only backend/web services, internal/external health verification; NO down -v, no changes to host firewall, WireGuard or Nginx. Migration rollback is NOT automatically safe and explicitly requires inspection.
- Android release Gradle supports environment-supplied persistent signing key and incrementing versionCode/versionName; scripts/android/build-beta-release.ps1 builds a signed installable release APK with default API URL https://family-point.com/. No signing key committed or generated.
- Next: operator review current VPS state; create off-host encrypted copy of BETA database backup, inspect migrations V27–V34 and registration/public flags, choose approved tested SHA, run controlled script. Then create/store release keystore securely (backup required) and build/install Android APK, verify on cellular with laptop off.
- STATUS: COMMITTED ONLY. No VPS deployment, database migration, external Android installation or secret rotation performed.
