# FamilyPoints Beta on Contabo — deployment preparation

**Status:** configuration prepared; nothing has been installed on the VPS by these commits.

## Environment roles

- DEV: current laptop backend/React plus existing WireGuard/Nginx routing. Keep the dev domain and its API unchanged.
- BETA: VPS in Contabo, with an isolated PostgreSQL database, Spring Boot service and static React build served from HTTPS.
- Public Beta Web: `https://family-point.com/` (planned).
- Planned Beta API: **same HTTPS origin**, e.g. `https://family-point.com/auth/login`, `/workspaces/…`, `/api/client-health`. Nginx routes these paths to Spring Boot listening only on loopback. A separate API hostname can be configured instead if the existing DNS/virtual-host layout requires it.
- Android debug defaults to `https://api-dev.family-point.com:8443/`. Release defaults to `https://family-point.com/`, or override by passing `-PfamilyPointsReleaseApiUrl=https://YOUR-BETA-API/` during Gradle build.
- React production defaults to its own origin, or use `VITE_API_URL=https://YOUR-BETA-API` during `npm run build`.

## Step 1 — READ-ONLY VPS inspection, prior to any install

SSH with the user's existing key as `adminfp`. The actual Contabo configuration is Ubuntu 24.04, 4 vCPU, 8 GiB RAM. The current Nginx listens on 80 and 8443; the 1C Apache is on Windows and external 8080 is forwarded through UFW/NAT via WireGuard. On the VPS run:

```bash
uname -a
free -h
df -h /
for service in nginx apache2 postgresql docker; do
    printf '%s: ' "$service"
    systemctl is-active "$service" || true
done
sudo ss -lntp | grep -E ':(80|443|8443|8080|5432)\b' || true
sudo nginx -t
```

These commands do not change server configuration. Do not change the existing Nginx port-80/8443 sites, WireGuard forwarding, or UFW/NAT rules for 1C. The actual Apache is on the Windows laptop, not the VPS. Avoid sharing environment variables, secrets, tokens or private SSH keys.

## Step 2 — On findings, prepare Beta server runtime

1. Confirm VPS capacity: Spring Boot 4 / Java 21, PostgreSQL, Nginx, optional swap; compile Java and React on the laptop or in CI to avoid memory-heavy builds on the small VPS.
2. Use a separate PostgreSQL role and database (e.g. `family_points_beta`); deny public database port access. Back up before running Flyway and schedule off-machine backups with tested restore.
3. Put the backend secret values in a user-owned protected environment file outside Git, restrict permissions (e.g. `0600`) and issue a unique, strong Beta JWT secret. Docker administrators can see environment variables; never put production secrets in Git. Do not commit any secrets.
4. Use `deploy/beta/compose.yaml` on the VPS for PostgreSQL 17 and Java 21 API; bind backend port `127.0.0.1:18080` only, with restart policy and bounded heap. Build the JAR on laptop and place at `/opt/familypoints/beta/app/family-points.jar`.
5. Serve React `frontend/dist` via Nginx. Proxy `/auth/`, `/workspaces/` and `/api/` to backend while preserving the original URI. Check the complete route list, including optional APIs, before enabling the site. SPA routes use `try_files ... /index.html`.
6. Provision a valid public HTTPS certificate for the Beta domain and ensure all HTTP API calls from the browser use the HTTPS origin. Verify remote health `GET /api/client-health` returning `{"service":"family-points","status":"UP"}`.
7. Do not change the existing DEV / WireGuard virtual host. Avoid port 80 collisions with existing services.

## Step 3 — Android Beta

- Configure the Beta HTTPS endpoint via the release Gradle property if different from `family-point.com`.
- Generate and securely back up a private Android app-signing key **outside Git**. Use the same key for future upgrades so app data survives updates; never commit key/passwords.
- Install a signed Beta APK (not an unsigned release artifact) on a parent test device before a child device.
- Check online sign-in, offline sign-in, family members, balances, rewards, queued penalties/earnings, backend reconnection, and server restart.
- Never treat cached balances as spendable without a server-issued offline authorization policy.

## Release conditions

1. Maven build/tests and React production build pass.
2. Android Kotlin compilation and signed Beta APK install pass.
3. Current Flyway migrations apply to an isolated, backed-up Beta database.
4. TLS and actual phone network access work on mobile data without WireGuard.
5. Economy replay is idempotent and permissions are enforced by backend.
6. No child-device enforcement should be advertised until Usage Access, permission grants, policy enforcement and offline restrictions are actually tested.

## Actual VPS audit — 2026-10-12

User-tested: RAM 7.8 GiB / available 7.3 GiB, 94 GiB free disk; nginx and wg-quick@wg0 are active; docker and PostgreSQL are not installed; Nginx listens on 80 and 8443, but port 443 is not listening yet. UFW default denies incoming traffic with explicit SSH 22, WireGuard UDP 51820 and DEV 8443. 1C external 8080 is forwarded via an allowlist in UFW; it does NOT appear as a listening process in `ss` because DNAT is not a socket.

**Important:** Docker modifies netfilter/iptables and may affect the existing UFW routed policy. Before installing Docker, save the firewall/NAT/Nginx configs (privately on VPS), check DNAT and Docker package candidate availability:

```bash
sudo install -d -m 700 /root/fp-before-docker
sudo sh -c 'iptables-save > /root/fp-before-docker/iptables.rules'
sudo cp -a /etc/ufw /root/fp-before-docker/
sudo cp -a /etc/nginx /root/fp-before-docker/
sudo iptables -t nat -S PREROUTING | grep -E '8080|10\\.77\\.0\\.2' || true
apt-cache policy docker.io docker-compose-v2 docker-compose-plugin
sudo certbot certificates
```

Do not paste certificate private keys, auth credentials, SSH secrets or complete private configuration files in chat. Backups go into root-owned directories and must not be copied to public web roots. Check 1C NAT from an approved external client again after Docker installation; installing Docker without that check is not an acceptable rollout step.

## Empty Beta database and family onboarding (2026-10-12 decision)

**Do NOT restore/copy the development database, its accounts or test data into Beta.** The current development data is not suitable for real users.

The repo contains migrations V2–V20 but no initial compatible V1. Before launching the Beta API:
1. Reconstruct/verify the original pre-V2 schema as a V1 migration, test V1–V20 from empty database; **or** import a *schema-only* (no rows) verified V20 snapshot into a disposable new DB with an explicit Flyway V20 baseline. No user/data rows are transferred.
2. Add a transactional registration flow that creates the family's workspace, initial administrator, default workspace roles, POINTS economy and onboarding state.
3. Audit and enforce workspace isolation. `WorkspaceController` currently has cross-workspace read/update/delete risk, and several `EconomyController` endpoints do not check tenant membership. **Do not expose a multi-family Beta publicly until fixed and tested.**
4. Add first-run Web wizard to enter family members, roles, optional example tasks and rewards, with templates independently cloned into each family's workspace.

See `docs/MULTI_FAMILY_ONBOARDING_PLAN.md` for the full rollout and acceptance tests.

## Deployment assets

- `deploy/beta/compose.yaml`: PostgreSQL container with private volume; Java 21 runtime container serving Spring Boot through loopback only; requires a locally built JAR.
- `deploy/beta/.env.example`: template only. Real `.env` belongs on VPS, mode 0600.
- `deploy/beta/nginx-family-point.com.conf`: 443 HTTPS Beta, static React files under `/var/www/familypoints-beta`, proxies all `/auth`, `/api`, `/workspaces` requests to `127.0.0.1:18080`. Must NOT enable until the certificate exists and Nginx config test passes.

### Beta startup ordering after backup and Docker validation

1. Install Docker and Compose if the package candidates are valid, then **retest existing 1C routing and DEV** before creating any containers.
2. Create private `/opt/familypoints/beta`; copy `compose.yaml`, prepare `.env` and `app/family-points.jar` with secure ownership. Never expose Postgres on host networking.
3. `docker compose up -d db`; initialize an **empty-data** Beta schema using the verified V1–V20 chain (or approved schema-only V20 baseline), then inspect Flyway history.
4. `docker compose up -d api`; locally verify `http://127.0.0.1:18080/api/client-health` and backend logs.
5. Build frontend on laptop via `npm ci && npm run build`; deploy `frontend/dist` to `/var/www/familypoints-beta`.
6. Issue Let's Encrypt certificate for `family-point.com` with the previously installed Cloudflare DNS plugin; never print Cloudflare credentials. Enable only the new Nginx 443 site after `nginx -t`, then `sudo ufw allow 443/tcp` and reload Nginx. Test with mobile data; existing DEV continues on 8443.
7. Prepare and sign an Android Beta APK and test on one parent's phone before enrolling any child device. Rollbacks must preserve the PostgreSQL data volume.
