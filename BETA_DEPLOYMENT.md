# FamilyPoints BETA — isolated server deployment

## Scope
The configuration is for a **new, disposable, isolated BETA PostgreSQL database**. Do not connect it to existing family development/production databases or overwrite existing containers/volumes. On the server, Docker Compose is required (Linux Docker host or supported Linux VM); a Windows Server with only IIS/Apache/PostgreSQL cannot run this Linux-image stack directly without an appropriate Linux container runtime/VM.

## Prepare and run
1. Check that ports and database names do not conflict with existing server services. Install Docker Engine with Compose plugin on an isolated Linux host/VM.
2. Clone repository and checkout `feature/task-soft-delete`. Set up DNS and external TLS reverse proxy separately.
3. Copy `.env.beta.example` to `.env`, replace the example secrets with independent strong random values and restrict file permissions. Do not reuse local DEV JWT or DB secrets.
4. Run `docker compose --env-file .env -f compose.beta.yml config` to validate interpolation.
5. Run `docker compose --env-file .env -f compose.beta.yml up -d --build`.
6. On the server run `curl -f http://127.0.0.1:18080/healthz` and `curl -f http://127.0.0.1:18080/api/client-health`. Check `docker compose --env-file .env -f compose.beta.yml logs --tail=100 backend` for Flyway and boot errors.
7. Put a correctly configured HTTPS reverse proxy in front of 127.0.0.1:18080, forwarding X-Forwarded-Proto and Host. Never expose the localhost-only HTTP port publicly. Web and API use one HTTPS origin; no cross-origin browser CORS is needed.

## Important BETA gates
- **DO NOT expose to the public until explicitly reviewed:** account creation/invitation/onboarding, authentication, cross-workspace authorization, secrets, backup/restore, rate limits, HTTPS, log redaction, and privacy policy.
- Never use `docker compose down -v` on a database containing wanted BETA data. Back up with `pg_dump`, then test restore into a separate disposable instance. Database migrations run on startup with Flyway; inspect migrations and backup before applying to an existing deployment.
- Initial login accounts are **not automatically created** by this Compose stack. Establish a controlled authorized onboarding/bootstrap workflow before inviting testers.
- The server environment has not been inspected and deployment has not been executed from this chat.
- Android debug uses `https://api-dev.family-point.com:8443/`; configure Android BETA API separately and do not point tester APKs at unverified endpoints.

## Quick smoke checklist
- HTTPS certificate valid, no mixed content; `/healthz` and `/api/client-health` work.
- Parent and child log in; child cannot see or modify other members' tasks, including direct HTTP requests.
- Create a task with 2 scheduled slots; independently start/complete each; repeat action does not double-credit.
- Reopen tasks, offline/online Android read-only schedule cache, reopen after backend restart.
- Confirm PostgreSQL data survives `docker compose restart`; tested backup and restore.


## Executable operational checks and Android BETA build

From the Linux BETA Docker host, after the stack boots:
```bash
bash scripts/beta/smoke.sh http://127.0.0.1:18080
bash scripts/beta/backup.sh
```
The smoke check validates Nginx's own health, SPA HTML and the backend's health route (HTTP 200). It does **not** test user login, authorization or persistence. The backup script writes to ignored `backups/` and must be supplemented by a tested restore and encrypted off-host storage.

To produce a BETA-specific **debug** APK on the developer laptop, use a real, HTTPS, reachable endpoint (replace the placeholder with the actual API serving HTTPS origin):
```powershell
cd C:\Users\Oleh\IdeaProjects\family-points\android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug -PfamilyPointsDebugApiUrl=https://YOUR-BETA-DOMAIN/
```
The debug API setting defaults to the previous DEV endpoint unless an override is explicitly provided. The release API setting is separate (`familyPointsReleaseApiUrl`), and a debug APK is not a production-signed Play Store artifact. Verify TLS and `/api/client-health` before installing on testers' phones.

**Server prerequisite:** Determine whether the actual host is Windows Server with IIS/Apache, as discussed previously, or a Linux Docker host. This Compose file uses Linux containers and is not an IIS application. Do not uninstall or reconfigure the existing 1C/BAS, IIS, Apache, WireGuard or PostgreSQL services to deploy it. A separate Linux VM is safer when that host is Windows Server.

**Flyway precaution:** The repository uses `spring.flyway.baseline-on-migrate=true` and `spring.jpa.hibernate.ddl-auto=validate`. A fresh BETA database must be validated through a real startup; passing Compose syntax validation alone does not guarantee that all Flyway migrations and JPA validation can initialize an empty database.
