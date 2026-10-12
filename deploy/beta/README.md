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

SSH with the user's existing key as `adminfp`. On the VPS run:

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

These commands do not change server configuration. In particular, do not stop or replace Apache, which was previously reported to be using port 80, before identifying what it serves. Avoid sharing environment variables, secrets, tokens or private SSH keys.

## Step 2 — On findings, prepare Beta server runtime

1. Confirm VPS capacity: Spring Boot 4 / Java 21, PostgreSQL, Nginx, optional swap; compile Java and React on the laptop or in CI to avoid memory-heavy builds on the small VPS.
2. Use a separate PostgreSQL role and database (e.g. `family_points_beta`); deny public database port access. Back up before running Flyway and schedule off-machine backups with tested restore.
3. Put the backend secret values in a root-/service-owned environment file outside Git, restrict permissions (e.g. `0600`) and issue a unique, strong Beta JWT secret. Do not commit any secrets.
4. Run Spring Boot as an unprivileged systemd service bound to loopback; enable restarts and bounded Java heap appropriate for available memory.
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
